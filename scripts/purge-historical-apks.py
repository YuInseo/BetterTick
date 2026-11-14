#!/usr/bin/env python3
"""One-off, explicitly authorized cleanup of historical Android binaries.

Never claims to erase PR refs, GitHub caches of commits, forks or downloaded copies.
The latest debug release is retained so installed clients can still update.
"""
import argparse
import base64
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import time
from urllib.request import Request, urlopen

BINARY = re.compile(r'\.(apk|aab)$', re.I)
EXPECTED_REPOSITORY = 'YuInseo/BetterTick'


def git(directory, *args, binary=False):
    result = subprocess.run(['git', '-C', str(directory), *args], capture_output=True, check=True)
    return result.stdout if binary else result.stdout.decode().strip()


def refs(directory):
    lines = git(directory, 'for-each-ref', '--format=%(refname) %(objectname)', 'refs/heads', 'refs/tags')
    return dict(line.split(' ', 1) for line in lines.splitlines())


def tree_without_binaries(directory, ref):
    entries = git(directory, 'ls-tree', '-r', '-z', ref, binary=True).split(b'\0')
    return b'\0'.join(entry for entry in entries if entry and not BINARY.search(entry.split(b'\t', 1)[1].decode()))


def rewrite(directory):
    before = refs(directory)
    trees = {ref: tree_without_binaries(directory, ref) for ref in before}
    counts = {ref: git(directory, 'rev-list', '--count', ref) for ref in before}
    subprocess.run([
        'git', '-C', str(directory), 'filter-repo', '--force', '--invert-paths',
        '--path-glob', '*.apk', '--path-glob', '*.APK', '--path-glob', '*.aab', '--path-glob', '*.AAB',
        '--prune-empty', 'never', '--prune-degenerate', 'never',
    ], check=True)
    after = refs(directory)
    if before.keys() != after.keys():
        raise RuntimeError('Ref set changed during local rewrite')
    for ref in before:
        if tree_without_binaries(directory, ref) != trees[ref]:
            raise RuntimeError(f'Non-APK/AAB source changed at {ref}')
        # App versionCode currently uses commit count; never lower it here.
        if git(directory, 'rev-list', '--count', ref) != counts[ref]:
            raise RuntimeError(f'Commit count changed at {ref}')
    # Check every historical tree, including files subsequently deleted.
    names = git(directory, 'log', '--all', '--format=', '--name-only')
    if any(BINARY.search(name) for name in names.splitlines()):
        raise RuntimeError('Android binary remains in rewritten history')
    return before, after


class GitHub:
    def __init__(self, repository, token):
        self.base = f'https://api.github.com/repos/{repository}'
        self.token = token

    def call(self, method, path, payload=None):
        body = None if payload is None else json.dumps(payload).encode()
        request = Request(self.base + path, method=method, data=body, headers={
            'Authorization': f'Bearer {self.token}',
            'Accept': 'application/vnd.github+json',
            'X-GitHub-Api-Version': '2022-11-28',
            'Content-Type': 'application/json',
        })
        with urlopen(request, timeout=60) as response:
            content = response.read()
            return json.loads(content) if content else None

    def pages(self, path, key=None):
        collected = []
        page = 1
        while True:
            result = self.call('GET', f'{path}?per_page=100&page={page}')
            values = result[key] if key else result
            collected.extend(values)
            if len(values) < 100:
                return collected
            page += 1


def execute():
    repository = os.environ['GITHUB_REPOSITORY']
    if repository != EXPECTED_REPOSITORY:
        raise RuntimeError('Cleanup is restricted to the authorized repository')
    expected_main = os.environ['EXPECTED_MAIN_SHA']
    token = os.environ['GH_TOKEN']
    api = GitHub(repository, token)
    # Wait until normal APK publishing (including its optional small build) is done.
    for _ in range(60):
        runs = api.call('GET', '/actions/runs?per_page=100')['workflow_runs']
        active = [run for run in runs if run['name'] == 'Build Debug APK' and run['status'] != 'completed']
        if not active:
            break
        print('Waiting for the current APK publisher to finish', flush=True)
        time.sleep(15)
    else:
        raise RuntimeError('APK publisher still active; no cleanup performed')
    if api.call('GET', '/git/ref/heads/main')['object']['sha'] != expected_main:
        raise RuntimeError('main moved; inspect and rerun from the new head')
    latest = api.call('GET', '/releases/tags/latest-debug')
    if not any(asset['name'] == 'BetterTick.apk' for asset in latest['assets']):
        raise RuntimeError('Latest update APK is missing; no cleanup performed')
    if api.call('GET', '/git/ref/tags/latest-debug')['object']['sha'] != expected_main:
        raise RuntimeError('Latest APK has not been published from this main head')

    with tempfile.TemporaryDirectory(prefix='bettertick-apk-cleanup-') as temp:
        mirror = Path(temp) / 'repository.git'
        url = f'https://github.com/{repository}.git'
        # Public read; no token is embedded in a URL or written into the mirror.
        subprocess.run(['git', 'clone', '--mirror', url, str(mirror)], check=True)
        before, after = rewrite(mirror)
        if before.get('refs/heads/main') != expected_main:
            raise RuntimeError('main moved before cloning')
        live = subprocess.check_output(['git', 'ls-remote', '--heads', '--tags', url], text=True)
        live_refs = {line.split()[1]: line.split()[0] for line in live.splitlines() if not line.split()[1].endswith('^{}')}
        if live_refs != before:
            raise RuntimeError('A branch or tag moved during rewriting; refusing to overwrite newer work')
        changed = [ref for ref in before if before[ref] != after[ref]]
        if changed:
            git(mirror, 'remote', 'add', 'origin', url)
            header = base64.b64encode(f'x-access-token:{token}'.encode()).decode()
            command = ['git', '-C', str(mirror), '-c',
                       f'http.https://github.com/.extraheader=AUTHORIZATION: basic {header}',
                       'push', '--atomic', '--porcelain']
            command += [f'--force-with-lease={ref}:{before[ref]}' for ref in changed]
            command += ['origin'] + [f'{ref}:{ref}' for ref in changed]
            result = subprocess.run(command, capture_output=True, text=True)
            if result.returncode:
                # Git diagnostics show only the public origin URL, never the header.
                raise RuntimeError('Atomic history push rejected: ' + result.stderr)
            print(f'Rewrote {len(changed)} branches/tags with leases; source trees and commit counts preserved')
        verified = subprocess.check_output(['git', 'ls-remote', '--heads', '--tags', url], text=True)
        verified_refs = {line.split()[1]: line.split()[0] for line in verified.splitlines() if not line.split()[1].endswith('^{}')}
        if verified_refs != after:
            raise RuntimeError('Remote refs do not match the verified rewrite')

        # List first, delete second: mutations must not shift pagination.
        releases = api.pages('/releases')
        removed_assets = 0
        for release in releases:
            if release['tag_name'] == 'latest-debug':
                continue
            for asset in api.pages(f"/releases/{release['id']}/assets"):
                if BINARY.search(asset['name']):
                    api.call('DELETE', f"/releases/assets/{asset['id']}")
                    removed_assets += 1
        artifacts = api.pages('/actions/artifacts', 'artifacts')
        removed_artifacts = 0
        for artifact in artifacts:
            if 'apk' in artifact['name'].lower() or 'aab' in artifact['name'].lower():
                api.call('DELETE', f"/actions/artifacts/{artifact['id']}")
                removed_artifacts += 1
        # Gradle caches can contain old packaging task outputs. They are reproducible.
        caches = api.pages('/actions/caches', 'actions_caches')
        for cache in caches:
            api.call('DELETE', f"/actions/caches/{cache['id']}")
        remaining_artifacts = api.pages('/actions/artifacts', 'artifacts')
        if any('apk' in item['name'].lower() or 'aab' in item['name'].lower() for item in remaining_artifacts):
            raise RuntimeError('APK artifact still present')
        latest_after = api.call('GET', '/releases/tags/latest-debug')
        if not any(asset['name'] == 'BetterTick.apk' for asset in latest_after['assets']):
            raise RuntimeError('Latest update APK unexpectedly missing')
        # Release metadata can point at the old commit after rewriting its tag.
        api.call('PATCH', f"/releases/{latest_after['id']}", {'target_commitish': after['refs/heads/main']})
        summary = {
            'rewritten_refs': len(changed), 'old_release_apks_deleted': removed_assets,
            'apk_artifacts_deleted': removed_artifacts, 'build_caches_deleted': len(caches),
            'new_main_sha': after['refs/heads/main'], 'latest_debug_apk_retained': True,
            'limitations': 'PR refs, GitHub commit caches, forks and existing downloads are outside this cleanup.',
        }
        print(json.dumps(summary, indent=2))
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as report:
            report.write('## Historical APK cleanup\n\n```json\n' + json.dumps(summary, indent=2) + '\n```\n')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--dry-run-mirror', type=Path)
    parser.add_argument('--execute', action='store_true')
    args = parser.parse_args()
    if args.dry_run_mirror:
        old, new = rewrite(args.dry_run_mirror)
        print(json.dumps({'refs_checked': len(old), 'refs_changed': sum(old[r] != new[r] for r in old)}))
    elif args.execute:
        execute()
    else:
        parser.error('Choose --dry-run-mirror or --execute')
