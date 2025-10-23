package com.khazoda.helpfulcampfires.mixin;

import com.khazoda.helpfulcampfires.HelpfulCampfiresCommon;
import com.khazoda.helpfulcampfires.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  @Unique private static long helpfulcampfires$lastGlobalHearingCheck = 0L;

  @Unique private boolean helpfulcampfires$wasActive = false;
  @Unique private long helpfulcampfires$lastStatusChange = 0L;
  @Unique private long helpfulcampfires$nextAmbientSound = 0L;
  @Unique private long helpfulcampfires$firstLitTime = 0L;
  @Unique private long helpfulcampfires$nextAmbientFire = 0L;
  @Unique private Holder<MobEffect> helpfulcampfires$cachedEffectType = null;
  @Unique private int helpfulcampfires$cachedLightLevel = -1;
  @Unique private static final Map<Player, BlockPos> helpfulcampfires$playerCampfireMap = new HashMap<>();

  @Inject(method = "cookTick", at = @At("TAIL"))
  private static void helpfulcampfires$runEveryTick(Level level, BlockPos pos, BlockState state, CampfireBlockEntity blockEntity, CallbackInfo ci) {
    if (level.isClientSide() || !state.getValue(CampfireBlock.LIT)) return;

    CampfireBlockEntityMixin mixin = (CampfireBlockEntityMixin) (Object) blockEntity;
    if (mixin == null) return;

    Player closestPlayer = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 32, false);
    if (closestPlayer == null) return;

    BlockPos closestCampfire = helpfulcampfires$findClosestBlockInHearingRange(level, closestPlayer, CampfireBlock.class, 32);
    if (closestCampfire == null || !closestCampfire.equals(pos)) return;

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
      this.helpfulcampfires$cachedEffectType = (currentLightLevel == 10) ? MobEffects.JUMP : MobEffects.REGENERATION;
    }
  }

  @Unique
  private static void helpfulcampfires$handleStatusSounds(Level level, BlockPos pos, CampfireBlockEntityMixin mixin, boolean hasPlayersInRange, long currentTime) {
    if (hasPlayersInRange == mixin.helpfulcampfires$wasActive || currentTime - mixin.helpfulcampfires$lastStatusChange <= GRACE_PERIOD_TICKS)
      return;

    level.playSound(null, pos, hasPlayersInRange ? SoundRegistry.SWELL_IN.get() : SoundRegistry.SWELL_OUT.get(), SoundSource.BLOCKS, 0.25F, 1.0F);
    mixin.helpfulcampfires$lastStatusChange = currentTime;
    mixin.helpfulcampfires$wasActive = hasPlayersInRange;
  }

  @Unique
  private static void helpfulcampfires$handleAmbientSounds(Level level, BlockPos pos, CampfireBlockEntityMixin mixin, long currentTime) {

    if (level instanceof ServerLevel serverLevel) {
      if (level.getRandom().nextFloat() < 0.11F) {
        if (mixin.helpfulcampfires$cachedEffectType == MobEffects.REGENERATION) {
          // Normal campfire
          double x = pos.getX() + 0.5;
          double y = pos.getY() + 0.8;
          double z = pos.getZ() + 0.5;
          serverLevel.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0.2, 0.2, 0.2, 0.025);
        } else {
          // Soul campfire
          double x = pos.getX() + 0.5;
          double y = pos.getY() + 0.8;
          double z = pos.getZ() + 0.5;
          serverLevel.sendParticles(ParticleTypes.SOUL, x, y, z, 1, 0.3, 0.2, 0.3, 0.02);
        }
      }
    }

    if (currentTime - mixin.helpfulcampfires$firstLitTime < INITIAL_SOUND_DELAY / 2) return; //Short delay before fire crackle begins
    if (currentTime >= mixin.helpfulcampfires$nextAmbientFire) {
      level.playSound(null, pos, SoundRegistry.FIRE_CRACKLING.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
      mixin.helpfulcampfires$nextAmbientFire = currentTime + 180;
    }

    if (currentTime - mixin.helpfulcampfires$firstLitTime < INITIAL_SOUND_DELAY) return; //Normal delay before owl can hoot
    if (currentTime > mixin.helpfulcampfires$nextAmbientSound && level.getGameTime() > 13000) {
      mixin.helpfulcampfires$nextAmbientSound = currentTime + MIN_SHORT_AMBIENT_DELAY + (long) level.getRandom().nextInt(MAX_EXTRA_AMBIENT_DELAY);
      BlockPos soundPos = pos.offset(level.getRandom().nextIntBetweenInclusive(-10, 10), level.getRandom().nextIntBetweenInclusive(-10, 10), level.getRandom().nextIntBetweenInclusive(-10, 10));
      level.playSound(null, soundPos, SoundRegistry.OWL_Hooting.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
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

  @Unique
  private static BlockPos helpfulcampfires$findClosestBlockInHearingRange(Level level, Player player, Class<?> blockClass, int hearingRadius) {
    long currentTime = level.getGameTime();
    if (currentTime - helpfulcampfires$lastGlobalHearingCheck > 40) {
      helpfulcampfires$lastGlobalHearingCheck = currentTime;

      // Clean up map to prevent memory leaks
      helpfulcampfires$playerCampfireMap.entrySet().removeIf(entry ->
          entry.getKey().isRemoved() || !entry.getKey().isAlive()
      );

      BlockPos currentClosest = helpfulcampfires$playerCampfireMap.get(player);
      BlockPos playerPos = player.blockPosition();

      if (currentClosest != null && playerPos.distSqr(currentClosest) <= EFFECT_RADIUS_SQ) {
        return currentClosest;
      }

      BlockPos closestPos = null;
      double closestDistanceSq = Double.MAX_VALUE;
      BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

      for (int x = -hearingRadius; x <= hearingRadius; x++) {
        for (int y = -hearingRadius; y <= hearingRadius; y++) {
          for (int z = -hearingRadius; z <= hearingRadius; z++) {
            checkPos.set(playerPos.getX() + x, playerPos.getY() + y, playerPos.getZ() + z);
            BlockState blockState = level.getBlockState(checkPos);
            if (blockClass.isInstance(blockState.getBlock()) && blockState.getValue(CampfireBlock.LIT)) {
              double distanceSq = playerPos.distSqr(checkPos);
              if (distanceSq < closestDistanceSq) {
                closestDistanceSq = distanceSq;
                closestPos = checkPos.immutable();
              }
            }
          }
        }
      }
      helpfulcampfires$playerCampfireMap.put(player, closestPos);
    }
    return helpfulcampfires$playerCampfireMap.get(player);
  }

  @Inject(method = "dowse", at = @At("HEAD"))
  private void helpfulcampfires$onDowse(CallbackInfo ci) {
    this.helpfulcampfires$firstLitTime = 0L;
    this.helpfulcampfires$nextAmbientFire = 0L;
    this.helpfulcampfires$wasActive = false;
  }
}