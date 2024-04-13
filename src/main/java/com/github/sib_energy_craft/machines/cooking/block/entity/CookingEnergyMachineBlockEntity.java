package com.github.sib_energy_craft.machines.cooking.block.entity;

import com.github.sib_energy_craft.machines.block.AbstractEnergyMachineBlock;
import com.github.sib_energy_craft.machines.block.entity.AbstractEnergyMachineBlockEntity;
import com.github.sib_energy_craft.machines.block.entity.EnergyMachineInventoryType;
import com.github.sib_energy_craft.machines.cooking.block.entity.property.CookingEnergyMachineTypedProperties;
import com.github.sib_energy_craft.machines.core.ExperienceCreatingMachine;
import com.github.sib_energy_craft.machines.utils.ExperienceUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author sibmaks
 * @since 0.0.36
 */
public abstract class CookingEnergyMachineBlockEntity<B extends AbstractEnergyMachineBlock>
        extends AbstractEnergyMachineBlockEntity<B>
        implements RecipeUnlocker, RecipeInputProvider, ExperienceCreatingMachine {

    protected int cookTime;
    protected int cookTimeTotal;
    protected final Object2IntOpenHashMap<Identifier> recipesUsed;


    protected CookingEnergyMachineBlockEntity(@NotNull BlockEntityType<?> blockEntityType,
                                              @NotNull BlockPos blockPos,
                                              @NotNull BlockState blockState,
                                              @NotNull B block) {
        this(blockEntityType, blockPos, blockState, block, 1, 1, 1);
    }

    protected CookingEnergyMachineBlockEntity(@NotNull BlockEntityType<?> blockEntityType,
                                              @NotNull BlockPos blockPos,
                                              @NotNull BlockState blockState,
                                              @NotNull B block,
                                              int sourceSlots,
                                              int outputSlots,
                                              int parallelProcess) {
        super(blockEntityType, blockPos, blockState, block, sourceSlots, outputSlots, parallelProcess);
        this.recipesUsed = new Object2IntOpenHashMap<>();

        this.energyMachinePropertyMap.add(CookingEnergyMachineTypedProperties.COOKING_TIME, () -> cookTime);
        this.energyMachinePropertyMap.add(CookingEnergyMachineTypedProperties.COOKING_TIME_TOTAL, () -> cookTimeTotal);
    }

    @Override
    public void readNbt(@NotNull NbtCompound nbt) {
        super.readNbt(nbt);
        this.cookTime = nbt.getShort("CookTime");
        this.cookTimeTotal = nbt.getShort("CookTimeTotal");
        var nbtCompound = nbt.getCompound("RecipesUsed");
        for (var string : nbtCompound.getKeys()) {
            this.recipesUsed.put(new Identifier(string), nbtCompound.getInt(string));
        }
    }

    @Override
    protected void writeNbt(@NotNull NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putShort("CookTime", (short) this.cookTime);
        nbt.putShort("CookTimeTotal", (short) this.cookTimeTotal);
        var nbtCompound = new NbtCompound();
        this.recipesUsed.forEach((identifier, count) -> nbtCompound.putInt(identifier.toString(), count));
        nbt.put("RecipesUsed", nbtCompound);
    }

    @Override
    protected void onSourceSet(@NotNull World world, boolean wasSourceEmpty, boolean isSourceEmpty) {
        super.onSourceSet(world, wasSourceEmpty, isSourceEmpty);
        if (isSourceEmpty) {
            this.cookTime = 0;
        } else if (wasSourceEmpty) {
            this.cookTimeTotal = getCookTimeTotal(world);
        }
    }

    @Override
    public void setLastRecipe(@Nullable RecipeEntry<?> recipe) {
        if (recipe != null) {
            var identifier = recipe.id();
            this.recipesUsed.addTo(identifier, 1);
        }
    }

    @Override
    public RecipeEntry<?> getLastRecipe() {
        return null;
    }

    @Override
    public void provideRecipeInputs(@NotNull RecipeMatcher finder) {
        var sources = this.inventory.getInventory(EnergyMachineInventoryType.SOURCE);
        if (sources == null) {
            return;
        }
        for (int i = 0; i < sources.size(); i++) {
            var itemStack = sources.getStack(i);
            finder.addInput(itemStack);
        }
    }

    @Override
    public void dropExperienceForRecipesUsed(@NotNull ServerPlayerEntity player) {
        var list = this.getRecipesUsedAndDropExperience(player.getServerWorld(), player.getPos());
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
    protected List<RecipeEntry<?>> getRecipesUsedAndDropExperience(@NotNull ServerWorld world,
                                                                   @NotNull Vec3d pos) {
        var recipes = new ArrayList<RecipeEntry<?>>();
        var recipeManager = world.getRecipeManager();
        for (var entry : this.recipesUsed.object2IntEntrySet()) {
            var key = entry.getKey();
            recipeManager.get(key).ifPresent(recipe -> {
                recipes.add(recipe);
                dropExperience(world, pos, entry.getIntValue(), recipe);
            });
        }
        return recipes;
    }

    /**
     * Drop experience on recipe use
     *
     * @param world       game world
     * @param pos         position
     * @param id          recipe entry id
     * @param recipeEntry used recipe
     */
    protected void dropExperience(@NotNull ServerWorld world,
                                  @NotNull Vec3d pos,
                                  int id,
                                  @NotNull RecipeEntry<? extends Recipe<?>> recipeEntry) {
        var recipe = recipeEntry.value();
        if (recipe instanceof AbstractCookingRecipe cookingRecipe) {
            ExperienceUtils.drop(world, pos, id, cookingRecipe.getExperience());
        }
    }

    /**
     * Get current recipe by game world and process index
     *
     * @param world   game world
     * @param process process index
     * @return recipe
     * @since 0.0.16
     */
    public abstract @Nullable RecipeEntry<? extends Recipe<Inventory>> getRecipe(@NotNull World world, int process);

    /**
     * Get recipe using all source inventory
     *
     * @return recipe
     * @since 0.0.16
     */
    protected <C extends Inventory,
            R extends Recipe<C>> @Nullable RecipeEntry<R> getRecipe(@NotNull RecipeType<R> recipeType,
                                                                    @NotNull World world) {
        var sourceInventory = (C) inventory.getInventory(EnergyMachineInventoryType.SOURCE);
        if (sourceInventory == null) {
            return null;
        }
        var recipeManager = world.getRecipeManager();
        return recipeManager.getFirstMatch(recipeType, sourceInventory, world)
                .orElse(null);
    }

    /**
     * Get recipe using only passed slot
     *
     * @return recipe
     * @since 0.0.20
     */
    protected <C extends Inventory,
            T extends Recipe<C>> @Nullable RecipeEntry<T> getRecipe(@NotNull RecipeType<T> recipeType,
                                                                    @NotNull World world,
                                                                    int slot) {
        var sourceInventory = inventory.getInventory(EnergyMachineInventoryType.SOURCE);
        if (sourceInventory == null) {
            return null;
        }
        var sourceStack = sourceInventory.getStack(slot);
        var craftingInventory = (C) new SimpleInventory(sourceStack);
        var recipeManager = world.getRecipeManager();
        return recipeManager.getFirstMatch(recipeType, craftingInventory, world)
                .orElse(null);
    }

    /**
     * Method for calculation decrement of source items on cooking
     *
     * @param recipeEntry using recipe
     * @return amount of source to decrement
     */
    protected int calculateDecrement(@NotNull RecipeEntry<? extends Recipe<?>> recipeEntry) {
        return 1;
    }

    /**
     * Method for calculation of total cooking time.<br/>
     * Calculation can be based of recipe, some modifiers or even time of day.
     *
     * @param world game world
     * @return total cook time
     */
    public abstract int getCookTimeTotal(@NotNull World world);

    /**
     * Method return cook time increment
     *
     * @param world game world
     * @return cook time increment
     */
    public int getCookTimeInc(@NotNull World world) {
        return 1;
    }

    @Override
    protected boolean canProcess(int process,
                                 @NotNull World world,
                                 @NotNull BlockPos pos,
                                 @NotNull BlockState state,
                                 @NotNull Map<String, Object> processContext) {
        var recipeEntry = getRecipe(world, process);
        if (recipeEntry == null) {
            return false;
        }
        processContext.put("Recipe", recipeEntry);
        var maxCountPerStack = getMaxCountPerStack();
        return canAcceptRecipeOutput(process, world, recipeEntry, maxCountPerStack);
    }

    @Override
    protected boolean tickProcess(int process,
                                  @NotNull World world,
                                  @NotNull BlockPos pos,
                                  @NotNull BlockState state,
                                  @NotNull Map<String, Object> processContext) {
        int cookTimeInc = getCookTimeInc(world);
        cookTime += cookTimeInc;
        working = true;
        if (cookTime >= cookTimeTotal) {
            cookTime = 0;
            cookTimeTotal = getCookTimeTotal(world);
            return true;
        }
        return false;
    }

    @Override
    protected void onProcessFinished(int process,
                                     @NotNull World world,
                                     @NotNull BlockPos pos,
                                     @NotNull BlockState state,
                                     @NotNull Map<String, Object> processContext) {
        var recipe = (RecipeEntry<Recipe<Inventory>>) processContext.get("Recipe");
        var maxCountPerStack = getMaxCountPerStack();
        int decrement = calculateDecrement(recipe);
        if (craftRecipe(process, world, recipe, decrement, maxCountPerStack)) {
            setLastRecipe(recipe);
        }
    }

    /**
     * Get machine accept recipe output produced by specific process
     *
     * @param process     machine process index
     * @param world       game world
     * @param recipeEntry crafting recipe
     * @param count       max amount of output
     * @return true - machine can accept recipe, false - otherwise
     */
    protected abstract boolean canAcceptRecipeOutput(int process,
                                                     @NotNull World world,
                                                     @NotNull RecipeEntry<? extends Recipe<Inventory>> recipeEntry,
                                                     int count);

    /**
     * Craft recipe in specific process
     *
     * @param process   machine process index
     * @param world     game world
     * @param recipe    crafting recipe
     * @param decrement amount of source stack to decrement
     * @param maxCount  max amount of output
     * @return true - recipe crafted, false - otherwise
     */
    public abstract boolean craftRecipe(int process,
                                        @NotNull World world,
                                        @NotNull RecipeEntry<? extends Recipe<Inventory>> recipe,
                                        int decrement,
                                        int maxCount);

    @Override
    public void onStateReplaced(@NotNull BlockState state,
                                @NotNull ServerWorld serverWorld,
                                @NotNull BlockPos pos,
                                @NotNull BlockState newState,
                                boolean moved) {
        super.onStateReplaced(state, serverWorld, pos, newState, moved);
        getRecipesUsedAndDropExperience(serverWorld, Vec3d.ofCenter(pos));
    }
}
