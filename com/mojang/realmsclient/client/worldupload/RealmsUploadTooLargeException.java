package com.mojang.realmsclient.client.worldupload;

import com.mojang.realmsclient.Unit;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsUploadTooLargeException extends RealmsUploadException {
   final long sizeLimit;

   public RealmsUploadTooLargeException(long p_374265_) {
      this.sizeLimit = p_374265_;
   }

   @Override
   public Component[] getErrorMessages() {
      return new Component[]{
         Component.translatable("mco.upload.failed.too_big.title"),
         Component.translatable("mco.upload.failed.too_big.description", Unit.humanReadable(this.sizeLimit, Unit.getLargest(this.sizeLimit)))
      };
   }
}
