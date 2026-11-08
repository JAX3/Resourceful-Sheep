package com.jax3.resourcefulsheep.menu;

import com.jax3.resourcefulsheep.blockentity.MachineStatus;
import com.jax3.resourcefulsheep.blockentity.MechanicalSeparatorBlockEntity;
import com.jax3.resourcefulsheep.blockentity.RedstoneMode;
import com.jax3.resourcefulsheep.blockentity.SeparatorInventory;
import com.jax3.resourcefulsheep.recipe.SeparationLookup;
import com.jax3.resourcefulsheep.registry.ModBlocks;
import com.jax3.resourcefulsheep.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import org.jspecify.annotations.Nullable;

/** Container menu of the Mechanical Separator. Slot coordinates match {@code textures/gui/mechanical_separator.png}. */
public class MechanicalSeparatorMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 44, INPUT_Y = 56;
    public static final int PRIMARY_X = 108, PRIMARY_Y = 47;
    public static final int SECONDARY_X = 108, SECONDARY_Y = 69;
    public static final int BONUS_X = 130, BONUS_Y = 47;
    public static final int INVENTORY_Y = 111;
    public static final int BUTTON_REDSTONE = 0;

    private static final int MACHINE_SLOTS = SeparatorInventory.SIZE;
    private static final int PLAYER_START = MACHINE_SLOTS;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final Level level;
    private final @Nullable MechanicalSeparatorBlockEntity blockEntity;

    /** Client-side constructor, called with the extra data written by {@code openMenu(provider, pos)}. */
    public MechanicalSeparatorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), null,
                new SeparatorInventory(stack -> SeparationLookup.canProcess(playerInventory.player.level(), stack), () -> {}),
                new SimpleContainerData(MechanicalSeparatorBlockEntity.DATA_COUNT));
    }

    /** Server-side constructor. */
    public MechanicalSeparatorMenu(int containerId, Inventory playerInventory, MechanicalSeparatorBlockEntity be, ContainerData data) {
        this(containerId, playerInventory, be.getBlockPos(), be, be.getInventory(), data);
    }

    private MechanicalSeparatorMenu(int containerId, Inventory playerInventory, BlockPos pos, @Nullable MechanicalSeparatorBlockEntity be,
                                    SeparatorInventory inventory, ContainerData data) {
        super(ModMenus.MECHANICAL_SEPARATOR.get(), containerId);
        checkContainerDataCount(data, MechanicalSeparatorBlockEntity.DATA_COUNT);
        this.level = playerInventory.player.level();
        this.access = ContainerLevelAccess.create(level, pos);
        this.data = data;
        this.blockEntity = be;

        addSlot(new ResourceHandlerSlot(inventory, inventory::set, SeparatorInventory.INPUT, INPUT_X, INPUT_Y));
        addSlot(new ResourceHandlerSlot(inventory, inventory::set, SeparatorInventory.PRIMARY, PRIMARY_X, PRIMARY_Y));
        addSlot(new ResourceHandlerSlot(inventory, inventory::set, SeparatorInventory.SECONDARY, SECONDARY_X, SECONDARY_Y));
        addSlot(new ResourceHandlerSlot(inventory, inventory::set, SeparatorInventory.BONUS, BONUS_X, BONUS_Y));
        addStandardInventorySlots(playerInventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, original);
        } else if (SeparationLookup.canProcess(level, stack)) {
            if (!moveItemStackTo(stack, SeparatorInventory.INPUT, SeparatorInventory.INPUT + 1, false)) return ItemStack.EMPTY;
        } else if (index < PLAYER_START + 27) {
            if (!moveItemStackTo(stack, PLAYER_START + 27, PLAYER_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, PLAYER_START, PLAYER_START + 27, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_REDSTONE && blockEntity != null) {
            blockEntity.cycleRedstoneMode();
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.MECHANICAL_SEPARATOR.get());
    }

    // ------------------------------------------------------------------ synced values (client reads these)

    private int combine(int low, int high) {
        return (data.get(low) & 0xFFFF) | ((data.get(high) & 0xFFFF) << 16);
    }

    public int getProgress() { return combine(0, 1); }
    public int getMaxProgress() { return combine(2, 3); }
    public int getEnergy() { return combine(4, 5); }
    public int getCapacity() { return combine(6, 7); }
    public int getEnergyPerTick() { return combine(8, 9); }
    public MachineStatus getStatus() { return MachineStatus.byId(data.get(10)); }
    public RedstoneMode getRedstoneMode() { return RedstoneMode.byId(data.get(11)); }
    public boolean isPowered() { return data.get(12) != 0; }

    public float getProgressFraction() {
        int max = getMaxProgress();
        return max <= 0 ? 0 : Math.min(1.0F, getProgress() / (float) max);
    }

    public float getEnergyFraction() {
        int cap = getCapacity();
        return cap <= 0 ? 0 : Math.min(1.0F, getEnergy() / (float) cap);
    }
}
