package com.khazoda.helpfulcampfires.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class ChairEntity extends Entity {

  public ChairEntity(EntityType<?> type, Level level) {
    super(type, level);
    this.noPhysics = true;
  }

  private int ticksExisted = 0;

  @Override
  public void tick() {
    super.tick();
    if (this.isRemoved()) return;
    ticksExisted++;
    if (ticksExisted == 2 && this.getPassengers().isEmpty()) {
      AABB area = new AABB(this.blockPosition()).inflate(2);
      List<Player> players = this.level().getEntitiesOfClass(Player.class, area);
      for (Player player : players) {
        if (!player.isPassenger() && player.distanceTo(this) < 1.5) {
          player.startRiding(this, true);
          break;
        }
      }
    }
    // Remove entity if  no passengers after a second
    if (ticksExisted > 20 && this.getPassengers().isEmpty()) {
      this.discard();
    }
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
  }

  @Override
  protected void readAdditionalSaveData(CompoundTag compound) {
  }

  @Override
  protected void addAdditionalSaveData(CompoundTag compound) {
  }
}