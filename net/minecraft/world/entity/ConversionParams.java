package net.minecraft.world.entity;

import net.minecraft.world.scores.PlayerTeam;
import org.jspecify.annotations.Nullable;

public record ConversionParams(ConversionType type, boolean keepEquipment, boolean preserveCanPickUpLoot, @Nullable PlayerTeam team) {
   public static ConversionParams single(Mob p_371247_, boolean p_371563_, boolean p_371623_) {
      return new ConversionParams(ConversionType.SINGLE, p_371563_, p_371623_, p_371247_.getTeam());
   }

   @FunctionalInterface
   public interface AfterConversion<T extends Mob> {
      void finalizeConversion(T var1);
   }
}
