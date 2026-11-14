"""Diagnose which animation XML pattern PowerPoint rejects."""
import zipfile
from pptx import Presentation
from pptx.util import Inches
from pptx.enum.shapes import MSO_SHAPE
from lxml import etree

NS_P = 'http://schemas.openxmlformats.org/presentationml/2006/main'
NS_A = 'http://schemas.openxmlformats.org/drawingml/2006/main'

def make_test(name, timing):
    prs = Presentation()
    prs.slide_width = Inches(20); prs.slide_height = Inches(11.25)
    s = prs.slides.add_slide(prs.slide_layouts[6])
    sp = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(1), Inches(1), Inches(4), Inches(2))
    sp.fill.solid()
    prs.save(name)
    # inject timing
    zin = zipfile.ZipFile(name)
    entries = {n: zin.read(n) for n in zin.namelist()}
    zin.close()
    root = etree.fromstring(entries['ppt/slides/slide1.xml'])
    root.append(etree.fromstring(timing))
    entries['ppt/slides/slide1.xml'] = etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)
    with zipfile.ZipFile(name, 'w', zipfile.ZIP_DEFLATED) as z:
        for n, data in entries.items():
            z.writestr(n, data)

# TEST A: canonical fade (from a real PowerPoint save)
TIMING_A = f'''<p:timing xmlns:p="{NS_P}" xmlns:a="{NS_A}"><p:tnLst><p:par><p:cTn id="1" dur="indefinite" restart="never" nodeType="tmRoot"><p:childTnLst><p:seq concurrent="1" nextAc="seek"><p:cTn id="2" dur="indefinite" nodeType="mainSeq"><p:childTnLst><p:par><p:cTn id="3" fill="hold"><p:stCondLst><p:cond delay="indefinite"/></p:stCondLst><p:childTnLst><p:par><p:cTn id="4" fill="hold"><p:stCondLst><p:cond delay="0"/></p:stCondLst><p:childTnLst><p:par><p:cTn id="5" presetID="10" presetClass="entr" presetSubtype="0" fill="hold" nodeType="clickEffect"><p:stCondLst><p:cond delay="0"/></p:stCondLst><p:childTnLst><p:set><p:cBhvr><p:cTn id="6" dur="1" fill="hold"><p:stCondLst><p:cond delay="0"/></p:stCondLst></p:cTn><p:tgtEl><p:spTgt spid="2"/></p:tgtEl><p:attrNameLst><p:attrName>style.visibility</p:attrName></p:attrNameLst></p:cBhvr><p:to><p:strVal val="visible"/></p:to></p:set><p:animEffect transition="in" filter="fade"><p:cBhvr><p:cTn id="7" dur="500"/><p:tgtEl><p:spTgt spid="2"/></p:tgtEl></p:cBhvr></p:animEffect></p:childTnLst></p:cTn></p:par></p:childTnLst></p:cTn></p:par></p:childTnLst></p:cTn></p:par></p:childTnLst></p:cTn><p:prevCondLst><p:cond evt="onPrev" delay="0"><p:tgtEl><p:sldTgt/></p:tgtEl></p:cond></p:prevCondLst><p:nextCondLst><p:cond evt="onNext" delay="0"><p:tgtEl><p:sldTgt/></p:tgtEl></p:cond></p:nextCondLst></p:seq></p:childTnLst></p:cTn></p:par></p:tnLst></p:timing>'''

# TEST B: same but with afterEffect nodeType (auto after previous)
TIMING_B = TIMING_A.replace('nodeType="clickEffect"', 'nodeType="afterEffect"').replace('<p:cond delay="indefinite"/>', '<p:cond delay="indefinite"/><p:cond evt="onBegin" delay="0"><p:tn val="2"/></p:cond>')

make_test("test_a.pptx", TIMING_A)
print("built test_a.pptx (canonical clickEffect)")
make_test("test_b.pptx", TIMING_B)
print("built test_b.pptx (afterEffect auto-start)")
