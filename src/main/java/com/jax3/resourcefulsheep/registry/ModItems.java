package com.jax3.resourcefulsheep.registry;

import com.jax3.resourcefulsheep.ResourcefulSheep;
import com.jax3.resourcefulsheep.items.ResourceWoolItem;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(ResourcefulSheep.MOD_ID);

    /**
     * A single item for every kind of infused wool. The {@link ModDataComponents#SHEEP_RESOURCE} component identifies
     * the resource, so data packs can add new wool types at runtime without registering new items.
     */
    public static final DeferredItem<ResourceWoolItem> RESOURCE_WOOL = REGISTER.registerItem("resource_wool", ResourceWoolItem::new);

    public static final DeferredItem<BlockItem> MECHANICAL_SEPARATOR = REGISTER.registerSimpleBlockItem(ModBlocks.MECHANICAL_SEPARATOR);

    public static final DeferredItem<BlockItem> CREATIVE_GENERATOR = REGISTER.registerSimpleBlockItem(ModBlocks.CREATIVE_GENERATOR,
            p -> p.rarity(net.minecraft.world.item.Rarity.EPIC));

    private ModItems() {}
}
