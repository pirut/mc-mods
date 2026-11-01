package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;

public class FallenTreeConfiguration implements FeatureConfiguration {
   public static final Codec<FallenTreeConfiguration> CODEC = RecordCodecBuilder.create(
      p_409742_ -> p_409742_.group(
            BlockStateProvider.CODEC.fieldOf("trunk_provider").forGetter(p_410077_ -> p_410077_.trunkProvider),
            IntProvider.codec(0, 16).fieldOf("log_length").forGetter(p_410036_ -> p_410036_.logLength),
            TreeDecorator.CODEC.listOf().fieldOf("stump_decorators").forGetter(p_409945_ -> p_409945_.stumpDecorators),
            TreeDecorator.CODEC.listOf().fieldOf("log_decorators").forGetter(p_410827_ -> p_410827_.logDecorators)
         )
         .apply(p_409742_, FallenTreeConfiguration::new)
   );
   public final BlockStateProvider trunkProvider;
   public final IntProvider logLength;
   public final List<TreeDecorator> stumpDecorators;
   public final List<TreeDecorator> logDecorators;

   protected FallenTreeConfiguration(BlockStateProvider p_410206_, IntProvider p_410877_, List<TreeDecorator> p_410779_, List<TreeDecorator> p_409621_) {
      this.trunkProvider = p_410206_;
      this.logLength = p_410877_;
      this.stumpDecorators = p_410779_;
      this.logDecorators = p_409621_;
   }

   public static class FallenTreeConfigurationBuilder {
      private final BlockStateProvider trunkProvider;
      private final IntProvider logLength;
      private List<TreeDecorator> stumpDecorators = new ArrayList<>();
      private List<TreeDecorator> logDecorators = new ArrayList<>();

      public FallenTreeConfigurationBuilder(BlockStateProvider p_409863_, IntProvider p_409902_) {
         this.trunkProvider = p_409863_;
         this.logLength = p_409902_;
      }

      public FallenTreeConfiguration.FallenTreeConfigurationBuilder stumpDecorators(List<TreeDecorator> p_409939_) {
         this.stumpDecorators = p_409939_;
         return this;
      }

      public FallenTreeConfiguration.FallenTreeConfigurationBuilder logDecorators(List<TreeDecorator> p_410282_) {
         this.logDecorators = p_410282_;
         return this;
      }

      public FallenTreeConfiguration build() {
         return new FallenTreeConfiguration(this.trunkProvider, this.logLength, this.stumpDecorators, this.logDecorators);
      }
   }
}
