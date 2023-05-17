package com.github.sib_energy_craft.machines.screen.layout;

import com.github.sib_energy_craft.sec_utils.screen.slot.SlotType;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

/**
 * @author sibmaks
 * @since 0.0.21
 */
public class OneSlotMachineLayoutManager implements SlotLayoutManager {
    private final MultiSlotMachineLayoutManager multiSlotMachineLayoutManager;

    public OneSlotMachineLayoutManager(int quickAccessX, int quickAccessY,
                                       int playerInventoryX, int playerInventoryY,
                                       int sourceSlotX, int sourceSlotY,
                                       int chargeSlotX, int chargeSlotY,
                                       int outputSlotX, int outputSlotY) {
        this.multiSlotMachineLayoutManager = new MultiSlotMachineLayoutManager(
                quickAccessX, quickAccessY,
                playerInventoryX, playerInventoryY,
                new Vector2i[]{new Vector2i(sourceSlotX, sourceSlotY)},
                chargeSlotX, chargeSlotY,
                new Vector2i[]{new Vector2i(outputSlotX, outputSlotY)}
        );
    }

    @Override
    public @NotNull Vector2i getSlotPosition(@NotNull SlotType slotType, int typeIndex, int inventoryIndex) {
        return multiSlotMachineLayoutManager.getSlotPosition(slotType, typeIndex, inventoryIndex);
    }
}
