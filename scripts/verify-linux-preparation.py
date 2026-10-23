"""Offline checks only: no Linux installation, network requests or database writes.
Requires an already built JAR/release, current Java 8, and Git Bash on Windows (or Bash on Linux).
"""
from pathlib import Path
import subprocess,os,json,zipfile,tempfile,hashlib,tarfile,configparser,re,shutil
ROOT=Path(__file__).resolve().parents[1]
checks=[]
def check(name,condition):
 if not condition:raise AssertionError(name)
 checks.append(name);print('PASS',name)
bash=os.environ.get('UPSELF_BASH') or (str(Path(os.environ.get('ProgramFiles',r'C:\Program Files'))/'Git/bin/bash.exe') if os.name=='nt' else shutil.which('bash'))
check('bash-available',bash and Path(bash).is_file())
for script in sorted((ROOT/'deploy/linux').glob('*.sh')):
 check(script.name+'-LF-no-BOM',b'\r' not in script.read_bytes() and not script.read_bytes().startswith(b'\xef\xbb\xbf'))
 check(script.name+'-syntax',subprocess.run([bash,'-n',str(script)],capture_output=True).returncode==0)
releases=sorted((ROOT/'.local/linux-release').glob('*.tar.gz'))
check('release-available',bool(releases));archive=releases[-1];release=archive.with_suffix('').with_suffix('')
check('archive-sidecar-sha256',archive.with_name(archive.name+'.sha256').read_text().split()[0]==hashlib.sha256(archive.read_bytes()).hexdigest())
with tarfile.open(archive,'r:gz') as t:
 members=t.getmembers()
 check('archive-no-absolute-or-traversal-paths',all(not m.name.startswith('/') and '..' not in Path(m.name).parts and not m.issym() and not m.islnk() for m in members))
 check('release-top-level-allowlist',all(m.name.lstrip('./').split('/')[0] in {'app','web','linux','release.json','SHA256SUMS',''} for m in members))
 check('no-data-or-local-config-entries',not any(re.search(r'(^|/)(config|migration|uploads|node_modules|application-local\.yml|learning-demo\.json)(/|$)',m.name) for m in members))
for line in (release/'SHA256SUMS').read_text().splitlines():
 digest,name=line.split('  ',1)
 check('checksum-'+name,hashlib.sha256((release/name).read_bytes()).hexdigest()==digest)
# Run only artifact mode on the real release. Runtime mode must refuse this Windows host.
r=subprocess.run([bash,str(release/'linux/preflight.sh'),'--artifact-only'],capture_output=True,text=True)
check('artifact-preflight',r.returncode==0)
if os.name=='nt':
 r=subprocess.run([bash,str(release/'linux/preflight.sh'),'--runtime'],capture_output=True,text=True)
 check('runtime-refuses-Windows',r.returncode!=0 and 'Linux x86_64' in r.stderr)
 r=subprocess.run([bash,str(ROOT/'deploy/linux/backup.sh')],capture_output=True,text=True)
 check('backup-refuses-Windows-without-writing',r.returncode!=0 and 'Linux' in r.stderr)
work=ROOT/'.local/linux-prep-verification';work.mkdir(exist_ok=True)
# Real checksum failure test in a disposable fixture, not the delivered release.
with tempfile.TemporaryDirectory(dir=work) as td:
 q=Path(td)
 for d in ['linux','app','web']:(q/d).mkdir()
 shutil.copy2(release/'linux/preflight.sh',q/'linux/preflight.sh')
 (q/'app/up-self-admin.jar').write_text('fixture');(q/'web/index.html').write_text('fixture');(q/'release.json').write_text('{}')
 (q/'SHA256SUMS').write_text('0'*64+'  app/up-self-admin.jar\n')
 r=subprocess.run([bash,str(q/'linux/preflight.sh'),'--artifact-only'],capture_output=True,text=True)
 check('tampered-release-rejected',r.returncode!=0 and 'checksums do not match' in r.stderr)
# Parse YAML using the project's existing runtime libraries, adding no dependencies.
tools=json.loads((ROOT/'.local/development-tools.json').read_text(encoding='utf-8-sig'))
with tempfile.TemporaryDirectory(dir=work) as td:
 q=Path(td)
 with zipfile.ZipFile(release/'app/up-self-admin.jar') as jar:
  check('jar-has-V1-to-V6',all(any('/V'+str(v)+'__' in n for n in jar.namelist()) for v in range(1,7)))
  check('jar-has-no-local-config',not any('application-local' in n or 'local-credentials' in n for n in jar.namelist()))
  check('jar-has-new-progress-mapper',any('up-self-biz-' in n for n in jar.namelist()))
  for n in jar.namelist():
   if n.startswith('BOOT-INF/lib/') and ('snakeyaml-' in n or 'fastjson2-' in n):(q/Path(n).name).write_bytes(jar.read(n))
  biz=next(n for n in jar.namelist() if n.startswith('BOOT-INF/lib/up-self-biz-'))
  import io
  with zipfile.ZipFile(io.BytesIO(jar.read(biz))) as z:check('nested-biz-has-progress-SQL','mapper/learning/ProgressMapper.xml' in z.namelist())
 source=q/'ReadYaml.java';source.write_text('import java.io.*;import org.yaml.snakeyaml.Yaml;import com.alibaba.fastjson2.JSON;public class ReadYaml{public static void main(String[] a)throws Exception{try(InputStream i=new FileInputStream(a[0])){System.out.print(JSON.toJSONString(new Yaml().load(i)));}}}',encoding='utf-8')
 sep=';' if os.name=='nt' else ':';java=Path(tools['javaHome'])/'bin';exe='.exe' if os.name=='nt' else ''
 subprocess.run([str(java/('javac'+exe)),'-cp',str(q/'*'),str(source)],check=True,capture_output=True)
 def read_yaml(file):return json.loads(subprocess.check_output([str(java/('java'+exe)),'-cp',str(q)+sep+str(q/'*'),'ReadYaml',str(file)]))
 config=read_yaml(ROOT/'deploy/linux/application-prod.yml.example')
 check('druid-master-configuration',set(['url','username','password']).issubset(config['spring']['datasource']['druid']['master']))
 check('separate-flyway-credentials','UPSELF_MIGRATION_PASSWORD' in config['spring']['flyway']['password'])
 check('bootstrap-off-by-default',config['bootstrap']['enabled']=='${UPSELF_BOOTSTRAP_ENABLED:false}')
 check('two-factor-remains-off',config['custom']['totp']['enabled'] is False)
 check('private-upload-paths',config['learning']['storage-path']=='/var/lib/up-self/uploads' and config['assignment']['storage-path'].endswith('/uploads/assignments'))
 local=read_yaml(ROOT/'config/application-local.yml')
 sensitive=[]
 def collect(node,key=''):
  if isinstance(node,dict):
   for k,v in node.items():collect(v,str(k))
  elif isinstance(node,str) and len(node)>=10 and re.search(r'password|secret|encryptKey',key,re.I):sensitive.append(node.encode())
 collect(local)
 # Explicitly include bootstrap password values (their keys are account names).
 sensitive.extend(str(v).encode() for v in local.get('bootstrap',{}).get('passwords',{}).values() if len(str(v))>=10)
 payloads=[f.read_bytes() for f in release.rglob('*') if f.is_file() and f.suffix!='.jar']
 with zipfile.ZipFile(release/'app/up-self-admin.jar') as z:payloads.extend(z.read(n) for n in z.namelist() if not n.endswith('/') and not n.startswith('BOOT-INF/lib/'))
 check('no-actual-long-local-secrets-in-release',not any(value in content for value in sensitive for content in payloads))
unit=configparser.ConfigParser(interpolation=None);unit.optionxform=str;unit.read(ROOT/'deploy/linux/up-self.service.example',encoding='utf-8')
check('systemd-nonroot-service',unit['Service']['User']=='upself' and unit['Service']['ExecStart']=='/bin/bash /opt/up-self/current/linux/run.sh')
check('systemd-external-envfile',unit['Service']['EnvironmentFile']=='/etc/up-self/up-self.env')
for name in ['nginx-http.conf.example','nginx-https.conf.example']:
 text=(ROOT/'deploy/linux'/name).read_text()
 check(name+'-api-strip-prefix','proxy_pass http://127.0.0.1:8080/;' in text)
 check(name+'-auth-and-errors-preserved','proxy_set_header Authorization $http_authorization;' in text and 'proxy_intercept_errors off;' in text)
 check(name+'-no-static-uploads','alias /var/lib/up-self' not in text)
 check(name+'-no-api-cache','no-store' in text and 'access_log off;' in text)
fr=(ROOT/'docs/当前61个FR完成状态与报告差异.md').read_text(encoding='utf-8-sig')
fr_rows=re.findall(r'^\| FR-(\d\d) \|[^\n]+',fr.split('## 与上一轮核对相比')[0],re.M)
check('FR-01-to-61-exactly-once',list(map(int,fr_rows))==list(range(1,62)))
print('Linux preparation checks:',len(checks))
print('Scope: Windows + Git Bash syntax/artifact/config checks. NOT Linux, nginx -t, systemd execution, actual backup/restore or cloud deployment.')
