package com.github.sib_energy_craft.machines.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

/**
 * @since 0.0.1
 * @author sibmaks
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExperienceUtils {
    /**
     * Method process experience drop in word
     *
     * @param world game world
     * @param pos position to drop
     * @param multiplier experience multiplier
     * @param experience experience amount
     */
    public static void drop(@NotNull ServerWorld world,
                            @NotNull Vec3d pos,
                            int multiplier,
                            float experience) {
        var i = MathHelper.floor(multiplier * experience);
        var f = MathHelper.fractionalPart(multiplier * experience);
        if (f != 0.0f && Math.random() < f) {
            ++i;
        }
        ExperienceOrbEntity.spawn(world, pos, i);
    }
}
