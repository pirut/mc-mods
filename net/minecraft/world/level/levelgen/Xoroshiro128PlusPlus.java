package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import java.util.stream.LongStream;
import net.minecraft.Util;

public class Xoroshiro128PlusPlus {
   private long seedLo;
   private long seedHi;
   public static final Codec<Xoroshiro128PlusPlus> CODEC = Codec.LONG_STREAM
      .comapFlatMap(
         p_338097_ -> Util.fixedSize(p_338097_, 2).map(p_287742_ -> new Xoroshiro128PlusPlus(p_287742_[0], p_287742_[1])),
         p_287687_ -> LongStream.of(p_287687_.seedLo, p_287687_.seedHi)
      );

   public Xoroshiro128PlusPlus(RandomSupport.Seed128bit p_190095_) {
      this(p_190095_.seedLo(), p_190095_.seedHi());
   }

   public Xoroshiro128PlusPlus(long p_190092_, long p_190093_) {
      this.seedLo = p_190092_;
      this.seedHi = p_190093_;
      if ((this.seedLo | this.seedHi) == 0L) {
         this.seedLo = -7046029254386353131L;
         this.seedHi = 7640891576956012809L;
      }
   }

   public long nextLong() {
      long $$0 = this.seedLo;
      long $$1 = this.seedHi;
      long $$2 = Long.rotateLeft($$0 + $$1, 17) + $$0;
      $$1 ^= $$0;
      this.seedLo = Long.rotateLeft($$0, 49) ^ $$1 ^ $$1 << 21;
      this.seedHi = Long.rotateLeft($$1, 28);
      return $$2;
   }
}
