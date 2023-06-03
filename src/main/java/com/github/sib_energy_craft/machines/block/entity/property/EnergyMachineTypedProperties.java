package com.github.sib_energy_craft.machines.block.entity.property;

import com.github.sib_energy_craft.screen.property.ScreenPropertyType;
import com.github.sib_energy_craft.screen.property.ScreenPropertyTypes;

/**
 * @since 0.0.1
 * @author sibmaks
 */
public enum EnergyMachineTypedProperties implements EnergyMachineTypedProperty<Integer> {
    CHARGE,
    MAX_CHARGE;

    public static final int PROPERTIES_SIZE = values().length;

    @Override
    public int getIndex() {
        return ordinal();
    }

    @Override
    public ScreenPropertyType<Integer> getPropertyType() {
        return ScreenPropertyTypes.INT;
    }
}
