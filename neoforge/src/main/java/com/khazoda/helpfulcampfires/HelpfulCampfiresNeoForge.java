package com.khazoda.helpfulcampfires;


import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Constants.MOD_ID)
public class HelpfulCampfiresNeoForge {

  public HelpfulCampfiresNeoForge(IEventBus eventBus) {
    HelpfulCampfiresCommon.init();
    eventBus.addListener(this::onRegister);
  }

  private void onRegister(RegisterEvent event) {
    HelpfulCampfiresCommon.REGISTRARS.register(event.getRegistry());
  }
}