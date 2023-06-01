package com.github.sib_energy_craft.machines.screen.slot;

import com.github.sib_energy_craft.machines.core.ExperienceCreatingMachine;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

/**
 * @since 0.0.1
 * @author sibmaks
 */
public class OutputSlot extends Slot {
    private final PlayerEntity player;
    private int amount;

    public OutputSlot(@NotNull PlayerEntity player,
                      @NotNull Inventory inventory,
                      int index,
                      int x,
                      int y) {
        super(inventory, index, x, y);
        this.player = player;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack takeStack(int amount) {
        if (this.hasStack()) {
            this.amount += Math.min(amount, this.getStack().getCount());
        }
        return super.takeStack(amount);
    }

    @Override
    public void onTakeItem(@NotNull PlayerEntity player, @NotNull ItemStack stack) {
        this.onCrafted(stack);
        super.onTakeItem(player, stack);
    }

    @Override
    protected void onCrafted(@NotNull ItemStack stack, int amount) {
        this.amount += amount;
        this.onCrafted(stack);
    }

    @Override
    protected void onCrafted(@NotNull ItemStack stack) {
        stack.onCraft(this.player.getWorld(), this.player, this.amount);
        if (this.player instanceof ServerPlayerEntity serverPlayer &&
                this.inventory instanceof ExperienceCreatingMachine experienceCreatingMachine) {
            experienceCreatingMachine.dropExperienceForRecipesUsed(serverPlayer);
        }
        this.amount = 0;
    }
}