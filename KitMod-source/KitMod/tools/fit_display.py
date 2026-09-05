#!/usr/bin/env python3
# Fits item display transforms so models fit the inventory slot.
# Blockbench saves "gui" with rotation only; default scale 1 makes tall
# models overflow the 16-unit slot and get cropped. Author rotation kept.
# Minecraft: p' = translation/16 + Rx*Ry*Rz * (scale * p), p = vertex/16 - 0.5
import glob, json, math, os, sys

FLAT = {"gui": 0.92, "fixed": 0.88, "ground": 0.42, "head": 0.9}
HAND = {"thirdperson_righthand": 0.95, "thirdperson_lefthand": 0.95,
        "firstperson_righthand": 0.8, "firstperson_lefthand": 0.8}
DEF = {"gui": [30, 225, 0], "fixed": [0, 180, 0], "ground": [0, 0, 0],
       "head": [0, 180, 0], "thirdperson_righthand": [0, 60, 0],
       "thirdperson_lefthand": [0, 60, 0], "firstperson_righthand": [0, 45, 0],
       "firstperson_lefthand": [0, 45, 0]}


def rot(ax, deg):
    a = math.radians(deg); c = math.cos(a); s = math.sin(a)
    if ax == "x": return [[1, 0, 0], [0, c, -s], [0, s, c]]
    if ax == "y": return [[c, 0, s], [0, 1, 0], [-s, 0, c]]
    return [[c, -s, 0], [s, c, 0], [0, 0, 1]]


def mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def ap(m, v):
    return [sum(m[i][k] * v[k] for k in range(3)) for i in range(3)]


def points(model):
    out = []
    for el in model.get("elements", []):
        f = el["from"]; t = el["to"]
        cs = [[x, y, z] for x in (f[0], t[0]) for y in (f[1], t[1]) for z in (f[2], t[2])]
        er = el.get("rotation")
        if er:
            o = er["origin"]; R = rot(er["axis"], er["angle"])
            cs = [[ap(R, [c[i] - o[i] for i in range(3)])[k] + o[k] for k in range(3)] for c in cs]
        out += cs
    return [[c[i] / 16.0 - 0.5 for i in range(3)] for c in out]


def fit(path):
    model = json.load(open(path, encoding="utf-8"))
    pts = points(model)
    if not pts:
        return None
    disp = model.setdefault("display", {})
    for view in list(FLAT) + list(HAND):
        cur = disp.get(view, {})
        r = cur.get("rotation") or DEF[view]
        R = mul(mul(rot("x", r[0]), rot("y", r[1])), rot("z", r[2]))
        q = [ap(R, p) for p in pts]
        if view in FLAT:
            xs = [p[0] for p in q]; ys = [p[1] for p in q]
            span = max(max(xs) - min(xs), max(ys) - min(ys), 1e-6)
            sc = FLAT[view] / span
            tr = [-(max(xs) + min(xs)) / 2 * sc * 16, -(max(ys) + min(ys)) / 2 * sc * 16, 0.0]
        else:
            span = max(max(p[i] for p in q) - min(p[i] for p in q) for i in range(3))
            sc = min(1.0, HAND[view] / max(span, 1e-6))
            tr = list(cur.get("translation", [0, 0, 0]))
        disp[view] = {"rotation": [round(x, 2) for x in r],
                      "translation": [round(x, 3) for x in tr],
                      "scale": [round(sc, 4)] * 3}
    json.dump(model, open(path, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    return disp["gui"]["scale"][0]


folder = sys.argv[1] if len(sys.argv) > 1 else "."
files = sorted(glob.glob(os.path.join(folder, "*.json")))
for p in files:
    s = fit(p)
    if s:
        print("%-22s gui scale %s" % (os.path.basename(p)[:-5], s))
print("fitted models:", len(files))
