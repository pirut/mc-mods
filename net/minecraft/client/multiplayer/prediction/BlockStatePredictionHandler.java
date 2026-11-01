package net.minecraft.client.multiplayer.prediction;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.util.BlockSnapshot;

@OnlyIn(Dist.CLIENT)
public class BlockStatePredictionHandler implements AutoCloseable {
   private final Long2ObjectOpenHashMap<BlockStatePredictionHandler.ServerVerifiedState> serverVerifiedStates = new Long2ObjectOpenHashMap();
   private int currentSequenceNr;
   private boolean isPredicting;

   public void retainKnownServerState(BlockPos p_233868_, BlockState p_233869_, LocalPlayer p_233870_) {
      this.serverVerifiedStates
         .compute(
            p_233868_.asLong(),
            (p_352672_, p_352673_) -> p_352673_ != null
               ? p_352673_.setSequence(this.currentSequenceNr)
               : new BlockStatePredictionHandler.ServerVerifiedState(this.currentSequenceNr, p_233869_, p_233870_.position())
         );
   }

   public boolean updateKnownServerState(BlockPos p_233865_, BlockState p_233866_) {
      BlockStatePredictionHandler.ServerVerifiedState blockstatepredictionhandler$serververifiedstate = (BlockStatePredictionHandler.ServerVerifiedState)this.serverVerifiedStates
         .get(p_233865_.asLong());
      if (blockstatepredictionhandler$serververifiedstate == null) {
         return false;
      } else {
         blockstatepredictionhandler$serververifiedstate.setBlockState(p_233866_);
         return true;
      }
   }

   public void endPredictionsUpTo(int p_233857_, ClientLevel p_233858_) {
      ObjectIterator<Entry<BlockStatePredictionHandler.ServerVerifiedState>> objectiterator = this.serverVerifiedStates.long2ObjectEntrySet().iterator();

      while (objectiterator.hasNext()) {
         Entry<BlockStatePredictionHandler.ServerVerifiedState> entry = (Entry<BlockStatePredictionHandler.ServerVerifiedState>)objectiterator.next();
         BlockStatePredictionHandler.ServerVerifiedState blockstatepredictionhandler$serververifiedstate = (BlockStatePredictionHandler.ServerVerifiedState)entry.getValue();
         if (blockstatepredictionhandler$serververifiedstate.sequence <= p_233857_) {
            BlockPos blockpos = BlockPos.of(entry.getLongKey());
            objectiterator.remove();
            p_233858_.syncBlockState(
               blockpos, blockstatepredictionhandler$serververifiedstate.blockState, blockstatepredictionhandler$serververifiedstate.playerPos
            );
            if (blockstatepredictionhandler$serververifiedstate.snapshot != null
               && blockstatepredictionhandler$serververifiedstate.blockState == blockstatepredictionhandler$serververifiedstate.snapshot.getState()
               && blockstatepredictionhandler$serververifiedstate.snapshot.restoreBlockEntity(p_233858_, blockpos)) {
               p_233858_.sendBlockUpdated(
                  blockpos, blockstatepredictionhandler$serververifiedstate.blockState, blockstatepredictionhandler$serververifiedstate.blockState, 3
               );
            }
         }
      }
   }

   public BlockStatePredictionHandler startPredicting() {
      this.currentSequenceNr++;
      this.isPredicting = true;
      return this;
   }

   @Override
   public void close() {
      this.isPredicting = false;
   }

   public int currentSequence() {
      return this.currentSequenceNr;
   }

   public boolean isPredicting() {
      return this.isPredicting;
   }

   public void retainSnapshot(BlockPos pos, BlockSnapshot snapshot) {
      ((BlockStatePredictionHandler.ServerVerifiedState)this.serverVerifiedStates.get(pos.asLong())).snapshot = snapshot;
   }

   @OnlyIn(Dist.CLIENT)
   static class ServerVerifiedState {
      BlockSnapshot snapshot;
      final Vec3 playerPos;
      int sequence;
      BlockState blockState;

      ServerVerifiedState(int p_233878_, BlockState p_233879_, Vec3 p_233880_) {
         this.sequence = p_233878_;
         this.blockState = p_233879_;
         this.playerPos = p_233880_;
      }

      BlockStatePredictionHandler.ServerVerifiedState setSequence(int p_233882_) {
         this.sequence = p_233882_;
         return this;
      }

      void setBlockState(BlockState p_233884_) {
         this.blockState = p_233884_;
      }
   }
}
