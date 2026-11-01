package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.apache.commons.lang3.function.TriFunction;

public record WeatheringCopperBlocks(
   Block unaffected, Block exposed, Block weathered, Block oxidized, Block waxed, Block waxedExposed, Block waxedWeathered, Block waxedOxidized
) {
   public static <WaxedBlock extends Block, WeatheringBlock extends Block & WeatheringCopper> WeatheringCopperBlocks create(
      String p_436791_,
      TriFunction<String, Function<BlockBehaviour.Properties, Block>, BlockBehaviour.Properties, Block> p_436798_,
      Function<BlockBehaviour.Properties, WaxedBlock> p_436785_,
      BiFunction<WeatheringCopper.WeatherState, BlockBehaviour.Properties, WeatheringBlock> p_436754_,
      Function<WeatheringCopper.WeatherState, BlockBehaviour.Properties> p_436665_
   ) {
      return new WeatheringCopperBlocks(
         (Block)p_436798_.apply(
            p_436791_,
            (Function<BlockBehaviour.Properties, Block>)p_436799_ -> p_436754_.apply(WeatheringCopper.WeatherState.UNAFFECTED, p_436799_),
            p_436665_.apply(WeatheringCopper.WeatherState.UNAFFECTED)
         ),
         (Block)p_436798_.apply(
            "exposed_" + p_436791_,
            (Function<BlockBehaviour.Properties, Block>)p_436771_ -> p_436754_.apply(WeatheringCopper.WeatherState.EXPOSED, p_436771_),
            p_436665_.apply(WeatheringCopper.WeatherState.EXPOSED)
         ),
         (Block)p_436798_.apply(
            "weathered_" + p_436791_,
            (Function<BlockBehaviour.Properties, Block>)p_436712_ -> p_436754_.apply(WeatheringCopper.WeatherState.WEATHERED, p_436712_),
            p_436665_.apply(WeatheringCopper.WeatherState.WEATHERED)
         ),
         (Block)p_436798_.apply(
            "oxidized_" + p_436791_,
            (Function<BlockBehaviour.Properties, Block>)p_436766_ -> p_436754_.apply(WeatheringCopper.WeatherState.OXIDIZED, p_436766_),
            p_436665_.apply(WeatheringCopper.WeatherState.OXIDIZED)
         ),
         (Block)p_436798_.apply("waxed_" + p_436791_, p_436785_::apply, p_436665_.apply(WeatheringCopper.WeatherState.UNAFFECTED)),
         (Block)p_436798_.apply("waxed_exposed_" + p_436791_, p_436785_::apply, p_436665_.apply(WeatheringCopper.WeatherState.EXPOSED)),
         (Block)p_436798_.apply("waxed_weathered_" + p_436791_, p_436785_::apply, p_436665_.apply(WeatheringCopper.WeatherState.WEATHERED)),
         (Block)p_436798_.apply("waxed_oxidized_" + p_436791_, p_436785_::apply, p_436665_.apply(WeatheringCopper.WeatherState.OXIDIZED))
      );
   }

   public ImmutableBiMap<Block, Block> weatheringMapping() {
      return ImmutableBiMap.of(this.unaffected, this.exposed, this.exposed, this.weathered, this.weathered, this.oxidized);
   }

   public ImmutableBiMap<Block, Block> waxedMapping() {
      return ImmutableBiMap.of(
         this.unaffected, this.waxed, this.exposed, this.waxedExposed, this.weathered, this.waxedWeathered, this.oxidized, this.waxedOxidized
      );
   }

   public ImmutableList<Block> asList() {
      return ImmutableList.of(
         this.unaffected, this.waxed, this.exposed, this.waxedExposed, this.weathered, this.waxedWeathered, this.oxidized, this.waxedOxidized
      );
   }

   public void forEach(Consumer<Block> p_436775_) {
      p_436775_.accept(this.unaffected);
      p_436775_.accept(this.exposed);
      p_436775_.accept(this.weathered);
      p_436775_.accept(this.oxidized);
      p_436775_.accept(this.waxed);
      p_436775_.accept(this.waxedExposed);
      p_436775_.accept(this.waxedWeathered);
      p_436775_.accept(this.waxedOxidized);
   }
}
