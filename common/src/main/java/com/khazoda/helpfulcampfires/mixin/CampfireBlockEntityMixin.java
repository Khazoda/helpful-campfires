package com.khazoda.helpfulcampfires.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(CampfireBlockEntity.class)
public class CampfireBlockEntityMixin {

    @Unique
    private static final int EFFECT_RADIUS = 5;

    @Inject(method = "cookTick", at = @At("TAIL"))
    private static void helpfulcampfires$applyEffectsOnServer(Level level, BlockPos pos, BlockState state, CampfireBlockEntity blockEntity, CallbackInfo ci) {
        // Runs serverside for lit campfires only
        if (level.isClientSide()) return;
        if (!state.getValue(CampfireBlock.LIT)) return;

        helpfulcampfires$applyEffects(level, pos);
    }

    @Unique
    private static void helpfulcampfires$applyEffects(Level level, BlockPos pos) {
        // Create bounding box for effect area
        AABB aabb = new AABB(pos).inflate(EFFECT_RADIUS);

        // Find and affect all players in range with regeneration 2
        List<Player> players = level.getEntitiesOfClass(Player.class, aabb);
        if (!players.isEmpty()) {
            for (Player player : players) {
                if (pos.closerThan(player.blockPosition(), EFFECT_RADIUS)) {
                    if (!player.hasEffect(MobEffects.REGENERATION)) {
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, true, false));
                    }
                }
            }
        }
    }
}
