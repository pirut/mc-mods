package net.minecraft.world.item.slot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

public class LimitSlotSource extends TransformedSlotSource {
   public static final MapCodec<LimitSlotSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_461210_ -> commonFields(p_461210_)
         .and(ExtraCodecs.POSITIVE_INT.fieldOf("limit").forGetter(p_460638_ -> p_460638_.limit))
         .apply(p_461210_, LimitSlotSource::new)
   );
   private final int limit;

   private LimitSlotSource(SlotSource p_460692_, int p_461162_) {
      super(p_460692_);
      this.limit = p_461162_;
   }

   @Override
   public MapCodec<LimitSlotSource> codec() {
      return MAP_CODEC;
   }

   @Override
   protected SlotCollection transform(SlotCollection p_460776_) {
      return p_460776_.limit(this.limit);
   }
}
