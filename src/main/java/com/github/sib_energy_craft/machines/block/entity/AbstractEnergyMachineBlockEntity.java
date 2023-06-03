package com.github.sib_energy_craft.machines.block.entity;

import com.github.sib_energy_craft.containers.CleanEnergyContainer;
import com.github.sib_energy_craft.energy_api.Energy;
import com.github.sib_energy_craft.energy_api.EnergyOffer;
import com.github.sib_energy_craft.energy_api.consumer.EnergyConsumer;
import com.github.sib_energy_craft.energy_api.items.ChargeableItem;
import com.github.sib_energy_craft.energy_api.tags.CoreTags;
import com.github.sib_energy_craft.machines.CombinedInventory;
import com.github.sib_energy_craft.machines.block.AbstractEnergyMachineBlock;
import com.github.sib_energy_craft.machines.block.entity.property.EnergyMachinePropertyMap;
import com.github.sib_energy_craft.machines.block.entity.property.EnergyMachineTypedProperties;
import com.github.sib_energy_craft.machines.screen.AbstractEnergyMachineScreenHandler;
import com.github.sib_energy_craft.pipes.api.ItemConsumer;
import com.github.sib_energy_craft.pipes.api.ItemSupplier;
import com.github.sib_energy_craft.pipes.utils.PipeUtils;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @since 0.0.1
 * @author sibmaks
 */
public abstract class AbstractEnergyMachineBlockEntity<B extends AbstractEnergyMachineBlock>
        extends BlockEntity
        implements SidedInventory, ExtendedScreenHandlerFactory,
        EnergyConsumer,
        ItemConsumer, ItemSupplier {

    protected CleanEnergyContainer energyContainer;
    protected boolean working;

    protected final B block;
    protected final EnergyMachinePropertyMap energyMachinePropertyMap;
    protected final CombinedInventory<EnergyMachineInventoryType> inventory;
    private final EnumMap<EnergyMachineEvent, List<Runnable>> eventListeners;

    protected final int parallelProcess;
    protected final int sourceSlots;
    protected final int outputSlots;
    protected final int[] topSlots;
    protected final int[] sideSlots;
    protected final int[] bottomSlots;


    public AbstractEnergyMachineBlockEntity(@NotNull BlockEntityType<?> blockEntityType,
                                            @NotNull BlockPos blockPos,
                                            @NotNull BlockState blockState,
                                            @NotNull B block) {
        this(blockEntityType, blockPos, blockState, block, 1, 1, 1);
    }

    public AbstractEnergyMachineBlockEntity(@NotNull BlockEntityType<?> blockEntityType,
                                            @NotNull BlockPos blockPos,
                                            @NotNull BlockState blockState,
                                            @NotNull B block,
                                            int sourceSlots,
                                            int outputSlots,
                                            int parallelProcess) {
        super(blockEntityType, blockPos, blockState);
        this.block = block;

        var typedInventoryMap = new EnumMap<EnergyMachineInventoryType, Inventory>(EnergyMachineInventoryType.class);
        typedInventoryMap.put(EnergyMachineInventoryType.SOURCE, new SimpleInventory(sourceSlots));
        typedInventoryMap.put(EnergyMachineInventoryType.CHARGE, new SimpleInventory(1));
        typedInventoryMap.put(EnergyMachineInventoryType.OUTPUT, new SimpleInventory(outputSlots));
        this.inventory = new CombinedInventory<>(typedInventoryMap);

        this.sourceSlots = sourceSlots;
        this.outputSlots = outputSlots;
        this.parallelProcess = parallelProcess;

        this.topSlots = IntStream.range(0, sourceSlots).toArray();
        this.sideSlots = IntStream.concat(IntStream.of(sourceSlots), Arrays.stream(topSlots)).toArray();
        this.bottomSlots = IntStream.range(sourceSlots + 1, sourceSlots + 1 + outputSlots).toArray();

        this.energyContainer = new CleanEnergyContainer(Energy.ZERO, block.getMaxCharge());
        this.energyMachinePropertyMap = new EnergyMachinePropertyMap();
        this.energyMachinePropertyMap.add(EnergyMachineTypedProperties.CHARGE,
                () -> energyContainer.getCharge().intValue());
        this.energyMachinePropertyMap.add(EnergyMachineTypedProperties.MAX_CHARGE,
                () -> energyContainer.getMaxCharge().intValue());

        this.eventListeners = new EnumMap<>(EnergyMachineEvent.class);
    }

    @Override
    public void readNbt(@NotNull NbtCompound nbt) {
        super.readNbt(nbt);
        var inventoryCompound = nbt.getCompound("Inventory");
        this.inventory.readNbt(inventoryCompound);
        this.energyContainer = CleanEnergyContainer.readNbt(nbt);
    }

    @Override
    protected void writeNbt(@NotNull NbtCompound nbt) {
        super.writeNbt(nbt);
        var inventoryCompound = new NbtCompound();
        this.inventory.writeNbt(inventoryCompound);
        nbt.put("Inventory", inventoryCompound);
        this.energyContainer.writeNbt(nbt);
    }

    @Override
    public int[] getAvailableSlots(@NotNull Direction side) {
        if (side == Direction.DOWN) {
            return bottomSlots;
        }
        if (side == Direction.UP) {
            return topSlots;
        }
        return sideSlots;
    }

    @Override
    public boolean canInsert(int slot,
                             @NotNull ItemStack stack,
                             @Nullable Direction dir) {
        return this.isValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot,
                              @NotNull ItemStack stack,
                              @NotNull Direction dir) {
        var slotType = inventory.getType(slot);
        if (dir == Direction.DOWN && slotType == EnergyMachineInventoryType.CHARGE) {
            var item = stack.getItem();
            return item instanceof ChargeableItem chargeableItem && !chargeableItem.hasEnergy(stack);
        }
        return true;
    }

    @Override
    public int size() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.inventory.getStack(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return this.inventory.removeStack(slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return this.inventory.removeStack(slot);
    }

    @Override
    public void setStack(int slot, @NotNull ItemStack stack) {
        var world = this.world;
        if (world == null) {
            return;
        }
        var inventoryStack = this.inventory.getStack(slot);
        var sourceInventory = this.inventory.getInventory(EnergyMachineInventoryType.SOURCE);
        var wasSourceEmpty = sourceInventory != null && sourceInventory.isEmpty();

        var sameItem = inventoryStack.isEmpty() ||
                !stack.isEmpty() && stack.isItemEqual(inventoryStack) && ItemStack.areNbtEqual(stack, inventoryStack);
        this.inventory.setStack(slot, stack);
        int maxCountPerStack = this.getMaxCountPerStack();
        if (stack.getCount() > maxCountPerStack) {
            stack.setCount(maxCountPerStack);
        }
        var slotType = inventory.getType(slot);
        if (slotType == EnergyMachineInventoryType.SOURCE) {
            onSourceSet(world, wasSourceEmpty, sameItem);
            markDirty();
        }
    }

    /**
     * Called each time when source slot item type changed
     *
     * @param world game world
     * @param wasSourceEmpty  was source slot empty
     * @param itemTimeChanged was source item replaced
     */
    protected void onSourceSet(@NotNull World world, boolean wasSourceEmpty, boolean itemTimeChanged) {
        markDirty();
    }

    @Override
    public boolean canPlayerUse(@NotNull PlayerEntity player) {
        var world = this.world;
        if (world == null || world.getBlockEntity(this.pos) != this) {
            return false;
        }
        return player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public boolean isValid(int slot,
                           @NotNull ItemStack stack) {
        var slotType = inventory.getType(slot);
        if (slotType == EnergyMachineInventoryType.OUTPUT) {
            return false;
        }
        if(slotType == EnergyMachineInventoryType.CHARGE) {
            var item = stack.getItem();
            if(item instanceof ChargeableItem chargeableItem) {
                return chargeableItem.hasEnergy(stack);
            }
            return false;
        }
        return true;
    }

    @Override
    public void clear() {
        this.inventory.clear();
    }

    @Override
    public boolean isConsumeFrom(@NotNull Direction direction) {
        return true;
    }

    @Override
    public void receiveOffer(@NotNull EnergyOffer energyOffer) {
        final var energyLevel = block.getEnergyLevel();
        if (energyOffer.getEnergyAmount().compareTo(energyLevel.toBig) > 0) {
            if (energyOffer.acceptOffer()) {
                if (world instanceof ServerWorld serverWorld) {
                    serverWorld.breakBlock(pos, false);
                    return;
                }
            }
        }
        energyContainer.receiveOffer(energyOffer);
        markDirty();
    }

    /**
     * Method called when block of this entity is placed in the world.<br/>
     * As argument method accept charge of item, that used as basic block entity charge.
     *
     * @param charge item charge
     */
    public void onPlaced(int charge) {
        this.energyContainer.add(charge);
    }

    /**
     * Add event listeners
     *
     * @param event event type
     * @param listener event listener
     */
    public synchronized void addListener(@NotNull EnergyMachineEvent event,
                            @NotNull Runnable listener) {
        var listeners = this.eventListeners.computeIfAbsent(event, it -> new ArrayList<>());
        listeners.add(listener);
    }

    /**
     * Remove event listeners
     *
     * @param event event type
     * @param listener event listener
     */
    public synchronized void removeListener(@NotNull EnergyMachineEvent event,
                               @NotNull Runnable listener) {
        var listeners = this.eventListeners.computeIfAbsent(event, it -> new ArrayList<>());
        listeners.remove(listener);
    }

    /**
     * Dispatch event
     *
     * @param event event type
     */
    protected synchronized void dispatch(@NotNull EnergyMachineEvent event) {
        var listeners = this.eventListeners.get(event);
        if(listeners != null) {
            for (var listener : listeners) {
                listener.run();
            }
        }
    }

    /**
     * Method return amount of energy that used every cooking tick
     * @return amount of energy
     */
    public @NotNull Energy getEnergyUsagePerTick() {
        return Energy.ONE;
    }

    /**
     * Charge machine by item in charging slot
     *
     * @return true - machine was charged, false - otherwise
     */
    protected boolean charge() {
        var chargeStack = inventory.getStack(EnergyMachineInventoryType.CHARGE, 0);
        var chargeItem = chargeStack.getItem();
        if (chargeStack.isEmpty() || (!(chargeItem instanceof ChargeableItem chargeableItem))) {
            return false;
        }
        int charge = chargeableItem.getCharge(chargeStack);
        if (charge > 0) {
            int transferred = Math.min(
                    block.getEnergyLevel().to,
                    Math.min(
                            charge,
                            energyContainer.getFreeSpace().intValue()
                    )
            );
            chargeableItem.discharge(chargeStack, transferred);
            energyContainer.add(transferred);
            return true;
        }
        return false;
    }

    /**
     * Can machine processing specific process
     *
     * @param process process index
     * @param world game world
     * @param pos machine position
     * @param state machine state
     * @param processContext process context
     *
     * @return true - can process, false - otherwise
     * @since 0.0.36
     */
    abstract protected boolean canProcess(int process,
                                          @NotNull World world,
                                          @NotNull BlockPos pos,
                                          @NotNull BlockState state,
                                          @NotNull Map<String, Object> processContext);

    /**
     * Tick machine process logic.<br/>
     * It can affect as common machine state (not process related) as process related.<br/>
     * But process id will be passed in both cases.
     *
     * @param process process index
     * @param world game world
     * @param pos machine position
     * @param state machine state
     * @param processContext process context
     *
     * @return true - process cycle complete, false - otherwise
     * @since 0.0.36
     */
    abstract protected boolean tickProcess(int process,
                                           @NotNull World world,
                                           @NotNull BlockPos pos,
                                           @NotNull BlockState state,
                                           @NotNull Map<String, Object> processContext);

    /**
     * Process machine logic
     *
     * @param process process index
     * @param world game world
     * @param pos machine position
     * @param state machine state
     * @param processContext process context
     * @since 0.0.36
     *
     */
    abstract protected void onProcessFinished(int process,
                                              @NotNull World world,
                                              @NotNull BlockPos pos,
                                              @NotNull BlockState state,
                                              @NotNull Map<String, Object> processContext);

    /**
     * Method return mark is machine consume one energy per each tick or per each process
     *
     * @return true - machine consume energy on each process, false - machine consume energy on each cycle
     * @since 0.0.36
     */
    protected boolean isEachProcessUseEnergy() {
        return false;
    }

    /**
     * Default implementation of machine cooking tick
     *
     * @param world game world
     * @param pos block position
     * @param state block state
     * @param blockEntity block entity
     * @since 0.0.36
     */
    public static void simpleProcessingTick(
            @NotNull World world,
            @NotNull BlockPos pos,
            @NotNull BlockState state,
            @NotNull AbstractEnergyMachineBlockEntity<?> blockEntity) {
        if (world.isClient) {
            return;
        }
        var requiredEnergy = blockEntity.getEnergyUsagePerTick();
        var hasEnergy = blockEntity.energyContainer.hasAtLeast(requiredEnergy);
        var changed = false;
        var working = blockEntity.working;
        blockEntity.working = false;

        if(blockEntity.charge()) {
            changed = true;
        }

        if (!blockEntity.energyContainer.hasAtLeast(requiredEnergy)) {
            blockEntity.updateState(working, state, world, pos, changed);
            blockEntity.dispatch(EnergyMachineEvent.ENERGY_NOT_ENOUGH);
            return;
        }
        boolean eachProcessUseEnergy = blockEntity.isEachProcessUseEnergy();
        if(eachProcessUseEnergy) {
            changed = processEachProcessUseEnergy(world, pos, state, blockEntity);
        } else {
            changed = processUseEnergyOnce(world, pos, state, blockEntity);
        }

        boolean energyChanged = hasEnergy != blockEntity.energyContainer.hasAtLeast(requiredEnergy);
        blockEntity.updateState(working, state, world, pos, energyChanged || changed);
    }

    private static boolean processEachProcessUseEnergy(
            @NotNull World world,
            @NotNull BlockPos pos,
            @NotNull BlockState state,
            @NotNull AbstractEnergyMachineBlockEntity<?> blockEntity) {
        var requiredEnergy = blockEntity.getEnergyUsagePerTick();
        boolean canProcess = false;
        boolean anyProcessFinished = false;
        for (int process = 0; process < blockEntity.parallelProcess; process++) {
            var context = new HashMap<String, Object>();
            canProcess = blockEntity.canProcess(process, world, pos, state, context);
            if (!canProcess) {
                continue;
            }
            if (!blockEntity.energyContainer.subtract(requiredEnergy)) {
                blockEntity.dispatch(EnergyMachineEvent.ENERGY_NOT_ENOUGH);
                break;
            }
            var processFinished = blockEntity.tickProcess(process, world, pos, state, context);
            anyProcessFinished |= processFinished;
            blockEntity.dispatch(EnergyMachineEvent.ENERGY_USED);
            if (processFinished) {
                blockEntity.onProcessFinished(process, world, pos, state, context);
                blockEntity.dispatch(EnergyMachineEvent.PROCESSED);
            }
        }
        if (!canProcess) {
            blockEntity.dispatch(EnergyMachineEvent.CAN_NOT_PROCESS);
        }
        return canProcess | anyProcessFinished;
    }

    private static boolean processUseEnergyOnce(
            @NotNull World world,
            @NotNull BlockPos pos,
            @NotNull BlockState state,
            @NotNull AbstractEnergyMachineBlockEntity<?> blockEntity) {
        var requiredEnergy = blockEntity.getEnergyUsagePerTick();
        boolean energyUsed = false;
        boolean canMachineProcess = false;
        boolean processFinished = false;
        for (int process = 0; process < blockEntity.parallelProcess; process++) {
            var context = new HashMap<String, Object>();
            var canProcess = blockEntity.canProcess(process, world, pos, state, context);
            if (!canProcess) {
                continue;
            }
            canMachineProcess = true;
            if (!energyUsed) {
                if (!blockEntity.energyContainer.subtract(requiredEnergy)) {
                    blockEntity.dispatch(EnergyMachineEvent.ENERGY_NOT_ENOUGH);
                    break;
                }
                energyUsed = true;
                blockEntity.dispatch(EnergyMachineEvent.ENERGY_USED);
                processFinished = blockEntity.tickProcess(process, world, pos, state, context);
                if (processFinished) {
                    blockEntity.dispatch(EnergyMachineEvent.PROCESSED);
                }
            }
            if (processFinished) {
                blockEntity.onProcessFinished(process, world, pos, state, context);
            }
        }
        if (!canMachineProcess) {
            blockEntity.dispatch(EnergyMachineEvent.CAN_NOT_PROCESS);
        }
        return energyUsed | canMachineProcess | processFinished;
    }

    /**
     * Update block state
     *
     * @param wasWork was machine in working state before tick
     * @param state machine block state
     * @param world game world
     * @param pos machine block position
     * @param markDirty need mark machine state as dirty
     */
    protected void updateState(boolean wasWork,
                               @NotNull BlockState state,
                               @NotNull World world,
                               @NotNull BlockPos pos,
                               boolean markDirty) {
        if (wasWork != working) {
            state = state.with(AbstractEnergyMachineBlock.WORKING, working);
            world.setBlockState(pos, state, Block.NOTIFY_ALL);
            markDirty = true;
        }
        if (markDirty) {
            markDirty(world, pos, state);
        }
    }

    @Override
    public boolean canConsume(@NotNull ItemStack itemStack, @NotNull Direction direction) {
        if(CoreTags.isChargeable(itemStack)) {
            var chargeStack = inventory.getStack(EnergyMachineInventoryType.CHARGE, 0);
            return chargeStack.isEmpty() || PipeUtils.canMergeItems(chargeStack, itemStack);
        }
        var sourceInventory = inventory.getInventory(EnergyMachineInventoryType.SOURCE);
        if(sourceInventory == null) {
            return false;
        }
        for (int slot = 0; slot < sourceInventory.size(); slot++) {
            var inputStack = sourceInventory.getStack(slot);
            if(inputStack.isEmpty() || PipeUtils.canMergeItems(inputStack, itemStack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull ItemStack consume(@NotNull ItemStack itemStack, @NotNull Direction direction) {
        if(!canConsume(itemStack, direction)) {
            return itemStack;
        }
        markDirty();
        if (!CoreTags.isChargeable(itemStack)) {
            return inventory.addStack(EnergyMachineInventoryType.SOURCE, itemStack);
        }
        var chargeStack = inventory.getStack(EnergyMachineInventoryType.CHARGE, 0);
        if(chargeStack.isEmpty()) {
            inventory.setStack(EnergyMachineInventoryType.CHARGE, 0, itemStack);
            return ItemStack.EMPTY;
        }
        return PipeUtils.mergeItems(chargeStack, itemStack);
    }

    @Override
    public @NotNull List<ItemStack> canSupply(@NotNull Direction direction) {
        var outputInventory = inventory.getInventory(EnergyMachineInventoryType.OUTPUT);
        if(outputInventory == null) {
            return Collections.emptyList();
        }
        return IntStream.range(0, outputInventory.size())
                .mapToObj(outputInventory::getStack)
                .filter(it -> !it.isEmpty())
                .map(ItemStack::copy)
                .collect(Collectors.toList());
    }

    @Override
    public boolean supply(@NotNull ItemStack requested, @NotNull Direction direction) {
        if (!inventory.canRemoveItem(EnergyMachineInventoryType.OUTPUT, requested.getItem(), requested.getCount())) {
            return false;
        }
        var removed = inventory.removeItem(EnergyMachineInventoryType.OUTPUT, requested.getItem(),
                requested.getCount());
        markDirty();
        return removed.getCount() == requested.getCount();
    }

    @Override
    public void returnStack(@NotNull ItemStack requested, @NotNull Direction direction) {
        inventory.addStack(EnergyMachineInventoryType.OUTPUT, requested);
        markDirty();
    }

    /**
     * Get current energy machine charge
     *
     * @return machine charge
     */
    public int getCharge() {
        return energyContainer.getCharge().intValue();
    }

    /**
     * Create energy machine screen handler
     *
     * @param syncId sync id
     * @param playerInventory player inventory
     * @param player player
     * @return instance of energy machine screen handler
     */
    abstract protected AbstractEnergyMachineScreenHandler createScreenHandler(int syncId,
                                                                              @NotNull PlayerInventory playerInventory,
                                                                              @NotNull PlayerEntity player);

    @Nullable
    @Override
    public final ScreenHandler createMenu(int syncId,
                                          @NotNull PlayerInventory playerInventory,
                                          @NotNull PlayerEntity player) {
        var screenHandler = createScreenHandler(syncId, playerInventory, player);
        var world = player.getWorld();
        if(!world.isClient && player instanceof ServerPlayerEntity serverPlayerEntity) {
            var syncer = energyMachinePropertyMap.createSyncer(syncId, serverPlayerEntity);
            screenHandler.setPropertySyncer(syncer);
        }
        return screenHandler;
    }

    @Override
    public void writeScreenOpeningData(@NotNull ServerPlayerEntity player,
                                       @NotNull PacketByteBuf buf) {
    }

    /** Called when block state replaced
     *
     * @param state old state
     * @param serverWorld server world
     * @param pos machine position
     * @param newState new state
     * @param moved moved
     */
    public void onStateReplaced(@NotNull BlockState state,
                                @NotNull ServerWorld serverWorld,
                                @NotNull BlockPos pos,
                                @NotNull BlockState newState,
                                boolean moved) {
        ItemScatterer.spawn(world, pos, this);
    }
}
