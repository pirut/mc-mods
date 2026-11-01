package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

public record BlockItemStateProperties(Map<String, String> properties) implements TooltipProvider {
   public static final BlockItemStateProperties EMPTY = new BlockItemStateProperties(Map.of());
   public static final Codec<BlockItemStateProperties> CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING)
      .xmap(BlockItemStateProperties::new, BlockItemStateProperties::properties);
   private static final StreamCodec<ByteBuf, Map<String, String>> PROPERTIES_STREAM_CODEC = ByteBufCodecs.map(
      Object2ObjectOpenHashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8
   );
   public static final StreamCodec<ByteBuf, BlockItemStateProperties> STREAM_CODEC = PROPERTIES_STREAM_CODEC.map(
      BlockItemStateProperties::new, BlockItemStateProperties::properties
   );

   public <T extends Comparable<T>> BlockItemStateProperties with(Property<T> p_331215_, T p_331193_) {
      return new BlockItemStateProperties(Util.copyAndPut(this.properties, p_331215_.getName(), p_331215_.getName(p_331193_)));
   }

   public <T extends Comparable<T>> BlockItemStateProperties with(Property<T> p_330873_, BlockState p_330751_) {
      return this.with(p_330873_, p_330751_.getValue(p_330873_));
   }

   @Nullable
   public <T extends Comparable<T>> T get(Property<T> p_332023_) {
      String s = this.properties.get(p_332023_.getName());
      return s == null ? null : p_332023_.getValue(s).orElse(null);
   }

   public BlockState apply(BlockState p_330225_) {
      StateDefinition<Block, BlockState> statedefinition = p_330225_.getBlock().getStateDefinition();

      for (Entry<String, String> entry : this.properties.entrySet()) {
         Property<?> property = statedefinition.getProperty(entry.getKey());
         if (property != null) {
            p_330225_ = updateState(p_330225_, property, entry.getValue());
         }
      }

      return p_330225_;
   }

   private static <T extends Comparable<T>> BlockState updateState(BlockState p_331833_, Property<T> p_331585_, String p_331923_) {
      return p_331585_.getValue(p_331923_).map(p_360053_ -> p_331833_.setValue(p_331585_, p_360053_)).orElse(p_331833_);
   }

   public boolean isEmpty() {
      return this.properties.isEmpty();
   }

   @Override
   public void addToTooltip(Item.TooltipContext p_399910_, Consumer<Component> p_399710_, TooltipFlag p_399913_, DataComponentGetter p_399833_) {
      Integer integer = this.get(BeehiveBlock.HONEY_LEVEL);
      if (integer != null) {
         p_399710_.accept(Component.translatable("container.beehive.honey", integer, 5).withStyle(ChatFormatting.GRAY));
      }
   }
}
