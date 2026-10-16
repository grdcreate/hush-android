#!/usr/bin/env python3
"""Static consistency checks: every resource reference in Java/XML must exist."""
import os, re, sys, subprocess, zipfile

ROOT = "/home/user"
SRC = os.path.join(ROOT, "hush")
GEN = os.path.join(ROOT, "hush-build/gen/com/aurora/hush/R.java")
BT = os.path.join(ROOT, "sdk/bt/android-14")
APK = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "release/Hush-v1.1.0.apk")

errors = []
ok = 0

def good():
    global ok
    ok += 1

# ---- collect R.java symbols
rjava = open(GEN).read()
def rclass(name):
    m = re.search(r"class %s \{(.*?)\}" % name, rjava, re.S)
    return set(re.findall(r"int (\w+)", m.group(1))) if m else set()
ids, layouts, drawables, strings, colors, styles, mipmaps = (rclass(n) for n in
    ["id", "layout", "drawable", "string", "color", "style", "mipmap"])

# ---- Java references
java_refs = {"id": set(), "layout": set(), "drawable": set(), "string": set(), "color": set()}
for f in os.listdir(os.path.join(SRC, "src/com/aurora/hush")):
    if not f.endswith(".java"):
        continue
    t = open(os.path.join(SRC, "src/com/aurora/hush", f)).read()
    for kind in java_refs:
        java_refs[kind] |= set(re.findall(r"R\.%s\.(\w+)" % kind, t))

for kind, pool in [("id", ids), ("layout", layouts), ("drawable", drawables),
                   ("string", strings), ("color", colors)]:
    missing = java_refs[kind] - pool
    if missing:
        errors.append("Java R.%s missing: %s" % (kind, sorted(missing)))
    else:
        good()

# ---- XML layout references
res = os.path.join(SRC, "res")
xml_files = []
for dirpath, _, files in os.walk(res):
    for f in files:
        if f.endswith(".xml"):
            xml_files.append(os.path.join(dirpath, f))
for xf in xml_files:
    t = open(xf).read()
    for ref in re.findall(r"@drawable/(\w+)", t):
        if ref not in drawables:
            errors.append("%s: @drawable/%s missing" % (os.path.basename(xf), ref))
    for ref in re.findall(r"@string/(\w+)", t):
        if ref not in strings:
            errors.append("%s: @string/%s missing" % (os.path.basename(xf), ref))
    for ref in re.findall(r"@color/(\w+)", t):
        if ref not in colors:
            errors.append("%s: @color/%s missing" % (os.path.basename(xf), ref))
    for ref in re.findall(r"@style/(\w+)", t):
        if ref not in styles:
            errors.append("%s: @style/%s missing" % (os.path.basename(xf), ref))
    for ref in re.findall(r"@mipmap/(\w+)", t):
        if ref not in mipmaps:
            errors.append("%s: @mipmap/%s missing" % (os.path.basename(xf), ref))
good()

# ---- layout ids used in Java must exist in layouts (via R.java generated from them) — already covered.
# ---- custom view classes referenced in layouts exist in source
srcs = set(f[:-5] for f in os.listdir(os.path.join(SRC, "src/com/aurora/hush")) if f.endswith(".java"))
for xf in xml_files:
    t = open(xf).read()
    for cls in re.findall(r"<com\.aurora\.hush\.(\w+)", t):
        if cls not in srcs:
            errors.append("%s: custom view class %s missing" % (os.path.basename(xf), cls))
    else:
        good()

# ---- manifest components exist in dex
manifest = open(os.path.join(SRC, "AndroidManifest.xml")).read()
components = re.findall(r"android:name=\"\.(\w+)\"", manifest)
dex = zipfile.ZipFile(APK).read("classes.dex")
tmp = "/tmp/_classes.dex"
open(tmp, "wb").write(dex)
out = subprocess.run([os.path.join(BT, "dexdump"), tmp], capture_output=True, text=True).stdout
dex_classes = set(re.findall(r"Class descriptor\s+: 'Lcom/aurora/hush/(\w+);'", out))
for c in components:
    if c not in dex_classes:
        errors.append("manifest component %s not in dex" % c)
    else:
        good()

# ---- every activity/service/receiver in dex that manifest forgot? (informational)
print("components checked:", components)
print("dex classes:", len(dex_classes))

print()
if errors:
    print("STATIC CHECK FAILED (%d):" % len(errors))
    for e in errors:
        print("  -", e)
    sys.exit(1)
print("STATIC CHECK OK — %d check groups passed, 0 missing references" % ok)
