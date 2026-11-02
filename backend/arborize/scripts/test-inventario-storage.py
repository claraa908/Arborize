#!/usr/bin/env python3
"""PostgreSQL e MinIO reais, novos e descartáveis, sem serviços ou credenciais da aplicação."""
import argparse
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import tempfile
import time
import urllib.request
BASE=Path(__file__).resolve().parents[1]
TESTS='Arvore*Test,FotoArvore*Test,InventarioSecurityTest,RepresentacaoTerritorialTest,InventarioPersistenciaIT,InventarioStorageIT,FotoArvoreMultipartIT'

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--maven',default=shutil.which('mvn') or str(BASE/'mvnw'))
    parser.add_argument('--minio-image',default='arborize-minio-it:release-2025-09-07')
    parser.add_argument('--offline',action='store_true')
    args=parser.parse_args()
    for image in ['postgres:15',args.minio_image]:
        subprocess.run(['docker','image','inspect',image],check=True,stdout=subprocess.DEVNULL)
    suffix=secrets.token_hex(6);database='arborize_it_'+suffix;bucket='arborize-it-'+suffix
    pg='arborize-t20b-pg-'+suffix;minio='arborize-t20b-minio-'+suffix
    password=secrets.token_urlsafe(32);access='it'+secrets.token_hex(10);secret=secrets.token_urlsafe(32)
    started=[]
    with tempfile.TemporaryDirectory(prefix='arborize-storage-it-') as folder:
        folder=Path(folder)
        pgenv=folder/'pg.env';pgenv.write_text(f'POSTGRES_USER=postgres\nPOSTGRES_DB={database}\nPOSTGRES_PASSWORD={password}\n');pgenv.chmod(0o600)
        menv=folder/'minio.env';menv.write_text(f'MINIO_ROOT_USER={access}\nMINIO_ROOT_PASSWORD={secret}\nMINIO_BROWSER=off\n');menv.chmod(0o600)
        try:
            for name,image,port,mount,envfile,command in [
                (pg,'postgres:15','5432','/var/lib/postgresql/data:rw',pgenv,[]),
                (minio,args.minio_image,'9000','/data:rw',menv,['server','/data'])]:
                subprocess.run(['docker','run','--detach','--pull=never','--name',name,'--label','arborize.purpose=t20b-isolated-test',
                                '--publish','127.0.0.1::'+port,'--tmpfs',mount,'--env-file',str(envfile),image,*command],check=True,stdout=subprocess.DEVNULL)
                started.append(name)
            def port_of(name,port):
                binding=subprocess.check_output(['docker','port',name,port+'/tcp'],text=True).strip()
                if not binding.startswith('127.0.0.1:') or '\n' in binding:raise RuntimeError('Publicação não restrita ao loopback')
                value=int(binding.rsplit(':',1)[1])
                if value<=1024 or value in (5432,5433,9000,9001):raise RuntimeError('Porta de teste inválida')
                return value
            pgport=port_of(pg,'5432');mport=port_of(minio,'9000');endpoint=f'http://127.0.0.1:{mport}'
            ready=False
            for _ in range(300):
                postgres_ready=subprocess.run(['docker','exec',pg,'pg_isready','-h','127.0.0.1','-U','postgres','-d',database],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode==0
                try:
                    with urllib.request.urlopen(endpoint+'/minio/health/live',timeout=1) as response:minio_ready=response.status==200
                except Exception:minio_ready=False
                if postgres_ready and minio_ready:ready=True;break
                time.sleep(0.1)
            if not ready:raise RuntimeError('Infraestrutura descartável não ficou pronta; nenhum datasource alternativo será utilizado')
            version=subprocess.check_output(['docker','exec',minio,'/usr/local/bin/minio','--version'],text=True).splitlines()[0]
            print(version,flush=True)
            env=os.environ.copy()
            env.pop('ARBORIZE_IT_MINIO_PROXY_URL',None)
            env.update(ARBORIZE_IT_EPHEMERAL='true',ARBORIZE_IT_DB_USER='postgres',ARBORIZE_IT_DB_PASSWORD=password,
                       ARBORIZE_IT_JDBC_URL=f'jdbc:postgresql://127.0.0.1:{pgport}/{database}',
                       ARBORIZE_IT_MINIO_URL=endpoint,ARBORIZE_IT_MINIO_BUCKET=bucket,
                       ARBORIZE_IT_MINIO_ACCESS_KEY=access,ARBORIZE_IT_MINIO_SECRET_KEY=secret)
            command=[args.maven]+(['-o'] if args.offline else [])+['-Dtest='+TESTS,'test']
            return subprocess.run(command,cwd=BASE,env=env).returncode
        finally:
            for name in reversed(started):subprocess.run(['docker','rm','--force',name],check=True,stdout=subprocess.DEVNULL)
            print('Containers, banco, bucket e credenciais descartáveis removidos.',flush=True)
if __name__=='__main__':raise SystemExit(main())
