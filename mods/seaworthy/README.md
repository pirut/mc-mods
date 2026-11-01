# Seaworthy

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
  water and it drains.
- **Big ships.** The hull is worked out off the game thread, and buoyancy is spread over at most a few thousand
  points however big the ship gets.

Needs Sable 2.0 or later. NeoForge and Fabric, Minecraft 1.21.1. Install it on the server and on players' games
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

## Known limits

- Water inside a hull levels off along the ship's own up, not the world's. That's right for a ship floating upright
  and slightly off for one heeled far over.
- Hiding the water is drawn by Sable, with its "sub-level water occlusion" client option (on by default). If water
  still shows inside your hulls with a rendering mod installed, that is Sable's renderer.
