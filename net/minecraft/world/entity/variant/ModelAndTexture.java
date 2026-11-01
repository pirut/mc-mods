package net.minecraft.world.entity.variant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record ModelAndTexture<T>(T model, ClientAsset.ResourceTexture asset) {
   public ModelAndTexture(T p_400126_, Identifier p_468704_) {
      this(p_400126_, new ClientAsset.ResourceTexture(p_468704_));
   }

   public static <T> MapCodec<ModelAndTexture<T>> codec(Codec<T> p_399564_, T p_400177_) {
      return RecordCodecBuilder.mapCodec(
         p_450885_ -> p_450885_.group(
               p_399564_.optionalFieldOf("model", p_400177_).forGetter(ModelAndTexture::model),
               ClientAsset.ResourceTexture.DEFAULT_FIELD_CODEC.forGetter(ModelAndTexture::asset)
            )
            .apply(p_450885_, ModelAndTexture::new)
      );
   }

   public static <T> StreamCodec<RegistryFriendlyByteBuf, ModelAndTexture<T>> streamCodec(StreamCodec<? super RegistryFriendlyByteBuf, T> p_400232_) {
      return StreamCodec.composite(p_400232_, ModelAndTexture::model, ClientAsset.ResourceTexture.STREAM_CODEC, ModelAndTexture::asset, ModelAndTexture::new);
   }
}
