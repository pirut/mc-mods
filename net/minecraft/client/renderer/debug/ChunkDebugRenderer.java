package net.minecraft.client.renderer.debug;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChunkDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   final Minecraft minecraft;
   private double lastUpdateTime = Double.MIN_VALUE;
   private final int radius = 12;
   @Nullable
   private ChunkDebugRenderer.ChunkData data;

   public ChunkDebugRenderer(Minecraft p_113368_) {
      this.minecraft = p_113368_;
   }

   @Override
   public void render(PoseStack p_113370_, MultiBufferSource p_113371_, double p_113372_, double p_113373_, double p_113374_) {
      double $$5 = Util.getNanos();
      if ($$5 - this.lastUpdateTime > 3.0E9) {
         this.lastUpdateTime = $$5;
         IntegratedServer $$6 = this.minecraft.getSingleplayerServer();
         if ($$6 != null) {
            this.data = new ChunkDebugRenderer.ChunkData($$6, p_113372_, p_113374_);
         } else {
            this.data = null;
         }
      }

      if (this.data != null) {
         Map<ChunkPos, String> $$7 = this.data.serverData.getNow(null);
         double $$8 = this.minecraft.gameRenderer.getMainCamera().getPosition().y * 0.85;

         for (Entry<ChunkPos, String> $$9 : this.data.clientData.entrySet()) {
            ChunkPos $$10 = $$9.getKey();
            String $$11 = $$9.getValue();
            if ($$7 != null) {
               $$11 = $$11 + $$7.get($$10);
            }

            String[] $$12 = $$11.split("\n");
            int $$13 = 0;

            for (String $$14 : $$12) {
               DebugRenderer.renderFloatingText(
                  p_113370_,
                  p_113371_,
                  $$14,
                  SectionPos.sectionToBlockCoord($$10.x, 8),
                  $$8 + $$13,
                  SectionPos.sectionToBlockCoord($$10.z, 8),
                  -1,
                  0.15F,
                  true,
                  0.0F,
                  true
               );
               $$13 -= 2;
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   final class ChunkData {
      final Map<ChunkPos, String> clientData;
      final CompletableFuture<Map<ChunkPos, String>> serverData;

      ChunkData(IntegratedServer p_113382_, double p_113383_, double p_113384_) {
         ClientLevel $$3 = ChunkDebugRenderer.this.minecraft.level;
         ResourceKey<Level> $$4 = $$3.dimension();
         int $$5 = SectionPos.posToSectionCoord(p_113383_);
         int $$6 = SectionPos.posToSectionCoord(p_113384_);
         Builder<ChunkPos, String> $$7 = ImmutableMap.builder();
         ClientChunkCache $$8 = $$3.getChunkSource();

         for (int $$9 = $$5 - 12; $$9 <= $$5 + 12; $$9++) {
            for (int $$10 = $$6 - 12; $$10 <= $$6 + 12; $$10++) {
               ChunkPos $$11 = new ChunkPos($$9, $$10);
               String $$12 = "";
               LevelChunk $$13 = $$8.getChunk($$9, $$10, false);
               $$12 = $$12 + "Client: ";
               if ($$13 == null) {
                  $$12 = $$12 + "0n/a\n";
               } else {
                  $$12 = $$12 + ($$13.isEmpty() ? " E" : "");
                  $$12 = $$12 + "\n";
               }

               $$7.put($$11, $$12);
            }
         }

         this.clientData = $$7.build();
         this.serverData = p_113382_.submit(() -> {
            ServerLevel $$4x = p_113382_.getLevel($$4);
            if ($$4x == null) {
               return ImmutableMap.of();
            } else {
               Builder<ChunkPos, String> $$5x = ImmutableMap.builder();
               ServerChunkCache $$6x = $$4x.getChunkSource();

               for (int $$7x = $$5 - 12; $$7x <= $$5 + 12; $$7x++) {
                  for (int $$8x = $$6 - 12; $$8x <= $$6 + 12; $$8x++) {
                     ChunkPos $$9x = new ChunkPos($$7x, $$8x);
                     $$5x.put($$9x, "Server: " + $$6x.getChunkDebugData($$9x));
                  }
               }

               return $$5x.build();
            }
         });
      }
   }
}
