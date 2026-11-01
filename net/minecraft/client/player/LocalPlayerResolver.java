package net.minecraft.client.player;

import com.mojang.authlib.GameProfile;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.server.players.ProfileResolver;

@Environment(EnvType.CLIENT)
public class LocalPlayerResolver implements ProfileResolver {
   private final Minecraft minecraft;
   private final ProfileResolver parentResolver;

   public LocalPlayerResolver(Minecraft minecraft, ProfileResolver parentResolver) {
      this.minecraft = minecraft;
      this.parentResolver = parentResolver;
   }

   @Override
   public Optional<GameProfile> fetchByName(String name) {
      ClientPacketListener clientPacketListener = this.minecraft.getConnection();
      if (clientPacketListener != null) {
         PlayerInfo playerInfo = clientPacketListener.getPlayerInfoIgnoreCase(name);
         if (playerInfo != null) {
            return Optional.of(playerInfo.getProfile());
         }
      }

      return this.parentResolver.fetchByName(name);
   }

   @Override
   public Optional<GameProfile> fetchById(UUID id) {
      ClientPacketListener clientPacketListener = this.minecraft.getConnection();
      if (clientPacketListener != null) {
         PlayerInfo playerInfo = clientPacketListener.getPlayerInfo(id);
         if (playerInfo != null) {
            return Optional.of(playerInfo.getProfile());
         }
      }

      return this.parentResolver.fetchById(id);
   }
}
