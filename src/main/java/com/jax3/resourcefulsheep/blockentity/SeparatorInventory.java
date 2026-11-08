package com.jax3.resourcefulsheep.blockentity;

import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

/**
 * The separator's four slots: {@link #INPUT}, {@link #PRIMARY}, {@link #SECONDARY} and {@link #BONUS}.
 *
 * <p>Only the input slot is "valid" for insertion, which makes hoppers, pipes and the GUI refuse to put anything into
 * the output slots. The machine itself fills outputs through {@link #addOutput}, which bypasses that check.</p>
 */
public class SeparatorInventory extends ItemStacksResourceHandler {
    public static final int INPUT = 0;
    public static final int PRIMARY = 1;
    public static final int SECONDARY = 2;
    public static final int BONUS = 3;
    public static final int SIZE = 4;

    private final Predicate<ItemStack> inputValidator;
    private final Runnable onChanged;

    public SeparatorInventory(Predicate<ItemStack> inputValidator, Runnable onChanged) {
        super(SIZE);
        this.inputValidator = inputValidator;
        this.onChanged = onChanged;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return index == INPUT && !resource.isEmpty() && inputValidator.test(resource.toStack());
    }

    @Override
    protected void onContentsChanged(int index, ItemStack previousContents) {
        onChanged.run();
    }

    public ItemStack getStack(int index) {
        return stacks.get(index);
    }

    /** Replaces a slot's contents without validation (machine-internal use and loading). */
    public void setStack(int index, ItemStack stack) {
        ItemStack previous = stacks.get(index);
        stacks.set(index, stack);
        onContentsChanged(index, previous);
    }

    /** How many of {@code stack} could be added to {@code index}, ignoring validation. */
    public int spaceFor(int index, ItemStack stack) {
        if (stack.isEmpty()) return Integer.MAX_VALUE;
        ItemStack current = stacks.get(index);
        if (current.isEmpty()) return stack.getMaxStackSize();
        if (!ItemStack.isSameItemSameComponents(current, stack)) return 0;
        return Math.max(0, current.getMaxStackSize() - current.getCount());
    }

    /** Adds as much of {@code stack} as fits into {@code index}; returns what did not fit. */
    public ItemStack addOutput(int index, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int fit = Math.min(stack.getCount(), spaceFor(index, stack));
        if (fit <= 0) return stack;
        ItemStack current = stacks.get(index);
        setStack(index, current.isEmpty() ? stack.copyWithCount(fit) : current.copyWithCount(current.getCount() + fit));
        return stack.copyWithCount(stack.getCount() - fit);
    }
}
