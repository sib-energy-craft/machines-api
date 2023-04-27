package com.github.sib_energy_craft.machines.screen.slot;

import com.github.sib_energy_craft.energy_api.screen.ChargeSlot;
import com.github.sib_energy_craft.machines.block.entity.AbstractEnergyMachineProperties;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import static com.github.sib_energy_craft.machines.block.entity.AbstractEnergyMachineBlockEntity.*;

/**
 * @since 0.0.4
 * @author sibmaks
 */
public abstract class AbstractEnergyMachineScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    protected final World world;

    protected AbstractEnergyMachineScreenHandler(@NotNull ScreenHandlerType<?> type,
                                                 int syncId,
                                                 @NotNull PlayerInventory playerInventory) {
        this(type, syncId, playerInventory, new SimpleInventory(3), new ArrayPropertyDelegate(4));
    }

    protected AbstractEnergyMachineScreenHandler(@NotNull ScreenHandlerType<?> type,
                                                 int syncId,
                                                 @NotNull PlayerInventory playerInventory,
                                                 @NotNull Inventory inventory,
                                                 @NotNull PropertyDelegate propertyDelegate) {
        super(type, syncId);
        int i;
        checkSize(inventory, 3);
        checkDataCount(propertyDelegate, 4);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.world = playerInventory.player.world;
        this.addSlot(new Slot(inventory, SOURCE_SLOT, 56, 17));
        var chargeSlot = new ChargeSlot(inventory, CHARGE_SLOT, 56, 53, false);
        this.addSlot(chargeSlot);
        var outputSlot = new OutputSlot(playerInventory.player, inventory, OUTPUT_SLOT, 116, 35);
        this.addSlot(outputSlot);
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
        this.addProperties(propertyDelegate);
    }

    @Override
    public boolean canUse(@NotNull PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    /**
     * Get charge progress status
     *
     * @return charge progress
     */
    public int getChargeProgress() {
        int i = getCharge();
        int j = getMaxCharge();
        if (j == 0 || i == 0) {
            return 0;
        }
        return i * 13 / j;
    }

    /**
     * Get cook progress status
     *
     * @return cook progress
     */
    public int getCookProgress() {
        int i = getCookingTime();
        int j = getCookingTimeTotal();
        if (j == 0 || i == 0) {
            return 0;
        }
        return i * 22 / j;
    }

    /**
     * Get extractor charge
     *
     * @return charge
     */
    public int getCharge() {
        return propertyDelegate.get(AbstractEnergyMachineProperties.CHARGE.ordinal());
    }

    /**
     * Get extractor max charge
     *
     * @return max charge
     */
    public int getMaxCharge() {
        return propertyDelegate.get(AbstractEnergyMachineProperties.MAX_CHARGE.ordinal());
    }

    /**
     * Get extractor cooking time
     *
     * @return cooking time
     */
    public int getCookingTime() {
        return propertyDelegate.get(AbstractEnergyMachineProperties.COOKING_TIME.ordinal());
    }

    /**
     * Get extractor total cooking time
     *
     * @return total cooking time
     */
    public int getCookingTimeTotal() {
        return propertyDelegate.get(AbstractEnergyMachineProperties.COOKING_TIME_TOTAL.ordinal());
    }
}

