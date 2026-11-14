"""Create light/teal logo variants for the dark modern theme."""
from PIL import Image

src = Image.open("logo_mark.png").convert("RGBA")

# All-white version (for dark backgrounds)
white = Image.new("RGBA", src.size)
wpx, spx = white.load(), src.load()
for y in range(src.height):
    for x in range(src.width):
        r, g, b, a = spx[x, y]
        if a:
            wpx[x, y] = (255, 255, 255, a)
white.save("logo_light.png")

# Teal leaf-only version (mark without wordmark)
leaf = src.crop((0, 0, int(src.width * 0.20), src.height))
bbox = leaf.getbbox()
leaf = leaf.crop(bbox)
teal = Image.new("RGBA", leaf.size)
tpx, lpx = teal.load(), leaf.load()
for y in range(leaf.height):
    for x in range(leaf.width):
        r, g, b, a = lpx[x, y]
        if a:
            # blend toward teal-cyan (#2DD4BF) keeping some luminance variation
            tpx[x, y] = (45, 212, 191, a)
teal.save("leaf_teal.png")

print("saved logo_light.png", white.size, "| leaf_teal.png", teal.size)
