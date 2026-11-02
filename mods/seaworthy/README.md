# Create: Seaworthy

Watertight hulls for [Create Aeronautics](https://modrinth.com/mod/create-aeronautics) and any other ship built on
[Sable](https://github.com/ryanhcode/sable). Build a hull, and the sea stays out of it.

- **No water inside a sealed hull.** Below the waterline the inside of your ship is dry: no water drawn inside, no
  swimming, no underwater fog. Open-topped hulls stay dry too, as long as the rim is above the water.
- **Displacement.** The air in the hull lifts the ship by the water it keeps out, so a big steel ship floats like a
  real one, and sits deeper as you load it.
- **Flooding.** A hole below the waterline lets water into the compartment behind it, and it fills from the bottom up
  over time: faster for a bigger or deeper hole, slower as the water inside rises toward the waterline. Bulkheads
  keep the water in one compartment until it tops them. The water inside is drawn at its real level, rising
  smoothly and evenly across the floor (the sea's own surface is at sea level, so Seaworthy draws the top of the
  water inside itself). Patch the hole and the leak stops. Lift the ship out of the
  water and it drains. The water is saved with the world, so a flooded ship stays flooded across a restart. Water
  lies flat in the world when the ship leans, and shifts to the low side. In a flooded room you are under water and
  run out of breath. Holes taking on water show spray or bubbles, and you can hear them.
- **Big ships.** The hull is worked out off the game thread, and buoyancy is spread over at most a few thousand
  points however big the ship gets.

Needs Sable 1.0.4 or later, so it works with every Create Aeronautics release from 1.0.2 to the newest. NeoForge and
Fabric, Minecraft 1.21.1. Tested in-game with Sable 1.0.4, 1.0.6, 1.1.0, 1.1.3, 1.2.2, 2.0.0 and 2.0.6, Aeronautics
1.0.2 to 1.3.2, and Create 6.0.9 and 6.0.10. Create is optional; it adds pumps. Install it on the server and on players' games
(players without it can still join, and just see the water as before).

## How a hull is worked out

- **Outside** is everything the sea reaches from around the ship moving sideways or upwards. Water never has to
  flow *down* into the hold of an intact boat, so an open hold counts as inside, with its rim as the way in.
- Blocks keep water out by their shape, the way vanilla water does: a full block on every side, a slab on its full
  side, a closed trapdoor across its face. Glass panes count as watertight (the `seaworthy:watertight` tag), so
  portholes work. Add blocks to `seaworthy:watertight` or `seaworthy:leaky` with a datapack to change that.
- A **hole** of up to 2 blocks across (`maxHoleWidth`) is a breach: the compartment behind it floods over time. A
  wider gap is open sea, so a ship missing half a wall fills up as fast as the water can get in.
- Compartments are the separate dry spaces inside. Each floods on its own.

## Goggles

Wearing Create's Engineer's Goggles and looking at a block on a ship shows each compartment's flooding and leaks. Whether
a pump is running only shows in `/seaworthy status`.

## Pumps

A running Create mechanical pump on a ship empties the compartment it's in, or the wettest one it touches, overboard.
It needs no pipes, only rotation, and it can keep up with a leak if it's fast enough. Check on it with
`/seaworthy status`. This needs Create, so there's nothing to place on Fabric without a Create port.

## Commands

`/seaworthy status` lists each ship's compartments, how flooded they are, and how many leaks each has below the
waterline. Anyone can use it for ships within 128 blocks; operators see every ship in the dimension.

## Config

`config/seaworthy.properties`, written on first start:

| Key | Default | What it does |
| --- | --- | --- |
| `flooding` | `true` | Water comes in through holes below the waterline. |
| `floodSpeed` | `1.0` | How fast water comes in and drains. 1 is real-world speed for the hole's size and depth. |
| `buoyancy` | `1.0` | How strongly dry air lifts the hull. 1 matches the water density Sable floats blocks with. |
| `maxHoleWidth` | `2` | Holes up to this wide flood over time; wider gaps let the sea straight in. 0 turns it off. |
| `maxHullCells` | `8000000` | Ships with more blocks than this in their bounding box are left alone. |
| `maxLumps` | `4096` | How finely buoyancy is spread over a big hull. |
| `pumpRate` | `16` | Blocks of water a second a Create mechanical pump empties at 256 RPM. Slower pumps do less. |

- When a ship is first put together on the water, the hole its blocks leave in the sea is told to fill back in, so the
  ship floats at its proper height right where it was built. This happens once per ship, not after every restart.

## Testing

`tools/float-check.sh` starts the dev server with Sable, floats a test hull in a real ocean, floods it and checks the
heights, and exits non-zero if anything is off. It takes about seven minutes, so it isn't part of the normal build.

## Known limits

- Floodwater is only drawn on a ship that's leaning less than about 75 degrees. On its side or upside down the water
  is still there, but isn't drawn.
- A part-filled top layer of water counts as dry for players until it's full: full layers are real water, and the
  part-filled one is like air.
- Hiding the water is drawn by Sable, with its "sub-level water occlusion" client option (on by default). Sable only
  does that with vanilla's terrain renderer, so Seaworthy adds it to Sodium's. Shader packs (Iris) and other
  renderers, such as Embeddium, still show the sea inside hulls, though you can't swim in it.
