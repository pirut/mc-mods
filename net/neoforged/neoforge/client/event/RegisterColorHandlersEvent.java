package net.neoforged.neoforge.client.event;

import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ColorResolver;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public abstract class RegisterColorHandlersEvent extends Event implements IModBusEvent {
   @Internal
   protected RegisterColorHandlersEvent() {
   }

   public static class Block extends RegisterColorHandlersEvent {
      private final BlockColors blockColors;

      @Internal
      public Block(BlockColors blockColors) {
         this.blockColors = blockColors;
      }

      public BlockColors getBlockColors() {
         return this.blockColors;
      }

      public void register(BlockColor blockColor, net.minecraft.world.level.block.Block... blocks) {
         this.blockColors.register(blockColor, blocks);
      }
   }

   public static class ColorResolvers extends RegisterColorHandlersEvent {
      private final Builder<ColorResolver> builder;

      @Internal
      public ColorResolvers(Builder<ColorResolver> builder) {
         this.builder = builder;
      }

      public void register(ColorResolver resolver) {
         this.builder.add(resolver);
      }
   }

   public static class ItemTintSources extends RegisterColorHandlersEvent {
      private final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ItemTintSource>> idMapper;

      @Internal
      public ItemTintSources(ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ItemTintSource>> idMapper) {
         this.idMapper = idMapper;
      }

      public void register(Identifier location, MapCodec<? extends ItemTintSource> source) {
         this.idMapper.put(location, source);
      }
   }
}
