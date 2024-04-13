package com.github.sib_energy_craft.machines.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.CookingRecipeCategory;
import org.jetbrains.annotations.NotNull;

/**
 * @author sibmaks
 * @since 0.0.1
 */
public abstract class OpenAbstractCookingRecipe extends AbstractCookingRecipe {


    protected OpenAbstractCookingRecipe(@NotNull RecipeType<?> type,
                                        @NotNull String group,
                                        @NotNull CookingRecipeCategory category,
                                        @NotNull Ingredient input,
                                        @NotNull ItemStack output,
                                        float experience,
                                        int cookTime) {
        super(type, group, category, input, output, experience, cookTime);
    }

    /**
     * Getter for recipe input
     *
     * @return input
     */
    public Ingredient getInput() {
        return ingredient;
    }


    /**
     * Getter for recipe output
     *
     * @return output
     */
    public ItemStack getOutput() {
        return result;
    }
}
