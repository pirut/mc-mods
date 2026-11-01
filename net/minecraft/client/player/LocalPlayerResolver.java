package net.minecraft.client.player;

import com.mojang.authlib.GameProfile;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.server.players.ProfileResolver;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LocalPlayerResolver implements ProfileResolver {
   private final Minecraft minecraft;
   private final ProfileResolver parentResolver;

   public LocalPlayerResolver(Minecraft p_438981_, ProfileResolver p_439655_) {
      this.minecraft = p_438981_;
      this.parentResolver = p_439655_;
   }

   @Override
   public Optional<GameProfile> fetchByName(String p_440511_) {
      ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
      if (clientpacketlistener != null) {
         PlayerInfo playerinfo = clientpacketlistener.getPlayerInfoIgnoreCase(p_440511_);
         if (playerinfo != null) {
            return Optional.of(playerinfo.getProfile());
         }
      }

      return this.parentResolver.fetchByName(p_440511_);
   }

   @Override
   public Optional<GameProfile> fetchById(UUID p_438882_) {
      ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
      if (clientpacketlistener != null) {
         PlayerInfo playerinfo = clientpacketlistener.getPlayerInfo(p_438882_);
         if (playerinfo != null) {
            return Optional.of(playerinfo.getProfile());
         }
      }

      return this.parentResolver.fetchById(p_438882_);
   }
}
