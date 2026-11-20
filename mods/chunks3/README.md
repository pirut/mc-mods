# Chunks3

Cubic chunks for Minecraft, on NeoForge and Fabric. A dimension's blocks live in 16-block cubes from
Y -16,384 to 16,383 (32,768 blocks of height), with vanilla gameplay working on top (mining, building, light, mobs,
redstone, structures, portals) and a distant terrain renderer that draws to the horizon.

1.21.1 is the tree in `common/` and the loader folders: the Cubic world type below. 1.21.11 is a separate tree in
`mc12111/` (the engine port from before that world type) and does not add Cubic on the Create World screen.
Keeping the trees apart means the 1.21.1 world type and the 1.21.11 port can land in either order.

## The Cubic world type

Make a world, press **World Type** on the World tab until it says **Cubic**, and create it. The overworld is then
cubic: vanilla's terrain, sea level and biomes at vanilla's heights, from the world's seed, but with no floor at
Y -64. The rock, caves and ores go on down for thousands of blocks to a bedrock floor at Y -4,096. Vanilla's lava
still fills the caves from Y -54 to -64, a few blocks of rock seal it from below, the caves under that are open,
and lava lakes lie just above the bedrock. The Nether and the End are vanilla's. The far terrain draws the land
out to the horizon (about 94 km at the default detail).

- The ground's shape and the biomes are vanilla's for the seed. The cover (grass, sand, snow, trees, plants),
  the caves and the ores are Chunks3's own, made to look vanilla, so a Cubic world is not block for block the
  vanilla world of the same seed. Vanilla structures generate through the engine as in other cubic dimensions (none with Generate Structures
  off).
- `-Dchunks3.cubic.bedrockY=<y>` moves the bedrock floor (default -4096) for new ground; set it before making a
  world and keep it, since ground made with another value will not match.
- A Cubic world opened without Chunks3 is an empty world: its blocks live in Chunks3's cube files.

## Settings

Mods > Chunks3 > Config (NeoForge), Mod Menu's config button (Fabric, with Mod Menu installed), or the Chunks3
page in Sodium's Video Settings: the far terrain's distance (37 to 397 km, default 94 km) and sharpness, which
apply at once while playing, a frame rate to hold, and the engine's switches. In a cubic world the distance and
sharpness are on the pause menu too. Zoomed in (a spyglass, a zoom mod, a narrow field of view), the far terrain
where you look sharpens down to single blocks. The settings are kept in `config/chunks3-client.properties`.

## Using it from another mod

It is the engine of [Earth Platform](https://github.com/pirut/earth-platform), split out so other mods can build
on it. It is early: the API below can still change between alpha versions.

1. Depend on it: the `chunks3` jar for your loader on your build's classpath, and a dependency on `chunks3` in
   your `neoforge.mods.toml` or `fabric.mod.json` (or bundle it jar-in-jar).
2. Add a dimension whose dimension type is a 16-block "carrier": `min_y` 0, `height` 16, `logical_height` 16.
   Vanilla chunks still exist there, empty; the blocks live in cubes.
3. Register it once, from your mod's constructor or initializer, on both client and server:

```java
Cubic.register(MY_LEVEL, (level, folder) -> new MyGenerator(level.getSeed()));
```

The [`example/`](example) mod does exactly this in one class, with the library's Highlands test generator:
`./gradlew :chunks3:example:runClient`, make a world and type `/cubes enter`. The example is NeoForge only and is
never released.

A generator implements `io.github.pirut.chunks3.gen.CubeGenerator`. The required part is small: `id`, `generate`
(one 16³ cube), `surfaceY`, `farColumn` (the distant terrain at a level of detail) and `seaLevel`. Everything
else (structures, biome lookup, data prefetching, landing spots, the far renderer's map scale) has a default.
`gen.SurfaceGenerator` and `gen.HighlandsGenerator` show a complete heightmap world with caves, ores, trees and
vanilla structures; `gen.OverworldGenerator` (the Cubic world type's) reads vanilla's own terrain noise and biomes.

The rest of the API:

| Call | What it does |
| --- | --- |
| `Cubic.register(key, generators)` | Makes a dimension cubic. The first one registered is the "home" that vanilla's Nether and End portals lead back to (a Cubic world's overworld when none is) |
| `Cubic.CUBIC_TYPE`, `Cubic.adopt(...)` | Any dimension of type `chunks3:cubic` is cubic, made by `gen.OverworldGenerator`, for as long as its world is played |
| `CubicDimension.cloudHeight(...)` | Where shader packs' clouds float in that dimension |
| `Cubic.addTransit(test)` | Entities passing far above the terrain (a glider on a jet stream, a fast boat) get no generated cubes loaded around them; above `Cubic.CLEAR_SKY` they still get what players built |
| `Cubic.onArrival(listener)` | Told when a player sent with `CubicEvents.arriveWhenReady` has landed |
| `CubicLevel.of(level)` | The level's `CubeWorld` (null for vanilla levels): read and write blocks and light; on the server a `ServerCubeWorld`, with `loadAround` to make the cubes about a spot now |
| `CubicEvents.arriveWhenReady(...)` | Moves a player to a column once its cubes are made, landing on the ground |
| `util.DataWaits` | A generator that downloads its data marks the waits, so the far workers start other columns meanwhile |
| `/cubes enter`, `leave`, `where`, `status`, `light`, `find`, `locate`, `structures` | Debug commands |

## Compatibility

Sodium and Iris, with shader packs that support Voxy-style far terrain (Photon is the one it is tested with, but
any Voxy-compatible pack should work). Lithium: Chunks3 turns off the Lithium features that read chunk sections
directly. FerriteCore. Many content mods: Create, Mekanism, AE2, Waystones, Xaero's maps, JourneyMap, Chunky,
Cobblemon, Twilight Forest, the Aether, Sable, CC: Tweaked, WorldEdit (NeoForge), and Axiom (Fabric). Distant
Horizons is paused in cubic dimensions, since Chunks3 draws its own distant terrain. On Fabric the mod-compat
mixins are skipped for now, as in Earth Platform.

Developer flags are `-Dchunks3.*` system properties (for example `-Dchunks3.far.fade=false`); they are the
`earthplatform.*` flags in Earth Platform's `docs/FLAGS.md`, renamed. The local performance log is
`chunks3/perf/latest.jsonl` in the game folder.

## Building

Java 21. From the repo root:

```sh
./gradlew :chunks3:build                      # both loaders' jars, and the engine tests
./gradlew :chunks3:example:runClient          # a dev game with the example dimension
```

The far renderer compiles against Iris, which Gradle fetches from Modrinth's Maven (the tested build's version
ids are in `gradle.properties`). It is never bundled. The engine's tests live in `common/src/test` and run in the
NeoForge project as NeoForge unit tests, so the mixins they check are applied.

## Following Earth Platform

Until Earth Platform depends on this library, its engine keeps changing in Earth's tree.
`tools/port_from_earth.py` turns the engine part of an Earth commit range into a patch here (renamed, Earth-only
files left out) and applies it; hunks that no longer fit are left in `.rej` files. 0.1.0-alpha.1 was made from
Earth Platform build 69 (`59fdd45`); the engine now follows Earth up to build 74 (`4e639cc5`), so the next port
starts there. [`MIGRATING-EARTH.md`](MIGRATING-EARTH.md) lists what Earth needs to do to
switch over.

## License

LGPL-3.0-only (see the repo's `COPYING` and `COPYING.LESSER`). Mods of any license may depend on Chunks3; changes
to Chunks3 itself stay under the LGPL. The far terrain renderer is built on ideas from Voxy (Cortex) and Distant
Horizons (James Seibel and contributors) and contains no code from either.
