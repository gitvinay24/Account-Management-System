"""Build ClaimChain modern animated deck — dark fintech theme, native editable shapes."""
from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE, MSO_CONNECTOR
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
import copy

# ---------- palette ----------
BG      = RGBColor(0x0A, 0x0F, 0x1E)   # deep navy-black
BG2     = RGBColor(0x0E, 0x14, 0x28)
CARD    = RGBColor(0x12, 0x1A, 0x30)   # card surface
CARD2   = RGBColor(0x0F, 0x17, 0x2B)
STROKE  = RGBColor(0x1F, 0x2B, 0x45)
TEAL    = RGBColor(0x2D, 0xD4, 0xBF)   # primary accent
TEAL_D  = RGBColor(0x14, 0xB8, 0xA6)
BLUE    = RGBColor(0x38, 0xBD, 0xF8)
PURPLE  = RGBColor(0xA7, 0x8B, 0xFA)
PINK    = RGBColor(0xF4, 0x72, 0xB6)
AMBER   = RGBColor(0xFB, 0xBF, 0x24)
GREEN   = RGBColor(0x4A, 0xDE, 0x80)
RED     = RGBColor(0xFB, 0x71, 0x85)
WHITE   = RGBColor(0xFF, 0xFF, 0xFF)
HEAD    = RGBColor(0xF8, 0xFA, 0xFC)
BODY    = RGBColor(0xC7, 0xD2, 0xE0)
MUTED   = RGBColor(0x8B, 0x98, 0xAE)
DARKTXT = RGBColor(0x06, 0x0A, 0x14)   # text on bright accents

FONT = "Segoe UI"
FONT_H = "Segoe UI"     # headings; PowerPoint falls back cleanly
MONO  = "Consolas"

SLIDE_W = Inches(20)
SLIDE_H = Inches(11.25)

prs = Presentation()
prs.slide_width = SLIDE_W
prs.slide_height = SLIDE_H
BLANK = prs.slide_layouts[6]

# ---------- helpers ----------
def add_slide():
    return prs.slides.add_slide(BLANK)

def rect(slide, x, y, w, h, fill=None, line=None, line_w=0.75, shape=MSO_SHAPE.ROUNDED_RECTANGLE, radius=0.08, shadow=False):
    sp = slide.shapes.add_shape(shape, x, y, w, h)
    if shape == MSO_SHAPE.ROUNDED_RECTANGLE and radius is not None:
        try:
            sp.adjustments[0] = radius
        except Exception:
            pass
    if fill is None:
        sp.fill.background()
    else:
        sp.fill.solid()
        sp.fill.fore_color.rgb = fill
    if line is None:
        sp.line.fill.background()
    else:
        sp.line.color.rgb = line
        sp.line.width = Pt(line_w)
    sp.shadow.inherit = False
    return sp

def txt(slide, x, y, w, h, runs, size=14, color=BODY, bold=False, font=FONT,
        align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.TOP, spacing=1.0, space_after=4, wrap=True):
    """runs: str, or list of paragraphs; each paragraph str or list of (text, opts) tuples."""
    tb = slide.shapes.add_textbox(x, y, w, h)
    tf = tb.text_frame
    tf.word_wrap = wrap
    tf.vertical_anchor = anchor
    tf.margin_left = tf.margin_right = tf.margin_top = tf.margin_bottom = 0
    if isinstance(runs, str):
        runs = [runs]
    for i, para in enumerate(runs):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = align
        p.line_spacing = spacing
        p.space_after = Pt(space_after)
        if isinstance(para, str):
            para = [(para, {})]
        for item in para:
            if isinstance(item, str):
                t, o = item, {}
            else:
                t, o = item
            r = p.add_run()
            r.text = t
            f = r.font
            f.name = o.get("font", font)
            f.size = Pt(o.get("size", size))
            f.bold = o.get("bold", bold)
            f.italic = o.get("italic", False)
            f.color.rgb = o.get("color", color)
    return tb

def icon_badge(slide, x, y, d, glyph, accent, size=15):
    """Circle with a simple glyph text — used instead of image icons."""
    c = rect(slide, x, y, d, d, fill=None, line=accent, line_w=1.5, shape=MSO_SHAPE.OVAL)
    c2 = rect(slide, x, y, d, d, fill=accent, line=None, shape=MSO_SHAPE.OVAL)
    c2.fill.transparency = 0  # keep solid
    c2._element.getparent().remove(c2._element)  # replace with translucent version below
    return c

def badge(slide, x, y, d, glyph, accent):
    c = rect(slide, x, y, d, d, fill=CARD2, line=accent, line_w=1.25, shape=MSO_SHAPE.OVAL)
    tf = c.text_frame
    tf.word_wrap = False
    tf.margin_left = tf.margin_right = tf.margin_top = tf.margin_bottom = 0
    tf.vertical_anchor = MSO_ANCHOR.MIDDLE
    p = tf.paragraphs[0]
    p.alignment = PP_ALIGN.CENTER
    r = p.add_run(); r.text = glyph
    r.font.name = FONT; r.font.size = Pt(d.inches * 20); r.font.bold = True; r.font.color.rgb = accent
    return c

def chip(slide, x, y, w, h, label, accent):
    sp = rect(slide, x, y, w, h, fill=CARD2, line=accent, line_w=1, radius=0.5)
    tf = sp.text_frame
    tf.word_wrap = False
    tf.margin_left = tf.margin_right = Inches(0.06)
    tf.margin_top = tf.margin_bottom = 0
    tf.vertical_anchor = MSO_ANCHOR.MIDDLE
    p = tf.paragraphs[0]; p.alignment = PP_ALIGN.CENTER
    r = p.add_run(); r.text = label
    r.font.name = FONT; r.font.size = Pt(11); r.font.color.rgb = accent; r.font.bold = True
    return sp

def header(slide, kicker, title_runs, accent=TEAL, subtitle=None):
    txt(slide, Inches(0.9), Inches(0.42), Inches(12), Inches(0.35),
        kicker, size=13, color=accent, bold=True)
    txt(slide, Inches(0.9), Inches(0.72), Inches(14), Inches(1.0),
        title_runs, size=40, color=HEAD, bold=True)
    if subtitle:
        txt(slide, Inches(0.9), Inches(1.62), Inches(14), Inches(0.4),
            subtitle, size=15, color=MUTED)
    # accent underline
    u = rect(slide, Inches(0.92), Inches(1.5 if not subtitle else 2.05), Inches(1.6), Pt(3), fill=accent)
    return u

def footer(slide, page):
    txt(slide, Inches(0.9), Inches(10.72), Inches(6), Inches(0.3),
        [[("ClaimChain", {"bold": True, "color": TEAL}), ("  ·  Smarter Claims. Stronger Trust.", {"color": MUTED})]], size=10)
    txt(slide, Inches(18.6), Inches(10.72), Inches(0.6), Inches(0.3),
        f"{page:02d}", size=10, color=MUTED, align=PP_ALIGN.RIGHT)
    ln = rect(slide, Inches(0.9), Inches(10.62), Inches(18.2), Pt(1), fill=STROKE)

def glow_card(slide, x, y, w, h, accent=TEAL, fill=CARD):
    c = rect(slide, x, y, w, h, fill=fill, line=accent, line_w=1.25, radius=0.06)
    return c

# =========================================================
# SLIDE 1 — TITLE
# =========================================================
s = add_slide()
# background gradient block
bgshape = rect(s, 0, 0, SLIDE_W, SLIDE_H, fill=BG, line=None, shape=MSO_SHAPE.RECTANGLE)
# decorative corner glows
c1 = rect(s, Inches(-2), Inches(-2), Inches(7), Inches(7), fill=RGBColor(0x11,0x2A,0x2C), line=None, shape=MSO_SHAPE.OVAL)
c2 = rect(s, Inches(15), Inches(6.5), Inches(8), Inches(8), fill=RGBColor(0x0F,0x1E,0x3A), line=None, shape=MSO_SHAPE.OVAL)
# halo rings
for d, alpha in [(4.2, TEAL), (5.4, STROKE), (6.6, STROKE)]:
    rect(s, Inches(10 - d/2), Inches(3.6 - d/2), Inches(d), Inches(d), fill=None, line=alpha, line_w=1, shape=MSO_SHAPE.OVAL)
# center leaf logo
pic = s.shapes.add_picture("leaf_teal.png", Inches(10-0.62), Inches(3.6-0.62), Inches(1.24), Inches(1.29))
# kicker chip
chip(s, Inches(8.28), Inches(5.35), Inches(3.44), Inches(0.46), "HACKINDIA 2026  ·  TEAM METAMASHERS", TEAL)
# headline
txt(s, Inches(1.5), Inches(6.05), Inches(17), Inches(1.5),
    [[("Claim", {"color": HEAD}), ("Chain", {"color": TEAL})]],
    size=76, bold=True, align=PP_ALIGN.CENTER)
# tagline
txt(s, Inches(3), Inches(7.55), Inches(14), Inches(0.5),
    "AI-verified micro-insurance claims, settled on-chain in minutes — not weeks.",
    size=18, color=BODY, align=PP_ALIGN.CENTER)
# divider dots
for i, dx in enumerate([9.35, 9.9, 10.45]):
    rect(s, Inches(dx), Inches(8.32), Inches(0.1), Inches(0.1), fill=[TEAL, BLUE, PURPLE][i], shape=MSO_SHAPE.OVAL)
# team strip
names = ["Sapna Kumari", "Vinay Garg", "Vikas Bagla", "Utkarsh Vijay", "Yash Gupta"]
for i, n in enumerate(names):
    x = 4.55 + i * 2.28
    b = badge(s, Inches(x + 0.72), Inches(8.75), Inches(0.52), "◆", [TEAL, BLUE, PURPLE, PINK, AMBER][i])
    txt(s, Inches(x), Inches(9.42), Inches(2.0), Inches(0.3), n, size=12, color=BODY, align=PP_ALIGN.CENTER)
txt(s, Inches(0.9), Inches(10.72), Inches(18.2), Inches(0.3),
    "PROBLEM  ·  Micro-insurance claims take weeks, cost more to verify than they pay out, and can't be trusted once decided.",
    size=11, color=MUTED, align=PP_ALIGN.CENTER)

# =========================================================
# SLIDE 2 — INTRODUCTION
# =========================================================
s = add_slide()
rect(s, 0, 0, SLIDE_W, SLIDE_H, fill=BG, line=None, shape=MSO_SHAPE.RECTANGLE)
header(s, "01 · INTRODUCTION", [[("Why ClaimChain ", {"color": HEAD}), ("exists", {"color": TEAL})]],
       subtitle="Five reasons this problem is worth solving — and why now.")
cards = [
    ("01", "Solves Key Challenges", "Delayed claim processing, high verification costs and rampant insurance fraud drain the system.", RED),
    ("02", "AI-Powered Automation", "AI automates claim submission, document verification, damage assessment, fraud detection and decisions.", BLUE),
    ("03", "Blockchain Transparency", "Every claim event is recorded in a tamper-evident audit trail — transparent and accountable.", TEAL),
    ("04", "For People Who Matter", "Farmers, daily-wage earners, gig workers and low-income families get faster, fairer settlements.", AMBER),
    ("05", "A More Trustworthy Future", "Same engine extends to government subsidies, disaster relief, scholarships and warranties.", PURPLE),
]
cw, ch, gap = 3.44, 6.9, 0.24
x0 = (20 - (cw * 5 + gap * 4)) / 2
for i, (num, t, d, acc) in enumerate(cards):
    x = x0 + i * (cw + gap)
    glow_card(s, Inches(x), Inches(2.75), Inches(cw), Inches(ch), accent=acc, fill=CARD)
    txt(s, Inches(x + 0.28), Inches(3.05), Inches(cw - 0.56), Inches(0.8),
        num, size=30, bold=True, color=acc)
    txt(s, Inches(x + 0.28), Inches(3.95), Inches(cw - 0.56), Inches(1.1),
        t, size=17, bold=True, color=HEAD, spacing=1.02)
    txt(s, Inches(x + 0.28), Inches(5.15), Inches(cw - 0.56), Inches(4.2),
        d, size=13, color=BODY, spacing=1.18)
footer(s, 2)

# =========================================================
# SLIDE 3 — KEY FEATURES
# =========================================================
s = add_slide()
rect(s, 0, 0, SLIDE_W, SLIDE_H, fill=BG, line=None, shape=MSO_SHAPE.RECTANGLE)
header(s, "02 · KEY FEATURES", [[("Five pillars of the ", {"color": HEAD}), ("ClaimChain engine", {"color": TEAL})]],
       subtitle="From claim submission to payout — every step automated, verified and auditable.")
feats = [
    ("1", "AI-Powered Claim Submission", [("Upload damage photos, bills & identity docs from a smartphone.", 0), ("AI auto-extracts paperwork and data.", 0)], BLUE, "⇪"),
    ("2", "Automated Claim Verification", [("Document AI extracts policy and bill information.", 0), ("Computer vision assesses damage.", 0), ("Cross-checks policy terms & past claims.", 0)], PURPLE, "✓"),
    ("3", "AI-Based Fraud Detection", [("Detects duplicate claims and reused images.", 0), ("Identifies digitally altered photos.", 0), ("Flags suspicious docs and inflated bills.", 0)], PINK, "⚑"),
    ("4", "Instant Claim Decision", [("Approves, flags or escalates claims in under a minute.", 0), ("Clear explanations for every decision.", 0)], AMBER, "⚡"),
    ("5", "Blockchain Audit Trail", [("Every claim event hashed cryptographically.", 0), ("Tamper-evident history of decisions.", 0), ("Detects unauthorized changes.", 0)], GREEN, "⛓"),
]
cw, ch, gap = 3.44, 6.6, 0.24
x0 = (20 - (cw * 5 + gap * 4)) / 2
for i, (num, t, bullets, acc, glyph) in enumerate(feats):
    x = x0 + i * (cw + gap)
    glow_card(s, Inches(x), Inches(2.75), Inches(cw), Inches(ch), accent=acc, fill=CARD)
    badge(s, Inches(x + 0.28), Inches(3.05), Inches(0.66), glyph, acc)
    txt(s, Inches(x + 1.08), Inches(3.1), Inches(cw - 1.3), Inches(1.0),
        t, size=15.5, bold=True, color=HEAD, spacing=1.02)
    yy = 4.45
    for b, _ in bullets:
        rect(s, Inches(x + 0.3), Inches(yy + 0.09), Inches(0.09), Inches(0.09), fill=acc, shape=MSO_SHAPE.OVAL)
        txt(s, Inches(x + 0.52), Inches(yy), Inches(cw - 0.82), Inches(1.4), b, size=12.5, color=BODY, spacing=1.12)
        yy += 1.62 if len(b) > 48 else 1.28
footer(s, 3)

# =========================================================
# SLIDE 4 — FLOWCHART (pipeline)
# =========================================================
s = add_slide()
rect(s, 0, 0, SLIDE_W, SLIDE_H, fill=BG, line=None, shape=MSO_SHAPE.RECTANGLE)
header(s, "03 · HOW IT WORKS", [[("From claim to ", {"color": HEAD}), ("trusted payout", {"color": TEAL})]],
       subtitle="Every hop is hashed onto an immutable blockchain audit trail.")

steps = [
    ("1", "Claim Submission", "User uploads photos, bills, ID docs via mobile app.", BLUE),
    ("2", "AI Document Processing", "OCR extracts text, validates documents, checks authenticity.", PURPLE),
    ("3", "AI Verification & Fraud Detection", "Damage assessment, duplicate/reused-image detection, policy cross-check.", PINK),
    ("4", "Claim Decision", "Approve, flag or escalate — AI decides in under a minute.", AMBER),
    ("5", "Process Payout", "Approved: instant credit. Flagged: human review, then payout or rejection.", GREEN),
]
cw, chh, gap = 3.5, 3.3, 0.32
x0 = (20 - (cw * 5 + gap * 4)) / 2
for i, (num, t, d, acc) in enumerate(steps):
    x = x0 + i * (cw + gap)
    glow_card(s, Inches(x), Inches(2.7), Inches(cw), Inches(chh), accent=acc, fill=CARD)
    b = badge(s, Inches(x + 0.24), Inches(2.94), Inches(0.5), num, acc)
    txt(s, Inches(x + 0.88), Inches(2.96), Inches(cw - 1.1), Inches(0.9), t, size=14.5, bold=True, color=HEAD, spacing=1.0)
    txt(s, Inches(x + 0.26), Inches(4.0), Inches(cw - 0.52), Inches(1.8), d, size=12, color=BODY, spacing=1.15)
    if i < 4:
        ar = rect(s, Inches(x + cw + 0.04), Inches(4.12), Inches(0.24), Inches(0.24), fill=acc, shape=MSO_SHAPE.CHEVRON)

# decision branch chips
branch = rect(s, Inches(15.6), Inches(6.4), Inches(3.9), Inches(2.9), fill=CARD2, line=RED, line_w=1, radius=0.1)
txt(s, Inches(15.85), Inches(6.6), Inches(3.4), Inches(0.4),
    [[("When AI flags a claim", {"color": RED, "bold": True})]], size=13)
txt(s, Inches(15.85), Inches(7.05), Inches(3.45), Inches(2.1),
    [["•  Flagged claims route to a human reviewer."],
     ["•  Reviewer verifies AI details, approves or rejects."],
     ["•  Every review action is logged on-chain."]],
    size=11.5, color=BODY, spacing=1.15, space_after=6)
for i, run in enumerate(branch.text_frame.paragraphs):
    pass  # bullets drawn as text above

# audit band
band = rect(s, Inches(0.9), Inches(6.35), Inches(14.4), Inches(2.95), fill=CARD, line=TEAL, line_w=1, radius=0.07)
txt(s, Inches(1.2), Inches(6.55), Inches(12), Inches(0.4),
    [[("⛓  Blockchain Audit Trail", {"bold": True, "color": TEAL}), ("   — every step hashed and immutable", {"color": MUTED})]], size=14)
hashes = ["Submission", "Verification", "Decision", "Human Review", "Payout"]
hx = 1.2
for i, hname in enumerate(hashes):
    chip(s, Inches(hx), Inches(7.15), Inches(2.3), Inches(0.5), f"{hname} hash", TEAL if i % 2 == 0 else BLUE)
    if i < 4:
        txt(s, Inches(hx + 2.32), Inches(7.2), Inches(0.3), Inches(0.4), "→", size=16, color=MUTED)
    hx += 2.62
txt(s, Inches(1.2), Inches(7.95), Inches(13.8), Inches(1.2),
    [["✓  Tamper-evident records    ✓  Transparent & verifiable    ✓  Cannot be altered or deleted"],
     ["Customers, regulators and authorized parties can verify any claim using its hash — without private data."]],
    size=12, color=BODY, spacing=1.25)

footer(s, 4)

# =========================================================
# SLIDE 5 — WHY CLAIMCHAIN
# =========================================================
s = add_slide()
rect(s, 0, 0, SLIDE_W, SLIDE_H, fill=BG, line=None, shape=MSO_SHAPE.RECTANGLE)
header(s, "04 · WHY CLAIMCHAIN", [[("Six reasons it ", {"color": HEAD}), ("changes the game", {"color": TEAL})]],
       subtitle="Faster claims. Greater impact. — benefits that compound for everyone in the system.")

left = [
    ("Faster Claim Processing", "Claims settled in minutes instead of weeks — timely support when it's needed most.", TEAL, "⚡"),
    ("Reduced Operational Costs", "Automated verification minimizes manual effort, making small claims profitable to serve.", AMBER, "◈"),
    ("Fraud Prevention", "AI detects fake documents, duplicate claims and manipulated images before payout.", PINK, "⚑"),
]
right = [
    ("Greater Transparency", "Every event recorded on a blockchain — a tamper-evident trail customers and regulators can verify.", BLUE, "⛓"),
    ("Improved Trust", "Customers, insurers and regulators can independently verify decisions, building system-wide confidence.", PURPLE, "✓"),
    ("Scalable Impact", "Same engine extends to subsidies, disaster relief, scholarships, warranties — high fraud risk, low trust, solved.", GREEN, "▲"),
]
colw, colh = 7.1, 1.98
for i, (t, d, acc, glyph) in enumerate(left):
    y = 2.75 + i * 2.22
    glow_card(s, Inches(0.9), Inches(y), Inches(colw), Inches(colh), accent=acc, fill=CARD)
    badge(s, Inches(1.14), Inches(y + 0.55), Inches(0.8), glyph, acc)
    txt(s, Inches(2.14), Inches(y + 0.22), Inches(colw - 1.5), Inches(0.5), t, size=16.5, bold=True, color=acc)
    txt(s, Inches(2.14), Inches(y + 0.78), Inches(colw - 1.5), Inches(1.1), d, size=12.5, color=BODY, spacing=1.15)
for i, (t, d, acc, glyph) in enumerate(right):
    y = 2.75 + i * 2.22
    glow_card(s, Inches(12.0), Inches(y), Inches(colw), Inches(colh), accent=acc, fill=CARD)
    badge(s, Inches(12.24), Inches(y + 0.55), Inches(0.8), glyph, acc)
    txt(s, Inches(13.24), Inches(y + 0.22), Inches(colw - 1.5), Inches(0.5), t, size=16.5, bold=True, color=acc)
    txt(s, Inches(13.24), Inches(y + 0.78), Inches(colw - 1.5), Inches(1.1), d, size=12.5, color=BODY, spacing=1.15)

# center emblem
cc = rect(s, Inches(8.55), Inches(4.3), Inches(2.9), Inches(2.9), fill=BG2, line=TEAL, line_w=1.5, shape=MSO_SHAPE.OVAL)
s.shapes.add_picture("leaf_teal.png", Inches(8.55+1.45-0.5), Inches(4.3+1.05), Inches(1.0), Inches(1.04))
txt(s, Inches(8.55), Inches(6.35), Inches(2.9), Inches(0.4), "ClaimChain", size=15, bold=True, color=HEAD, align=PP_ALIGN.CENTER)
txt(s, Inches(8.55), Inches(6.72), Inches(2.9), Inches(0.3), "Faster Claims. Greater Impact.", size=9.5, color=MUTED, align=PP_ALIGN.CENTER)

# benefits-for-all strip
strip = rect(s, Inches(0.9), Inches(9.45), Inches(18.2), Inches(1.0), fill=CARD2, line=STROKE, line_w=1, radius=0.12)
lbl = rect(s, Inches(0.9), Inches(9.45), Inches(2.1), Inches(1.0), fill=TEAL, line=None, radius=0.12)
tf = lbl.text_frame; tf.vertical_anchor = MSO_ANCHOR.MIDDLE
p = tf.paragraphs[0]; p.alignment = PP_ALIGN.CENTER
r = p.add_run(); r.text = "Benefits\nFor All"; r.font.bold = True; r.font.size = Pt(13); r.font.color.rgb = DARKTXT; r.font.name = FONT
cols = [("Customers", "Faster payouts, clearer decisions, more trust."),
        ("Insurers", "Lower costs, less fraud, higher efficiency."),
        ("Regulators", "Independent verification and stronger oversight."),
        ("Communities", "Greater financial security and social impact.")]
cx = 3.3
for i, (t, d) in enumerate(cols):
    txt(s, Inches(cx), Inches(9.58), Inches(3.9), Inches(0.35), t, size=12.5, bold=True, color=TEAL if i % 2 == 0 else BLUE)
    txt(s, Inches(cx), Inches(9.95), Inches(3.9), Inches(0.45), d, size=10.5, color=MUTED)
    if i < 3:
        rect(s, Inches(cx + 3.78), Inches(9.6), Pt(1), Inches(0.7), fill=STROKE)
    cx += 3.95
footer(s, 5)

# =========================================================
# SLIDE 6 — RISK FACTORS
# =========================================================
s = add_slide()
rect(s, 0, 0, SLIDE_W, SLIDE_H, fill=BG, line=None, shape=MSO_SHAPE.RECTANGLE)
header(s, "05 · RISK FACTORS", [[("Why today's system is ", {"color": HEAD}), ("high risk, low trust", {"color": RED})]],
       subtitle="Challenges in today's claim processing — delays, costs and fraud compound each other.")
risks = [
    ("Fraudulent Claims", "Fake photographs, duplicate claims, inflated bills and forged documents.", RED, "⚑"),
    ("High Verification Cost", "Manual verification can cost nearly as much as the claim payout itself.", AMBER, "₹"),
    ("Delayed Processing", "Honest customers wait weeks for decisions that should take minutes.", PURPLE, "⏳"),
    ("Manipulated or Altered Media", "Images and documents digitally altered or reused across multiple claims.", PINK, "✎"),
    ("Policy Non-Compliance", "Claims that miss policy terms or eligibility criteria cause incorrect payouts.", BLUE, "📋"),
    ("Organized Fraud Networks", "Coordinated rings file multiple false claims across insurers.", RED, "🔒"),
]
pos = [(0.9, 2.75), (0.9, 4.95), (0.9, 7.15), (12.0, 2.75), (12.0, 4.95), (12.0, 7.15)]
for (t, d, acc, glyph), (x, y) in zip(risks, pos):
    glow_card(s, Inches(x), Inches(y), Inches(7.1), Inches(1.95), accent=acc, fill=CARD)
    badge(s, Inches(x + 0.24), Inches(y + 0.55), Inches(0.8), glyph, acc)
    txt(s, Inches(x + 1.24), Inches(y + 0.28), Inches(5.6), Inches(0.5), t, size=16, bold=True, color=acc)
    txt(s, Inches(x + 1.24), Inches(y + 0.85), Inches(5.6), Inches(0.9), d, size=12.5, color=BODY, spacing=1.15)

# center callout
cc = rect(s, Inches(8.35), Inches(3.6), Inches(3.3), Inches(3.6), fill=CARD2, line=RED, line_w=1.25, radius=0.1)
txt(s, Inches(8.5), Inches(4.0), Inches(3.0), Inches(1.6), "HIGH RISK,\nLOW TRUST", size=22, bold=True, color=RED, align=PP_ALIGN.CENTER, spacing=1.05)
txt(s, Inches(8.55), Inches(5.75), Inches(2.9), Inches(1.3),
    "These risks create delays, increase costs, and reduce confidence in the system.",
    size=11.5, color=BODY, align=PP_ALIGN.CENTER, spacing=1.15)
# bottom note
note = rect(s, Inches(0.9), Inches(9.45), Inches(18.2), Inches(0.85), fill=CARD2, line=STROKE, line_w=1, radius=0.2)
txt(s, Inches(1.3), Inches(9.63), Inches(16), Inches(0.5),
    [[("❧  ", {"color": GREEN, "bold": True}), ("Addressing these risks is essential to build a faster, fairer, and more trustworthy claims ecosystem.", {"color": BODY, "italic": True}),
      ("    — ClaimChain", {"color": MUTED})]], size=12.5)
footer(s, 6)

# =========================================================
# SLIDE 7 — TECH STACK
# =========================================================
s = add_slide()
rect(s, 0, 0, SLIDE_W, SLIDE_H, fill=BG, line=None, shape=MSO_SHAPE.RECTANGLE)
header(s, "06 · TECHNOLOGY STACK", [[("The stack powering ", {"color": HEAD}), ("safer tomorrows", {"color": TEAL})]],
       subtitle="Right tools for automation, transparency and real-world deployment.")

def tech_panel(x, y, w, h, title, sub, acc, items, item_w=1.62, glyph="◆"):
    glow_card(s, Inches(x), Inches(y), Inches(w), Inches(h), accent=acc, fill=CARD)
    rect(s, Inches(x + 0.26), Inches(y + 0.24), Inches(0.5), Inches(0.5), fill=None, line=acc, line_w=1.25, shape=MSO_SHAPE.OVAL)
    txt(s, Inches(x + 0.32), Inches(y + 0.3), Inches(0.4), Inches(0.4), glyph, size=15, bold=True, color=acc)
    txt(s, Inches(x + 0.9), Inches(y + 0.22), Inches(w - 1.2), Inches(0.4), title, size=15, bold=True, color=acc)
    txt(s, Inches(x + 0.9), Inches(y + 0.6), Inches(w - 1.2), Inches(0.3), sub, size=10.5, color=MUTED)
    ix = x + 0.26
    for name in items:
        chip(s, Inches(ix), Inches(y + 1.05), Inches(item_w - 0.06), Inches(0.9), "", acc)
        txt(s, Inches(ix + 0.02), Inches(y + 1.18), Inches(item_w - 0.1), Inches(0.7), name, size=10, color=BODY, align=PP_ALIGN.CENTER, spacing=1.0)
        ix += item_w

tech_panel(0.9, 2.65, 9.1, 2.35, "Core Technologies", "Key technologies that drive ClaimChain", TEAL,
           ["Artificial Intelligence", "Blockchain", "Computer Vision", "OCR", "Cryptographic Hashing"], 1.72, "⚙")
tech_panel(10.2, 2.65, 8.9, 2.35, "AI Tools & Techniques", "Intelligent tools for automation and fraud detection", BLUE,
           ["Doc AI (Document AI)", "AI-Based Fraud Detection", "Image Analysis", "Perceptual Hashing", "Damage Assessment"], 1.68, "✦")
tech_panel(0.9, 5.2, 9.1, 2.35, "Programming Languages", "Used for development and integration", PURPLE,
           ["Python (AI/Backend)", "Java (Backend)", "JavaScript (Frontend)", "React (Frontend)", "Solidity (Contracts)"], 1.72, "⌨")
tech_panel(10.2, 5.2, 8.9, 2.35, "Blockchain Components", "Building transparency and trust", GREEN,
           ["Immutable Ledger", "Hash Chain", "Tamper-Evident Audit Trail", "Smart Contracts (Ethereum)"], 2.1, "⛓")
tech_panel(0.9, 7.75, 18.2, 2.55, "Integration & Deployment", "Bringing everything together for real-world impact", AMBER,
           ["Cloud Platform (AWS/GCP)", "Database (MySQL/MongoDB)", "API Integration", "Human-in-the-Loop Verification", "Web / Mobile Application"], 3.5, "✚")
footer(s, 7)

# =========================================================
# SLIDE 8 — FUTURE SCOPE
# =========================================================
s = add_slide()
rect(s, 0, 0, SLIDE_W, SLIDE_H, fill=BG, line=None, shape=MSO_SHAPE.RECTANGLE)
header(s, "07 · FUTURE SCOPE", [[("Beyond insurance — ", {"color": HEAD}), ("a transparent tomorrow", {"color": TEAL})]],
       subtitle="The same AI + blockchain verification engine, expanding across sectors.")
future = [
    ("01", "Expansion to Government Subsidies", ["Automate subsidy verification & distribution", "Ensure transparency, prevent fraud"], TEAL),
    ("02", "Disaster Relief Management", ["Enable faster disaster assistance during natural disasters", "Verify claims using AI and satellite data"], BLUE),
    ("03", "Scholarship Verification", ["Automate document verification", "Prevent fake/duplicate applications"], PURPLE),
    ("04", "Warranty Claims Processing", ["Extend AI-based verification to product warranties", "Detect fraudulent warranty requests"], PINK),
    ("05", "Advanced AI Fraud Detection", ["Deep learning + better image analysis", "Detect complex fraud patterns"], AMBER),
    ("06", "Scalable Blockchain Integration", ["Handle large claim volumes", "Interoperability with other blockchains"], GREEN),
    ("07", "Improved Customer Accessibility", ["Simpler mobile/web apps, multilingual support", "Accessibility for rural communities"], RED),
    ("08", "Broader Industry Adoption", ["Healthcare, education, agriculture, enterprises", "Universal transparent benefit distribution"], TEAL_D),
]
pos = [(0.9, 2.7), (0.9, 4.62), (0.9, 6.54), (0.9, 8.46),
       (12.0, 2.7), (12.0, 4.62), (12.0, 6.54), (12.0, 8.46)]
for (num, t, bullets, acc), (x, y) in zip(future, pos):
    glow_card(s, Inches(x), Inches(y), Inches(7.1), Inches(1.72), accent=acc, fill=CARD)
    nb = rect(s, Inches(x + 0.22), Inches(y + 0.24), Inches(0.62), Inches(0.62), fill=CARD2, line=acc, line_w=1, radius=0.25)
    tf = nb.text_frame; tf.vertical_anchor = MSO_ANCHOR.MIDDLE
    p = tf.paragraphs[0]; p.alignment = PP_ALIGN.CENTER
    r = p.add_run(); r.text = num; r.font.bold = True; r.font.size = Pt(13); r.font.color.rgb = acc; r.font.name = MONO
    txt(s, Inches(x + 1.05), Inches(y + 0.2), Inches(5.9), Inches(0.45), t, size=14.5, bold=True, color=HEAD)
    yy = y + 0.72
    for b in bullets:
        rect(s, Inches(x + 1.07), Inches(yy + 0.08), Inches(0.08), Inches(0.08), fill=acc, shape=MSO_SHAPE.OVAL)
        txt(s, Inches(x + 1.27), Inches(yy), Inches(5.7), Inches(0.5), b, size=11, color=BODY)
        yy += 0.42

# center vision hub
hub = rect(s, Inches(8.6), Inches(4.4), Inches(2.8), Inches(2.8), fill=BG2, line=TEAL, line_w=1.5, shape=MSO_SHAPE.OVAL)
txt(s, Inches(8.6), Inches(5.15), Inches(2.8), Inches(0.5), "◎", size=34, color=TEAL, align=PP_ALIGN.CENTER)
txt(s, Inches(8.6), Inches(5.9), Inches(2.8), Inches(0.5), "Future Vision", size=14, bold=True, color=HEAD, align=PP_ALIGN.CENTER)
txt(s, Inches(8.7), Inches(6.35), Inches(2.6), Inches(0.7),
    "A global platform for AI-verified assistance.",
    size=9.5, color=MUTED, align=PP_ALIGN.CENTER, spacing=1.15)

# vision banner
vb = rect(s, Inches(0.9), Inches(10.35), Inches(18.2), Inches(0.55), fill=CARD2, line=TEAL, line_w=1, radius=0.5)
txt(s, Inches(1.5), Inches(10.44), Inches(15.5), Inches(0.4),
    [[("◎ Future Vision:  ", {"bold": True, "color": TEAL}),
      ("a fairer, more inclusive society — powered by transparent, AI-verified assistance across every sector.", {"color": BODY})]], size=11.5)
txt(s, Inches(18.3), Inches(10.44), Inches(0.6), Inches(0.35), "08", size=10, color=MUTED, align=PP_ALIGN.RIGHT)

# =========================================================
# SLIDE 9 — THANK YOU
# =========================================================
s = add_slide()
rect(s, 0, 0, SLIDE_W, SLIDE_H, fill=BG, line=None, shape=MSO_SHAPE.RECTANGLE)
c1 = rect(s, Inches(-2), Inches(-2), Inches(7), Inches(7), fill=RGBColor(0x11,0x2A,0x2C), line=None, shape=MSO_SHAPE.OVAL)
c2 = rect(s, Inches(15), Inches(6.5), Inches(8), Inches(8), fill=RGBColor(0x0F,0x1E,0x3A), line=None, shape=MSO_SHAPE.OVAL)
for d, acc in [(4.2, TEAL), (5.4, STROKE)]:
    rect(s, Inches(10 - d/2), Inches(3.9 - d/2), Inches(d), Inches(d), fill=None, line=acc, line_w=1, shape=MSO_SHAPE.OVAL)
s.shapes.add_picture("leaf_teal.png", Inches(10-0.5), Inches(3.9-0.55), Inches(1.0), Inches(1.04))
txt(s, Inches(2), Inches(5.35), Inches(16), Inches(1.2),
    [[("Thank ", {"color": HEAD}), ("You", {"color": TEAL})]], size=64, bold=True, align=PP_ALIGN.CENTER)
txt(s, Inches(3), Inches(6.65), Inches(14), Inches(0.5),
    "Questions, feedback, or a pilot in mind — let's talk.",
    size=16, color=BODY, align=PP_ALIGN.CENTER)
chip(s, Inches(7.4), Inches(7.5), Inches(5.2), Inches(0.5), "TEAM METAMASHERS  ·  HACKINDIA 2026", TEAL)
txt(s, Inches(0.9), Inches(10.72), Inches(18.2), Inches(0.3),
    "ClaimChain  ·  Smarter Claims. Stronger Trust.  ·  Built at HackIndia 2026",
    size=11, color=MUTED, align=PP_ALIGN.CENTER)

prs.save("ClaimChain_Modern_Animated.pptx")
print("Saved ClaimChain_Modern_Animated.pptx with", len(prs.slides._sldIdLst), "slides")
