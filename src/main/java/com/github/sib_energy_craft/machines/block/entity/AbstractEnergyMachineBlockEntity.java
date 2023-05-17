package com.github.sib_energy_craft.machines.block.entity;

import com.github.sib_energy_craft.containers.CleanEnergyContainer;
import com.github.sib_energy_craft.energy_api.Energy;
import com.github.sib_energy_craft.energy_api.EnergyOffer;
import com.github.sib_energy_craft.energy_api.consumer.EnergyConsumer;
import com.github.sib_energy_craft.energy_api.items.ChargeableItem;
import com.github.sib_energy_craft.energy_api.tags.CoreTags;
import com.github.sib_energy_craft.machines.CombinedInventory;
import com.github.sib_energy_craft.machines.block.AbstractEnergyMachineBlock;
import com.github.sib_energy_craft.machines.utils.EnergyMachineUtils;
import com.github.sib_energy_craft.machines.utils.ExperienceUtils;
import com.github.sib_energy_craft.pipes.api.ItemConsumer;
import com.github.sib_energy_craft.pipes.api.ItemSupplier;
import com.github.sib_energy_craft.pipes.utils.PipeUtils;
import com.github.sib_energy_craft.sec_utils.screen.PropertyMap;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.github.sib_energy_craft.machines.block.entity.EnergyMachineProperty.*;

/**
 * @since 0.0.1
 * @author sibmaks
 */
public abstract class AbstractEnergyMachineBlockEntity extends LockableContainerBlockEntity
        implements SidedInventory, RecipeUnlocker, RecipeInputProvider, EnergyConsumer, ItemConsumer, ItemSupplier {
    private static final Energy ENERGY_ONE = Energy.of(1);

    protected int cookTime;
    protected int cookTimeTotal;
    protected CleanEnergyContainer energyContainer;
    protected boolean working;

    protected final AbstractEnergyMachineBlock block;
    protected final PropertyMap<EnergyMachineProperty> propertyMap;
    protected final Object2IntOpenHashMap<Identifier> recipesUsed;
    protected final CombinedInventory<EnergyMachineInventoryType> inventory;
    private final EnumMap<EnergyMachineEvent, List<Runnable>> eventListeners;
    protected final int slots;
    protected final int[] topSlots;
    protected final int[] sideSlots;
    protected final int[] bottomSlots;


    public AbstractEnergyMachineBlockEntity(@NotNull BlockEntityType<?> blockEntityType,
                                            @NotNull BlockPos blockPos,
                                            @NotNull BlockState blockState,
                                            @NotNull AbstractEnergyMachineBlock block) {
        this(blockEntityType, blockPos, blockState, block, 1);
    }

    public AbstractEnergyMachineBlockEntity(@NotNull BlockEntityType<?> blockEntityType,
                                            @NotNull BlockPos blockPos,
                                            @NotNull BlockState blockState,
                                            @NotNull AbstractEnergyMachineBlock block,
                                            int slots) {
        super(blockEntityType, blockPos, blockState);
        this.block = block;
        this.recipesUsed = new Object2IntOpenHashMap<>();

        var enumMap = new EnumMap<EnergyMachineInventoryType, Inventory>(EnergyMachineInventoryType.class);
        enumMap.put(EnergyMachineInventoryType.SOURCE, new SimpleInventory(slots));
        enumMap.put(EnergyMachineInventoryType.CHARGE, new SimpleInventory(1));
        enumMap.put(EnergyMachineInventoryType.OUTPUT, new SimpleInventory(slots));
        this.inventory = new CombinedInventory<>(enumMap);

        this.slots = slots;
        var sourceSlots = IntStream.range(0, slots).boxed().toList();
        var outputSlots = IntStream.range(slots + 1, slots + 1 + slots).boxed().toList();
        this.topSlots = sourceSlots.stream().mapToInt(it -> it).toArray();
        this.sideSlots = Stream.concat(Stream.of(slots), sourceSlots.stream()).mapToInt(it -> it).toArray();
        this.bottomSlots = outputSlots.stream().mapToInt(it -> it).toArray();

        this.energyContainer = new CleanEnergyContainer(Energy.ZERO, block.getMaxCharge());
        this.propertyMap = new PropertyMap<>(EnergyMachineProperty.class);
        this.propertyMap.add(COOKING_TIME, () -> cookTime);
        this.propertyMap.add(COOKING_TIME_TOTAL, () -> cookTimeTotal);
        this.propertyMap.add(CHARGE, () -> this.energyContainer.getCharge().intValue());
        this.propertyMap.add(MAX_CHARGE, () -> this.energyContainer.getMaxCharge().intValue());
        this.eventListeners = new EnumMap<EnergyMachineEvent, List<Runnable>>(EnergyMachineEvent.class);
    }

    @Override
    public void readNbt(@NotNull NbtCompound nbt) {
        super.readNbt(nbt);
        this.inventory.readNbt(nbt);
        this.cookTime = nbt.getShort("CookTime");
        this.cookTimeTotal = nbt.getShort("CookTimeTotal");
        var nbtCompound = nbt.getCompound("RecipesUsed");
        for (var string : nbtCompound.getKeys()) {
            this.recipesUsed.put(new Identifier(string), nbtCompound.getInt(string));
        }
        this.energyContainer = CleanEnergyContainer.readNbt(nbt);
    }

    @Override
    protected void writeNbt(@NotNull NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putShort("CookTime", (short) this.cookTime);
        nbt.putShort("CookTimeTotal", (short) this.cookTimeTotal);
        this.inventory.writeNbt(nbt);
        var nbtCompound = new NbtCompound();
        this.recipesUsed.forEach((identifier, count) -> nbtCompound.putInt(identifier.toString(), count));
        nbt.put("RecipesUsed", nbtCompound);
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
        var itemStack = this.inventory.getStack(slot);
        var sameItem = !stack.isEmpty() && stack.isItemEqual(itemStack) && ItemStack.areNbtEqual(stack, itemStack);
        this.inventory.setStack(slot, stack);
        if (stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
        var slotType = inventory.getType(slot);
        if (slotType == EnergyMachineInventoryType.SOURCE && !sameItem) {
            this.cookTimeTotal = getCookTimeTotal(world);
            this.cookTime = 0;
            this.markDirty();
        }
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
    public void setLastRecipe(@Nullable Recipe<?> recipe) {
        if (recipe != null) {
            var identifier = recipe.getId();
            this.recipesUsed.addTo(identifier, 1);
        }
    }

    @Override
    @Nullable
    public Recipe<?> getLastRecipe() {
        return null;
    }

    @Override
    public void unlockLastRecipe(@NotNull PlayerEntity player) {
    }

    @Override
    public void provideRecipeInputs(@NotNull RecipeMatcher finder) {
        var sources = this.inventory.getInventory(EnergyMachineInventoryType.SOURCE);
        if(sources == null) {
            return;
        }
        for (int i = 0; i < sources.size(); i++) {
            var itemStack = sources.getStack(i);
            finder.addInput(itemStack);
        }
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
     * Method can be used for drop experience for last used recipes
     *
     * @param player player - crafter
     */
    public void dropExperienceForRecipesUsed(@NotNull ServerPlayerEntity player) {
        var list = this.getRecipesUsedAndDropExperience(player.getWorld(), player.getPos());
        player.unlockRecipes(list);
        this.recipesUsed.clear();
    }

    /**
     * Method can be used for drop experience for last used recipes.<br/>
     * Last used recipes returned
     *
     * @param world game world
     * @param pos   position to drop
     * @return list of last used recipes
     */
    public List<Recipe<?>> getRecipesUsedAndDropExperience(@NotNull ServerWorld world,
                                                           @NotNull Vec3d pos) {
        ArrayList<Recipe<?>> list = Lists.newArrayList();
        var recipeManager = world.getRecipeManager();
        for (var entry : this.recipesUsed.object2IntEntrySet()) {
            var key = entry.getKey();
            recipeManager.get(key).ifPresent(recipe -> {
                list.add(recipe);
                dropExperience(world, pos, entry.getIntValue(), recipe);
            });
        }
        return list;
    }

    /**
     * Add event listeners
     *
     * @param event event type
     * @param listener event listener
     */
    public void addListener(@NotNull EnergyMachineEvent event,
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
    public void removeListener(@NotNull EnergyMachineEvent event,
                               @NotNull Runnable listener) {
        var listeners = this.eventListeners.computeIfAbsent(event, it -> new ArrayList<>());
        listeners.remove(listener);
    }

    /**
     * Dispatch event
     *
     * @param event event type
     */
    protected void dispatch(@NotNull EnergyMachineEvent event) {
        var listeners = this.eventListeners.get(event);
        if(listeners != null) {
            for (var listener : listeners) {
                listener.run();
            }
        }
    }

    /**
     * Drop experience on recipe use
     *
     * @param world game world
     * @param pos position
     * @param id recipe entry id
     * @param recipe used recipe
     */
    protected void dropExperience(@NotNull ServerWorld world,
                                  @NotNull Vec3d pos,
                                  int id,
                                  @NotNull Recipe<?> recipe) {
        if (recipe instanceof AbstractCookingRecipe cookingRecipe) {
            ExperienceUtils.drop(world, pos, id, cookingRecipe.getExperience());
        }
    }

    /**
     * Method return amount of energy that used every cooking tick
     * @return amount of energy
     */
    public @NotNull Energy getEnergyUsagePerTick() {
        return ENERGY_ONE;
    }

    /**
     * Get current recipe
     *
     * @return recipe
     * @since 0.0.16
     */
    abstract public @Nullable Recipe<Inventory> getRecipe(@NotNull World world);

    /**
     * Get current recipe
     *
     * @return recipe
     * @since 0.0.16
     */
    protected <C extends Inventory, T extends Recipe<C>> @Nullable T getRecipe(@NotNull RecipeType<T> recipeType,
                                                                               @NotNull World world) {
        var sourceInventory = (C) inventory.getInventory(EnergyMachineInventoryType.SOURCE);
        if(sourceInventory == null) {
            return null;
        }
        var recipeManager = world.getRecipeManager();
        return recipeManager.getFirstMatch(recipeType, sourceInventory, world)
                .orElse(null);
    }

    /**
     * Method for calculation decrement of source items on cooking
     *
     * @param recipe using recipe
     * @return amount of source to decrement
     */
    protected int calculateDecrement(@NotNull Recipe<?> recipe) {
        return 1;
    }

    /**
     * Method for calculation of total cooking time.<br/>
     * Calculation can be based of recipe, some modifiers or even time of day.
     *
     * @param world game world
     * @return total cook time
     */
    abstract public int getCookTimeTotal(@NotNull World world);

    /**
     * Method return cook time increment
     *
     * @param world game world
     * @return cook time increment
     */
    public int getCookTimeInc(@NotNull World world) {
        return 1;
    }

    /**
     * Charge machine by item in charging slot
     *
     * @return true - machine was charged, false - otherwise
     */
    protected boolean charge() {
        var chargeStack = inventory.getStack(EnergyMachineInventoryType.CHARGE, 0);
        var chargeItem = chargeStack.getItem();
        if (!chargeStack.isEmpty() && (chargeItem instanceof ChargeableItem chargeableItem)) {
            int charge = chargeableItem.getCharge(chargeStack);
            if (charge > 0) {
                int transferred = Math.min(
                        block.getEnergyLevel().to,
                        Math.min(charge, energyContainer.getFreeSpace().intValue())
                );
                chargeableItem.discharge(chargeStack, transferred);
                energyContainer.add(transferred);
                return true;
            }
        }
        return false;
    }

    /**
     * Default implementation of machine cooking tick
     *
     * @param world game world
     * @param pos block position
     * @param state block state
     * @param blockEntity block entity
     */
    public static void simpleCookingTick(
            @NotNull World world,
            @NotNull BlockPos pos,
            @NotNull BlockState state,
            @NotNull AbstractEnergyMachineBlockEntity blockEntity) {
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
            return;
        }
        var recipe = blockEntity.getRecipe(world);
        if (recipe == null) {
            blockEntity.updateState(working, state, world, pos, changed);
            return;
        }
        var maxCountPerStack = blockEntity.getMaxCountPerStack();
        boolean energyUsed = false;
        boolean canCook = false;
        boolean cooked = false;
        int cookTimeInc = blockEntity.getCookTimeInc(world);
        for (int i = 0; i < blockEntity.slots; i++) {
            if (!EnergyMachineUtils.canAcceptRecipeOutput(i, blockEntity.inventory, world, recipe, maxCountPerStack)) {
                continue;
            }
            canCook = true;
            if(!energyUsed) {
                if(blockEntity.energyContainer.subtract(requiredEnergy)) {
                    energyUsed = true;
                    blockEntity.cookTime += cookTimeInc;
                    blockEntity.working = true;
                    if (blockEntity.cookTime >= blockEntity.cookTimeTotal) {
                        blockEntity.cookTime = 0;
                        blockEntity.cookTimeTotal = blockEntity.getCookTimeTotal(world);
                        cooked = true;
                    }
                    changed = true;
                    blockEntity.dispatch(EnergyMachineEvent.ENERGY_USED);
                } else {
                    blockEntity.dispatch(EnergyMachineEvent.ENERGY_NOT_ENOUGH);
                    break;
                }
            }
            if (cooked) {
                int decrement = blockEntity.calculateDecrement(recipe);
                if (EnergyMachineUtils.craftRecipe(i, blockEntity.inventory, world, recipe, decrement, maxCountPerStack)) {
                    blockEntity.setLastRecipe(recipe);
                }
                blockEntity.dispatch(EnergyMachineEvent.COOKED);
            }
        }
        if(!canCook) {
            blockEntity.cookTime = 0;
            blockEntity.dispatch(EnergyMachineEvent.CAN_NOT_COOK);
        }

        boolean energyChanged = hasEnergy != blockEntity.energyContainer.hasAtLeast(requiredEnergy);
        blockEntity.updateState(working, state, world, pos, energyChanged|| changed);
    }

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
        if(CoreTags.isChargeable(itemStack)) {
            var chargeStack = inventory.getStack(EnergyMachineInventoryType.CHARGE, 0);
            if(chargeStack.isEmpty()) {
                inventory.setStack(EnergyMachineInventoryType.CHARGE, 0, itemStack);
                return ItemStack.EMPTY;
            }
            return PipeUtils.mergeItems(chargeStack, itemStack);
        }
        return inventory.addStack(EnergyMachineInventoryType.SOURCE, itemStack);
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
}
