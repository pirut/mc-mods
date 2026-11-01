package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

public enum CreakingHeartState implements StringRepresentable {
   UPROOTED("uprooted"),
   DORMANT("dormant"),
   AWAKE("awake");

   private final String name;

   private CreakingHeartState(String p_394470_) {
      this.name = p_394470_;
   }

   @Override
   public String toString() {
      return this.name;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
