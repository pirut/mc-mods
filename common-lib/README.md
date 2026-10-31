# common-lib

Internal code shared by the mods in this repo. It is not a mod and is never released on its own: a mod that
sets `uses_common_lib=true` in its `gradle.properties` gets it nested inside its jars (`jarJar` on NeoForge,
`include` on Fabric), so players never need an extra download.

Rules:

- Plain Java only. No Minecraft, NeoForge or Fabric classes, so one jar works on both loaders.
- When several installed mods carry it, the loader keeps the newest copy. So add things, and don't rename or
  remove them; bump `common_lib_version` in the root `gradle.properties` with every change.
- Move code here once two mods need it, not before.
