"""Bisect the corruption: build v1 (no anim), t-only (transitions), timing-only variants."""
import zipfile
from lxml import etree

NS_P = 'http://schemas.openxmlformats.org/presentationml/2006/main'
NS_P14 = 'http://schemas.microsoft.com/office/powerpoint/2010/main'

def rezip(src, out, transform=None):
    zin = zipfile.ZipFile(src)
    entries = {n: zin.read(n) for n in zin.namelist()}
    zin.close()
    if transform:
        transform(entries)
    with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as z:
        for n, data in entries.items():
            z.writestr(n, data)

def add_transitions(entries, with_p14=False):
    for i in range(1, 10):
        name = f'ppt/slides/slide{i}.xml'
        root = etree.fromstring(entries[name])
        p14 = f' xmlns:p14="{NS_P14}"' if with_p14 else ''
        dur = ' p14:dur="700"' if with_p14 else ''
        t = etree.fromstring(f'<p:transition xmlns:p="{NS_P}"{p14} spd="med"{dur}><p:fade/></p:transition>')
        root.append(t)
        entries[name] = etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)

# copy of the clean deck for baseline
rezip('ClaimChain_Modern_Animated.pptx', 'bisect_v1.pptx')
# transitions only (plain)
rezip('ClaimChain_Modern_Animated.pptx', 'bisect_trans.pptx', lambda e: add_transitions(e, with_p14=False))
# transitions only (with p14:dur like the failing file)
rezip('ClaimChain_Modern_Animated.pptx', 'bisect_trans_p14.pptx', lambda e: add_transitions(e, with_p14=True))
print('built bisect files')
