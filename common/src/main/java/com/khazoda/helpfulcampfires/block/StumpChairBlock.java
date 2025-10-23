package com.khazoda.helpfulcampfires.block;

import com.khazoda.helpfulcampfires.entity.ChairEntity;
import com.khazoda.helpfulcampfires.registry.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class StumpChairBlock extends Block {
  public static final Properties defaultProperties = Properties.of().sound(SoundType.WOOD).strength(0.4f).noOcclusion().pushReaction(PushReaction.DESTROY);

  public StumpChairBlock() {
    super(defaultProperties);
  }

  @Override
  protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
    if (level.isClientSide()) return InteractionResult.SUCCESS;

    AABB searchArea = new AABB(pos).inflate(3);
    // Check if there's already a chair with a mob passenger
    var existingSeats = level.getEntitiesOfClass(ChairEntity.class, searchArea);
    for (ChairEntity seat : existingSeats) {
      if (!seat.getPassengers().isEmpty()) {
        // Eject any mob passengers to make room for the player
        for (Entity passenger : seat.getPassengers()) {
          if (passenger instanceof Mob mob && !mob.isRemoved()) {
            mob.stopRiding();
            mob.setDeltaMovement((mob.getX() - pos.getX()) * 0.5, 0.3, (mob.getZ() - pos.getZ()) * 0.5);
          }
        }
        return player.startRiding(seat, true) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
      }
    }

    // Check for nearby leashed mobs to mount on stump
    List<Mob> leashedMobs = level.getEntitiesOfClass(Mob.class, searchArea, mob -> mob.isLeashed() && mob.isAlive() && !mob.isPassenger());
    if (!leashedMobs.isEmpty()) {
      Mob closestMob = null;
      double closestDistance = Double.MAX_VALUE;
      for (Mob mob : leashedMobs) {
        double distance = mob.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (distance < closestDistance) {
          closestDistance = distance;
          closestMob = mob;
        }
      }

      if (closestMob != null && closestDistance < 16) {
        ChairEntity seat = new ChairEntity(MainRegistry.CHAIR_ENTITY.get(), level);
        seat.setPos(pos.getX() + 0.5, pos.getY() + 0.45, pos.getZ() + 0.5);
        if (!level.addFreshEntity(seat)) {
          return InteractionResult.FAIL;
        }
        if (closestMob.isLeashed()) {
          ItemStack leash = new ItemStack(Items.LEAD);
          closestMob.spawnAtLocation(leash);
          closestMob.dropLeash(true, false);
        }
        if (closestMob.startRiding(seat, true)) {
          return InteractionResult.SUCCESS;
        }
      }
    }

    // Normal player sitting (no mobs to mount or eject)
    if (!player.isPassenger()) {
      ChairEntity seat = new ChairEntity(MainRegistry.CHAIR_ENTITY.get(), level);
      seat.setPos(pos.getX() + 0.5, pos.getY() + 0.45, pos.getZ() + 0.5);
      if (level.addFreshEntity(seat)) {
        return player.startRiding(seat, true) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
      }
    }
    return InteractionResult.PASS;
  }

  @Override
  protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    VoxelShape shape = Shapes.empty();
    shape = Shapes.or(shape, Shapes.create(0.25, 0, 0.25, 0.75, 0.5, 0.75));
    return shape;
  }

}
