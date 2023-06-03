package com.github.sib_energy_craft.machines.cooking.screen;

import com.github.sib_energy_craft.machines.cooking.block.entity.property.CookingEnergyMachineTypedProperties;
import com.github.sib_energy_craft.machines.screen.EnergyMachineState;
import lombok.Getter;

/**
 * @author sibmaks
 * @since 0.0.36
 */
@Getter
public class CookingEnergyMachineState extends EnergyMachineState {
    private int cookingTime;
    private int cookingTimeTotal;

    @Override
    public <V> void changeProperty(int index, V value) {
        super.changeProperty(index, value);
        if(index == CookingEnergyMachineTypedProperties.COOKING_TIME.getIndex()) {
            cookingTime = (int) value;
        } else if(index == CookingEnergyMachineTypedProperties.COOKING_TIME_TOTAL.getIndex()) {
            cookingTimeTotal = (int) value;
        }
    }
}
