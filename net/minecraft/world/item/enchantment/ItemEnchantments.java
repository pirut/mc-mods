package net.minecraft.world.item.enchantment;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import org.jspecify.annotations.Nullable;

public class ItemEnchantments implements TooltipProvider {
   public static final ItemEnchantments EMPTY = new ItemEnchantments(new Object2IntOpenHashMap());
   private static final Codec<Integer> LEVEL_CODEC = Codec.intRange(1, 255);
   public static final Codec<ItemEnchantments> CODEC = Codec.unboundedMap(Enchantment.CODEC, LEVEL_CODEC)
      .xmap(p_399435_ -> new ItemEnchantments(new Object2IntOpenHashMap(p_399435_)), p_340785_ -> p_340785_.enchantments);
   public static final StreamCodec<RegistryFriendlyByteBuf, ItemEnchantments> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.map(Object2IntOpenHashMap::new, Enchantment.STREAM_CODEC, ByteBufCodecs.VAR_INT),
      p_340784_ -> p_340784_.enchantments,
      ItemEnchantments::new
   );
   final Object2IntOpenHashMap<Holder<Enchantment>> enchantments;

   ItemEnchantments(Object2IntOpenHashMap<Holder<Enchantment>> p_341287_) {
      this.enchantments = p_341287_;
      ObjectIterator var2 = p_341287_.object2IntEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<Holder<Enchantment>> entry = (Entry<Holder<Enchantment>>)var2.next();
         int i = entry.getIntValue();
         if (i < 0 || i > 255) {
            throw new IllegalArgumentException("Enchantment " + entry.getKey() + " has invalid level " + i);
         }
      }
   }

   public int getLevel(Holder<Enchantment> p_345693_) {
      return this.enchantments.getInt(p_345693_);
   }

   @Override
   public void addToTooltip(Item.TooltipContext p_341290_, Consumer<Component> p_331119_, TooltipFlag p_330400_, DataComponentGetter p_400004_) {
      HolderLookup.Provider holderlookup$provider = p_341290_.registries();
      HolderSet<Enchantment> holderset = getTagOrEmpty(holderlookup$provider, Registries.ENCHANTMENT, EnchantmentTags.TOOLTIP_ORDER);

      for (Holder<Enchantment> holder : holderset) {
         int i = this.enchantments.getInt(holder);
         if (i > 0) {
            p_331119_.accept(Enchantment.getFullname(holder, i));
         }
      }

      ObjectIterator var10 = this.enchantments.object2IntEntrySet().iterator();

      while (var10.hasNext()) {
         Entry<Holder<Enchantment>> entry = (Entry<Holder<Enchantment>>)var10.next();
         Holder<Enchantment> holder1 = (Holder<Enchantment>)entry.getKey();
         if (!holderset.contains(holder1)) {
            p_331119_.accept(Enchantment.getFullname((Holder<Enchantment>)entry.getKey(), entry.getIntValue()));
         }
      }
   }

   private static <T> HolderSet<T> getTagOrEmpty(@Nullable HolderLookup.Provider p_341186_, ResourceKey<Registry<T>> p_341113_, TagKey<T> p_341409_) {
      if (p_341186_ != null) {
         Optional<HolderSet.Named<T>> optional = p_341186_.lookupOrThrow(p_341113_).get(p_341409_);
         if (optional.isPresent()) {
            return optional.get();
         }
      }

      return HolderSet.direct();
   }

   public Set<Holder<Enchantment>> keySet() {
      return Collections.unmodifiableSet(this.enchantments.keySet());
   }

   public Set<Entry<Holder<Enchantment>>> entrySet() {
      return Collections.unmodifiableSet(this.enchantments.object2IntEntrySet());
   }

   public int size() {
      return this.enchantments.size();
   }

   public boolean isEmpty() {
      return this.enchantments.isEmpty();
   }

   @Override
   public boolean equals(Object p_331697_) {
      if (this == p_331697_) {
         return true;
      } else {
         return p_331697_ instanceof ItemEnchantments itemenchantments ? this.enchantments.equals(itemenchantments.enchantments) : false;
      }
   }

   @Override
   public int hashCode() {
      return this.enchantments.hashCode();
   }

   @Override
   public String toString() {
      return "ItemEnchantments{enchantments=" + this.enchantments + "}";
   }

   public static class Mutable {
      private final Object2IntOpenHashMap<Holder<Enchantment>> enchantments = new Object2IntOpenHashMap();

      public Mutable(ItemEnchantments p_330722_) {
         this.enchantments.putAll(p_330722_.enchantments);
      }

      public void set(Holder<Enchantment> p_344861_, int p_330832_) {
         if (p_330832_ <= 0) {
            this.enchantments.removeInt(p_344861_);
         } else {
            this.enchantments.put(p_344861_, Math.min(p_330832_, 255));
         }
      }

      public void upgrade(Holder<Enchantment> p_346263_, int p_331153_) {
         if (p_331153_ > 0) {
            this.enchantments.merge(p_346263_, Math.min(p_331153_, 255), Integer::max);
         }
      }

      public void removeIf(Predicate<Holder<Enchantment>> p_332079_) {
         this.enchantments.keySet().removeIf(p_332079_);
      }

      public int getLevel(Holder<Enchantment> p_345430_) {
         return this.enchantments.getOrDefault(p_345430_, 0);
      }

      public Set<Holder<Enchantment>> keySet() {
         return this.enchantments.keySet();
      }

      public ItemEnchantments toImmutable() {
         return new ItemEnchantments(this.enchantments);
      }
   }
}
