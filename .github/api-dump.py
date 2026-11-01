# TEMPORARY: see api-dump.yml.
import json, os, re, subprocess, sys, urllib.request, zipfile
out = sys.argv[1]; os.makedirs(out, exist_ok=True)
VERSIONS = ["1.21.1", "1.21.4", "1.21.5", "1.21.8", "1.21.10", "1.21.11", "26.1", "26.1.2", "26.2", "26.3"]
PREFIXES = ["net/minecraft/client/gui/", "net/minecraft/client/Minecraft", "net/minecraft/client/KeyMapping",
            "net/minecraft/client/User", "net/minecraft/client/KeyboardHandler", "net/minecraft/Util", "net/minecraft/util/Util",
            "net/minecraft/SharedConstants", "net/minecraft/WorldVersion", "com/mojang/blaze3d/platform/GlUtil",
            "com/mojang/blaze3d/platform/InputConstants", "net/minecraft/client/input/", "net/minecraft/util/FormattedCharSequence",
            "net/minecraft/util/ARGB", "net/minecraft/network/chat/Component", "net/minecraft/util/Util", "net/minecraft/client/gui/render/"]
def get(url):
    with urllib.request.urlopen(url, timeout=120) as r: return r.read()
def want(name): return any(name.startswith(p) for p in PREFIXES)
manifest = json.loads(get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
with open(f"{out}/releases.txt", "w") as f:
    for v in manifest["versions"]:
        if v["type"] == "release": f.write(f'{v["id"]} {v["releaseTime"]}\n')
for url, name in [("https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml", "neoforge-versions.txt"),
                  ("https://meta.fabricmc.net/v2/versions/loader", "fabric-loader.json"),
                  ("https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml", "fabric-api-versions.txt"),
                  ("https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml", "loom-versions.txt"),
                  ("https://maven.neoforged.net/releases/net/neoforged/moddev/net.neoforged.moddev.gradle.plugin/maven-metadata.xml", "mdg-versions.txt"),
                  ("https://maven.parchmentmc.org/org/parchmentmc/data/maven-metadata.xml", "parchment.txt")]:
    try:
        data = get(url).decode()
        if name.endswith(".txt"): data = "\n".join(re.findall(r"<version>([^<]+)</version>", data)) or data
        else: data = "\n".join(e["version"] + (" stable" if e.get("stable") else "") for e in json.loads(data)[:40])
        open(f"{out}/{name}", "w").write(data + "\n")
    except Exception as e:
        open(f"{out}/{name}", "w").write(f"failed: {e}\n")
for pmc in ["1.21.1", "1.21.4", "1.21.5", "1.21.8", "1.21.10", "1.21.11"]:
    try:
        data = get(f"https://maven.parchmentmc.org/org/parchmentmc/data/parchment-{pmc}/maven-metadata.xml").decode()
        open(f"{out}/parchment-{pmc}.txt", "w").write("\n".join(re.findall(r"<version>([^<]+)</version>", data)) + "\n")
    except Exception as e:
        open(f"{out}/parchment-{pmc}.txt", "w").write(f"failed: {e}\n")
by_id = {v["id"]: v for v in manifest["versions"]}
for ver in VERSIONS:
    if ver not in by_id: open(f"{out}/{ver}-MISSING", "w").write("not in manifest\n"); continue
    meta = json.loads(get(by_id[ver]["url"]))
    dl = meta["downloads"]
    if "client_mappings" in dl:
        text = get(dl["client_mappings"]["url"]).decode()
        keep, lines = False, []
        for line in text.splitlines():
            if line.startswith("#"): continue
            if not line.startswith(" "):
                cls = line.split(" -> ")[0]
                keep = want(cls.replace(".", "/"))
                if keep: lines.append(cls)
            elif keep:
                body = line.strip().split(" -> ")[0]
                body = re.sub(r"^\d+:\d+:", "", body); body = re.sub(r":\d+:\d+$", "", body)
                lines.append("    " + body)
        open(f"{out}/{ver}.txt", "w").write("\n".join(lines) + "\n")
    else:
        jar = f"/tmp/{ver}.jar"
        open(jar, "wb").write(get(dl["client"]["url"]))
        with zipfile.ZipFile(jar) as z:
            classes = sorted(n[:-6] for n in z.namelist() if n.endswith(".class") and want(n))
        res = []
        for i in range(0, len(classes), 200):
            r = subprocess.run(["javap", "-p", "-cp", jar] + [c.replace("/", ".") for c in classes[i:i+200]],
                               capture_output=True, text=True)
            res.append(r.stdout + r.stderr)
        open(f"{out}/{ver}.txt", "w").write("".join(res))
