#!/usr/bin/env python3
"""Maven + PostgreSQL 15 efêmero. Nunca reutiliza o datasource ou containers da aplicação."""
import argparse
import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import tempfile
import time

BASE = Path(__file__).resolve().parents[1]
TESTS = 'Arvore*Test,FotoArvore*Test,InventarioSecurityTest,RepresentacaoTerritorialTest,InventarioPersistenciaIT'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--maven', default=shutil.which('mvn') or str(BASE / 'mvnw'))
    parser.add_argument('--offline', action='store_true', help='Exige plugins e dependências já no cache.')
    args = parser.parse_args()
    subprocess.run(['docker', 'info', '--format', '{{.ServerVersion}}'], check=True, stdout=subprocess.DEVNULL)
    # Não baixa nem atualiza imagens implicitamente.
    subprocess.run(['docker', 'image', 'inspect', 'postgres:15'], check=True, stdout=subprocess.DEVNULL)
    suffix = secrets.token_hex(6)
    container = 'arborize-t20a-' + suffix
    database = 'arborize_it_' + suffix
    password = secrets.token_urlsafe(32)
    started = False
    with tempfile.TemporaryDirectory(prefix='arborize-it-') as directory:
        envfile = Path(directory) / 'postgres.env'
        envfile.write_text('POSTGRES_USER=postgres\nPOSTGRES_DB=' + database + '\nPOSTGRES_PASSWORD=' + password + '\n')
        envfile.chmod(0o600)
        try:
            subprocess.run(['docker', 'run', '--detach', '--pull=never', '--name', container,
                            '--label', 'arborize.purpose=t20a-isolated-test',
                            '--publish', '127.0.0.1::5432', '--tmpfs', '/var/lib/postgresql/data:rw',
                            '--env-file', str(envfile), 'postgres:15'], check=True, stdout=subprocess.DEVNULL)
            started = True
            ready = False
            for _ in range(150):
                result = subprocess.run(['docker', 'exec', container, 'pg_isready', '-h', '127.0.0.1', '-U', 'postgres', '-d', database],
                                        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                if result.returncode == 0:
                    ready = True
                    break
                time.sleep(0.1)
            if not ready:
                raise RuntimeError('PostgreSQL efêmero não ficou pronto; testes não foram iniciados.')
            binding = subprocess.check_output(['docker', 'port', container, '5432/tcp'], text=True).strip()
            if not binding.startswith('127.0.0.1:') or '\n' in binding:
                raise RuntimeError('Publicação da porta não está restrita ao loopback.')
            port = int(binding.rsplit(':', 1)[1])
            if port <= 1024 or port in (5432, 5433):
                raise RuntimeError('Porta não é adequada ao isolamento dos testes.')
            env = os.environ.copy()
            env.update(ARBORIZE_IT_EPHEMERAL='true', ARBORIZE_IT_DB_USER='postgres',
                       ARBORIZE_IT_DB_PASSWORD=password,
                       ARBORIZE_IT_JDBC_URL=f'jdbc:postgresql://127.0.0.1:{port}/{database}')
            print('Executando Maven com PostgreSQL novo, perfil exclusivo e create-drop. MinIO simulado.', flush=True)
            command = [args.maven]
            if args.offline:
                command.append('-o')
            command.extend(['-Dtest=' + TESTS, 'test'])
            # A senha fica apenas no ambiente dos processos; não é argumento ou arquivo do repositório.
            return subprocess.run(command, cwd=BASE, env=env).returncode
        finally:
            if started:
                subprocess.run(['docker', 'rm', '--force', container], check=True, stdout=subprocess.DEVNULL)
                print('Container e dados efêmeros descartados.', flush=True)


if __name__ == '__main__':
    raise SystemExit(main())
