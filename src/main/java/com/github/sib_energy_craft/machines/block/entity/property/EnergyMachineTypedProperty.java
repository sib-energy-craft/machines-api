package com.github.sib_energy_craft.machines.block.entity.property;

import com.github.sib_energy_craft.screen.property.ScreenPropertyType;

/**
 * @author sibmaks
 * @since 0.0.30
 */
public interface EnergyMachineTypedProperty<T> {

    /**
     * Get property index
     *
     * @return index
     */
    int getIndex();

    /**
     * Get screen property type
     *
     * @return type
     * @see com.github.sib_energy_craft.screen.property.ScreenPropertyTypes
     */
    ScreenPropertyType<T> getPropertyType();
}
