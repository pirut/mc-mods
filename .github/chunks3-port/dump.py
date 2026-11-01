# TEMPORARY: dumps the Minecraft classes Chunks3 uses, per version, for the port. Removed before merge.
import json, os, re, subprocess, sys, urllib.request, zipfile
out = sys.argv[1]; os.makedirs(out, exist_ok=True)
VERSIONS = ["1.21.1", "1.21.4", "1.21.5", "1.21.8", "1.21.10", "1.21.11", "26.1.2", "26.2", "26.3"]
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
def want(cls): return cls.split("$")[0] in outers
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
            classes = sorted(n[:-6].replace("/", ".") for n in z.namelist() if n.endswith(".class") and want(n[:-6].replace("/", ".")))
        found = {c.split("$")[0] for c in classes}
        res = []
        for i in range(0, len(classes), 150):
            r = subprocess.run(["javap", "-p", "-cp", jar] + classes[i:i+150], capture_output=True, text=True)
            res.append(r.stdout + r.stderr)
        open(f"{out}/{ver}.txt", "w").write("".join(res))
    open(f"{out}/{ver}-missing.txt", "w").write("\n".join(sorted(outers - found)) + "\n")
