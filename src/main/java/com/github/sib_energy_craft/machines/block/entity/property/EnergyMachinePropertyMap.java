package com.github.sib_energy_craft.machines.block.entity.property;

import com.github.sib_energy_craft.network.PropertyUpdateSyncer;
import com.github.sib_energy_craft.screen.property.TypedScreenProperty;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author sibmaks
 * @since 0.0.31
 */
public class EnergyMachinePropertyMap {
    private final List<TypedScreenProperty<?>> properties;

    public EnergyMachinePropertyMap() {
        this.properties = new ArrayList<>();
    }

    /**
     * Add property into map
     *
     * @param property property type
     * @param supplier property value supplier
     * @param <T> property Java type
     */
    public synchronized <T> void add(@NotNull EnergyMachineTypedProperty<T> property,
                                     @NotNull Supplier<T> supplier) {
        var typedProperty = new TypedScreenProperty<>(property.getIndex(), property.getPropertyType(), supplier);
        properties.add(typedProperty);
    }

    /**
     * Create property syncer
     *
     * @param syncId sync id
     * @param serverPlayerEntity server player
     * @return property syncer instance
     */
    public @NotNull PropertyUpdateSyncer createSyncer(int syncId,
                                                      @NotNull ServerPlayerEntity serverPlayerEntity) {
        return new PropertyUpdateSyncer(syncId, serverPlayerEntity, properties);
    }
}
