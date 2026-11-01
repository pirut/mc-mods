#!/usr/bin/env python3
"""Ports Earth Platform's engine changes into Chunks3.

Until Earth Platform depends on Chunks3, its cubic engine keeps changing in its own tree. This turns
the engine part of an Earth commit range into a patch on this library (package, mod id, mixin names and
-D flags renamed, Earth-only files left out) and applies it. Hunks that no longer fit, because the
library changed those lines (its loader wiring, for one), are left in .rej files to port by hand.

    python3 mods/chunks3/tools/port_from_earth.py ../earth-platform 59fdd45..main

Run it from the root of mc-mods with a clean work tree. Chunks3 0.1.0-alpha.1 was made from Earth
Platform at 59fdd45 (build 69), so the first port starts there; afterwards start from the last Earth
commit ported (write it in the commit message).
"""
import re
import subprocess
import sys

# Earth Platform path prefix -> Chunks3 path prefix. Anything not listed is Earth's own and is left out.
PATHS = [
    ('minecraft/src/main/java/io/earthplatform/minecraft/cubic/', 'mods/chunks3/common/src/main/java/io/github/pirut/chunks3/'),
    ('minecraft/src/test/java/io/earthplatform/minecraft/cubic/', 'mods/chunks3/common/src/test/java/io/github/pirut/chunks3/'),
    ('minecraft/src/main/java/io/earthplatform/minecraft/diagnostics/', 'mods/chunks3/common/src/main/java/io/github/pirut/chunks3/diagnostics/'),
    ('minecraft/src/test/java/io/earthplatform/minecraft/diagnostics/', 'mods/chunks3/common/src/test/java/io/github/pirut/chunks3/diagnostics/'),
    ('minecraft/src/main/java/io/earthplatform/minecraft/platform/', 'mods/chunks3/common/src/main/java/io/github/pirut/chunks3/platform/'),
    ('minecraft/src/neoforge/java/io/earthplatform/minecraft/neoforge/', 'mods/chunks3/neoforge/src/main/java/io/github/pirut/chunks3/neoforge/'),
    ('fabric/src/main/java/io/earthplatform/minecraft/fabric/', 'mods/chunks3/fabric/src/main/java/io/github/pirut/chunks3/fabric/'),
    ('minecraft/src/main/resources/earthplatform.cubic.mixins.json', 'mods/chunks3/common/src/main/resources/chunks3.mixins.json'),
    ('geography/src/main/java/io/earthplatform/geography/globe/NamedThreads.java', 'mods/chunks3/common/src/main/java/io/github/pirut/chunks3/util/NamedThreads.java'),
    ('geography/src/main/java/io/earthplatform/geography/globe/TickShare.java', 'mods/chunks3/common/src/main/java/io/github/pirut/chunks3/util/TickShare.java'),
    ('geography/src/main/java/io/earthplatform/geography/globe/DataWaits.java', 'mods/chunks3/common/src/main/java/io/github/pirut/chunks3/util/DataWaits.java'),
]
# Earth's own parts of the engine package, and loader glue that only wires Earth's features.
EARTH_ONLY = re.compile(r'/cubic/(earth|play|jet)/|/mixin/(CubicJet\w*|Earth\w*|CubicBoat|CubicCurrentMove|GliderTurn|CubicWaterTint)Mixin\.java$'
                        r'|/compat/SableWind\.java$|/cubic/CubicSettings\.java$|EarthPlatform(NeoForge|Fabric|FabricClient)\.java$'
                        r'|NeoForgeBuildCheck\.java$|/neoforge/mixin/(ElytraFlight|GliderChunkLoad)Mixin\.java$'
                        r'|/fabric/mixin/(ElytraWear|PresetEditor)Mixin\.java$|(FirstVisit|Cubic)Bench\.java$')
# Names saved in worlds and player data: kept as Earth Platform wrote them, so its worlds load unchanged.
SAVED = ['"earthplatform.cubic_home"', '"earthplatform_cubic_portals"', '"earthplatform:twilight_return"', '"earthplatform:aether_return"']


def rename(text):
    kept = {}
    for i, name in enumerate(SAVED):
        token = '\0SAVED%d\0' % i
        kept[token] = name
        text = text.replace(name, token)
    for old, new in [('io.earthplatform.minecraft.cubic', 'io.github.pirut.chunks3'),
                     ('io.earthplatform.minecraft.diagnostics', 'io.github.pirut.chunks3.diagnostics'),
                     ('io.earthplatform.minecraft.platform', 'io.github.pirut.chunks3.platform'),
                     ('io.earthplatform.minecraft.neoforge', 'io.github.pirut.chunks3.neoforge'),
                     ('io.earthplatform.minecraft.fabric', 'io.github.pirut.chunks3.fabric'),
                     ('io.earthplatform.geography.globe.NamedThreads', 'io.github.pirut.chunks3.util.NamedThreads'),
                     ('io.earthplatform.geography.globe.TickShare', 'io.github.pirut.chunks3.util.TickShare'),
                     ('io.earthplatform.geography.globe.DataWaits', 'io.github.pirut.chunks3.util.DataWaits'),
                     ('EarthPlatformMod.LOGGER', 'Cubic.LOGGER'), ('EarthPlatformMod.MOD_ID', 'Cubic.MOD_ID'),
                     ('earthplatform$', 'chunks3$'), ('earthplatform.cubic.mixins.json', 'chunks3.mixins.json'),
                     ('"earthplatform.', '"chunks3.'), ('-Dearthplatform.', '-Dchunks3.')]:
        text = text.replace(old, new)
    for token, name in kept.items():
        text = text.replace(token, name)
    return text


def map_path(path):
    if path == '/dev/null':
        return path
    for old, new in PATHS:
        if path.startswith(old) and not EARTH_ONLY.search(path):
            return new + path[len(old):]
    return None


def main():
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    earth, commits = sys.argv[1], sys.argv[2]
    diff = subprocess.run(['git', '-C', earth, 'diff', '--no-renames', '--binary', commits],
                          check=True, capture_output=True, text=True).stdout
    out, keep, skipped = [], False, []
    for chunk in re.split(r'(?m)^(?=diff --git )', diff):
        if not chunk.startswith('diff --git '):
            continue
        a, b = re.match(r'diff --git a/(\S+) b/(\S+)', chunk).groups()
        target = map_path(b) or map_path(a)
        if target is None:
            skipped.append(b)
            continue
        chunk = chunk.replace('a/' + a, 'a/' + target).replace('b/' + b, 'b/' + target)
        out.append(rename(chunk))
    if not out:
        print('No engine changes in', commits)
        return
    patch = ''.join(out)
    print('Porting %d engine files (%d Earth-only files left out)' % (len(out), len(skipped)))
    result = subprocess.run(['git', 'apply', '--reject', '--whitespace=nowarn'], input=patch, text=True)
    if result.returncode != 0:
        print('Some hunks did not apply: each is in a .rej file next to its file. Port those by hand, delete the .rej\n'
              'files, then build and test (./gradlew :chunks3:build).')
        sys.exit(result.returncode)


if __name__ == '__main__':
    main()
