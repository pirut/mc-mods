# Changelog

## Unreleased

- A **Cubic** world type on the Create World screen, on NeoForge and Fabric. Before, Chunks3 on its own made
  every new world a vanilla one, and the far terrain never ran, since no dimension was cubic unless another mod
  registered one. A Cubic world's overworld is cubic: vanilla's terrain, sea and biomes at vanilla's heights from
  the seed, with the ground going on down past Y -64 to a bedrock floor at Y -4,096
  (`-Dchunks3.cubic.bedrockY`), and the far terrain out to the horizon.
- Cubic worlds show more continuous forests, steadier shores and better-preserved ridges at a distance.
  Far terrain reuses height samples across neighbouring voxels and levels, with cheaper noise from 32-block
  voxels out (`-Dchunks3.cubic.smoothLevel`). Filling far levels straight from the seed was inspired by
  SeedGen by MobiusMeansBusiness.
- New players and players respawning with no bed land on the ground in a cubic overworld, within the spawn radius.
- Creating a Cubic world skips the experimental settings warning when nothing else is experimental (both
  loaders), and so does opening one on Fabric (NeoForge remembers the answer itself).
- `SurfaceGenerator` subclasses can set a fixed rock datum, the bedrock floor and what fills caves at each height.

## 0.1.0-alpha.1

The first release: Earth Platform's cubic engine (as of Earth Platform build 69) as its own library mod, for
NeoForge and Fabric on Minecraft 1.21.1.

- Cubic dimensions from Y -16,384 to 16,383, registered by other mods with `Cubic.register`.
- Vanilla play in cubes: mining, building, light, mobs, pathfinding, redstone, pistons, structures, portals.
- The distant terrain renderer, with Sodium and Iris shader pack support.
- The mod compatibility layer (Create, Mekanism, AE2, Waystones, maps, Chunky, CC: Tweaked, WorldEdit, Axiom
  and more) and Lithium settings.
- An example mod with one cubic dimension, kept in the repo and never released.
