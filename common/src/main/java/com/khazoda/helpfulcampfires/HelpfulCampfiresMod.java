package com.khazoda.helpfulcampfires;

import com.khazoda.helpfulcampfires.platform.Services;
import com.khazoda.helpfulcampfires.registry.Reggie;
import com.khazoda.helpfulcampfires.registry.Reginald;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

import static com.khazoda.helpfulcampfires.Constants.ID;

public class HelpfulCampfiresMod {
  public static final Reginald REGISTRARS = new Reginald();
  private static final Reggie<SoundEvent> SOUND_REGISTRAR = REGISTRARS.get(Registries.SOUND_EVENT);

  public static final Supplier<SoundEvent> FIRE_CRACKLING = SOUND_REGISTRAR.register("fire_crackling", () -> SoundEvent.createVariableRangeEvent(ID("fire_crackling")));
  public static final Supplier<SoundEvent> OWL_Hooting = SOUND_REGISTRAR.register("owl_hooting", () -> SoundEvent.createVariableRangeEvent(ID("owl_hooting")));
  public static final Supplier<SoundEvent> SWELL_IN = SOUND_REGISTRAR.register("swell_in", () -> SoundEvent.createVariableRangeEvent(ID("swell_in")));
  public static final Supplier<SoundEvent> SWELL_OUT = SOUND_REGISTRAR.register("swell_out", () -> SoundEvent.createVariableRangeEvent(ID("swell_out")));

  public static void init() {
    if (Services.PLATFORM.isModLoaded("helpfulcampfires")) Constants.LOG.info("- Helpful Campfires Loaded -");
  }
}