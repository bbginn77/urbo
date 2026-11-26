"""Copy the published URBO site into the app and make it work offline.
Usage: python3 tools/sync_site.py /path/to/site"""
import re, shutil, sys, pathlib
src = pathlib.Path(sys.argv[1]); dst = pathlib.Path(__file__).resolve().parent.parent / 'app/src/main/assets/www'
for f in ['data.json', 'urbowx.js', 'urbotr.js', 'urbo_terrain.webp']:
    shutil.copy(src / f, dst / f)
for d in ['art', 'img']:
    shutil.copytree(src / d, dst / d, dirs_exist_ok=True)
shutil.copy(src / 'fonts/Brightportish-Regular.ttf', dst / 'fonts/Brightportish-Regular.ttf')
s = (src / 'index.html').read_text(encoding='utf-8')
s = s.replace('<link rel="preconnect" href="https://fonts.googleapis.com">\n', '')
s = re.sub(r'<link href="https://fonts.googleapis.com/css2[^>]*>', '<link href="fonts.css" rel="stylesheet">', s)
if 'tvnav.js' not in s:
    s = s.replace('<script src="urbowx.js"></script>', '<script src="urbowx.js"></script>\n<script src="tvnav.js" defer></script>', 1)
(dst / 'index.html').write_text(s, encoding='utf-8')
print('synced', dst)
