package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.Util;
import net.minecraft.util.thread.ConsecutiveExecutor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ServerList {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ConsecutiveExecutor IO_EXECUTOR = new ConsecutiveExecutor(Util.backgroundExecutor(), "server-list-io");
   private static final int MAX_HIDDEN_SERVERS = 16;
   private final Minecraft minecraft;
   private final List<ServerData> serverList = Lists.newArrayList();
   private final List<ServerData> hiddenServerList = Lists.newArrayList();

   public ServerList(Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   public void load() {
      try {
         this.serverList.clear();
         this.hiddenServerList.clear();
         CompoundTag compoundTag = NbtIo.read(this.minecraft.gameDirectory.toPath().resolve("servers.dat"));
         if (compoundTag == null) {
            return;
         }

         compoundTag.getListOrEmpty("servers").compoundStream().forEach(compoundTagx -> {
            ServerData serverData = ServerData.read(compoundTagx);
            if (compoundTagx.getBooleanOr("hidden", false)) {
               this.hiddenServerList.add(serverData);
            } else {
               this.serverList.add(serverData);
            }
         });
      } catch (Exception var2) {
         LOGGER.error("Couldn't load server list", var2);
      }
   }

   public void save() {
      try {
         ListTag listTag = new ListTag();

         for (ServerData serverData : this.serverList) {
            CompoundTag compoundTag = serverData.write();
            compoundTag.putBoolean("hidden", false);
            listTag.add(compoundTag);
         }

         for (ServerData serverData : this.hiddenServerList) {
            CompoundTag compoundTag = serverData.write();
            compoundTag.putBoolean("hidden", true);
            listTag.add(compoundTag);
         }

         CompoundTag compoundTag2 = new CompoundTag();
         compoundTag2.put("servers", listTag);
         Path path = this.minecraft.gameDirectory.toPath();
         Path path2 = Files.createTempFile(path, "servers", ".dat");
         NbtIo.write(compoundTag2, path2);
         Path path3 = path.resolve("servers.dat_old");
         Path path4 = path.resolve("servers.dat");
         Util.safeReplaceFile(path4, path2, path3);
      } catch (Exception var7) {
         LOGGER.error("Couldn't save server list", var7);
      }
   }

   public ServerData get(int index) {
      return this.serverList.get(index);
   }

   @Nullable
   public ServerData get(String ip) {
      for (ServerData serverData : this.serverList) {
         if (serverData.ip.equals(ip)) {
            return serverData;
         }
      }

      for (ServerData serverDatax : this.hiddenServerList) {
         if (serverDatax.ip.equals(ip)) {
            return serverDatax;
         }
      }

      return null;
   }

   @Nullable
   public ServerData unhide(String ip) {
      for (int i = 0; i < this.hiddenServerList.size(); i++) {
         ServerData serverData = this.hiddenServerList.get(i);
         if (serverData.ip.equals(ip)) {
            this.hiddenServerList.remove(i);
            this.serverList.add(serverData);
            return serverData;
         }
      }

      return null;
   }

   public void remove(ServerData serverData) {
      if (!this.serverList.remove(serverData)) {
         this.hiddenServerList.remove(serverData);
      }
   }

   public void add(ServerData server, boolean hidden) {
      if (hidden) {
         this.hiddenServerList.add(0, server);

         while (this.hiddenServerList.size() > 16) {
            this.hiddenServerList.remove(this.hiddenServerList.size() - 1);
         }
      } else {
         this.serverList.add(server);
      }
   }

   public int size() {
      return this.serverList.size();
   }

   public void swap(int pos1, int pos2) {
      ServerData serverData = this.get(pos1);
      this.serverList.set(pos1, this.get(pos2));
      this.serverList.set(pos2, serverData);
      this.save();
   }

   public void replace(int index, ServerData server) {
      this.serverList.set(index, server);
   }

   private static boolean set(ServerData server, List<ServerData> serverList) {
      for (int i = 0; i < serverList.size(); i++) {
         ServerData serverData = serverList.get(i);
         if (Objects.equals(serverData.name, server.name) && serverData.ip.equals(server.ip)) {
            serverList.set(i, server);
            return true;
         }
      }

      return false;
   }

   public static void saveSingleServer(ServerData server) {
      IO_EXECUTOR.schedule(() -> {
         ServerList serverList = new ServerList(Minecraft.getInstance());
         serverList.load();
         if (!set(server, serverList.serverList)) {
            set(server, serverList.hiddenServerList);
         }

         serverList.save();
      });
   }
}
