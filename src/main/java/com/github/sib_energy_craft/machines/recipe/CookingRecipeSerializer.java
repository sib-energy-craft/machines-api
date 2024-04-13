package com.github.sib_energy_craft.machines.recipe;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.CookingRecipeSerializer.RecipeFactory;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.book.CookingRecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.util.dynamic.Codecs;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @since 0.0.1
 * @author sibmaks
 */
public record CookingRecipeSerializer<T extends OpenAbstractCookingRecipe>(@NotNull RecipeFactory<T> recipeFactory,
                                                                           int cookingTime) implements RecipeSerializer<T> {

    public CookingRecipeSerializer(@NotNull RecipeFactory<T> recipeFactory) {

        this.codec = RecordCodecBuilder.create((instance) -> {
            Products.P6 var10000 = instance.group(Codecs.createStrictOptionalFieldCodec(Codec.STRING, "group", "").forGetter((recipe) -> {
                return recipe.group;
            }), CookingRecipeCategory.CODEC.fieldOf("category").orElse(CookingRecipeCategory.MISC).forGetter((recipe) -> {
                return recipe.category;
            }), Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("ingredient").forGetter((recipe) -> {
                return recipe.ingredient;
            }), Registries.ITEM.getCodec().xmap(ItemStack::new, ItemStack::getItem).fieldOf("result").forGetter((recipe) -> {
                return recipe.result;
            }), Codec.FLOAT.fieldOf("experience").orElse(0.0F).forGetter((recipe) -> {
                return recipe.experience;
            }), Codec.INT.fieldOf("cookingtime").orElse(cookingTime).forGetter((recipe) -> {
                return recipe.cookingTime;
            }));
            Objects.requireNonNull(recipeFactory);
            return var10000.apply(instance, recipeFactory::create);
        });
    }

    @Override
    public Codec<T> codec() {
        return null;
    }

    @Override
    public T read(PacketByteBuf buf) {
        var group = buf.readString();
        var category = buf.readEnumConstant(CookingRecipeCategory.class);
        var ingredient = Ingredient.fromPacket(buf);
        var itemStack = buf.readItemStack();
        var exp = buf.readFloat();
        var cookTime = buf.readVarInt();
        return this.recipeFactory.create(group, category, ingredient, itemStack, exp, cookTime);
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
