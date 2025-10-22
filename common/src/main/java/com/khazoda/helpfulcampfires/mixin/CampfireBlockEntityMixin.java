package com.khazoda.helpfulcampfires.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
  // Effect Distances & Debounce time
  @Unique private static final int EFFECT_RADIUS = 5;
  @Unique private static final int EFFECT_REMOVAL_RADIUS = EFFECT_RADIUS + 5;
  @Unique private static final int GRACE_PERIOD_TICKS = 10;

  // Sound timing
  @Unique private static final int AMBIENT_SOUND_INTERVAL = 80;
  @Unique private static final int MIN_SHORT_AMBIENT_DELAY = 60;
  @Unique private static final int MAX_EXTRA_AMBIENT_DELAY = 40;

  // Mixin State tracking
  @Unique private boolean helpfulcampfires$wasActive = false;
  @Unique private long helpfulcampfires$lastStatusChange = 0L;
  @Unique private long helpfulcampfires$nextAmbientSound = 0L;

  /* This is the campfire's standard ticking method, runs when it isn't cooking also */
  @Inject(method = "cookTick", at = @At("TAIL"))
  private static void helpfulcampfires$applyEffectsOnServer(Level level, BlockPos pos, BlockState state, CampfireBlockEntity blockEntity, CallbackInfo ci) {

    if (level.isClientSide() || !state.getValue(CampfireBlock.LIT)) {
      return;
    }

    CampfireBlockEntityMixin mixin = (CampfireBlockEntityMixin) (Object) blockEntity;
    if (mixin == null) {
      return;
    }

    long currentTime = level.getGameTime();
    boolean hasPlayersInRange = helpfulcampfires$checkAndHandleEffects(level, pos);

    helpfulcampfires$handleStatusSounds(level, pos, mixin, hasPlayersInRange, currentTime);
    if (hasPlayersInRange) {
      helpfulcampfires$handleAmbientSounds(level, pos, mixin, currentTime);
    }
  }

  /* Plays sound effects when entering and exiting the campfire's effective range */
  @Unique
  private static void helpfulcampfires$handleStatusSounds(Level level, BlockPos pos, CampfireBlockEntityMixin mixin, boolean hasPlayersInRange, long currentTime) {
    if (hasPlayersInRange == mixin.helpfulcampfires$wasActive || currentTime - mixin.helpfulcampfires$lastStatusChange <= GRACE_PERIOD_TICKS) {
      return;
    }

    level.playSound(null, pos, hasPlayersInRange ? SoundEvents.CONDUIT_ACTIVATE : SoundEvents.CONDUIT_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);

    mixin.helpfulcampfires$lastStatusChange = currentTime;
    mixin.helpfulcampfires$wasActive = hasPlayersInRange;
  }

  /* Plays ambient sounds on the campfire */
  @Unique
  private static void helpfulcampfires$handleAmbientSounds(Level level, BlockPos pos, CampfireBlockEntityMixin mixin, long currentTime) {
    // Regular ambient sound
    if (currentTime % AMBIENT_SOUND_INTERVAL == 0) {
      level.playSound(null, pos, SoundEvents.CONDUIT_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    // Random short ambient sounds
    if (currentTime > mixin.helpfulcampfires$nextAmbientSound) {
      mixin.helpfulcampfires$nextAmbientSound = currentTime + MIN_SHORT_AMBIENT_DELAY + (long) level.getRandom().nextInt(MAX_EXTRA_AMBIENT_DELAY);
      level.playSound(null, pos, SoundEvents.CONDUIT_AMBIENT_SHORT, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
  }

  /* Handles application and removal of effects on players entering and leaving campfire area */
  @Unique
  private static boolean helpfulcampfires$checkAndHandleEffects(Level level, BlockPos pos) {
    AABB effectArea = new AABB(pos).inflate(EFFECT_REMOVAL_RADIUS);
    List<Player> players = level.getEntitiesOfClass(Player.class, effectArea);
    if (players.isEmpty()) {
      return false;
    }

    boolean hasPlayersInRange = false;
    for (Player player : players) {
      double distance = pos.distSqr(player.blockPosition());

      if (distance <= EFFECT_RADIUS * EFFECT_RADIUS) {
        hasPlayersInRange = true;
        if (!player.hasEffect(MobEffects.REGENERATION)) {
          player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, true, false));
        }
      } else if (distance <= EFFECT_REMOVAL_RADIUS * EFFECT_REMOVAL_RADIUS) {
        player.removeEffect(MobEffects.REGENERATION);
      }
    }
    return hasPlayersInRange;
  }
}
