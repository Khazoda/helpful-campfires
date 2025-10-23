package com.khazoda.helpfulcampfires;

import net.fabricmc.api.ModInitializer;

public class HelpfulCampfiresFabric implements ModInitializer {

  @Override
  public void onInitialize() {
    HelpfulCampfiresCommon.init();
    HelpfulCampfiresCommon.REGISTRARS.registerAll();
  }
}
