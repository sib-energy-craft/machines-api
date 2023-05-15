package com.github.sib_energy_craft.machines.screen;

/**
 * @author sibmaks
 * @since 0.0.13
 */
public record VisualSettings(
        int quickAccessX, int quickAccessY,
        int playerInventoryX, int playerInventoryY,
        int sourceSlotsX, int sourceSlotsY,
        int chargeSlotX, int chargeSlotY,
        int outputSlotsX, int outputSlotsY) {
}
