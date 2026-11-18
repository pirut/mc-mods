import os, json, hashlib, subprocess, tempfile
from pathlib import Path
import requests
api=requests.Session()
api.headers.update(Authorization=os.environ['MODRINTH_TOKEN'], **{'User-Agent':'pirut/mc-mods release workflow'})
base='https://api.modrinth.com/v2'
def call(method,path,**kwargs):
    r=api.request(method,base+path,timeout=60,**kwargs)
    if not r.ok: raise RuntimeError(f'{method} {path}: {r.status_code} {r.text[:800]}')
    return r.json() if r.content else None
user=call('GET','/user')
assert user['username'].lower()=='pirut'
owned=call('GET','/user/'+user['id']+'/projects')
def filehash(path): return hashlib.sha512(path.read_bytes()).hexdigest()
def update(id,data): return call('PATCH','/version/'+id,json=data)
def work(mod,title,slug,cat):
    earth=mod=='earth'
    folder=Path('mods')/mod
    source='https://github.com/pirut/earth-platform' if earth else f'https://github.com/pirut/mc-mods/tree/main/mods/{mod}'
    if earth: version='0.1.0-alpha.73'
    else:
        props=dict(line.split('=',1) for line in (folder/'gradle.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
        version=props['mod_version']
    dest=Path(tempfile.mkdtemp())
    repo='pirut/earth-platform' if earth else 'pirut/mc-mods'
    tag='combined-73' if earth else f'{mod}-v{version}'
    subprocess.run(['gh','release','download',tag,'-R',repo,'-D',dest,'-p','*.jar*','-p','SHA256SUMS.txt'],check=True)
    for sums in dest.glob('*.sha256' if earth else 'SHA256SUMS.txt'):
        subprocess.run(['sha256sum','-c',sums.name],cwd=dest,check=True)
    matches=[p for p in owned if p['slug']==slug or p['title']==title or p['source_url']==source]
    if len(matches)>1: raise ValueError('multiple matches')
    if matches: p=matches[0]
    else:
        data=dict(title=title,slug=slug,description=props['mod_description'],body=(folder/'README.md').read_text().replace('](docs/',f'](https://github.com/pirut/mc-mods/blob/main/mods/{mod}/docs/'),categories=[cat],client_side='required',server_side='required',project_type='mod',license_id='LGPL-3.0-only',source_url=source,issues_url='https://github.com/pirut/mc-mods/issues',is_draft=True,initial_versions=[])
        files={'data':(None,json.dumps(data),'application/json')}
        icon=next(folder.glob('common/src/main/resources/assets/*/icon.png'),None)
        if icon: files['icon']=(icon.name,icon.read_bytes(),'image/png')
        p=call('POST','/project',files=files)
    pid=p['id']
    existing=call('GET',f'/project/{pid}/version')
    for jar in sorted(dest.glob('*.jar')):
        fabric='fabric' in jar.name
        number=version+('+fabric' if fabric else '')
        v=next((v for v in existing if v['version_number']==number),None)
        if v:
            assert any(f['hashes']['sha512']==filehash(jar) for f in v['files'])
            if v['version_type']!='beta': update(v['id'],{'version_type':'beta'})
        else:
            deps=['P7dR8mSH'] if fabric else []
            if mod in ('cc-agent','seaworthy'): deps.append(call('GET','/project/'+('cc-tweaked' if mod=='cc-agent' else 'sable'))['id'])
            data=dict(project_id=pid,name=f'{title} {version} ({"Fabric" if fabric else "NeoForge"})',version_number=number,version_type='beta',game_versions=['1.21.1'],loaders=['fabric' if fabric else 'neoforge'],featured=False,dependencies=[dict(project_id=d,dependency_type='required') for d in deps],file_parts=['jar'],primary_file='jar',changelog=f'Current Minecraft 1.21.1 build. [Release notes](https://github.com/{repo}/releases/tag/{tag}).')
            v=call('POST','/version',files={'data':(None,json.dumps(data),'application/json'),'jar':(jar.name,jar.read_bytes(),'application/java-archive')})
        if p['status']=='draft':
            env={'seaworthy':'server_only_client_optional','bug-courier':'client_only','pirutlib':'client_or_server'}.get(mod,'client_and_server')
            r=api.patch('https://api.modrinth.com/v3/version/'+v['id'],json={'environment':env})
            r.raise_for_status()
        read=call('GET','/version/'+v['id'])
        assert read['version_type']=='beta' and any(f['hashes']['sha512']==filehash(jar) for f in read['files'])
        print('VERIFIED',slug,jar.name,v['id'],flush=True)
    patch={}
    if p.get('source_url')!=source: patch['source_url']=source
    if p['status']=='draft': patch['status']='processing'
    if patch: call('PATCH','/project/'+pid,json=patch)
    check=call('GET','/project/'+pid)
    print('PROJECT',slug,pid,check['status'],check['source_url'],flush=True)
for row in [('bloodfeud','Blood Feud','blood-feud','mobs'),('earth','Earth Platform','earth-platform','worldgen'),('pirutlib','Pirut Lib','pirut-lib','library'),('chunks3','Chunks3','chunks3','library'),('cc-agent','CC: Agent','cc-agent','technology'),('seaworthy','Create: Seaworthy','create-seaworthy','technology'),('bug-courier','Bug Courier','bug-courier','utility')]:
    try: work(*row)
    except Exception as e: print('BLOCKED',row[0],str(e),flush=True)
# Use the same artwork across new listings and point players to the canonical docs.
details={
    'bug-courier':('2219/499/639306203332588579','Bug Courier adds a Report a mod bug button to the pause menu. Choose a mod, review the prefilled report, and open its issue tracker to submit it. Install this mod on the client.'),
    'chunks3':('2219/261/639306141645325399','Chunks3 is a cubic terrain library. A consuming mod registers a dimension and generator. The library alone adds no playable dimension. Earth Platform build 73 uses its own engine and does not require this library.'),
    'create-seaworthy':('2219/500/639306203380246689','Adds hull sealing, displacement buoyancy and compartment flooding for Sable ships. Requires Sable 1.0.4 or newer. Install on the server and add it on clients for the interior water rendering.'),
    'pirut-lib':(None,'Shared loader hooks and game helpers for Pirut’s mods. Use the library version required by the consuming mod. Blood Feud 0.1.0-alpha.3 bundles its copy.'),
    'cc-agent':(None,'Use Claude Code or Codex with CC: Tweaked computers and turtles. Requires CC: Tweaked 1.120.0 or newer and a local signed-in coding agent. Install the mod on the server and clients.'),
}
for p in call('GET','/user/'+user['id']+'/projects'):
    if p['slug'] not in details: continue
    image,lead=details[p['slug']]
    if image and not p.get('icon_url'):
        img=requests.get('https://media.forgecdn.net/avatars/'+image+'.png')
        img.raise_for_status()
        r=api.patch(base+'/project/'+p['id']+'/icon?ext=png',data=img.content,headers={'Content-Type':'image/png'})
        r.raise_for_status()
    body=lead+'\n\nFor Minecraft Java Edition 1.21.1. Choose the JAR for your loader. Fabric installations require Fabric API.\n\n[Source and documentation]('+p['source_url']+')'
    call('PATCH','/project/'+p['id'],json={'body':body})
    result=call('GET','/project/'+p['id'])
    assert result['icon_url'] and result['body']==body
    print('LISTING',p['slug'],result['status'],result['source_url'])
