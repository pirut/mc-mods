package net.minecraft.world.item;

import com.google.common.collect.ImmutableBiMap;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopperBlocks;

public record WeatheringCopperItems(
   Item unaffected, Item exposed, Item weathered, Item oxidized, Item waxed, Item waxedExposed, Item waxedWeathered, Item waxedOxidized
) {
   public static WeatheringCopperItems create(WeatheringCopperBlocks p_436628_, Function<Block, Item> p_436717_) {
      return new WeatheringCopperItems(
         p_436717_.apply(p_436628_.unaffected()),
         p_436717_.apply(p_436628_.exposed()),
         p_436717_.apply(p_436628_.weathered()),
         p_436717_.apply(p_436628_.oxidized()),
         p_436717_.apply(p_436628_.waxed()),
         p_436717_.apply(p_436628_.waxedExposed()),
         p_436717_.apply(p_436628_.waxedWeathered()),
         p_436717_.apply(p_436628_.waxedOxidized())
      );
   }

   public ImmutableBiMap<Item, Item> waxedMapping() {
      return ImmutableBiMap.of(
         this.unaffected, this.waxed, this.exposed, this.waxedExposed, this.weathered, this.waxedWeathered, this.oxidized, this.waxedOxidized
      );
   }

   public void forEach(Consumer<Item> p_436728_) {
      p_436728_.accept(this.unaffected);
      p_436728_.accept(this.exposed);
      p_436728_.accept(this.weathered);
      p_436728_.accept(this.oxidized);
      p_436728_.accept(this.waxed);
      p_436728_.accept(this.waxedExposed);
      p_436728_.accept(this.waxedWeathered);
      p_436728_.accept(this.waxedOxidized);
   }
}
