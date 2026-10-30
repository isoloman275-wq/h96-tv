#!/usr/bin/env python3
"""Generate launcher banner + icon PNGs for NZ1 Free TV (no design deps beyond Pillow)."""
from PIL import Image, ImageDraw, ImageFont
import os

RES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "app", "src", "main", "res")
ACCENT = (53, 224, 255)
DARK = (11, 23, 38)
CARD = (23, 51, 79)
WHITE = (240, 246, 252)


def load_font(size):
    for p in ("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
              "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf"):
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def rounded_bg(w, h, r, top=CARD, bot=DARK, stroke=None):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, w - 1, h - 1], radius=r, fill=bot)
    d.rounded_rectangle([0, 0, w - 1, h // 2], radius=r, fill=top)  # cheap two-tone
    if stroke:
        d.rounded_rectangle([1, 1, w - 2, h - 2], radius=r, outline=stroke, width=3)
    return img


# ---- banner 320x180 (TV store banner) --------------------------------------
b = rounded_bg(320, 180, 18, stroke=ACCENT)
d = ImageDraw.Draw(b)
f_big, f_sm = load_font(44), load_font(20)
d.text((160, 74), "NZ1", font=f_big, anchor="mm", fill=ACCENT)
d.text((160, 118), "FREE TV", font=f_sm, anchor="mm", fill=WHITE)
out_b = os.path.join(RES, "drawable")
os.makedirs(out_b, exist_ok=True)
b.save(os.path.join(out_b, "banner.png"))

# ---- launcher icons ---------------------------------------------------------
for dpi, size in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144)):
    ic = rounded_bg(size, size, int(size * 0.22), stroke=ACCENT if size >= 96 else None)
    d = ImageDraw.Draw(ic)
    f = load_font(int(size * 0.34))
    d.text((size / 2, size * 0.42), "NZ1", font=f, anchor="mm", fill=ACCENT)
    d.text((size / 2, size * 0.68), "TV", font=load_font(int(size * 0.22)),
           anchor="mm", fill=WHITE)
    out = os.path.join(RES, f"mipmap-{dpi}")
    os.makedirs(out, exist_ok=True)
    ic.save(os.path.join(out, "ic_launcher.png"))

print("banner + icons written")
