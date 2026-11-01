package net.minecraft.world.item.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public record TargetedConditionalEffect<T>(EnchantmentTarget enchanted, EnchantmentTarget affected, T effect, Optional<LootItemCondition> requirements) {
   public static <S> Codec<TargetedConditionalEffect<S>> codec(Codec<S> p_344749_, ContextKeySet p_381041_) {
      return RecordCodecBuilder.create(
         p_380895_ -> p_380895_.group(
               EnchantmentTarget.CODEC.fieldOf("enchanted").forGetter(TargetedConditionalEffect::enchanted),
               EnchantmentTarget.CODEC.fieldOf("affected").forGetter(TargetedConditionalEffect::affected),
               p_344749_.fieldOf("effect").forGetter(TargetedConditionalEffect::effect),
               ConditionalEffect.conditionCodec(p_381041_).optionalFieldOf("requirements").forGetter(TargetedConditionalEffect::requirements)
            )
            .apply(p_380895_, TargetedConditionalEffect::new)
      );
   }

   public static <S> Codec<TargetedConditionalEffect<S>> equipmentDropsCodec(Codec<S> p_345181_, ContextKeySet p_381042_) {
      return RecordCodecBuilder.create(
         p_380892_ -> p_380892_.group(
               EnchantmentTarget.CODEC
                  .validate(
                     p_345851_ -> p_345851_ != EnchantmentTarget.DAMAGING_ENTITY
                        ? DataResult.success(p_345851_)
                        : DataResult.error(() -> "enchanted must be attacker or victim")
                  )
                  .fieldOf("enchanted")
                  .forGetter(TargetedConditionalEffect::enchanted),
               p_345181_.fieldOf("effect").forGetter(TargetedConditionalEffect::effect),
               ConditionalEffect.conditionCodec(p_381042_).optionalFieldOf("requirements").forGetter(TargetedConditionalEffect::requirements)
            )
            .apply(p_380892_, (p_345692_, p_345215_, p_346096_) -> new TargetedConditionalEffect<>(p_345692_, EnchantmentTarget.VICTIM, p_345215_, p_346096_))
      );
   }

   public boolean matches(LootContext p_346180_) {
      return this.requirements.isEmpty() ? true : this.requirements.get().test(p_346180_);
   }
}
