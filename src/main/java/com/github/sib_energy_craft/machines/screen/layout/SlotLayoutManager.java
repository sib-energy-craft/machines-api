package com.github.sib_energy_craft.machines.screen.layout;

import com.github.sib_energy_craft.sec_utils.screen.slot.SlotType;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

/**
 * @author sibmaks
 * @since 0.0.21
 */
public interface SlotLayoutManager {
    /**
     * Get slot position
     *
     * @param slotType slot type
     * @param typeIndex slot type index
     * @param inventoryIndex slot inventoryIndex
     * @return slot position
     */
    @NotNull Vector2i getSlotPosition(@NotNull SlotType slotType, int typeIndex, int inventoryIndex);
}
