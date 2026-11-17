"""Clean up communique release notes before they are published.

Usage:
  release_notes.py highlights [--trim] <communique.md>  Print cleaned notes. Exit 3 if over the limits.
  release_notes.py discord <notes.md>                    Print the notes with headings as bold text.
"""

import re
import sys

MAX_CHARS = 1200
MAX_BULLETS = 5
# The maintainer and automation accounts. Other contributors keep their credit.
UNCREDITED = r"(?:AlexProgrammerDE|dependabot|renovate|github-actions|[\w-]+\[bot\])(?:\[bot\])?"
MENTION = rf"@{UNCREDITED}(?![\w-])"
FULL_CHANGELOG = re.compile(r"^\*\*Full Changelog\*\*:")
BULLET = re.compile(r"^(?:[-*+]|\d+\.)\s")
HEADING = re.compile(r"^#{1,6}\s")


def remove_credits(text: str) -> str:
    # [@user](https://github.com/user) -> @user, so the rules below see one form.
    text = re.sub(rf"\[({MENTION})\]\([^)]*\)", r"\1", text, flags=re.I)
    text = re.sub(rf"^[-*+]\s+{MENTION}\s+made their first contribution.*\n?", "", text, flags=re.I | re.M)
    # Remove each mention with the separator next to it, from the most specific form to the least.
    for pattern in (
        rf"\s*\(\s*{MENTION}\s*\)",  # Feature (@user)
        rf",?\s+by\s+{MENTION}",  # Feature by @user
        rf",\s*{MENTION}",  # (#12, @user)
        rf"{MENTION}\s*,\s*",  # (@user, @other)
        rf"\s+and\s+{MENTION}",  # (@other and @user)
    ):
        text = re.sub(pattern, "", text, flags=re.I)
    return text


def units(lines: list[str]) -> list[list[str]]:
    """Split Markdown into headings, list items, and paragraphs. Code blocks stay with their unit."""
    result: list[list[str]] = []
    fence = False
    for line in lines:
        starts_unit = not fence and (HEADING.match(line) or BULLET.match(line))
        after_blank = result and result[-1][-1] == "" and line and not line[0].isspace()
        if starts_unit or not result or (not fence and after_blank):
            result.append([line])
        else:
            result[-1].append(line)
        if line.lstrip().startswith("```"):
            fence = not fence
    return result


def render(parts: list[list[str]]) -> str:
    # Drop headings whose section became empty.
    kept = [unit for i, unit in enumerate(parts)
            if not HEADING.match(unit[0]) or (i + 1 < len(parts) and not HEADING.match(parts[i + 1][0]))]
    text = "\n".join(line for unit in kept for line in unit)
    return re.sub(r"\n{3,}", "\n\n", text).strip()


def bullets(parts: list[list[str]]) -> int:
    return sum(1 for unit in parts if BULLET.match(unit[0]))


def trim(parts: list[list[str]]) -> list[list[str]]:
    seen = 0
    limited = []
    for unit in parts:
        if BULLET.match(unit[0]):
            seen += 1
            if seen > MAX_BULLETS:
                continue
        limited.append(unit)
    while len(limited) > 1 and len(render(limited)) > MAX_CHARS:
        limited.pop()
    return limited


def highlights(path: str, force: bool) -> int:
    lines = open(path).read().strip().splitlines()
    if lines and lines[0].startswith("# "):
        lines = lines[1:]  # The "# <tag>: <title>" heading. Each release keeps its own name.
    footer = [line for line in lines if FULL_CHANGELOG.match(line)]
    body = [line for line in lines if not FULL_CHANGELOG.match(line)]
    parts = units(remove_credits("\n".join(body)).splitlines())
    fits = len(render(parts)) <= MAX_CHARS and bullets(parts) <= MAX_BULLETS
    if not fits and force:
        parts = trim(parts)
    print("\n\n".join(filter(None, [render(parts), *footer])))
    return 0 if fits or force else 3


def discord(path: str) -> int:
    print(re.sub(r"^#{1,6}\s+(.+)$", r"**\1**", open(path).read(), flags=re.M).strip())
    return 0


if __name__ == "__main__":
    args = sys.argv[1:]
    if args[:1] == ["highlights"]:
        sys.exit(highlights(args[-1], "--trim" in args))
    if args[:1] == ["discord"]:
        sys.exit(discord(args[1]))
    sys.exit(__doc__)
