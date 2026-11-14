"""Isolate the exact difference: test_a (PASSES) vs t_fade (FAILS).

Differences in t_fade vs test_a:
 1. grpId="0" on effect cTn
 2. extra <p:cond evt="onBegin"> in click-group stCondLst (auto-start)
 3. nodeType afterEffect vs clickEffect
 4. rezip order (irrelevant)

Build each variant of test_a-style timing to find the killer.
"""
import zipfile
from pptx import Presentation
from pptx.util import Inches
from pptx.enum.shapes import MSO_SHAPE
from lxml import etree

NS_P = 'http://schemas.openxmlformats.org/presentationml/2006/main'
NS_A = 'http://schemas.openxmlformats.org/drawingml/2006/main'

BASE_TIMING = (
    '<p:timing xmlns:p="{p}" xmlns:a="{a}"><p:tnLst><p:par><p:cTn id="1" dur="indefinite" restart="never" nodeType="tmRoot">'
    '<p:childTnLst><p:seq concurrent="1" nextAc="seek">'
    '<p:cTn id="2" dur="indefinite" nodeType="mainSeq"><p:childTnLst>'
    '<p:par><p:cTn id="3" fill="hold">'
    '<p:stCondLst><p:cond delay="indefinite"/>{GRP_EXTRA_COND}</p:stCondLst>'
    '<p:childTnLst>'
    '<p:par><p:cTn id="4" fill="hold"><p:stCondLst><p:cond delay="0"/></p:stCondLst><p:childTnLst>'
    '<p:par><p:cTn id="5" presetID="10" presetClass="entr" presetSubtype="0" fill="hold"{GRPID} nodeType="{NODETYPE}">'
    '<p:stCondLst><p:cond delay="0"/></p:stCondLst>'
    '<p:childTnLst>'
    '<p:set><p:cBhvr><p:cTn id="6" dur="1" fill="hold"><p:stCondLst><p:cond delay="0"/></p:stCondLst></p:cTn>'
    '<p:tgtEl><p:spTgt spid="{spid}"/></p:tgtEl>'
    '<p:attrNameLst><p:attrName>style.visibility</p:attrName></p:attrNameLst></p:cBhvr>'
    '<p:to><p:strVal val="visible"/></p:to></p:set>'
    '<p:animEffect transition="in" filter="fade"><p:cBhvr><p:cTn id="7" dur="500"/>'
    '<p:tgtEl><p:spTgt spid="{spid}"/></p:tgtEl></p:cBhvr></p:animEffect>'
    '</p:childTnLst></p:cTn></p:par>'
    '</p:childTnLst></p:cTn></p:par>'
    '</p:childTnLst></p:cTn></p:par>'
    '</p:childTnLst></p:cTn>'
    '<p:prevCondLst><p:cond evt="onPrev" delay="0"><p:tgtEl><p:sldTgt/></p:tgtEl></p:cond></p:prevCondLst>'
    '<p:nextCondLst><p:cond evt="onNext" delay="0"><p:tgtEl><p:sldTgt/></p:tgtEl></p:cond></p:nextCondLst>'
    '</p:seq></p:childTnLst></p:cTn></p:par></p:tnLst></p:timing>'
)

def build(name, grpid=False, nodetype='clickEffect', grp_extra=''):
    xml = BASE_TIMING.format(
        p=NS_P, a=NS_A,
        GRPID=' grpId="0"' if grpid else '',
        NODETYPE=nodetype,
        GRP_EXTRA_COND=grp_extra,
        spid=2,
    )
    prs = Presentation()
    prs.slide_width = Inches(20); prs.slide_height = Inches(11.25)
    s = prs.slides.add_slide(prs.slide_layouts[6])
    s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(1), Inches(1), Inches(2), Inches(1))
    prs.save(name)
    zin = zipfile.ZipFile(name); entries = {n: zin.read(n) for n in zin.namelist()}; zin.close()
    root = etree.fromstring(entries['ppt/slides/slide1.xml'])
    root.append(etree.fromstring(xml))
    entries['ppt/slides/slide1.xml'] = etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)
    with zipfile.ZipFile(name, 'w', zipfile.ZIP_DEFLATED) as z:
        for n, data in entries.items():
            z.writestr(n, data)
    print('built', name)

# canonical again (sanity)
build('v_canon.pptx')
# + grpId only
build('v_grpid.pptx', grpid=True)
# + afterEffect only
build('v_after.pptx', nodetype='afterEffect')
# + auto-start cond only
build('v_autostart.pptx', grp_extra='<p:cond evt="onBegin" delay="0"><p:tn val="2"/></p:cond>')
# all three
build('v_all.pptx', grpid=True, nodetype='afterEffect', grp_extra='<p:cond evt="onBegin" delay="0"><p:tn val="2"/></p:cond>')
print('done')
