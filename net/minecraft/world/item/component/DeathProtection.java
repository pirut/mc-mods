package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ClearAllStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;

public record DeathProtection(List<ConsumeEffect> deathEffects) {
   public static final Codec<DeathProtection> CODEC = RecordCodecBuilder.create(
      p_373089_ -> p_373089_.group(ConsumeEffect.CODEC.listOf().optionalFieldOf("death_effects", List.of()).forGetter(DeathProtection::deathEffects))
         .apply(p_373089_, DeathProtection::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, DeathProtection> STREAM_CODEC = StreamCodec.composite(
      ConsumeEffect.STREAM_CODEC.apply(ByteBufCodecs.list()), DeathProtection::deathEffects, DeathProtection::new
   );
   public static final DeathProtection TOTEM_OF_UNDYING = new DeathProtection(
      List.of(
         new ClearAllStatusEffectsConsumeEffect(),
         new ApplyStatusEffectsConsumeEffect(
            List.of(
               new MobEffectInstance(MobEffects.REGENERATION, 900, 1),
               new MobEffectInstance(MobEffects.ABSORPTION, 100, 1),
               new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0)
            )
         )
      )
   );

   public void applyEffects(ItemStack p_372914_, LivingEntity p_373008_) {
      for (ConsumeEffect consumeeffect : this.deathEffects) {
         consumeeffect.apply(p_373008_.level(), p_372914_, p_373008_);
      }
   }
}
