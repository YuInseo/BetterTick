"""Print small, synthetic emulator screenshots in log-safe chunks for remote visual review."""
import base64
from pathlib import Path

for path in sorted(Path("app/build/reports/ui-regression").rglob("*.png")):
    # adb pull can leave a directory named like a screenshot; only real files are printed.
    if not path.is_file():
        continue
    encoded = base64.b64encode(path.read_bytes()).decode("ascii")
    chunks = [encoded[i:i + 3500] for i in range(0, len(encoded), 3500)]
    for index, chunk in enumerate(chunks):
        print(f"UI_PNG {path.stem} {index}/{len(chunks)} {chunk}")
