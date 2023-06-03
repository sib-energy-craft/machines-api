package com.github.sib_energy_craft.machines.cooking.block.entity.property;

import com.github.sib_energy_craft.machines.block.entity.property.EnergyMachineTypedProperties;
import com.github.sib_energy_craft.machines.block.entity.property.EnergyMachineTypedProperty;
import com.github.sib_energy_craft.screen.property.ScreenPropertyType;
import com.github.sib_energy_craft.screen.property.ScreenPropertyTypes;

/**
 * @author sibmaks
 * @since 0.0.36
 */
public enum CookingEnergyMachineTypedProperties implements EnergyMachineTypedProperty<Integer> {
    COOKING_TIME,
    COOKING_TIME_TOTAL;

    private static final int OFFSET = EnergyMachineTypedProperties.values().length;

    @Override
    public int getIndex() {
        return OFFSET + ordinal();
    }

    @Override
    public ScreenPropertyType<Integer> getPropertyType() {
        return ScreenPropertyTypes.INT;
    }
}
