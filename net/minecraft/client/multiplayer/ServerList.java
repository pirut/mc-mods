package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.thread.ProcessorMailbox;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ServerList {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ProcessorMailbox<Runnable> IO_MAILBOX = ProcessorMailbox.create(Util.backgroundExecutor(), "server-list-io");
   private static final int MAX_HIDDEN_SERVERS = 16;
   private final Minecraft minecraft;
   private final List<ServerData> serverList = Lists.newArrayList();
   private final List<ServerData> hiddenServerList = Lists.newArrayList();

   public ServerList(Minecraft p_105430_) {
      this.minecraft = p_105430_;
   }

   public void load() {
      try {
         this.serverList.clear();
         this.hiddenServerList.clear();
         CompoundTag $$0 = NbtIo.read(this.minecraft.gameDirectory.toPath().resolve("servers.dat"));
         if ($$0 == null) {
            return;
         }

         ListTag $$1 = $$0.getList("servers", 10);

         for (int $$2 = 0; $$2 < $$1.size(); $$2++) {
            CompoundTag $$3 = $$1.getCompound($$2);
            ServerData $$4 = ServerData.read($$3);
            if ($$3.getBoolean("hidden")) {
               this.hiddenServerList.add($$4);
            } else {
               this.serverList.add($$4);
            }
         }
      } catch (Exception var6) {
         LOGGER.error("Couldn't load server list", var6);
      }
   }

   public void save() {
      try {
         ListTag $$0 = new ListTag();

         for (ServerData $$1 : this.serverList) {
            CompoundTag $$2 = $$1.write();
            $$2.putBoolean("hidden", false);
            $$0.add($$2);
         }

         for (ServerData $$3 : this.hiddenServerList) {
            CompoundTag $$4 = $$3.write();
            $$4.putBoolean("hidden", true);
            $$0.add($$4);
         }

         CompoundTag $$5 = new CompoundTag();
         $$5.put("servers", $$0);
         Path $$6 = this.minecraft.gameDirectory.toPath();
         Path $$7 = Files.createTempFile($$6, "servers", ".dat");
         NbtIo.write($$5, $$7);
         Path $$8 = $$6.resolve("servers.dat_old");
         Path $$9 = $$6.resolve("servers.dat");
         Util.safeReplaceFile($$9, $$7, $$8);
      } catch (Exception var7) {
         LOGGER.error("Couldn't save server list", var7);
      }
   }

   public ServerData get(int p_105433_) {
      return this.serverList.get(p_105433_);
   }

   @Nullable
   public ServerData get(String p_233846_) {
      for (ServerData $$1 : this.serverList) {
         if ($$1.ip.equals(p_233846_)) {
            return $$1;
         }
      }

      for (ServerData $$2 : this.hiddenServerList) {
         if ($$2.ip.equals(p_233846_)) {
            return $$2;
         }
      }

      return null;
   }

   @Nullable
   public ServerData unhide(String p_233848_) {
      for (int $$1 = 0; $$1 < this.hiddenServerList.size(); $$1++) {
         ServerData $$2 = this.hiddenServerList.get($$1);
         if ($$2.ip.equals(p_233848_)) {
            this.hiddenServerList.remove($$1);
            this.serverList.add($$2);
            return $$2;
         }
      }

      return null;
   }

   public void remove(ServerData p_105441_) {
      if (!this.serverList.remove(p_105441_)) {
         this.hiddenServerList.remove(p_105441_);
      }
   }

   public void add(ServerData p_233843_, boolean p_233844_) {
      if (p_233844_) {
         this.hiddenServerList.add(0, p_233843_);

         while (this.hiddenServerList.size() > 16) {
            this.hiddenServerList.remove(this.hiddenServerList.size() - 1);
         }
      } else {
         this.serverList.add(p_233843_);
      }
   }

   public int size() {
      return this.serverList.size();
   }

   public void swap(int p_105435_, int p_105436_) {
      ServerData $$2 = this.get(p_105435_);
      this.serverList.set(p_105435_, this.get(p_105436_));
      this.serverList.set(p_105436_, $$2);
      this.save();
   }

   public void replace(int p_105438_, ServerData p_105439_) {
      this.serverList.set(p_105438_, p_105439_);
   }

   private static boolean set(ServerData p_233840_, List<ServerData> p_233841_) {
      for (int $$2 = 0; $$2 < p_233841_.size(); $$2++) {
         ServerData $$3 = p_233841_.get($$2);
         if ($$3.name.equals(p_233840_.name) && $$3.ip.equals(p_233840_.ip)) {
            p_233841_.set($$2, p_233840_);
            return true;
         }
      }

      return false;
   }

   public static void saveSingleServer(ServerData p_105447_) {
      IO_MAILBOX.tell(() -> {
         ServerList $$1 = new ServerList(Minecraft.getInstance());
         $$1.load();
         if (!set(p_105447_, $$1.serverList)) {
            set(p_105447_, $$1.hiddenServerList);
         }

         $$1.save();
      });
   }
}
