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
import javax.annotation.Nullable;
import net.minecraft.core.SectionPos;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

public class EntitySectionStorage<T extends EntityAccess> {
   private final Class<T> entityClass;
   private final Long2ObjectFunction<Visibility> intialSectionVisibility;
   private final Long2ObjectMap<EntitySection<T>> sections = new Long2ObjectOpenHashMap();
   private final LongSortedSet sectionIds = new LongAVLTreeSet();

   public EntitySectionStorage(Class<T> p_156855_, Long2ObjectFunction<Visibility> p_156856_) {
      this.entityClass = p_156855_;
      this.intialSectionVisibility = p_156856_;
   }

   public void forEachAccessibleNonEmptySection(AABB p_188363_, AbortableIterationConsumer<EntitySection<T>> p_261588_) {
      int $$2 = 2;
      int $$3 = SectionPos.posToSectionCoord(p_188363_.minX - 2.0);
      int $$4 = SectionPos.posToSectionCoord(p_188363_.minY - 4.0);
      int $$5 = SectionPos.posToSectionCoord(p_188363_.minZ - 2.0);
      int $$6 = SectionPos.posToSectionCoord(p_188363_.maxX + 2.0);
      int $$7 = SectionPos.posToSectionCoord(p_188363_.maxY + 0.0);
      int $$8 = SectionPos.posToSectionCoord(p_188363_.maxZ + 2.0);

      for (int $$9 = $$3; $$9 <= $$6; $$9++) {
         long $$10 = SectionPos.asLong($$9, 0, 0);
         long $$11 = SectionPos.asLong($$9, -1, -1);
         LongIterator $$12 = this.sectionIds.subSet($$10, $$11 + 1L).iterator();

         while ($$12.hasNext()) {
            long $$13 = $$12.nextLong();
            int $$14 = SectionPos.y($$13);
            int $$15 = SectionPos.z($$13);
            if ($$14 >= $$4 && $$14 <= $$7 && $$15 >= $$5 && $$15 <= $$8) {
               EntitySection<T> $$16 = (EntitySection<T>)this.sections.get($$13);
               if ($$16 != null && !$$16.isEmpty() && $$16.getStatus().isAccessible() && p_261588_.accept($$16).shouldAbort()) {
                  return;
               }
            }
         }
      }
   }

   public LongStream getExistingSectionPositionsInChunk(long p_156862_) {
      int $$1 = ChunkPos.getX(p_156862_);
      int $$2 = ChunkPos.getZ(p_156862_);
      LongSortedSet $$3 = this.getChunkSections($$1, $$2);
      if ($$3.isEmpty()) {
         return LongStream.empty();
      } else {
         OfLong $$4 = $$3.iterator();
         return StreamSupport.longStream(Spliterators.spliteratorUnknownSize($$4, 1301), false);
      }
   }

   private LongSortedSet getChunkSections(int p_156859_, int p_156860_) {
      long $$2 = SectionPos.asLong(p_156859_, 0, p_156860_);
      long $$3 = SectionPos.asLong(p_156859_, -1, p_156860_);
      return this.sectionIds.subSet($$2, $$3 + 1L);
   }

   public Stream<EntitySection<T>> getExistingSectionsInChunk(long p_156889_) {
      return this.getExistingSectionPositionsInChunk(p_156889_).<EntitySection<T>>mapToObj(this.sections::get).filter(Objects::nonNull);
   }

   private static long getChunkKeyFromSectionKey(long p_156900_) {
      return ChunkPos.asLong(SectionPos.x(p_156900_), SectionPos.z(p_156900_));
   }

   public EntitySection<T> getOrCreateSection(long p_156894_) {
      return (EntitySection<T>)this.sections.computeIfAbsent(p_156894_, this::createSection);
   }

   @Nullable
   public EntitySection<T> getSection(long p_156896_) {
      return (EntitySection<T>)this.sections.get(p_156896_);
   }

   private EntitySection<T> createSection(long p_156902_) {
      long $$1 = getChunkKeyFromSectionKey(p_156902_);
      Visibility $$2 = (Visibility)this.intialSectionVisibility.get($$1);
      this.sectionIds.add(p_156902_);
      return new EntitySection<>(this.entityClass, $$2);
   }

   public LongSet getAllChunksWithExistingSections() {
      LongSet $$0 = new LongOpenHashSet();
      this.sections.keySet().forEach(p_156886_ -> $$0.add(getChunkKeyFromSectionKey(p_156886_)));
      return $$0;
   }

   public void getEntities(AABB p_261820_, AbortableIterationConsumer<T> p_261992_) {
      this.forEachAccessibleNonEmptySection(p_261820_, p_261459_ -> p_261459_.getEntities(p_261820_, p_261992_));
   }

   public <U extends T> void getEntities(EntityTypeTest<T, U> p_261630_, AABB p_261843_, AbortableIterationConsumer<U> p_261742_) {
      this.forEachAccessibleNonEmptySection(p_261843_, p_261463_ -> p_261463_.getEntities(p_261630_, p_261843_, p_261742_));
   }

   public void remove(long p_156898_) {
      this.sections.remove(p_156898_);
      this.sectionIds.remove(p_156898_);
   }

   @VisibleForDebug
   public int count() {
      return this.sectionIds.size();
   }
}
