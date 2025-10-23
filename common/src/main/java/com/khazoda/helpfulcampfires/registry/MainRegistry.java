package com.khazoda.helpfulcampfires.registry;

import com.khazoda.helpfulcampfires.HelpfulCampfiresCommon;
import com.khazoda.helpfulcampfires.block.StumpChairBlock;
import com.khazoda.helpfulcampfires.registry.helper.Reggie;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public class MainRegistry {
  private static final Reggie<Block> BLOCK_REGISTRAR = HelpfulCampfiresCommon.REGISTRARS.get(Registries.BLOCK);
  private static final Reggie<Item> ITEM_REGISTRAR = HelpfulCampfiresCommon.REGISTRARS.get(Registries.ITEM);

  public static final Supplier<Block> STUMP_CHAIR_BLOCK = BLOCK_REGISTRAR.register("stump_chair", StumpChairBlock::new);
  public static final Supplier<Item> STUMP_CHAIR_ITEM = ITEM_REGISTRAR.register("stump_chair", () -> new BlockItem(STUMP_CHAIR_BLOCK.get(), new Item.Properties()));

  @SuppressWarnings("unused")
  public static final Supplier<CreativeModeTab> HELPFUL_CAMPFIRES_TAB = HelpfulCampfiresCommon.REGISTRARS.get(Registries.CREATIVE_MODE_TAB).register("main",
      () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0).title(Component.translatable("itemGroup.helpfulcampfires.main"))
          .icon(() -> STUMP_CHAIR_ITEM.get().getDefaultInstance()).displayItems((parameters, output) -> {
            output.accept(STUMP_CHAIR_ITEM.get());
          }).build());

  public static void init() {

  }
}
