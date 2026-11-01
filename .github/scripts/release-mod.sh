#!/usr/bin/env bash
# Releases one mod: .github/scripts/release-mod.sh <folder in mods/> [<folder with its jars>]
#
# Makes the GitHub release <folder>-v<mod_version> with the mod's jars (one per loader and Minecraft version) and
# the notes from its CHANGELOG.md, then uploads the jars to Modrinth when the mod has a modrinth_project_id and the
# MODRINTH_TOKEN secret is set. A version that is already released is left alone, so running this twice never
# releases twice. Expects the jars to be built already: the release workflow collects every Minecraft version's
# jars into one folder; without one, the jars in mods/<folder>/*/build/*/libs are used.
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
required=$(prop modrinth_required "$dir/gradle.properties")
tag="$mod-v$version"

if gh release view "$tag" >/dev/null 2>&1; then
  echo "$tag is already released; nothing to do."
  exit 0
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

pre=()
case "$version" in *-alpha*|*-beta*|*-rc*) pre=(--prerelease) ;; esac
gh release create "$tag" "$dist"/* --title "$name $version" --notes-file "$notes" \
  --target "${GITHUB_SHA:-HEAD}" --latest=false "${pre[@]}"
echo "Released $tag"

# Modrinth
if [ -z "$project" ]; then
  echo "$mod has no modrinth_project_id: GitHub release only."
  exit 0
fi
if [ -z "${MODRINTH_TOKEN:-}" ]; then
  echo "::notice::No MODRINTH_TOKEN secret, so $tag was not uploaded to Modrinth."
  exit 0
fi
api=https://api.modrinth.com/v2
agent="${GITHUB_REPOSITORY:-pirut/mc-mods} release workflow"
changelog=$(cat "$RUNNER_TEMP/notes-$mod.md")
case "$version" in *-beta*) type=beta ;; *-alpha*) type=alpha ;; *) type=release ;; esac
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
    || { cat "$RUNNER_TEMP/modrinth-response.json"; echo; echo "::error::Modrinth upload of $1 failed."; exit 1; }
  echo "Uploaded $1 to Modrinth (version id $(jq -r .id "$RUNNER_TEMP/modrinth-response.json"))"
}

# One Modrinth version per jar, numbered <version>+<minecraft>-<loader> (for example 0.6.0+1.21.1-neoforge). Its
# game versions are modrinth_game_versions (comma-separated) from the Minecraft version's properties, or just
# that Minecraft version.
for j in "${jars[@]}"; do
  read -r loader m jar <<<"$j"
  games=$(prop modrinth_game_versions "versions/$m.properties" "$dir/gradle.properties" "$dir/versions/$m.properties")
  [ -n "$games" ] || games=$m
  games_json=$(jq -nc --arg g "$games" '$g | split(",") | map(gsub("^ +| +$"; "")) | map(select(length > 0))')
  if [ "$loader" = neoforge ]; then
    upload "$version+$m-neoforge" "$name $version for NeoForge $m" "$jar" '["neoforge"]' "$deps_common" "$games_json"
  else
    # Fabric API's Modrinth project id is P7dR8mSH.
    deps=$(jq -c '. + [{project_id: "P7dR8mSH", dependency_type: "required"}]' <<<"$deps_common")
    upload "$version+$m-fabric" "$name $version for Fabric $m" "$jar" '["fabric", "quilt"]' "$deps" "$games_json"
  fi
done
