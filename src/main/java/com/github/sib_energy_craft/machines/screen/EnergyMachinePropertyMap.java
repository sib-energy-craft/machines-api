package com.github.sib_energy_craft.machines.screen;

import com.github.sib_energy_craft.machines.block.entity.EnergyMachineProperty;
import net.minecraft.screen.PropertyDelegate;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author sibmaks
 * @since 0.0.24
 */
public class EnergyMachinePropertyMap implements PropertyDelegate {
    private final Map<Integer, Supplier<Integer>> supplierMap;
    private final Map<Integer, Consumer<Integer>> consumerMap;

    public EnergyMachinePropertyMap() {
        this.supplierMap = new HashMap<>();
        this.consumerMap = new HashMap<>();
    }

    /**
     * Add read-write property into map
     *
     * @param property property ket
     * @param supplier property supplier
     * @param consumer property consumer
     */
    public<K extends EnergyMachineProperty> void add(@NotNull K property,
                                                     @NotNull Supplier<Integer> supplier,
                                                     @NotNull Consumer<Integer> consumer) {
        int index = property.ordinal();
        if(supplierMap.containsKey(index)) {
            throw new IllegalArgumentException("index %s already exists".formatted(index));
        }
        this.supplierMap.put(index, supplier);
        this.consumerMap.put(index, consumer);
    }

    /**
     * Add readonly property into map
     *
     * @param property property ket
     * @param supplier property supplier
     */
    public<K extends EnergyMachineProperty> void add(@NotNull K property,
                                                     @NotNull Supplier<Integer> supplier) {
        int index = property.ordinal();
        if(supplierMap.containsKey(index)) {
            throw new IllegalArgumentException("index %s already exists".formatted(index));
        }
        this.supplierMap.put(index, supplier);
        this.consumerMap.put(index, it -> {});
    }

    @Override
    public int get(int index) {
        var propertySupplier = supplierMap.get(index);
        return propertySupplier.get();
    }

    @Override
    public void set(int index, int value) {
        var propertyConsumer = consumerMap.get(index);
        propertyConsumer.accept(value);
    }

    @Override
    public int size() {
        return supplierMap.size();
    }
}
