package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.ReferenceCounted;

public record HiddenByteBuf(ByteBuf contents) implements ReferenceCounted {
   public HiddenByteBuf(final ByteBuf contents) {
      this.contents = ByteBufUtil.ensureAccessible(contents);
   }

   public static Object pack(Object object) {
      return object instanceof ByteBuf byteBuf ? new HiddenByteBuf(byteBuf) : object;
   }

   public static Object unpack(Object object) {
      return object instanceof HiddenByteBuf hiddenByteBuf ? ByteBufUtil.ensureAccessible(hiddenByteBuf.contents) : object;
   }

   public int refCnt() {
      return this.contents.refCnt();
   }

   public HiddenByteBuf retain() {
      this.contents.retain();
      return this;
   }

   public HiddenByteBuf retain(int increment) {
      this.contents.retain(increment);
      return this;
   }

   public HiddenByteBuf touch() {
      this.contents.touch();
      return this;
   }

   public HiddenByteBuf touch(Object hint) {
      this.contents.touch(hint);
      return this;
   }

   public boolean release() {
      return this.contents.release();
   }

   public boolean release(int i) {
      return this.contents.release(i);
   }
}
