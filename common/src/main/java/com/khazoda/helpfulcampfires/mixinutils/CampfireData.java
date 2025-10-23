package com.khazoda.helpfulcampfires.mixinutils;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;

public class CampfireData {
  public long firstLitTime = 0L;
  public long nextAmbientSound = 0L;
  public long nextAmbientFire = 0L;
  public long lastStatusChange = 0L;
  public boolean wasActive = false;
  public Holder<MobEffect> effectType = null;
  public int lightLevel = -1;
}