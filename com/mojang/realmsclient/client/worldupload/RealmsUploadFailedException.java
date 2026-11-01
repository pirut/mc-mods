package com.mojang.realmsclient.client.worldupload;

import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsUploadFailedException extends RealmsUploadException {
   private final Component errorMessage;

   public RealmsUploadFailedException(Component p_374193_) {
      this.errorMessage = p_374193_;
   }

   public RealmsUploadFailedException(String p_374157_) {
      this(Component.literal(p_374157_));
   }

   @Override
   public Component getStatusMessage() {
      return Component.translatable("mco.upload.failed", this.errorMessage);
   }
}
