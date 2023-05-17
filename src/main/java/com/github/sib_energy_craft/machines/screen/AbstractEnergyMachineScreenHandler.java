package com.github.sib_energy_craft.machines.screen;

import com.github.sib_energy_craft.energy_api.screen.ChargeSlot;
import com.github.sib_energy_craft.energy_api.tags.CoreTags;
import com.github.sib_energy_craft.machines.block.entity.EnergyMachineProperty;
import com.github.sib_energy_craft.sec_utils.screen.SlotsScreenHandler;
import com.github.sib_energy_craft.sec_utils.screen.slot.SlotGroupMetaBuilder;
import com.github.sib_energy_craft.sec_utils.screen.slot.SlotGroupsMeta;
import com.github.sib_energy_craft.sec_utils.screen.slot.SlotGroupsMetaBuilder;
import com.github.sib_energy_craft.sec_utils.screen.slot.SlotTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * @since 0.0.4
 * @author sibmaks
 */
public abstract class AbstractEnergyMachineScreenHandler extends SlotsScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    protected final SlotGroupsMeta slotGroupsMeta;
    protected final World world;
    protected final int slotCount;

    protected AbstractEnergyMachineScreenHandler(@NotNull ScreenHandlerType<?> type,
                                                 int syncId,
                                                 @NotNull PlayerInventory playerInventory,
                                                 @NotNull VisualSettings visualSettings) {
        this(type, syncId, playerInventory, 1, visualSettings);
    }

    protected AbstractEnergyMachineScreenHandler(@NotNull ScreenHandlerType<?> type,
                                                 int syncId,
                                                 @NotNull PlayerInventory playerInventory,
                                                 int slotCount,
                                                 @NotNull VisualSettings visualSettings) {
        this(type, syncId, playerInventory, new SimpleInventory(1 + slotCount * 2), new ArrayPropertyDelegate(4),
                slotCount, visualSettings);
    }


    protected AbstractEnergyMachineScreenHandler(@NotNull ScreenHandlerType<?> type,
                                                 int syncId,
                                                 @NotNull PlayerInventory playerInventory,
                                                 @NotNull Inventory inventory,
                                                 @NotNull PropertyDelegate propertyDelegate,
                                                 @NotNull VisualSettings visualSettings) {
        this(type, syncId, playerInventory, inventory, propertyDelegate, 1, visualSettings);
    }

    protected AbstractEnergyMachineScreenHandler(@NotNull ScreenHandlerType<?> type,
                                                 int syncId,
                                                 @NotNull PlayerInventory playerInventory,
                                                 @NotNull Inventory inventory,
                                                 @NotNull PropertyDelegate propertyDelegate,
                                                 int slotCount,
                                                 @NotNull VisualSettings visualSettings) {
        super(type, syncId);
        checkSize(inventory, 1 + slotCount * 2);
        checkDataCount(propertyDelegate, 4);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.world = playerInventory.player.world;
        this.slotGroupsMeta = buildSlots(visualSettings, slotCount, playerInventory, inventory);
        this.slotCount = slotCount;
        this.addProperties(propertyDelegate);
    }

    private @NotNull SlotGroupsMeta buildSlots(@NotNull VisualSettings vs,
                                               int slots,
                                               @NotNull PlayerInventory playerInventory,
                                               @NotNull Inventory inventory) {
        int globalSlotIndex = 0;
        var slotGroupsBuilder = SlotGroupsMetaBuilder.builder();

        int quickAccessSlots = 9;
        {
            var slotQuickAccessGroupBuilder = SlotGroupMetaBuilder.builder(SlotTypes.QUICK_ACCESS);
            for (int i = 0; i < quickAccessSlots; ++i) {
                slotQuickAccessGroupBuilder.addSlot(globalSlotIndex++, i);
                var slot = new Slot(playerInventory, i, vs.quickAccessX() + i * 18, vs.quickAccessY());
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
                    var slot = new Slot(playerInventory, index, vs.playerInventoryX() + j * 18, vs.playerInventoryY() + i * 18);
                    this.addSlot(slot);
                }
            }
            var playerSlotGroup = slotPlayerGroupBuilder.build();
            slotGroupsBuilder.add(playerSlotGroup);
        }

        {
            var slotGroupBuilder = SlotGroupMetaBuilder.builder(EnergyMachineSlotTypes.SOURCE);
            for (int i = 0; i < slots; ++i) {
                slotGroupBuilder.addSlot(globalSlotIndex++, i);
                var slot = new Slot(inventory, i, vs.sourceSlotsX() + i * 18, vs.sourceSlotsY());
                this.addSlot(slot);
            }
            var slotGroup = slotGroupBuilder.build();
            slotGroupsBuilder.add(slotGroup);
        }

        {
            var slotGroupBuilder = SlotGroupMetaBuilder.builder(EnergyMachineSlotTypes.CHARGE);
            slotGroupBuilder.addSlot(globalSlotIndex++, slots);
            var chargeSlot = new ChargeSlot(inventory, slots, vs.chargeSlotX(), vs.chargeSlotY(), false);
            this.addSlot(chargeSlot);
            var slotGroup = slotGroupBuilder.build();
            slotGroupsBuilder.add(slotGroup);
        }

        {
            var slotGroupBuilder = SlotGroupMetaBuilder.builder(EnergyMachineSlotTypes.OUTPUT);
            for (int i = 0; i < slots; ++i) {
                slotGroupBuilder.addSlot(globalSlotIndex++, i);
                var slot = new Slot(inventory, slots + 1 + i, vs.outputSlotsX() + i * 18, vs.outputSlotsY());
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
                if (slotType == EnergyMachineSlotTypes.SOURCE || slotType == EnergyMachineSlotTypes.CHARGE ||
                        slotType == EnergyMachineSlotTypes.OUTPUT) {
                    if (!insertItem(slotGroupsMeta, slotStack, SlotTypes.QUICK_ACCESS, SlotTypes.PLAYER_INVENTORY)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if(isUsedInMachine(slotStack)) {
                        if (!insertItem(slotGroupsMeta, slotStack, EnergyMachineSlotTypes.SOURCE)) {
                            return ItemStack.EMPTY;
                        }
                    }
                    if(CoreTags.isChargeable(slotStack)) {
                        if (!insertItem(slotGroupsMeta, slotStack, EnergyMachineSlotTypes.CHARGE)) {
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

    abstract protected boolean isUsedInMachine(@NotNull ItemStack itemStack);

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
    public int getCookProgress(int width) {
        int i = getCookingTime();
        int j = getCookingTimeTotal();
        if (j == 0 || i == 0) {
            return 0;
        }
        return i * width / j;
    }

    /**
     * Get extractor charge
     *
     * @return charge
     */
    public int getCharge() {
        return propertyDelegate.get(EnergyMachineProperty.CHARGE.ordinal());
    }

    /**
     * Get extractor max charge
     *
     * @return max charge
     */
    public int getMaxCharge() {
        return propertyDelegate.get(EnergyMachineProperty.MAX_CHARGE.ordinal());
    }

    /**
     * Get extractor cooking time
     *
     * @return cooking time
     */
    public int getCookingTime() {
        return propertyDelegate.get(EnergyMachineProperty.COOKING_TIME.ordinal());
    }

    /**
     * Get extractor total cooking time
     *
     * @return total cooking time
     */
    public int getCookingTimeTotal() {
        return propertyDelegate.get(EnergyMachineProperty.COOKING_TIME_TOTAL.ordinal());
    }
}

