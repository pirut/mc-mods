# Changelog

## Unreleased

- A **Cubic** world type on the Create World screen, on NeoForge and Fabric. Before, Chunks3 on its own made
  every new world a vanilla one, and the far terrain never ran, since no dimension was cubic unless another mod
  registered one. A Cubic world's overworld is cubic: vanilla's terrain, sea and biomes at vanilla's heights from
  the seed, with the ground going on down past Y -64 to a bedrock floor at Y -4,096
  (`-Dchunks3.cubic.bedrockY`), and the far terrain out to the horizon.
- Far terrain in a Cubic world reads vanilla's noise only as finely as each far level needs: one height and one
  biome per far voxel, a short search down from vanilla's own depth estimate instead of a read at every height,
  heights kept so one sample serves every coarser level, and no 3D noise from 32-block voxels out
  (`-Dchunks3.cubic.smoothLevel`). The idea of filling far levels straight from the seed comes from SeedGen by
  MobiusMeansBusiness; the code is Chunks3's own.
- New players and players respawning with no bed land on the ground in a cubic overworld, within the spawn radius.
- Creating a Cubic world skips the experimental settings warning when nothing else is experimental (both
  loaders), and so does opening one on Fabric (NeoForge remembers the answer itself).
- `SurfaceGenerator` subclasses can set a fixed rock datum, the bedrock floor and what fills caves at each height.
- **Settings, with a far distance slider.** Mods > Chunks3 > Config on NeoForge, Mod Menu's config button on
  Fabric (Mod Menu optional), and a Chunks3 page in Sodium's Video Settings when Sodium is installed: the far
  terrain's distance (37 to 397 km, default 94 km), its sharpness (Fast, as before, to Ultra), a frame rate to
  hold, textured levels, and the engine's switches (smooth ground and block textures, experimental and off;
  fading between levels; zoom sharpening; cave and shadow culling; shader pack options). The distance and
  sharpness are also on the pause menu in cubic worlds, and apply at once while playing; the switches apply at
  the next start. Saved in `config/chunks3-client.properties`; `-Dchunks3.*` on the command line still wins.
- **Zooming in shows the ground's blocks.** A zoomed view (spyglass, zoom mod, narrow field of view) now spends
  its far columns on the spot it looks at first, down to a new far level of single blocks (the near ground's own
  heights, cover, trees and edits), then on the rest of the view: through a spyglass, single blocks 1 km off and
  two-block voxels 8 km off, where a block is a pixel. It used to count less of the zoom until the whole view
  fitted its budget, under three times for a spyglass, so far ground stayed in 16-block voxels 8 km off however
  far it zoomed. `-Dchunks3.far.zoomSharpness` (default 2) sets how fine zoomed far terrain goes.
- The far terrain engine follows Earth Platform up to build 74: far distance changes while playing, a frame rate
  to hold, quick stand-ins for every far level (the whole far view stands within seconds of arriving; on in Cubic
  worlds), far terrain kept on disk with the world, ground columns sent as vertical runs, faster zoom
  sharpening, far trunks no longer brown posts, giant groves, near cube and far culling, shader packs made for
  Distant Horizons, making room when memory runs short, and a fix for 20 or more cores. Earth's vanilla caves are
  in for generators that ask for them; worlds keep the caves they were made with. The wire version is now
  `cubic-3`, so client and server need the same build.

## 0.1.0-alpha.1

The first release: Earth Platform's cubic engine (as of Earth Platform build 69) as its own library mod, for
NeoForge and Fabric on Minecraft 1.21.1.

- Cubic dimensions from Y -16,384 to 16,383, registered by other mods with `Cubic.register`.
- Vanilla play in cubes: mining, building, light, mobs, pathfinding, redstone, pistons, structures, portals.
- The distant terrain renderer, with Sodium and Iris shader pack support.
- The mod compatibility layer (Create, Mekanism, AE2, Waystones, maps, Chunky, CC: Tweaked, WorldEdit, Axiom
  and more) and Lithium settings.
- An example mod with one cubic dimension, kept in the repo and never released.
