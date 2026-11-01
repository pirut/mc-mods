# Chunks3

Cubic chunks for Minecraft 1.21.1 and 1.21.11, on NeoForge and Fabric. Chunks3 is a library mod: another mod registers a
dimension with it, and that dimension's blocks then live in 16-block cubes from Y -16,384 to 16,383 (32,768
blocks of height), with vanilla gameplay working on top (mining, building, light, mobs, redstone, structures,
portals) and a distant terrain renderer that draws to the horizon. It adds nothing to the game by itself.

It is the engine of [Earth Platform](https://github.com/pirut/earth-platform), split out so other mods can build
on it. It is early: the API below can still change between alpha versions.

## Using it

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
vanilla structures.

The rest of the API:

| Call | What it does |
| --- | --- |
| `Cubic.register(key, generators)` | Makes a dimension cubic. The first one registered is the "home" that vanilla's Nether and End portals lead back to |
| `CubicDimension.cloudHeight(...)` | Where shader packs' clouds float in that dimension |
| `Cubic.addTransit(test)` | Entities passing far above the terrain (a glider on a jet stream, a fast boat) get no generated cubes loaded around them; above `Cubic.CLEAR_SKY` they still get what players built |
| `Cubic.onArrival(listener)` | Told when a player sent with `CubicEvents.arriveWhenReady` has landed |
| `CubicLevel.of(level)` | The level's `CubeWorld` (null for vanilla levels): read and write blocks and light; on the server a `ServerCubeWorld`, with `loadAround` to make the cubes about a spot now |
| `CubicEvents.arriveWhenReady(...)` | Moves a player to a column once its cubes are made, landing on the ground |
| `util.DataWaits` | A generator that downloads its data marks the waits, so the far workers start other columns meanwhile |
| `/cubes enter`, `leave`, `where`, `status`, `light`, `find`, `locate`, `structures` | Debug commands |

## Compatibility

On 1.21.11 the engine and both renderers are ported, but the mixins into other mods (including Sodium and Iris)
are left out until each is checked against that mod's 1.21.11 build, so shader packs are not drawn with there yet,
and the cube and far terrain lines are missing from the debug screen. The rest of this section is about 1.21.1.

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
./gradlew :chunks3:build -Pmc=1.21.11         # the same for 1.21.11
./gradlew :chunks3:example:runClient          # a dev game with the example dimension
```

One source tree builds every version. Code that differs sits side by side in the same file between
`//#if MC >= 1.21.11`, `//#else` and `//#endif` lines, written for 1.21.1 with the other branches commented out
with `//$$ `; `preprocess.gradle` switches them for the version being built, and `versions/remap.txt` renames what
Mojang renamed everywhere (`ResourceLocation` became `Identifier`). `versions/<mc>.properties` holds each version's
dependency versions and the files it leaves out.

The far renderer compiles against Iris, which Gradle fetches from Modrinth's Maven (the tested build's version
ids are in `gradle.properties`). It is never bundled. The engine's tests live in `common/src/test` and run in the
NeoForge project as NeoForge unit tests, so the mixins they check are applied.

## Following Earth Platform

Until Earth Platform depends on this library, its engine keeps changing in Earth's tree.
`tools/port_from_earth.py` turns the engine part of an Earth commit range into a patch here (renamed, Earth-only
files left out) and applies it; hunks that no longer fit are left in `.rej` files. 0.1.0-alpha.1 was made from
Earth Platform build 69 (`59fdd45`). [`MIGRATING-EARTH.md`](MIGRATING-EARTH.md) lists what Earth needs to do to
switch over.

## License

LGPL-3.0-only (see the repo's `COPYING` and `COPYING.LESSER`). Mods of any license may depend on Chunks3; changes
to Chunks3 itself stay under the LGPL. The far terrain renderer is built on ideas from Voxy (Cortex) and Distant
Horizons (James Seibel and contributors) and contains no code from either.
