package com.github.sib_energy_craft.machines.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.CookingRecipeCategory;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * @since 0.0.1
 * @author sibmaks
 */
public abstract class OpenAbstractCookingRecipe extends AbstractCookingRecipe {


    public OpenAbstractCookingRecipe(@NotNull RecipeType<?> type, Identifier id,
                                     @NotNull String group,
                                     @NotNull CookingRecipeCategory category,
                                     @NotNull Ingredient input,
                                     @NotNull ItemStack output,
                                     float experience,
                                     int cookTime) {
        super(type, id, group, category, input, output, experience, cookTime);
    }

    /**
     * Getter for recipe input
     * @return input
     */
    public Ingredient getInput() {
        return input;
    }


    /**
     * Getter for recipe output
     * @return output
     */
    public ItemStack getOutput() {
        return output;
    }
}
