package com.github.sib_energy_craft.machines.screen;

import com.github.sib_energy_craft.machines.block.entity.property.EnergyMachineTypedProperties;
import lombok.Getter;

/**
 * @author sibmaks
 * @since 0.0.28
 */
@Getter
public class EnergyMachineState {
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
        if(index == EnergyMachineTypedProperties.CHARGE.getIndex()) {
            charge = (int) value;
        } else if(index == EnergyMachineTypedProperties.MAX_CHARGE.getIndex()) {
            maxCharge = (int) value;
        }
    }
}
