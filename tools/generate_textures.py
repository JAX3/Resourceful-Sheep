#!/usr/bin/env python3
"""
Generates every texture used by Resourceful Sheep as original pixel art.

Run from the project root:  python3 tools/generate_textures.py
Requires Pillow. Output goes to src/main/resources/assets/resourcefulsheep/textures/.

All art is procedurally drawn here (no vanilla textures are read or copied); only
the UV *layout* of the sheep fur model (64x32) is matched so the overlay wraps
the vanilla model correctly.
"""
import os
import random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "resourcefulsheep", "textures")


def out(path):
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    return full


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def mix(c1, c2, t):
    return tuple(int(round(c1[i] * (1 - t) + c2[i] * t)) for i in range(4))


def shade(c, f):
    return (max(0, min(255, int(c[0] * f))), max(0, min(255, int(c[1] * f))), max(0, min(255, int(c[2] * f))), c[3])


CLEAR = (0, 0, 0, 0)

# ---------------------------------------------------------------------------
# Wool item: a fluffy wound skein. Shape map: '.' empty, 'o' outline, 1-4 shade
# levels (1 = darkest .. 4 = brightest), 'h' highlight. Hand-designed 16x16.
# ---------------------------------------------------------------------------
WOOL_SHAPE = [
    "................",
    "......oooo......",
    "....oo4344oo....",
    "...o44433444o...",
    "..o3443344443o..",
    "..o4332334433o..",
    ".o344433343343o.",
    ".o433444332443o.",
    ".o332343444332o.",
    ".o244333433432o.",
    "..o3443323342o..",
    "..o2334443322o..",
    "...o22332222o...",
    "....oo2112oo....",
    "......oooo......",
    "................",
]

# Strand lines across the skein (used for "wound yarn" look and resource strands).
STRANDS = [(4, 4), (5, 5), (6, 6), (7, 7), (8, 8), (9, 9), (10, 10), (11, 11),
           (6, 3), (7, 4), (8, 5), (9, 6), (10, 7), (11, 8), (12, 9)]


def wool_palette(base):
    """4 shades from dark to light plus outline."""
    return {
        "o": shade(base, 0.45),
        "1": shade(base, 0.62),
        "2": shade(base, 0.78),
        "3": shade(base, 0.92),
        "4": mix(base, (255, 255, 255, 255), 0.18),
    }


def draw_wool(base):
    img = Image.new("RGBA", (16, 16), CLEAR)
    pal = wool_palette(base)
    for y, row in enumerate(WOOL_SHAPE):
        for x, ch in enumerate(row):
            if ch in pal:
                c = pal[ch]
                # Wound-yarn look: diagonal winding lines across the skein.
                if ch not in "o" and (x + y) % 4 == 0:
                    c = shade(c, 0.84)
                elif ch not in "o" and (x - y) % 7 == 0:
                    c = mix(c, (255, 255, 255, 255), 0.12)
                img.putpixel((x, y), c)
    return img


def inside(x, y):
    return WOOL_SHAPE[y][x] not in ".o"


def put(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16 and inside(x, y):
        img.putpixel((x, y), c)


# Resource definitions for the 12 built-in wool items: base colour + decorator.
def deco_metal_strands(light, dark):
    def f(img, rnd):
        for (x, y) in STRANDS:
            put(img, x, y, light if (x + y) % 3 else dark)
        for (x, y) in [(5, 9), (6, 10), (9, 4), (10, 5)]:
            put(img, x, y, light)
    return f


def deco_sparkle(spark, glow):
    def f(img, rnd):
        for (x, y) in [(5, 5), (10, 4), (11, 9), (6, 10), (8, 7)]:
            put(img, x, y, spark)
            for dx, dy in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
                if rnd.random() < 0.6:
                    put(img, x + dx, y + dy, glow)
    return f


def deco_crystals(c1, c2, edge):
    def f(img, rnd):
        for (x, y) in [(4, 6), (9, 4), (11, 8), (6, 10), (8, 8)]:
            put(img, x, y, c1)
            put(img, x + 1, y, c2)
            put(img, x, y + 1, c2)
            put(img, x + 1, y + 1, edge)
    return f


def deco_flecks(colors, count, seed):
    def f(img, rnd):
        r = random.Random(seed)
        n = 0
        while n < count:
            x, y = r.randint(2, 13), r.randint(2, 12)
            if inside(x, y):
                put(img, x, y, colors[n % len(colors)])
                n += 1
    return f


def deco_wood(ring, dark):
    def f(img, rnd):
        for (x, y) in STRANDS[:8]:
            put(img, x, y, dark)
        for (x, y) in [(4, 8), (5, 8), (9, 10), (10, 10), (7, 5), (8, 5)]:
            put(img, x, y, ring)
    return f


def deco_stone(dark, light):
    def f(img, rnd):
        r = random.Random(42)
        for _ in range(14):
            x, y = r.randint(2, 13), r.randint(2, 12)
            put(img, x, y, dark if r.random() < 0.6 else light)
    return f


BUILTIN = {
    # name: (wool colour, accent colour, decorator)
    "iron": ("#C8C8C8", "#F4F4F4", deco_metal_strands(hexc("#FFFFFF"), hexc("#8A8A8A"))),
    "gold": ("#F2C230", "#FFF6A8", deco_sparkle(hexc("#FFFFE0"), hexc("#FFE45C"))),
    "copper": ("#D27A4E", "#5FD3B5", deco_metal_strands(hexc("#F4A27A"), hexc("#3FA58A"))),
    "diamond": ("#7FE6E0", "#E8FFFF", deco_crystals(hexc("#D9FFFF"), hexc("#3DCBD0"), hexc("#1F8C93"))),
    "emerald": ("#3FC46A", "#B9FFC9", deco_crystals(hexc("#B9FFC9"), hexc("#17A346"), hexc("#0A6B2C"))),
    "redstone": ("#9E1D1D", "#FF3B2B", deco_flecks([hexc("#FF4A3A"), hexc("#FFB0A0"), hexc("#FF2010")], 12, 7)),
    "lapis": ("#2F55B5", "#9DB8FF", deco_flecks([hexc("#C9D7FF"), hexc("#F2D45C"), hexc("#6F8FE8")], 10, 11)),
    "coal": ("#3A3A3E", "#8C8C96", deco_flecks([hexc("#1A1A1C"), hexc("#7A7A84"), hexc("#AAAAB4")], 13, 5)),
    "netherite": ("#4A3E44", "#9B6FB0", deco_metal_strands(hexc("#B58BD0"), hexc("#2A2228"))),
    "stone": ("#8E8E8E", "#B5B5B5", deco_stone(hexc("#5E5E5E"), hexc("#BDBDBD"))),
    "wood": ("#A8834F", "#6B4F2A", deco_wood(hexc("#D2B07A"), hexc("#6B4F2A"))),
    "quartz": ("#EDE6DC", "#FFFFFF", deco_crystals(hexc("#FFFFFF"), hexc("#D8CCBE"), hexc("#B5A898"))),
}


def generate_wool_items():
    for name, (base, accent, deco) in BUILTIN.items():
        img = draw_wool(hexc(base))
        deco(img, random.Random(sum(map(ord, name))))
        img.save(out(f"item/{name}_wool.png"))
    # Fallback (tinted) layers for data-pack resources: greyscale wool + accent flecks.
    grey = draw_wool(hexc("#E6E6E6"))
    for (x, y) in STRANDS[::2]:
        put(grey, x, y, hexc("#FFFFFF"))
    grey.save(out("item/resource_wool_base.png"))
    acc = Image.new("RGBA", (16, 16), CLEAR)
    for (x, y) in [(5, 5), (10, 4), (11, 9), (6, 10), (8, 7), (4, 8), (12, 6)]:
        acc.putpixel((x, y), (255, 255, 255, 255))
    for (x, y) in [(6, 5), (10, 5), (7, 10), (9, 7)]:
        acc.putpixel((x, y), (200, 200, 200, 255))
    acc.save(out("item/resource_wool_overlay.png"))


# ---------------------------------------------------------------------------
# Sheep fur overlay (64x32, vanilla SheepFurModel UV layout) - greyscale so the
# renderer can tint it, plus a sparse accent ("glint strands") layer.
# ---------------------------------------------------------------------------
def box_faces(u, v, w, h, d):
    """Returns dict face -> (x0, y0, x1, y1) for a model cube at texOffs(u, v)."""
    return {
        "top": (u + d, v, u + d + w, v + d),
        "bottom": (u + d + w, v, u + d + 2 * w, v + d),
        "east": (u, v + d, u + d, v + d + h),
        "north": (u + d, v + d, u + d + w, v + d + h),
        "west": (u + d + w, v + d, u + 2 * d + w, v + d + h),
        "south": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }


def fur_noise(x, y, rnd):
    # Curly fleece: base 200 with clumps; deterministic per pixel.
    clump = ((x * 7 + y * 13) % 11) / 11.0
    v = 196 + int(40 * clump) + rnd.randint(-14, 14)
    if (x + 2 * y) % 5 == 0:
        v -= 22
    if (2 * x + y) % 7 == 0:
        v += 14
    return max(120, min(255, v))


def generate_sheep_fur():
    rnd = random.Random(1337)
    wool = Image.new("RGBA", (64, 32), CLEAR)
    accent = Image.new("RGBA", (64, 32), CLEAR)

    def fill(rect, mask=None):
        x0, y0, x1, y1 = rect
        for y in range(y0, y1):
            for x in range(x0, x1):
                if mask and not mask(x - x0, y - y0, x1 - x0, y1 - y0):
                    continue
                g = fur_noise(x, y, rnd)
                wool.putpixel((x, y), (g, g, g, 255))
                if rnd.random() < 0.07:
                    a = rnd.choice([255, 230, 200])
                    accent.putpixel((x, y), (255, 255, 255, a))

    head = box_faces(0, 0, 6, 6, 6)
    for face in ("top", "east", "west", "south"):
        fill(head[face])
    # Face: only a fringe of wool across the top two rows so the sheep's face shows.
    fill(head["north"], lambda x, y, w, h: y < 2 or (y < 3 and x in (0, w - 1)))
    body = box_faces(28, 8, 8, 16, 6)
    for face in body:
        fill(body[face])
    legs = box_faces(0, 16, 4, 6, 4)
    fill(legs["top"])
    for face in ("east", "north", "west", "south"):
        fill(legs[face], lambda x, y, w, h: y < 4)

    # A few long glinting strands along the body sides for the "metallic strands" look.
    for (x0, y0, x1, y1) in (body["north"], body["south"], body["east"], body["west"]):
        for sx in range(x0 + 1, x1, 3):
            for y in range(y0 + 2, y1 - 2, 4):
                accent.putpixel((sx, y), (255, 255, 255, 255))
                if y + 1 < y1:
                    accent.putpixel((sx, y + 1), (235, 235, 235, 200))

    wool.save(out("entity/sheep/infused_wool.png"))
    accent.save(out("entity/sheep/infused_wool_accent.png"))


# ---------------------------------------------------------------------------
# Mechanical Separator block textures (16x16).
# ---------------------------------------------------------------------------
STEEL = {
    "edge": hexc("#3B3F45"), "dark": hexc("#5A6068"), "mid": hexc("#7D848D"),
    "light": hexc("#A3AAB3"), "hi": hexc("#C9CED4"), "rivet": hexc("#D9DDE1"),
    "copper": hexc("#C46A3A"), "copper_d": hexc("#8A4526"), "copper_l": hexc("#E8955F"),
    "glass": hexc("#1E2A33"), "glass_l": hexc("#2F4554"),
    "red": hexc("#B3261E"), "red_l": hexc("#FF5A4A"),
}


def frame_base():
    img = Image.new("RGBA", (16, 16), STEEL["mid"])
    rnd = random.Random(9)
    for y in range(16):
        for x in range(16):
            n = rnd.randint(-6, 6)
            c = STEEL["mid"]
            img.putpixel((x, y), (c[0] + n, c[1] + n, c[2] + n, 255))
    # Bevelled frame
    for i in range(16):
        img.putpixel((i, 0), STEEL["hi"])
        img.putpixel((0, i), STEEL["hi"])
        img.putpixel((i, 15), STEEL["edge"])
        img.putpixel((15, i), STEEL["edge"])
        img.putpixel((i, 1), STEEL["light"])
        img.putpixel((1, i), STEEL["light"])
        img.putpixel((i, 14), STEEL["dark"])
        img.putpixel((14, i), STEEL["dark"])
    for (x, y) in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        img.putpixel((x, y), STEEL["rivet"])
        img.putpixel((x + 1, y + 1) if x < 13 and y < 13 else (x, y), STEEL["rivet"])
    return img


GEAR_PIXELS = {
    # 8x8 gear sprite frames (rotation by 22.5deg approximated with 2 alternating poses)
    0: ["..#..#..", ".######.", "##.##.##", ".##..##.", ".##..##.", "##.##.##", ".######.", "..#..#.."],
    1: [".#....#.", "########", ".#.##.#.", ".##..##.", ".##..##.", ".#.##.#.", "########", ".#....#."],
}


def draw_gear(img, ox, oy, frame, color, dark):
    pose = GEAR_PIXELS[frame % 2]
    for y, row in enumerate(pose):
        for x, ch in enumerate(row):
            if ch == "#":
                img.putpixel((ox + x, oy + y), color if (x + y + frame) % 3 else dark)


def generate_block():
    # Side: panels with copper piping
    side = frame_base()
    for x in range(3, 13):
        side.putpixel((x, 7), STEEL["copper"])
        side.putpixel((x, 8), STEEL["copper_d"])
        side.putpixel((x, 6), STEEL["copper_l"])
    for y in range(3, 13):
        side.putpixel((7, y), STEEL["dark"])
        side.putpixel((8, y), STEEL["light"])
    for (x, y) in [(4, 4), (11, 4), (4, 11), (11, 11)]:
        side.putpixel((x, y), STEEL["dark"])
    side.save(out("block/mechanical_separator_side.png"))

    # Top: intake grate (where wool goes in)
    top = frame_base()
    for y in range(3, 13):
        for x in range(3, 13):
            top.putpixel((x, y), STEEL["glass"] if (y % 2 == 0) else STEEL["dark"])
    for x in range(3, 13):
        top.putpixel((x, 3), STEEL["edge"])
        top.putpixel((x, 12), STEEL["light"])
    top.save(out("block/mechanical_separator_top.png"))

    # Bottom: plain heavy plate with output vent
    bottom = frame_base()
    for x in range(5, 11):
        for y in range(6, 10):
            bottom.putpixel((x, y), STEEL["edge"] if y % 2 else STEEL["dark"])
    bottom.save(out("block/mechanical_separator_bottom.png"))

    # Back: FE input port (red/copper socket with lightning mark)
    back = frame_base()
    for y in range(4, 12):
        for x in range(4, 12):
            back.putpixel((x, y), STEEL["dark"])
    for y in range(5, 11):
        for x in range(5, 11):
            back.putpixel((x, y), STEEL["copper_d"])
    bolt = [(8, 5), (7, 6), (7, 7), (6, 8), (7, 8), (8, 8), (8, 9), (7, 10)]
    for (x, y) in bolt:
        back.putpixel((x, y), hexc("#FFE14A"))
    back.save(out("block/mechanical_separator_back.png"))

    # Front: processing chamber window with gears. Inactive = still, active = 4-frame animation strip.
    def front(frame, active):
        img = frame_base()
        # Window
        for y in range(3, 13):
            for x in range(3, 13):
                img.putpixel((x, y), STEEL["glass"])
        for x in range(3, 13):
            img.putpixel((x, 3), STEEL["edge"])
            img.putpixel((x, 12), STEEL["hi"])
        for y in range(3, 13):
            img.putpixel((3, y), STEEL["edge"])
            img.putpixel((12, y), STEEL["hi"])
        gear_c = STEEL["copper_l"] if active else STEEL["copper"]
        draw_gear(img, 4, 4, frame, gear_c, STEEL["copper_d"])
        # Glass reflection
        img.putpixel((11, 4), STEEL["glass_l"])
        img.putpixel((10, 5), STEEL["glass_l"])
        # Fluff inside when active: wool tufts being pulled apart
        if active:
            for (x, y) in [((frame * 3) % 6 + 5, 10), ((frame * 5 + 2) % 6 + 5, 11)]:
                img.putpixel((x, y), hexc("#F2F2F2"))
        # Status light
        img.putpixel((13, 7), STEEL["red_l"] if active else STEEL["red"])
        img.putpixel((13, 8), STEEL["red_l"] if active else STEEL["red"])
        return img

    front(0, False).save(out("block/mechanical_separator_front.png"))
    strip = Image.new("RGBA", (16, 64), CLEAR)
    for f in range(4):
        strip.paste(front(f, True), (0, f * 16))
    strip.save(out("block/mechanical_separator_front_active.png"))
    with open(out("block/mechanical_separator_front_active.png.mcmeta"), "w") as fh:
        fh.write('{\n  "animation": {\n    "frametime": 3,\n    "interpolate": false\n  }\n}\n')


# ---------------------------------------------------------------------------
# GUI (256x256): background 176x193 + sprite strip at y >= 194.
# ---------------------------------------------------------------------------
GUI = {
    "bg": hexc("#C6C6C6"), "white": hexc("#FFFFFF"), "shadow": hexc("#555555"),
    "black": hexc("#000000"), "slot": hexc("#8B8B8B"), "slot_d": hexc("#373737"),
    "panel": hexc("#B4B9BF"), "panel_d": hexc("#6F757C"), "panel_l": hexc("#DDE1E5"),
}


def rect(img, x0, y0, x1, y1, c):
    for y in range(y0, y1):
        for x in range(x0, x1):
            img.putpixel((x, y), c)


def bevel_panel(img, x, y, w, h):
    """Standard vanilla-style window: black outline, white top-left, grey bottom-right."""
    rect(img, x, y, x + w, y + h, GUI["bg"])
    for i in range(x + 2, x + w - 2):
        img.putpixel((i, y), GUI["black"])
        img.putpixel((i, y + h - 1), GUI["black"])
        img.putpixel((i, y + 1), GUI["white"])
        img.putpixel((i, y + 2), GUI["white"])
        img.putpixel((i, y + h - 2), GUI["shadow"])
        img.putpixel((i, y + h - 3), GUI["shadow"])
    for j in range(y + 2, y + h - 2):
        img.putpixel((x, j), GUI["black"])
        img.putpixel((x + w - 1, j), GUI["black"])
        img.putpixel((x + 1, j), GUI["white"])
        img.putpixel((x + 2, j), GUI["white"])
        img.putpixel((x + w - 2, j), GUI["shadow"])
        img.putpixel((x + w - 3, j), GUI["shadow"])
    # rounded corners
    for (cx, cy) in [(x + 1, y + 1), (x + w - 2, y + h - 2)]:
        img.putpixel((cx, cy), GUI["black"])
    img.putpixel((x + w - 2, y + 1), GUI["black"])
    img.putpixel((x + 1, y + h - 2), GUI["black"])
    img.putpixel((x + 3, y + 3), GUI["white"])
    img.putpixel((x + w - 4, y + h - 4), GUI["shadow"])
    img.putpixel((x + w - 3, y + 2), GUI["bg"])
    img.putpixel((x + 2, y + h - 3), GUI["bg"])


def slot(img, x, y, size=18):
    """Slot frame whose inner 16x16 starts at (x+1, y+1)."""
    rect(img, x, y, x + size, y + size, GUI["slot"])
    for i in range(size - 1):
        img.putpixel((x + i, y), GUI["slot_d"])
        img.putpixel((x, y + i), GUI["slot_d"])
    for i in range(1, size):
        img.putpixel((x + i, y + size - 1), GUI["white"])
        img.putpixel((x + size - 1, y + i), GUI["white"])


def big_slot(img, x, y):
    """26x26 output slot (frame around an 18x18) in a darker industrial style."""
    rect(img, x, y, x + 26, y + 26, GUI["panel_d"])
    slot(img, x + 4, y + 4)


def inset(img, x, y, w, h, fill):
    rect(img, x, y, x + w, y + h, fill)
    for i in range(w):
        img.putpixel((x + i, y), GUI["slot_d"])
        img.putpixel((x + i, y + h - 1), GUI["white"])
    for j in range(h):
        img.putpixel((x, y + j), GUI["slot_d"])
        img.putpixel((x + w - 1, y + j), GUI["white"])


def screw(img, x, y):
    img.putpixel((x, y), GUI["panel_d"])
    img.putpixel((x + 1, y), GUI["white"])
    img.putpixel((x, y + 1), GUI["shadow"])
    img.putpixel((x + 1, y + 1), GUI["panel_d"])


def arrow_shape(fill, outline):
    """32x16 right-pointing arrow."""
    a = Image.new("RGBA", (32, 16), CLEAR)
    for y in range(5, 11):
        for x in range(1, 22):
            a.putpixel((x, y), fill)
    for i in range(8):
        for y in range(8 - i - 1, 8 + i + 1):
            if 0 <= y < 16:
                a.putpixel((22 + (7 - i), y), fill)
    # outline
    px = a.load()
    o = Image.new("RGBA", (32, 16), CLEAR)
    for y in range(16):
        for x in range(32):
            if px[x, y][3] == 0:
                for dx, dy in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 32 and 0 <= ny < 16 and px[nx, ny][3]:
                        o.putpixel((x, y), outline)
                        break
    o.alpha_composite(a)
    return o


def generate_gui():
    img = Image.new("RGBA", (256, 256), CLEAR)
    W, H = 176, 193
    bevel_panel(img, 0, 0, W, H)

    # Machine "faceplate" area behind the processing section
    rect(img, 7, 14, 169, 98, GUI["panel"])
    for i in range(7, 169):
        img.putpixel((i, 14), GUI["panel_d"])
        img.putpixel((i, 97), GUI["panel_l"])
    for j in range(14, 98):
        img.putpixel((7, j), GUI["panel_d"])
        img.putpixel((168, j), GUI["panel_l"])
    for (sx, sy) in [(9, 16), (165, 16), (9, 94), (165, 94)]:
        screw(img, sx, sy)
    # Hazard stripe along the top of the faceplate
    for x in range(12, 30):
        c = hexc("#E3B341") if (x // 3) % 2 == 0 else hexc("#3A3A3A")
        img.putpixel((x, 18), c)
        img.putpixel((x, 19), c)
    for x in range(146, 164):
        c = hexc("#E3B341") if (x // 3) % 2 == 0 else hexc("#3A3A3A")
        img.putpixel((x, 18), c)
        img.putpixel((x, 19), c)

    # Energy bar frame: inner fill area at (35,19) 106x10
    inset(img, 33, 17, 110, 14, hexc("#2A1414"))
    for x in range(35, 141):
        for y in range(19, 29):
            # empty cells with tick marks every 10 px
            img.putpixel((x, y), hexc("#3A1C1C") if (x - 35) % 10 else hexc("#4A2626"))
    # Lightning icon left of the bar
    for (x, y) in [(28, 18), (27, 19), (26, 20), (25, 21), (26, 21), (27, 21), (28, 21), (27, 22), (26, 23), (25, 24), (26, 24)]:
        img.putpixel((x, y), hexc("#FFD43B"))
        img.putpixel((x + 1, y + 1), hexc("#8A6A00")) if img.getpixel((x + 1, y + 1)) == GUI["panel"] else None

    # Input slot (item at 44,56) and output slots
    slot(img, 43, 55)
    big_slot(img, 103, 42)        # primary: item at 108,47 -> frame 107,46 inside the 26px housing
    slot(img, 107, 68)            # secondary: item at 108,69
    slot(img, 129, 46)            # bonus: item at 130,47
    # little labels: wool icon guide in input slot background (faint)
    for (x, y) in [(48, 60), (49, 59), (50, 59), (51, 60), (52, 61), (51, 62), (49, 62), (48, 61), (53, 62), (47, 63)]:
        img.putpixel((x, y), hexc("#9C9C9C"))
    # empty progress arrow at (66,56)
    img.alpha_composite(arrow_shape(hexc("#8B8B8B"), hexc("#555555")), (66, 56))
    # gear housing at (76,74) 12x12
    inset(img, 75, 73, 14, 14, hexc("#7A7F86"))
    # status lamp housing at (9,87)
    inset(img, 8, 86, 10, 10, hexc("#2A2A2A"))

    # Player inventory slots (8, 111) 3x9 and hotbar (8, 169)
    for row in range(3):
        for col in range(9):
            slot(img, 7 + col * 18, 110 + row * 18)
    for col in range(9):
        slot(img, 7 + col * 18, 168)

    # ---- sprites ----
    # Energy fill (0,194) 106x10: red->orange->yellow gradient with highlight
    for x in range(106):
        t = x / 105.0
        c = mix(hexc("#C0261B"), hexc("#FF9A1F"), min(1, t * 1.6)) if t < 0.62 else mix(hexc("#FF9A1F"), hexc("#FFE45C"), (t - 0.62) / 0.38)
        for y in range(10):
            f = 1.15 if y in (1, 2) else (0.75 if y >= 8 else 1.0)
            cc = shade(c, f)
            if (x % 10) == 0:
                cc = shade(cc, 0.85)
            img.putpixel((x, 194 + y), cc)
    # Filled arrow (106,194)
    img.alpha_composite(arrow_shape(hexc("#FFFFFF"), hexc("#555555")), (106, 194))
    filled = arrow_shape(hexc("#4FC3F7"), hexc("#1F5F80"))
    img.alpha_composite(filled, (106, 194))
    # Gear frames (138,194) 4x 12x12
    for f in range(4):
        g = Image.new("RGBA", (12, 12), CLEAR)
        draw_gear(g, 2, 2, f, hexc("#E8955F"), hexc("#8A4526"))
        if f % 2:
            g = g.transpose(Image.FLIP_LEFT_RIGHT)
        img.alpha_composite(g, (138 + f * 12, 194))
    # Status lamps (186,194) 6x 8x8 in MachineStatus order
    lamp_colors = ["#9AA0A6", "#55D66B", "#E8B23A", "#E35D5D", "#E35D5D", "#B04040"]
    for i, col in enumerate(lamp_colors):
        lx = 186 + i * 8
        base = hexc(col)
        for y in range(8):
            for x in range(8):
                d = ((x - 3.5) ** 2 + (y - 3.5) ** 2) ** 0.5
                if d <= 3.6:
                    c = shade(base, 1.0 - d * 0.08)
                    if d < 1.4:
                        c = mix(c, hexc("#FFFFFF"), 0.45)
                    img.putpixel((lx + x, 194 + y), c)
                elif d <= 4.2:
                    img.putpixel((lx + x, 194 + y), shade(base, 0.45))

    # Redstone mode icons (176,0) 4x 16x16: IGNORED, HIGH, LOW, DISABLED
    def torch(ox, oy, lit):
        for y in range(7, 14):
            img.putpixel((ox + 7, oy + y), hexc("#6B4F2A"))
            img.putpixel((ox + 8, oy + y), hexc("#8A6A3A"))
        head = hexc("#FF3B2B") if lit else hexc("#5A1A14")
        for (x, y) in [(7, 4), (8, 4), (6, 5), (7, 5), (8, 5), (9, 5), (7, 6), (8, 6)]:
            img.putpixel((ox + x, oy + y), head)
        if lit:
            for (x, y) in [(7, 3), (5, 5), (10, 5), (7, 2)]:
                img.putpixel((ox + x, oy + y), hexc("#FFB0A0"))

    # IGNORED: gunpowder-like dust pile (grey) = "always on"
    for (x, y) in [(5, 10), (6, 9), (7, 9), (8, 9), (9, 9), (10, 10), (4, 11), (11, 11), (6, 10), (7, 10), (8, 10), (9, 10)]:
        img.putpixel((176 + x, y), hexc("#9A9A9A"))
    for (x, y) in [(6, 8), (8, 8), (10, 9)]:
        img.putpixel((176 + x, y), hexc("#C8C8C8"))
    torch(192, 0, True)     # HIGH
    torch(208, 0, False)    # LOW
    # DISABLED: torch with a red bar
    torch(224, 0, False)
    for i in range(3, 13):
        img.putpixel((224 + i, i), hexc("#E02020"))
        img.putpixel((224 + i + 1, i), hexc("#E02020")) if i < 12 else None
    # Button backgrounds (176,64) normal, (192,64) hovered
    for bx, light in [(176, False), (192, True)]:
        face = hexc("#A8A8A8") if not light else hexc("#BEC8D8")
        rect(img, bx, 64, bx + 16, 80, face)
        for i in range(16):
            img.putpixel((bx + i, 64), GUI["white"])
            img.putpixel((bx, 64 + i), GUI["white"])
            img.putpixel((bx + i, 79), GUI["slot_d"])
            img.putpixel((bx + 15, 64 + i), GUI["slot_d"])
    img.save(out("gui/mechanical_separator.png"))


# ---------------------------------------------------------------------------
# JEI sheet (256x256)
# ---------------------------------------------------------------------------
def sheep_sprite(w, h, wool, face):
    """Side-view sheep of size w x h (32x24 or 16x16 scale)."""
    s = Image.new("RGBA", (32, 24), CLEAR)
    body = [
        "................................",
        "................................",
        "........oooooooooooooo..........",
        "......oo44444434444444oo........",
        ".....o4444344444443444444o......",
        "....o444444443444444444444o.....",
        "....o4434444444444434444444oooo.",
        "....o44444444444444444444444ffo.",
        "...o4444444344444444444444444ffo",
        "...o4434444444443444444444444eff",
        "...o44444444444444444444444ffffo",
        "...o444444444444444444444444ffo.",
        "....o4444443444444444444444fffo.",
        "....o444444444444434444444oooo..",
        ".....o44444444444444444444o.....",
        "......oo4444444444444444oo......",
        "........oooooooooooooooo........",
        ".........ll..ll....ll..ll.......",
        ".........ll..ll....ll..ll.......",
        ".........ll..ll....ll..ll.......",
        ".........hh..hh....hh..hh.......",
        "................................",
        "................................",
        "................................",
    ]
    pal = {"o": shade(wool, 0.55), "4": wool, "3": shade(wool, 0.82), "f": face, "e": hexc("#1A1A1A"),
           "l": hexc("#D9C3A6"), "h": hexc("#5A4632")}
    for y, row in enumerate(body):
        for x, ch in enumerate(row):
            if ch in pal:
                s.putpixel((x, y), pal[ch])
    return s


def generate_jei():
    img = Image.new("RGBA", (256, 256), CLEAR)
    # (0,16) 32x24 sheep illustration with a golden infused fleece and sparkles
    sheep = sheep_sprite(32, 24, hexc("#F2C230"), hexc("#E7D2B5"))
    for (x, y) in [(8, 5), (14, 9), (20, 6), (11, 12), (23, 11)]:
        sheep.putpixel((x, y), hexc("#FFFBE0"))
    img.alpha_composite(sheep, (0, 16))
    # (0,0) 16x16 icon: small sheep head + sparkle
    icon = Image.new("RGBA", (16, 16), CLEAR)
    small = sheep_sprite(32, 24, hexc("#C8C8C8"), hexc("#E7D2B5")).resize((16, 12), Image.NEAREST)
    icon.alpha_composite(small, (0, 3))
    for (x, y) in [(3, 1), (2, 2), (3, 2), (4, 2), (3, 3), (12, 1)]:
        icon.putpixel((x, y), hexc("#FFE45C"))
    img.alpha_composite(icon, (0, 0))
    # (32,16) energy empty 8x40, (40,16) energy full 8x40
    for y in range(40):
        for x in range(8):
            edge = x in (0, 7) or y in (0, 39)
            img.putpixel((32 + x, 16 + y), hexc("#373737") if edge else hexc("#3A1C1C"))
            t = y / 39.0
            c = mix(hexc("#FFE45C"), hexc("#C0261B"), t)
            img.putpixel((40 + x, 16 + y), hexc("#373737") if edge else shade(c, 1.1 if x in (2, 3) else 1.0))
    img.save(out("gui/jei.png"))


def main():
    generate_wool_items()
    generate_sheep_fur()
    generate_block()
    generate_gui()
    generate_jei()
    print("Textures written to", os.path.abspath(ROOT))


if __name__ == "__main__":
    main()
