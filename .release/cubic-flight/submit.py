import hashlib,json,os
from pathlib import Path
import requests
os.chdir(Path(__file__).resolve().parent)
jar=next(Path('.').glob('*.jar'))
raw=jar.read_bytes()
assert hashlib.sha256(raw).hexdigest()=='0dcb306ad1a9800f91a1a02fc337cc4423256c23c508ff17c259ca1c2962573a'
body=Path('description.md').read_text()
name='Cubic Flight 0.2.0-local (NeoForge)'
def mr():
    session=requests.Session()
    session.headers.update(Authorization=os.environ['MODRINTH_TOKEN'])
    def req(method,path,v=2,**kwargs):
        r=session.request(method,f'https://api.modrinth.com/v{v}'+path,timeout=60,**kwargs)
        if not r.ok: raise ValueError(r.status_code,r.text[:600])
        return r.json() if r.content else None
    u=req('GET','/user')
    assert u['username'].lower()=='pirut'
    projects=req('GET','/user/'+u['id']+'/projects')
    matches=[p for p in projects if p['slug']=='cubic-flight' or p['title']=='Cubic Flight']
    assert len(matches)<=1
    def part(data):return (None,json.dumps(data),'application/json')
    if matches:
        p=matches[0]
        assert p['slug']=='cubic-flight' and p['title']=='Cubic Flight'
    else:
        p=req('POST','/project',files={'data':part(dict(title='Cubic Flight',slug='cubic-flight',description="Walk on cubic Earth terrain from an ultraFLIGHT chart and return to your ship at the corresponding map coordinates.",body=body,categories=['adventure','transportation'],client_side='required',server_side='required',project_type='mod',license_id='LicenseRef-All-Rights-Reserved',is_draft=True,initial_versions=[])),'icon':('icon.png',Path('icon.png').read_bytes(),'image/png')})
    pid=p['id']
    versions=req('GET',f'/project/{pid}/version')
    if versions:
        assert len(versions)==1
        version=versions[0]
    else:
        version=req('POST','/version',files={'data':part(dict(project_id=pid,name=name,version_number='0.2.0-local',version_type='beta',game_versions=['1.21.1'],loaders=['neoforge'],featured=False,dependencies=[dict(file_name='ultraflight-0.3.6.jar',dependency_type='required')],file_parts=['jar'],primary_file='jar',changelog=body)),'jar':(jar.name,raw,'application/java-archive')})
    vid=version['id']
    v=req('GET','/version/'+vid)
    assert v['version_type']=='beta' and v['files'][0]['hashes']['sha512']==hashlib.sha512(raw).hexdigest() and v['game_versions']==['1.21.1'] and v['loaders']==['neoforge']
    if p['status']=='draft':
        req('PATCH','/version/'+vid,v=3,json={'environment':'client_and_server'})
        req('PATCH','/project/'+pid,json={'status':'processing'})
    p=req('GET','/project/'+pid)
    print('Verified Modrinth',pid,vid,p['status'],v['files'][0]['filename'],p['license']['id'],flush=True)
def cf():
    s=requests.Session()
    s.headers.update({'X-Api-Token':os.environ['CURSEFORGE_TOKEN']})
    base='https://minecraft.curseforge.com/api/'
    def get(p):
        r=s.get(base+p,timeout=45);r.raise_for_status();return r.json()
    types=get('game/version-types')
    versions=get('game/versions')
    tags=[]
    for n,slug in [('1.21.1','minecraft-'),('NeoForge','modloader'),('Java 21','java'),('Client','environment'),('Server','environment')]:
        ids=[t['id'] for t in types if t['slug'].startswith(slug)]
        tags.append(next(v['id'] for v in versions if v['name']==n and v['gameVersionTypeID'] in ids))
    r=s.post(base+'projects/1815062/upload-file',files={'metadata':(None,json.dumps(dict(displayName=name,changelog=body,changelogType='markdown',releaseType='beta',gameVersions=tags)),'application/json'),'file':(jar.name,raw,'application/java-archive')},timeout=60)
    r.raise_for_status()
    print('Submitted CurseForge',r.json())
{'modrinth':mr,'curseforge':cf}[os.environ['STORE']]()
