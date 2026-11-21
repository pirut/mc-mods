#!/usr/bin/env bash
# Computes the next release version for a mod with tag/release collision avoidance.
# Usage: .github/scripts/bump-version.sh <mod> <current_version> <mode>
# Modes: none | auto | patch | minor | major | alpha | beta
set -euo pipefail

if [ "$#" -ne 3 ]; then
  echo "Usage: $0 <mod> <current_version> <mode>" >&2
  exit 2
fi

mod=$1
current=$2
mode=$3

semver_re='^([0-9]+)\.([0-9]+)\.([0-9]+)(-(alpha|beta|rc)\.([0-9]+))?$'
if [[ ! "$current" =~ $semver_re ]]; then
  echo "Error: invalid version '$current' (expected X.Y.Z or X.Y.Z-(alpha|beta|rc).N)" >&2
  exit 1
fi

major=$((10#${BASH_REMATCH[1]}))
minor=$((10#${BASH_REMATCH[2]}))
patch=$((10#${BASH_REMATCH[3]}))
pre_type="${BASH_REMATCH[5]:-}"
pre_num="${BASH_REMATCH[6]:-}"

case "$mode" in
  none|auto|patch|minor|major|alpha|beta) ;;
  *)
    echo "Error: invalid bump mode '$mode' (expected none|auto|patch|minor|major|alpha|beta)" >&2
    exit 1
    ;;
esac

if [ "$mode" = "none" ]; then
  printf '%s\n' "$current"
  exit 0
fi

if [ "${EXISTING_TAGS+x}" = "x" ]; then
  existing_tags="$EXISTING_TAGS"
else
  existing_tags=$(
    {
      git tag -l "${mod}-v*" 2>/dev/null || true
      if command -v gh >/dev/null 2>&1; then
        gh release list --repo "${GITHUB_REPOSITORY:-pirut/mc-mods}" --limit 200 --json tagName --jq '.[].tagName' 2>/dev/null || true
      fi
    } | awk 'NF' | sort -u
  )
fi

tag_exists() {
  local tag="${mod}-v$1"
  [ -n "$existing_tags" ] && printf '%s\n' "$existing_tags" | grep -Fxq -- "$tag"
}

format_version() {
  if [ -n "$cand_pre_type" ]; then
    printf '%d.%d.%d-%s.%d' "$cand_major" "$cand_minor" "$cand_patch" "$cand_pre_type" "$cand_pre_num"
  else
    printf '%d.%d.%d' "$cand_major" "$cand_minor" "$cand_patch"
  fi
}

cand_major=$major
cand_minor=$minor
cand_patch=$patch
cand_pre_type=$pre_type
cand_pre_num=${pre_num:+ $((10#$pre_num))}
cand_pre_num=${cand_pre_num# }

case "$mode" in
  auto)
    if ! tag_exists "$current"; then
      printf '%s\n' "$current"
      exit 0
    fi
    if [ -n "$pre_type" ]; then
      cand_pre_num=$((10#$pre_num + 1))
    else
      cand_patch=$((patch + 1))
    fi
    ;;
  patch)
    if [ -n "$pre_type" ]; then
      cand_pre_type=""
      cand_pre_num=""
    else
      cand_patch=$((patch + 1))
    fi
    ;;
  minor)
    cand_minor=$((minor + 1))
    cand_patch=0
    cand_pre_type=""
    cand_pre_num=""
    ;;
  major)
    cand_major=$((major + 1))
    cand_minor=0
    cand_patch=0
    cand_pre_type=""
    cand_pre_num=""
    ;;
  alpha)
    if [ "$pre_type" = "alpha" ]; then
      cand_pre_num=$((10#$pre_num + 1))
    else
      cand_patch=$((patch + 1))
      cand_pre_type="alpha"
      cand_pre_num=1
    fi
    ;;
  beta)
    if [ "$pre_type" = "alpha" ]; then
      cand_pre_type="beta"
      cand_pre_num=1
    elif [ "$pre_type" = "beta" ]; then
      cand_pre_num=$((10#$pre_num + 1))
    else
      cand_patch=$((patch + 1))
      cand_pre_type="beta"
      cand_pre_num=1
    fi
    ;;
esac

candidate="$(format_version)"
while tag_exists "$candidate"; do
  if [ -n "$cand_pre_type" ]; then
    cand_pre_num=$((cand_pre_num + 1))
  else
    cand_patch=$((cand_patch + 1))
  fi
  candidate="$(format_version)"
done

printf '%s\n' "$candidate"
