#!/usr/bin/env bash
# Offline unit test suite for bump-version.sh and prepare-release.sh.
# Usage: .github/scripts/test-release-scripts.sh
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
bump_sh="$script_dir/bump-version.sh"
prep_sh="$script_dir/prepare-release.sh"

passed=0
failed=0

assert_eq() {
  local label=$1 expected=$2 actual=$3
  if [ "$expected" = "$actual" ]; then
    passed=$((passed + 1))
  else
    failed=$((failed + 1))
    printf 'FAIL [%s]\n  expected: %q\n  actual:   %q\n' "$label" "$expected" "$actual" >&2
  fi
}

assert_bump() {
  local label=$1 tags=$2 mod=$3 cur=$4 mode=$5 expected=$6
  local actual
  actual="$(EXISTING_TAGS="$tags" "$bump_sh" "$mod" "$cur" "$mode")"
  assert_eq "bump: $label" "$expected" "$actual"
}

assert_bump_fails() {
  local label=$1 mod=$2 cur=$3 mode=$4
  if EXISTING_TAGS="" "$bump_sh" "$mod" "$cur" "$mode" >/dev/null 2>&1; then
    failed=$((failed + 1))
    printf 'FAIL [bump fails: %s] expected non-zero exit for (%s, %s, %s)\n' "$label" "$mod" "$cur" "$mode" >&2
  else
    passed=$((passed + 1))
  fi
}

# ==============================================================================
# bump-version.sh tests
# ==============================================================================

# mode: none
assert_bump "none stable" "cc-agent-v0.5.2" cc-agent 0.5.2 none 0.5.2
assert_bump "none prerelease" "pirutlib-v0.1.0-alpha.3" pirutlib 0.1.0-alpha.3 none 0.1.0-alpha.3

# mode: auto (unreleased keeps current version)
assert_bump "auto unreleased stable" "cc-agent-v0.5.2" cc-agent 0.5.3 auto 0.5.3
assert_bump "auto unreleased alpha" "pirutlib-v0.1.0-alpha.3" pirutlib 0.1.0-alpha.4 auto 0.1.0-alpha.4
assert_bump "auto unreleased empty tags" "" chunks3 0.1.0-alpha.1 auto 0.1.0-alpha.1

# mode: auto (already released increments next)
assert_bump "auto released stable" "cc-agent-v0.5.2" cc-agent 0.5.2 auto 0.5.3
assert_bump "auto released alpha" "pirutlib-v0.1.0-alpha.3" pirutlib 0.1.0-alpha.3 auto 0.1.0-alpha.4
assert_bump "auto released beta" "cc-agent-v0.5.3-beta.1" cc-agent 0.5.3-beta.1 auto 0.5.3-beta.2
assert_bump "auto released rc" "bloodfeud-v1.0.0-rc.1" bloodfeud 1.0.0-rc.1 auto 1.0.0-rc.2

# mode: patch (graduates prerelease, increments stable)
assert_bump "patch stable" "" cc-agent 0.5.2 patch 0.5.3
assert_bump "patch graduate alpha" "" pirutlib 0.1.0-alpha.3 patch 0.1.0
assert_bump "patch graduate beta" "" cc-agent 0.5.3-beta.1 patch 0.5.3
assert_bump "patch graduate rc" "" bloodfeud 1.2.3-rc.2 patch 1.2.3

# mode: minor
assert_bump "minor stable" "" cc-agent 0.5.2 minor 0.6.0
assert_bump "minor prerelease" "" pirutlib 0.1.0-alpha.3 minor 0.2.0

# mode: major
assert_bump "major stable" "" cc-agent 0.5.2 major 1.0.0
assert_bump "major prerelease" "" bloodfeud 1.2.3-beta.4 major 2.0.0

# mode: alpha
assert_bump "alpha from alpha" "" pirutlib 0.1.0-alpha.3 alpha 0.1.0-alpha.4
assert_bump "alpha from beta" "" cc-agent 0.5.3-beta.1 alpha 0.5.4-alpha.1
assert_bump "alpha from rc" "" cc-agent 0.5.3-rc.2 alpha 0.5.4-alpha.1
assert_bump "alpha from stable" "" cc-agent 0.5.2 alpha 0.5.3-alpha.1

# mode: beta
assert_bump "beta from alpha" "" pirutlib 0.1.0-alpha.3 beta 0.1.0-beta.1
assert_bump "beta from beta" "" cc-agent 0.5.3-beta.1 beta 0.5.3-beta.2
assert_bump "beta from rc" "" cc-agent 0.5.3-rc.1 beta 0.5.4-beta.1
assert_bump "beta from stable" "" cc-agent 0.5.2 beta 0.5.3-beta.1

# collision avoidance
assert_bump "cc-agent 0.5.2 beta collides with 0.5.3-beta.1" \
  $'cc-agent-v0.5.1\ncc-agent-v0.5.2\ncc-agent-v0.5.3-beta.1' \
  cc-agent 0.5.2 beta 0.5.3-beta.2

assert_bump "multi-step prerelease collision" \
  $'cc-agent-v0.5.3-beta.1\ncc-agent-v0.5.3-beta.2\ncc-agent-v0.5.3-beta.3' \
  cc-agent 0.5.2 beta 0.5.3-beta.4

assert_bump "multi-step stable collision on patch" \
  $'cc-agent-v0.5.2\ncc-agent-v0.5.3\ncc-agent-v0.5.4' \
  cc-agent 0.5.2 patch 0.5.5

assert_bump "multi-step stable collision on auto" \
  $'cc-agent-v0.5.2\ncc-agent-v0.5.3\ncc-agent-v0.5.4' \
  cc-agent 0.5.2 auto 0.5.5

assert_bump "minor collision steps patch" \
  $'bloodfeud-v0.2.0\nbloodfeud-v0.2.1' \
  bloodfeud 0.1.5 minor 0.2.2

assert_bump "other mod tags do not collide" \
  $'other-v0.5.3\ncc-agent-v0.5.3-beta.1' \
  cc-agent 0.5.2 patch 0.5.3

# invalid versions & modes
assert_bump_fails "missing patch" cc-agent 1.2 patch
assert_bump_fails "leading v" cc-agent v1.2.3 patch
assert_bump_fails "unknown prerelease tag" cc-agent 1.2.3-dev.1 patch
assert_bump_fails "prerelease without number" cc-agent 1.2.3-alpha patch
assert_bump_fails "garbage version" cc-agent not-a-version auto
assert_bump_fails "invalid mode" cc-agent 1.2.3 invalid

# ==============================================================================
# prepare-release.sh tests
# ==============================================================================

tmp_root="$(mktemp -d)"
trap 'rm -rf "$tmp_root"' EXIT

# Test 1: Updates gradle.properties cleanly & inserts fallback changelog from PR title/number
t1="$tmp_root/t1"
mkdir -p "$t1/mods/demo"
cat <<'EOP' > "$t1/mods/demo/gradle.properties"
# Header comment
mod_id=demo
mod_name=Demo Mod
mod_version=0.5.2
mod_description=Keep this line=with=equals
EOP
cat <<'EOC' > "$t1/mods/demo/CHANGELOG.md"
# Changelog

## 0.5.2

- Old change.
EOC

(
  cd "$t1"
  PR_TITLE="Fix crash on startup" PR_NUMBER="42" PR_BODY="Just a normal description." \
    "$prep_sh" demo 0.5.3
)

assert_eq "t1 gradle.properties updated" \
  "$(printf '# Header comment\nmod_id=demo\nmod_name=Demo Mod\nmod_version=0.5.3\nmod_description=Keep this line=with=equals')" \
  "$(cat "$t1/mods/demo/gradle.properties")"

assert_eq "t1 changelog inserted after # Changelog" \
  "$(printf '# Changelog\n\n## 0.5.3\n\n- Fix crash on startup (#42)\n\n## 0.5.2\n\n- Old change.')" \
  "$(cat "$t1/mods/demo/CHANGELOG.md")"

# Verify release-mod.sh awk extraction finds the new section
extracted_t1="$(awk -v v="0.5.3" '$0 == "## " v {on=1; next} /^## / {on=0} on' "$t1/mods/demo/CHANGELOG.md" | sed '/^$/d')"
assert_eq "t1 release-mod awk extraction" "- Fix crash on startup (#42)" "$extracted_t1"

# Test 2: Preserves existing non-empty handcrafted ## <version> section
t2="$tmp_root/t2"
mkdir -p "$t2/mods/demo"
printf 'mod_id=demo\nmod_version=0.5.2\n' > "$t2/mods/demo/gradle.properties"
cat <<'EOC' > "$t2/mods/demo/CHANGELOG.md"
# Changelog

## 0.5.3

- Handcrafted release note with details.

## 0.5.2

- Old note.
EOC
before_t2="$(cat "$t2/mods/demo/CHANGELOG.md")"
(
  cd "$t2"
  PR_TITLE="Should be ignored" PR_NUMBER="99" PR_BODY=$'## Release Notes\n\n- Also ignored' \
    "$prep_sh" demo 0.5.3
)
assert_eq "t2 preserves handcrafted changelog" "$before_t2" "$(cat "$t2/mods/demo/CHANGELOG.md")"
assert_eq "t2 still updates mod_version" "mod_version=0.5.3" "$(grep '^mod_version=' "$t2/mods/demo/gradle.properties")"

# Test 3: Extracts ## Release Notes (case-insensitive) up to next ## heading, preserving ### subheadings & code fences
t3="$tmp_root/t3"
mkdir -p "$t3/mods/demo"
printf 'mod_id=demo\nmod_version=0.1.0\n' > "$t3/mods/demo/gradle.properties"
printf '# Changelog\n\n## 0.1.0\n\n- Initial.\n' > "$t3/mods/demo/CHANGELOG.md"
pr_body_t3=$(cat <<'EOB'
Summary of the PR.

## release notes

- Added feature A
- Fixed bug B

### Details

```sh
## not a heading inside fence
echo "$HOME"
```

## Testing

- Ran unit tests
EOB
)
(
  cd "$t3"
  PR_TITLE="Fallback title" PR_NUMBER="7" PR_BODY="$pr_body_t3" \
    "$prep_sh" demo 0.2.0
)
expected_t3=$(cat <<'EOC'
# Changelog

## 0.2.0

- Added feature A
- Fixed bug B

### Details

```sh
## not a heading inside fence
echo "$HOME"
```

## 0.1.0

- Initial.
EOC
)
assert_eq "t3 extracts ## Release Notes cleanly" "$expected_t3" "$(cat "$t3/mods/demo/CHANGELOG.md")"

# Test 4: Extracts ### Release Notes up to next ### or ## heading
t4="$tmp_root/t4"
mkdir -p "$t4/mods/demo"
printf 'mod_id=demo\nmod_version=0.1.0\n' > "$t4/mods/demo/gradle.properties"
printf '# Changelog\n\n## 0.1.0\n\n- Initial.\n' > "$t4/mods/demo/CHANGELOG.md"
pr_body_t4=$(cat <<'EOB'
## Context
Some context.

### Release Notes
- Bullet one
- Bullet two

### Checklist
- [x] Tested
EOB
)
(
  cd "$t4"
  PR_TITLE="Fallback" PR_NUMBER="8" PR_BODY="$pr_body_t4" \
    "$prep_sh" demo 0.1.1
)
expected_t4=$(cat <<'EOC'
# Changelog

## 0.1.1

- Bullet one
- Bullet two

## 0.1.0

- Initial.
EOC
)
assert_eq "t4 extracts ### Release Notes up to next ###" "$expected_t4" "$(cat "$t4/mods/demo/CHANGELOG.md")"

# Test 5: Special characters ($, &, \, backticks, single/double quotes) in PR title and body
t5="$tmp_root/t5"
mkdir -p "$t5/mods/demo"
printf 'mod_id=demo\nmod_version=1.0.0\n' > "$t5/mods/demo/gradle.properties"
printf '# Changelog\n\n## 1.0.0\n\n- Base.\n' > "$t5/mods/demo/CHANGELOG.md"
special_title='Fix $HOME & `cmd` \1 \\ "double" '\''single'\'' $(echo nope)'
(
  cd "$t5"
  PR_TITLE="$special_title" PR_NUMBER="123" PR_BODY="" \
    "$prep_sh" demo 1.0.1
)
expected_t5_line="- ${special_title} (#123)"
actual_t5_line="$(awk -v v="1.0.1" '$0 == "## " v {on=1; next} /^## / {on=0} on' "$t5/mods/demo/CHANGELOG.md" | sed '/^$/d')"
assert_eq "t5 special chars in PR title preserved verbatim" "$expected_t5_line" "$actual_t5_line"

t5b="$tmp_root/t5b"
mkdir -p "$t5b/mods/demo"
printf 'mod_id=demo\nmod_version=1.0.0\n' > "$t5b/mods/demo/gradle.properties"
printf '# Changelog\n\n## 1.0.0\n\n- Base.\n' > "$t5b/mods/demo/CHANGELOG.md"
special_body=$(printf '## Release Notes\n\n- Keep $PATH, &amp;, \\n, \\1, `code`, "quotes", '\''ticks'\'', $(whoami)\n')
(
  cd "$t5b"
  PR_TITLE="ignored" PR_NUMBER="124" PR_BODY="$special_body" \
    "$prep_sh" demo 1.0.1
)
expected_t5b_line='- Keep $PATH, &amp;, \n, \1, `code`, "quotes", '\''ticks'\'', $(whoami)'
actual_t5b_line="$(awk -v v="1.0.1" '$0 == "## " v {on=1; next} /^## / {on=0} on' "$t5b/mods/demo/CHANGELOG.md" | sed '/^$/d')"
assert_eq "t5b special chars in PR body preserved verbatim" "$expected_t5b_line" "$actual_t5b_line"

# Test 6: Creates missing CHANGELOG.md with # Changelog and fallback when PR vars unset
t6="$tmp_root/t6"
mkdir -p "$t6/mods/demo"
printf 'mod_id=demo\nmod_version=0.1.0\n' > "$t6/mods/demo/gradle.properties"
(
  cd "$t6"
  unset PR_TITLE PR_NUMBER PR_BODY
  "$prep_sh" demo 0.1.1
)
expected_t6=$(printf '# Changelog\n\n## 0.1.1\n\n- Release 0.1.1')
assert_eq "t6 creates missing CHANGELOG.md" "$expected_t6" "$(cat "$t6/mods/demo/CHANGELOG.md")"

# Test 7: Preserves CRLF line endings in gradle.properties
t7="$tmp_root/t7"
mkdir -p "$t7/mods/demo"
printf 'mod_id=demo\r\nmod_version=0.1.0\r\nmod_name=Demo\r\n' > "$t7/mods/demo/gradle.properties"
(
  cd "$t7"
  unset PR_TITLE PR_NUMBER PR_BODY
  "$prep_sh" demo 0.1.1
)
crlf_check="$(python3 -c 'import sys; d=open(sys.argv[1],"rb").read(); print("OK" if d==b"mod_id=demo\r\nmod_version=0.1.1\r\nmod_name=Demo\r\n" else repr(d))' "$t7/mods/demo/gradle.properties")"
assert_eq "t7 preserves CRLF line endings" "OK" "$crlf_check"

echo "Passed: $passed, Failed: $failed"
[ "$failed" -eq 0 ]
