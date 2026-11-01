package net.minecraft.server.jsonrpc.api;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public record PlayerDto(Optional<UUID> id, Optional<String> name) {
   public static final MapCodec<PlayerDto> CODEC = RecordCodecBuilder.mapCodec(
      p_443441_ -> p_443441_.group(
            UUIDUtil.STRING_CODEC.optionalFieldOf("id").forGetter(PlayerDto::id), Codec.STRING.optionalFieldOf("name").forGetter(PlayerDto::name)
         )
         .apply(p_443441_, PlayerDto::new)
   );

   public static PlayerDto from(GameProfile p_443291_) {
      return new PlayerDto(Optional.of(p_443291_.id()), Optional.of(p_443291_.name()));
   }

   public static PlayerDto from(NameAndId p_443191_) {
      return new PlayerDto(Optional.of(p_443191_.id()), Optional.of(p_443191_.name()));
   }

   public static PlayerDto from(ServerPlayer p_442882_) {
      GameProfile gameprofile = p_442882_.getGameProfile();
      return from(gameprofile);
   }
}
