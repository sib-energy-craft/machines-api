package com.github.sib_energy_craft.machines.recipe;

import com.mojang.serialization.Codec;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.book.CookingRecipeCategory;
import org.jetbrains.annotations.NotNull;

/**
 * @author sibmaks
 * @since 0.0.1
 */
public record CookingRecipeSerializer<T extends OpenAbstractCookingRecipe>(
        @NotNull AbstractCookingRecipe.RecipeFactory<T> recipeFactory,
        @NotNull Codec<T> codec,
        int cookingTime) implements RecipeSerializer<T> {

    @Override
    public @NotNull T read(@NotNull PacketByteBuf packetByteBuf) {
        var group = packetByteBuf.readString();
        var cookingRecipeCategory = packetByteBuf.readEnumConstant(CookingRecipeCategory.class);
        var ingredient = Ingredient.fromPacket(packetByteBuf);
        var output = packetByteBuf.readItemStack();
        var experience = packetByteBuf.readFloat();
        var cookingTime = packetByteBuf.readVarInt();
        return this.recipeFactory.create(group, cookingRecipeCategory, ingredient, output, experience, cookingTime);
    }

    @Override
    public void write(PacketByteBuf packetByteBuf, T abstractCookingRecipe) {
        packetByteBuf.writeString(abstractCookingRecipe.getGroup());
        packetByteBuf.writeEnumConstant(abstractCookingRecipe.getCategory());
        abstractCookingRecipe.getInput().write(packetByteBuf);
        packetByteBuf.writeItemStack(abstractCookingRecipe.getOutput());
        packetByteBuf.writeFloat(abstractCookingRecipe.getExperience());
        packetByteBuf.writeVarInt(abstractCookingRecipe.getCookingTime());
    }
}
