# Chunks3 for Minecraft 1.21.11

This tree is the 1.21.11 engine port. The 1.21.1 sources, including the Cubic world type, stay in the parent
`mods/chunks3` folders. `versions/1.21.11.properties` sets `src_root=mc12111`, so a 1.21.11 build compiles this
tree and a 1.21.1 build does not.

The Cubic world type is not in this tree. It is not on the 1.21.11 Create World screen.

Sources are written for 1.21.1 with `//#if` branches for 1.21.11. `preprocess.gradle` selects the branch at
build time. See that file for the directive syntax.
