"""Structural bisect: (a) one effect w/ my 4-level nesting, (b) 3 effects w/ my nesting,
(c) 3 effects w/ canonical 3-level nesting (delay on middle par)."""
import zipfile
from pptx import Presentation
from pptx.util import Inches
from pptx.enum.shapes import MSO_SHAPE
from lxml import etree

NS_P = 'http://schemas.openxmlformats.org/presentationml/2006/main'
NS_A = 'http://schemas.openxmlformats.org/drawingml/2006/main'

BEHAVIORS = ('<p:set><p:cBhvr><p:cTn id="{i1}" dur="1" fill="hold"><p:stCondLst><p:cond delay="0"/></p:stCondLst></p:cTn>'
             '<p:tgtEl><p:spTgt spid="{spid}"/></p:tgtEl>'
             '<p:attrNameLst><p:attrName>style.visibility</p:attrName></p:attrNameLst></p:cBhvr>'
             '<p:to><p:strVal val="visible"/></p:to></p:set>'
             '<p:animEffect transition="in" filter="fade"><p:cBhvr><p:cTn id="{i2}" dur="500"/>'
             '<p:tgtEl><p:spTgt spid="{spid}"/></p:tgtEl></p:cBhvr></p:animEffect>')

def my_effect_par(spid, delay, ids):
    """My original 4-level: outer(delay) > wrap(0) > preset(0)."""
    i1, i2 = next(ids), next(ids)
    outer, wrap, e = next(ids), next(ids), next(ids)
    b = BEHAVIORS.format(i1=i1, i2=i2, spid=spid)
    return (f'<p:par><p:cTn id="{outer}" fill="hold">'
            f'<p:stCondLst><p:cond delay="{delay}"/></p:stCondLst>'
            f'<p:childTnLst><p:par><p:cTn id="{wrap}" fill="hold">'
            f'<p:stCondLst><p:cond delay="0"/></p:stCondLst>'
            f'<p:childTnLst><p:par><p:cTn id="{e}" presetID="10" presetClass="entr" presetSubtype="0" '
            f'fill="hold" grpId="0" nodeType="afterEffect">'
            f'<p:stCondLst><p:cond delay="0"/></p:stCondLst>'
            f'<p:childTnLst>{b}</p:childTnLst></p:cTn></p:par></p:childTnLst>'
            f'</p:cTn></p:par></p:childTnLst></p:cTn></p:par>')

def canon_effect_par(spid, delay, ids):
    """Canonical 3-level: midPar(delay) > preset(0) — like real PowerPoint 'with previous + delay'."""
    i1, i2 = next(ids), next(ids)
    mid, e = next(ids), next(ids)
    b = BEHAVIORS.format(i1=i1, i2=i2, spid=spid)
    return (f'<p:par><p:cTn id="{mid}" fill="hold">'
            f'<p:stCondLst><p:cond delay="{delay}"/></p:stCondLst>'
            f'<p:childTnLst><p:par><p:cTn id="{e}" presetID="10" presetClass="entr" presetSubtype="0" '
            f'fill="hold" grpId="0" nodeType="afterEffect">'
            f'<p:stCondLst><p:cond delay="0"/></p:stCondLst>'
            f'<p:childTnLst>{b}</p:childTnLst></p:cTn></p:par></p:childTnLst></p:cTn></p:par>')

def timing(effects_xml, seq_id, grp_id):
    return (f'<p:timing xmlns:p="{NS_P}" xmlns:a="{NS_A}">'
            f'<p:tnLst><p:par><p:cTn id="1" dur="indefinite" restart="never" nodeType="tmRoot">'
            f'<p:childTnLst><p:seq concurrent="1" nextAc="seek">'
            f'<p:cTn id="{seq_id}" dur="indefinite" nodeType="mainSeq"><p:childTnLst>'
            f'<p:par><p:cTn id="{grp_id}" fill="hold">'
            f'<p:stCondLst><p:cond delay="indefinite"/>'
            f'<p:cond evt="onBegin" delay="0"><p:tn val="{seq_id}"/></p:cond></p:stCondLst>'
            f'<p:childTnLst>{effects_xml}</p:childTnLst>'
            f'</p:cTn></p:par>'
            f'</p:childTnLst></p:cTn>'
            f'<p:prevCondLst><p:cond evt="onPrev" delay="0"><p:tgtEl><p:sldTgt/></p:tgtEl></p:cond></p:prevCondLst>'
            f'<p:nextCondLst><p:cond evt="onNext" delay="0"><p:tgtEl><p:sldTgt/></p:tgtEl></p:cond></p:nextCondLst>'
            f'</p:seq></p:childTnLst></p:cTn></p:par></p:tnLst></p:timing>')

def make(name, builder, n_effects):
    prs = Presentation()
    prs.slide_width = Inches(20); prs.slide_height = Inches(11.25)
    s = prs.slides.add_slide(prs.slide_layouts[6])
    for k in range(3):
        sp = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(1 + k*2), Inches(1), Inches(1.6), Inches(1))
        sp.fill.solid()
    prs.save(name)
    ids_iter = iter(range(1, 1000))
    seq_id, grp_id = next(ids_iter), next(ids_iter)
    spids = [sp.shape_id for sp in s.shapes]
    pars = ''.join(builder(spids[i % 3], i * 200, ids_iter) for i in range(n_effects))
    xml = timing(pars, seq_id, grp_id)
    zin = zipfile.ZipFile(name); entries = {n: zin.read(n) for n in zin.namelist()}; zin.close()
    root = etree.fromstring(entries['ppt/slides/slide1.xml'])
    root.append(etree.fromstring(xml))
    entries['ppt/slides/slide1.xml'] = etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)
    with zipfile.ZipFile(name, 'w', zipfile.ZIP_DEFLATED) as z:
        for n, data in entries.items():
            z.writestr(n, data)
    print('built', name)

make('s1_one.pptx', my_effect_par, 1)
make('s2_three.pptx', my_effect_par, 3)
make('s3_canon3.pptx', canon_effect_par, 3)
