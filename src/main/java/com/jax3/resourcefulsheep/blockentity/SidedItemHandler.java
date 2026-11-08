package com.jax3.resourcefulsheep.blockentity;

import java.util.Objects;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Automation view of the separator inventory for one side: exposes all four slots (so pipes can inspect contents)
 * but only allows insertion into the input slot and/or extraction from the output slots, depending on the side's
 * configuration.
 */
public final class SidedItemHandler implements ResourceHandler<ItemResource> {
    private final SeparatorInventory inventory;
    private final boolean allowInsert;
    private final boolean allowExtract;

    public SidedItemHandler(SeparatorInventory inventory, boolean allowInsert, boolean allowExtract) {
        this.inventory = inventory;
        this.allowInsert = allowInsert;
        this.allowExtract = allowExtract;
    }

    @Override
    public int size() {
        return inventory.size();
    }

    @Override
    public ItemResource getResource(int index) {
        return inventory.getResource(index);
    }

    @Override
    public long getAmountAsLong(int index) {
        return inventory.getAmountAsLong(index);
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return inventory.getCapacityAsLong(index, resource);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return allowInsert && inventory.isValid(index, resource);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size());
        if (!allowInsert || index != SeparatorInventory.INPUT) return 0;
        return inventory.insert(index, resource, amount, transaction);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size());
        if (!allowExtract || index == SeparatorInventory.INPUT) return 0;
        return inventory.extract(index, resource, amount, transaction);
    }
}
