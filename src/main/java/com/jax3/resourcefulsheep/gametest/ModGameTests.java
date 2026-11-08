package com.jax3.resourcefulsheep.gametest;

import com.google.gson.JsonParser;
import com.jax3.resourcefulsheep.ResourcefulSheep;
import com.jax3.resourcefulsheep.blockentity.MachineStatus;
import com.jax3.resourcefulsheep.blockentity.MechanicalSeparatorBlockEntity;
import com.jax3.resourcefulsheep.blockentity.SeparatorInventory;
import com.jax3.resourcefulsheep.config.ModConfigs;
import com.jax3.resourcefulsheep.entity.SheepInfusion;
import com.jax3.resourcefulsheep.entity.SheepInfusionEvents;
import com.jax3.resourcefulsheep.items.ResourceWoolItem;
import com.jax3.resourcefulsheep.recipe.SeparationLookup;
import com.jax3.resourcefulsheep.registry.ModBlocks;
import com.jax3.resourcefulsheep.registry.ModItems;
import com.jax3.resourcefulsheep.resource.SheepResource;
import com.jax3.resourcefulsheep.resource.SheepResourceRegistry;
import com.jax3.resourcefulsheep.resource.Separation;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * In-world integration tests, run with {@code ./gradlew runGameTestServer} (a real dedicated-server environment,
 * which also proves no client classes are loaded on the server).
 */
public final class ModGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, ResourcefulSheep.MOD_ID);
    private static final Identifier STRUCTURE = ResourcefulSheep.id("test_platform");
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new java.util.LinkedHashMap<>();

    static {
        TESTS.put("definitions_loaded", ModGameTests::definitionsLoaded);
        TESTS.put("feed_iron_block_infuses_sheep", ModGameTests::feedIronBlock);
        TESTS.put("unrecognised_item_is_ignored", ModGameTests::unrecognisedItemIgnored);
        TESTS.put("shearing_infused_sheep_drops_resource_wool", ModGameTests::shearInfused);
        TESTS.put("shearing_plain_sheep_drops_normal_wool", ModGameTests::shearPlain);
        TESTS.put("infusion_runs_out_after_uses", ModGameTests::infusionRunsOut);
        TESTS.put("dispenser_shears_infused_sheep", ModGameTests::dispenserShears);
        TESTS.put("infusion_survives_save_and_load", ModGameTests::infusionPersists);
        TESTS.put("replacing_infusion_is_refused_by_default", ModGameTests::replaceRefused);
        TESTS.put("separator_produces_raw_iron_and_string", ModGameTests::separatorProcesses);
        TESTS.put("separator_halts_without_energy", ModGameTests::separatorNoEnergy);
        TESTS.put("separator_automation_sides", ModGameTests::separatorAutomationSides);
        TESTS.put("separator_hopper_chain", ModGameTests::separatorHopperChain);
        TESTS.put("separator_never_overfills_output", ModGameTests::separatorOutputFull);
        TESTS.put("separator_state_survives_save_and_load", ModGameTests::separatorPersists);
        TESTS.put("separating_recipe_handles_plain_wool", ModGameTests::separatingRecipe);
        TESTS.put("config_multipliers_affect_values", ModGameTests::configMultipliers);
        TESTS.put("custom_definition_with_tag_parses", ModGameTests::customDefinitionParses);
        TESTS.put("creative_generator_powers_separator", ModGameTests::creativeGeneratorPowers);
        TESTS.put("generator_creates_valid_definitions", ModGameTests::generatorCreatesDefinitions);
        TESTS.forEach((name, fn) -> FUNCTIONS.register(name, () -> fn));
    }

    private ModGameTests() {}

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(ResourcefulSheep.id("default"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS.keySet()) {
            ResourceKey<Consumer<GameTestHelper>> fn = ResourceKey.create(Registries.TEST_FUNCTION, ResourcefulSheep.id(name));
            event.registerTest(ResourcefulSheep.id(name), new FunctionGameTestInstance(fn, new TestData<>(env, STRUCTURE, 400, 0, true)));
        }
    }

    // ------------------------------------------------------------------ helpers

    private static final Identifier IRON = ResourcefulSheep.id("iron");

    private static Sheep sheep(GameTestHelper h) {
        Sheep sheep = h.spawn(EntityTypes.SHEEP, new BlockPos(3, 1, 3));
        sheep.setNoAi(true);
        sheep.setColor(DyeColor.WHITE);
        return sheep;
    }

    private static Player playerHolding(GameTestHelper h, ItemStack stack) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    private static void feed(Player player, Sheep sheep) {
        player.interactOn(sheep, InteractionHand.MAIN_HAND, sheep.position());
    }

    private static List<ItemStack> shear(GameTestHelper h, Sheep sheep) {
        IShearable shearable = sheep;
        return shearable.onSheared(null, new ItemStack(Items.SHEARS), h.getLevel(), sheep.blockPosition());
    }

    private static MechanicalSeparatorBlockEntity separator(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, ModBlocks.MECHANICAL_SEPARATOR.get().defaultBlockState());
        return h.getBlockEntity(pos, MechanicalSeparatorBlockEntity.class);
    }

    private static int insertEnergy(EnergyHandler handler, int amount) {
        int total = 0;
        while (total < amount) {
            try (Transaction tx = Transaction.openRoot()) {
                int inserted = handler.insert(amount - total, tx);
                tx.commit();
                if (inserted <= 0) break;
                total += inserted;
            }
        }
        return total;
    }

    private static int insertItem(ResourceHandler<ItemResource> handler, ItemStack stack) {
        try (Transaction tx = Transaction.openRoot()) {
            int n = handler.insert(ItemResource.of(stack), stack.getCount(), tx);
            tx.commit();
            return n;
        }
    }

    // ------------------------------------------------------------------ sheep tests

    private static void definitionsLoaded(GameTestHelper h) {
        SheepResourceRegistry reg = SheepResourceRegistry.server();
        for (String name : List.of("iron", "gold", "copper", "diamond", "emerald", "redstone", "lapis", "coal", "netherite", "stone", "wood", "quartz")) {
            h.assertTrue(reg.byId(ResourcefulSheep.id(name)).isPresent(), "missing built-in resource " + name);
        }
        h.assertTrue(reg.byFeedingItem(new ItemStack(Items.IRON_BLOCK)).map(SheepResource::id).orElse(null) != null
                && reg.byFeedingItem(new ItemStack(Items.IRON_BLOCK)).get().id().equals(IRON), "iron block should map to iron");
        h.assertTrue(reg.byFeedingItem(new ItemStack(Items.IRON_INGOT)).isEmpty(), "iron ingot must not be a feeding item by default");
        Separation iron = reg.byId(IRON).get().separation();
        h.assertTrue(iron.primary().create().is(Items.RAW_IRON) && iron.primary().count() == 1, "iron separates to 1 raw iron");
        h.assertTrue(iron.secondary().isPresent() && iron.secondary().get().create().is(Items.STRING) && iron.secondary().get().count() == 2, "iron gives 2 string");
        h.assertTrue(iron.effectiveEnergy() == 20000 && iron.effectiveTicks() == 100, "iron costs 20,000 FE over 100 ticks, got " + iron.effectiveEnergy() + "/" + iron.effectiveTicks());
        h.succeed();
    }

    private static void feedIronBlock(GameTestHelper h) {
        Sheep sheep = sheep(h);
        Player player = playerHolding(h, new ItemStack(Items.IRON_BLOCK, 3));
        feed(player, sheep);
        SheepInfusion infusion = SheepInfusionEvents.getInfusion(sheep);
        h.assertTrue(infusion.resource().map(IRON::equals).orElse(false), "sheep should be infused with iron, was " + infusion);
        h.assertTrue(infusion.usesRemaining() == 9, "iron infusion should last 9 shearings, was " + infusion.usesRemaining());
        h.assertTrue(player.getMainHandItem().getCount() == 2, "one iron block should be consumed");
        h.assertTrue(sheep.getColor() == DyeColor.WHITE, "dye colour must be preserved");
        // Feeding again at full uses is refused and does not consume.
        feed(player, sheep);
        h.assertTrue(player.getMainHandItem().getCount() == 2, "a fully infused sheep must not consume more items");
        h.succeed();
    }

    private static void unrecognisedItemIgnored(GameTestHelper h) {
        Sheep sheep = sheep(h);
        Player player = playerHolding(h, new ItemStack(Items.DIRT, 5));
        feed(player, sheep);
        h.assertFalse(SheepInfusionEvents.getInfusion(sheep).isInfused(), "dirt must not infuse");
        h.assertTrue(player.getMainHandItem().getCount() == 5, "dirt must not be consumed");
        h.succeed();
    }

    private static void shearInfused(GameTestHelper h) {
        Sheep sheep = sheep(h);
        feed(playerHolding(h, new ItemStack(Items.IRON_BLOCK)), sheep);
        List<ItemStack> drops = shear(h, sheep);
        h.assertTrue(sheep.isSheared(), "sheep should be sheared");
        h.assertTrue(drops.stream().noneMatch(s -> s.is(Items.WOOL.white())), "infused sheep must not drop normal wool: " + drops);
        long ironWool = drops.stream().filter(s -> IRON.equals(ResourceWoolItem.getResourceId(s))).mapToInt(ItemStack::getCount).sum();
        h.assertTrue(ironWool == 1, "expected exactly 1 iron wool, got " + drops);
        h.assertTrue(SheepInfusionEvents.getInfusion(sheep).usesRemaining() == 8, "one use consumed");
        // Already sheared: a second shear attempt does nothing (no duplication).
        h.assertFalse(((IShearable) sheep).isShearable(null, new ItemStack(Items.SHEARS), h.getLevel(), sheep.blockPosition()), "sheared sheep not shearable");
        h.succeed();
    }

    private static void shearPlain(GameTestHelper h) {
        Sheep sheep = sheep(h);
        List<ItemStack> drops = shear(h, sheep);
        h.assertTrue(!drops.isEmpty() && drops.stream().allMatch(s -> s.is(Items.WOOL.white())), "plain sheep drops white wool, got " + drops);
        h.succeed();
    }

    private static void infusionRunsOut(GameTestHelper h) {
        Sheep sheep = sheep(h);
        feed(playerHolding(h, new ItemStack(Items.DIAMOND)), sheep); // diamond: 1 use
        List<ItemStack> first = shear(h, sheep);
        h.assertTrue(first.stream().anyMatch(s -> ResourcefulSheep.id("diamond").equals(ResourceWoolItem.getResourceId(s))), "diamond wool dropped");
        h.assertFalse(SheepInfusionEvents.getInfusion(sheep).isInfused(), "single-use infusion must be cleared after shearing");
        sheep.setSheared(false); // regrow
        List<ItemStack> second = shear(h, sheep);
        h.assertTrue(second.stream().allMatch(s -> s.is(Items.WOOL.white())), "sheep returns to normal wool, got " + second);
        h.succeed();
    }

    private static void dispenserShears(GameTestHelper h) {
        Sheep sheep = h.spawn(EntityTypes.SHEEP, new BlockPos(3, 1, 3));
        sheep.setNoAi(true);
        feed(playerHolding(h, new ItemStack(Items.GOLD_BLOCK)), sheep);
        BlockPos dispenserPos = new BlockPos(3, 1, 2);
        h.setBlock(dispenserPos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.SOUTH));
        DispenserBlockEntity dispenser = h.getBlockEntity(dispenserPos, DispenserBlockEntity.class);
        dispenser.setItem(0, new ItemStack(Items.SHEARS));
        h.setBlock(new BlockPos(2, 1, 2), Blocks.REDSTONE_BLOCK);
        h.succeedWhen(() -> {
            h.assertTrue(sheep.isSheared(), "dispenser should shear the sheep");
            AABB box = new AABB(h.absolutePos(new BlockPos(3, 1, 3))).inflate(3);
            List<ItemEntity> items = h.getLevel().getEntitiesOfClass(ItemEntity.class, box);
            h.assertTrue(items.stream().anyMatch(e -> ResourcefulSheep.id("gold").equals(ResourceWoolItem.getResourceId(e.getItem()))), "gold wool item entity expected");
            h.assertTrue(items.stream().noneMatch(e -> e.getItem().is(Items.WOOL.white())), "no plain wool expected");
        });
    }

    private static void infusionPersists(GameTestHelper h) {
        Sheep sheep = sheep(h);
        feed(playerHolding(h, new ItemStack(Items.EMERALD)), sheep);
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        sheep.saveWithoutId(out);
        CompoundTag tag = out.buildResult();
        Sheep loaded = (Sheep) EntityType.create(EntityTypes.SHEEP, TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), tag),
                h.getLevel(), EntitySpawnReason.LOAD).orElseThrow();
        h.assertTrue(SheepInfusionEvents.getInfusion(loaded).equals(SheepInfusionEvents.getInfusion(sheep)),
                "infusion lost on reload: " + SheepInfusionEvents.getInfusion(loaded));
        h.succeed();
    }

    private static void replaceRefused(GameTestHelper h) {
        Sheep sheep = sheep(h);
        feed(playerHolding(h, new ItemStack(Items.IRON_BLOCK)), sheep);
        Player player = playerHolding(h, new ItemStack(Items.GOLD_BLOCK));
        feed(player, sheep);
        h.assertTrue(SheepInfusionEvents.getInfusion(sheep).resource().map(IRON::equals).orElse(false), "iron infusion must not be replaced");
        h.assertTrue(player.getMainHandItem().getCount() == 1, "refused feeding must not consume");
        h.succeed();
    }

    // ------------------------------------------------------------------ separator tests

    private static void separatorProcesses(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        MechanicalSeparatorBlockEntity be = separator(h, pos);
        h.assertTrue(insertEnergy(be.getEnergy(), 100_000) == 100_000, "machine should accept 100,000 FE");
        be.getInventory().setStack(SeparatorInventory.INPUT, ResourceWoolItem.create(IRON, 1));
        h.succeedWhen(() -> {
            h.assertTrue(be.getInventory().getStack(SeparatorInventory.PRIMARY).is(Items.RAW_IRON), "raw iron expected");
            h.assertTrue(be.getInventory().getStack(SeparatorInventory.SECONDARY).is(Items.STRING)
                    && be.getInventory().getStack(SeparatorInventory.SECONDARY).getCount() == 2, "2 string expected");
            h.assertTrue(be.getInventory().getStack(SeparatorInventory.INPUT).isEmpty(), "input consumed");
            h.assertTrue(be.getEnergy().getEnergy() == 80_000, "exactly 20,000 FE used, have " + be.getEnergy().getEnergy());
        });
    }

    private static void separatorNoEnergy(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        MechanicalSeparatorBlockEntity be = separator(h, pos);
        be.getInventory().setStack(SeparatorInventory.INPUT, ResourceWoolItem.create(IRON, 1));
        insertEnergy(be.getEnergy(), 5_000); // enough for 25 of 100 ticks
        h.runAfterDelay(60, () -> {
            h.assertTrue(be.getStatus() == MachineStatus.NO_ENERGY, "status should be NO_ENERGY, was " + be.getStatus());
            h.assertTrue(be.getProgress() == 25, "progress should pause at 25, was " + be.getProgress());
            h.assertTrue(!be.getInventory().getStack(SeparatorInventory.INPUT).isEmpty(), "input must remain");
            h.assertTrue(be.getInventory().getStack(SeparatorInventory.PRIMARY).isEmpty(), "no output without energy");
            h.assertFalse(h.getBlockState(pos).getValue(com.jax3.resourcefulsheep.blocks.MechanicalSeparatorBlock.ACTIVE), "inactive when unpowered");
            insertEnergy(be.getEnergy(), 100_000);
        });
        h.runAfterDelay(160, () -> {
            h.assertTrue(be.getInventory().getStack(SeparatorInventory.PRIMARY).is(Items.RAW_IRON), "resumes and finishes once powered");
            h.succeed();
        });
    }

    private static void separatorAutomationSides(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        MechanicalSeparatorBlockEntity be = separator(h, pos); // facing north
        var level = h.getLevel();
        BlockPos abs = h.absolutePos(pos);
        ResourceHandler<ItemResource> top = level.getCapability(Capabilities.Item.BLOCK, abs, Direction.UP);
        ResourceHandler<ItemResource> bottom = level.getCapability(Capabilities.Item.BLOCK, abs, Direction.DOWN);
        ResourceHandler<ItemResource> front = level.getCapability(Capabilities.Item.BLOCK, abs, Direction.NORTH);
        h.assertTrue(top != null && bottom != null, "top and bottom expose item handlers");
        h.assertTrue(front == null, "front exposes no item handler by default");
        h.assertTrue(insertItem(top, new ItemStack(Items.DIRT)) == 0, "invalid input rejected");
        h.assertTrue(insertItem(top, ResourceWoolItem.create(IRON, 4)) == 4, "wool accepted from top");
        h.assertTrue(insertItem(bottom, ResourceWoolItem.create(IRON, 1)) == 0, "bottom is output-only");
        be.getInventory().setStack(SeparatorInventory.PRIMARY, new ItemStack(Items.RAW_IRON, 5));
        try (Transaction tx = Transaction.openRoot()) {
            h.assertTrue(top.extract(ItemResource.of(Items.RAW_IRON), 5, tx) == 0, "top cannot extract outputs");
            h.assertTrue(bottom.extract(ItemResource.of(Items.RAW_IRON), 5, tx) == 5, "bottom extracts outputs");
            h.assertTrue(bottom.extract(ResourceWoolItem.getResource(ResourceWoolItem.create(IRON, 1), false).map(r -> ItemResource.of(ResourceWoolItem.create(IRON, 1))).orElseThrow(), 1, tx) == 0,
                    "input cannot be extracted");
        }
        EnergyHandler frontEnergy = level.getCapability(Capabilities.Energy.BLOCK, abs, Direction.NORTH);
        EnergyHandler backEnergy = level.getCapability(Capabilities.Energy.BLOCK, abs, Direction.SOUTH);
        h.assertTrue(frontEnergy == null && backEnergy != null, "energy accepted on back, not front");
        try (Transaction tx = Transaction.openRoot()) {
            h.assertTrue(backEnergy.insert(5_000, tx) == ModConfigs.getInt(ModConfigs.SERVER.maxEnergyInput), "insert rate limited");
            h.assertTrue(backEnergy.extract(100, tx) == 0, "machine never outputs energy");
        }
        h.succeed();
    }

    private static void separatorHopperChain(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 2, 3);
        MechanicalSeparatorBlockEntity be = separator(h, pos);
        insertEnergy(be.getEnergy(), 100_000);
        h.setBlock(new BlockPos(3, 3, 3), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        h.setBlock(new BlockPos(3, 1, 3), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.NORTH));
        h.setBlock(new BlockPos(3, 1, 2), Blocks.CHEST);
        HopperBlockEntity input = h.getBlockEntity(new BlockPos(3, 3, 3), HopperBlockEntity.class);
        input.setItem(0, ResourceWoolItem.create(ResourcefulSheep.id("coal"), 2));
        h.succeedWhen(() -> {
            BlockEntity chest = h.getBlockEntity(new BlockPos(3, 1, 2), BlockEntity.class);
            int coal = 0, string = 0;
            var container = (net.minecraft.world.Container) chest;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack s = container.getItem(i);
                if (s.is(Items.COAL)) coal += s.getCount();
                if (s.is(Items.STRING)) string += s.getCount();
            }
            h.assertTrue(coal == 2 && string == 4, "chest should receive 2 coal and 4 string, has " + coal + "/" + string);
        });
    }

    private static void separatorOutputFull(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        MechanicalSeparatorBlockEntity be = separator(h, pos);
        insertEnergy(be.getEnergy(), 100_000);
        be.getInventory().setStack(SeparatorInventory.SECONDARY, new ItemStack(Items.STRING, 63));
        be.getInventory().setStack(SeparatorInventory.INPUT, ResourceWoolItem.create(IRON, 1));
        h.runAfterDelay(120, () -> {
            h.assertTrue(be.getStatus() == MachineStatus.OUTPUT_FULL, "should report OUTPUT_FULL, was " + be.getStatus());
            h.assertTrue(be.getInventory().getStack(SeparatorInventory.INPUT).getCount() == 1, "input untouched");
            h.assertTrue(be.getEnergy().getEnergy() == 100_000, "no energy used while blocked");
            h.succeed();
        });
    }

    private static void separatorPersists(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        MechanicalSeparatorBlockEntity be = separator(h, pos);
        insertEnergy(be.getEnergy(), 50_000);
        be.getInventory().setStack(SeparatorInventory.INPUT, ResourceWoolItem.create(IRON, 3));
        h.runAfterDelay(10, () -> {
            CompoundTag tag = be.saveWithFullMetadata(h.getLevel().registryAccess());
            BlockEntity loaded = BlockEntity.loadStatic(be.getBlockPos(), be.getBlockState(), tag, h.getLevel().registryAccess());
            h.assertTrue(loaded instanceof MechanicalSeparatorBlockEntity, "reload produced " + loaded);
            MechanicalSeparatorBlockEntity copy = (MechanicalSeparatorBlockEntity) loaded;
            h.assertTrue(copy.getEnergy().getEnergy() == be.getEnergy().getEnergy(), "energy persisted");
            h.assertTrue(copy.getProgress() == be.getProgress() && copy.getProgress() > 0, "progress persisted: " + copy.getProgress());
            h.assertTrue(ItemStack.matches(copy.getInventory().getStack(SeparatorInventory.INPUT), be.getInventory().getStack(SeparatorInventory.INPUT)), "inventory persisted");
            h.succeed();
        });
    }

    private static void separatingRecipe(GameTestHelper h) {
        var sep = SeparationLookup.find(h.getLevel(), new ItemStack(Items.WOOL.red()));
        h.assertTrue(sep.isPresent() && sep.get().primary().create().is(Items.STRING) && sep.get().primary().count() == 3,
                "separating recipe should turn wool into 3 string: " + sep);
        h.assertTrue(SeparationLookup.find(h.getLevel(), new ItemStack(Items.DIRT)).isEmpty(), "dirt is not separable");
        h.succeed();
    }

    private static void configMultipliers(GameTestHelper h) {
        Separation iron = SheepResourceRegistry.server().byId(IRON).orElseThrow().separation();
        double oldEnergy = ModConfigs.SERVER.energyCostMultiplier.get();
        double oldSpeed = ModConfigs.SERVER.processingSpeedMultiplier.get();
        try {
            ModConfigs.SERVER.energyCostMultiplier.set(0.5);
            ModConfigs.SERVER.processingSpeedMultiplier.set(2.0);
            h.assertTrue(iron.effectiveTicks() == 50, "speed x2 halves ticks, got " + iron.effectiveTicks());
            h.assertTrue(iron.effectiveEnergy() == 10_000, "energy x0.5 halves cost, got " + iron.effectiveEnergy());
        } finally {
            ModConfigs.SERVER.energyCostMultiplier.set(oldEnergy);
            ModConfigs.SERVER.processingSpeedMultiplier.set(oldSpeed);
        }
        h.assertTrue(iron.effectiveEnergy() == 20_000, "restored");
        h.succeed();
    }

    private static void customDefinitionParses(GameTestHelper h) {
        String json = """
                {
                  "feeding_item": "#c:storage_blocks/iron",
                  "feeding_amount": 2,
                  "display_name": "Test Metal",
                  "wool_color": "#123456",
                  "accent_color": [255, 0, 0],
                  "category": "ore",
                  "separator": { "primary_output": { "id": "minecraft:iron_nugget", "count": 4 }, "secondary_count": 0 }
                }""";
        var ops = h.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        SheepResource.Definition def = SheepResource.DEFINITION_CODEC.parse(ops, JsonParser.parseString(json)).getOrThrow();
        SheepResource r = SheepResource.withId(ResourcefulSheep.id("test_metal"), def);
        h.assertTrue(r.feedingItem().test(new ItemStack(Items.IRON_BLOCK)), "tag-based feeding item should accept iron blocks");
        h.assertTrue(r.woolColor() == 0x123456 && r.accentColor() == 0xFF0000, "colours parsed");
        h.assertTrue(r.effectiveFeedingAmount() == 2, "feeding amount");
        Separation s = r.separation();
        h.assertTrue(s.primary().create().is(Items.IRON_NUGGET) && s.primary().count() == 4 && s.secondary().isEmpty(), "separator block parsed: " + s);
        h.assertTrue(r.name().getString().equals("Test Metal"), "display name");

        String bad = "{ \"feeding_item\": \"notamod:missing_item\" }";
        h.assertTrue(SheepResource.DEFINITION_CODEC.parse(ops, JsonParser.parseString(bad)).error().isPresent(), "missing items are reported as errors");
        h.assertTrue(ModItems.RESOURCE_WOOL.get() != null, "items registered");
        h.succeed();
    }

    private static void creativeGeneratorPowers(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        MechanicalSeparatorBlockEntity be = separator(h, pos); // facing north: back (south) accepts FE
        h.setBlock(new BlockPos(3, 1, 4), ModBlocks.CREATIVE_GENERATOR.get());
        be.getInventory().setStack(SeparatorInventory.INPUT, ResourceWoolItem.create(IRON, 1));
        // Pull side: any face of the generator offers unlimited energy.
        EnergyHandler source = h.getLevel().getCapability(Capabilities.Energy.BLOCK, h.absolutePos(new BlockPos(3, 1, 4)), Direction.UP);
        h.assertTrue(source != null, "generator exposes an energy handler");
        try (Transaction tx = Transaction.openRoot()) {
            h.assertTrue(source.extract(5_000_000, tx) == 5_000_000, "unlimited extraction");
        }
        h.succeedWhen(() -> {
            h.assertTrue(be.getEnergy().getEnergy() > 0, "generator should push FE into the separator");
            h.assertTrue(be.getInventory().getStack(SeparatorInventory.PRIMARY).is(Items.RAW_IRON), "separator runs on generator power");
        });
    }

    private static void generatorCreatesDefinitions(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        var plan = com.jax3.resourcefulsheep.command.ResourceGenerator.plan(registries, SheepResourceRegistry.server(), false);
        java.util.Set<String> names = new java.util.HashSet<>();
        plan.forEach(g -> names.add(g.name()));
        h.assertTrue(names.contains("slime") && names.contains("wheat") && names.contains("bone_meal"), "vanilla storage-block materials discovered: " + names);
        h.assertFalse(names.contains("iron") || names.contains("diamond") || names.contains("netherite"), "already-defined materials skipped: " + names);
        h.assertFalse(names.stream().anyMatch(n -> n.startsWith("raw_")), "raw ore blocks skipped: " + names);
        var slime = plan.stream().filter(g -> g.name().equals("slime")).findFirst().orElseThrow();
        h.assertTrue(slime.output() == Items.SLIME_BALL && slime.uses() == 9, "slime block -> 9 slime balls, got " + slime.output() + " x" + slime.uses());

        // Every generated file must load through the real definition codec, and actually accept its block.
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        for (var g : plan) {
            var parsed = SheepResource.DEFINITION_CODEC.parse(ops, g.json());
            h.assertTrue(parsed.error().isEmpty(), g.name() + " does not parse: " + parsed.error().map(Object::toString).orElse(""));
            SheepResource r = SheepResource.withId(ResourcefulSheep.id(g.name()), parsed.getOrThrow());
            h.assertTrue(r.feedingItem().items().findAny().isPresent(), g.name() + " feeds nothing");
        }
        // Writing produces one file per definition plus pack.mcmeta.
        try {
            java.nio.file.Path tmp = java.nio.file.Files.createTempDirectory("rs-gen");
            java.nio.file.Path pack = com.jax3.resourcefulsheep.command.ResourceGenerator.write(tmp, plan);
            h.assertTrue(java.nio.file.Files.exists(pack.resolve("pack.mcmeta")), "pack.mcmeta written");
            try (var files = java.nio.file.Files.list(pack.resolve("data/resourcefulsheep_generated/resourcefulsheep/sheep_resources"))) {
                h.assertTrue(files.count() == plan.size(), "one JSON per resource");
            }
        } catch (java.io.IOException e) {
            h.fail("write failed: " + e);
        }
        h.succeed();
    }
}
