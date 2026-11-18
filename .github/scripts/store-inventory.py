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
