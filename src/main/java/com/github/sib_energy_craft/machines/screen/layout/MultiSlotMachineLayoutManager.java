package com.github.sib_energy_craft.machines.screen.layout;

import com.github.sib_energy_craft.machines.screen.EnergyMachineSlotTypes;
import com.github.sib_energy_craft.sec_utils.screen.slot.SlotType;
import com.github.sib_energy_craft.sec_utils.screen.slot.SlotTypes;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

/**
 * @author sibmaks
 * @since 0.0.21
 */
public class MultiSlotMachineLayoutManager implements SlotLayoutManager {
    private final int quickAccessX;
    private final int quickAccessY;

    private final int playerInventoryX;
    private final int playerInventoryY;

    private final Vector2i[] sourceSlots;
    private final Vector2i chargeSlot;
    private final Vector2i[] outputSlots;

    public MultiSlotMachineLayoutManager(int quickAccessX, int quickAccessY,
                                         int playerInventoryX, int playerInventoryY,
                                         @NotNull Vector2i[] sourceSlots,
                                         int chargeSlotX, int chargeSlotY,
                                         @NotNull Vector2i[] outputSlots) {
        this.quickAccessX = quickAccessX;
        this.quickAccessY = quickAccessY;
        this.playerInventoryX = playerInventoryX;
        this.playerInventoryY = playerInventoryY;
        this.sourceSlots = sourceSlots;
        this.chargeSlot = new Vector2i(chargeSlotX, chargeSlotY);
        this.outputSlots = outputSlots;
    }

    public MultiSlotMachineLayoutManager(int quickAccessX, int quickAccessY,
                                        int playerInventoryX, int playerInventoryY,
                                        int sourceSlotX, int sourceSlotY,
                                        int chargeSlotX, int chargeSlotY,
                                        int outputSlotX, int outputSlotY) {
        this(
                quickAccessX, quickAccessY,
                playerInventoryX, playerInventoryY,
                new Vector2i[]{new Vector2i(sourceSlotX, sourceSlotY)},
                chargeSlotX, chargeSlotY,
                new Vector2i[]{new Vector2i(outputSlotX, outputSlotY)}
        );
    }

    @Override
    public @NotNull Vector2i getSlotPosition(@NotNull SlotType slotType, int typeIndex, int inventoryIndex) {
        if(slotType == SlotTypes.QUICK_ACCESS) {
            return new Vector2i(quickAccessX + typeIndex * 18, quickAccessY);
        }
        if(slotType == SlotTypes.PLAYER_INVENTORY) {
            int i = typeIndex / 9;
            int j = typeIndex - i * 9;
            return new Vector2i(playerInventoryX + j * 18, playerInventoryY + i * 18);
        }
        if(slotType == EnergyMachineSlotTypes.SOURCE && typeIndex >= 0 && typeIndex < sourceSlots.length) {
            return sourceSlots[typeIndex];
        }
        if(slotType == EnergyMachineSlotTypes.CHARGE) {
            return chargeSlot;
        }
        if(slotType == EnergyMachineSlotTypes.OUTPUT && typeIndex >= 0 && typeIndex < outputSlots.length) {
            return outputSlots[typeIndex];
        }
        return new Vector2i(0, 0);
    }
}
