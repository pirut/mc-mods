package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.ReferenceCounted;

public record HiddenByteBuf(ByteBuf contents) implements ReferenceCounted {
   public HiddenByteBuf(ByteBuf contents) {
      this.contents = ByteBufUtil.ensureAccessible(contents);
   }

   public static Object pack(Object p_390365_) {
      return p_390365_ instanceof ByteBuf bytebuf ? new HiddenByteBuf(bytebuf) : p_390365_;
   }

   public static Object unpack(Object p_390514_) {
      return p_390514_ instanceof HiddenByteBuf hiddenbytebuf ? ByteBufUtil.ensureAccessible(hiddenbytebuf.contents) : p_390514_;
   }

   public int refCnt() {
      return this.contents.refCnt();
   }

   public HiddenByteBuf retain() {
      this.contents.retain();
      return this;
   }

   public HiddenByteBuf retain(int p_390414_) {
      this.contents.retain(p_390414_);
      return this;
   }

   public HiddenByteBuf touch() {
      this.contents.touch();
      return this;
   }

   public HiddenByteBuf touch(Object p_390392_) {
      this.contents.touch(p_390392_);
      return this;
   }

   public boolean release() {
      return this.contents.release();
   }

   public boolean release(int p_390522_) {
      return this.contents.release(p_390522_);
   }
}
