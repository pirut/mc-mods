# mc-mods

JR Bussard's small Minecraft mods, in one repo. Each mod lives in its own folder under [`mods/`](mods), has its
own version and changelog, and is released on its own.

## Mods

| Mod | What it does | Loaders | Get it |
| --- | --- | --- | --- |
| [CC: Agent](mods/cc-agent) | Lets Claude Code, Codex or any MCP client write programs for and drive CC: Tweaked computers and turtles. | NeoForge, Fabric | [Releases](https://github.com/pirut/mc-mods/releases?q=cc-agent) |
| [Bug Courier](mods/bug-courier) | A clear in-game way for players to report bugs to the mods they play. | NeoForge, Fabric | [Releases](https://github.com/pirut/mc-mods/releases?q=bug-courier) |
| [Create: Seaworthy](mods/seaworthy) | Watertight hulls for Create Aeronautics and Sable ships: dry hulls below the waterline, displacement buoyancy, and compartments that flood through holes. | NeoForge, Fabric | [Releases](https://github.com/pirut/mc-mods/releases?q=seaworthy) |
| [Chunks3](mods/chunks3) | Cubic chunks as a library: other mods register dimensions up to 32,768 blocks tall, with a distant terrain renderer. The engine of Earth Platform. | NeoForge, Fabric | [Releases](https://github.com/pirut/mc-mods/releases?q=chunks3) |
| [Earth Platform](https://github.com/pirut/earth-platform) | The real Earth in Minecraft at true scale, with real terrain and climate and a far view to the horizon. Lives in its own repo. | NeoForge, Fabric | [Modrinth](https://modrinth.com/mod/earth-platform) |

All mods are for Minecraft 1.21.1. Issues for any mod in this repo go to
[this repo's issues](https://github.com/pirut/mc-mods/issues); say which mod in the title.

## Layout

```
mods/<mod>/           one folder per mod
  gradle.properties   id, name, version, metadata, Modrinth project id
  CHANGELOG.md        one "## <version>" section per release
  common/             code for both loaders (compiled into each jar)
  neoforge/ fabric/   entry points and loader-only code
  example/            optional: a never-released example mod that uses it (see mods/chunks3)
build-logic/          shared Gradle plugins: mcmods.neoforge, mcmods.fabric, mcmods.library
common-lib/           internal library a mod can bundle into its jars (uses_common_lib=true)
template/             a buildable example mod to copy for a new one
```

The repo's `gradle.properties` holds the versions every mod shares (Minecraft, NeoForge, Fabric Loader and
API, Parchment). A mod can override any of them in its own `gradle.properties`, and every value there fills
`${...}` in its `neoforge.mods.toml` and `fabric.mod.json`.

## Building

Java 21.

```sh
./gradlew build                          # every mod
./gradlew :cc-agent:build                # one mod
./gradlew :cc-agent:neoforge:runClient   # a dev game (also :fabric:runClient, runServer)
```

Jars land in `mods/<mod>/<loader>/build/libs`, named `<mod_id>-<loader>-<minecraft>-<version>.jar`.

## A new mod

Copy [`template/`](template) to `mods/<name>` and follow its README. Gradle picks the folder up by itself.

## Releasing

Each mod is tagged on its own as `<folder>-v<version>`, for example `cc-agent-v0.5.1`, and only that mod is
built and released.

1. Bump `mod_version` in the mod's `gradle.properties`.
2. Add a `## <version>` section to its `CHANGELOG.md`. It becomes the release notes.
3. Merge to `main`. The Release workflow releases every mod whose version has no release yet.

You can also push a tag `<folder>-v<version>` yourself, or run the Release workflow from the Actions tab with
the mod's folder name (this also retries a failed upload).

Every release goes to GitHub with the jars and their SHA-256 sums. A mod with a `modrinth_project_id` in its
`gradle.properties` is also uploaded to Modrinth (the NeoForge jar as `<version>`, the Fabric jar as
`<version>+fabric`). That needs the `MODRINTH_TOKEN` repository secret: a Modrinth personal access token with
the "Create versions" scope. `modrinth_required` lists extra Modrinth project ids the mod needs.

## License

LGPL-3.0-only unless a mod says otherwise: see [`COPYING`](COPYING) and [`COPYING.LESSER`](COPYING.LESSER). A
mod with its own license sets `mod_license` and puts its license files in its folder.
