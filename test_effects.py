"""Test each effect type (using the exact builders from inject_anim.py) as standalone files."""
import zipfile
from pptx import Presentation
from pptx.util import Inches
from pptx.enum.shapes import MSO_SHAPE
from lxml import etree

NS_P = 'http://schemas.openxmlformats.org/presentationml/2006/main'
NS_A = 'http://schemas.openxmlformats.org/drawingml/2006/main'

ID = [0]
def nid():
    ID[0] += 1
    return ID[0]

def beh_set(spid):
    return (f'<p:set><p:cBhvr><p:cTn id="{nid()}" dur="1" fill="hold">'
            f'<p:stCondLst><p:cond delay="0"/></p:stCondLst></p:cTn>'
            f'<p:tgtEl><p:spTgt spid="{spid}"/></p:tgtEl>'
            f'<p:attrNameLst><p:attrName>style.visibility</p:attrName></p:attrNameLst>'
            f'</p:cBhvr><p:to><p:strVal val="visible"/></p:to></p:set>')

def beh_anim_effect(spid, dur, filt):
    return (f'<p:animEffect transition="in" filter="{filt}"><p:cBhvr>'
            f'<p:cTn id="{nid()}" dur="{dur}"/>'
            f'<p:tgtEl><p:spTgt spid="{spid}"/></p:tgtEl></p:cBhvr></p:animEffect>')

def beh_anim_xy(spid, dur, attr, v0, v1):
    return (f'<p:anim calcmode="lin" valueType="num"><p:cBhvr additive="base">'
            f'<p:cTn id="{nid()}" dur="{dur}" fill="hold"/>'
            f'<p:tgtEl><p:spTgt spid="{spid}"/></p:tgtEl>'
            f'<p:attrNameLst><p:attrName>{attr}</p:attrName></p:attrNameLst></p:cBhvr>'
            f'<p:tavLst>'
            f'<p:tav tm="0"><p:val><p:strVal val="{v0}"/></p:val></p:tav>'
            f'<p:tav tm="100000"><p:val><p:strVal val="{v1}"/></p:val></p:tav>'
            f'</p:tavLst></p:anim>')

def beh_scale(spid, dur, frm=60):
    return (f'<p:animScale><p:cBhvr><p:cTn id="{nid()}" dur="{dur}" fill="hold"/>'
            f'<p:tgtEl><p:spTgt spid="{spid}"/></p:tgtEl></p:cBhvr>'
            f'<p:from x="{frm*1000}" y="{frm*1000}"/><p:to x="100000" y="100000"/></p:animScale>')

def effect_xml(eff, spid, dur):
    b = beh_set(spid)
    if eff == 'fade':
        b += beh_anim_effect(spid, dur, 'fade'); pid, sub = 10, 0
    elif eff == 'wipeUp':
        b += beh_anim_effect(spid, dur, 'wipe(up)'); pid, sub = 22, 4
    elif eff == 'flyUp':
        b += beh_anim_xy(spid, dur, 'ppt_x', '#ppt_x', '#ppt_x')
        b += beh_anim_xy(spid, dur, 'ppt_y', '1+#ppt_h/2', '#ppt_y'); pid, sub = 2, 4
    elif eff == 'riseUp':
        b += beh_anim_effect(spid, dur, 'fade')
        b += beh_anim_xy(spid, dur, 'ppt_y', '#ppt_y+0.04', '#ppt_y'); pid, sub = 2, 4
    elif eff == 'zoom':
        b += beh_scale(spid, dur, 60)
        b += beh_anim_effect(spid, dur, 'fade'); pid, sub = 23, 16
    else:
        b += beh_anim_effect(spid, dur, 'fade'); pid, sub = 10, 0
    return pid, sub, b

def effect_par(eff, spid, delay, dur):
    outer = nid(); wrap = nid()
    pid, sub, behaviors = effect_xml(eff, spid, dur)
    e = nid()
    return (f'<p:par><p:cTn id="{outer}" fill="hold">'
            f'<p:stCondLst><p:cond delay="{delay}"/></p:stCondLst>'
            f'<p:childTnLst><p:par><p:cTn id="{wrap}" fill="hold">'
            f'<p:stCondLst><p:cond delay="0"/></p:stCondLst>'
            f'<p:childTnLst><p:par><p:cTn id="{e}" presetID="{pid}" presetClass="entr" '
            f'presetSubtype="{sub}" fill="hold" grpId="0" nodeType="afterEffect">'
            f'<p:stCondLst><p:cond delay="0"/></p:stCondLst>'
            f'<p:childTnLst>{behaviors}</p:childTnLst>'
            f'</p:cTn></p:par></p:childTnLst>'
            f'</p:cTn></p:par></p:childTnLst>'
            f'</p:cTn></p:par>')

def timing_xml(effects):
    root = nid(); seq = nid(); grp = nid()
    body = ''.join(effect_par(eff, spid, d, dur) for eff, spid, d, dur in effects)
    return (f'<p:timing xmlns:p="{NS_P}" xmlns:a="{NS_A}">'
            f'<p:tnLst><p:par><p:cTn id="{root}" dur="indefinite" restart="never" nodeType="tmRoot">'
            f'<p:childTnLst><p:seq concurrent="1" nextAc="seek">'
            f'<p:cTn id="{seq}" dur="indefinite" nodeType="mainSeq"><p:childTnLst>'
            f'<p:par><p:cTn id="{grp}" fill="hold">'
            f'<p:stCondLst><p:cond delay="indefinite"/>'
            f'<p:cond evt="onBegin" delay="0"><p:tn val="{seq}"/></p:cond></p:stCondLst>'
            f'<p:childTnLst>{body}</p:childTnLst>'
            f'</p:cTn></p:par>'
            f'</p:childTnLst></p:cTn>'
            f'<p:prevCondLst><p:cond evt="onPrev" delay="0"><p:tgtEl><p:sldTgt/></p:tgtEl></p:cond></p:prevCondLst>'
            f'<p:nextCondLst><p:cond evt="onNext" delay="0"><p:tgtEl><p:sldTgt/></p:tgtEl></p:cond></p:nextCondLst>'
            f'</p:seq></p:childTnLst></p:cTn></p:par></p:tnLst></p:timing>')

def make_test(name, effects):
    ID[0] = 0
    prs = Presentation()
    prs.slide_width = Inches(20); prs.slide_height = Inches(11.25)
    s = prs.slides.add_slide(prs.slide_layouts[6])
    # three shapes so stagger is real
    for k in range(3):
        sp = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(1 + k*2), Inches(1), Inches(1.6), Inches(1))
        sp.fill.solid()
    prs.save(name)
    zin = zipfile.ZipFile(name)
    entries = {n: zin.read(n) for n in zin.namelist()}
    zin.close()
    root = etree.fromstring(entries['ppt/slides/slide1.xml'])
    ids = [sp.shape_id for sp in s.shapes]
    effs = [(effects[i % len(effects)], ids[i % len(ids)], i * 200, 500) for i in range(len(ids))]
    root.append(etree.fromstring(timing_xml(effs)))
    entries['ppt/slides/slide1.xml'] = etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)
    with zipfile.ZipFile(name, 'w', zipfile.ZIP_DEFLATED) as z:
        for n, data in entries.items():
            z.writestr(n, data)

make_test("t_fade.pptx", ['fade'])
make_test("t_wipe.pptx", ['wipeUp'])
make_test("t_fly.pptx", ['flyUp'])
make_test("t_rise.pptx", ['riseUp'])
make_test("t_zoom.pptx", ['zoom'])
print('built 5 effect test files')
