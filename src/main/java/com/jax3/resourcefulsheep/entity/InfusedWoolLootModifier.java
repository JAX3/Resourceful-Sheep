package com.jax3.resourcefulsheep.entity;

import com.jax3.resourcefulsheep.ResourcefulSheep;
import com.jax3.resourcefulsheep.config.ModConfigs;
import com.jax3.resourcefulsheep.items.ResourceWoolItem;
import com.jax3.resourcefulsheep.registry.ModTags;
import com.jax3.resourcefulsheep.resource.SheepResource;
import com.jax3.resourcefulsheep.resource.SheepResourceRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Replaces the wool from shearing loot tables with resource wool when the sheared entity is infused.
 *
 * <p>Vanilla sheep (and NeoForge's {@code IShearable}, which shears and dispensers both go through) generate their
 * drops from the {@code minecraft:shearing/sheep} loot table, so hooking the loot is the one place that covers hand
 * shears, dispensers and modded shearing tools alike. The infusion is consumed here, exactly once per loot roll,
 * which also prevents double drops.</p>
 *
 * <p>The JSON instance ({@code data/resourcefulsheep/loot_modifiers/infused_wool.json}) restricts it to tables whose
 * path starts with {@code table_prefix} (default {@code shearing/}), so modded sheep that use their own shearing
 * tables also work once their entity type is added to {@code #resourcefulsheep:infusable}.</p>
 */
public final class InfusedWoolLootModifier extends LootModifier {
    public static final MapCodec<InfusedWoolLootModifier> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i).and(
            Codec.STRING.optionalFieldOf("table_prefix", "shearing/").forGetter(m -> m.tablePrefix)
    ).apply(i, InfusedWoolLootModifier::new));

    private final String tablePrefix;

    public InfusedWoolLootModifier(Optional<Holder<LootItemCondition>> condition, int priority, String tablePrefix) {
        super(condition, priority);
        this.tablePrefix = tablePrefix;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!context.getQueriedLootTableId().getPath().startsWith(tablePrefix)) return loot;
        Entity entity = context.getOptional(LootContextParams.THIS_ENTITY);
        if (entity == null || !ModTags.isInfusable(entity)) return loot;

        SheepInfusion infusion = SheepInfusionEvents.getInfusion(entity);
        if (!infusion.isInfused()) return loot;
        Optional<SheepResource> resource = SheepResourceRegistry.server().byId(infusion.resource().get());
        if (resource.isEmpty()) {
            // Definition was removed by a data pack change: drop the stale infusion and leave vanilla loot alone.
            ResourcefulSheep.LOGGER.debug("Clearing unknown infusion {} from {}", infusion.resource().get(), entity);
            SheepInfusionEvents.setInfusion(entity, SheepInfusion.NONE);
            return loot;
        }

        loot.removeIf(stack -> stack.is(ModTags.REPLACEABLE_WOOL));
        int count = resource.get().effectiveWoolDropCount();
        if (context.getRandom().nextDouble() < ModConfigs.getDouble(ModConfigs.SERVER.shearingBonusChance)) {
            count += ModConfigs.getInt(ModConfigs.SERVER.shearingBonusAmount);
        }
        while (count > 0) {
            int n = Math.min(count, 64);
            loot.add(ResourceWoolItem.create(resource.get().id(), n));
            count -= n;
        }

        SheepInfusionEvents.setInfusion(entity, infusion.afterShearing(context.getLevel().getGameTime(),
                ModConfigs.getInt(ModConfigs.SERVER.infusionCooldownTicks)));
        return loot;
    }

    @Override
    public MapCodec<? extends LootModifier> codec() {
        return CODEC;
    }
}
