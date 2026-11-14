"""Inject entrance animations (p:timing) and slide transitions (p:transition) into the deck.
Uses the PowerPoint-canonical 3-level timing structure (validated via COM bisect):
  clickGroup par (delay=indefinite + onBegin auto-start)
    > midPar (stagger delay)
      > preset par (entrance effect)
Every slide auto-plays a staggered cascade when it appears.
"""
import zipfile
from lxml import etree

NS_P = 'http://schemas.openxmlformats.org/presentationml/2006/main'
NS_A = 'http://schemas.openxmlformats.org/drawingml/2006/main'
NS_P14 = 'http://schemas.microsoft.com/office/powerpoint/2010/main'

SRC = "ClaimChain_Modern_Animated.pptx"
OUT = "ClaimChain_Modern_Animated_v2.pptx"

ID = [0]
def nid():
    ID[0] += 1
    return ID[0]

# ---------------- behaviors ----------------
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

# ---------------- effect presets ----------------
def effect_xml(eff, spid, dur):
    b = beh_set(spid)
    if eff == 'fade':
        b += beh_anim_effect(spid, dur, 'fade'); pid, sub = 10, 0
    elif eff == 'wipeUp':
        b += beh_anim_effect(spid, dur, 'wipe(up)'); pid, sub = 22, 4
    elif eff == 'wipeRight':
        b += beh_anim_effect(spid, dur, 'wipe(right)'); pid, sub = 22, 2
    elif eff == 'flyUp':
        b += beh_anim_xy(spid, dur, 'ppt_x', '#ppt_x', '#ppt_x')
        b += beh_anim_xy(spid, dur, 'ppt_y', '1+#ppt_h/2', '#ppt_y'); pid, sub = 2, 4
    elif eff == 'flyLeft':
        b += beh_anim_xy(spid, dur, 'ppt_x', '0-#ppt_w/2', '#ppt_x')
        b += beh_anim_xy(spid, dur, 'ppt_y', '#ppt_y', '#ppt_y'); pid, sub = 2, 8
    elif eff == 'flyRight':
        b += beh_anim_xy(spid, dur, 'ppt_x', '1+#ppt_w/2', '#ppt_x')
        b += beh_anim_xy(spid, dur, 'ppt_y', '#ppt_y', '#ppt_y'); pid, sub = 2, 2
    elif eff == 'riseUp':  # fade + slight upward drift
        b += beh_anim_effect(spid, dur, 'fade')
        b += beh_anim_xy(spid, dur, 'ppt_y', '#ppt_y+0.04', '#ppt_y'); pid, sub = 2, 4
    elif eff == 'zoom':
        b += beh_scale(spid, dur, 60)
        b += beh_anim_effect(spid, dur, 'fade'); pid, sub = 23, 16
    else:
        b += beh_anim_effect(spid, dur, 'fade'); pid, sub = 10, 0
    return pid, sub, b

def effect_par(eff, spid, delay, dur):
    """Canonical 3-level: midPar(delay) > presetPar(0) > behaviors."""
    mid, e = nid(), nid()
    pid, sub, behaviors = effect_xml(eff, spid, dur)
    return (f'<p:par><p:cTn id="{mid}" fill="hold">'
            f'<p:stCondLst><p:cond delay="{delay}"/></p:stCondLst>'
            f'<p:childTnLst><p:par><p:cTn id="{e}" presetID="{pid}" presetClass="entr" '
            f'presetSubtype="{sub}" fill="hold" grpId="0" nodeType="afterEffect">'
            f'<p:stCondLst><p:cond delay="0"/></p:stCondLst>'
            f'<p:childTnLst>{behaviors}</p:childTnLst>'
            f'</p:cTn></p:par></p:childTnLst>'
            f'</p:cTn></p:par>')

def timing_xml(effects):
    """effects: list of (eff, spid, delay, dur). One auto-start click group; staggered delays."""
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

TRANSITIONS = {
    0: '<p:fade/>',
    1: '<p:push dir="u"/>',
    2: '<p:fade/>',
    3: '<p:wipe dir="r"/>',
    4: '<p:push dir="l"/>',
    5: '<p:fade/>',
    6: '<p:wipe dir="r"/>',
    7: '<p:push dir="u"/>',
    8: '<p:fade thruBlk="1"/>',
}

def transition_xml(idx):
    inner = TRANSITIONS.get(idx, '<p:fade/>')
    return (f'<p:transition xmlns:p="{NS_P}" xmlns:p14="{NS_P14}" spd="med" p14:dur="700">{inner}</p:transition>')

# ---------------- schedules ----------------
def sched_generic(shape_ids):
    """Content slides: header cascade, then staggered content cascade."""
    effects = []
    hdr = ['flyLeft', 'flyLeft', 'fade', 'wipeUp']
    delays = [0, 120, 260, 400]
    cycle = ['riseUp', 'flyUp', 'fade', 'zoom']
    for i, spid in enumerate(shape_ids):
        if i == 0:
            continue  # background
        if i <= 4:
            effects.append((hdr[i - 1], spid, delays[i - 1], 550))
        else:
            j = i - 5
            effects.append((cycle[j % 4], spid, 560 + j * 90, 550))
    return effects

def sched_title(shape_ids):
    """Slide 1 — cinematic build."""
    plan = [
        ('fade', 1, 0, 900), ('fade', 2, 0, 900),
        ('zoom', 3, 250, 700), ('zoom', 4, 350, 700), ('zoom', 5, 450, 700),
        ('zoom', 6, 500, 650), ('fade', 7, 800, 450),
        ('flyUp', 8, 900, 650), ('fade', 9, 1200, 500),
    ]
    for k in range(3):
        plan.append(('zoom', 10 + k, 1350 + k * 120, 400))
    for t in range(5):
        base = 1750 + t * 110
        plan.append(('flyUp', 13 + t * 2, base, 450))
        plan.append(('fade', 14 + t * 2, base + 80, 400))
    plan.append(('fade', 23, 2450, 600))
    return [(eff, shape_ids[idx], d, dur) for eff, idx, d, dur in plan if idx < len(shape_ids)]

def sched_thanks(shape_ids):
    plan = [('fade', 1, 0, 900), ('fade', 2, 0, 900),
            ('zoom', 3, 250, 700), ('zoom', 4, 350, 700),
            ('zoom', 5, 500, 650), ('flyUp', 6, 800, 650),
            ('fade', 7, 1150, 500), ('fade', 8, 1400, 500), ('fade', 9, 1650, 500)]
    return [(eff, shape_ids[idx], d, dur) for eff, idx, d, dur in plan if idx < len(shape_ids)]

# ---------------- main ----------------
from pptx import Presentation
prs = Presentation(SRC)
slide_shape_ids = [[sp.shape_id for sp in slide.shapes] for slide in prs.slides]

schedules = []
for idx in range(9):
    if idx == 0:
        schedules.append(sched_title(slide_shape_ids[0]))
    elif idx == 8:
        schedules.append(sched_thanks(slide_shape_ids[8]))
    else:
        schedules.append(sched_generic(slide_shape_ids[idx]))

zin = zipfile.ZipFile(SRC)
entries = {n: zin.read(n) for n in zin.namelist()}
zin.close()

for idx in range(9):
    name = f'ppt/slides/slide{idx+1}.xml'
    root = etree.fromstring(entries[name])
    ID[0] = 0
    root.append(etree.fromstring(transition_xml(idx)))
    root.append(etree.fromstring(timing_xml(schedules[idx])))
    entries[name] = etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)
    print(f'slide{idx+1}: {len(schedules[idx])} animated elements, transition={TRANSITIONS.get(idx)}')

with zipfile.ZipFile(OUT, 'w', zipfile.ZIP_DEFLATED) as z:
    for n, data in entries.items():
        z.writestr(n, data)

prs2 = Presentation(OUT)
print(f'\nValidated reopen: {OUT} with {len(prs2.slides)} slides')
