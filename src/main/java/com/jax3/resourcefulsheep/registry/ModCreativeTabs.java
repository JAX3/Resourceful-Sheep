package com.jax3.resourcefulsheep.registry;

import com.jax3.resourcefulsheep.ResourcefulSheep;
import com.jax3.resourcefulsheep.items.ResourceWoolItem;
import com.jax3.resourcefulsheep.resource.SheepResource;
import com.jax3.resourcefulsheep.resource.SheepResourceRegistry;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ResourcefulSheep.MOD_ID);

    /** Built-in resources, used to populate the tab before a world (and therefore the data pack) is loaded. */
    public static final List<String> BUILT_IN = List.of("iron", "gold", "copper", "diamond", "emerald", "redstone", "lapis",
            "coal", "netherite", "stone", "wood", "quartz");

    public static final Supplier<CreativeModeTab> MAIN = REGISTER.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.resourcefulsheep"))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> ResourceWoolItem.create(ResourcefulSheep.id("gold"), 1))
            .displayItems((params, output) -> {
                output.accept(ModItems.MECHANICAL_SEPARATOR.get());
                output.accept(ModItems.CREATIVE_GENERATOR.get());
                SheepResourceRegistry loaded = SheepResourceRegistry.client();
                if (loaded.isEmpty()) {
                    BUILT_IN.forEach(name -> output.accept(ResourceWoolItem.create(ResourcefulSheep.id(name), 1)));
                } else {
                    for (SheepResource resource : loaded.all()) {
                        output.accept(ResourceWoolItem.create(resource.id(), 1));
                    }
                }
            })
            .build());

    private ModCreativeTabs() {}

    public static Identifier tabId() {
        return ResourcefulSheep.id("main");
    }
}
