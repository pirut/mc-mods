#!/usr/bin/env bash
# Idempotently creates or updates mod:* and release* labels on GitHub.
# Usage: .github/scripts/sync-labels.sh
set -euo pipefail

repo="${GITHUB_REPOSITORY:-pirut/mc-mods}"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/../.." && pwd)"

sync_label() {
  local name=$1 color=$2 desc=$3
  gh label create "$name" --repo "$repo" --color "$color" --description "$desc" --force
}

for dir in "$repo_root"/mods/*/; do
  [ -f "$dir/gradle.properties" ] || continue
  folder="$(basename "$dir")"
  mod_name="$(grep -m1 '^mod_name=' "$dir/gradle.properties" | cut -d= -f2- | tr -d '\r' || true)"
  if [ -z "$mod_name" ]; then
    mod_name="$folder"
  fi
  sync_label "mod:$folder" "1d76db" "Changes to $mod_name (mods/$folder)"
done

sync_label "release"       "0e8a16" "Ship a new build on merge (uses bumped version, or auto-bumps next)"
sync_label "release:patch" "2ea44f" "Ship a patch release on merge (0.5.2 -> 0.5.3, or graduate prerelease)"
sync_label "release:minor" "fbca04" "Ship a minor release on merge (0.1.x -> 0.2.0)"
sync_label "release:major" "d93f0b" "Ship a major release on merge (0.x.y -> 1.0.0)"
sync_label "release:alpha" "c5def5" "Ship an alpha prerelease on merge (next -alpha.N)"
sync_label "release:beta"  "bfd4f2" "Ship a beta prerelease on merge (next -beta.N)"
