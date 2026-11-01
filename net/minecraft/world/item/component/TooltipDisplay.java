package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSortedSets;
import java.util.List;
import java.util.SequencedSet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record TooltipDisplay(boolean hideTooltip, SequencedSet<DataComponentType<?>> hiddenComponents) {
   private static final Codec<SequencedSet<DataComponentType<?>>> COMPONENT_SET_CODEC = DataComponentType.CODEC
      .listOf()
      .xmap(ReferenceLinkedOpenHashSet::new, List::copyOf);
   public static final Codec<TooltipDisplay> CODEC = RecordCodecBuilder.create(
      p_399586_ -> p_399586_.group(
            Codec.BOOL.optionalFieldOf("hide_tooltip", false).forGetter(TooltipDisplay::hideTooltip),
            COMPONENT_SET_CODEC.optionalFieldOf("hidden_components", ReferenceSortedSets.emptySet()).forGetter(TooltipDisplay::hiddenComponents)
         )
         .apply(p_399586_, TooltipDisplay::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, TooltipDisplay> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL,
      TooltipDisplay::hideTooltip,
      DataComponentType.STREAM_CODEC.apply(ByteBufCodecs.collection(ReferenceLinkedOpenHashSet::new)),
      TooltipDisplay::hiddenComponents,
      TooltipDisplay::new
   );
   public static final TooltipDisplay DEFAULT = new TooltipDisplay(false, ReferenceSortedSets.emptySet());

   public TooltipDisplay withHidden(DataComponentType<?> p_400224_, boolean p_400307_) {
      if (this.hiddenComponents.contains(p_400224_) == p_400307_) {
         return this;
      } else {
         SequencedSet<DataComponentType<?>> sequencedset = new ReferenceLinkedOpenHashSet(this.hiddenComponents);
         if (p_400307_) {
            sequencedset.add(p_400224_);
         } else {
            sequencedset.remove(p_400224_);
         }

         return new TooltipDisplay(this.hideTooltip, sequencedset);
      }
   }

   public boolean shows(DataComponentType<?> p_399593_) {
      return !this.hideTooltip && !this.hiddenComponents.contains(p_399593_);
   }
}
