package net.minecraft.world.level.block.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public abstract class ContainerOpenersCounter {
   private static final int CHECK_TICK_DELAY = 5;
   private int openCount;
   private double maxInteractionRange;

   protected abstract void onOpen(Level var1, BlockPos var2, BlockState var3);

   protected abstract void onClose(Level var1, BlockPos var2, BlockState var3);

   protected abstract void openerCountChanged(Level var1, BlockPos var2, BlockState var3, int var4, int var5);

   protected abstract boolean isOwnContainer(Player var1);

   public void incrementOpeners(Player p_155453_, Level p_155454_, BlockPos p_155455_, BlockState p_155456_) {
      int $$4 = this.openCount++;
      if ($$4 == 0) {
         this.onOpen(p_155454_, p_155455_, p_155456_);
         p_155454_.gameEvent(p_155453_, GameEvent.CONTAINER_OPEN, p_155455_);
         scheduleRecheck(p_155454_, p_155455_, p_155456_);
      }

      this.openerCountChanged(p_155454_, p_155455_, p_155456_, $$4, this.openCount);
      this.maxInteractionRange = Math.max(p_155453_.blockInteractionRange(), this.maxInteractionRange);
   }

   public void decrementOpeners(Player p_155469_, Level p_155470_, BlockPos p_155471_, BlockState p_155472_) {
      int $$4 = this.openCount--;
      if (this.openCount == 0) {
         this.onClose(p_155470_, p_155471_, p_155472_);
         p_155470_.gameEvent(p_155469_, GameEvent.CONTAINER_CLOSE, p_155471_);
         this.maxInteractionRange = 0.0;
      }

      this.openerCountChanged(p_155470_, p_155471_, p_155472_, $$4, this.openCount);
   }

   private List<Player> getPlayersWithContainerOpen(Level p_320335_, BlockPos p_320624_) {
      double $$2 = this.maxInteractionRange + 4.0;
      AABB $$3 = new AABB(p_320624_).inflate($$2);
      return p_320335_.getEntities(EntityTypeTest.forClass(Player.class), $$3, this::isOwnContainer);
   }

   public void recheckOpeners(Level p_155477_, BlockPos p_155478_, BlockState p_155479_) {
      List<Player> $$3 = this.getPlayersWithContainerOpen(p_155477_, p_155478_);
      this.maxInteractionRange = 0.0;

      for (Player $$4 : $$3) {
         this.maxInteractionRange = Math.max($$4.blockInteractionRange(), this.maxInteractionRange);
      }

      int $$5 = $$3.size();
      int $$6 = this.openCount;
      if ($$6 != $$5) {
         boolean $$7 = $$5 != 0;
         boolean $$8 = $$6 != 0;
         if ($$7 && !$$8) {
            this.onOpen(p_155477_, p_155478_, p_155479_);
            p_155477_.gameEvent(null, GameEvent.CONTAINER_OPEN, p_155478_);
         } else if (!$$7) {
            this.onClose(p_155477_, p_155478_, p_155479_);
            p_155477_.gameEvent(null, GameEvent.CONTAINER_CLOSE, p_155478_);
         }

         this.openCount = $$5;
      }

      this.openerCountChanged(p_155477_, p_155478_, p_155479_, $$6, $$5);
      if ($$5 > 0) {
         scheduleRecheck(p_155477_, p_155478_, p_155479_);
      }
   }

   public int getOpenerCount() {
      return this.openCount;
   }

   private static void scheduleRecheck(Level p_155481_, BlockPos p_155482_, BlockState p_155483_) {
      p_155481_.scheduleTick(p_155482_, p_155483_.getBlock(), 5);
   }
}
