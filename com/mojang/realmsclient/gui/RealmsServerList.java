package com.mojang.realmsclient.gui;

import com.mojang.realmsclient.dto.RealmsServer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsServerList implements Iterable<RealmsServer> {
   private final Minecraft minecraft;
   private final Set<RealmsServer> removedServers = new HashSet<>();
   private List<RealmsServer> servers = List.of();

   public RealmsServerList(Minecraft p_239233_) {
      this.minecraft = p_239233_;
   }

   public void updateServersList(List<RealmsServer> p_239869_) {
      List<RealmsServer> $$1 = new ArrayList<>(p_239869_);
      $$1.sort(new RealmsServer.McoServerComparator(this.minecraft.getUser().getName()));
      boolean $$2 = $$1.removeAll(this.removedServers);
      if (!$$2) {
         this.removedServers.clear();
      }

      this.servers = $$1;
   }

   public void removeItem(RealmsServer p_240077_) {
      this.servers.remove(p_240077_);
      this.removedServers.add(p_240077_);
   }

   @Override
   public Iterator<RealmsServer> iterator() {
      return this.servers.iterator();
   }

   public boolean isEmpty() {
      return this.servers.isEmpty();
   }
}
