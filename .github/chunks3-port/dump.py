# TEMPORARY: dumps the Minecraft classes Chunks3 uses, per version, for the port. Removed before merge.
import json, os, re, subprocess, sys, urllib.request, zipfile
out = sys.argv[1]; os.makedirs(out, exist_ok=True)
VERSIONS = sys.argv[2].split(",") if len(sys.argv) > 2 else ["1.21.1", "1.21.11", "26.1.2", "26.3"]
here = os.path.dirname(os.path.abspath(__file__))
WANT = set()
for l in open(os.path.join(here, "classes.txt")):
    l = l.strip()
    if l: WANT.add(l.split("$")[0])
# Imports can name nested classes as a.b.Outer.Inner; keep each dotted prefix that starts with a capital.
outers = set()
for c in WANT:
    parts = c.split("."); acc = []
    for p in parts:
        acc.append(p)
        if p[:1].isupper(): outers.add(".".join(acc)); break
EXTRA = [l.strip() for l in open(os.path.join(here, "extra.txt")) if l.strip() and not l.startswith("#")]
def want(cls): return cls.split("$")[0] in outers or any(cls == e or cls.startswith(e + "$") or (e.endswith(".") and cls.startswith(e)) for e in EXTRA)
def get(url):
    with urllib.request.urlopen(url, timeout=300) as r: return r.read()
manifest = json.loads(get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
by_id = {v["id"]: v for v in manifest["versions"]}
for ver in VERSIONS:
    meta = json.loads(get(by_id[ver]["url"]))
    dl = meta["downloads"]
    found = set()
    if "client_mappings" in dl:
        text = get(dl["client_mappings"]["url"]).decode()
        keep, lines = False, []
        open(f"{out}/{ver}-classes.txt", "w").write("\n".join(sorted(l.split(" -> ")[0] for l in text.splitlines() if l and not l.startswith(("#", " ")))) + "\n")
        for line in text.splitlines():
            if line.startswith("#"): continue
            if not line.startswith(" "):
                cls = line.split(" -> ")[0]
                keep = want(cls)
                if keep: lines.append(cls); found.add(cls.split("$")[0])
            elif keep:
                body = line.strip().split(" -> ")[0]
                body = re.sub(r"^\d+:\d+:", "", body); body = re.sub(r":\d+:\d+$", "", body)
                lines.append("    " + body)
        open(f"{out}/{ver}.txt", "w").write("\n".join(lines) + "\n")
    else:
        jar = f"/tmp/{ver}.jar"
        open(jar, "wb").write(get(dl["client"]["url"]))
        with zipfile.ZipFile(jar) as z:
            open(f"{out}/{ver}-classes.txt", "w").write("\n".join(sorted(n[:-6].replace("/", ".") for n in z.namelist() if n.endswith(".class"))) + "\n")
            classes = sorted(n[:-6].replace("/", ".") for n in z.namelist() if n.endswith(".class") and want(n[:-6].replace("/", ".")))
        found = {c.split("$")[0] for c in classes}
        res = []
        for i in range(0, len(classes), 150):
            r = subprocess.run(["javap", "-p", "-cp", jar] + classes[i:i+150], capture_output=True, text=True)
            res.append(r.stdout + r.stderr)
        open(f"{out}/{ver}.txt", "w").write("".join(res))
    open(f"{out}/{ver}-missing.txt", "w").write("\n".join(sorted(outers - found)) + "\n")

# Iris and Sodium builds on Modrinth for each version and loader.
for slug in ["iris", "sodium"]:
    try:
        vs = json.loads(get(f"https://api.modrinth.com/v2/project/{slug}/version"))
        with open(f"{out}/modrinth-{slug}.txt", "w") as f:
            for v in vs: f.write(f'{v["id"]} {v["version_number"]} {",".join(v["loaders"])} {",".join(v["game_versions"])}\n')
    except Exception as e:
        open(f"{out}/modrinth-{slug}.txt", "w").write(f"failed: {e}\n")
