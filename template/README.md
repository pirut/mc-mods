# Example Mod (template)

The starting point for a new mod. It builds with the rest of the repo so it never goes stale, but it is never
released.

## Making a new mod

1. Copy this folder to `mods/<your-mod>` (the folder name is the mod's name in tags, such as
   `<your-mod>-v0.1.0`, and in Gradle paths, such as `:<your-mod>:build`).
2. In `gradle.properties`, set `mod_id`, `mod_name`, `mod_group`, `mod_description` and `mod_version`.
3. Rename the `io.github.pirut.examplemod` packages and the entry points, and update the entry point in
   `fabric/src/main/resources/fabric.mod.json`. Rename `assets/examplemod`.
4. Fix the links in `neoforge.mods.toml` and `fabric.mod.json` to `.../tree/main/mods/<your-mod>`.
5. Delete `fabric/` or `neoforge/` if the mod is only for one loader.
6. Add the mod to the table in the root `README.md`.

That's all: `settings.gradle` picks up every folder in `mods/` that has a `gradle.properties`.

## Layout

- `common/`: code and assets for both loaders. It is compiled into each loader's jar, not built on its own,
  so it may only use Minecraft classes that both loaders see the same way (Mojang names).
- `neoforge/`, `fabric/`: the entry points and loader-only code.
- `gradle.properties`: the mod's id, version and metadata. Anything here fills `${...}` in
  `neoforge.mods.toml` and `fabric.mod.json`, and can override the repo's shared versions.
- `CHANGELOG.md`: one `## <version>` section per release; the release notes come from it.
