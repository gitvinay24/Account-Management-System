import zipfile, os, json, re
from pptx import Presentation
from pptx.util import Emu

PPTX = r"C:\Users\vinay\Downloads\Untitled design.pptx"
OUT_DIR = "pptx_extract"
os.makedirs(OUT_DIR, exist_ok=True)
os.makedirs(os.path.join(OUT_DIR, "media"), exist_ok=True)

# Save media files
with zipfile.ZipFile(PPTX) as z:
    media = [n for n in z.namelist() if n.startswith("ppt/media/")]
    print(f"MEDIA FILES ({len(media)}):")
    for n in media:
        info = z.getinfo(n)
        print(f"  {n}  {info.file_size/1024:.0f} KB")
        z.extract(n, OUT_DIR)

prs = Presentation(PPTX)
print(f"\nSLIDE SIZE: {prs.slide_width} x {prs.slide_height} EMU = {prs.slide_width/914400:.2f} x {prs.slide_height/914400:.2f} inches")

def describe_shape(sh, depth=0):
    ind = "  " * depth
    lines = []
    st = sh.shape_type
    name = sh.name
    try:
        pos = f"({Emu(sh.left).inches:.2f},{Emu(sh.top).inches:.2f}) {Emu(sh.width).inches:.2f}x{Emu(sh.height).inches:.2f}in"
    except Exception:
        pos = "(no pos)"
    txt = ""
    if sh.has_text_frame:
        txt = " | ".join(r.text for p in sh.text_frame.paragraphs for r in p.runs if r.text.strip())
    lines.append(f"{ind}[{st}] '{name}' {pos} TEXT: {txt[:200]}")
    if sh.shape_type == 6:  # group
        for sub in sh.shapes:
            lines.extend(describe_shape(sub, depth+1))
    return lines

print(f"\nSLIDES: {len(prs.slides)}")
for i, slide in enumerate(prs.slides, 1):
    print(f"\n===== SLIDE {i} (layout: {slide.slide_layout.name}) =====")
    for sh in slide.shapes:
        for line in describe_shape(sh):
            print(line)
    # notes
    if slide.has_notes_slide and slide.notes_slide.notes_text_frame.text.strip():
        print(f"  NOTES: {slide.notes_slide.notes_text_frame.text[:300]}")
