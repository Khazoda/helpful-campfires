package com.khazoda.helpfulcampfires;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import javax.swing.text.html.parser.Entity;

public class EmptyEntityRenderer extends EntityRenderer<net.minecraft.world.entity.Entity> {
  public EmptyEntityRenderer(EntityRendererProvider.Context context) {
    super(context);
  }

  @Override
  public ResourceLocation getTextureLocation(net.minecraft.world.entity.Entity entity) {
    return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/stone.png");
  }

  @Override
  public void render(net.minecraft.world.entity.Entity p_entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
  }
}