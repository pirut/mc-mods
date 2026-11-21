#!/usr/bin/env bash
# Releases one mod: .github/scripts/release-mod.sh <folder in mods/>
#
# Makes the GitHub release <folder>-v<mod_version> with the mod's jars and the notes from its CHANGELOG.md, then
# uploads the jars to Modrinth (when the mod has a modrinth_project_id and the MODRINTH_TOKEN secret is set) and to
# CurseForge (when it has a curseforge_project_id and the CURSEFORGE_TOKEN secret is set).
# A version that is already on GitHub is not released again, but the store uploads still run: Modrinth skips a
# version it already has. CurseForge can't be asked that, so it only gets the files when the GitHub release was
# made in this run, or when CURSEFORGE_UPLOAD=1 (the "Upload to CurseForge too" box when run by hand).
# Expects the jars to be built already (./gradlew :<folder>:build).
set -euo pipefail

mod=$1
dir="mods/$mod"
[ -f "$dir/gradle.properties" ] || { echo "::error::No mod at $dir"; exit 1; }

: "${RUNNER_TEMP:=/tmp}"

prop() { # key [file...]: the last value wins, like Gradle's mod-over-root order
  local key=$1; shift
  grep -h "^$key=" "$@" 2>/dev/null | tail -n1 | cut -d= -f2- | tr -d '\r' || true
}
id=$(prop mod_id "$dir/gradle.properties")
name=$(prop mod_name "$dir/gradle.properties")
version=$(prop mod_version "$dir/gradle.properties")
mc=$(prop minecraft_version gradle.properties "$dir/gradle.properties")
project=$(prop modrinth_project_id "$dir/gradle.properties")
cf_project=$(prop curseforge_project_id "$dir/gradle.properties")
required=$(prop modrinth_required "$dir/gradle.properties")
tag="$mod-v$version"
repo="${GITHUB_REPOSITORY:-pirut/mc-mods}"
release_url="https://github.com/$repo/releases/tag/$tag"

gh_status="Not run"
modrinth_status="Not run"
curseforge_status="Not run"

append_summary() {
  if [ -n "${RELEASE_SUMMARY_FILE:-}" ]; then
    printf '| `%s` (%s) | `%s` | %s | %s | %s |\n' \
      "$mod" "${name:-$mod}" "$version" "$gh_status" "$modrinth_status" "$curseforge_status" \
      >> "$RELEASE_SUMMARY_FILE"
  fi
}

fresh=1
if gh release view "$tag" >/dev/null 2>&1; then
  echo "$tag is already on GitHub; checking the stores."
  fresh=0
fi

# This version's section of the mod's CHANGELOG.md
awk -v v="$version" '$0 == "## " v {on=1; next} /^## / {on=0} on' "$dir/CHANGELOG.md" > "$RUNNER_TEMP/notes-$mod.md"
if [ ! -s "$RUNNER_TEMP/notes-$mod.md" ]; then
  echo "::error::$dir/CHANGELOG.md has no '## $version' section"
  gh_status="Failed (missing CHANGELOG section)"
  append_summary
  exit 1
fi

dist="$RUNNER_TEMP/dist-$mod"
rm -rf "$dist" && mkdir -p "$dist"
jars=()
for loader in neoforge fabric; do
  jar="$dir/$loader/build/libs/$id-$loader-$mc-$version.jar"
  if [ -f "$jar" ]; then cp "$jar" "$dist/" && jars+=("$loader"); fi
done
if [ ${#jars[@]} -eq 0 ]; then
  echo "::error::No jars for $mod $version; build it first"
  gh_status="Failed (no jars built)"
  append_summary
  exit 1
fi
( cd "$dist" && sha256sum *.jar > SHA256SUMS.txt )

notes="$RUNNER_TEMP/release-notes-$mod.md"
{
  cat "$RUNNER_TEMP/notes-$mod.md"
  echo
  echo "For Minecraft $mc: $(for l in "${jars[@]}"; do printf '`%s` ' "$id-$l-$mc-$version.jar"; done)"
  echo
  echo '```'; cat "$dist/SHA256SUMS.txt"; echo '```'
} > "$notes"

if [ "$fresh" = 1 ]; then
  pre=()
  case "$version" in *-alpha*|*-beta*|*-rc*) pre=(--prerelease) ;; esac
  target="${RELEASE_TARGET:-$(git rev-parse HEAD)}"
  if gh release create "$tag" "$dist"/* --title "$name $version" --notes-file "$notes" \
    --target "$target" --latest=false "${pre[@]}"; then
    echo "Released $tag"
    gh_status="[\`$tag\`]($release_url) (created)"
  else
    echo "::error::Failed to create GitHub release $tag"
    gh_status="Failed"
    append_summary
    exit 1
  fi
else
  gh_status="[\`$tag\`]($release_url) (existing)"
fi

changelog=$(cat "$RUNNER_TEMP/notes-$mod.md")
case "$version" in *-beta*) type=beta ;; *-alpha*) type=alpha ;; *) type=release ;; esac
agent="${GITHUB_REPOSITORY:-pirut/mc-mods} release workflow"
failed=0

modrinth() {
if [ -z "$project" ]; then
  echo "$mod has no modrinth_project_id: not on Modrinth."
  modrinth_status="Not configured"
  return 0
fi
if [ -z "${MODRINTH_TOKEN:-}" ]; then
  echo "::notice::No MODRINTH_TOKEN secret, so $tag was not uploaded to Modrinth."
  modrinth_status="Skipped (no token)"
  return 0
fi
api=https://api.modrinth.com/v2
# modrinth_required: comma-separated Modrinth project ids the mod needs on every loader.
deps_common=$(jq -nc --arg r "$required" '$r | split(",") | map(select(length > 0) | {project_id: ., dependency_type: "required"})')
local mr_uploaded=0 mr_skipped=0

upload() { # version-number title jar-file loaders-json dependencies-json
  status=$(curl -sS -o /dev/null -w '%{http_code}' -A "$agent" -H "Authorization: $MODRINTH_TOKEN" \
    "$api/project/$project/version/$1")
  if [ "$status" = 200 ]; then
    echo "$1 is already on Modrinth; nothing to upload."
    mr_skipped=$((mr_skipped + 1))
    return 0
  fi
  jq -n --arg project "$project" --arg name "$2" --arg number "$1" --arg changelog "$changelog" \
    --arg type "$type" --arg mc "$mc" --argjson loaders "$4" --argjson deps "$5" \
    '{project_id: $project, name: $name, version_number: $number, changelog: $changelog,
      version_type: $type, loaders: $loaders, game_versions: [$mc], dependencies: $deps,
      featured: false, status: "listed", file_parts: ["jar"], primary_file: "jar"}' > "$RUNNER_TEMP/modrinth.json"
  curl -sS --fail-with-body -A "$agent" -H "Authorization: $MODRINTH_TOKEN" \
    -F "data=<$RUNNER_TEMP/modrinth.json;type=application/json" \
    -F "jar=@$dist/$3;type=application/java-archive" \
    "$api/version" > "$RUNNER_TEMP/modrinth-response.json" \
    || { cat "$RUNNER_TEMP/modrinth-response.json"; echo; echo "::error::Modrinth upload of $1 failed."; modrinth_status="Failed"; return 1; }
  echo "Uploaded $1 to Modrinth (version id $(jq -r .id "$RUNNER_TEMP/modrinth-response.json"))"
  mr_uploaded=$((mr_uploaded + 1))
}

for loader in "${jars[@]}"; do
  jar="$id-$loader-$mc-$version.jar"
  if [ "$loader" = neoforge ]; then
    upload "$version" "$name $version" "$jar" '["neoforge"]' "$deps_common" || return 1
  else
    # Fabric API's Modrinth project id is P7dR8mSH.
    deps=$(jq -c '. + [{project_id: "P7dR8mSH", dependency_type: "required"}]' <<<"$deps_common")
    upload "$version+fabric" "$name $version for Fabric" "$jar" '["fabric", "quilt"]' "$deps" || return 1
  fi
done

if [ "$mr_uploaded" -gt 0 ]; then
  modrinth_status="[Uploaded](https://modrinth.com/project/$project)"
else
  modrinth_status="[Already published](https://modrinth.com/project/$project)"
fi
}

curseforge() {
if [ -z "$cf_project" ]; then
  echo "$mod has no curseforge_project_id: not on CurseForge."
  curseforge_status="Not configured"
  return 0
fi
if [ "$fresh" != 1 ] && [ "${CURSEFORGE_UPLOAD:-}" != 1 ]; then
  echo "$tag was released before, so it is not uploaded to CurseForge again (tick 'Upload to CurseForge too' to force it)."
  curseforge_status="Skipped (already released)"
  return 0
fi
if [ -z "${CURSEFORGE_TOKEN:-}" ]; then
  echo "::notice::No CURSEFORGE_TOKEN secret, so $tag was not uploaded to CurseForge."
  curseforge_status="Skipped (no token)"
  return 0
fi
local cf=https://minecraft.curseforge.com/api
get() { curl -sS --fail-with-body -A "$agent" -H "X-Api-Token: $CURSEFORGE_TOKEN" "$cf/$1"; }
get game/version-types > "$RUNNER_TEMP/cf-types.json" || { cat "$RUNNER_TEMP/cf-types.json"; echo "::error::CurseForge refused the token."; curseforge_status="Failed (auth)"; return 1; }
get game/versions > "$RUNNER_TEMP/cf-versions.json" || { cat "$RUNNER_TEMP/cf-versions.json"; echo "::error::Could not read CurseForge game versions."; curseforge_status="Failed (metadata)"; return 1; }
id_of() { # name type-slug-pattern: CurseForge's id for a game version, loader, Java or side tag
  jq -r --arg n "$1" --arg t "$2" --slurpfile types "$RUNNER_TEMP/cf-types.json" \
    '($types[0] | map(select(.slug | test($t))) | map(.id)) as $ids
     | map(select(.name == $n and ((.gameVersionTypeID as $g | $ids | index($g)) != null))) | .[0].id // empty' \
    "$RUNNER_TEMP/cf-versions.json"
}
local mcv neo fab quilt extras
mcv=$(id_of "$mc" "^minecraft-") ; neo=$(id_of NeoForge '^modloader$'); fab=$(id_of Fabric '^modloader$'); quilt=$(id_of Quilt '^modloader$')
[ -n "$mcv" ] && [ -n "$neo" ] && [ -n "$fab" ] || { echo "::error::CurseForge is missing the $mc, NeoForge or Fabric tag."; curseforge_status="Failed (tags)"; return 1; }
# Java and side tags only help players filter; left off if CurseForge renames them.
extras=$(for t in "Java 21:^java$" "Client:^environment$" "Server:^environment$"; do id_of "${t%%:*}" "${t#*:}"; done | jq -sc '.')
cf_upload() { # display-name jar game-version-ids-json relations-json
  jq -n --arg name "$1" --arg changelog "$changelog" --arg type "$type" \
    --argjson versions "$3" --argjson extras "$extras" --argjson relations "$4" \
    '{displayName: $name, changelog: $changelog, changelogType: "markdown", releaseType: $type,
      gameVersions: ($versions + $extras)}
      + (if ($relations | length) > 0 then {relations: {projects: $relations}} else {} end)' > "$RUNNER_TEMP/curseforge.json"
  if [ "${CURSEFORGE_DRY_RUN:-}" = 1 ]; then
    echo "Dry run, not uploaded: $2 with $(jq -c 'del(.changelog)' "$RUNNER_TEMP/curseforge.json")"; return 0
  fi
  curl -sS --fail-with-body -A "$agent" -H "X-Api-Token: $CURSEFORGE_TOKEN" \
    -F "metadata=<$RUNNER_TEMP/curseforge.json;type=application/json" \
    -F "file=@$dist/$2;type=application/java-archive" \
    "$cf/projects/$cf_project/upload-file" > "$RUNNER_TEMP/curseforge-response.json" \
    || { cat "$RUNNER_TEMP/curseforge-response.json"; echo; echo "::error::CurseForge upload of $2 failed."; curseforge_status="Failed"; return 1; }
  echo "Uploaded $2 to CurseForge as file $(jq -r .id "$RUNNER_TEMP/curseforge-response.json"); it goes live after CurseForge's review."
}
for loader in "${jars[@]}"; do
  jar="$id-$loader-$mc-$version.jar"
  if [ "$loader" = neoforge ]; then
    cf_upload "$name $version (NeoForge)" "$jar" "[$mcv, $neo]" '[]' || return 1
  else
    cf_upload "$name $version (Fabric)" "$jar" "$(jq -nc --argjson q "${quilt:-null}" "[$mcv, $fab, \$q] | map(select(. != null))")" \
      '[{"slug": "fabric-api", "type": "requiredDependency"}]' || return 1
  fi
done

if [ "${CURSEFORGE_DRY_RUN:-}" = 1 ]; then
  curseforge_status="Dry run"
else
  curseforge_status="Uploaded (in review)"
fi
}

modrinth || failed=1
curseforge || failed=1
append_summary
exit $failed
