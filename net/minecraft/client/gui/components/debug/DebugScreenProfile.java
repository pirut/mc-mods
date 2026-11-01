package net.minecraft.client.gui.components.debug;

import net.minecraft.util.StringRepresentable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum DebugScreenProfile implements StringRepresentable {
   DEFAULT("default", "debug.options.profile.default"),
   PERFORMANCE("performance", "debug.options.profile.performance");

   public static final StringRepresentable.EnumCodec<DebugScreenProfile> CODEC = StringRepresentable.fromEnum(DebugScreenProfile::values);
   private final String name;
   private final String translationKey;

   private DebugScreenProfile(String p_433145_, String p_434436_) {
      this.name = p_433145_;
      this.translationKey = p_434436_;
   }

   public String translationKey() {
      return this.translationKey;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
