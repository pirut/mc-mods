package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

public class PlayerService {
   private static final Component DEFAULT_KICK_MESSAGE = Component.translatable("multiplayer.disconnect.kicked");

   public static List<PlayerDto> get(MinecraftApi p_442966_) {
      return p_442966_.playerListService().getPlayers().stream().map(PlayerDto::from).toList();
   }

   public static List<PlayerDto> kick(MinecraftApi p_442902_, List<PlayerService.KickDto> p_451679_, ClientInfo p_442923_) {
      List<PlayerDto> list = new ArrayList<>();

      for (PlayerService.KickDto playerservice$kickdto : p_451679_) {
         ServerPlayer serverplayer = getServerPlayer(p_442902_, playerservice$kickdto.player());
         if (serverplayer != null) {
            p_442902_.playerListService().remove(serverplayer, p_442923_);
            serverplayer.connection.disconnect(playerservice$kickdto.message.flatMap(Message::asComponent).orElse(DEFAULT_KICK_MESSAGE));
            list.add(playerservice$kickdto.player());
         }
      }

      return list;
   }

   @Nullable
   private static ServerPlayer getServerPlayer(MinecraftApi p_442904_, PlayerDto p_443244_) {
      if (p_443244_.id().isPresent()) {
         return p_442904_.playerListService().getPlayer(p_443244_.id().get());
      } else {
         return p_443244_.name().isPresent() ? p_442904_.playerListService().getPlayerByName(p_443244_.name().get()) : null;
      }
   }

   public record KickDto(PlayerDto player, Optional<Message> message) {
      public static final MapCodec<PlayerService.KickDto> CODEC = RecordCodecBuilder.mapCodec(
         p_451663_ -> p_451663_.group(
               PlayerDto.CODEC.codec().fieldOf("player").forGetter(PlayerService.KickDto::player),
               Message.CODEC.optionalFieldOf("message").forGetter(PlayerService.KickDto::message)
            )
            .apply(p_451663_, PlayerService.KickDto::new)
      );
   }
}
