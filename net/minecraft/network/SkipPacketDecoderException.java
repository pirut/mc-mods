package net.minecraft.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.IdDispatchCodec;

public class SkipPacketDecoderException extends DecoderException implements IdDispatchCodec.DontDecorateException, SkipPacketException {
   public SkipPacketDecoderException(String p_412786_) {
      super(p_412786_);
   }

   public SkipPacketDecoderException(Throwable p_412103_) {
      super(p_412103_);
   }
}
