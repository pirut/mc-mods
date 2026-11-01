package net.minecraft.world.level.entity;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import java.util.Objects;
import java.util.Spliterators;
import java.util.PrimitiveIterator.OfLong;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.core.SectionPos;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class EntitySectionStorage<T extends EntityAccess> {
   public static final int CHONKY_ENTITY_SEARCH_GRACE = 2;
   public static final int MAX_NON_CHONKY_ENTITY_SIZE = 4;
   private final Class<T> entityClass;
   private final Long2ObjectFunction<Visibility> intialSectionVisibility;
   private final Long2ObjectMap<EntitySection<T>> sections = new Long2ObjectOpenHashMap();
   private final LongSortedSet sectionIds = new LongAVLTreeSet();

   public EntitySectionStorage(Class<T> entityClass, Long2ObjectFunction<Visibility> initialSectionVisibility) {
      this.entityClass = entityClass;
      this.intialSectionVisibility = initialSectionVisibility;
   }

   public void forEachAccessibleNonEmptySection(AABB boundingBox, AbortableIterationConsumer<EntitySection<T>> consumer) {
      int i = SectionPos.posToSectionCoord(boundingBox.minX - 2.0);
      int j = SectionPos.posToSectionCoord(boundingBox.minY - 4.0);
      int k = SectionPos.posToSectionCoord(boundingBox.minZ - 2.0);
      int l = SectionPos.posToSectionCoord(boundingBox.maxX + 2.0);
      int m = SectionPos.posToSectionCoord(boundingBox.maxY + 0.0);
      int n = SectionPos.posToSectionCoord(boundingBox.maxZ + 2.0);

      for (int o = i; o <= l; o++) {
         long p = SectionPos.asLong(o, 0, 0);
         long q = SectionPos.asLong(o, -1, -1);
         LongIterator longIterator = this.sectionIds.subSet(p, q + 1L).iterator();

         while (longIterator.hasNext()) {
            long r = longIterator.nextLong();
            int s = SectionPos.y(r);
            int t = SectionPos.z(r);
            if (s >= j && s <= m && t >= k && t <= n) {
               EntitySection<T> entitySection = (EntitySection<T>)this.sections.get(r);
               if (entitySection != null
                  && !entitySection.isEmpty()
                  && entitySection.getStatus().isAccessible()
                  && consumer.accept(entitySection).shouldAbort()) {
                  return;
               }
            }
         }
      }
   }

   public LongStream getExistingSectionPositionsInChunk(long pos) {
      int i = ChunkPos.getX(pos);
      int j = ChunkPos.getZ(pos);
      LongSortedSet longSortedSet = this.getChunkSections(i, j);
      if (longSortedSet.isEmpty()) {
         return LongStream.empty();
      } else {
         OfLong ofLong = longSortedSet.iterator();
         return StreamSupport.longStream(Spliterators.spliteratorUnknownSize(ofLong, 1301), false);
      }
   }

   private LongSortedSet getChunkSections(int x, int z) {
      long l = SectionPos.asLong(x, 0, z);
      long m = SectionPos.asLong(x, -1, z);
      return this.sectionIds.subSet(l, m + 1L);
   }

   public Stream<EntitySection<T>> getExistingSectionsInChunk(long pos) {
      return this.getExistingSectionPositionsInChunk(pos).<EntitySection<T>>mapToObj(this.sections::get).filter(Objects::nonNull);
   }

   private static long getChunkKeyFromSectionKey(long pos) {
      return ChunkPos.asLong(SectionPos.x(pos), SectionPos.z(pos));
   }

   public EntitySection<T> getOrCreateSection(long sectionPos) {
      return (EntitySection<T>)this.sections.computeIfAbsent(sectionPos, this::createSection);
   }

   @Nullable
   public EntitySection<T> getSection(long sectionPos) {
      return (EntitySection<T>)this.sections.get(sectionPos);
   }

   private EntitySection<T> createSection(long sectionPos) {
      long l = getChunkKeyFromSectionKey(sectionPos);
      Visibility visibility = (Visibility)this.intialSectionVisibility.get(l);
      this.sectionIds.add(sectionPos);
      return new EntitySection<>(this.entityClass, visibility);
   }

   public LongSet getAllChunksWithExistingSections() {
      LongSet longSet = new LongOpenHashSet();
      this.sections.keySet().forEach(l -> longSet.add(getChunkKeyFromSectionKey(l)));
      return longSet;
   }

   public void getEntities(AABB bounds, AbortableIterationConsumer<T> consumer) {
      this.forEachAccessibleNonEmptySection(bounds, entitySection -> entitySection.getEntities(bounds, consumer));
   }

   public <U extends T> void getEntities(EntityTypeTest<T, U> test, AABB bounds, AbortableIterationConsumer<U> consumer) {
      this.forEachAccessibleNonEmptySection(bounds, entitySection -> entitySection.getEntities(test, bounds, consumer));
   }

   public void remove(long sectionId) {
      this.sections.remove(sectionId);
      this.sectionIds.remove(sectionId);
   }

   @VisibleForDebug
   public int count() {
      return this.sectionIds.size();
   }
}
