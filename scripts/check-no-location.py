#!/usr/bin/env python3
"""Inspect the merged manifest, including permissions contributed by libraries."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
manifests = list((root / 'app/build/intermediates/merged_manifests/debug').rglob('AndroidManifest.xml'))
if not manifests:
    raise SystemExit('Debug merged manifest missing: run assembleDebug first')
name = '{http://schemas.android.com/apk/res/android}name'
for manifest in manifests:
    tree = ET.parse(manifest)
    for node in tree.getroot().iter():
        value = node.get(name, '')
        if node.tag.startswith('uses-permission') and value in {
            'android.permission.ACCESS_FINE_LOCATION',
            'android.permission.ACCESS_COARSE_LOCATION',
            'android.permission.ACCESS_BACKGROUND_LOCATION',
            'android.permission.FOREGROUND_SERVICE_LOCATION',
        }:
            raise SystemExit(f'Removed location permission reintroduced: {value}')
        if node.tag == 'service' and value.endswith('LocationTrackingService'):
            raise SystemExit('Removed tracking service reintroduced')
print('Merged debug manifest: no location permissions or tracking service.')
