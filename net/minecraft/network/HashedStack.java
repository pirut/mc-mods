package net.minecraft.network;

import com.mojang.datafixers.DataFixUtils;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface HashedStack {
   HashedStack EMPTY = new HashedStack() {
      @Override
      public String toString() {
         return "<empty>";
      }

      @Override
      public boolean matches(ItemStack p_412039_, HashedPatchMap.HashGenerator p_412698_) {
         return p_412039_.isEmpty();
      }
   };
   StreamCodec<RegistryFriendlyByteBuf, HashedStack> STREAM_CODEC = ByteBufCodecs.optional(HashedStack.ActualItem.STREAM_CODEC)
      .map(
         p_412528_ -> (HashedStack)DataFixUtils.orElse(p_412528_, EMPTY),
         p_412104_ -> p_412104_ instanceof HashedStack.ActualItem hashedstack$actualitem ? Optional.of(hashedstack$actualitem) : Optional.empty()
      );

   boolean matches(ItemStack var1, HashedPatchMap.HashGenerator var2);

   static HashedStack create(ItemStack p_412136_, HashedPatchMap.HashGenerator p_412663_) {
      return (HashedStack)(p_412136_.isEmpty()
         ? EMPTY
         : new HashedStack.ActualItem(p_412136_.getItemHolder(), p_412136_.getCount(), HashedPatchMap.create(p_412136_.getComponentsPatch(), p_412663_)));
   }

   public record ActualItem(Holder<Item> item, int count, HashedPatchMap components) implements HashedStack {
      public static final StreamCodec<RegistryFriendlyByteBuf, HashedStack.ActualItem> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.holderRegistry(Registries.ITEM),
         HashedStack.ActualItem::item,
         ByteBufCodecs.VAR_INT,
         HashedStack.ActualItem::count,
         HashedPatchMap.STREAM_CODEC,
         HashedStack.ActualItem::components,
         HashedStack.ActualItem::new
      );

      @Override
      public boolean matches(ItemStack p_412763_, HashedPatchMap.HashGenerator p_412137_) {
         if (this.count != p_412763_.getCount()) {
            return false;
         } else {
            return !this.item.equals(p_412763_.getItemHolder()) ? false : this.components.matches(p_412763_.getComponentsPatch(), p_412137_);
         }
      }
   }
}
