package net.neoforged.neoforge.client.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

public record ExtraFaceData(int color, int blockLight, int skyLight, boolean ambientOcclusion) {
   public static final ExtraFaceData DEFAULT = new ExtraFaceData(-1, 0, 0, true);
   public static final Codec<Integer> COLOR = Codec.either(Codec.INT, Codec.STRING)
      .xmap(either -> (Integer)either.map(Function.identity(), str -> (int)Long.parseLong(str, 16)), color -> Either.right(Integer.toHexString(color)));
   public static final Codec<ExtraFaceData> CODEC = RecordCodecBuilder.create(
      builder -> builder.group(
            COLOR.optionalFieldOf("color", -1).forGetter(ExtraFaceData::color),
            Codec.intRange(0, 15).optionalFieldOf("block_light", 0).forGetter(ExtraFaceData::blockLight),
            Codec.intRange(0, 15).optionalFieldOf("sky_light", 0).forGetter(ExtraFaceData::skyLight),
            Codec.BOOL.optionalFieldOf("ambient_occlusion", true).forGetter(ExtraFaceData::ambientOcclusion)
         )
         .apply(builder, ExtraFaceData::new)
   );

   @Nullable
   public static ExtraFaceData read(@Nullable JsonElement obj, @Nullable ExtraFaceData fallback) throws JsonParseException {
      return obj == null ? fallback : (ExtraFaceData)CODEC.parse(JsonOps.INSTANCE, obj).getOrThrow(JsonParseException::new);
   }
}
