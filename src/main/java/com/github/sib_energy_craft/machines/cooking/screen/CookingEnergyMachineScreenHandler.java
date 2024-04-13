package com.github.sib_energy_craft.machines.cooking.screen;

import com.github.sib_energy_craft.machines.screen.AbstractEnergyMachineScreenHandler;
import com.github.sib_energy_craft.machines.screen.layout.SlotLayoutManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerType;
import org.jetbrains.annotations.NotNull;

/**
 * @author sibmaks
 * @since 0.0.36
 */
public abstract class CookingEnergyMachineScreenHandler<S extends CookingEnergyMachineState>
        extends AbstractEnergyMachineScreenHandler<S> {

    protected CookingEnergyMachineScreenHandler(@NotNull ScreenHandlerType<?> type,
                                                int syncId,
                                                @NotNull PlayerInventory playerInventory,
                                                S energyMachineState,
                                                @NotNull SlotLayoutManager slotLayoutManager) {
        this(type, syncId, playerInventory, 1, 1, energyMachineState, slotLayoutManager);
    }

    protected CookingEnergyMachineScreenHandler(@NotNull ScreenHandlerType<?> type,
                                                int syncId,
                                                @NotNull PlayerInventory playerInventory,
                                                int sourceSlots,
                                                int outputSlots,
                                                S energyMachineState,
                                                @NotNull SlotLayoutManager slotLayoutManager) {
        this(type,
                syncId,
                playerInventory,
                new SimpleInventory(1 + sourceSlots + outputSlots),
                sourceSlots,
                outputSlots,
                energyMachineState,
                slotLayoutManager
        );
    }


    protected CookingEnergyMachineScreenHandler(@NotNull ScreenHandlerType<?> type,
                                                int syncId,
                                                @NotNull PlayerInventory playerInventory,
                                                @NotNull Inventory inventory,
                                                S energyMachineState,
                                                @NotNull SlotLayoutManager slotLayoutManager) {
        this(type, syncId, playerInventory, inventory, 1, 1, energyMachineState, slotLayoutManager);
    }

    protected CookingEnergyMachineScreenHandler(@NotNull ScreenHandlerType<?> type,
                                                int syncId,
                                                @NotNull PlayerInventory playerInventory,
                                                @NotNull Inventory inventory,
                                                int sourceSlots,
                                                int outputSlots,
                                                S energyMachineState,
                                                @NotNull SlotLayoutManager slotLayoutManager) {
        super(type, syncId, playerInventory, inventory, sourceSlots, outputSlots, energyMachineState, slotLayoutManager);
    }

    protected abstract boolean isUsedInMachine(@NotNull ItemStack itemStack);

    /**
     * Get cook progress status
     *
     * @return cook progress
     */
    public int getCookProgress(int width) {
        int i = energyMachineState.getCookingTime();
        int j = energyMachineState.getCookingTimeTotal();
        if (j == 0 || i == 0) {
            return 0;
        }
        return i * width / j;
    }

    /**
     * Get extractor cooking time
     *
     * @return cooking time
     */
    public int getCookingTime() {
        return energyMachineState.getCookingTime();
    }

    /**
     * Get extractor total cooking time
     *
     * @return total cooking time
     */
    public int getCookingTimeTotal() {
        return energyMachineState.getCookingTimeTotal();
    }

}

