package com.github.sib_energy_craft.machines.screen;

import com.github.sib_energy_craft.machines.block.entity.property.EnergyMachineTypedProperties;
import lombok.Getter;

/**
 * @author sibmaks
 * @since 0.0.28
 */
@Getter
public class EnergyMachineState {
    private int cookingTime;
    private int cookingTimeTotal;
    private int charge;
    private int maxCharge;

    /**
     * Change property value by index
     *
     * @param index property index
     * @param value property value
     * @param <V> type of property
     */
    public <V> void changeProperty(int index, V value) {
        if(index == EnergyMachineTypedProperties.COOKING_TIME.ordinal()) {
            cookingTime = (int) value;
        } else if(index == EnergyMachineTypedProperties.COOKING_TIME_TOTAL.ordinal()) {
            cookingTimeTotal = (int) value;
        } else if(index == EnergyMachineTypedProperties.CHARGE.ordinal()) {
            charge = (int) value;
        } else if(index == EnergyMachineTypedProperties.MAX_CHARGE.ordinal()) {
            maxCharge = (int) value;
        }
    }
}
