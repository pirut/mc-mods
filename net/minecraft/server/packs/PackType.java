package net.minecraft.server.packs;

import net.minecraft.util.StringRepresentable;

public enum PackType implements StringRepresentable {
   CLIENT_RESOURCES("assets"),
   SERVER_DATA("data");

   private final String directory;

   private PackType(String p_265479_) {
      this.directory = p_265479_;
   }

   public String getDirectory() {
      return this.directory;
   }

   @Override
   public String getSerializedName() {
      return this.name().toLowerCase();
   }
}
