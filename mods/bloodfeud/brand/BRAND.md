# Blood Feud brand kit

Everything here is original, drawn for Blood Feud. `make-art.py` redraws every image from code (Python + Pillow):
`python3 make-art.py` writes them to `out/`.

| File | Use |
|---|---|
| `bloodfeud-icon-512.png` | Mod icon, Modrinth/CurseForge project icon, avatar. The 128 px copy ships in the jar. |
| `bloodfeud-wordmark.png` | The name on its own, transparent background. |
| `bloodfeud-banner-1920x480.png` | Modrinth/CurseForge header, README top, YouTube/Discord banner. |
| `bloodfeud-social-1280x640.png` | GitHub social preview, link cards, thumbnails. |
| `bloodfeud-palette.png` | The colors below. |

## Name and lines

- **Name:** Blood Feud. Always two words, both capitalized. Never "the Nemesis System" or anything borrowed from
  other games.
- **Tagline:** Mobs that remember you.
- **One line:** Kill a few zombies while their friends watch, and one of them walks away with a name, a grudge and a
  level.
- **Short (Modrinth summary, under 100 characters):** Mobs that remember you: named rivals, packs, Kings, scars and
  relic weapons.
- **Alternate taglines:** "It remembers. Do you?" · "Every kill has a witness." · "Somebody saw that."

## Colors

| Name | Hex | Where |
|---|---|---|
| Blood | `#B2121F` | The wordmark, the icon's banner, accents |
| Deep | `#6E0A14` | Shadows, banner stripes |
| Ember | `#E84034` | Eyes, highlights, warnings |
| Bone | `#E8DCC4` | Skull, body text on dark |
| Bone shade | `#B0A28A` | Secondary text |
| Gold | `#D9A441` | Bosses (their names are gold in game), hilts |
| Royal | `#A24BD8` | Kings (their names are light purple in game) |
| Ink | `#140A0C` | Background |
| Night | `#261216` | Background checks |

Dark backgrounds only. The game's own rank colors are gold for Brutes, red for Bosses and light purple for Kings, so
the kit keeps those meanings.

## Type

- **Headings:** the kit's own 5x7 pixel font in `make-art.py`, uppercase, with drips under the wordmark only.
- **Small text:** DejaVu Sans Bold (free, ships with most Linux systems), in Bone shade.

## Logo rules

- The skull sits on a crimson pack banner with two crossed blades. Keep it square and on Ink.
- Scale in whole multiples of the pixel grid (32 px grid), with nearest-neighbor scaling, never smoothed.
- Don't recolor the skull or add a gradient. Don't put the wordmark on a light background.

## Voice

Brutes talk like grudge-holding Minecraft mobs: short, cocky, a little funny, about what actually happened
("Still got your iron pickaxe. Want it back? Come and take it."). No gore beyond the name, no real-world slurs or
swearing, and no lines lifted from other games, books or films. Writing for the store page is plain and direct.

## Store page

See [STORE.md](STORE.md) for the Modrinth/CurseForge description.
