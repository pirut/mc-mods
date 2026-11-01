package net.neoforged.neoforge.client.model.data;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.jetbrains.annotations.UnmodifiableView;

@EventBusSubscriber(
   modid = "neoforge",
   value = {Dist.CLIENT}
)
public class ModelDataManager {
   private final Thread owningThread = Thread.currentThread();
   private final Level level;
   private final Long2ObjectMap<Set<BlockPos>> needModelDataRefresh = new Long2ObjectOpenHashMap();
   private final Long2ObjectMap<Long2ObjectMap<ModelData>> modelDataCache = new Long2ObjectOpenHashMap();
   public static final Long2ObjectFunction<ModelData> EMPTY_SNAPSHOT = pos -> ModelData.EMPTY;

   public ModelDataManager(Level level) {
      this.level = level;
   }

   public void requestRefresh(BlockEntity blockEntity) {
      if (this.isOtherThread()) {
         throw new UnsupportedOperationException("Cannot request ModelData refresh outside the owning thread: " + this.owningThread);
      } else {
         Preconditions.checkNotNull(blockEntity, "BlockEntity must not be null");
         Preconditions.checkState(blockEntity.getLevel() == this.level, "BlockEntity does not belong to the level owning this manager");
         ((Set)this.needModelDataRefresh.computeIfAbsent(SectionPos.asLong(blockEntity.getBlockPos()), $ -> new HashSet())).add(blockEntity.getBlockPos());
      }
   }

   @UnmodifiableView
   public Long2ObjectMap<ModelData> getAt(SectionPos pos) {
      long sectionKey = pos.asLong();
      this.refreshAt(sectionKey);
      Long2ObjectMap<ModelData> map = (Long2ObjectMap<ModelData>)this.modelDataCache.get(sectionKey);
      return map != null ? Long2ObjectMaps.unmodifiable(map) : Long2ObjectMaps.emptyMap();
   }

   public ModelData getAt(BlockPos pos) {
      Preconditions.checkArgument(this.level.isClientSide, "Cannot request model data for server level");
      long sectionPos = SectionPos.asLong(pos);
      this.refreshAt(sectionPos);
      return (ModelData)((Long2ObjectMap)this.modelDataCache.getOrDefault(sectionPos, Long2ObjectMaps.emptyMap())).getOrDefault(pos.asLong(), ModelData.EMPTY);
   }

   public Long2ObjectFunction<ModelData> snapshotSectionRegion(
      int sectionMinX, int sectionMinY, int sectionMinZ, int sectionMaxX, int sectionMaxY, int sectionMaxZ
   ) {
      if (this.isOtherThread()) {
         throw new UnsupportedOperationException("Cannot snapshot active manager outside the owning thread: " + this.owningThread);
      } else {
         Long2ObjectMap<ModelData> cache = new Long2ObjectOpenHashMap();
         cache.defaultReturnValue(ModelData.EMPTY);

         for (int x = sectionMinX; x <= sectionMaxX; x++) {
            for (int y = sectionMinY; y <= sectionMaxY; y++) {
               for (int z = sectionMinZ; z <= sectionMaxZ; z++) {
                  long sectionPos = SectionPos.asLong(x, y, z);
                  this.refreshAt(sectionPos);
                  cache.putAll((Map)this.modelDataCache.getOrDefault(sectionPos, Long2ObjectMaps.emptyMap()));
               }
            }
         }

         return (Long2ObjectFunction<ModelData>)(cache.isEmpty() ? EMPTY_SNAPSHOT : cache);
      }
   }

   private void refreshAt(long section) {
      if (!this.isOtherThread()) {
         Set<BlockPos> needUpdate = (Set<BlockPos>)this.needModelDataRefresh.remove(section);
         if (needUpdate != null) {
            Long2ObjectMap<ModelData> data = (Long2ObjectMap<ModelData>)this.modelDataCache.computeIfAbsent(section, $ -> new Long2ObjectOpenHashMap());

            for (BlockPos pos : needUpdate) {
               BlockEntity toUpdate = this.level.getBlockEntity(pos);
               ModelData newData = ModelData.EMPTY;
               if (toUpdate != null && !toUpdate.isRemoved()) {
                  newData = toUpdate.getModelData();
                  if (newData == null) {
                     throw new NullPointerException("Null ModelData provided by " + BlockEntityType.getKey(toUpdate.getType()) + " @ " + pos);
                  }
               }

               if (newData != ModelData.EMPTY) {
                  data.put(pos.asLong(), newData);
               } else {
                  data.remove(pos.asLong());
               }
            }

            if (data.isEmpty()) {
               this.modelDataCache.remove(section);
            }
         }
      }
   }

   private boolean isOtherThread() {
      return Thread.currentThread() != this.owningThread;
   }

   @SubscribeEvent
   public static void onChunkUnload(ChunkEvent.Unload event) {
      Level level = event.getChunk().getLevel();
      if (level != null) {
         ModelDataManager modelDataManager = level.getModelDataManager();
         if (modelDataManager != null) {
            ChunkPos chunk = event.getChunk().getPos();
            int maxSection = level.getMaxSection();

            for (int y = level.getMinSection(); y < maxSection; y++) {
               long section = SectionPos.asLong(chunk.x, y, chunk.z);
               modelDataManager.needModelDataRefresh.remove(section);
               modelDataManager.modelDataCache.remove(section);
            }
         }
      }
   }
}
