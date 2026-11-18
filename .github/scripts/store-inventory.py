import os
import requests
api = requests.Session()
api.headers.update(Authorization=os.environ['MODRINTH_TOKEN'])
base = 'https://api.modrinth.com/v2'
def get(path):
    response = api.get(base + path)
    response.raise_for_status()
    return response.json()
user = get('/user')
print('account', user['username'])
for project in get('/user/' + user['id'] + '/projects'):
    print(project['id'], project['slug'], project['status'], project['source_url'])
    for version in get('/project/' + project['id'] + '/version')[:4]:
        print(version['id'], version['version_number'], version['version_type'], [(f['filename'],f['hashes']['sha512']) for f in version['files']])
