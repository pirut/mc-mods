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
   public static ServerStateService.ServerState status(MinecraftApi api) {
      return !api.serverStateService().isReady()
         ? ServerStateService.ServerState.NOT_STARTED
         : new ServerStateService.ServerState(true, PlayerService.get(api), ServerStatus.Version.current());
   }

   public static boolean save(MinecraftApi api, boolean flush, ClientInfo clientInfo) {
      return api.serverStateService().saveEverything(true, flush, true, clientInfo);
   }

   public static boolean stop(MinecraftApi api, ClientInfo clientInfo) {
      api.submit(() -> api.serverStateService().halt(false, clientInfo));
      return true;
   }

   public static boolean systemMessage(MinecraftApi api, ServerStateService.SystemMessage message, ClientInfo clientInfo) {
      Component component = message.message().asComponent().orElse(null);
      if (component == null) {
         return false;
      } else {
         if (message.receivingPlayers().isPresent()) {
            if (message.receivingPlayers().get().isEmpty()) {
               return false;
            }

            for (PlayerDto playerDto : message.receivingPlayers().get()) {
               ServerPlayer serverPlayer;
               if (playerDto.id().isPresent()) {
                  serverPlayer = api.playerListService().getPlayer(playerDto.id().get());
               } else {
                  if (!playerDto.name().isPresent()) {
                     continue;
                  }

                  serverPlayer = api.playerListService().getPlayerByName(playerDto.name().get());
               }

               if (serverPlayer != null) {
                  serverPlayer.sendSystemMessage(component, message.overlay());
               }
            }
         } else {
            api.serverStateService().broadcastSystemMessage(component, message.overlay(), clientInfo);
         }

         return true;
      }
   }

   public record ServerState(boolean started, List<PlayerDto> players, ServerStatus.Version version) {
      public static final Codec<ServerStateService.ServerState> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.BOOL.fieldOf("started").forGetter(ServerStateService.ServerState::started),
               PlayerDto.CODEC.codec().listOf().lenientOptionalFieldOf("players", List.of()).forGetter(ServerStateService.ServerState::players),
               ServerStatus.Version.CODEC.fieldOf("version").forGetter(ServerStateService.ServerState::version)
            )
            .apply(instance, ServerStateService.ServerState::new)
      );
      public static final ServerStateService.ServerState NOT_STARTED = new ServerStateService.ServerState(false, List.of(), ServerStatus.Version.current());
   }

   public record SystemMessage(Message message, boolean overlay, Optional<List<PlayerDto>> receivingPlayers) {
      public static final Codec<ServerStateService.SystemMessage> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Message.CODEC.fieldOf("message").forGetter(ServerStateService.SystemMessage::message),
               Codec.BOOL.fieldOf("overlay").forGetter(ServerStateService.SystemMessage::overlay),
               PlayerDto.CODEC.codec().listOf().lenientOptionalFieldOf("receivingPlayers").forGetter(ServerStateService.SystemMessage::receivingPlayers)
            )
            .apply(instance, ServerStateService.SystemMessage::new)
      );
   }
}
