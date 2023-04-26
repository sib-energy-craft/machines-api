package com.github.sib_energy_craft.machines.recipe;

import com.google.gson.JsonObject;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.CookingRecipeSerializer.RecipeFactory;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CookingRecipeCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.NotNull;

/**
 * @since 0.0.1
 * @author sibmaks
 */
public record CookingRecipeSerializer<T extends OpenAbstractCookingRecipe>(@NotNull RecipeFactory<T> recipeFactory,
                                                                           int cookingTime) implements RecipeSerializer<T> {

    @Override
    public @NotNull T read(@NotNull Identifier identifier, @NotNull JsonObject jsonObject) {
        var string = JsonHelper.getString(jsonObject, "group", "");
        var category = JsonHelper.getString(jsonObject, "category", null);
        var cookingRecipeCategory = CookingRecipeCategory.CODEC.byId(category, CookingRecipeCategory.MISC);
        var jsonElement = JsonHelper.hasArray(jsonObject, "ingredient") ?
                JsonHelper.getArray(jsonObject, "ingredient") : JsonHelper.getObject(jsonObject, "ingredient");
        var ingredient = Ingredient.fromJson(jsonElement);
        var itemStack = ShapedRecipe.outputFromJson(JsonHelper.getObject(jsonObject, "result"));
        var f = JsonHelper.getFloat(jsonObject, "experience", 0.0f);
        var i = JsonHelper.getInt(jsonObject, "cookingTime", this.cookingTime);
        return this.recipeFactory.create(identifier, string, cookingRecipeCategory, ingredient, itemStack, f, i);
    }

    @Override
    public @NotNull T read(@NotNull Identifier identifier, @NotNull PacketByteBuf packetByteBuf) {
        var string = packetByteBuf.readString();
        var cookingRecipeCategory = packetByteBuf.readEnumConstant(CookingRecipeCategory.class);
        var ingredient = Ingredient.fromPacket(packetByteBuf);
        var itemStack = packetByteBuf.readItemStack();
        var f = packetByteBuf.readFloat();
        var i = packetByteBuf.readVarInt();
        return this.recipeFactory.create(identifier, string, cookingRecipeCategory, ingredient, itemStack, f, i);
    }

    @Override
    public void write(PacketByteBuf packetByteBuf, T abstractCookingRecipe) {
        packetByteBuf.writeString(abstractCookingRecipe.getGroup());
        packetByteBuf.writeEnumConstant(abstractCookingRecipe.getCategory());
        abstractCookingRecipe.getInput().write(packetByteBuf);
        packetByteBuf.writeItemStack(abstractCookingRecipe.getOutput());
        packetByteBuf.writeFloat(abstractCookingRecipe.getExperience());
        packetByteBuf.writeVarInt(abstractCookingRecipe.getCookTime());
    }

}
