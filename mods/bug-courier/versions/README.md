# Bug Courier's Minecraft versions

One file per Minecraft version Bug Courier is built for (`minecraft_targets` in `../gradle.properties`). Each
sets what differs for that version on top of the repo's `versions/<mc>.properties`:

- `fabric_minecraft`: the Minecraft versions the Fabric jar accepts.
- `extra_src`: the source folders that differ between versions.

The source folders:

| Folder | What | Versions |
| --- | --- | --- |
| `common/src/mc<version>` | `Mc`: every call into Minecraft that changed between versions, one copy per version | each |
| `common/src/gui-graphics` | `BaseScreen` and `GraphicsCanvas`: screens draw with `GuiGraphics` | 1.21.1 to 1.21.11 |
| `common/src/gui-26` | `BaseScreen`, `ExtractorCanvas` and `TextView`: screens draw with `GuiGraphicsExtractor` | 26.x |
| `common/src/textview-1.21.1` | `TextView` on `AbstractScrollWidget` | 1.21.1 |
| `common/src/textview-1.21.4` | `TextView` on `AbstractTextAreaWidget` | 1.21.4 to 1.21.11 |
| `fabric/src/fabric-1.21`, `fabric/src/fabric-26` | `FabricCompat`: Fabric API names that changed in 26.1 | 1.21.x, 26.x |

To add a Minecraft version: add it to `minecraft_targets`, copy the nearest `versions/<mc>.properties` and
`common/src/mc<version>` folder, build with `./gradlew :bug-courier:build -Pmc=<mc>`, and fix what doesn't compile.
