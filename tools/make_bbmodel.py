#!/usr/bin/env python3
"""Builds docs/blockbench/sebastian_hili.bbmodel: the classic player model with Sebastian's skin
embedded, ready to open in Blockbench (File > Open Model)."""
import base64
import json
import os
import uuid

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SKIN = os.path.join(ROOT, "src/main/resources/assets/sebastianhilihorror/textures/entity/sebastian_hili.png")
OUT = os.path.join(ROOT, "docs/blockbench/sebastian_hili.bbmodel")


def uid():
    return str(uuid.uuid4())


def box_faces(u, v, w, h, d):
    """Blockbench's own box-UV layout for a w*h*d cube whose texture starts at (u, v)."""
    def r(x1, y1, x2, y2):
        return [x1 + u, y1 + v, x2 + u, y2 + v]
    return {
        "north": {"uv": r(d, d, d + w, d + h), "texture": 0},
        "east": {"uv": r(0, d, d, d + h), "texture": 0},
        "south": {"uv": r(2 * d + 2 * w, d, 2 * d + w, d + h), "texture": 0},
        "west": {"uv": r(d + w, d, 2 * d + w, d + h), "texture": 0},
        "up": {"uv": r(d + w, d, d, 0), "texture": 0},
        "down": {"uv": r(d + 2 * w, 0, d + w, d), "texture": 0},
    }


def cube(name, origin, size, uv, pivot, inflate=0.0):
    x, y, z = origin
    w, h, d = size
    c = {
        "name": name, "box_uv": True, "rescale": False, "locked": False, "render_order": "default",
        "allow_mirror_modeling": True, "from": [x, y, z], "to": [x + w, y + h, z + d],
        "autouv": 0, "color": 0, "origin": pivot, "uv_offset": uv,
        "faces": box_faces(uv[0], uv[1], w, h, d), "type": "cube", "uuid": uid(),
    }
    if inflate:
        c["inflate"] = inflate
    return c


# (group, pivot, [(cube name, origin, size, uv, inflate)])  standard 64x64 classic-arm player
PARTS = [
    ("head", [0, 24, 0], [("head", [-4, 24, -4], [8, 8, 8], [0, 0], 0), ("hair (hat layer)", [-4, 24, -4], [8, 8, 8], [32, 0], 0.5)]),
    ("body", [0, 24, 0], [("body", [-4, 12, -2], [8, 12, 4], [16, 16], 0), ("blazer (jacket layer)", [-4, 12, -2], [8, 12, 4], [16, 32], 0.25)]),
    # Blockbench space: the model faces north (-Z), so his right side is +X (east)
    ("right_arm", [5, 22, 0], [("right_arm", [4, 12, -2], [4, 12, 4], [40, 16], 0), ("right_sleeve", [4, 12, -2], [4, 12, 4], [40, 32], 0.25)]),
    ("left_arm", [-5, 22, 0], [("left_arm", [-8, 12, -2], [4, 12, 4], [32, 48], 0), ("left_sleeve", [-8, 12, -2], [4, 12, 4], [48, 48], 0.25)]),
    ("right_leg", [1.9, 12, 0], [("right_leg", [-0.1, 0, -2], [4, 12, 4], [0, 16], 0), ("right_pants", [-0.1, 0, -2], [4, 12, 4], [0, 32], 0.25)]),
    ("left_leg", [-1.9, 12, 0], [("left_leg", [-3.9, 0, -2], [4, 12, 4], [16, 48], 0), ("left_pants", [-3.9, 0, -2], [4, 12, 4], [0, 48], 0.25)]),
]

elements, outliner = [], []
for group, pivot, cubes in PARTS:
    kids = []
    for name, origin, size, uv, inflate in cubes:
        c = cube(name, origin, size, uv, pivot, inflate)
        elements.append(c)
        kids.append(c["uuid"])
    outliner.append({"name": group, "origin": pivot, "color": 0, "uuid": uid(), "export": True,
                     "mirror_uv": False, "isOpen": True, "locked": False, "visibility": True,
                     "autouv": 0, "children": kids})

with open(SKIN, "rb") as f:
    png = base64.b64encode(f.read()).decode()

model = {
    "meta": {"format_version": "4.10", "model_format": "free", "box_uv": True},
    "name": "sebastian_hili",
    "model_identifier": "",
    "visible_box": [1, 2, 0],
    "variable_placeholders": "",
    "variable_placeholder_buttons": [],
    "timeline_setups": [],
    "unhandled_root_fields": {},
    "resolution": {"width": 64, "height": 64},
    "elements": elements,
    "outliner": outliner,
    "textures": [{
        "path": "", "name": "sebastian_hili.png", "folder": "", "namespace": "", "id": "0",
        "width": 64, "height": 64, "uv_width": 64, "uv_height": 64, "particle": False,
        "use_as_default": False, "layers_enabled": False, "sync_to_project": "", "render_mode": "default",
        "render_sides": "auto", "frame_time": 1, "frame_order_type": "loop", "frame_order": "",
        "frame_interpolate": False, "visible": True, "internal": True, "saved": False, "uuid": uid(),
        "relative_path": "", "source": "data:image/png;base64," + png}],
}
os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w") as f:
    json.dump(model, f, indent=1)
print("wrote", OUT, len(elements), "cubes")
