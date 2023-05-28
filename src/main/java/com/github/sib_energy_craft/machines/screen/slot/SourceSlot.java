package com.github.sib_energy_craft.machines.screen.slot;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * @author sibmaks
 * @since 0.0.31
 */
public class SourceSlot extends Slot {
    private final Predicate<ItemStack> usedInMachinePredicate;

    public SourceSlot(@NotNull Inventory inventory,
                      int index,
                      int x,
                      int y,
                      @NotNull Predicate<ItemStack> usedInMachinePredicate) {
        super(inventory, index, x, y);
        this.usedInMachinePredicate = usedInMachinePredicate;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return usedInMachinePredicate.test(stack);
    }
}
