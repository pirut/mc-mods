package net.neoforged.neoforge.common.damagesource;

import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

@FunctionalInterface
public interface IScalingFunction {
   IScalingFunction DEFAULT = (source, target, amount, difficulty) -> {
      if (source.scalesWithDifficulty()) {
         return switch (target.level().getDifficulty()) {
            case PEACEFUL -> 0.0F;
            case EASY -> Math.min(amount / 2.0F + 1.0F, amount);
            case NORMAL -> amount;
            case HARD -> amount * 1.5F;
         };
      } else {
         return amount;
      }
   };

   float scaleDamage(DamageSource var1, Player var2, float var3, Difficulty var4);
}
