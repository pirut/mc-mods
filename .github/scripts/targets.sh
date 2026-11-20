#!/usr/bin/env bash
# Prints the Minecraft versions the given mods are built for, as a JSON array (for a workflow matrix):
#   .github/scripts/targets.sh [<folder in mods/>...]
# With no mods: every mod in mods/ and the template. A mod's versions are minecraft_targets in its
# gradle.properties (comma-separated), or the repo's mc_default.
#
#   .github/scripts/targets.sh --mods <minecraft> <folder>...
# Prints which of the given mods are built for that Minecraft version, space-separated.
set -euo pipefail
cd "$(dirname "$0")/../.."

default=$(grep -h '^mc_default=' gradle.properties | cut -d= -f2-)
targets_of() { # dir
  local t
  t=$(grep -h '^minecraft_targets=' "$1/gradle.properties" 2>/dev/null | tail -n1 | cut -d= -f2- || true)
  [ -n "$t" ] || t=$default
  tr ',' '\n' <<<"$t" | sed 's/^ *//; s/ *$//' | grep -v '^$'
}

if [ "${1:-}" = --mods ]; then
  mc=$2; shift 2
  out=()
  for m in "$@"; do targets_of "mods/$m" | grep -qxF "$mc" && out+=("$m"); done
  echo "${out[*]:-}"
  exit 0
fi

dirs=()
if [ $# -gt 0 ]; then for m in "$@"; do dirs+=("mods/$m"); done
else for d in mods/*/ template/; do [ -f "$d/gradle.properties" ] && dirs+=("${d%/}"); done
fi
for d in "${dirs[@]}"; do targets_of "$d"; done | sort -uV | jq -Rnc '[inputs]'
