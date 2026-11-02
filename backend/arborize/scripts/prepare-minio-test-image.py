#!/usr/bin/env python3
"""Imagem local MinIO para testes: release oficial e checksum fixos, sem instalar binários no host."""
from pathlib import Path
import hashlib
import platform
import shutil
import subprocess
import tempfile
import urllib.request
VERSION='RELEASE.2025-09-07T16-13-09Z'
SHA256='7c5bd8512c6e966455b1d198209358b2d191c77a83ab377c4073281065fb855f'
NAME='minio.linux-amd64.'+VERSION
BASE='https://github.com/minio/minio/releases/download/'+VERSION+'/'
if platform.machine() not in ('x86_64','amd64'):raise SystemExit('Este executor requer Linux amd64.')
subprocess.run(['docker','image','inspect','debian:bookworm-slim'],check=True,stdout=subprocess.DEVNULL)
with tempfile.TemporaryDirectory(prefix='arborize-minio-image-') as folder:
    folder=Path(folder)
    with urllib.request.urlopen(BASE+NAME) as response,(folder/'minio').open('wb') as output:shutil.copyfileobj(response,output)
    with urllib.request.urlopen(BASE+NAME+'.sha256sum') as response:official=response.read().decode().split()[0]
    with (folder/'minio').open('rb') as binary:digest=hashlib.file_digest(binary,'sha256').hexdigest()
    if digest!=SHA256 or digest!=official:raise SystemExit('SHA-256 não confere; imagem não será construída.')
    (folder/'minio').chmod(0o755)
    (folder/'Dockerfile').write_text('FROM debian:bookworm-slim\nCOPY minio /usr/local/bin/minio\nENTRYPOINT ["/usr/local/bin/minio"]\n')
    subprocess.run(['docker','build','--pull=false','-t','arborize-minio-it:release-2025-09-07',str(folder)],check=True)
