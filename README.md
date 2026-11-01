# mc-mods

JR Bussard's small Minecraft mods, in one repo. Each mod lives in its own folder under [`mods/`](mods), has its
own version and changelog, and is released on its own.

## Mods

| Mod | What it does | Loaders | Get it |
| --- | --- | --- | --- |
| [CC: Agent](mods/cc-agent) | Lets Claude Code, Codex or any MCP client write programs for and drive CC: Tweaked computers and turtles. | NeoForge, Fabric | [Releases](https://github.com/pirut/mc-mods/releases?q=cc-agent) |
| [Bug Courier](mods/bug-courier) | A clear in-game way for players to report bugs to the mods they play. | NeoForge, Fabric | [Releases](https://github.com/pirut/mc-mods/releases?q=bug-courier) |
| [Chunks3](mods/chunks3) | Cubic chunks as a library: other mods register dimensions up to 32,768 blocks tall, with a distant terrain renderer. The engine of Earth Platform. | NeoForge, Fabric | [Releases](https://github.com/pirut/mc-mods/releases?q=chunks3) |
| [Earth Platform](https://github.com/pirut/earth-platform) | The real Earth in Minecraft at true scale, with real terrain and climate and a far view to the horizon. Lives in its own repo. | NeoForge, Fabric | [Modrinth](https://modrinth.com/mod/earth-platform) |

Each mod's README says which Minecraft versions it's for. Issues for any mod in this repo go to
[this repo's issues](https://github.com/pirut/mc-mods/issues); say which mod in the title.

## Layout

```
mods/<mod>/           one folder per mod
  gradle.properties   id, name, version, metadata, Modrinth project id
  CHANGELOG.md        one "## <version>" section per release
  common/             code for both loaders (compiled into each jar)
  neoforge/ fabric/   entry points and loader-only code
  example/            optional: a never-released example mod that uses it (see mods/chunks3)
  versions/<mc>.properties  optional: this mod's settings for one Minecraft version
versions/<mc>.properties  each Minecraft version's Minecraft, NeoForge, Fabric, Parchment and Java versions
build-logic/          shared Gradle plugins: mcmods.neoforge, mcmods.fabric, mcmods.library
common-lib/           internal library a mod can bundle into its jars (uses_common_lib=true)
template/             a buildable example mod to copy for a new one
```

## Minecraft versions

A build is for one Minecraft version: `mc_default` in the repo's `gradle.properties`, or another one with
`-Pmc=<version>`. Each version has a `versions/<mc>.properties` with the Minecraft, NeoForge, Fabric Loader and API,
Parchment and Java versions every mod builds against. A mod lists the versions it's built for in its
`gradle.properties`, for example `minecraft_targets=1.21.1,1.21.11,26.3`; without it, only `mc_default`. A build
for a version leaves out the mods that don't target it.

Properties are merged in this order, later ones winning: the repo's `gradle.properties`, `versions/<mc>.properties`,
the mod's `gradle.properties`, then the mod's own `mods/<mod>/versions/<mc>.properties`. So a mod's
`gradle.properties` holds what is the same for every Minecraft version, and its `versions/<mc>.properties` what
differs: dependency versions, a NeoForge range, and these:

- `extra_src`: more source folders for that version, relative to the mod folder, comma-separated. A folder under
  `common/` goes into both loaders' jars, one under `neoforge/` or `fabric/` only into that loader's. A folder
  named `java` or `resources` is added as it is; any other adds its `java/` and `resources/`. So code that differs
  between versions lives in folders like `common/src/mc1.21.1/java` and `common/src/mc26.1/java`, and each
  version's file names the ones it uses.
- `exclude_src`: Ant patterns to leave out of the sources and resources for that version (`**/FooMixin.java`).
- `loaders`: `neoforge` or `fabric`, when the mod only has one loader on that version.
- `modrinth_game_versions`: the Minecraft versions Modrinth lists that version's jars for, if not just itself.

The merged values fill `${...}` in the mod's `neoforge.mods.toml` and `fabric.mod.json`, so those can use
`${minecraft_version}`, `${minecraft_version_range}`, `${java_version}` and so on.

Minecraft 26.1 and later ship without obfuscation (`unobfuscated=true` in their `versions/` file): there are no
Parchment mappings, Fabric uses Loom's non-remapping plugin, and they need Java 25. A mod's Fabric
`build.gradle` still uses `modImplementation` and friends for mods it depends on; on those versions they are plain
dependencies.

## Building

Java 21 for Minecraft 1.21.x and Java 25 for 26.x (Gradle downloads a missing one).

```sh
./gradlew build                                   # every mod, for mc_default
./gradlew build -Pmc=26.3                         # every mod that targets 26.3
./gradlew :cc-agent:build -Pmc=1.21.11            # one mod
./gradlew :cc-agent:neoforge:runClient -Pmc=26.3  # a dev game (also :fabric:runClient, runServer)
```

Jars land in `mods/<mod>/<loader>/build/<minecraft>/libs`, named `<mod_id>-<loader>-<minecraft>-<version>.jar`.
CI builds every Minecraft version any mod targets, one job each.

## A new mod

Copy [`template/`](template) to `mods/<name>` and follow its README. Gradle picks the folder up by itself.

## Releasing

Each mod is tagged on its own as `<folder>-v<version>`, for example `cc-agent-v0.5.1`, and only that mod is
built and released, with jars for every Minecraft version it targets.

1. Bump `mod_version` in the mod's `gradle.properties`.
2. Add a `## <version>` section to its `CHANGELOG.md`. It becomes the release notes.
3. Merge to `main`. The Release workflow releases every mod whose version has no release yet.

You can also push a tag `<folder>-v<version>` yourself, or run the Release workflow from the Actions tab with
the mod's folder name (this also retries a failed upload).

Every release goes to GitHub with the jars and their SHA-256 sums. A mod with a `modrinth_project_id` in its
`gradle.properties` is also uploaded to Modrinth, one Modrinth version per jar, numbered
`<version>+<minecraft>-<loader>` (for example `0.6.0+1.21.1-neoforge`). That needs the `MODRINTH_TOKEN` repository secret: a Modrinth personal access token with
the "Create versions" scope. `modrinth_required` lists extra Modrinth project ids the mod needs.

## License

LGPL-3.0-only unless a mod says otherwise: see [`COPYING`](COPYING) and [`COPYING.LESSER`](COPYING.LESSER). A
mod with its own license sets `mod_license` and puts its license files in its folder.
