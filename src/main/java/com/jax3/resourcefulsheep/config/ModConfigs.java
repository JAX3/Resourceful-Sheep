package com.jax3.resourcefulsheep.config;

import com.jax3.resourcefulsheep.blockentity.RedstoneMode;
import com.jax3.resourcefulsheep.blockentity.RelativeSide;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * All global configuration for Resourceful Sheep.
 *
 * <p>The {@link #SERVER} config is per-world ({@code <world>/syncedconfig/resourcefulsheep-synced.toml}) and is
 * synchronised to connected clients by NeoForge, so values that affect what clients display (JEI energy figures,
 * wool colour blending) stay consistent in multiplayer. {@link #CLIENT} holds purely visual preferences.</p>
 *
 * <p>Per-resource values (feeding item, outputs, colours...) live in data-pack JSON, see
 * {@code SheepResourceManager}. Values here act as global defaults and multipliers on top of them.</p>
 */
public final class ModConfigs {
    public static final Server SERVER;
    public static final ModConfigSpec SERVER_SPEC;
    public static final Client CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    static {
        var server = new ModConfigSpec.Builder().configure(Server::new);
        SERVER = server.getLeft();
        SERVER_SPEC = server.getRight();
        var client = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT = client.getLeft();
        CLIENT_SPEC = client.getRight();
    }

    private ModConfigs() {}

    /** Reads a config value, falling back to its default before the config file is loaded (e.g. on the main menu). */
    public static <T> T get(ModConfigSpec.ConfigValue<T> value) {
        try {
            return value.get();
        } catch (IllegalStateException notLoaded) {
            return value.getDefault();
        }
    }

    public static int getInt(ModConfigSpec.IntValue value) {
        return get(value);
    }

    public static double getDouble(ModConfigSpec.DoubleValue value) {
        return get(value);
    }

    public static boolean getBool(ModConfigSpec.BooleanValue value) {
        return get(value);
    }

    public static final class Server {
        // ---------------------------------------------------------------- sheep
        public final ModConfigSpec.BooleanValue feedingEnabled;
        public final ModConfigSpec.IntValue feedingAmountOverride;
        public final ModConfigSpec.BooleanValue allowBabies;
        public final ModConfigSpec.BooleanValue allowFeedingShearedSheep;
        public final ModConfigSpec.BooleanValue allowReplacingInfusion;
        public final ModConfigSpec.BooleanValue sameResourceRefreshesUses;
        public final ModConfigSpec.BooleanValue infusionConsumedByShearing;
        public final ModConfigSpec.DoubleValue infusionUsesMultiplier;
        public final ModConfigSpec.IntValue woolDropCountOverride;
        public final ModConfigSpec.DoubleValue shearingBonusChance;
        public final ModConfigSpec.IntValue shearingBonusAmount;
        public final ModConfigSpec.BooleanValue preserveWoolColour;
        public final ModConfigSpec.BooleanValue feedingParticles;
        public final ModConfigSpec.IntValue infusionCooldownTicks;
        public final ModConfigSpec.BooleanValue creativeConsumesItems;
        public final ModConfigSpec.BooleanValue creativeIgnoresCooldown;
        public final ModConfigSpec.BooleanValue sneakToInspect;

        // ---------------------------------------------------------------- separator
        public final ModConfigSpec.IntValue energyCapacity;
        public final ModConfigSpec.IntValue maxEnergyInput;
        public final ModConfigSpec.IntValue defaultEnergyCost;
        public final ModConfigSpec.IntValue energyPerTickOverride;
        public final ModConfigSpec.IntValue defaultProcessingTicks;
        public final ModConfigSpec.IntValue defaultStringOutput;
        public final ModConfigSpec.DoubleValue primaryOutputMultiplier;
        public final ModConfigSpec.DoubleValue bonusPrimaryChance;
        public final ModConfigSpec.DoubleValue energyCostMultiplier;
        public final ModConfigSpec.DoubleValue processingSpeedMultiplier;
        public final ModConfigSpec.BooleanValue resetProgressWhenUnpowered;
        public final ModConfigSpec.ConfigValue<List<? extends String>> energyInputSides;
        public final ModConfigSpec.ConfigValue<List<? extends String>> itemInputSides;
        public final ModConfigSpec.ConfigValue<List<? extends String>> itemOutputSides;
        public final ModConfigSpec.BooleanValue autoPushOutputs;
        public final ModConfigSpec.IntValue autoPushInterval;
        public final ModConfigSpec.BooleanValue redstoneControlEnabled;
        public final ModConfigSpec.EnumValue<RedstoneMode> defaultRedstoneMode;
        public final ModConfigSpec.IntValue creativeGeneratorOutput;

        Server(ModConfigSpec.Builder b) {
            b.comment("Resource infusion of sheep. Per-resource settings live in data/<namespace>/resourcefulsheep/sheep_resources/*.json")
                    .push("sheep");
            feedingEnabled = b.comment("Allow players to infuse sheep by right-clicking them with a resource.")
                    .define("feedingEnabled", true);
            feedingAmountOverride = b.comment("Items consumed per feeding. -1 uses each resource definition's 'feeding_amount'.")
                    .defineInRange("feedingAmountOverride", -1, -1, 64);
            allowBabies = b.comment("Allow lambs to be infused (their wool is only dropped once grown).")
                    .define("allowBabies", false);
            allowFeedingShearedSheep = b.comment("Allow infusing a sheep that is currently sheared; its wool regrows infused.")
                    .define("allowFeedingShearedSheep", true);
            allowReplacingInfusion = b.comment("Allow feeding a different resource to an already-infused sheep, replacing the old infusion (the old one is lost).")
                    .define("allowReplacingInfusion", false);
            sameResourceRefreshesUses = b.comment("Feeding the same resource again tops the remaining shear count back up to full.")
                    .define("sameResourceRefreshesUses", true);
            infusionConsumedByShearing = b.comment("If true, each shearing uses up one 'use' of the infusion; when none remain the sheep returns to normal.",
                            "If false, infusions are PERMANENT (turns sheep into an unlimited generator - not recommended for survival packs).")
                    .define("infusionConsumedByShearing", true);
            infusionUsesMultiplier = b.comment("Multiplier applied to each resource's 'infusion_uses' (number of shearings an infusion lasts). Result is rounded, minimum 1.")
                    .defineInRange("infusionUsesMultiplier", 1.0, 0.01, 100.0);
            woolDropCountOverride = b.comment("Infused wool items dropped per shearing. -1 uses each resource definition's 'wool_drop_count'.")
                    .defineInRange("woolDropCountOverride", -1, -1, 64);
            shearingBonusChance = b.comment("Chance (0-1) for a shearing to drop bonus infused wool.")
                    .defineInRange("shearingBonusChance", 0.0, 0.0, 1.0);
            shearingBonusAmount = b.comment("Number of bonus infused wool items when the shearing bonus triggers.")
                    .defineInRange("shearingBonusAmount", 1, 0, 64);
            preserveWoolColour = b.comment("If true an infused sheep's wool keeps a hint of its dye colour; if false it shows only the resource colour.")
                    .define("preserveWoolColour", true);
            feedingParticles = b.comment("Show resource-coloured particles and play a sound when a sheep is infused.")
                    .define("feedingParticles", true);
            infusionCooldownTicks = b.comment("Ticks after an infusion runs out before the sheep can be infused again. 0 disables.")
                    .defineInRange("infusionCooldownTicks", 0, 0, 24000 * 10);
            creativeConsumesItems = b.comment("Whether feeding consumes items for players in creative mode.")
                    .define("creativeConsumesItems", false);
            creativeIgnoresCooldown = b.comment("Whether creative-mode players bypass the infusion cooldown and replacement restrictions.")
                    .define("creativeIgnoresCooldown", true);
            sneakToInspect = b.comment("Sneak + right-click a sheep with an empty hand to see its infusion in the action bar.")
                    .define("sneakToInspect", true);
            b.pop();

            b.comment("Mechanical Separator machine").push("separator");
            energyCapacity = b.comment("Internal FE buffer size.")
                    .defineInRange("energyCapacity", 100_000, 1, Integer.MAX_VALUE);
            maxEnergyInput = b.comment("Maximum FE accepted per tick (per insertion call).")
                    .defineInRange("maxEnergyInput", 1_000, 1, Integer.MAX_VALUE);
            defaultEnergyCost = b.comment("Total FE to separate one wool when a resource/recipe does not specify 'energy_cost'.")
                    .defineInRange("defaultEnergyCost", 20_000, 0, Integer.MAX_VALUE);
            energyPerTickOverride = b.comment("If > 0, the machine always uses exactly this much FE per tick (total = this x processing ticks),",
                            "ignoring per-recipe energy costs. 0 = derive per-tick usage from each recipe's total energy cost.")
                    .defineInRange("energyPerTickOverride", 0, 0, Integer.MAX_VALUE);
            defaultProcessingTicks = b.comment("Processing time in ticks when a resource/recipe does not specify 'processing_ticks'.")
                    .defineInRange("defaultProcessingTicks", 100, 1, 72_000);
            defaultStringOutput = b.comment("String produced per wool when a resource does not specify 'secondary_count'.")
                    .defineInRange("defaultStringOutput", 2, 0, 64);
            primaryOutputMultiplier = b.comment("Multiplier on the primary (resource) output. Fractions are applied as a chance for one extra item.")
                    .defineInRange("primaryOutputMultiplier", 1.0, 0.0, 64.0);
            bonusPrimaryChance = b.comment("Global chance (0-1) to produce one extra primary output per operation, in addition to any per-resource bonus.")
                    .defineInRange("bonusPrimaryChance", 0.0, 0.0, 1.0);
            energyCostMultiplier = b.comment("Efficiency multiplier on total energy cost. 0.5 = half the FE per operation.")
                    .defineInRange("energyCostMultiplier", 1.0, 0.0, 100.0);
            processingSpeedMultiplier = b.comment("Speed multiplier. 2.0 = operations take half as many ticks (total energy is unchanged).")
                    .defineInRange("processingSpeedMultiplier", 1.0, 0.01, 100.0);
            resetProgressWhenUnpowered = b.comment("If true, progress resets when the machine runs out of energy instead of pausing.")
                    .define("resetProgressWhenUnpowered", false);
            String sideHelp = "Valid values: FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM (relative to the machine's front face).";
            energyInputSides = b.comment("Sides that accept FE.", sideHelp)
                    .defineListAllowEmpty("energyInputSides", List.of("BACK", "LEFT", "RIGHT", "TOP", "BOTTOM"), () -> "BACK", ModConfigs::isSide);
            itemInputSides = b.comment("Sides through which hoppers/pipes may insert wool.", sideHelp)
                    .defineListAllowEmpty("itemInputSides", List.of("TOP", "BACK", "LEFT", "RIGHT"), () -> "TOP", ModConfigs::isSide);
            itemOutputSides = b.comment("Sides through which hoppers/pipes may extract outputs, and that outputs are auto-pushed into.", sideHelp)
                    .defineListAllowEmpty("itemOutputSides", List.of("BOTTOM"), () -> "BOTTOM", ModConfigs::isSide);
            autoPushOutputs = b.comment("Automatically push outputs into inventories adjacent to output sides.")
                    .define("autoPushOutputs", true);
            autoPushInterval = b.comment("Ticks between automatic output pushes.")
                    .defineInRange("autoPushInterval", 10, 1, 1200);
            redstoneControlEnabled = b.comment("Allow players to change the redstone mode from the GUI. If false every machine uses defaultRedstoneMode.")
                    .define("redstoneControlEnabled", true);
            defaultRedstoneMode = b.comment("Redstone mode for newly placed machines.")
                    .defineEnum("defaultRedstoneMode", RedstoneMode.IGNORED);
            b.pop();

            b.comment("Creative Generator (creative-only infinite FE source for testing)").push("creative_generator");
            creativeGeneratorOutput = b.comment("FE pushed into each adjacent energy receiver per tick (receivers still apply their own input limits). 0 disables pushing.")
                    .defineInRange("creativeGeneratorOutput", 1_000_000, 0, Integer.MAX_VALUE);
            b.pop();
        }

        public Set<RelativeSide> sides(ModConfigSpec.ConfigValue<List<? extends String>> value) {
            EnumSet<RelativeSide> set = EnumSet.noneOf(RelativeSide.class);
            for (String s : get(value)) {
                RelativeSide side = RelativeSide.parse(s);
                if (side != null) set.add(side);
            }
            return set;
        }
    }

    public static final class Client {
        public final ModConfigSpec.BooleanValue ambientParticles;
        public final ModConfigSpec.DoubleValue dyeTintStrength;
        public final ModConfigSpec.BooleanValue showSheepSparkleOverlay;
        public final ModConfigSpec.BooleanValue showResourceGrowths;
        public final ModConfigSpec.BooleanValue detailedWoolTooltips;
        public final ModConfigSpec.BooleanValue detailedMachineTooltips;

        Client(ModConfigSpec.Builder b) {
            ambientParticles = b.comment("Occasionally emit resource-coloured particles from infused sheep.")
                    .define("ambientParticles", true);
            dyeTintStrength = b.comment("How strongly a sheep's dye colour shows through infused wool (0 = pure resource colour, 1 = pure dye colour).",
                            "Only applies when the server option preserveWoolColour is enabled.")
                    .defineInRange("dyeTintStrength", 0.3, 0.0, 1.0);
            showSheepSparkleOverlay = b.comment("Render the glinting accent overlay on infused sheep wool.")
                    .define("showSheepSparkleOverlay", true);
            showResourceGrowths = b.comment("Render small blocks of the infused resource growing out of the sheep's fleece (fewer as the infusion is used up).")
                    .define("showResourceGrowths", true);
            detailedWoolTooltips = b.comment("Show separation output, energy and time in resource wool tooltips (otherwise only while holding Shift).")
                    .define("detailedWoolTooltips", false);
            detailedMachineTooltips = b.comment("Show extended status tooltips (energy per tick, recipe time) in the Mechanical Separator GUI.")
                    .define("detailedMachineTooltips", true);
        }
    }

    private static boolean isSide(Object o) {
        return o instanceof String s && RelativeSide.parse(s) != null;
    }

    static String upper(String s) {
        return s.trim().toUpperCase(Locale.ROOT);
    }

    /** Small convenience for lambdas that need lazily-evaluated config values. */
    public static <T> Supplier<T> lazy(ModConfigSpec.ConfigValue<T> value) {
        return () -> get(value);
    }
}
