# Changelog

## 0.2.0-alpha.1

- Minecraft 1.21.11 on NeoForge (21.11) and Fabric, alongside 1.21.1, from one source tree.
- On 1.21.11 the near cubes and the far water draw through the game's new GPU device and its terrain pipelines,
  the cubes' block entities draw with the game's own, and fog is pushed out from the game's fog buffer.
- On 1.21.11 the mixins into other mods (Sodium, Iris, Create, maps and the rest) are left out until each is
  checked against that mod's 1.21.11 build, so shader packs are not drawn with there yet. The debug screen lines
  are not ported yet either.

## 0.1.0-alpha.1

The first release: Earth Platform's cubic engine (as of Earth Platform build 69) as its own library mod, for
NeoForge and Fabric on Minecraft 1.21.1.

- Cubic dimensions from Y -16,384 to 16,383, registered by other mods with `Cubic.register`.
- Vanilla play in cubes: mining, building, light, mobs, pathfinding, redstone, pistons, structures, portals.
- The distant terrain renderer, with Sodium and Iris shader pack support.
- The mod compatibility layer (Create, Mekanism, AE2, Waystones, maps, Chunky, CC: Tweaked, WorldEdit, Axiom
  and more) and Lithium settings.
- An example mod with one cubic dimension, kept in the repo and never released.
