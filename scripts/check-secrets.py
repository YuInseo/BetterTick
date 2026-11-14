#!/usr/bin/env python3
"""Scan tracked text for server credentials without printing matching values.

Firebase client config and the existing debug certificate are public by design.
This is a regression guard, not a replacement for GitHub secret scanning.
"""
import re
import subprocess
import sys
from pathlib import Path

root = Path(__file__).resolve().parents[1]
patterns = {
    'private key': re.compile(rb'-----BEGIN (?:RSA |EC |OPENSSH |ENCRYPTED )?PRIVATE KEY-----'),
    'service account': re.compile(rb'"type"\s*:\s*"service_account"'),
    'Google OAuth client secret': re.compile(rb'GOCSPX-[A-Za-z0-9_-]{20,}'),
    'GitHub access token': re.compile(rb'(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{40,})'),
    'AWS access key': re.compile(rb'(?:AKIA|ASIA)[A-Z0-9]{16}'),
}
paths = subprocess.check_output(['git', 'ls-files', '-z'], cwd=root).split(b'\0')
failures = []
for raw in paths:
    if not raw:
        continue
    name = raw.decode('utf-8')
    path = root / name
    if not path.is_file():
        continue
    # Never allow new signing keys or local env files to enter git.
    if ((path.suffix.lower() in {'.jks', '.keystore', '.p12', '.pem'} and name != 'app/debug.keystore')
            or (path.name.startswith('.env') and path.name != '.env.example')
            or path.name == 'keystore.properties'):
        failures.append((name, 'private configuration or signing key'))
    content = path.read_bytes()
    if b'\0' in content:
        continue
    for label, pattern in patterns.items():
        if pattern.search(content):
            failures.append((name, label))
for name, label in failures:
    print(f'::error file={name}::{label} detected; remove and rotate the credential')
if failures:
    sys.exit(1)
print('Tracked files: no recognized server credentials or private signing keys found.')
