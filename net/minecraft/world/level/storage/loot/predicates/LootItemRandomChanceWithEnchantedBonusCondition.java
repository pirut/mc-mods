package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public record LootItemRandomChanceWithEnchantedBonusCondition(float unenchantedChance, LevelBasedValue enchantedChance, Holder<Enchantment> enchantment)
   implements LootItemCondition {
   public static final MapCodec<LootItemRandomChanceWithEnchantedBonusCondition> CODEC = RecordCodecBuilder.mapCodec(
      p_352054_ -> p_352054_.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("unenchanted_chance").forGetter(LootItemRandomChanceWithEnchantedBonusCondition::unenchantedChance),
            LevelBasedValue.CODEC.fieldOf("enchanted_chance").forGetter(LootItemRandomChanceWithEnchantedBonusCondition::enchantedChance),
            Enchantment.CODEC.fieldOf("enchantment").forGetter(LootItemRandomChanceWithEnchantedBonusCondition::enchantment)
         )
         .apply(p_352054_, LootItemRandomChanceWithEnchantedBonusCondition::new)
   );

   @Override
   public LootItemConditionType getType() {
      return LootItemConditions.RANDOM_CHANCE_WITH_ENCHANTED_BONUS;
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return ImmutableSet.of(LootContextParams.ATTACKING_ENTITY);
   }

   public boolean test(LootContext p_345165_) {
      Entity $$1 = p_345165_.getParamOrNull(LootContextParams.ATTACKING_ENTITY);
      int $$3 = $$1 instanceof LivingEntity $$2 ? EnchantmentHelper.getEnchantmentLevel(this.enchantment, $$2) : 0;
      float $$4 = $$3 > 0 ? this.enchantedChance.calculate($$3) : this.unenchantedChance;
      return p_345165_.getRandom().nextFloat() < $$4;
   }

   public static LootItemCondition.Builder randomChanceAndLootingBoost(HolderLookup.Provider p_346115_, float p_345739_, float p_345989_) {
      HolderLookup.RegistryLookup<Enchantment> $$3 = p_346115_.lookupOrThrow(Registries.ENCHANTMENT);
      return () -> new LootItemRandomChanceWithEnchantedBonusCondition(
         p_345739_, new LevelBasedValue.Linear(p_345739_ + p_345989_, p_345989_), $$3.getOrThrow(Enchantments.LOOTING)
      );
   }
}
