package net.minecraft.server.dedicated;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.slf4j.Logger;

public class DedicatedPlayerList extends PlayerList {
   private static final Logger LOGGER = LogUtils.getLogger();

   public DedicatedPlayerList(DedicatedServer p_203709_, LayeredRegistryAccess<RegistryLayer> p_251851_, PlayerDataStorage p_203711_) {
      super(p_203709_, p_251851_, p_203711_, p_203709_.getProperties().maxPlayers);
      DedicatedServerProperties $$3 = p_203709_.getProperties();
      this.setViewDistance($$3.viewDistance);
      this.setSimulationDistance($$3.simulationDistance);
      super.setUsingWhiteList($$3.whiteList.get());
      this.loadUserBanList();
      this.saveUserBanList();
      this.loadIpBanList();
      this.saveIpBanList();
      this.loadOps();
      this.loadWhiteList();
      this.saveOps();
      if (!this.getWhiteList().getFile().exists()) {
         this.saveWhiteList();
      }
   }

   @Override
   public void setUsingWhiteList(boolean p_139584_) {
      super.setUsingWhiteList(p_139584_);
      this.getServer().storeUsingWhiteList(p_139584_);
   }

   @Override
   public void op(GameProfile p_139582_) {
      super.op(p_139582_);
      this.saveOps();
   }

   @Override
   public void deop(GameProfile p_139587_) {
      super.deop(p_139587_);
      this.saveOps();
   }

   @Override
   public void reloadWhiteList() {
      this.loadWhiteList();
   }

   private void saveIpBanList() {
      try {
         this.getIpBans().save();
      } catch (IOException var2) {
         LOGGER.warn("Failed to save ip banlist: ", var2);
      }
   }

   private void saveUserBanList() {
      try {
         this.getBans().save();
      } catch (IOException var2) {
         LOGGER.warn("Failed to save user banlist: ", var2);
      }
   }

   private void loadIpBanList() {
      try {
         this.getIpBans().load();
      } catch (IOException var2) {
         LOGGER.warn("Failed to load ip banlist: ", var2);
      }
   }

   private void loadUserBanList() {
      try {
         this.getBans().load();
      } catch (IOException var2) {
         LOGGER.warn("Failed to load user banlist: ", var2);
      }
   }

   private void loadOps() {
      try {
         this.getOps().load();
      } catch (Exception var2) {
         LOGGER.warn("Failed to load operators list: ", var2);
      }
   }

   private void saveOps() {
      try {
         this.getOps().save();
      } catch (Exception var2) {
         LOGGER.warn("Failed to save operators list: ", var2);
      }
   }

   private void loadWhiteList() {
      try {
         this.getWhiteList().load();
      } catch (Exception var2) {
         LOGGER.warn("Failed to load white-list: ", var2);
      }
   }

   private void saveWhiteList() {
      try {
         this.getWhiteList().save();
      } catch (Exception var2) {
         LOGGER.warn("Failed to save white-list: ", var2);
      }
   }

   @Override
   public boolean isWhiteListed(GameProfile p_139590_) {
      return !this.isUsingWhitelist() || this.isOp(p_139590_) || this.getWhiteList().isWhiteListed(p_139590_);
   }

   public DedicatedServer getServer() {
      return (DedicatedServer)super.getServer();
   }

   @Override
   public boolean canBypassPlayerLimit(GameProfile p_139592_) {
      return this.getOps().canBypassPlayerLimit(p_139592_);
   }
}
