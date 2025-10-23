package com.khazoda.helpfulcampfires;

import com.khazoda.helpfulcampfires.platform.Services;
import com.khazoda.helpfulcampfires.registry.MainRegistry;
import com.khazoda.helpfulcampfires.registry.helper.Reginald;

public class HelpfulCampfiresCommon {
  public static final Reginald REGISTRARS = new Reginald();

  public static void init() {
    MainRegistry.init();
    if (Services.PLATFORM.isModLoaded("helpfulcampfires"))
      Constants.LOG.info("- Helpful Campfires Loaded -");
  }
}