package com.github.sib_energy_craft.machines.block.entity;

import com.github.sib_energy_craft.containers.CleanEnergyContainer;
import com.github.sib_energy_craft.energy_api.Energy;
import com.github.sib_energy_craft.energy_api.EnergyOffer;
import com.github.sib_energy_craft.energy_api.consumer.EnergyConsumer;
import com.github.sib_energy_craft.energy_api.items.ChargeableItem;
import com.github.sib_energy_craft.energy_api.tags.CoreTags;
import com.github.sib_energy_craft.machines.block.AbstractEnergyMachineBlock;
import com.github.sib_energy_craft.machines.utils.ExperienceUtils;
import com.github.sib_energy_craft.pipes.api.ItemConsumer;
import com.github.sib_energy_craft.pipes.api.ItemSupplier;
import com.github.sib_energy_craft.pipes.utils.PipeUtils;
import com.github.sib_energy_craft.sec_utils.screen.PropertyMap;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.sib_energy_craft.machines.block.entity.AbstractEnergyMachineProperties.*;

/**
 * @since 0.0.1
 * @author sibmaks
 */
public abstract class AbstractEnergyMachineBlockEntity extends LockableContainerBlockEntity
        implements SidedInventory, RecipeUnlocker, RecipeInputProvider, EnergyConsumer, ItemConsumer, ItemSupplier {
    private static final Energy ENERGY_ONE = Energy.of(1);

    public static final int SOURCE_SLOT = 0;
    public static final int CHARGE_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;

    private static final int[] TOP_SLOTS = new int[]{SOURCE_SLOT};
    private static final int[] BOTTOM_SLOTS = new int[]{OUTPUT_SLOT};
    private static final int[] SIDE_SLOTS = new int[]{CHARGE_SLOT, SOURCE_SLOT};

    protected int cookTime;
    protected int cookTimeTotal;
    protected DefaultedList<ItemStack> inventory;
    protected CleanEnergyContainer energyContainer;
    protected boolean working;

    protected final AbstractEnergyMachineBlock block;
    protected final PropertyMap<AbstractEnergyMachineProperties> propertyMap;
    protected final Object2IntOpenHashMap<Identifier> recipesUsed;

    public AbstractEnergyMachineBlockEntity(@NotNull BlockEntityType<?> blockEntityType,
                                            @NotNull BlockPos blockPos,
                                            @NotNull BlockState blockState,
                                            @NotNull AbstractEnergyMachineBlock block) {
        super(blockEntityType, blockPos, blockState);
        this.block = block;
        this.recipesUsed = new Object2IntOpenHashMap<>();
        this.inventory = DefaultedList.ofSize(3, ItemStack.EMPTY);
        this.energyContainer = new CleanEnergyContainer(Energy.ZERO, block.getMaxCharge());
        this.propertyMap = new PropertyMap<>(AbstractEnergyMachineProperties.class);
        this.propertyMap.add(COOKING_TIME, () -> cookTime);
        this.propertyMap.add(COOKING_TIME_TOTAL, () -> cookTimeTotal);
        this.propertyMap.add(CHARGE, () -> this.energyContainer.getCharge().intValue());
        this.propertyMap.add(MAX_CHARGE, () -> this.energyContainer.getMaxCharge().intValue());
    }

    @Override
    public void readNbt(@NotNull NbtCompound nbt) {
        super.readNbt(nbt);
        this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        Inventories.readNbt(nbt, this.inventory);
        this.cookTime = nbt.getShort("CookTime");
        this.cookTimeTotal = nbt.getShort("CookTimeTotal");
        var nbtCompound = nbt.getCompound("RecipesUsed");
        for (String string : nbtCompound.getKeys()) {
            this.recipesUsed.put(new Identifier(string), nbtCompound.getInt(string));
        }
        this.energyContainer = CleanEnergyContainer.readNbt(nbt);
    }

    @Override
    protected void writeNbt(@NotNull NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putShort("CookTime", (short) this.cookTime);
        nbt.putShort("CookTimeTotal", (short) this.cookTimeTotal);
        Inventories.writeNbt(nbt, this.inventory);
        var nbtCompound = new NbtCompound();
        this.recipesUsed.forEach((identifier, count) -> nbtCompound.putInt(identifier.toString(), count));
        nbt.put("RecipesUsed", nbtCompound);
        this.energyContainer.writeNbt(nbt);
    }

    @Override
    public int[] getAvailableSlots(@NotNull Direction side) {
        if (side == Direction.DOWN) {
            return BOTTOM_SLOTS;
        }
        if (side == Direction.UP) {
            return TOP_SLOTS;
        }
        return SIDE_SLOTS;
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
        if (dir == Direction.DOWN && slot == CHARGE_SLOT) {
            var item = stack.getItem();
            return item instanceof ChargeableItem chargeableItem && !chargeableItem.hasEnergy(stack);
        }
        return true;
    }

    @Override
    public int size() {
        return this.inventory.size();
    }

    @Override
    public boolean isEmpty() {
        return this.inventory.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return Inventories.splitStack(this.inventory, slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.inventory, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        var world = this.world;
        if (world == null) {
            return;
        }
        var itemStack = this.inventory.get(slot);
        var sameItem = !stack.isEmpty() && stack.isItemEqual(itemStack) && ItemStack.areNbtEqual(stack, itemStack);
        this.inventory.set(slot, stack);
        if (stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
        if (slot == SOURCE_SLOT && !sameItem) {
            this.cookTimeTotal = getCookTime(world);
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
        if (slot == OUTPUT_SLOT) {
            return false;
        }
        if(slot == CHARGE_SLOT) {
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
        for (var itemStack : this.inventory) {
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
     * Method for calculation of cooking time.<br/>
     * Calculation can be based of recipe, some modifiers or even time of day.
     *
     * @param world game world
     * @return cook time
     */
    abstract public int getCookTime(@NotNull World world);

    /**
     * Get current recipe type
     *
     * @return recipe type
     * @param <C> inventory type
     * @param <T> recipe type
     */
    abstract public<C extends Inventory, T extends Recipe<C>> @NotNull RecipeType<T> getRecipeType();

    /**
     * Method for calculation decrement on cooking
     *
     * @param recipe using recipe
     * @return amount of source to decrement
     */
    protected int calculateDecrement(@NotNull Recipe<?> recipe) {
        return 1;
    }

    protected static boolean canAcceptRecipeOutput(@NotNull World world,
                                                   @NotNull Recipe<?> recipe,
                                                   @NotNull DefaultedList<ItemStack> slots,
                                                   int count) {
        if (slots.get(SOURCE_SLOT).isEmpty()) {
            return false;
        }
        var outputStack = recipe.getOutput(world.getRegistryManager());
        if (outputStack.isEmpty()) {
            return false;
        }
        var outputSlotStack = slots.get(OUTPUT_SLOT);
        if (outputSlotStack.isEmpty()) {
            return true;
        }
        if (!outputSlotStack.isItemEqual(outputStack)) {
            return false;
        }
        if (outputSlotStack.getCount() < count &&
                outputSlotStack.getCount() < outputSlotStack.getMaxCount()) {
            return true;
        }
        return outputSlotStack.getCount() < outputStack.getMaxCount();
    }

    protected static boolean craftRecipe(@NotNull World world,
                                         @NotNull Recipe<Inventory> recipe,
                                         @NotNull DefaultedList<ItemStack> slots,
                                         int decrement,
                                         int maxCount) {
        if (!canAcceptRecipeOutput(world, recipe, slots, maxCount)) {
            return false;
        }
        var sourceSlot = slots.get(SOURCE_SLOT);
        var registryManager = world.getRegistryManager();
        var recipeStack = recipe.getOutput(registryManager);
        var outputStack = slots.get(OUTPUT_SLOT);
        if (outputStack.isEmpty()) {
            slots.set(OUTPUT_SLOT, recipeStack.copy());
        } else if (outputStack.isOf(recipeStack.getItem())) {
            outputStack.increment(recipeStack.getCount());
        }
        sourceSlot.decrement(decrement);
        return true;
    }

    protected static <T extends AbstractCookingRecipe> int getCookTime(@NotNull World world,
                                                                       @NotNull RecipeType<T> recipeType,
                                                                       @NotNull Inventory inventory) {
        return world.getRecipeManager()
                .getFirstMatch(recipeType, inventory, world)
                .map(AbstractCookingRecipe::getCookTime)
                .orElse(200);
    }

    protected static void charge(@NotNull AbstractEnergyMachineBlockEntity blockEntity) {
        var itemStack = blockEntity.inventory.get(CHARGE_SLOT);
        var item = itemStack.getItem();
        if (!itemStack.isEmpty() && (item instanceof ChargeableItem chargeableItem)) {
            int charge = chargeableItem.getCharge(itemStack);
            if (charge > 0) {
                int transferred = Math.min(charge, blockEntity.energyContainer.getFreeSpace().intValue());
                chargeableItem.discharge(itemStack, transferred);
                blockEntity.energyContainer.add(transferred);
            }
        }
    }

    public static <T extends Recipe<Inventory>> void simpleCookingTick(
            @NotNull World world,
            @NotNull BlockPos pos,
            @NotNull BlockState state,
            @NotNull AbstractEnergyMachineBlockEntity blockEntity) {
        if (world.isClient) {
            return;
        }
        var hasEnergy = blockEntity.energyContainer.hasAtLeast(ENERGY_ONE);
        var changed = false;
        var working = blockEntity.working;
        blockEntity.working = false;

        charge(blockEntity);

        if (!blockEntity.energyContainer.hasAtLeast(ENERGY_ONE)) {
            boolean energyChanged = hasEnergy != blockEntity.energyContainer.hasEnergy();
            updateState(working, blockEntity, state, world, pos, energyChanged);
            return;
        }
        var recipeManager = world.getRecipeManager();
        var recipeType = blockEntity.getRecipeType();
        var recipe = recipeManager.getFirstMatch(recipeType, blockEntity, world)
                .orElse(null);
        if (recipe == null) {
            boolean energyChanged = hasEnergy != blockEntity.energyContainer.hasEnergy();
            updateState(working, blockEntity, state, world, pos, energyChanged);
            return;
        }
        var maxCountPerStack = blockEntity.getMaxCountPerStack();
        if (canAcceptRecipeOutput(world, recipe, blockEntity.inventory, maxCountPerStack)) {
            if (blockEntity.energyContainer.subtract(ENERGY_ONE)) {
                ++blockEntity.cookTime;
                blockEntity.working = true;
                if (blockEntity.cookTime >= blockEntity.cookTimeTotal) {
                    blockEntity.cookTime = 0;
                    blockEntity.cookTimeTotal = blockEntity.getCookTime(world);
                    int decrement = blockEntity.calculateDecrement(recipe);
                    if (craftRecipe(world, recipe, blockEntity.inventory, decrement, maxCountPerStack)) {
                        blockEntity.setLastRecipe(recipe);
                    }
                }
                changed = true;
            }
        } else {
            blockEntity.cookTime = 0;
        }

        boolean energyChanged = hasEnergy != blockEntity.energyContainer.hasEnergy();
        updateState(working, blockEntity, state, world, pos, energyChanged|| changed);
    }

    private static void updateState(boolean wasWork,
                                    @NotNull AbstractEnergyMachineBlockEntity blockEntity,
                                    @NotNull BlockState state,
                                    @NotNull World world,
                                    @NotNull BlockPos pos,
                                    boolean markDirty) {
        if (wasWork != blockEntity.working) {
            state = state.with(AbstractEnergyMachineBlock.WORKING, blockEntity.working);
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
            var chargeSlot = inventory.get(CHARGE_SLOT);
            return chargeSlot.isEmpty() || PipeUtils.canMergeItems(chargeSlot, itemStack);
        }
        var inputStack = inventory.get(SOURCE_SLOT);
        return isValid(SOURCE_SLOT, itemStack) &&
                (inputStack.isEmpty() || PipeUtils.canMergeItems(inputStack, itemStack));
    }

    @Override
    public @NotNull ItemStack consume(@NotNull ItemStack itemStack, @NotNull Direction direction) {
        if(!canConsume(itemStack, direction)) {
            return itemStack;
        }
        markDirty();
        if(AbstractFurnaceBlockEntity.canUseAsFuel(itemStack)) {
            var chargeSlot = inventory.get(CHARGE_SLOT);
            if(chargeSlot.isEmpty()) {
                inventory.set(CHARGE_SLOT, itemStack);
                return ItemStack.EMPTY;
            }
            return PipeUtils.mergeItems(chargeSlot, itemStack);
        }
        var inputStack = inventory.get(SOURCE_SLOT);
        if(inputStack.isEmpty()) {
            inventory.set(SOURCE_SLOT, itemStack);
            return ItemStack.EMPTY;
        }
        return PipeUtils.mergeItems(inputStack, itemStack);
    }

    @Override
    public @NotNull List<ItemStack> canSupply(@NotNull Direction direction) {
        var outputStack = inventory.get(OUTPUT_SLOT);
        if(outputStack.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(outputStack);
    }

    @Override
    public boolean supply(@NotNull ItemStack requested, @NotNull Direction direction) {
        var outputStack = inventory.get(OUTPUT_SLOT);
        if(outputStack.isEmpty() || !outputStack.isItemEqual(requested) || outputStack.getCount() < requested.getCount()) {
            return false;
        }
        outputStack.decrement(requested.getCount());
        markDirty();
        return true;
    }

    @Override
    public void returnStack(@NotNull ItemStack requested, @NotNull Direction direction) {
        var outputStack = inventory.get(OUTPUT_SLOT);
        if(outputStack.isEmpty()) {
            inventory.set(OUTPUT_SLOT, requested);
        } else {
            outputStack.increment(requested.getCount());
        }
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
