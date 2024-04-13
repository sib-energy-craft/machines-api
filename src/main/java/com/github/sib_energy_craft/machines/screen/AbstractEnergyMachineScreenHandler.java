package com.github.sib_energy_craft.machines.screen;

import com.github.sib_energy_craft.energy_api.screen.ChargeSlot;
import com.github.sib_energy_craft.energy_api.tags.CoreTags;
import com.github.sib_energy_craft.machines.block.entity.EnergyMachineInventoryType;
import com.github.sib_energy_craft.machines.screen.layout.SlotLayoutManager;
import com.github.sib_energy_craft.machines.screen.slot.OutputSlot;
import com.github.sib_energy_craft.machines.screen.slot.SourceSlot;
import com.github.sib_energy_craft.network.PropertyUpdateSyncer;
import com.github.sib_energy_craft.screen.TypedPropertyScreenHandler;
import com.github.sib_energy_craft.sec_utils.screen.SlotsScreenHandler;
import com.github.sib_energy_craft.sec_utils.screen.slot.SlotGroupMetaBuilder;
import com.github.sib_energy_craft.sec_utils.screen.slot.SlotGroupsMeta;
import com.github.sib_energy_craft.sec_utils.screen.slot.SlotGroupsMetaBuilder;
import com.github.sib_energy_craft.sec_utils.screen.slot.SlotTypes;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * @since 0.0.4
 * @author sibmaks
 */
public abstract class AbstractEnergyMachineScreenHandler<S extends EnergyMachineState> extends SlotsScreenHandler
        implements TypedPropertyScreenHandler {
    protected final Inventory inventory;
    protected final SlotGroupsMeta slotGroupsMeta;
    protected final World world;
    protected final int sourceSlots;
    protected final int outputSlots;
    @Getter
    protected final S energyMachineState;
    @Setter
    private PropertyUpdateSyncer propertySyncer;

    protected AbstractEnergyMachineScreenHandler(@NotNull ScreenHandlerType<?> type,
                                                 int syncId,
                                                 @NotNull PlayerInventory playerInventory,
                                                 S energyMachineState,
                                                 @NotNull SlotLayoutManager slotLayoutManager) {
        this(type, syncId, playerInventory, 1, 1, energyMachineState, slotLayoutManager);
    }

    protected AbstractEnergyMachineScreenHandler(@NotNull ScreenHandlerType<?> type,
                                                 int syncId,
                                                 @NotNull PlayerInventory playerInventory,
                                                 int sourceSlots,
                                                 int outputSlots,
                                                 S energyMachineState,
                                                 @NotNull SlotLayoutManager slotLayoutManager) {
        this(type,
                syncId,
                playerInventory,
                new SimpleInventory(1 + sourceSlots + outputSlots),
                sourceSlots,
                outputSlots,
                energyMachineState,
                slotLayoutManager
        );
    }


    protected AbstractEnergyMachineScreenHandler(@NotNull ScreenHandlerType<?> type,
                                                 int syncId,
                                                 @NotNull PlayerInventory playerInventory,
                                                 @NotNull Inventory inventory,
                                                 S energyMachineState,
                                                 @NotNull SlotLayoutManager slotLayoutManager) {
        this(type, syncId, playerInventory, inventory, 1, 1, energyMachineState, slotLayoutManager);
    }

    protected AbstractEnergyMachineScreenHandler(@NotNull ScreenHandlerType<?> type,
                                                 int syncId,
                                                 @NotNull PlayerInventory playerInventory,
                                                 @NotNull Inventory inventory,
                                                 int sourceSlots,
                                                 int outputSlots,
                                                 S energyMachineState,
                                                 @NotNull SlotLayoutManager slotLayoutManager) {
        super(type, syncId);
        checkSize(inventory, 1 + sourceSlots + outputSlots);
        this.inventory = inventory;
        this.world = playerInventory.player.getWorld();
        this.sourceSlots = sourceSlots;
        this.outputSlots = outputSlots;
        this.energyMachineState = energyMachineState;
        this.slotGroupsMeta = buildSlots(slotLayoutManager, sourceSlots, outputSlots, playerInventory, inventory);
    }

    private @NotNull SlotGroupsMeta buildSlots(@NotNull SlotLayoutManager slotLayoutManager,
                                               int sourceSlots,
                                               int outputSlots,
                                               @NotNull PlayerInventory playerInventory,
                                               @NotNull Inventory inventory) {
        int globalSlotIndex = 0;
        var slotGroupsBuilder = SlotGroupsMetaBuilder.builder();

        int quickAccessSlots = 9;
        {
            var slotQuickAccessGroupBuilder = SlotGroupMetaBuilder.builder(SlotTypes.QUICK_ACCESS);
            for (int i = 0; i < quickAccessSlots; ++i) {
                slotQuickAccessGroupBuilder.addSlot(globalSlotIndex++, i);
                var pos = slotLayoutManager.getSlotPosition(SlotTypes.QUICK_ACCESS, i, i);
                var slot = new Slot(playerInventory, i, pos.x, pos.y);
                this.addSlot(slot);
            }
            var quickAccessSlotGroup = slotQuickAccessGroupBuilder.build();
            slotGroupsBuilder.add(quickAccessSlotGroup);
        }

        {
            var slotPlayerGroupBuilder = SlotGroupMetaBuilder.builder(SlotTypes.PLAYER_INVENTORY);
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < 9; ++j) {
                    int index = j + i * 9 + quickAccessSlots;
                    slotPlayerGroupBuilder.addSlot(globalSlotIndex++, index);
                    var pos = slotLayoutManager.getSlotPosition(SlotTypes.PLAYER_INVENTORY, j + i * 9, index);
                    var slot = new Slot(playerInventory, index, pos.x, pos.y);
                    this.addSlot(slot);
                }
            }
            var playerSlotGroup = slotPlayerGroupBuilder.build();
            slotGroupsBuilder.add(playerSlotGroup);
        }

        {
            var slotGroupBuilder = SlotGroupMetaBuilder.builder(EnergyMachineInventoryType.SOURCE);
            for (int i = 0; i < sourceSlots; ++i) {
                slotGroupBuilder.addSlot(globalSlotIndex++, i);
                var pos = slotLayoutManager.getSlotPosition(EnergyMachineInventoryType.SOURCE, i, i);
                var slot = new SourceSlot(inventory, i, pos.x, pos.y, this::isUsedInMachine);
                this.addSlot(slot);
            }
            var slotGroup = slotGroupBuilder.build();
            slotGroupsBuilder.add(slotGroup);
        }

        {
            var slotGroupBuilder = SlotGroupMetaBuilder.builder(EnergyMachineInventoryType.CHARGE);
            slotGroupBuilder.addSlot(globalSlotIndex++, sourceSlots);
            var pos = slotLayoutManager.getSlotPosition(EnergyMachineInventoryType.CHARGE, 0, sourceSlots);
            var chargeSlot = new ChargeSlot(inventory, sourceSlots, pos.x, pos.y, false);
            this.addSlot(chargeSlot);
            var slotGroup = slotGroupBuilder.build();
            slotGroupsBuilder.add(slotGroup);
        }

        {
            var slotGroupBuilder = SlotGroupMetaBuilder.builder(EnergyMachineInventoryType.OUTPUT);
            for (int i = 0; i < outputSlots; ++i) {
                int index = sourceSlots + 1 + i;
                slotGroupBuilder.addSlot(globalSlotIndex++, index);
                var pos = slotLayoutManager.getSlotPosition(EnergyMachineInventoryType.OUTPUT, i, index);
                var slot = new OutputSlot(playerInventory.player, inventory, index, pos.x, pos.y);
                this.addSlot(slot);
            }

            var slotGroup = slotGroupBuilder.build();
            slotGroupsBuilder.add(slotGroup);
        }

        return slotGroupsBuilder.build();
    }

    @NotNull
    @Override
    public ItemStack quickMove(@NotNull PlayerEntity player, int index) {
        var itemStack = ItemStack.EMPTY;
        var slot = this.slots.get(index);
        if (slot.hasStack()) {
            var slotStack = slot.getStack();
            itemStack = slotStack.copy();

            var slotMeta = this.slotGroupsMeta.getByGlobalSlotIndex(index);
            if(slotMeta != null) {
                var slotType = slotMeta.getSlotType();
                if (slotType == EnergyMachineInventoryType.SOURCE || slotType == EnergyMachineInventoryType.CHARGE ||
                        slotType == EnergyMachineInventoryType.OUTPUT) {
                    if (!insertItem(slotGroupsMeta, slotStack, SlotTypes.PLAYER_INVENTORY, SlotTypes.QUICK_ACCESS)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if(isUsedInMachine(slotStack)) {
                        if (!insertItem(slotGroupsMeta, slotStack, EnergyMachineInventoryType.SOURCE)) {
                            return ItemStack.EMPTY;
                        }
                    }
                    if(CoreTags.isChargeable(slotStack)) {
                        if (!insertItem(slotGroupsMeta, slotStack, EnergyMachineInventoryType.CHARGE)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
                if (slotType == SlotTypes.QUICK_ACCESS) {
                    if (!insertItem(slotGroupsMeta, slotStack, SlotTypes.PLAYER_INVENTORY)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotType == SlotTypes.PLAYER_INVENTORY) {
                    if (!insertItem(slotGroupsMeta, slotStack, SlotTypes.QUICK_ACCESS)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
            slot.onQuickTransfer(slotStack, itemStack);

            if (slotStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
            if (slotStack.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTakeItem(player, slotStack);
        }
        return itemStack;
    }

    protected abstract boolean isUsedInMachine(@NotNull ItemStack itemStack);

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
        int i = energyMachineState.getCharge();
        int j = energyMachineState.getMaxCharge();
        if (j == 0 || i == 0) {
            return 0;
        }
        return i * 13 / j;
    }

    /**
     * Get extractor charge
     *
     * @return charge
     */
    public int getCharge() {
        return energyMachineState.getCharge();
    }

    /**
     * Get extractor max charge
     *
     * @return max charge
     */
    public int getMaxCharge() {
        return energyMachineState.getMaxCharge();
    }

    @Override
    public <V> void onTypedPropertyChanged(int index, V value) {
        energyMachineState.changeProperty(index, value);
    }

    @Override
    public void sendContentUpdates() {
        super.sendContentUpdates();
        var syncer = this.propertySyncer;
        if (syncer != null) {
            syncer.run();
        }

    }
}

