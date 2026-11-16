# Pirut Lib

The shared library behind Pirut's mods. It does the things every mod would otherwise build again: hooking into
NeoForge and Fabric the same way, title cards, world save data and finding places for mobs to stand. Mods nest it in
their own jars, so players never install it by hand.

## Using it from a mod in this repo

Add `uses_pirut_lib=true` to the mod's `gradle.properties`. The build compiles against it, nests it in both loaders'
jars, and loads it in dev runs. Add it as a required dependency in `neoforge.mods.toml` (`modId = "pirutlib"`) and
`fabric.mod.json` (`"pirutlib": ">=0.1.0-alpha.1"`).

## What's in it

- `Loader`: the running loader's name, config and game folders, whether this is a player's game, and installed mods'
  versions. Replaces each mod's own `Platform` for these.
- `ServerEvents`: server tick, start and stop; entities loading (return false to drop one); deaths, players included;
  every hit on a living entity (ignore it, or change its damage); command registration. The same on both loaders,
  so a mod that only needs these has a one-line entry point per loader. See `mods/bloodfeud` for an example.
- `Titles`: title cards with a subtitle, and action bar lines.
- `Stores`: a world's `SavedData`, with the data fix detail that makes plain vanilla and Fabric crash handled.
- `Spots`: whether a mob can stand somewhere, and a spot near a place on the surface or underground.

The plain-Java helpers that need no game classes (such as `PropertiesFile` for config files) stay in
[common-lib](../../common-lib), which Pirut Lib also carries.

## Rules for changing it

The newest copy any installed mod carries is the one that loads, so changes must stay backward compatible: add
methods and classes, don't rename or remove them, and bump `mod_version` with every change.
