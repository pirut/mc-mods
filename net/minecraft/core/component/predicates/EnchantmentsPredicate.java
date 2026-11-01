package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Function;
import net.minecraft.advancements.criterion.EnchantmentPredicate;
import net.minecraft.advancements.criterion.SingleComponentItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.common.CommonHooks;

public abstract class EnchantmentsPredicate implements SingleComponentItemPredicate<ItemEnchantments> {
   private final List<EnchantmentPredicate> enchantments;

   protected EnchantmentsPredicate(List<EnchantmentPredicate> p_400025_) {
      this.enchantments = p_400025_;
   }

   public static <T extends EnchantmentsPredicate> Codec<T> codec(Function<List<EnchantmentPredicate>, T> p_400213_) {
      return EnchantmentPredicate.CODEC.listOf().xmap(p_400213_, EnchantmentsPredicate::enchantments);
   }

   protected List<EnchantmentPredicate> enchantments() {
      return this.enchantments;
   }

   public boolean matches(ItemEnchantments p_399718_) {
      for (EnchantmentPredicate enchantmentpredicate : this.enchantments) {
         if (!enchantmentpredicate.containedIn(p_399718_)) {
            return false;
         }
      }

      return true;
   }

   public static EnchantmentsPredicate.Enchantments enchantments(List<EnchantmentPredicate> p_399739_) {
      return new EnchantmentsPredicate.Enchantments(p_399739_);
   }

   public static EnchantmentsPredicate.StoredEnchantments storedEnchantments(List<EnchantmentPredicate> p_399510_) {
      return new EnchantmentsPredicate.StoredEnchantments(p_399510_);
   }

   public static class Enchantments extends EnchantmentsPredicate {
      public static final Codec<EnchantmentsPredicate.Enchantments> CODEC = codec(EnchantmentsPredicate.Enchantments::new);

      protected Enchantments(List<EnchantmentPredicate> p_399954_) {
         super(p_399954_);
      }

      @Override
      public DataComponentType<ItemEnchantments> componentType() {
         return DataComponents.ENCHANTMENTS;
      }

      @Override
      public boolean matches(DataComponentGetter getter) {
         if (getter instanceof ItemStack stack) {
            HolderLookup.RegistryLookup<Enchantment> lookup = CommonHooks.resolveLookup(Registries.ENCHANTMENT);
            if (lookup != null) {
               return this.matches(stack.getAllEnchantments(lookup));
            }
         }

         return super.matches(getter);
      }
   }

   public static class StoredEnchantments extends EnchantmentsPredicate {
      public static final Codec<EnchantmentsPredicate.StoredEnchantments> CODEC = codec(EnchantmentsPredicate.StoredEnchantments::new);

      protected StoredEnchantments(List<EnchantmentPredicate> p_399693_) {
         super(p_399693_);
      }

      @Override
      public DataComponentType<ItemEnchantments> componentType() {
         return DataComponents.STORED_ENCHANTMENTS;
      }
   }
}
