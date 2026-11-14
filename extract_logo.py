"""Extract the ClaimChain logo mark from slide 9 image (searching slide 8 as fallback)."""
from PIL import Image

for src in [r"pptx_extract\ppt\media\image9.png", r"pptx_extract\ppt\media\image8.png"]:
    img = Image.open(src).convert("RGBA")
    w, h = img.size
    # logo sits near the top-center; scan generous box
    box = img.crop((int(w*0.30), int(h*0.22), int(w*0.72), int(h*0.48)))
    # build alpha from "distance from white" heuristic
    px = box.load()
    W, H = box.size
    alpha = Image.new("L", box.size, 0)
    al = alpha.load()
    for y in range(H):
        for x in range(W):
            r, g, b, _ = px[x, y]
            # white-ish -> transparent
            if r > 235 and g > 235 and b > 235:
                a = 0
            else:
                # partial transparency near edges: use saturation
                mx, mn = max(r, g, b), min(r, g, b)
                sat = 0 if mx == 0 else (mx - mn) / mx
                lum = (r + g + b) / 3
                if sat < 0.08 and lum > 210:
                    a = 0  # light gray -> transparent
                elif sat < 0.12:
                    a = int(255 * (1 - (lum - 160) / 75)) if lum > 160 else 255
                else:
                    a = 255
            al[x, y] = max(0, min(255, a))
    # tight-crop to content
    bbox = alpha.getbbox()
    if bbox:
        pad = 6
        bbox = (max(0, bbox[0]-pad), max(0, bbox[1]-pad), min(W, bbox[2]+pad), min(H, bbox[3]+pad))
        out = box.crop(bbox)
        out.putalpha(alpha.crop(bbox))
        out.save("logo_mark.png")
        print(f"logo saved from {src}: {out.size}")
        break
