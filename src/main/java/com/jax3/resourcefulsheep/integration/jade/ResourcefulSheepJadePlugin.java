package com.jax3.resourcefulsheep.integration.jade;

import com.jax3.resourcefulsheep.blockentity.MechanicalSeparatorBlockEntity;
import com.jax3.resourcefulsheep.blocks.MechanicalSeparatorBlock;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade integration (optional: only loaded by Jade through {@link WailaPlugin}, so the mod runs without Jade).
 *
 * <ul>
 *     <li><b>Sheep</b> (any {@code #resourcefulsheep:infusable} mob): infused resource, shearings left, cooldown.</li>
 *     <li><b>Mechanical Separator</b>: Jade's progress arrow plus the machine status and FE/t. Stored energy and the
 *     inventory are shown by Jade's built-in support for NeoForge's energy and item capabilities.</li>
 * </ul>
 * Each part can be toggled in Jade's settings (Plugins &gt; Resourceful Sheep).
 */
@WailaPlugin
public class ResourcefulSheepJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(SeparatorStatusDataProvider.INSTANCE, MechanicalSeparatorBlockEntity.class);
        registration.registerProgress(SeparatorProgressProvider.INSTANCE, MechanicalSeparatorBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(SheepInfusionProvider.INSTANCE, LivingEntity.class);
        registration.registerBlockComponent(SeparatorStatusProvider.INSTANCE, MechanicalSeparatorBlock.class);
        registration.registerProgressClient(SeparatorProgressProvider.INSTANCE);
    }
}
