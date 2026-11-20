#!/usr/bin/env bash
# Releases one mod: .github/scripts/release-mod.sh <folder in mods/> [<folder with its jars>]
#
# Makes the GitHub release <folder>-v<mod_version> with the mod's jars (one per loader and Minecraft version)
# and the notes from its CHANGELOG.md, then uploads the jars to Modrinth (when the mod has a
# modrinth_project_id and the MODRINTH_TOKEN secret is set) and to CurseForge (when it has a
# curseforge_project_id and the CURSEFORGE_TOKEN secret is set).
# A version that is already on GitHub is not released again, but the store uploads still run: Modrinth skips a
# version it already has. CurseForge can't be asked that, so it only gets the files when the GitHub release was
# made in this run, or when CURSEFORGE_UPLOAD=1 (the "Upload to CurseForge too" box when run by hand).
# Expects the jars to be built already: the release workflow collects every Minecraft version's jars into one
# folder; without one, the jars in mods/<folder>/*/build/*/libs are used.
#
# Modrinth version numbers stay <version> and <version>+fabric when a mod ships one Minecraft version, matching
# versions already on the store. A mod that ships several Minecraft versions gets one Modrinth version per jar,
# numbered <version>+<minecraft>-<loader>.
set -euo pipefail

mod=$1
jars_dir=${2:-}
dir="mods/$mod"
[ -f "$dir/gradle.properties" ] || { echo "::error::No mod at $dir"; exit 1; }

prop() { # key [file...]: the last value wins, like Gradle's mod-over-root order
  local key=$1; shift
  grep -h "^$key=" "$@" 2>/dev/null | tail -n1 | cut -d= -f2- || true
}
id=$(prop mod_id "$dir/gradle.properties")
name=$(prop mod_name "$dir/gradle.properties")
version=$(prop mod_version "$dir/gradle.properties")
project=$(prop modrinth_project_id "$dir/gradle.properties")
cf_project=$(prop curseforge_project_id "$dir/gradle.properties")
required=$(prop modrinth_required "$dir/gradle.properties")
tag="$mod-v$version"

fresh=1
if gh release view "$tag" >/dev/null 2>&1; then
  echo "$tag is already on GitHub; checking the stores."
  fresh=0
fi

# This version's section of the mod's CHANGELOG.md
awk -v v="$version" '$0 == "## " v {on=1; next} /^## / {on=0} on' "$dir/CHANGELOG.md" > "$RUNNER_TEMP/notes-$mod.md"
if [ ! -s "$RUNNER_TEMP/notes-$mod.md" ]; then
  echo "::error::$dir/CHANGELOG.md has no '## $version' section"
  exit 1
fi

dist="$RUNNER_TEMP/dist-$mod"
rm -rf "$dist" && mkdir -p "$dist"
if [ -n "$jars_dir" ]; then
  candidates=("$jars_dir"/*.jar)
else
  candidates=("$dir"/*/build/*/libs/*.jar)
fi
# Jars are named <mod_id>-<loader>-<minecraft>-<version>.jar.
jars=() # "loader minecraft file" each
for jar in "${candidates[@]}"; do
  [ -f "$jar" ] || continue
  base=$(basename "$jar")
  case "$base" in *-sources.jar) continue ;; "$id-"*"-$version.jar") ;; *) continue ;; esac
  rest=${base#"$id-"}; rest=${rest%"-$version.jar"}
  loader=${rest%%-*}; mc=${rest#*-}
  case "$loader" in neoforge|fabric) ;; *) continue ;; esac
  cp "$jar" "$dist/" && jars+=("$loader $mc $base")
done
[ ${#jars[@]} -gt 0 ] || { echo "::error::No jars for $mod $version; build it first"; exit 1; }
( cd "$dist" && sha256sum *.jar > SHA256SUMS.txt )
mcs=$(for j in "${jars[@]}"; do echo "${j#* }" | cut -d' ' -f1; done | sort -uV)
mc_count=$(echo "$mcs" | wc -w)

notes="$RUNNER_TEMP/release-notes-$mod.md"
{
  cat "$RUNNER_TEMP/notes-$mod.md"
  echo
  for m in $mcs; do
    printf -- '- Minecraft %s: ' "$m"
    for j in "${jars[@]}"; do read -r l jm f <<<"$j"; [ "$jm" = "$m" ] && printf '`%s` ' "$f"; done
    echo
  done
  echo
  echo '```'; cat "$dist/SHA256SUMS.txt"; echo '```'
} > "$notes"

if [ "$fresh" = 1 ]; then
  pre=()
  case "$version" in *-alpha*|*-beta*|*-rc*) pre=(--prerelease) ;; esac
  gh release create "$tag" "$dist"/* --title "$name $version" --notes-file "$notes" \
    --target "${GITHUB_SHA:-HEAD}" --latest=false "${pre[@]}"
  echo "Released $tag"
fi

changelog=$(cat "$RUNNER_TEMP/notes-$mod.md")
case "$version" in *-beta*) type=beta ;; *-alpha*) type=alpha ;; *) type=release ;; esac
agent="${GITHUB_REPOSITORY:-pirut/mc-mods} release workflow"
failed=0

modrinth() {
if [ -z "$project" ]; then
  echo "$mod has no modrinth_project_id: not on Modrinth."
  return 0
fi
if [ -z "${MODRINTH_TOKEN:-}" ]; then
  echo "::notice::No MODRINTH_TOKEN secret, so $tag was not uploaded to Modrinth."
  return 0
fi
api=https://api.modrinth.com/v2
# modrinth_required: comma-separated Modrinth project ids the mod needs on every loader.
deps_common=$(jq -nc --arg r "$required" '$r | split(",") | map(select(length > 0) | {project_id: ., dependency_type: "required"})')

upload() { # version-number title jar-file loaders-json dependencies-json game-versions-json
  status=$(curl -sS -o /dev/null -w '%{http_code}' -A "$agent" -H "Authorization: $MODRINTH_TOKEN" \
    "$api/project/$project/version/$1")
  if [ "$status" = 200 ]; then echo "$1 is already on Modrinth; nothing to upload."; return 0; fi
  jq -n --arg project "$project" --arg name "$2" --arg number "$1" --arg changelog "$changelog" \
    --arg type "$type" --argjson mc "$6" --argjson loaders "$4" --argjson deps "$5" \
    '{project_id: $project, name: $name, version_number: $number, changelog: $changelog,
      version_type: $type, loaders: $loaders, game_versions: $mc, dependencies: $deps,
      featured: false, status: "listed", file_parts: ["jar"], primary_file: "jar"}' > "$RUNNER_TEMP/modrinth.json"
  curl -sS --fail-with-body -A "$agent" -H "Authorization: $MODRINTH_TOKEN" \
    -F "data=<$RUNNER_TEMP/modrinth.json;type=application/json" \
    -F "jar=@$dist/$3;type=application/java-archive" \
    "$api/version" > "$RUNNER_TEMP/modrinth-response.json" \
    || { cat "$RUNNER_TEMP/modrinth-response.json"; echo; echo "::error::Modrinth upload of $1 failed."; return 1; }
  echo "Uploaded $1 to Modrinth (version id $(jq -r .id "$RUNNER_TEMP/modrinth-response.json"))"
}

# One Minecraft version keeps the historical Modrinth numbers (<version>, <version>+fabric) so a store retry
# does not publish a second copy. Several Minecraft versions need one number per jar.
for j in "${jars[@]}"; do
  read -r loader m jar <<<"$j"
  games=$(prop modrinth_game_versions "versions/$m.properties" "$dir/gradle.properties" "$dir/versions/$m.properties")
  [ -n "$games" ] || games=$m
  games_json=$(jq -nc --arg g "$games" '$g | split(",") | map(gsub("^ +| +$"; "")) | map(select(length > 0))')
  if [ "$mc_count" -eq 1 ]; then
    if [ "$loader" = neoforge ]; then
      number=$version
      title="$name $version"
    else
      number="$version+fabric"
      title="$name $version for Fabric"
    fi
  else
    number="$version+$m-$loader"
    if [ "$loader" = neoforge ]; then
      title="$name $version for NeoForge $m"
    else
      title="$name $version for Fabric $m"
    fi
  fi
  if [ "$loader" = neoforge ]; then
    upload "$number" "$title" "$jar" '["neoforge"]' "$deps_common" "$games_json" || return 1
  else
    # Fabric API's Modrinth project id is P7dR8mSH.
    deps=$(jq -c '. + [{project_id: "P7dR8mSH", dependency_type: "required"}]' <<<"$deps_common")
    upload "$number" "$title" "$jar" '["fabric", "quilt"]' "$deps" "$games_json" || return 1
  fi
done
}

curseforge() {
if [ -z "$cf_project" ]; then
  echo "$mod has no curseforge_project_id: not on CurseForge."
  return 0
fi
if [ "$fresh" != 1 ] && [ "${CURSEFORGE_UPLOAD:-}" != 1 ]; then
  echo "$tag was released before, so it is not uploaded to CurseForge again (tick 'Upload to CurseForge too' to force it)."
  return 0
fi
if [ -z "${CURSEFORGE_TOKEN:-}" ]; then
  echo "::notice::No CURSEFORGE_TOKEN secret, so $tag was not uploaded to CurseForge."
  return 0
fi
local cf=https://minecraft.curseforge.com/api
get() { curl -sS --fail-with-body -A "$agent" -H "X-Api-Token: $CURSEFORGE_TOKEN" "$cf/$1"; }
get game/version-types > "$RUNNER_TEMP/cf-types.json" || { cat "$RUNNER_TEMP/cf-types.json"; echo "::error::CurseForge refused the token."; return 1; }
get game/versions > "$RUNNER_TEMP/cf-versions.json" || { cat "$RUNNER_TEMP/cf-versions.json"; echo "::error::Could not read CurseForge game versions."; return 1; }
id_of() { # name type-slug-pattern: CurseForge's id for a game version, loader, Java or side tag
  jq -r --arg n "$1" --arg t "$2" --slurpfile types "$RUNNER_TEMP/cf-types.json" \
    '($types[0] | map(select(.slug | test($t))) | map(.id)) as $ids
     | map(select(.name == $n and ((.gameVersionTypeID as $g | $ids | index($g)) != null))) | .[0].id // empty' \
    "$RUNNER_TEMP/cf-versions.json"
}
local neo fab quilt client server extras
neo=$(id_of NeoForge '^modloader$'); fab=$(id_of Fabric '^modloader$'); quilt=$(id_of Quilt '^modloader$')
client=$(id_of Client '^environment$'); server=$(id_of Server '^environment$')
[ -n "$neo" ] && [ -n "$fab" ] || { echo "::error::CurseForge is missing the NeoForge or Fabric tag."; return 1; }
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
    || { cat "$RUNNER_TEMP/curseforge-response.json"; echo; echo "::error::CurseForge upload of $2 failed."; return 1; }
  echo "Uploaded $2 to CurseForge as file $(jq -r .id "$RUNNER_TEMP/curseforge-response.json"); it goes live after CurseForge's review."
}
for j in "${jars[@]}"; do
  read -r loader m jar <<<"$j"
  mcv=$(id_of "$m" "^minecraft-")
  [ -n "$mcv" ] || { echo "::error::CurseForge is missing the $m tag."; return 1; }
  java_name=$(prop java_version "versions/$m.properties" "$dir/versions/$m.properties")
  case "$java_name" in
    ''|21) java_name='Java 21' ;;
    25) java_name='Java 25' ;;
    Java\ *) ;;
    *) java_name="Java $java_name" ;;
  esac
  java_id=$(id_of "$java_name" '^java$')
  # Java and side tags only help players filter; left off if CurseForge renames them.
  extras=$(jq -nc --argjson j "${java_id:-null}" --argjson c "${client:-null}" --argjson s "${server:-null}" \
    '[$j, $c, $s] | map(select(. != null))')
  if [ "$mc_count" -eq 1 ]; then
    neo_title="$name $version (NeoForge)"
    fab_title="$name $version (Fabric)"
  else
    neo_title="$name $version for Minecraft $m (NeoForge)"
    fab_title="$name $version for Minecraft $m (Fabric)"
  fi
  if [ "$loader" = neoforge ]; then
    cf_upload "$neo_title" "$jar" "[$mcv, $neo]" '[]' || return 1
  else
    cf_upload "$fab_title" "$jar" "$(jq -nc --argjson q "${quilt:-null}" "[$mcv, $fab, \$q] | map(select(. != null))")" \
      '[{"slug": "fabric-api", "type": "requiredDependency"}]' || return 1
  fi
done
}

modrinth || failed=1
curseforge || failed=1
exit $failed
