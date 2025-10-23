package com.khazoda.helpfulcampfires.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StumpChairBlock extends Block {
  public static final Properties defaultProperties = Properties.of().sound(SoundType.WOOD).strength(0.1f).noOcclusion().pushReaction(PushReaction.DESTROY);

  public StumpChairBlock() {
    super(defaultProperties);
  }

  @Override
  protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    VoxelShape shape = Shapes.empty();
    shape = Shapes.or(shape, Shapes.create(0.25, 0, 0.25, 0.75, 0.5, 0.75));
    return shape;
  }

}
