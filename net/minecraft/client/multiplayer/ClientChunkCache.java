package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientChunkCache extends ChunkSource {
   static final Logger LOGGER = LogUtils.getLogger();
   private final LevelChunk emptyChunk;
   private final LevelLightEngine lightEngine;
   volatile ClientChunkCache.Storage storage;
   final ClientLevel level;

   public ClientChunkCache(ClientLevel p_104414_, int p_104415_) {
      this.level = p_104414_;
      this.emptyChunk = new EmptyLevelChunk(p_104414_, new ChunkPos(0, 0), p_104414_.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS));
      this.lightEngine = new LevelLightEngine(this, true, p_104414_.dimensionType().hasSkyLight());
      this.storage = new ClientChunkCache.Storage(calculateStorageRange(p_104415_));
   }

   @Override
   public LevelLightEngine getLightEngine() {
      return this.lightEngine;
   }

   private static boolean isValidChunk(@Nullable LevelChunk p_104439_, int p_104440_, int p_104441_) {
      if (p_104439_ == null) {
         return false;
      } else {
         ChunkPos chunkpos = p_104439_.getPos();
         return chunkpos.x == p_104440_ && chunkpos.z == p_104441_;
      }
   }

   public void drop(ChunkPos p_295783_) {
      if (this.storage.inRange(p_295783_.x, p_295783_.z)) {
         int i = this.storage.getIndex(p_295783_.x, p_295783_.z);
         LevelChunk levelchunk = this.storage.getChunk(i);
         if (isValidChunk(levelchunk, p_295783_.x, p_295783_.z)) {
            NeoForge.EVENT_BUS.post(new ChunkEvent.Unload(levelchunk));
            this.storage.drop(i, levelchunk);
         }
      }
   }

   @Nullable
   public LevelChunk getChunk(int p_104451_, int p_104452_, ChunkStatus p_330230_, boolean p_104454_) {
      if (this.storage.inRange(p_104451_, p_104452_)) {
         LevelChunk levelchunk = this.storage.getChunk(this.storage.getIndex(p_104451_, p_104452_));
         if (isValidChunk(levelchunk, p_104451_, p_104452_)) {
            return levelchunk;
         }
      }

      return p_104454_ ? this.emptyChunk : null;
   }

   @Override
   public BlockGetter getLevel() {
      return this.level;
   }

   public void replaceBiomes(int p_275374_, int p_275226_, FriendlyByteBuf p_275745_) {
      if (!this.storage.inRange(p_275374_, p_275226_)) {
         LOGGER.warn("Ignoring chunk since it's not in the view range: {}, {}", p_275374_, p_275226_);
      } else {
         int i = this.storage.getIndex(p_275374_, p_275226_);
         LevelChunk levelchunk = this.storage.chunks.get(i);
         if (!isValidChunk(levelchunk, p_275374_, p_275226_)) {
            LOGGER.warn("Ignoring chunk since it's not present: {}, {}", p_275374_, p_275226_);
         } else {
            levelchunk.replaceBiomes(p_275745_);
         }
      }
   }

   @Nullable
   public LevelChunk replaceWithPacketData(
      int p_194117_,
      int p_194118_,
      FriendlyByteBuf p_194119_,
      Map<Heightmap.Types, long[]> p_405767_,
      Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> p_194121_
   ) {
      if (!this.storage.inRange(p_194117_, p_194118_)) {
         LOGGER.warn("Ignoring chunk since it's not in the view range: {}, {}", p_194117_, p_194118_);
         return null;
      } else {
         int i = this.storage.getIndex(p_194117_, p_194118_);
         LevelChunk levelchunk = this.storage.chunks.get(i);
         ChunkPos chunkpos = new ChunkPos(p_194117_, p_194118_);
         if (!isValidChunk(levelchunk, p_194117_, p_194118_)) {
            levelchunk = new LevelChunk(this.level, chunkpos);
            levelchunk.replaceWithPacketData(p_194119_, p_405767_, p_194121_);
            this.storage.replace(i, levelchunk);
         } else {
            levelchunk.replaceWithPacketData(p_194119_, p_405767_, p_194121_);
            this.storage.refreshEmptySections(levelchunk);
         }

         this.level.onChunkLoaded(chunkpos);
         NeoForge.EVENT_BUS.post(new ChunkEvent.Load(levelchunk, false));
         return levelchunk;
      }
   }

   @Override
   public void tick(BooleanSupplier p_202421_, boolean p_202422_) {
   }

   public void updateViewCenter(int p_104460_, int p_104461_) {
      this.storage.viewCenterX = p_104460_;
      this.storage.viewCenterZ = p_104461_;
   }

   public void updateViewRadius(int p_104417_) {
      int i = this.storage.chunkRadius;
      int j = calculateStorageRange(p_104417_);
      if (i != j) {
         ClientChunkCache.Storage clientchunkcache$storage = new ClientChunkCache.Storage(j);
         clientchunkcache$storage.viewCenterX = this.storage.viewCenterX;
         clientchunkcache$storage.viewCenterZ = this.storage.viewCenterZ;

         for (int k = 0; k < this.storage.chunks.length(); k++) {
            LevelChunk levelchunk = this.storage.chunks.get(k);
            if (levelchunk != null) {
               ChunkPos chunkpos = levelchunk.getPos();
               if (clientchunkcache$storage.inRange(chunkpos.x, chunkpos.z)) {
                  clientchunkcache$storage.replace(clientchunkcache$storage.getIndex(chunkpos.x, chunkpos.z), levelchunk);
               }
            }
         }

         this.storage = clientchunkcache$storage;
      }
   }

   private static int calculateStorageRange(int p_104449_) {
      return Math.max(2, p_104449_) + 3;
   }

   @Override
   public String gatherStats() {
      return this.storage.chunks.length() + ", " + this.getLoadedChunksCount();
   }

   @Override
   public int getLoadedChunksCount() {
      return this.storage.chunkCount;
   }

   @Override
   public void onLightUpdate(LightLayer p_104436_, SectionPos p_104437_) {
      Minecraft.getInstance().levelRenderer.setSectionDirty(p_104437_.x(), p_104437_.y(), p_104437_.z());
   }

   public LongOpenHashSet getLoadedEmptySections() {
      return this.storage.loadedEmptySections;
   }

   @Override
   public void onSectionEmptinessChanged(int p_366524_, int p_366407_, int p_366651_, boolean p_366887_) {
      this.storage.onSectionEmptinessChanged(p_366524_, p_366407_, p_366651_, p_366887_);
   }

   @OnlyIn(Dist.CLIENT)
   final class Storage {
      final AtomicReferenceArray<LevelChunk> chunks;
      final LongOpenHashSet loadedEmptySections = new LongOpenHashSet();
      final int chunkRadius;
      private final int viewRange;
      volatile int viewCenterX;
      volatile int viewCenterZ;
      int chunkCount;

      Storage(int p_104474_) {
         this.chunkRadius = p_104474_;
         this.viewRange = p_104474_ * 2 + 1;
         this.chunks = new AtomicReferenceArray<>(this.viewRange * this.viewRange);
      }

      int getIndex(int p_104482_, int p_104483_) {
         return Math.floorMod(p_104483_, this.viewRange) * this.viewRange + Math.floorMod(p_104482_, this.viewRange);
      }

      void replace(int p_104485_, @Nullable LevelChunk p_104486_) {
         LevelChunk levelchunk = this.chunks.getAndSet(p_104485_, p_104486_);
         if (levelchunk != null) {
            this.chunkCount--;
            this.dropEmptySections(levelchunk);
            ClientChunkCache.this.level.unload(levelchunk);
         }

         if (p_104486_ != null) {
            this.chunkCount++;
            this.addEmptySections(p_104486_);
         }
      }

      void drop(int p_366627_, LevelChunk p_366475_) {
         if (this.chunks.compareAndSet(p_366627_, p_366475_, null)) {
            this.chunkCount--;
            this.dropEmptySections(p_366475_);
         }

         ClientChunkCache.this.level.unload(p_366475_);
      }

      public void onSectionEmptinessChanged(int p_366606_, int p_366859_, int p_366870_, boolean p_366411_) {
         if (this.inRange(p_366606_, p_366870_)) {
            long i = SectionPos.asLong(p_366606_, p_366859_, p_366870_);
            if (p_366411_) {
               this.loadedEmptySections.add(i);
            } else if (this.loadedEmptySections.remove(i)) {
               ClientChunkCache.this.level.onSectionBecomingNonEmpty(i);
            }
         }
      }

      private void dropEmptySections(LevelChunk p_366765_) {
         LevelChunkSection[] alevelchunksection = p_366765_.getSections();

         for (int i = 0; i < alevelchunksection.length; i++) {
            ChunkPos chunkpos = p_366765_.getPos();
            this.loadedEmptySections.remove(SectionPos.asLong(chunkpos.x, p_366765_.getSectionYFromSectionIndex(i), chunkpos.z));
         }
      }

      private void addEmptySections(LevelChunk p_366694_) {
         LevelChunkSection[] alevelchunksection = p_366694_.getSections();

         for (int i = 0; i < alevelchunksection.length; i++) {
            LevelChunkSection levelchunksection = alevelchunksection[i];
            if (levelchunksection.hasOnlyAir()) {
               ChunkPos chunkpos = p_366694_.getPos();
               this.loadedEmptySections.add(SectionPos.asLong(chunkpos.x, p_366694_.getSectionYFromSectionIndex(i), chunkpos.z));
            }
         }
      }

      void refreshEmptySections(LevelChunk p_386574_) {
         ChunkPos chunkpos = p_386574_.getPos();
         LevelChunkSection[] alevelchunksection = p_386574_.getSections();

         for (int i = 0; i < alevelchunksection.length; i++) {
            LevelChunkSection levelchunksection = alevelchunksection[i];
            long j = SectionPos.asLong(chunkpos.x, p_386574_.getSectionYFromSectionIndex(i), chunkpos.z);
            if (levelchunksection.hasOnlyAir()) {
               this.loadedEmptySections.add(j);
            } else if (this.loadedEmptySections.remove(j)) {
               ClientChunkCache.this.level.onSectionBecomingNonEmpty(j);
            }
         }
      }

      boolean inRange(int p_104501_, int p_104502_) {
         return Math.abs(p_104501_ - this.viewCenterX) <= this.chunkRadius && Math.abs(p_104502_ - this.viewCenterZ) <= this.chunkRadius;
      }

      @Nullable
      protected LevelChunk getChunk(int p_104480_) {
         return this.chunks.get(p_104480_);
      }

      private void dumpChunks(String p_171623_) {
         try (FileOutputStream fileoutputstream = new FileOutputStream(p_171623_)) {
            int i = ClientChunkCache.this.storage.chunkRadius;

            for (int j = this.viewCenterZ - i; j <= this.viewCenterZ + i; j++) {
               for (int k = this.viewCenterX - i; k <= this.viewCenterX + i; k++) {
                  LevelChunk levelchunk = ClientChunkCache.this.storage.chunks.get(ClientChunkCache.this.storage.getIndex(k, j));
                  if (levelchunk != null) {
                     ChunkPos chunkpos = levelchunk.getPos();
                     fileoutputstream.write((chunkpos.x + "\t" + chunkpos.z + "\t" + levelchunk.isEmpty() + "\n").getBytes(StandardCharsets.UTF_8));
                  }
               }
            }
         } catch (IOException var10) {
            ClientChunkCache.LOGGER.error("Failed to dump chunks to file {}", p_171623_, var10);
         }
      }
   }
}
