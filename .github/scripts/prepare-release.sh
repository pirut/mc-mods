#!/usr/bin/env bash
# Updates mods/<mod>/gradle.properties to <target_version> and ensures mods/<mod>/CHANGELOG.md
# has a non-empty "## <target_version>" section.
# Usage: .github/scripts/prepare-release.sh <mod> <target_version>
set -euo pipefail

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <mod> <target_version>" >&2
  exit 2
fi

mod=$1
target_version=$2

if [ ! -f "mods/$mod/gradle.properties" ]; then
  echo "Error: mods/$mod/gradle.properties not found" >&2
  exit 1
fi

python3 - "$mod" "$target_version" <<'PY'
import os
import re
import sys

mod = sys.argv[1]
target_version = sys.argv[2]

props_path = os.path.join("mods", mod, "gradle.properties")
changelog_path = os.path.join("mods", mod, "CHANGELOG.md")


def update_gradle_properties(path: str, version: str) -> None:
    with open(path, "r", encoding="utf-8", newline="") as f:
        lines = f.readlines()

    found = False
    changed = False
    new_lines = []
    for line in lines:
        if line.startswith("mod_version="):
            found = True
            if line.endswith("\r\n"):
                eol = "\r\n"
            elif line.endswith("\n"):
                eol = "\n"
            else:
                eol = ""
            replacement = f"mod_version={version}{eol}"
            if line != replacement:
                changed = True
            new_lines.append(replacement)
        else:
            new_lines.append(line)

    if not found:
        sys.stderr.write(f"Error: mod_version= not found in {path}\n")
        sys.exit(1)

    if changed:
        with open(path, "w", encoding="utf-8", newline="") as f:
            f.writelines(new_lines)


def extract_pr_release_notes(body: str) -> str:
    if not body:
        return ""
    lines = body.replace("\r\n", "\n").replace("\r", "\n").split("\n")
    in_fence = False
    fence_char = ""
    fence_len = 0
    capturing = False
    capture_level = 0
    collected = []

    heading_target = re.compile(r"^(#{2,3})[ \t]+release[ \t]+notes[ \t]*#*$", re.IGNORECASE)
    any_heading = re.compile(r"^(#{1,6})(?:[ \t]+.*|[ \t]*)$")
    fence_re = re.compile(r"^[ \t]*(`{3,}|~{3,})")

    for line in lines:
        fm = fence_re.match(line)
        if fm:
            marker = fm.group(1)
            if not in_fence:
                in_fence = True
                fence_char = marker[0]
                fence_len = len(marker)
            elif marker[0] == fence_char and len(marker) >= fence_len:
                in_fence = False
                fence_char = ""
                fence_len = 0
            if capturing:
                collected.append(line)
            continue

        if not in_fence:
            if not capturing:
                tm = heading_target.match(line.strip())
                if tm:
                    capturing = True
                    capture_level = len(tm.group(1))
                    continue
            else:
                hm = any_heading.match(line)
                if hm and len(hm.group(1)) <= capture_level:
                    break
                collected.append(line)
        elif capturing:
            collected.append(line)

    return "\n".join(collected).strip()


def build_notes(version: str) -> str:
    pr_body = os.environ.get("PR_BODY", "")
    extracted = extract_pr_release_notes(pr_body)
    if extracted:
        return extracted

    pr_title = os.environ.get("PR_TITLE", "").strip()
    pr_number = os.environ.get("PR_NUMBER", "").strip()
    if pr_title and pr_number:
        return f"- {pr_title} (#{pr_number})"
    return f"- Release {version}"


def ensure_changelog(path: str, version: str) -> None:
    if not os.path.exists(path):
        notes = build_notes(version)
        content = f"# Changelog\n\n## {version}\n\n{notes}\n"
        with open(path, "w", encoding="utf-8", newline="") as f:
            f.write(content)
        return

    with open(path, "r", encoding="utf-8", newline="") as f:
        raw = f.read()

    eol = "\r\n" if "\r\n" in raw else "\n"
    norm = raw.replace("\r\n", "\n")
    lines = norm.split("\n")

    section_heading = f"## {version}"
    section_idx = None
    section_end = len(lines)
    for i, line in enumerate(lines):
        if line == section_heading:
            section_idx = i
            for j in range(i + 1, len(lines)):
                if lines[j].startswith("## "):
                    section_end = j
                    break
            break

    if section_idx is not None:
        section_body = "\n".join(lines[section_idx + 1 : section_end]).strip()
        if section_body:
            # Non-empty section already exists; leave CHANGELOG.md untouched.
            return
        # Empty heading exists: populate it in place.
        notes = build_notes(version)
        before = lines[: section_idx + 1]
        after = lines[section_end:]
        new_block = ["", *notes.split("\n")]
        if after and any(l.strip() for l in after):
            new_lines = before + new_block + [""] + after
        else:
            new_lines = before + new_block + [""]
        result = "\n".join(new_lines)
        if not result.endswith("\n"):
            result += "\n"
        with open(path, "w", encoding="utf-8", newline="") as f:
            f.write(result.replace("\n", eol))
        return

    notes = build_notes(version)
    header_idx = None
    for i, line in enumerate(lines):
        if re.match(r"^#[ \t]+Changelog[ \t]*$", line, re.IGNORECASE):
            header_idx = i
            break

    section_lines = [f"## {version}", "", *notes.split("\n")]

    if header_idx is not None:
        prefix = lines[: header_idx + 1]
        rest = lines[header_idx + 1 :]
        while rest and rest[0].strip() == "":
            rest.pop(0)
        if rest:
            new_lines = prefix + [""] + section_lines + [""] + rest
        else:
            new_lines = prefix + [""] + section_lines + [""]
    else:
        rest = list(lines)
        while rest and rest[0].strip() == "":
            rest.pop(0)
        if rest:
            new_lines = ["# Changelog", ""] + section_lines + [""] + rest
        else:
            new_lines = ["# Changelog", ""] + section_lines + [""]

    result = "\n".join(new_lines)
    if not result.endswith("\n"):
        result += "\n"
    with open(path, "w", encoding="utf-8", newline="") as f:
        f.write(result.replace("\n", eol))


update_gradle_properties(props_path, target_version)
ensure_changelog(changelog_path, target_version)
PY
