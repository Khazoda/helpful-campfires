package com.khazoda.helpfulcampfires;

import com.khazoda.helpfulcampfires.registry.MainRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;

public class HelpfulCampfiresFabricClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    EntityRendererRegistry.register(MainRegistry.CHAIR_ENTITY.get(), NoopRenderer::new);
  }
}
