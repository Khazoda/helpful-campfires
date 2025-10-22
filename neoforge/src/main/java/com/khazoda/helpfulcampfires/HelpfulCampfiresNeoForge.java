package com.khazoda.helpfulcampfires;


import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class HelpfulCampfiresNeoForge {

  public HelpfulCampfiresNeoForge(IEventBus eventBus) {
    HelpfulCampfiresMod.init();

  }
}