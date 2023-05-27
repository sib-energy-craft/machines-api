package com.github.sib_energy_craft.machines.core;

import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

/**
 * @author sibmaks
 * @since 0.0.28
 */
public interface ExperienceCreatingMachine {

    /**
     * Method can be used for drop experience for last used recipes
     *
     * @param player server player
     */
    void dropExperienceForRecipesUsed(@NotNull ServerPlayerEntity player);
}
