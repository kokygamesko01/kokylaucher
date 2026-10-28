import zipfile, shutil, os

jar_path = os.environ.get("HARNESS_SCRATCH", "") + "/gradle-home/caches/modules-2/files-2.1/com.android.tools.build/aapt2/8.2.0-10154469/5b075efacc43be82d5d2a9ddf6430cac25c3f2ec/aapt2-8.2.0-10154469-linux.jar"
aapt2_path = "/data/user/0/com.androidharness.app/files/linux/bin/aapt2"

if not os.path.exists(jar_path):
    print("JAR not found at:", jar_path)
    exit(1)

# Backup
shutil.copy2(jar_path, jar_path + '.bak')

# Read existing entries (non-aapt2)
with zipfile.ZipFile(jar_path, 'r') as zin:
    entries = {}
    for item in zin.infolist():
        if item.filename != 'aapt2':
            entries[item.filename] = zin.read(item.filename)

# Read ARM64 aapt2
with open(aapt2_path, 'rb') as f:
    aapt2_data = f.read()

# Write new JAR with ARM64 aapt2
with zipfile.ZipFile(jar_path, 'w', zipfile.ZIP_DEFLATED) as zout:
    for name, data in entries.items():
        zout.writestr(name, data)
    zout.writestr('aapt2', aapt2_data)

print('JAR updated with ARM64 aapt2')
print('aapt2 size:', len(aapt2_data))
