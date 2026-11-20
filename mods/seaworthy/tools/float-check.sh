#!/usr/bin/env bash
# In-game float check: starts the NeoForge dev server with Sable, floats a test hull in a real ocean, floods it and
# checks the heights. Exits non-zero if a check fails. Takes about 8 minutes and needs JAVA_HOME set to Java 21.
#
#   mods/seaworthy/tools/float-check.sh
#
# It uses its own game folder and world (neoforge/run-floatcheck, deleted first), and downloads Sable from the same
# maven the build uses. It isn't part of `gradle build`: the physics engine makes it slow.
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../../.." && pwd)"
SABLE="$(grep '^sable_version=' "$HERE/../gradle.properties" | cut -d= -f2)"
RUN="$HERE/../neoforge/run-floatcheck"
LOG="$RUN/server.log"
PORT=25585
PASS=floatcheck
# Where a sub-level's first plot starts, in Sable's plot grid.
PLOT=20481032
EXPECT_SEALED=64.75
EXPECT_FLOODED=63.1
TOLERANCE=0.25
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
GRADLE_INIT="$(mktemp)"
FAILED=0

rcon() { "$JAVA" "$HERE/Rcon.java" "$PORT" "$PASS" "$@"; }
check() { # name value expected tolerance
    if awk -v v="$2" -v e="$3" -v t="$4" 'BEGIN { d = v - e; if (d < 0) d = -d; exit !(d <= t) }'; then
        echo "PASS $1: $2 (expected $3)"
    else
        echo "FAIL $1: $2 (expected $3 +/- $4)"
        FAILED=1
    fi
}
height() { rcon 'sable info @l' | grep -E 'Position' | sed -E 's/.*Position: *//' | awk '{print $2}'; }
flooded() { rcon 'seaworthy status' | grep -E '#1' | sed -E 's/.*#1: *([0-9]+)%.*/\1/'; }

finish() {
    rcon stop >/dev/null 2>&1
    [ -n "${SERVER:-}" ] && wait "$SERVER" 2>/dev/null
    rm -f "$GRADLE_INIT"
}
trap finish EXIT

rm -rf "$RUN"
mkdir -p "$RUN/mods"
JAR="$RUN/mods/sable-neoforge-1.21.1-$SABLE.jar"
curl -fsSL -o "$JAR" "https://maven.ryanhcode.dev/releases/dev/ryanhcode/sable/sable-neoforge-1.21.1/$SABLE/sable-neoforge-1.21.1-$SABLE.jar" \
    || { echo "Couldn't download Sable $SABLE"; exit 2; }
cat > "$RUN/server.properties" <<EOF
enable-rcon=true
rcon.port=$PORT
rcon.password=$PASS
server-ip=127.0.0.1
server-port=25586
online-mode=false
EOF
cat > "$GRADLE_INIT" <<EOF
allprojects {
    afterEvaluate { p ->
        if (p.path == ':seaworthy:neoforge') p.neoForge.runs.server.gameDirectory = p.file('run-floatcheck')
    }
}
EOF
(cd "$ROOT" && ./gradlew --init-script "$GRADLE_INIT" :seaworthy:neoforge:runServer --console=plain >"$LOG" 2>&1) &
SERVER=$!

for _ in $(seq 1 180); do
    grep -qE 'Done \([0-9.]+s\)!' "$LOG" && break
    kill -0 "$SERVER" 2>/dev/null || { tail -20 "$LOG"; echo "The server stopped while starting"; exit 2; }
    sleep 2
done
grep -qE 'Done \([0-9.]+s\)!' "$LOG" || { echo "The server didn't start"; exit 2; }

OCEAN="$(rcon 'locate biome minecraft:deep_ocean' | grep -E 'nearest')"
X="$(echo "$OCEAN" | sed -E 's/.*\[(-?[0-9]+), [^,]+, (-?[0-9]+)\].*/\1/')"
Z="$(echo "$OCEAN" | sed -E 's/.*\[(-?[0-9]+), [^,]+, (-?[0-9]+)\].*/\2/')"
[ -n "$X" ] || { echo "No ocean found: $OCEAN"; exit 2; }
rcon "forceload add $((X - 16)) $((Z - 16)) $((X + 32)) $((Z + 16))" >/dev/null
sleep 15

# A 15 x 11 oak hull with 8-high walls and an open top, built sitting in the sea so it leaves a hole behind.
rcon "fill $X 60 $((Z - 5)) $((X + 14)) 68 $((Z + 5)) minecraft:oak_planks hollow" >/dev/null
sleep 3
rcon "fill $((X + 1)) 68 $((Z - 4)) $((X + 13)) 68 $((Z + 4)) minecraft:air" >/dev/null
sleep 3
rcon "sable assemble area $X 60 $((Z - 5)) $((X + 14)) 68 $((Z + 5))" >/dev/null
sleep 30
check "sealed hull floats" "$(height)" "$EXPECT_SEALED" "$TOLERANCE"
check "sealed hull is dry" "$(flooded)" 0 0

rcon "setblock $((PLOT + 7)) 128 $((PLOT + 5)) minecraft:air" >/dev/null
sleep 60
PART="$(flooded)"
if [ "${PART:-0}" -gt 0 ] && [ "${PART:-0}" -lt 30 ]; then
    echo "PASS flooding is gradual: $PART% after 60s"
else
    echo "FAIL flooding is gradual: ${PART:-none}% after 60s"
    FAILED=1
fi
sleep 240
check "flooded hull settles" "$(height)" "$EXPECT_FLOODED" "$TOLERANCE"

if grep -qE 'Seaworthy stopped|Couldn.t work out' "$LOG"; then
    echo "FAIL Seaworthy logged an error:"
    grep -E 'Seaworthy stopped|Couldn.t work out' "$LOG" | head -5
    FAILED=1
else
    echo "PASS no Seaworthy errors in the log"
fi
[ "$FAILED" = 0 ] && echo "All float checks passed." || echo "Some float checks failed; see $LOG"
exit "$FAILED"
