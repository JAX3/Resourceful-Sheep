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

## In-world game tests ✅ (`./gradlew runGameTestServer`): all 24 required tests passed
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
| `creative_generator_powers_separator` | Generator placed behind a separator pushes FE in, and the separator processes Iron Wool on that power alone. Any face allows unlimited extraction (5,000,000 FE pulled in one call). |
| `generator_creates_valid_definitions` | `/resourcefulsheep generate` logic: discovers slime, wheat and bone meal; skips already-defined iron, diamond and netherite and the raw ore blocks; slime block → 9 slime balls. Every generated JSON parses through the real codec, and writing produces one file per resource plus `pack.mcmeta`. |
| `generator_dedupe_removes_duplicate_wool` | After `generate all`: iron, gold, copper, redstone, lapis, coal, diamond, emerald and netherite are flagged as duplicates. Slime, wheat and bone meal are kept, and built-ins are never flagged. Only the duplicate files are deleted. |
| `generator_pick_and_choose` | Material suggestions list slime but not raw ore blocks. An unknown material is rejected. `material slime` uses the storage tag ×9. `item amethyst_block` is fed directly, gives amethyst shard ×1 and parses. `item iron_block` uses the iron tag and is reported as already covered by the built-in. `writeOne` keeps the existing files. |
| `separator_output_rolls_two_to_four` | Every resource's separator output is labelled 2-4 and the machine reserves room for 4. 1,000 rolls per resource stay within 2-4 and produce all of 2, 3 and 4. A plain number still means a fixed count, and min > max is rejected. |
| `custom_definition_with_tag_parses` | A definition using a `#c:storage_blocks/iron` tag, a display name, hex and `[r,g,b]` colours and a custom separator block parses and behaves correctly. Missing items are reported as errors. |

## Real client run ✅ (`./gradlew runClient -PscreenshotHarness`)
The harness creates a world, stages the scene and saves screenshots to `run/screenshots/`. It confirmed:
- Infused sheep render resource-coloured fleece with accent strands on the vanilla model, including dye blending (light-blue + diamond). Plain sheep are unchanged.
- All 12 wool item icons and the separator block/item render with no missing-texture pattern. The log has no model or texture warnings.
- The separator GUI works: energy bar and text, progress arrow, status line, redstone button, slots, and outputs including a bonus roll.
- JEI shows both categories (12 infusion and 13 separation recipes) with catalysts, energy and time.
- Client definitions are synced from the integrated server (12 received).

### Generator, end to end in the real client
The harness ran `/resourcefulsheep generate` in a live world:
- The command wrote 5 definitions and reloaded automatically.
- The server went from 12 to 17 loaded definitions, and all 17 synced to the client.
- JEI showed 17 infusion recipes, including the generated Bone Meal resource with auto-tinted wool.

In a live world, the pick-and-choose commands ran in sequence:
- `generate material slime` added Slime.
- `generate item minecraft:amethyst_block` added Amethyst.
- `iron_block` and `emerald_block` were refused as already covered.
- `generate hand` (holding a Honey Block) added Honey.
- `generate remove slime` removed Slime.

Each step reloaded live: 12 → 13 → 14 → 15 → 14 definitions, all synced to the client.

Earlier, also in a live world, `generate all` followed by `generate dedupe` went from 26 wool types back to 17. It removed the 9 copies of built-ins and listed the reason for each.

This run also caught and fixed a bug: tag-based definitions crashed a live `/reload`, because the empty-tag check ran before Minecraft had bound the new tags.

### Jade integration (real client + dedicated server)
- Jade 26.3.5 loads the Resourceful Sheep plugin on both the game-test server and the client, and all 24 game tests still pass with Jade installed.
- In the client, looking at a diamond-infused sheep showed "Infused: Diamond" and "9 shearing(s) left".
- Looking at a running separator showed "Status: Processing", "Usage: 200 FE/t", its contents, Jade's energy bar and a progress bar.
- The first client run failed: Jade 26.3 rejects one provider class that does both server data and tooltip. Splitting it into separate server and client providers fixed it. The dedicated-server test alone would not have caught this.

## Not tested / known limitations
- **Real multiplayer** with two separate machines was not run. Instead:
  - Every sync path ran in the integrated server, including definitions, recipes, attachment sync and menu data.
  - The game-test server confirmed dedicated-server loading.
- **Third-party energy cables and item pipes** (Mekanism, Thermal, etc.) were not installed. Compatibility is through the standard NeoForge capabilities, which the tests exercise directly and through vanilla hoppers.
- **Modded sheep:** the infusion visuals only apply to renderers that extend the vanilla `SheepRenderer`. Shearing support requires the mod to use a `shearing/...` loot table.
- **Tag-based example definitions** (`examples/`) for tin, lead and others are guarded by `mod_loaded` and `tag_empty` conditions. With the target mods absent they are skipped, as intended. Parsing with a real tag is covered by `custom_definition_with_tag_parses`.
- **Rendering backend:** screenshots came from software Vulkan, because Xvfb offered no matching GLX visual for OpenGL. The textures and models are backend-independent.
- **Sounds:** sound playback wasn't verified, because the container has no audio device.
