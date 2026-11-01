package net.minecraft.world.level.gameevent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;

public class GameEventDispatcher {
   private final ServerLevel level;

   public GameEventDispatcher(ServerLevel p_251921_) {
      this.level = p_251921_;
   }

   public void post(Holder<GameEvent> p_316151_, Vec3 p_250613_, GameEvent.Context p_251777_) {
      int $$3 = p_316151_.value().notificationRadius();
      BlockPos $$4 = BlockPos.containing(p_250613_);
      int $$5 = SectionPos.blockToSectionCoord($$4.getX() - $$3);
      int $$6 = SectionPos.blockToSectionCoord($$4.getY() - $$3);
      int $$7 = SectionPos.blockToSectionCoord($$4.getZ() - $$3);
      int $$8 = SectionPos.blockToSectionCoord($$4.getX() + $$3);
      int $$9 = SectionPos.blockToSectionCoord($$4.getY() + $$3);
      int $$10 = SectionPos.blockToSectionCoord($$4.getZ() + $$3);
      List<GameEvent.ListenerInfo> $$11 = new ArrayList<>();
      GameEventListenerRegistry.ListenerVisitor $$12 = (p_316091_, p_316092_) -> {
         if (p_316091_.getDeliveryMode() == GameEventListener.DeliveryMode.BY_DISTANCE) {
            $$11.add(new GameEvent.ListenerInfo(p_316151_, p_250613_, p_251777_, p_316091_, p_316092_));
         } else {
            p_316091_.handleGameEvent(this.level, p_316151_, p_251777_, p_250613_);
         }
      };
      boolean $$13 = false;

      for (int $$14 = $$5; $$14 <= $$8; $$14++) {
         for (int $$15 = $$7; $$15 <= $$10; $$15++) {
            ChunkAccess $$16 = this.level.getChunkSource().getChunkNow($$14, $$15);
            if ($$16 != null) {
               for (int $$17 = $$6; $$17 <= $$9; $$17++) {
                  $$13 |= $$16.getListenerRegistry($$17).visitInRangeListeners(p_316151_, p_250613_, p_251777_, $$12);
               }
            }
         }
      }

      if (!$$11.isEmpty()) {
         this.handleGameEventMessagesInQueue($$11);
      }

      if ($$13) {
         DebugPackets.sendGameEventInfo(this.level, p_316151_, p_250613_);
      }
   }

   private void handleGameEventMessagesInQueue(List<GameEvent.ListenerInfo> p_251433_) {
      Collections.sort(p_251433_);

      for (GameEvent.ListenerInfo $$1 : p_251433_) {
         GameEventListener $$2 = $$1.recipient();
         $$2.handleGameEvent(this.level, $$1.gameEvent(), $$1.context(), $$1.source());
      }
   }
}
