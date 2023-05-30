package com.github.sib_energy_craft.machines;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author sibmaks
 * @since 0.0.13
 */
public class CombinedInventory<T extends Enum<T>> implements Inventory {
    private final List<Inventory> inventories;
    private final EnumMap<T, Inventory> typedInventories;
    private final Map<Inventory, T> inventoryTypes;
    private final Map<Integer, Inventory> stacks;
    private final Map<Inventory, Integer> offsets;

    public CombinedInventory(@NotNull EnumMap<T, Inventory> inventories) {
        this.inventories = List.copyOf(inventories.values());
        this.typedInventories = new EnumMap<>(inventories);
        this.inventoryTypes = inventories.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        this.stacks = new HashMap<>();
        this.offsets = new HashMap<>();
        int stack = 0;
        for (var inventory : this.inventories) {
            offsets.put(inventory, stack);
            for (int i = 0; i < inventory.size(); i++) {
                stacks.put(stack++, inventory);
            }
        }
    }

    @Override
    public int size() {
        return inventories.stream()
                .mapToInt(Inventory::size)
                .sum();
    }

    @Override
    public boolean isEmpty() {
        return inventories.stream().allMatch(Inventory::isEmpty);
    }

    @Override
    public @NotNull ItemStack getStack(int slot) {
        var inventory = stacks.get(slot);
        var offset = offsets.get(inventory);
        return inventory.getStack(slot - offset);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        var inventory = stacks.get(slot);
        var offset = offsets.get(inventory);
        return inventory.removeStack(slot - offset, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        var inventory = stacks.get(slot);
        var offset = offsets.get(inventory);
        return inventory.removeStack(slot - offset);
    }

    @Override
    public void setStack(int slot, @NotNull ItemStack stack) {
        var inventory = stacks.get(slot);
        var offset = offsets.get(inventory);
        inventory.setStack(slot - offset, stack);
    }

    @Override
    public void markDirty() {
        for (var inventory : inventories) {
            inventory.markDirty();
        }
    }

    @Override
    public boolean canPlayerUse(@NotNull PlayerEntity player) {
        return inventories.stream()
                .allMatch(it -> it.canPlayerUse(player));
    }

    @Override
    public void clear() {
        for (var inventory : inventories) {
            inventory.clear();
        }
    }

    /**
     * Get item stack from specific inventory type
     *
     * @param type slot type
     * @param slot slot index
     * @return item stack in inventory
     */
    public @NotNull ItemStack getStack(@NotNull T type, int slot) {
        var inventory = typedInventories.get(type);
        return inventory == null ? ItemStack.EMPTY : inventory.getStack(slot);
    }

    /**
     * Get item stack from specific inventory type
     *
     * @param type slot type
     * @param slot slot index
     * @param itemStack stack to insert
     */
    public void setStack(@NotNull T type,
                         int slot,
                         @NotNull ItemStack itemStack) {
        var inventory = typedInventories.get(type);
        if(inventory != null) {
            inventory.setStack(slot, itemStack);
        }
    }

    /**
     * Get inventory by type
     *
     * @param inventoryType inventory type
     * @return inventory if exists, null - otherwise
     */
    public @Nullable Inventory getInventory(@NotNull T inventoryType) {
        return typedInventories.get(inventoryType);
    }

    /**
     * Get inventory type by slot index
     * @param slot slot index
     * @return inventory type or null
     */
    public @Nullable T getType(int slot) {
        var inventory = stacks.get(slot);
        return inventory == null ? null : inventoryTypes.get(inventory);
    }

    /**
     * Add stack into inventory
     *
     * @param inventoryType inventory type
     * @param stack stack to insert
     * @return not inserted stack
     */
    public @NotNull ItemStack addStack(@NotNull T inventoryType,
                                       @NotNull ItemStack stack) {
        var inventory = getInventory(inventoryType);
        if(inventory == null) {
            return stack;
        }
        var copiedStack = stack.copy();
        addToExistingSlot(inventory, copiedStack);
        if (copiedStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        addToNewSlot(inventory, copiedStack);
        if (copiedStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return copiedStack;
    }

    /**
     * Can stack be inserted into inventory
     *
     * @param inventoryType inventory type
     * @param stack stack to insert
     * @return not inserted stack
     */
    public boolean canInsert(@NotNull T inventoryType,
                             @NotNull ItemStack stack) {
        var inventory = getInventory(inventoryType);
        if(inventory == null) {
            return false;
        }
        if(canAddToExistingSlot(inventory, stack)) {
            return true;
        }
        return canAddToNewSlot(inventory);
    }

    /**
     * Searches this inventory for the specified item and removes the given amount from this inventory.
     *
     * @param inventoryType inventory type
     * @param item item to remove
     * @param count amount to remove
     * @return the stack of removed items
     */
    public @NotNull ItemStack removeItem(@NotNull T inventoryType,
                                         @NotNull Item item,
                                         int count) {
        var removedStack = new ItemStack(item, 0);
        var inventory = getInventory(inventoryType);
        if(inventory == null) {
            return removedStack;
        }
        for (int i = inventory.size() - 1; i >= 0; --i) {
            var inventoryStack = inventory.getStack(i);
            if (!inventoryStack.getItem().equals(item)) {
                continue;
            }
            int needs = count - removedStack.getCount();
            var splitStack = inventoryStack.split(needs);
            removedStack.increment(splitStack.getCount());
            if (removedStack.getCount() == count) {
                break;
            }
        }
        if (!removedStack.isEmpty()) {
            this.markDirty();
        }
        return removedStack;
    }

    /**
     * Check ability to remove from inventory required amount of items.
     *
     * @param inventoryType inventory type
     * @param item item to remove
     * @param count amount to remove
     * @return true - can remove, false - otherwise
     */
    public boolean canRemoveItem(@NotNull T inventoryType,
                                 @NotNull Item item,
                                 int count) {
        var inventory = getInventory(inventoryType);
        if(inventory == null) {
            return false;
        }
        int have = 0;
        for (int i = inventory.size() - 1; i >= 0; --i) {
            var inventoryStack = inventory.getStack(i);
            if (!inventoryStack.getItem().equals(item)) {
                continue;
            }
            have += inventoryStack.getCount();
            if (have >= count) {
                break;
            }
        }
        return have >= count;
    }

    private static boolean canAddToExistingSlot(@NotNull Inventory inventory,
                                                @NotNull ItemStack stack) {
        for (int i = 0; i < inventory.size(); ++i) {
            var itemStack = inventory.getStack(i);
            if (!ItemStack.canCombine(itemStack, stack)) {
                continue;
            }
            if(canTransfer(inventory, stack, itemStack)) {
                return true;
            }
        }
        return false;
    }

    private static void addToExistingSlot(@NotNull Inventory inventory,
                                          @NotNull ItemStack stack) {
        for (int i = 0; i < inventory.size(); ++i) {
            var itemStack = inventory.getStack(i);
            if (!ItemStack.canCombine(itemStack, stack)) {
                continue;
            }
            transfer(inventory, stack, itemStack);
            if (!stack.isEmpty()) continue;
            return;
        }
    }

    private static void addToNewSlot(@NotNull Inventory inventory,
                                     @NotNull ItemStack stack) {
        for (int i = 0; i < inventory.size(); ++i) {
            var itemStack = inventory.getStack(i);
            if (!itemStack.isEmpty()) {
                continue;
            }
            inventory.setStack(i, stack.copy());
            stack.setCount(0);
            return;
        }
    }

    private static boolean canAddToNewSlot(@NotNull Inventory inventory) {
        for (int i = 0; i < inventory.size(); ++i) {
            var itemStack = inventory.getStack(i);
            if (!itemStack.isEmpty()) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static void transfer(@NotNull Inventory inventory,
                                 @NotNull ItemStack source,
                                 @NotNull ItemStack target) {
        int i = Math.min(inventory.getMaxCountPerStack(), target.getMaxCount());
        int j = Math.min(source.getCount(), i - target.getCount());
        if (j > 0) {
            target.increment(j);
            source.decrement(j);
            inventory.markDirty();
        }
    }

    private static boolean canTransfer(@NotNull Inventory inventory,
                                       @NotNull ItemStack source,
                                       @NotNull ItemStack target) {
        int i = Math.min(inventory.getMaxCountPerStack(), target.getMaxCount());
        int j = Math.min(source.getCount(), i - target.getCount());
        return j > 0;
    }

    public void readNbt(@NotNull NbtCompound nbt) {
        for (var entry : typedInventories.entrySet()) {
            var inventoryType = entry.getKey();
            var code = "inventory_%s".formatted(inventoryType.name());
            var nbtList = nbt.getList(code, NbtElement.COMPOUND_TYPE);
            var inventory = entry.getValue();
            for (int i = 0; i < nbtList.size(); ++i) {
                var nbtCompound = nbtList.getCompound(i);
                int j = nbtCompound.getByte("Slot") & 0xFF;
                if (j >= stacks.size()) {
                    continue;
                }
                inventory.setStack(j, ItemStack.fromNbt(nbtCompound));
            }
        }
    }

    public void writeNbt(@NotNull NbtCompound nbt) {
        for (var entry : typedInventories.entrySet()) {
            var inventoryType = entry.getKey();
            var nbtList = new NbtList();
            var inventory = entry.getValue();
            for (int i = 0; i < inventory.size(); ++i) {
                var itemStack = inventory.getStack(i);
                if (itemStack.isEmpty()) {
                    continue;
                }
                var nbtCompound = new NbtCompound();
                nbtCompound.putByte("Slot", (byte)i);
                itemStack.writeNbt(nbtCompound);
                nbtList.add(nbtCompound);
            }
            var code = "inventory_%s".formatted(inventoryType.name());
            nbt.put(code, nbtList);
        }
    }
}
