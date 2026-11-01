package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.level.ServerPlayer;

public class ServerStateService {
   public static ServerStateService.ServerState status(MinecraftApi p_443178_) {
      return !p_443178_.serverStateService().isReady()
         ? ServerStateService.ServerState.NOT_STARTED
         : new ServerStateService.ServerState(true, PlayerService.get(p_443178_), ServerStatus.Version.current());
   }

   public static boolean save(MinecraftApi p_442668_, boolean p_442983_, ClientInfo p_442792_) {
      return p_442668_.serverStateService().saveEverything(true, p_442983_, true, p_442792_);
   }

   public static boolean stop(MinecraftApi p_442865_, ClientInfo p_443096_) {
      p_442865_.submit(() -> p_442865_.serverStateService().halt(false, p_443096_));
      return true;
   }

   public static boolean systemMessage(MinecraftApi p_443593_, ServerStateService.SystemMessage p_442970_, ClientInfo p_443224_) {
      Component component = p_442970_.message().asComponent().orElse(null);
      if (component == null) {
         return false;
      } else {
         if (p_442970_.receivingPlayers().isPresent()) {
            if (p_442970_.receivingPlayers().get().isEmpty()) {
               return false;
            }

            for (PlayerDto playerdto : p_442970_.receivingPlayers().get()) {
               ServerPlayer serverplayer;
               if (playerdto.id().isPresent()) {
                  serverplayer = p_443593_.playerListService().getPlayer(playerdto.id().get());
               } else {
                  if (!playerdto.name().isPresent()) {
                     continue;
                  }

                  serverplayer = p_443593_.playerListService().getPlayerByName(playerdto.name().get());
               }

               if (serverplayer != null) {
                  serverplayer.sendSystemMessage(component, p_442970_.overlay());
               }
            }
         } else {
            p_443593_.serverStateService().broadcastSystemMessage(component, p_442970_.overlay(), p_443224_);
         }

         return true;
      }
   }

   public record ServerState(boolean started, List<PlayerDto> players, ServerStatus.Version version) {
      public static final Codec<ServerStateService.ServerState> CODEC = RecordCodecBuilder.create(
         p_443402_ -> p_443402_.group(
               Codec.BOOL.fieldOf("started").forGetter(ServerStateService.ServerState::started),
               PlayerDto.CODEC.codec().listOf().lenientOptionalFieldOf("players", List.of()).forGetter(ServerStateService.ServerState::players),
               ServerStatus.Version.CODEC.fieldOf("version").forGetter(ServerStateService.ServerState::version)
            )
            .apply(p_443402_, ServerStateService.ServerState::new)
      );
      public static final ServerStateService.ServerState NOT_STARTED = new ServerStateService.ServerState(false, List.of(), ServerStatus.Version.current());
   }

   public record SystemMessage(Message message, boolean overlay, Optional<List<PlayerDto>> receivingPlayers) {
      public static final Codec<ServerStateService.SystemMessage> CODEC = RecordCodecBuilder.create(
         p_442613_ -> p_442613_.group(
               Message.CODEC.fieldOf("message").forGetter(ServerStateService.SystemMessage::message),
               Codec.BOOL.fieldOf("overlay").forGetter(ServerStateService.SystemMessage::overlay),
               PlayerDto.CODEC.codec().listOf().lenientOptionalFieldOf("receivingPlayers").forGetter(ServerStateService.SystemMessage::receivingPlayers)
            )
            .apply(p_442613_, ServerStateService.SystemMessage::new)
      );
   }
}
