# Fixes Blockbench models for vanilla MC: UVs -> 0..16, display fitted to slot.
import glob, json, math, os, sys

FLAT = {"gui": 0.95, "fixed": 0.9, "ground": 0.62, "head": 1.0}
HAND = {"thirdperson_righthand": 1.15, "thirdperson_lefthand": 1.15,
    "firstperson_righthand": 1.0, "firstperson_lefthand": 1.0}
# per-item tweaks: rot = degrees added, scale = multiplier, move = 1/16 block
F1, F3 = "firstperson_righthand", "thirdperson_righthand"
F1L, F3L = "firstperson_lefthand", "thirdperson_lefthand"
FLIP = {"rot": [0, 0, 180]}
PS1 = {"scale": 0.7, "move": [0, 3.5, 0]}
TWEAK = {
  "vampire_scythe": {F3: FLIP, F3L: FLIP, F1: FLIP, F1L: FLIP},
  "legendary_sword": {F3: {"scale": 1.55}, F3L: {"scale": 1.55}},
  "pizza_sword": {F1: PS1, F1L: PS1, F3: {"scale": 0.85}, F3L: {"scale": 0.85}},
}

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

def faces(model):
  for el in model.get("elements", []):
    for fc in (el.get("faces") or {}).values():
      if fc.get("uv"):
        yield fc

def fix_uv(model):
  ts = model.pop("texture_size", None)
  if not ts:
    return None
  biggest = max((max(abs(x) for x in fc["uv"]) for fc in faces(model)), default=0.0)
  if biggest <= 16.001:
    return None
  su, sv = 16.0 / ts[0], 16.0 / ts[1]
  if su == 1.0 and sv == 1.0:
    return None
  for fc in faces(model):
    u = fc["uv"]
    fc["uv"] = [round(u[0] * su, 4), round(u[1] * sv, 4),
          round(u[2] * su, 4), round(u[3] * sv, 4)]
  return ts

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

def fit_display(model, name=""):
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
      sc = HAND[view] / max(span, 1e-6)
      tr = list(cur.get("translation", [0, 0, 0]))
    tw = TWEAK.get(name, {}).get(view)
    if tw:
      r = [r[i] + tw.get("rot", [0, 0, 0])[i] for i in range(3)]
      sc *= tw.get("scale", 1.0)
      tr = [tr[i] + tw.get("move", [0, 0, 0])[i] for i in range(3)]
    disp[view] = {"rotation": [round(x, 2) for x in r],
           "translation": [round(x, 3) for x in tr],
           "scale": [round(sc, 4)] * 3}
  return disp["gui"]["scale"][0]

folder = sys.argv[1] if len(sys.argv) > 1 else "."
files = sorted(glob.glob(os.path.join(folder, "*.json")))
for path in files:
  model = json.load(open(path, encoding="utf-8"))
  rescaled = fix_uv(model)
  scale = fit_display(model, os.path.basename(path)[:-5])
  json.dump(model, open(path, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
  print("%-22s %-9s gui %s" % (os.path.basename(path)[:-5],
                "uv fixed" if rescaled else "uv ok", scale))
print("models fixed:", len(files))
