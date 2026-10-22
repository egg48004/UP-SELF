"""Run with backend and frontend already running. Reads local credentials in memory; never logs them. Uses bundled Java/JAR libraries and checked-in non-personal test media. No package installation. Only its own disposable fixture rows are removed."""
from pathlib import Path
import sys,subprocess,zipfile,json,importlib.util,os,shutil
root=Path(__file__).resolve().parents[1];work=root/'.local/frontend-verification';work.mkdir(parents=True,exist_ok=True)
cpdir=work/'browser-check-dependencies';cpdir.mkdir(exist_ok=True)
(work/'ReadLocalYaml.java').write_text('import java.io.*;import org.yaml.snakeyaml.Yaml;import com.alibaba.fastjson2.JSON;public class ReadLocalYaml{public static void main(String[] a)throws Exception{try(InputStream i=new FileInputStream(a[0])){System.out.print(JSON.toJSONString(new Yaml().load(i)));}}}',encoding='utf-8')
with zipfile.ZipFile(root/'up-self-admin/target/up-self-admin.jar') as z:
 for n in z.namelist():
  if n.startswith('BOOT-INF/lib/') and ('snakeyaml-' in n or 'fastjson2-' in n):(cpdir/Path(n).name).write_bytes(z.read(n))
java=Path(r'C:\Program Files\Java\jdk1.8.0_231\bin');cp=str(cpdir/'*')
subprocess.run([str(java/'javac.exe'),'-encoding','UTF-8','-cp',cp,str(work/'ReadLocalYaml.java')],check=True,capture_output=True)
config=json.loads(subprocess.check_output([str(java/'java.exe'),'-cp',str(work)+';'+cp,'ReadLocalYaml',str(root/'config/application-local.yml')]))
spec=importlib.util.spec_from_file_location('authcheck',root/'scripts/verify-local.py');auth=importlib.util.module_from_spec(spec);spec.loader.exec_module(auth)
env=os.environ.copy();runtime=Path(r'C:\Users\eggma\Documents\Codex\local-env\nodejs\runtime-path.txt').read_text(encoding='utf-8').strip();env['PATH']=runtime+';'+env.get('PATH','')
for k in ['admin01','mentor01','student01','maintenance']:env['UPSELF_'+k.upper()+'_PASSWORD']=config['bootstrap']['passwords'][k]
fixtures=root/'frontend/tests/fixtures'
(work/'screenshots').mkdir(exist_ok=True)
env['UPSELF_MEDIA_DIR']=str(fixtures);env['UPSELF_SCREENSHOTS']=str(work/'screenshots');env['UPSELF_TEST_WORK']=str(work)
extra=sys.argv[1:]
out=work/('frontend-e2e-extra.log' if extra else 'frontend-e2e.log')
with out.open('w',encoding='utf-8') as log:
 r=subprocess.run([runtime+'\\node.exe',str(root/'frontend/node_modules/@playwright/test/cli.js'),'test',*extra],cwd=root/'frontend',env=env,stdout=log,stderr=subprocess.STDOUT)
extra_file=work/'e2e-extra.json'
main_file=work/'e2e-generated.json'
records=[]
if extra_file.exists(): records.append((json.loads(extra_file.read_text()), 'UI action check %'))
if main_file.exists(): records.append((json.loads(main_file.read_text()), 'Learning together %'))
for generated, title_pattern in records:
 for cid in generated.get('courses',[]):
  cid=int(cid)
  rows=auth.sql('SELECT storage_key FROM learning_resource WHERE chapter_id IN(SELECT id FROM learning_chapter WHERE course_id='+str(cid)+')')
  # Only IDs recorded by this invocation and matching its fixture prefix may be cleaned.
  if auth.sql('SELECT COUNT(*) FROM learning_course WHERE id='+str(cid)+" AND title LIKE '"+title_pattern+"'")!='1': continue
  auth.sql('UPDATE learning_resource SET caption_resource_id=NULL WHERE chapter_id IN(SELECT id FROM learning_chapter WHERE course_id='+str(cid)+');DELETE FROM learning_resource WHERE chapter_id IN(SELECT id FROM learning_chapter WHERE course_id='+str(cid)+');DELETE FROM learning_post WHERE course_id='+str(cid)+';DELETE FROM learning_chapter WHERE course_id='+str(cid)+';DELETE FROM learning_enrollment WHERE course_id='+str(cid)+';DELETE FROM learning_course WHERE id='+str(cid)+';')
  for key in rows.splitlines():
   file=(root/'.local/uploads'/key).resolve();base=(root/'.local/uploads').resolve()
   if base in file.parents and file.is_file():file.unlink()
 for uid in generated.get('users',[]):auth.sql('DELETE FROM sys_user WHERE id='+str(int(uid))+" AND user_name LIKE 'ui_%';")
extra_file.unlink(missing_ok=True)
main_file.unlink(missing_ok=True)
shutil.rmtree(cpdir)
(work/'ReadLocalYaml.java').unlink(missing_ok=True)
(work/'ReadLocalYaml.class').unlink(missing_ok=True)
print('Browser checks exit:',r.returncode)
print(out.read_text(encoding='utf-8')[-5500:].encode('ascii',errors='replace').decode())
raise SystemExit(r.returncode)
