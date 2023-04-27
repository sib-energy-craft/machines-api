package com.github.sib_energy_craft.machines.item;

import com.github.sib_energy_craft.machines.block.AbstractEnergyMachineBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;

/**
 * @since 0.0.6
 * @author sibmaks
 */
public class EnergyMachineBlockItem<T extends AbstractEnergyMachineBlock> extends BlockItem {

    public EnergyMachineBlockItem(@NotNull T block, @NotNull Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(@NotNull ItemStack stack,
                              @Nullable World world,
                              @NotNull List<Text> tooltip,
                              @NotNull TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        var block = getBlock();
        var maxInput = block.getMaxCharge();
        var maxCharge = block.getEnergyLevel().to;
        var textColor = Color.GRAY.getRGB();
        var textStyle = Style.EMPTY.withColor(textColor);
        tooltip.add(Text.translatable("attribute.name.sib_energy_craft.max_input_eu", maxInput)
                .setStyle(textStyle));
        tooltip.add(Text.translatable("attribute.name.sib_energy_craft.max_charge", maxCharge)
                .setStyle(textStyle));
    }

    @Override
    public AbstractEnergyMachineBlock getBlock() {
        return (AbstractEnergyMachineBlock) super.getBlock();
    }
}
