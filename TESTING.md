# Test report

Environment:
- Minecraft 26.3 + NeoForge 26.3.0.101, JEI 31.17.0.109
- OpenJDK 25 on Linux
- Client rendering through Mesa lavapipe (software Vulkan) on Xvfb

## Build ✅
- `./gradlew build`: **BUILD SUCCESSFUL**.
  - All dependencies resolve: NeoForge, JEI API, plus JEI and its config library at runtime.
  - Produces `build/libs/resourcefulsheep-1.0.0.jar`.

## Unit tests ✅ (`./gradlew test`): 5/5 passed
Relative side mapping for every facing, side-name parsing, redstone modes, infusion countdown and cooldown, and permanent infusions.

## In-world game tests ✅ (`./gradlew runGameTestServer`): all 19 required tests passed
These run on a **dedicated-server** game test server, which also confirms the mod loads with no client classes present.

| Test | Verifies |
|---|---|
| `definitions_loaded` | All 12 built-in definitions load. Iron Block → iron; Iron Ingot is not a feeding item. Iron separates to 1 Raw Iron + 2 String at 20,000 FE / 100 ticks. |
| `feed_iron_block_infuses_sheep` | Feeding an Iron Block infuses the sheep (9 uses), consumes exactly 1 block and keeps the dye colour. Re-feeding at full uses consumes nothing. |
| `unrecognised_item_is_ignored` | Dirt neither infuses nor is consumed. |
| `shearing_infused_sheep_drops_resource_wool` | Exactly 1 Iron Wool and no white wool. Uses go 9 → 8. A sheared sheep can't be sheared again (no duplication). |
| `shearing_plain_sheep_drops_normal_wool` | Uninfused sheep still drop white wool. |
| `infusion_runs_out_after_uses` | A Diamond infusion (1 use) clears after shearing, and the next shearing gives normal wool. |
| `dispenser_shears_infused_sheep` | A powered dispenser with shears drops Gold Wool and no plain wool. |
| `infusion_survives_save_and_load` | The attachment round-trips through entity save/load. |
| `replacing_infusion_is_refused_by_default` | Gold can't overwrite iron, and the refused feeding consumes nothing. |
| `separator_produces_raw_iron_and_string` | Iron Wool → Raw Iron + 2 String, using exactly 20,000 FE. |
| `separator_halts_without_energy` | Progress pauses at 25/100 with 5,000 FE, status is NO_ENERGY and the block goes inactive. It resumes and finishes once powered. |
| `separator_automation_sides` | Top accepts wool and rejects dirt. Bottom is extract-only. Front has no item capability. Energy is accepted on the back but not the front. Insert rate is limited. The machine never outputs energy. |
| `separator_hopper_chain` | Hopper → separator → hopper → chest delivers 2 Coal + 4 String. |
| `separator_never_overfills_output` | With 63 string already in the slot, the machine reports OUTPUT_FULL and uses no energy or input. |
| `separator_state_survives_save_and_load` | Energy, progress and inventory round-trip through block entity save/load. |
| `separating_recipe_handles_plain_wool` | The custom recipe type matches `#minecraft:wool` → 3 String. Dirt is not separable. |
| `config_multipliers_affect_values` | Speed ×2 → 50 ticks and energy ×0.5 → 10,000 FE. Restored afterwards. |
| `custom_definition_with_tag_parses` | A definition using a `#c:storage_blocks/iron` tag, a display name, hex and `[r,g,b]` colours and a custom separator block parses and behaves correctly. Missing items are reported as errors. |

## Real client run ✅ (`./gradlew runClient -PscreenshotHarness`)
The harness creates a world, stages the scene and saves screenshots to `run/screenshots/`. It confirmed:
- Infused sheep render resource-coloured fleece with accent strands on the vanilla model, including dye blending (light-blue + diamond). Plain sheep are unchanged.
- All 12 wool item icons and the separator block/item render with no missing-texture pattern. The log has no model or texture warnings.
- The separator GUI works: energy bar and text, progress arrow, status line, redstone button, slots, and outputs including a bonus roll.
- JEI shows both categories (12 infusion and 13 separation recipes) with catalysts, energy and time.
- Client definitions are synced from the integrated server (12 received).

## Not tested / known limitations
- **Real multiplayer** with two separate machines was not run. Instead:
  - Every sync path ran in the integrated server, including definitions, recipes, attachment sync and menu data.
  - The game-test server confirmed dedicated-server loading.
- **Third-party energy cables and item pipes** (Mekanism, Thermal, etc.) were not installed. Compatibility is through the standard NeoForge capabilities, which the tests exercise directly and through vanilla hoppers.
- **Modded sheep:** the infusion visuals only apply to renderers that extend the vanilla `SheepRenderer`. Shearing support requires the mod to use a `shearing/...` loot table.
- **Tag-based example definitions** (`examples/`) for tin, lead and others are guarded by `mod_loaded` and `tag_empty` conditions. With the target mods absent they are skipped, as intended. Parsing with a real tag is covered by `custom_definition_with_tag_parses`.
- **Rendering backend:** screenshots came from software Vulkan, because Xvfb offered no matching GLX visual for OpenGL. The textures and models are backend-independent.
- **Sounds:** sound playback wasn't verified, because the container has no audio device.
