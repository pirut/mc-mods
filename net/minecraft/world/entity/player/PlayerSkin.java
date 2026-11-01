package net.minecraft.world.entity.player;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

public record PlayerSkin(
   ClientAsset.Texture body, @Nullable ClientAsset.Texture cape, @Nullable ClientAsset.Texture elytra, PlayerModelType model, boolean secure
) {
   public static PlayerSkin insecure(
      ClientAsset.Texture p_451290_, @Nullable ClientAsset.Texture p_451495_, @Nullable ClientAsset.Texture p_451024_, PlayerModelType p_451100_
   ) {
      return new PlayerSkin(p_451290_, p_451495_, p_451024_, p_451100_, false);
   }

   public PlayerSkin with(PlayerSkin.Patch p_451337_) {
      return p_451337_.equals(PlayerSkin.Patch.EMPTY)
         ? this
         : insecure(
            (ClientAsset.Texture)DataFixUtils.orElse(p_451337_.body, this.body),
            (ClientAsset.Texture)DataFixUtils.orElse(p_451337_.cape, this.cape),
            (ClientAsset.Texture)DataFixUtils.orElse(p_451337_.elytra, this.elytra),
            p_451337_.model.orElse(this.model)
         );
   }

   public record Patch(
      Optional<ClientAsset.ResourceTexture> body,
      Optional<ClientAsset.ResourceTexture> cape,
      Optional<ClientAsset.ResourceTexture> elytra,
      Optional<PlayerModelType> model
   ) {
      public static final PlayerSkin.Patch EMPTY = new PlayerSkin.Patch(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
      public static final MapCodec<PlayerSkin.Patch> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_450964_ -> p_450964_.group(
               ClientAsset.ResourceTexture.CODEC.optionalFieldOf("texture").forGetter(PlayerSkin.Patch::body),
               ClientAsset.ResourceTexture.CODEC.optionalFieldOf("cape").forGetter(PlayerSkin.Patch::cape),
               ClientAsset.ResourceTexture.CODEC.optionalFieldOf("elytra").forGetter(PlayerSkin.Patch::elytra),
               PlayerModelType.CODEC.optionalFieldOf("model").forGetter(PlayerSkin.Patch::model)
            )
            .apply(p_450964_, PlayerSkin.Patch::create)
      );
      public static final StreamCodec<ByteBuf, PlayerSkin.Patch> STREAM_CODEC = StreamCodec.composite(
         ClientAsset.ResourceTexture.STREAM_CODEC.apply(ByteBufCodecs::optional),
         PlayerSkin.Patch::body,
         ClientAsset.ResourceTexture.STREAM_CODEC.apply(ByteBufCodecs::optional),
         PlayerSkin.Patch::cape,
         ClientAsset.ResourceTexture.STREAM_CODEC.apply(ByteBufCodecs::optional),
         PlayerSkin.Patch::elytra,
         PlayerModelType.STREAM_CODEC.apply(ByteBufCodecs::optional),
         PlayerSkin.Patch::model,
         PlayerSkin.Patch::create
      );

      public static PlayerSkin.Patch create(
         Optional<ClientAsset.ResourceTexture> p_451197_,
         Optional<ClientAsset.ResourceTexture> p_451025_,
         Optional<ClientAsset.ResourceTexture> p_451497_,
         Optional<PlayerModelType> p_451198_
      ) {
         return p_451197_.isEmpty() && p_451025_.isEmpty() && p_451497_.isEmpty() && p_451198_.isEmpty()
            ? EMPTY
            : new PlayerSkin.Patch(p_451197_, p_451025_, p_451497_, p_451198_);
      }
   }
}
