#!/usr/bin/env python3
"""
Writes the JSON assets and data for Resourceful Sheep (models, blockstates, item model
definitions, language file, built-in resource definitions, recipes, loot, tags).

Run from the project root:  python3 tools/generate_data.py
The generated files are committed; this script exists so they stay consistent and are easy to extend.
"""
import json
import os

MOD = "resourcefulsheep"
RES = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", MOD)
DATA = os.path.join(RES, "data")


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


# name -> definition. Keep in sync with tools/generate_textures.py BUILTIN colours.
RESOURCES = {
    "iron": dict(display="Iron", feed="minecraft:iron_block", out="minecraft:raw_iron", cat="ore", uses=9,
                 wool="#C8C8C8", accent="#F4F4F4", energy=20000, ticks=100,
                 bonus={"item": {"id": "minecraft:iron_nugget", "count": 3}, "chance": 0.1}),
    "gold": dict(display="Gold", feed="minecraft:gold_block", out="minecraft:raw_gold", cat="ore", uses=9,
                 wool="#F2C230", accent="#FFF6A8", energy=20000, ticks=100,
                 bonus={"item": {"id": "minecraft:gold_nugget", "count": 3}, "chance": 0.1}),
    "copper": dict(display="Copper", feed="minecraft:copper_block", out="minecraft:raw_copper", cat="ore", uses=9,
                   wool="#D27A4E", accent="#5FD3B5", energy=16000, ticks=100),
    "diamond": dict(display="Diamond", feed="minecraft:diamond", out="minecraft:diamond", cat="gem", uses=1,
                    wool="#7FE6E0", accent="#E8FFFF", energy=40000, ticks=160),
    "emerald": dict(display="Emerald", feed="minecraft:emerald", out="minecraft:emerald", cat="gem", uses=1,
                    wool="#3FC46A", accent="#B9FFC9", energy=40000, ticks=160),
    "redstone": dict(display="Redstone", feed="minecraft:redstone_block", out="minecraft:redstone", cat="dust", uses=9,
                     wool="#9E1D1D", accent="#FF3B2B", energy=12000, ticks=80),
    "lapis": dict(display="Lapis", feed="minecraft:lapis_block", out="minecraft:lapis_lazuli", cat="gem", uses=9,
                  wool="#2F55B5", accent="#9DB8FF", energy=12000, ticks=80),
    "coal": dict(display="Coal", feed="minecraft:coal_block", out="minecraft:coal", cat="material", uses=9,
                 wool="#3A3A3E", accent="#8C8C96", energy=8000, ticks=80),
    "netherite": dict(display="Netherite", feed="minecraft:netherite_scrap", out="minecraft:netherite_scrap", cat="material", uses=1,
                      wool="#4A3E44", accent="#9B6FB0", energy=80000, ticks=200),
    "stone": dict(display="Stone", feed="minecraft:cobblestone", out="minecraft:cobblestone", cat="material", uses=1,
                  wool="#8E8E8E", accent="#B5B5B5", energy=4000, ticks=40),
    "wood": dict(display="Wood", feed="minecraft:oak_log", out="minecraft:oak_log", cat="organic", uses=1,
                 wool="#A8834F", accent="#6B4F2A", energy=4000, ticks=40),
    "quartz": dict(display="Quartz", feed="minecraft:quartz", out="minecraft:quartz", cat="gem", uses=1,
                   wool="#EDE6DC", accent="#FFFFFF", energy=16000, ticks=100),
}


def resource_definitions():
    for name, r in RESOURCES.items():
        sep = {
            "primary_output": r["out"],
            "primary_count": 1,
            "secondary_output": "minecraft:string",
            "secondary_count": 2,
            "energy_cost": r["energy"],
            "processing_ticks": r["ticks"],
        }
        if "bonus" in r:
            sep["bonus_output"] = r["bonus"]
        write(os.path.join(DATA, MOD, MOD, "sheep_resources", f"{name}.json"), {
            "feeding_item": r["feed"],
            "feeding_amount": 1,
            "wool_color": r["wool"],
            "accent_color": r["accent"],
            "category": r["cat"],
            "wool_drop_count": 1,
            "infusion_uses": r["uses"],
            "separator": sep,
        })


def item_models():
    cases = []
    for name in RESOURCES:
        write(os.path.join(ASSETS, "models", "item", f"{name}_wool.json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}_wool"}})
        cases.append({"when": f"{MOD}:{name}", "model": {"type": "minecraft:model", "model": f"{MOD}:item/{name}_wool"}})
    # Fallback for data-pack resources: greyscale wool tinted with wool_color + accent flecks tinted with accent_color.
    write(os.path.join(ASSETS, "models", "item", "resource_wool.json"), {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"{MOD}:item/resource_wool_base", "layer1": f"{MOD}:item/resource_wool_overlay"},
    })
    write(os.path.join(ASSETS, "items", "resource_wool.json"), {
        "model": {
            "type": "minecraft:select",
            "property": "minecraft:component",
            "component": f"{MOD}:sheep_resource",
            "cases": cases,
            "fallback": {
                "type": "minecraft:model",
                "model": f"{MOD}:item/resource_wool",
                "tints": [
                    {"type": f"{MOD}:resource_wool", "channel": "wool"},
                    {"type": f"{MOD}:resource_wool", "channel": "accent"},
                ],
            },
        }
    })
    write(os.path.join(ASSETS, "items", "mechanical_separator.json"),
          {"model": {"type": "minecraft:model", "model": f"{MOD}:block/mechanical_separator"}})


def block_models():
    def model(front):
        return {
            "parent": "minecraft:block/cube",
            "textures": {
                "particle": f"{MOD}:block/mechanical_separator_side",
                "north": f"{MOD}:block/{front}",
                "south": f"{MOD}:block/mechanical_separator_back",
                "east": f"{MOD}:block/mechanical_separator_side",
                "west": f"{MOD}:block/mechanical_separator_side",
                "up": f"{MOD}:block/mechanical_separator_top",
                "down": f"{MOD}:block/mechanical_separator_bottom",
            },
        }
    write(os.path.join(ASSETS, "models", "block", "mechanical_separator.json"), model("mechanical_separator_front"))
    write(os.path.join(ASSETS, "models", "block", "mechanical_separator_active.json"), model("mechanical_separator_front_active"))
    variants = {}
    for facing, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for active in ("false", "true"):
            v = {"model": f"{MOD}:block/mechanical_separator" + ("_active" if active == "true" else "")}
            if rot:
                v["y"] = rot
            variants[f"active={active},facing={facing}"] = v
    write(os.path.join(ASSETS, "blockstates", "mechanical_separator.json"), {"variants": variants})


def lang():
    en = {
        "itemGroup.resourcefulsheep": "Resourceful Sheep",
        "block.resourcefulsheep.mechanical_separator": "Mechanical Separator",
        "item.resourcefulsheep.resource_wool": "Resource Wool",
        "item.resourcefulsheep.resource_wool.named": "%s Wool",
        "item.resourcefulsheep.resource_wool.unknown": "Unknown Resource Wool",
        "tooltip.resourcefulsheep.wool.contains": "Infused with: %s",
        "tooltip.resourcefulsheep.wool.no_resource": "No resource - craft or shear it from an infused sheep",
        "tooltip.resourcefulsheep.wool.unknown_resource": "Unknown resource '%s' (removed by a data pack?)",
        "tooltip.resourcefulsheep.wool.separates_into": "Mechanical Separator output:",
        "tooltip.resourcefulsheep.output_line": "%sx %s",
        "tooltip.resourcefulsheep.chance_line": "%sx %s (%s%% chance)",
        "tooltip.resourcefulsheep.energy_time": "%s FE over %s s",
        "tooltip.resourcefulsheep.hold_shift": "Hold Shift for separator details",
        "category.resourcefulsheep.ore": "Ore",
        "category.resourcefulsheep.gem": "Gem",
        "category.resourcefulsheep.dust": "Dust",
        "category.resourcefulsheep.material": "Material",
        "category.resourcefulsheep.organic": "Organic",
        "message.resourcefulsheep.infused": "The sheep's wool shimmers with %s!",
        "message.resourcefulsheep.too_young": "This lamb is too young to be infused",
        "message.resourcefulsheep.needs_wool": "This sheep needs to regrow its wool first",
        "message.resourcefulsheep.cooldown": "This sheep needs to rest for %s more seconds",
        "message.resourcefulsheep.already_infused": "This sheep is already fully infused with %s",
        "message.resourcefulsheep.already_infused_other": "This sheep is already infused with %s",
        "message.resourcefulsheep.needs_more": "Infusing needs %s x %s",
        "message.resourcefulsheep.inspect.none": "This sheep is not infused",
        "message.resourcefulsheep.inspect.cooldown": "This sheep is resting (%s s before it can be infused)",
        "message.resourcefulsheep.inspect.infused": "Infused with %s - %s shearing(s) left",
        "message.resourcefulsheep.inspect.permanent": "Permanently infused with %s",
        "gui.resourcefulsheep.energy": "Energy: %s / %s FE",
        "gui.resourcefulsheep.energy_short": "%s / %s FE",
        "gui.resourcefulsheep.energy_input": "Max input: %s FE/t",
        "gui.resourcefulsheep.energy_usage": "Usage: %s FE/t",
        "gui.resourcefulsheep.progress": "Progress: %s%%",
        "gui.resourcefulsheep.processing_percent": "Processing: %s%%",
        "gui.resourcefulsheep.time_left": "%s s left of %s s",
        "gui.resourcefulsheep.operation_energy": "Operation cost: %s FE",
        "gui.resourcefulsheep.status.idle": "Idle",
        "gui.resourcefulsheep.status.idle.desc": "Insert resource wool to begin",
        "gui.resourcefulsheep.status.processing": "Processing",
        "gui.resourcefulsheep.status.processing.desc": "Separating wool into its resource",
        "gui.resourcefulsheep.status.no_energy": "No Energy",
        "gui.resourcefulsheep.status.no_energy.desc": "Not enough FE for the next tick - progress is paused",
        "gui.resourcefulsheep.status.output_full": "Output Full",
        "gui.resourcefulsheep.status.output_full.desc": "Empty the output slots to continue",
        "gui.resourcefulsheep.status.invalid_input": "Invalid Input",
        "gui.resourcefulsheep.status.invalid_input.desc": "This item cannot be separated",
        "gui.resourcefulsheep.status.redstone_disabled": "Disabled by Redstone",
        "gui.resourcefulsheep.status.redstone_disabled.desc": "Change the redstone signal or mode to run",
        "gui.resourcefulsheep.redstone": "Redstone: %s",
        "gui.resourcefulsheep.redstone.ignored": "Ignored",
        "gui.resourcefulsheep.redstone.ignored.desc": "Always runs",
        "gui.resourcefulsheep.redstone.high": "Active with Signal",
        "gui.resourcefulsheep.redstone.high.desc": "Runs only while powered",
        "gui.resourcefulsheep.redstone.low": "Active without Signal",
        "gui.resourcefulsheep.redstone.low.desc": "Runs only while unpowered",
        "gui.resourcefulsheep.redstone.disabled": "Disabled",
        "gui.resourcefulsheep.redstone.disabled.desc": "Never runs",
        "gui.resourcefulsheep.redstone.powered": "Currently powered",
        "gui.resourcefulsheep.redstone.unpowered": "Currently unpowered",
        "gui.resourcefulsheep.redstone.click": "Click to change",
        "gui.resourcefulsheep.redstone.locked": "Redstone control is disabled in the server config",
        "jei.resourcefulsheep.resource_infusion": "Resource Infusion",
        "jei.resourcefulsheep.mechanical_separation": "Mechanical Separation",
        "jei.resourcefulsheep.consumed": "Consumed: %s per feeding",
        "jei.resourcefulsheep.per_shearing": "Dropped per shearing: %s",
        "jei.resourcefulsheep.uses": "Lasts %s shearing(s)",
        "jei.resourcefulsheep.uses_permanent": "Permanent infusion",
        "jei.resourcefulsheep.category": "Category: %s",
        "jei.resourcefulsheep.energy": "Energy: %s FE",
        "jei.resourcefulsheep.energy_per_tick": "%s FE/t",
        "jei.resourcefulsheep.time": "Time: %s seconds",
        "jei.resourcefulsheep.chance": "%s%% chance",
        "jei.resourcefulsheep.average": "Average: %s",
        "jei.resourcefulsheep.info.resource_wool": "Right-click an adult sheep with a resource (see Resource Infusion) to infuse it. Shearing an infused sheep drops this wool, which a powered Mechanical Separator turns back into the resource plus string.",
        "jei.resourcefulsheep.info.separator": "Separates resource wool into its resource and string using Forge Energy (FE). Hoppers and pipes insert from the top and sides and extract from the bottom by default; outputs are pushed into an inventory below. Configure sides and costs in the server config.",
        "resourcefulsheep.configuration.title": "Resourceful Sheep Configuration",
        "resourcefulsheep.configuration.section.resourcefulsheep.synced.toml": "Gameplay (per world)",
        "resourcefulsheep.configuration.section.resourcefulsheep.synced.toml.title": "Gameplay (per world)",
        "resourcefulsheep.configuration.section.resourcefulsheep.client.toml": "Client",
        "resourcefulsheep.configuration.section.resourcefulsheep.client.toml.title": "Client",
        "resourcefulsheep.configuration.sheep": "Sheep",
        "resourcefulsheep.configuration.separator": "Mechanical Separator",
    }
    for name, r in RESOURCES.items():
        en[f"resource.{MOD}.{name}"] = r["display"]
    write(os.path.join(ASSETS, "lang", "en_us.json"), dict(sorted(en.items())))


def recipes_loot_tags():
    write(os.path.join(DATA, MOD, "recipe", "mechanical_separator.json"), {
        "type": "minecraft:crafting_shaped",
        "category": "redstone",
        "key": {
            "I": "minecraft:iron_ingot",
            "C": "minecraft:copper_ingot",
            "H": "minecraft:hopper",
            "P": "minecraft:piston",
            "R": "minecraft:redstone",
            "S": "minecraft:shears",
        },
        "pattern": ["IHI", "PSP", "CRC"],
        "result": {"id": f"{MOD}:mechanical_separator"},
    })
    # Example explicit separating recipe: plain wool back into string.
    write(os.path.join(DATA, MOD, "recipe", "separating", "wool_to_string.json"), {
        "type": f"{MOD}:separating",
        "ingredient": "#minecraft:wool",
        "primary_output": {"id": "minecraft:string", "count": 3},
        "energy_cost": 2000,
        "processing_ticks": 40,
    })
    write(os.path.join(DATA, MOD, "loot_table", "blocks", "mechanical_separator.json"), {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "condition": {"type": "minecraft:survives_explosion"},
            "entries": [{
                "type": "minecraft:item",
                "name": f"{MOD}:mechanical_separator",
                "modifier": {
                    "type": "minecraft:copy_components",
                    "source": "block_entity",
                    "include": ["minecraft:custom_name", f"{MOD}:machine_contents"],
                },
            }],
        }],
        "random_sequence": f"{MOD}:blocks/mechanical_separator",
    })
    write(os.path.join(DATA, MOD, "loot_modifiers", "infused_wool.json"), {
        "type": f"{MOD}:infused_wool",
        "table_prefix": "shearing/",
    })
    write(os.path.join(DATA, MOD, "tags", "entity_type", "infusable.json"), {"values": ["minecraft:sheep"]})
    write(os.path.join(DATA, MOD, "tags", "item", "replaceable_wool.json"), {"values": ["#minecraft:wool"]})
    write(os.path.join(DATA, "minecraft", "tags", "block", "mineable", "pickaxe.json"), {"values": [f"{MOD}:mechanical_separator"]})
    write(os.path.join(DATA, "minecraft", "tags", "block", "needs_stone_tool.json"), {"values": [f"{MOD}:mechanical_separator"]})


if __name__ == "__main__":
    resource_definitions()
    item_models()
    block_models()
    lang()
    recipes_loot_tags()
    print("Data written to", os.path.abspath(RES))


# ---------------------------------------------------------------------------
# Game test structure: 7x5x7 with a stone floor (NBT, gzip).
# ---------------------------------------------------------------------------
def write_test_structure(data_version=5023):
    import gzip
    import struct

    def tag_string(s):
        b = s.encode("utf-8")
        return struct.pack(">H", len(b)) + b

    def named(tag_id, name, payload):
        return bytes([tag_id]) + tag_string(name) + payload

    def int_list(values):
        return bytes([3]) + struct.pack(">i", len(values)) + b"".join(struct.pack(">i", v) for v in values)

    def compound(entries):
        return b"".join(entries) + b"\x00"

    sx, sy, sz = 7, 5, 7
    palette = [compound([named(8, "Name", tag_string("minecraft:air"))]),
               compound([named(8, "Name", tag_string("minecraft:stone"))])]
    blocks = []
    for y in range(sy):
        for z in range(sz):
            for x in range(sx):
                blocks.append(compound([
                    named(9, "pos", int_list([x, y, z])),
                    named(3, "state", struct.pack(">i", 1 if y == 0 else 0)),
                ]))
    root = compound([
        named(3, "DataVersion", struct.pack(">i", data_version)),
        named(9, "size", int_list([sx, sy, sz])),
        named(9, "palette", bytes([10]) + struct.pack(">i", len(palette)) + b"".join(palette)),
        named(9, "blocks", bytes([10]) + struct.pack(">i", len(blocks)) + b"".join(blocks)),
        named(9, "entities", bytes([0]) + struct.pack(">i", 0)),
    ])
    data = bytes([10]) + tag_string("") + root
    path = os.path.join(DATA, MOD, "structure", "test_platform.nbt")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with gzip.open(path, "wb") as f:
        f.write(data)


write_test_structure()
