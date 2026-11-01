package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.Nullable;

public record CustomModelData(List<Float> floats, List<Boolean> flags, List<String> strings, List<Integer> colors) {
   public static final CustomModelData EMPTY = new CustomModelData(List.of(), List.of(), List.of(), List.of());
   public static final Codec<CustomModelData> CODEC = RecordCodecBuilder.create(
      p_387502_ -> p_387502_.group(
            Codec.FLOAT.listOf().optionalFieldOf("floats", List.of()).forGetter(CustomModelData::floats),
            Codec.BOOL.listOf().optionalFieldOf("flags", List.of()).forGetter(CustomModelData::flags),
            Codec.STRING.listOf().optionalFieldOf("strings", List.of()).forGetter(CustomModelData::strings),
            ExtraCodecs.RGB_COLOR_CODEC.listOf().optionalFieldOf("colors", List.of()).forGetter(CustomModelData::colors)
         )
         .apply(p_387502_, CustomModelData::new)
   );
   public static final StreamCodec<ByteBuf, CustomModelData> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list()),
      CustomModelData::floats,
      ByteBufCodecs.BOOL.apply(ByteBufCodecs.list()),
      CustomModelData::flags,
      ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
      CustomModelData::strings,
      ByteBufCodecs.INT.apply(ByteBufCodecs.list()),
      CustomModelData::colors,
      CustomModelData::new
   );

   @Nullable
   private static <T> T getSafe(List<T> p_387689_, int p_388541_) {
      return p_388541_ >= 0 && p_388541_ < p_387689_.size() ? p_387689_.get(p_388541_) : null;
   }

   @Nullable
   public Float getFloat(int p_386964_) {
      return getSafe(this.floats, p_386964_);
   }

   @Nullable
   public Boolean getBoolean(int p_388606_) {
      return getSafe(this.flags, p_388606_);
   }

   @Nullable
   public String getString(int p_388912_) {
      return getSafe(this.strings, p_388912_);
   }

   @Nullable
   public Integer getColor(int p_388650_) {
      return getSafe(this.colors, p_388650_);
   }
}
