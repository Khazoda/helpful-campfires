package com.khazoda.helpfulcampfires.mixin;

import com.khazoda.helpfulcampfires.Constants;
import com.khazoda.helpfulcampfires.mixinutils.CampfireData;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(CampfireBlockEntity.class)
public class CampfireBlockEntityMixin {
  @Unique private static final int EFFECT_RADIUS = 4;
  @Unique private static final int EFFECT_REMOVAL_RADIUS = EFFECT_RADIUS + 5;
  @Unique private static final int GRACE_PERIOD_TICKS = 10;
  @Unique private static final int MIN_SHORT_AMBIENT_DELAY = 500;
  @Unique private static final int MAX_EXTRA_AMBIENT_DELAY = 100;
  @Unique private static final int INITIAL_SOUND_DELAY = 100;
  @Unique private static final int HEARING_RADIUS = 8;

  @Unique private static final int EFFECT_RADIUS_SQ = EFFECT_RADIUS * EFFECT_RADIUS;
  @Unique private static final int EFFECT_REMOVAL_RADIUS_SQ = EFFECT_REMOVAL_RADIUS * EFFECT_REMOVAL_RADIUS;

  @Unique private static final Map<BlockPos, CampfireData> helpfulcampfires$campfireData = new ConcurrentHashMap<>();
  @Unique private static final Map<Player, BlockPos> helpfulcampfires$playerCampfireMap = new ConcurrentHashMap<>();

  @Unique private static long helpfulcampfires$lastGlobalHearingCheck = 0L;

  @Inject(method = "cookTick", at = @At("TAIL"))
  private static void helpfulcampfires$runEveryTick(Level level, BlockPos pos, BlockState state, CampfireBlockEntity blockEntity, CallbackInfo ci) {
    if (level.isClientSide() || !state.getValue(CampfireBlock.LIT)) return;

    // Find the closest player to determine if this campfire should be active
    Player closestPlayer = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), HEARING_RADIUS, false);
    if (closestPlayer == null) return;

    // Only activate this campfire if it's the closest one to the player
    BlockPos activeCampfire = helpfulcampfires$findActiveCampfireForPlayer(level, closestPlayer);
    if (!pos.equals(activeCampfire)) return;

    // This campfire is the active one for the closest player - proceed with effects
    CampfireData data = helpfulcampfires$campfireData.computeIfAbsent(pos, k -> new CampfireData());
    helpfulcampfires$updateCampfireType(state, data);
    boolean hasPlayersInRange = helpfulcampfires$checkAndHandleEffects(level, pos, data.effectType);
    long currentTime = level.getGameTime();
    helpfulcampfires$handleCampfireSoundsAndParticles(level, pos, data, hasPlayersInRange, currentTime);
  }

  @Unique
  private static BlockPos helpfulcampfires$findActiveCampfireForPlayer(Level level, Player player) {
    long currentTime = level.getGameTime();

    // Only update the player's campfire mapping every 2s to prevent lag
    if (currentTime - helpfulcampfires$lastGlobalHearingCheck > 40) {
      helpfulcampfires$lastGlobalHearingCheck = currentTime;

      helpfulcampfires$playerCampfireMap.entrySet().removeIf(entry -> entry.getKey().isRemoved() || !entry.getKey().isAlive() || entry.getKey().level() != level // Player changed dimensions, remove them from the map
      );

      BlockPos currentActive = helpfulcampfires$playerCampfireMap.get(player);
      BlockPos playerPos = player.blockPosition();

      // If player is still close to their current active campfire, keep it
      if (currentActive != null && playerPos.distSqr(currentActive) <= EFFECT_RADIUS_SQ) {
        return currentActive;
      }

      // Search in a radius around the player for lit campfires & determine closest
      BlockPos closestCampfire = null;
      double closestDistanceSq = Double.MAX_VALUE;
      BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
      for (int x = -HEARING_RADIUS; x <= HEARING_RADIUS; x++) {
        for (int y = -HEARING_RADIUS; y <= HEARING_RADIUS; y++) {
          for (int z = -HEARING_RADIUS; z <= HEARING_RADIUS; z++) {
            checkPos.set(playerPos.getX() + x, playerPos.getY() + y, playerPos.getZ() + z);
            BlockState blockState = level.getBlockState(checkPos);
            if (blockState.getBlock() instanceof CampfireBlock && blockState.getValue(CampfireBlock.LIT)) {
              double distanceSq = playerPos.distSqr(checkPos);
              if (distanceSq < closestDistanceSq) {
                closestDistanceSq = distanceSq;
                closestCampfire = checkPos.immutable();
              }
            }
          }
        }
      }
      helpfulcampfires$playerCampfireMap.put(player, closestCampfire);
      return closestCampfire;
    }
    // Return cached result
    return helpfulcampfires$playerCampfireMap.get(player);
  }

  @Unique
  private static void helpfulcampfires$updateCampfireType(BlockState state, CampfireData data) {
    int currentLightLevel = state.getLightEmission();
    if (data.effectType == null || currentLightLevel != data.lightLevel) {
      data.lightLevel = currentLightLevel;
      data.effectType = (currentLightLevel == 10) ? MobEffects.JUMP : MobEffects.REGENERATION;
    }
  }

  @Unique
  private static void helpfulcampfires$handleCampfireSoundsAndParticles(Level level, BlockPos pos, CampfireData data, boolean hasPlayersInRange, long currentTime) {
    if (hasPlayersInRange != data.wasActive && currentTime - data.lastStatusChange > GRACE_PERIOD_TICKS) {
      level.playSound(null, pos, hasPlayersInRange ? SoundRegistry.SWELL_IN.get() : SoundRegistry.SWELL_OUT.get(), SoundSource.BLOCKS, 0.25F, 1.0F);
      data.lastStatusChange = currentTime;
      data.wasActive = hasPlayersInRange;
    }

    if (hasPlayersInRange) {
      helpfulcampfires$handleAmbientEffects(level, pos, data, currentTime);
    }
  }

  @Unique
  private static void helpfulcampfires$handleAmbientEffects(Level level, BlockPos pos, CampfireData data, long currentTime) {
    if (level instanceof ServerLevel serverLevel && level.getRandom().nextFloat() < 0.11F) {
      double x = pos.getX() + 0.5;
      double y = pos.getY() + 0.8;
      double z = pos.getZ() + 0.5;

      if (data.effectType == MobEffects.REGENERATION) {
        serverLevel.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0.2, 0.2, 0.2, 0.025);
      } else {
        serverLevel.sendParticles(ParticleTypes.SOUL, x, y, z, 1, 0.3, 0.2, 0.3, 0.02);
      }
    }

    if (currentTime - data.firstLitTime >= INITIAL_SOUND_DELAY / 2 && currentTime >= data.nextAmbientFire) {
      level.playSound(null, pos, SoundRegistry.FIRE_CRACKLING.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
      data.nextAmbientFire = currentTime + 180;
    }

    if (currentTime - data.firstLitTime >= INITIAL_SOUND_DELAY && currentTime > data.nextAmbientSound && level.isNight()) {
      data.nextAmbientSound = currentTime + MIN_SHORT_AMBIENT_DELAY + (long) level.getRandom().nextInt(MAX_EXTRA_AMBIENT_DELAY);
      BlockPos soundPos = pos.offset(level.getRandom().nextIntBetweenInclusive(-10, 10), level.getRandom().nextIntBetweenInclusive(-10, 10), level.getRandom().nextIntBetweenInclusive(-10, 10));
      level.playSound(null, soundPos, SoundRegistry.OWL_Hooting.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
    }
  }

  @Unique
  private static boolean helpfulcampfires$checkAndHandleEffects(Level level, BlockPos pos, Holder<MobEffect> effectType) {
    AABB effectArea = new AABB(pos).inflate(EFFECT_REMOVAL_RADIUS);
    List<Player> players = level.getEntitiesOfClass(Player.class, effectArea);
    if (players.isEmpty()) return false;

    boolean hasPlayersInRange = false;
    for (Player player : players) {
      double distanceSq = pos.distSqr(player.blockPosition());
      if (distanceSq <= EFFECT_RADIUS_SQ && effectType != null) {
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
    BlockPos pos = ((CampfireBlockEntity) (Object) this).getBlockPos();
    helpfulcampfires$campfireData.remove(pos);
    helpfulcampfires$playerCampfireMap.entrySet().removeIf(entry -> pos.equals(entry.getValue()));
  }


}