package net.minecraft.client.gui.components.debug;

import net.minecraft.util.StringRepresentable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum DebugScreenEntryStatus implements StringRepresentable {
   ALWAYS_ON("alwaysOn"),
   IN_OVERLAY("inOverlay"),
   NEVER("never");

   public static final StringRepresentable.EnumCodec<DebugScreenEntryStatus> CODEC = StringRepresentable.fromEnum(DebugScreenEntryStatus::values);
   private final String name;

   private DebugScreenEntryStatus(String p_432774_) {
      this.name = p_432774_;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
