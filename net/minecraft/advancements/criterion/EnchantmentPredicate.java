package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public record EnchantmentPredicate(Optional<HolderSet<Enchantment>> enchantments, MinMaxBounds.Ints level) {
   public static final Codec<EnchantmentPredicate> CODEC = RecordCodecBuilder.create(
      p_467482_ -> p_467482_.group(
            RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("enchantments").forGetter(EnchantmentPredicate::enchantments),
            MinMaxBounds.Ints.CODEC.optionalFieldOf("levels", MinMaxBounds.Ints.ANY).forGetter(EnchantmentPredicate::level)
         )
         .apply(p_467482_, EnchantmentPredicate::new)
   );

   public EnchantmentPredicate(Holder<Enchantment> p_467292_, MinMaxBounds.Ints p_469595_) {
      this(Optional.of(HolderSet.direct(p_467292_)), p_469595_);
   }

   public EnchantmentPredicate(HolderSet<Enchantment> p_468396_, MinMaxBounds.Ints p_468943_) {
      this(Optional.of(p_468396_), p_468943_);
   }

   public boolean containedIn(ItemEnchantments p_467874_) {
      if (this.enchantments.isPresent()) {
         for (Holder<Enchantment> holder : this.enchantments.get()) {
            if (this.matchesEnchantment(p_467874_, holder)) {
               return true;
            }
         }

         return false;
      } else if (this.level != MinMaxBounds.Ints.ANY) {
         for (Entry<Holder<Enchantment>> entry : p_467874_.entrySet()) {
            if (this.level.matches(entry.getIntValue())) {
               return true;
            }
         }

         return false;
      } else {
         return !p_467874_.isEmpty();
      }
   }

   private boolean matchesEnchantment(ItemEnchantments p_467033_, Holder<Enchantment> p_468989_) {
      int i = p_467033_.getLevel(p_468989_);
      if (i == 0) {
         return false;
      } else {
         return this.level == MinMaxBounds.Ints.ANY ? true : this.level.matches(i);
      }
   }
}
