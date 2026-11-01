# Moving Earth Platform onto Chunks3

Earth Platform still carries its own copy of the engine. Nothing in Earth changes until it switches. This is the
list of what the switch takes, written against Earth Platform build 69 (`59fdd45`), the commit Chunks3
0.1.0-alpha.1 was made from. An earlier attempt at the same switch (earth-platform draft PR #114, on build 63)
shows most of the Earth-side glue.

## Before switching

- **Port Earth's newer engine changes first.** Everything Earth changed in the engine after `59fdd45` (the build 70
  far terrain work, for one) goes into Chunks3 with `tools/port_from_earth.py ../earth-platform 59fdd45..main`,
  then a Chunks3 release. Otherwise the switch loses that work.

## In Earth's build

- Depend on Chunks3 in both loaders and ship it inside Earth's jar (NeoForge `jarJar`, Fabric `include`), so
  players still install one file. Pin a range like `[0.1.0-alpha.1,0.2)`. Add `chunks3` as a required
  dependency in `neoforge.mods.toml` and `fabric.mod.json`.
- Delete the engine from Earth's tree: `minecraft/src/main/java/io/earthplatform/minecraft/cubic/` except
  `earth/`, `play/` and `jet/`, plus `diagnostics/` and `platform/`, and the engine tests. Keep the Earth-only files
  listed below.
- Earth keeps the Lithium options in its own metadata (harmless twice), or drops them since Chunks3 declares them.

## Registering the Earth world

- Register the dimension from both entry points, before any level loads:
  `Cubic.register(EARTH_LEVEL, (level, folder) -> CubicSettings.open(folder, level.getSeed(), biomes))`, with
  `EARTH_LEVEL` = `earthplatform:cubic`. The key stays the same, so existing worlds load. `CubicSettings` (the
  generator pin in the dimension folder and all the Earth data sources) moves into Earth's `earth/` package.
- `Cubic.LEVEL` is gone: Earth code that used it uses its own `EARTH_LEVEL`, and engine code uses `Cubic.home()`
  or `Cubic.isCubic(key)`.
- `CubicDimension.cloudHeight(EarthClouds::height)` (or what `PhotonClouds` read from `EarthConfig`) gives shader
  packs the Earth's cloud height. `PhotonClouds` no longer reads Earth's config.
- `EarthGenerator` overrides `mercatorMetresPerBlock()` with `grid().metresPerBlock()`. The engine sends it to
  clients as `FarScale` (it was `EarthScale`).
- `EarthGenerator.missingData(failure)` returns true for `MissingTerrainException`, so the engine retries a
  cube instead of failing it.

## Jet stream and travel

- `Cubic.addTransit(JetStream::outrunsTerrain)`: entities it accepts get no generated cubes near them. Above
  `Cubic.CLEAR_SKY` (9000, the jet stream's clear sky, set with `-Dchunks3.clearSky`) they still get the cubes
  players built, as before. The engine no longer tells "above the terrain" apart from "carried by the ocean
  current"; the boat case is under `CLEAR_SKY`, so it gets nothing, as before.
- `/earth goto` and friends: the travel code (`CubicEvents.travel`, data prefetch, landing search) left the
  engine. Earth's own travel class does the Earth part and then calls `CubicEvents.arriveWhenReady(source, player,
  x, z, view)`. `EarthMap.arrived` hooks in with `Cubic.onArrival(EarthMap::arrived)`; the failure path
  (`EarthMap.failed`) stays in Earth's travel class.
- `SableWind.tick` (the jet stream pushing Sable ships) moves to Earth's `jet/` package and keeps its level tick.

## Mixins that stay in Earth

Earth gets its own mixin config (say `earthplatform.earth.mixins.json`) with these, which Chunks3 left out:

- Jet stream: `CubicJetFlightMixin`, `CubicJetMoveCheckMixin`, `CubicJetTicketMixin`, `CubicJetTickMixin`,
  `CubicJetRocketMixin`, `CubicJetWeatherMixin`, `CubicJetLerpMixin`, `CubicJetRenderDistanceMixin`,
  `GliderTurnMixin`; ocean current: `CubicBoatMixin`, `CubicCurrentMoveMixin`.
- Earth client look: `EarthCreateWorldMixin`, `EarthCloudsMixin`, `EarthHorizonMixin`, `EarthFogColourMixin`,
  `EarthFlightCameraMixin`, `CubicWaterTintMixin` (the dark sea tint), `EarthCompassReequipMixin`,
  `EarthCompassSpinMixin`.
- NeoForge only: `ElytraFlightMixin`, `GliderChunkLoadMixin` (Earth's `earthplatform.neoforge.mixins.json` keeps
  them; `MarkAndNotifyMixin` is now Chunks3's).
- Fabric only: `ElytraWearMixin`, `PresetEditorMixin`. Earth's `EarthOpenWorldMixin` goes: Chunks3's
  `CubicOpenWorldMixin` does the same for every registered cubic dimension.
- Earth's `MixinConfigTest` counts change with the split.

## Loader wiring

- Chunks3 wires the engine's events itself (payloads, spawn rules, level load/save/tick, POI claims, portals,
  `/cubes`, respawn, the client's frame, render stages, fog, HUD and debug text, flawless frames, Axiom flushes on
  Fabric). Earth's `NeoForgeEvents`, `NeoForgeClientEvents`, `EarthPlatformFabric` and
  `EarthPlatformFabricClient` keep only the Earth calls (config, `EarthWorlds`, compass, rules, admin, memory,
  `ImageryCalibration`, `SableWind`, sleep, build check, keys, preset editor, Earth HUD and pause menu).
- The Fabric mixins that fire NeoForge-style hooks (`ServerLevelHooksMixin`, `LevelRendererStagesMixin`,
  `FogRendererHooksMixin`, `MinecraftHooksMixin`, `ClientLevelLoadMixin`, `ClientRespawnMixin`,
  `PlayerListRespawnMixin`, `SpawnPlacementsMixin`, `DebugScreenMixin`) are Chunks3's now. Earth's sleep hook
  (`EarthBasics.sleptThrough`) needs its own small Fabric mixin, since Chunks3's `ServerLevelHooksMixin` dropped it.
- The loader abstraction (`Loader`, `Platform`, `ClientPlatform`, `Payloads`) is Chunks3's
  `io.github.pirut.chunks3.platform`, with the implementations in the Chunks3 jar. Earth can use it for its own
  loader calls. `ClientPlatform.configScreen` left it: Earth opens its config screen itself.
- Earth's payload registrar keeps `EarthWorlds.payloads`; the engine's payloads are now on the `chunks3`
  namespace, so a build 69 client cannot join a Chunks3-based server and the other way round (the build check
  already refuses mismatched builds).

## Shared helpers

- `NamedThreads`, `TickShare` and `DataWaits` are in `io.github.pirut.chunks3.util`. Earth's `geography` module
  must use these (not its own copies) for the far workers to see its download waits and for `TickShare` pauses to
  be one shared state. The simplest way: geography's classes delegate to Chunks3's, or geography moves to them.

## What players and server owners notice

- Flags: engine `-Dearthplatform.*` flags become `-Dchunks3.*` (`docs/FLAGS.md` needs the rename; Earth's
  `EarthMixinPlugin` can copy old spellings across, as #114 did).
- The local performance log moves from `earthplatform/perf/` to `chunks3/perf/`.
- Thread names say "Cube worker" and "Far terrain" instead of "Earth cube worker" and "Earth far terrain"; the
  damaged save file chat line starts "Chunks3:" instead of "Earth Platform:".
- Saved names do not change: dimension `earthplatform:cubic`, the `earthplatform.cubic_home` player tag, the
  `earthplatform_cubic_portals` saved data and the Twilight Forest and Aether return tags, so worlds load as
  before.

## Checking the switch

The usual far-view fingerprints and smoke tests (Yosemite, Photon on and off, a survival trip through the Nether
and back), plus a world made on build 69 loading on the switched build with its builds, portals and home intact.
