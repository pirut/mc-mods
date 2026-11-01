package net.minecraft.world.effect;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class MobEffectUtil {
   public static Component formatDuration(MobEffectInstance p_268116_, float p_268280_, float p_314720_) {
      if (p_268116_.isInfiniteDuration()) {
         return Component.translatable("effect.duration.infinite");
      } else {
         int $$3 = Mth.floor(p_268116_.getDuration() * p_268280_);
         return Component.literal(StringUtil.formatTickDuration($$3, p_314720_));
      }
   }

   public static boolean hasDigSpeed(LivingEntity p_19585_) {
      return p_19585_.hasEffect(MobEffects.DIG_SPEED) || p_19585_.hasEffect(MobEffects.CONDUIT_POWER);
   }

   public static int getDigSpeedAmplification(LivingEntity p_19587_) {
      int $$1 = 0;
      int $$2 = 0;
      if (p_19587_.hasEffect(MobEffects.DIG_SPEED)) {
         $$1 = p_19587_.getEffect(MobEffects.DIG_SPEED).getAmplifier();
      }

      if (p_19587_.hasEffect(MobEffects.CONDUIT_POWER)) {
         $$2 = p_19587_.getEffect(MobEffects.CONDUIT_POWER).getAmplifier();
      }

      return Math.max($$1, $$2);
   }

   public static boolean hasWaterBreathing(LivingEntity p_19589_) {
      return p_19589_.hasEffect(MobEffects.WATER_BREATHING) || p_19589_.hasEffect(MobEffects.CONDUIT_POWER);
   }

   public static List<ServerPlayer> addEffectToPlayersAround(
      ServerLevel p_216947_, @Nullable Entity p_216948_, Vec3 p_216949_, double p_216950_, MobEffectInstance p_216951_, int p_216952_
   ) {
      Holder<MobEffect> $$6 = p_216951_.getEffect();
      List<ServerPlayer> $$7 = p_216947_.getPlayers(
         p_267925_ -> p_267925_.gameMode.isSurvival()
            && (p_216948_ == null || !p_216948_.isAlliedTo(p_267925_))
            && p_216949_.closerThan(p_267925_.position(), p_216950_)
            && (
               !p_267925_.hasEffect($$6)
                  || p_267925_.getEffect($$6).getAmplifier() < p_216951_.getAmplifier()
                  || p_267925_.getEffect($$6).endsWithin(p_216952_ - 1)
            )
      );
      $$7.forEach(p_238232_ -> p_238232_.addEffect(new MobEffectInstance(p_216951_), p_216948_));
      return $$7;
   }
}
