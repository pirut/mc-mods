package net.minecraft.util.debugchart;

import com.google.common.collect.Maps;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import net.minecraft.Util;
import net.minecraft.network.protocol.game.ClientboundDebugSamplePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

public class DebugSampleSubscriptionTracker {
   public static final int STOP_SENDING_AFTER_TICKS = 200;
   public static final int STOP_SENDING_AFTER_MS = 10000;
   private final PlayerList playerList;
   private final EnumMap<RemoteDebugSampleType, Map<ServerPlayer, DebugSampleSubscriptionTracker.SubscriptionStartedAt>> subscriptions;
   private final Queue<DebugSampleSubscriptionTracker.SubscriptionRequest> subscriptionRequestQueue = new LinkedList<>();

   public DebugSampleSubscriptionTracker(PlayerList p_323956_) {
      this.playerList = p_323956_;
      this.subscriptions = new EnumMap<>(RemoteDebugSampleType.class);

      for (RemoteDebugSampleType $$1 : RemoteDebugSampleType.values()) {
         this.subscriptions.put($$1, Maps.newHashMap());
      }
   }

   public boolean shouldLogSamples(RemoteDebugSampleType p_324431_) {
      return !this.subscriptions.get(p_324431_).isEmpty();
   }

   public void broadcast(ClientboundDebugSamplePacket p_324398_) {
      for (ServerPlayer $$2 : this.subscriptions.get(p_324398_.debugSampleType()).keySet()) {
         $$2.connection.send(p_324398_);
      }
   }

   public void subscribe(ServerPlayer p_324211_, RemoteDebugSampleType p_324355_) {
      if (this.playerList.isOp(p_324211_.getGameProfile())) {
         this.subscriptionRequestQueue.add(new DebugSampleSubscriptionTracker.SubscriptionRequest(p_324211_, p_324355_));
      }
   }

   public void tick(int p_323889_) {
      long $$1 = Util.getMillis();
      this.handleSubscriptions($$1, p_323889_);
      this.handleUnsubscriptions($$1, p_323889_);
   }

   private void handleSubscriptions(long p_324350_, int p_323574_) {
      for (DebugSampleSubscriptionTracker.SubscriptionRequest $$2 : this.subscriptionRequestQueue) {
         this.subscriptions.get($$2.sampleType()).put($$2.player(), new DebugSampleSubscriptionTracker.SubscriptionStartedAt(p_324350_, p_323574_));
      }
   }

   private void handleUnsubscriptions(long p_323971_, int p_324253_) {
      for (Map<ServerPlayer, DebugSampleSubscriptionTracker.SubscriptionStartedAt> $$2 : this.subscriptions.values()) {
         $$2.entrySet().removeIf(p_323887_ -> {
            boolean $$3 = !this.playerList.isOp(p_323887_.getKey().getGameProfile());
            DebugSampleSubscriptionTracker.SubscriptionStartedAt $$4 = p_323887_.getValue();
            return $$3 || p_324253_ > $$4.tick() + 200 && p_323971_ > $$4.millis() + 10000L;
         });
      }
   }

   record SubscriptionRequest(ServerPlayer player, RemoteDebugSampleType sampleType) {
   }

   record SubscriptionStartedAt(long millis, int tick) {
   }
}
