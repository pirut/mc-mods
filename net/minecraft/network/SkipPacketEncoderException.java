package net.minecraft.network;

import io.netty.handler.codec.EncoderException;
import net.minecraft.network.codec.IdDispatchCodec;

public class SkipPacketEncoderException extends EncoderException implements IdDispatchCodec.DontDecorateException, SkipPacketException {
   public SkipPacketEncoderException(String p_412734_) {
      super(p_412734_);
   }

   public SkipPacketEncoderException(Throwable p_412348_) {
      super(p_412348_);
   }
}
