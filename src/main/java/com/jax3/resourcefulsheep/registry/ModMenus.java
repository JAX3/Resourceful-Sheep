package com.jax3.resourcefulsheep.registry;

import com.jax3.resourcefulsheep.ResourcefulSheep;
import com.jax3.resourcefulsheep.menu.MechanicalSeparatorMenu;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(Registries.MENU, ResourcefulSheep.MOD_ID);

    public static final Supplier<MenuType<MechanicalSeparatorMenu>> MECHANICAL_SEPARATOR = REGISTER.register("mechanical_separator",
            () -> IMenuTypeExtension.create(MechanicalSeparatorMenu::new));

    private ModMenus() {}
}
