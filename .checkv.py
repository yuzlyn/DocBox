import glob
import os
import zipfile

root = os.path.expanduser("~/.gradle/caches/modules-2/files-2.1/androidx.compose.ui")
patterns = ["Vertical", "writingMode", "WritingMode", "verticalText"]
for module in ("ui-text-android", "ui-android", "foundation-android"):
    for aar in glob.glob(f"{root}/{module}/1.12.1/**/*.aar", recursive=True):
        with zipfile.ZipFile(aar) as z:
            names = [n for n in z.namelist() if n.endswith("classes.jar")]
            if not names:
                continue
            with z.open(names[0]) as f:
                data = f.read()
        tmp = os.path.join(os.path.dirname(aar), "_classes.jar")
        open(tmp, "wb").write(data)
        hits = {}
        with zipfile.ZipFile(tmp) as cj:
            for entry in cj.namelist():
                if not entry.endswith(".class"):
                    continue
                blob = cj.read(entry)
                for p in patterns:
                    if p.encode() in blob:
                        hits.setdefault(p, []).append(entry)
        print(f"== {module} {os.path.basename(aar)}")
        for p, entries in hits.items():
            print(f"   {p}: {len(entries)} classes, e.g. {entries[:4]}")
        os.remove(tmp)
