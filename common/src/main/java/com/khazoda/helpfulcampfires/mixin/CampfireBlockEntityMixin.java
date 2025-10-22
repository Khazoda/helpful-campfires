package com.khazoda.helpfulcampfires.mixin;

import com.khazoda.helpfulcampfires.HelpfulCampfiresMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
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
  @Unique private static final int EFFECT_RADIUS = 4;
  @Unique private static final int EFFECT_REMOVAL_RADIUS = EFFECT_RADIUS + 5;
  @Unique private static final int GRACE_PERIOD_TICKS = 10;
  @Unique private static final int MIN_SHORT_AMBIENT_DELAY = 500;
  @Unique private static final int MAX_EXTRA_AMBIENT_DELAY = 100;
  @Unique private static final int INITIAL_SOUND_DELAY = 100;

  @Unique private static final int EFFECT_RADIUS_SQ = EFFECT_RADIUS * EFFECT_RADIUS;
  @Unique private static final int EFFECT_REMOVAL_RADIUS_SQ = EFFECT_REMOVAL_RADIUS * EFFECT_REMOVAL_RADIUS;

  @Unique private boolean helpfulcampfires$wasActive = false;
  @Unique private long helpfulcampfires$lastStatusChange = 0L;
  @Unique private long helpfulcampfires$nextAmbientSound = 0L;
  @Unique private long helpfulcampfires$firstLitTime = 0L;
  @Unique private long helpfulcampfires$nextAmbientFire = 0L;
  @Unique private Holder<MobEffect> helpfulcampfires$cachedEffectType = null;
  @Unique private int helpfulcampfires$cachedLightLevel = -1;

  @Inject(method = "cookTick", at = @At("TAIL"))
  private static void helpfulcampfires$runEveryTick(Level level, BlockPos pos, BlockState state, CampfireBlockEntity blockEntity, CallbackInfo ci) {
    if (level.isClientSide() || !state.getValue(CampfireBlock.LIT)) return;

    CampfireBlockEntityMixin mixin = (CampfireBlockEntityMixin) (Object) blockEntity;
    if (mixin == null) return;

    long currentTime = level.getGameTime();
    if (mixin.helpfulcampfires$firstLitTime == 0L)
      mixin.helpfulcampfires$firstLitTime = currentTime;

    if (mixin.helpfulcampfires$cachedEffectType == null)
      mixin.helpfulcampfires$determineCampfireType(state);
    boolean hasPlayersInRange = mixin.helpfulcampfires$checkAndHandleEffects(level, pos, mixin.helpfulcampfires$cachedEffectType);

    helpfulcampfires$handleStatusSounds(level, pos, mixin, hasPlayersInRange, currentTime);
    if (hasPlayersInRange) helpfulcampfires$handleAmbientSounds(level, pos, mixin, currentTime);
  }

  @Unique
  private void helpfulcampfires$determineCampfireType(BlockState state) {
    int currentLightLevel = state.getLightEmission();
    if (this.helpfulcampfires$cachedEffectType == null || currentLightLevel != this.helpfulcampfires$cachedLightLevel) {
      this.helpfulcampfires$cachedLightLevel = currentLightLevel;
      /* Soul Campfires have light level 10, normal have 15 */
      this.helpfulcampfires$cachedEffectType = (currentLightLevel == 10) ? MobEffects.JUMP : MobEffects.REGENERATION;
    }
  }

  @Unique
  private static void helpfulcampfires$handleStatusSounds(Level level, BlockPos pos, CampfireBlockEntityMixin mixin, boolean hasPlayersInRange, long currentTime) {
    if (hasPlayersInRange == mixin.helpfulcampfires$wasActive || currentTime - mixin.helpfulcampfires$lastStatusChange <= GRACE_PERIOD_TICKS)
      return;

    level.playSound(null, pos, hasPlayersInRange ? HelpfulCampfiresMod.SWELL_IN.get() : HelpfulCampfiresMod.SWELL_OUT.get(), SoundSource.BLOCKS, 0.4F, 1.0F);
    mixin.helpfulcampfires$lastStatusChange = currentTime;
    mixin.helpfulcampfires$wasActive = hasPlayersInRange;
  }

  @Unique
  private static void helpfulcampfires$handleAmbientSounds(Level level, BlockPos pos, CampfireBlockEntityMixin mixin, long currentTime) {
    if (currentTime - mixin.helpfulcampfires$firstLitTime < INITIAL_SOUND_DELAY) return;

    if (currentTime >= mixin.helpfulcampfires$nextAmbientFire) {
      level.playSound(null, pos, HelpfulCampfiresMod.FIRE_CRACKLING.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
      mixin.helpfulcampfires$nextAmbientFire = currentTime + 180; // 9 seconds in ticks
    }

    if (currentTime > mixin.helpfulcampfires$nextAmbientSound && level.getGameTime() > 13000) {
      mixin.helpfulcampfires$nextAmbientSound = currentTime + MIN_SHORT_AMBIENT_DELAY + (long) level.getRandom().nextInt(MAX_EXTRA_AMBIENT_DELAY);
      BlockPos soundPos = pos.offset(level.getRandom().nextIntBetweenInclusive(-10, 10), level.getRandom().nextIntBetweenInclusive(-10, 10), level.getRandom().nextIntBetweenInclusive(-10, 10));
      level.playSound(null, soundPos, HelpfulCampfiresMod.OWL_Hooting.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
    }
  }

  @Unique
  private boolean helpfulcampfires$checkAndHandleEffects(Level level, BlockPos pos, Holder<MobEffect> effectType) {
    AABB effectArea = new AABB(pos).inflate(EFFECT_REMOVAL_RADIUS);
    List<Player> players = level.getEntitiesOfClass(Player.class, effectArea);
    if (players.isEmpty()) return false;

    boolean hasPlayersInRange = false;
    for (Player player : players) {
      double distanceSq = pos.distSqr(player.blockPosition());
      if (distanceSq <= EFFECT_RADIUS_SQ) {
        hasPlayersInRange = true;
        if (!player.hasEffect(effectType)) {
          player.addEffect(new MobEffectInstance(effectType, 100, 1, true, false));
        }
      } else if (distanceSq <= EFFECT_REMOVAL_RADIUS_SQ) {
        player.removeEffect(effectType);
      }
    }
    return hasPlayersInRange;
  }

  @Inject(method = "dowse", at = @At("HEAD"))
  private void helpfulcampfires$onDowse(CallbackInfo ci) {
    this.helpfulcampfires$firstLitTime = 0L;
    this.helpfulcampfires$nextAmbientFire = 0L;
    this.helpfulcampfires$wasActive = false;
  }
}