#!/usr/bin/env python3
"""Writes all JSON resources (models, blockstates, loot, tags, recipes, worldgen,
damage types, advancements, lang, sounds) plus the empty GameTest structure.
Run from the project root: python3 tools/generate_data.py
"""
import gzip
import io
import json
import os
import struct

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src/main/resources")
M = "sebastianhilihorror"


def w(rel, obj):
    path = os.path.join(RES, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


BLOCKS = ["zinc_ore", "deepslate_zinc_ore", "raw_zinc_block", "zinc_block"]
ITEMS = ["raw_zinc", "zinc_ingot", "penny"]

# ------------------------------------------------------------ models / blockstates
ORE_BASES = {"zinc_ore": "minecraft:block/stone", "deepslate_zinc_ore": "minecraft:block/deepslate"}


def ore_model(base, overlay):
    """Vanilla stone/deepslate underneath + our transparent zinc overlay on top (cutout).
    Matches the world exactly, including resource packs."""
    def faces(tex):
        return {d: {"texture": tex, "cullface": d} for d in ["down", "up", "north", "south", "west", "east"]}
    return {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": {"particle": base, "base": base, "overlay": overlay},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces("#base")},
                         {"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces("#overlay")}]}


for b in BLOCKS:
    w(f"assets/{M}/blockstates/{b}.json", {"variants": {"": {"model": f"{M}:block/{b}"}}})
    if b in ORE_BASES:
        w(f"assets/{M}/models/block/{b}.json", ore_model(ORE_BASES[b], f"{M}:block/{b}_overlay"))
    else:
        w(f"assets/{M}/models/block/{b}.json", {"parent": "minecraft:block/cube_all",
                                                 "textures": {"all": f"{M}:block/{b}"}})
    w(f"assets/{M}/models/item/{b}.json", {"parent": f"{M}:block/{b}"})
for i in ITEMS:
    w(f"assets/{M}/models/item/{i}.json", {"parent": "minecraft:item/generated",
                                            "textures": {"layer0": f"{M}:item/{i}"}})
w(f"assets/{M}/models/item/sebastian_hili_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})

# ------------------------------------------------------------ loot tables
def self_drop(b):
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": f"{M}:{b}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": f"{M}:blocks/{b}"}


def ore_drop(b):
    silk = {"condition": "minecraft:match_tool", "predicate": {"enchantments": [
        {"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{
        "type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": f"{M}:{b}", "conditions": [silk]},
            {"type": "minecraft:item", "name": f"{M}:raw_zinc", "functions": [
                {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1.0, "max": 3.0},
                 "add": False},
                {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                 "formula": "minecraft:ore_drops"},
                {"function": "minecraft:explosion_decay"}]}]}]}],
            "random_sequence": f"{M}:blocks/{b}"}


for b in BLOCKS:
    w(f"data/{M}/loot_tables/blocks/{b}.json", ore_drop(b) if "ore" in b else self_drop(b))

# ------------------------------------------------------------ tags
def tag(rel, values):
    w(rel, {"replace": False, "values": values})


tag("data/minecraft/tags/blocks/mineable/pickaxe.json", [f"{M}:{b}" for b in BLOCKS])
tag("data/minecraft/tags/blocks/needs_stone_tool.json", [f"{M}:{b}" for b in BLOCKS])
tag(f"data/{M}/tags/blocks/zinc_ores.json", [f"{M}:zinc_ore", f"{M}:deepslate_zinc_ore"])
tag(f"data/{M}/tags/items/zinc_ores.json", [f"{M}:zinc_ore", f"{M}:deepslate_zinc_ore"])
tag("data/forge/tags/blocks/ores/zinc.json", [f"#{M}:zinc_ores"])
tag("data/forge/tags/items/ores/zinc.json", [f"#{M}:zinc_ores"])
tag("data/forge/tags/blocks/ores.json", ["#forge:ores/zinc"])
tag("data/forge/tags/items/ores.json", ["#forge:ores/zinc"])
tag("data/forge/tags/blocks/ores_in_ground/stone.json", [f"{M}:zinc_ore"])
tag("data/forge/tags/blocks/ores_in_ground/deepslate.json", [f"{M}:deepslate_zinc_ore"])
tag("data/forge/tags/items/ores_in_ground/stone.json", [f"{M}:zinc_ore"])
tag("data/forge/tags/items/ores_in_ground/deepslate.json", [f"{M}:deepslate_zinc_ore"])
tag("data/forge/tags/blocks/ore_rates/singular.json", [f"#{M}:zinc_ores"])
tag("data/forge/tags/blocks/storage_blocks/zinc.json", [f"{M}:zinc_block"])
tag("data/forge/tags/blocks/storage_blocks/raw_zinc.json", [f"{M}:raw_zinc_block"])
tag("data/forge/tags/blocks/storage_blocks.json", ["#forge:storage_blocks/zinc", "#forge:storage_blocks/raw_zinc"])
tag("data/forge/tags/items/storage_blocks/zinc.json", [f"{M}:zinc_block"])
tag("data/forge/tags/items/storage_blocks/raw_zinc.json", [f"{M}:raw_zinc_block"])
tag("data/forge/tags/items/storage_blocks.json", ["#forge:storage_blocks/zinc", "#forge:storage_blocks/raw_zinc"])
tag("data/forge/tags/items/ingots/zinc.json", [f"{M}:zinc_ingot"])
tag("data/forge/tags/items/ingots.json", ["#forge:ingots/zinc"])
tag("data/forge/tags/items/raw_materials/zinc.json", [f"{M}:raw_zinc"])
tag("data/forge/tags/items/raw_materials.json", ["#forge:raw_materials/zinc"])
tag("data/forge/tags/items/coins.json", [f"{M}:penny"])
tag(f"data/{M}/tags/items/pennies.json", [f"{M}:penny"])
tag("data/minecraft/tags/items/beacon_payment_items.json", [f"{M}:zinc_ingot"])

# ------------------------------------------------------------ recipes
def item(i):
    return {"item": i if ":" in i else f"{M}:{i}"}


def smelt(name, kind, ingredient, result, xp, time):
    w(f"data/{M}/recipes/{name}.json", {"type": f"minecraft:{kind}", "category": "misc",
                                         "ingredient": ingredient, "result": f"{M}:{result}",
                                         "experience": xp, "cookingtime": time})


smelt("zinc_ingot_from_smelting_raw_zinc", "smelting", item("raw_zinc"), "zinc_ingot", 0.7, 200)
smelt("zinc_ingot_from_blasting_raw_zinc", "blasting", item("raw_zinc"), "zinc_ingot", 0.7, 100)
smelt("zinc_ingot_from_smelting_zinc_ore", "smelting", {"tag": f"{M}:zinc_ores"}, "zinc_ingot", 0.7, 200)
smelt("zinc_ingot_from_blasting_zinc_ore", "blasting", {"tag": f"{M}:zinc_ores"}, "zinc_ingot", 0.7, 100)


def nine_block(block, unit):
    w(f"data/{M}/recipes/{block}.json", {"type": "minecraft:crafting_shaped", "category": "building",
                                          "pattern": ["###", "###", "###"], "key": {"#": item(unit)},
                                          "result": {"item": f"{M}:{block}"}})
    w(f"data/{M}/recipes/{unit}_from_{block}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                                      "ingredients": [item(block)],
                                                      "result": {"item": f"{M}:{unit}", "count": 9}})


nine_block("zinc_block", "zinc_ingot")
nine_block("raw_zinc_block", "raw_zinc")
w(f"data/{M}/recipes/penny.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                    "ingredients": [item("zinc_ingot"), item("zinc_ingot"), item("zinc_ingot"),
                                                    {"item": "minecraft:copper_ingot"}],
                                    "result": {"item": f"{M}:penny", "count": 4}})

# recipe-book unlock advancements
def unlock(recipe, trigger_item):
    w(f"data/{M}/advancements/recipes/misc/{recipe}.json", {
        "parent": "minecraft:recipes/root",
        "criteria": {
            "has_item": {"trigger": "minecraft:inventory_changed",
                         "conditions": {"items": [{"items": [trigger_item]}]}},
            "has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipe": f"{M}:{recipe}"}}},
        "requirements": [["has_item", "has_the_recipe"]],
        "rewards": {"recipes": [f"{M}:{recipe}"]}})


for r, t in [("zinc_ingot_from_smelting_raw_zinc", "raw_zinc"), ("zinc_ingot_from_blasting_raw_zinc", "raw_zinc"),
             ("zinc_ingot_from_smelting_zinc_ore", "zinc_ore"), ("zinc_ingot_from_blasting_zinc_ore", "zinc_ore"),
             ("zinc_block", "zinc_ingot"), ("zinc_ingot_from_zinc_block", "zinc_block"),
             ("raw_zinc_block", "raw_zinc"), ("raw_zinc_from_raw_zinc_block", "raw_zinc_block"),
             ("penny", "zinc_ingot")]:
    unlock(r, f"{M}:{t}")

# ------------------------------------------------------------ worldgen
w(f"data/{M}/worldgen/configured_feature/ore_zinc.json", {"type": "minecraft:ore", "config": {
    "discard_chance_on_air_exposure": 0.0, "size": 10, "targets": [
        {"state": {"Name": f"{M}:zinc_ore"},
         "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}},
        {"state": {"Name": f"{M}:deepslate_zinc_ore"},
         "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"}}]}})
w(f"data/{M}/worldgen/placed_feature/ore_zinc.json", {"feature": f"{M}:ore_zinc", "placement": [
    {"type": "minecraft:count", "count": 15},
    {"type": "minecraft:in_square"},
    {"type": "minecraft:height_range", "height": {"type": "minecraft:trapezoid",
                                                  "min_inclusive": {"absolute": -16},
                                                  "max_inclusive": {"absolute": 112}}},
    {"type": "minecraft:biome"}]})
w(f"data/{M}/forge/biome_modifier/add_zinc_ore.json", {"type": "forge:add_features", "biomes": "#minecraft:is_overworld",
                                                         "features": f"{M}:ore_zinc", "step": "underground_ores"})

# ------------------------------------------------------------ damage types (+ tags so they always kill)
DAMAGE = {"penny_charge": "penny_charge", "penny_rage": "penny_rage", "penny_lie": "penny_lie"}
for d in DAMAGE:
    w(f"data/{M}/damage_type/{d}.json", {"message_id": f"{M}.{d}", "scaling": "never", "exhaustion": 0.0})
for t in ["bypasses_armor", "bypasses_effects", "bypasses_resistance", "bypasses_enchantments",
          "bypasses_invulnerability", "bypasses_shield", "always_most_significant_fall"][:6]:
    tag(f"data/minecraft/tags/damage_type/{t}.json", [f"{M}:{d}" for d in DAMAGE])
# boss attacks: ordinary damage (armour and protection help), each with its own death message
BOSS_DAMAGE = {
    "boss_dash": "%1$s was trampled by Sebastian Hili in the red",
    "boss_penny": "%1$s was pelted to death with pennies",
    "boss_eruption": "%1$s was swallowed by the red",
    "boss_slash": "%1$s turned around too late",
    "boss_wave": "%1$s forgot to jump",
    "boss_lightning": "%1$s was struck by red lightning",
    "boss_stalk": "%1$s was found in the dark",
}
for d in BOSS_DAMAGE:
    w(f"data/{M}/damage_type/{d}.json", {"message_id": f"{M}.{d}", "scaling": "never", "exhaustion": 0.1})
tag("data/minecraft/tags/damage_type/is_lightning.json", [f"{M}:penny_lie", f"{M}:boss_lightning"])
tag("data/minecraft/tags/damage_type/is_fire.json", [f"{M}:boss_eruption"])
tag("data/minecraft/tags/damage_type/is_projectile.json", [f"{M}:boss_penny"])

# ------------------------------------------------------------ the red pocket dimension
w(f"data/{M}/dimension_type/the_red.json", {
    "ultrawarm": True, "natural": False, "piglin_safe": False, "respawn_anchor_works": False,
    "bed_works": False, "has_raids": False, "has_skylight": False, "has_ceiling": False,
    "coordinate_scale": 1.0, "ambient_light": 0.35, "fixed_time": 18000, "logical_height": 256,
    "effects": "minecraft:the_nether", "infiniburn": "#minecraft:infiniburn_nether",
    "min_y": 0, "height": 256, "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0})
w(f"data/{M}/dimension/the_red.json", {
    "type": f"{M}:the_red",
    "generator": {"type": "minecraft:flat", "settings": {
        "biome": f"{M}:the_red", "layers": [], "lakes": False, "features": False, "structure_overrides": []}}})
w(f"data/{M}/worldgen/biome/the_red.json", {
    "has_precipitation": False, "temperature": 2.0, "downfall": 0.0,
    "effects": {"fog_color": 0x6E0A0A, "sky_color": 0x400000, "water_color": 0x8A1010, "water_fog_color": 0x3A0000,
                "particle": {"options": {"type": "minecraft:crimson_spore"}, "probability": 0.03},
                "music": {"sound": f"{M}:silence", "min_delay": 24000, "max_delay": 24000, "replace_current_music": True}},
    "spawners": {}, "spawn_costs": {}, "carvers": {}, "features": []})

# ------------------------------------------------------------ advancements
def adv(name, parent, icon, title, desc, criteria, frame="task", hidden=False, announce=True, background=None):
    display = {"icon": {"item": icon}, "title": {"translate": f"advancements.{M}.{name}.title"},
               "description": {"translate": f"advancements.{M}.{name}.description"},
               "frame": frame, "show_toast": True, "announce_to_chat": announce, "hidden": hidden}
    if background:
        display["background"] = background
    obj = {"display": display, "criteria": criteria, "requirements": [[k] for k in criteria][:1] if len(criteria) == 1 else [list(criteria)]}
    if parent:
        obj["parent"] = f"{M}:{parent}"
    w(f"data/{M}/advancements/{name}.json", obj)
    LANG[f"advancements.{M}.{name}.title"] = title
    LANG[f"advancements.{M}.{name}.description"] = desc


def has(i):
    return {"has": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": [i]}]}}}


CODE = {"code": {"trigger": "minecraft:impossible"}}
LANG = {}
adv("root", None, f"{M}:zinc_ore", "Sebastian Hili Horror", "Dig up some zinc. Someone is watching.",
    has(f"{M}:raw_zinc"), background=f"{M}:textures/block/zinc_block.png", announce=False)
adv("mint_condition", "root", f"{M}:penny", "Mint Condition", "Craft a Penny. He would like it.",
    has(f"{M}:penny"))
adv("spare_change", "mint_condition", f"{M}:penny", "Spare Change", "Give Sebastian Hili a penny", CODE)
adv("honest_answer", "mint_condition", "minecraft:writable_book", "Honest Answer",
    "Truthfully tell Sebastian Hili you have no pennies", CODE)
adv("pants_on_fire", "honest_answer", "minecraft:lightning_rod", "Pants on Fire",
    "Lie to Sebastian Hili about a penny", CODE, frame="challenge", hidden=True)
adv("over_a_penny", "mint_condition", "minecraft:iron_sword", "Over a Penny",
    "Get run through by Sebastian Hili", CODE, hidden=True)
adv("money_to_burn", "mint_condition", "minecraft:lava_bucket", "Money to Burn",
    "Burn a penny where Sebastian Hili can see it", CODE, frame="goal", hidden=True)
adv("debt_collector", "root", "minecraft:netherite_sword", "Debt Collector", "Kill Sebastian Hili. For now.",
    {"killed": {"trigger": "minecraft:player_killed_entity",
                "conditions": {"entity": [{"condition": "minecraft:entity_properties", "entity": "this",
                                           "predicate": {"type": f"{M}:sebastian_hili"}}]}}},
    frame="challenge")
adv("evidence", "mint_condition", "minecraft:filled_map", "Exhibit A",
    "Refuse Sebastian Hili five times in a row and receive something", CODE, hidden=True)
adv("the_world_looks_red", "evidence", "minecraft:crimson_nylium", "The World Looks Red",
    "Survive Sebastian Hili until the song ends", CODE, frame="challenge", hidden=True)
adv("good_morning", "root", "minecraft:red_bed", "Good Morning", "Wake up to find someone watching you sleep",
    CODE, hidden=True)

# ------------------------------------------------------------ sounds
SOUNDS = {
    "heartbeat": "Heartbeat",
    "whisper": "Something whispers",
    "rage_scream": "Sebastian Hili screams",
    "penny_clink": "Penny clinks",
    "sting": "Something is behind you",
}
sounds = {k: {"subtitle": f"subtitles.{M}.{k}", "sounds": [f"{M}:{k}"]} for k in SOUNDS}
sounds["boss_music"] = {"subtitle": f"subtitles.{M}.boss_music",
                        "sounds": [{"name": f"{M}:music/the_world_looks_red", "stream": True}]}
sounds["silence"] = {"sounds": []}
w(f"assets/{M}/sounds.json", sounds)

# ------------------------------------------------------------ lang
LANG.update({
    f"block.{M}.zinc_ore": "Zinc Ore",
    f"block.{M}.deepslate_zinc_ore": "Deepslate Zinc Ore",
    f"block.{M}.raw_zinc_block": "Block of Raw Zinc",
    f"block.{M}.zinc_block": "Block of Zinc",
    f"item.{M}.raw_zinc": "Raw Zinc",
    f"item.{M}.zinc_ingot": "Zinc Ingot",
    f"item.{M}.penny": "Penny",
    f"item.{M}.penny.desc": "One cent. 97.5% zinc. He wants it.",
    f"item.{M}.sebastian_hili_spawn_egg": "Sebastian Hili Spawn Egg",
    f"entity.{M}.sebastian_hili": "Sebastian Hili",
    f"itemGroup.{M}": "Sebastian Hili Horror",
    f"death.attack.{M}.penny_charge": "%1$s was run through by Sebastian Hili over a penny",
    f"death.attack.{M}.penny_charge.player": "%1$s was run through by %2$s over a penny",
    f"death.attack.{M}.penny_charge.item": "%1$s was run through by %2$s over a penny",
    f"death.attack.{M}.penny_rage": "%1$s was run through by Sebastian Hili because someone burned a penny",
    f"death.attack.{M}.penny_rage.player": "%1$s was run through by %2$s because someone burned a penny",
    f"death.attack.{M}.penny_rage.item": "%1$s was run through by %2$s because someone burned a penny",
    f"death.attack.{M}.penny_lie": "%1$s lied to Sebastian Hili about a penny",
    f"death.attack.{M}.penny_lie.player": "%1$s lied to %2$s about a penny and was struck down",
    f"death.attack.{M}.penny_lie.item": "%1$s lied to %2$s about a penny and was struck down",
})
for k, v in BOSS_DAMAGE.items():
    for suffix in ("", ".player", ".item"):
        LANG[f"death.attack.{M}.{k}{suffix}"] = v
LANG[f"subtitles.{M}.boss_music"] = "The World Looks Red plays"
LANG[f"item.{M}.burned_penny_map"] = "Exhibit A"
LANG[f"item.{M}.burned_penny_map.lore"] = "A burned penny. Show it to him. If you dare."
for k, v in SOUNDS.items():
    LANG[f"subtitles.{M}.{k}"] = v
w(f"assets/{M}/lang/en_us.json", dict(sorted(LANG.items())))


# ------------------------------------------------------------ GameTest structure (stone floor, air above)
def nbt_payload(tag_type, value):
    if tag_type == 3:
        return struct.pack(">i", value)
    if tag_type == 8:
        b = value.encode("utf-8")
        return struct.pack(">H", len(b)) + b
    if tag_type == 9:
        elem_type, items = value
        out = struct.pack(">bi", elem_type if items else 0, len(items))
        for it in items:
            out += nbt_payload(elem_type, it)
        return out
    if tag_type == 10:
        out = b""
        for name, (t, v) in value.items():
            nb = name.encode()
            out += struct.pack(">bH", t, len(nb)) + nb + nbt_payload(t, v)
        return out + b"\x00"
    raise ValueError(tag_type)


def write_structure(rel, sx, sy, sz):
    blocks = [(10, {"pos": (9, (3, [x, 0, z])), "state": (3, 0)}) for x in range(sx) for z in range(sz)]
    root = {
        "DataVersion": (3, 3465),
        "size": (9, (3, [sx, sy, sz])),
        "palette": (9, (10, [{"Name": (8, "minecraft:stone")}])),
        "blocks": (9, (10, [b[1] for b in blocks])),
        "entities": (9, (10, [])),
    }
    raw = struct.pack(">bH", 10, 0) + nbt_payload(10, root)
    path = os.path.join(RES, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    buf = io.BytesIO()
    with gzip.GzipFile(fileobj=buf, mode="wb", mtime=0) as g:
        g.write(raw)
    open(path, "wb").write(buf.getvalue())


write_structure(f"data/{M}/structures/empty.nbt", 15, 8, 15)
print("data written")
