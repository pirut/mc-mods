package net.minecraft.world.entity.monster.warden;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Streams;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class AngerManagement {
   @VisibleForTesting
   protected static final int CONVERSION_DELAY = 2;
   @VisibleForTesting
   protected static final int MAX_ANGER = 150;
   private static final int DEFAULT_ANGER_DECREASE = 1;
   private int conversionDelay = Mth.randomBetweenInclusive(RandomSource.create(), 0, 2);
   int highestAnger;
   private static final Codec<Pair<UUID, Integer>> SUSPECT_ANGER_PAIR = RecordCodecBuilder.create(
      p_253580_ -> p_253580_.group(
            UUIDUtil.CODEC.fieldOf("uuid").forGetter(Pair::getFirst), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("anger").forGetter(Pair::getSecond)
         )
         .apply(p_253580_, Pair::of)
   );
   private final Predicate<Entity> filter;
   @VisibleForTesting
   protected final ArrayList<Entity> suspects;
   private final AngerManagement.Sorter suspectSorter;
   @VisibleForTesting
   protected final Object2IntMap<Entity> angerBySuspect;
   @VisibleForTesting
   protected final Object2IntMap<UUID> angerByUuid;

   public static Codec<AngerManagement> codec(Predicate<Entity> p_219278_) {
      return RecordCodecBuilder.create(
         p_219281_ -> p_219281_.group(
               SUSPECT_ANGER_PAIR.listOf().fieldOf("suspects").orElse(Collections.emptyList()).forGetter(AngerManagement::createUuidAngerPairs)
            )
            .apply(p_219281_, p_219284_ -> new AngerManagement(p_219278_, p_219284_))
      );
   }

   public AngerManagement(Predicate<Entity> p_219254_, List<Pair<UUID, Integer>> p_219255_) {
      this.filter = p_219254_;
      this.suspects = new ArrayList<>();
      this.suspectSorter = new AngerManagement.Sorter(this);
      this.angerBySuspect = new Object2IntOpenHashMap();
      this.angerByUuid = new Object2IntOpenHashMap(p_219255_.size());
      p_219255_.forEach(p_219272_ -> this.angerByUuid.put((UUID)p_219272_.getFirst(), (Integer)p_219272_.getSecond()));
   }

   private List<Pair<UUID, Integer>> createUuidAngerPairs() {
      return Streams.concat(
            new Stream[]{
               this.suspects.stream().map(p_219295_ -> Pair.of(p_219295_.getUUID(), this.angerBySuspect.getInt(p_219295_))),
               this.angerByUuid.object2IntEntrySet().stream().map(p_219276_ -> Pair.of((UUID)p_219276_.getKey(), p_219276_.getIntValue()))
            }
         )
         .collect(Collectors.toList());
   }

   public void tick(ServerLevel p_219264_, Predicate<Entity> p_219265_) {
      this.conversionDelay--;
      if (this.conversionDelay <= 0) {
         this.convertFromUuids(p_219264_);
         this.conversionDelay = 2;
      }

      ObjectIterator<Entry<UUID>> $$2 = this.angerByUuid.object2IntEntrySet().iterator();

      while ($$2.hasNext()) {
         Entry<UUID> $$3 = (Entry<UUID>)$$2.next();
         int $$4 = $$3.getIntValue();
         if ($$4 <= 1) {
            $$2.remove();
         } else {
            $$3.setValue($$4 - 1);
         }
      }

      ObjectIterator<Entry<Entity>> $$5 = this.angerBySuspect.object2IntEntrySet().iterator();

      while ($$5.hasNext()) {
         Entry<Entity> $$6 = (Entry<Entity>)$$5.next();
         int $$7 = $$6.getIntValue();
         Entity $$8 = (Entity)$$6.getKey();
         Entity.RemovalReason $$9 = $$8.getRemovalReason();
         if ($$7 > 1 && p_219265_.test($$8) && $$9 == null) {
            $$6.setValue($$7 - 1);
         } else {
            this.suspects.remove($$8);
            $$5.remove();
            if ($$7 > 1 && $$9 != null) {
               switch ($$9) {
                  case CHANGED_DIMENSION:
                  case UNLOADED_TO_CHUNK:
                  case UNLOADED_WITH_PLAYER:
                     this.angerByUuid.put($$8.getUUID(), $$7 - 1);
               }
            }
         }
      }

      this.sortAndUpdateHighestAnger();
   }

   private void sortAndUpdateHighestAnger() {
      this.highestAnger = 0;
      this.suspects.sort(this.suspectSorter);
      if (this.suspects.size() == 1) {
         this.highestAnger = this.angerBySuspect.getInt(this.suspects.get(0));
      }
   }

   private void convertFromUuids(ServerLevel p_219262_) {
      ObjectIterator<Entry<UUID>> $$1 = this.angerByUuid.object2IntEntrySet().iterator();

      while ($$1.hasNext()) {
         Entry<UUID> $$2 = (Entry<UUID>)$$1.next();
         int $$3 = $$2.getIntValue();
         Entity $$4 = p_219262_.getEntity((UUID)$$2.getKey());
         if ($$4 != null) {
            this.angerBySuspect.put($$4, $$3);
            this.suspects.add($$4);
            $$1.remove();
         }
      }
   }

   public int increaseAnger(Entity p_219269_, int p_219270_) {
      boolean $$2 = !this.angerBySuspect.containsKey(p_219269_);
      int $$3 = this.angerBySuspect.computeInt(p_219269_, (p_219259_, p_219260_) -> Math.min(150, (p_219260_ == null ? 0 : p_219260_) + p_219270_));
      if ($$2) {
         int $$4 = this.angerByUuid.removeInt(p_219269_.getUUID());
         $$3 += $$4;
         this.angerBySuspect.put(p_219269_, $$3);
         this.suspects.add(p_219269_);
      }

      this.sortAndUpdateHighestAnger();
      return $$3;
   }

   public void clearAnger(Entity p_219267_) {
      this.angerBySuspect.removeInt(p_219267_);
      this.suspects.remove(p_219267_);
      this.sortAndUpdateHighestAnger();
   }

   @Nullable
   private Entity getTopSuspect() {
      return this.suspects.stream().filter(this.filter).findFirst().orElse(null);
   }

   public int getActiveAnger(@Nullable Entity p_219287_) {
      return p_219287_ == null ? this.highestAnger : this.angerBySuspect.getInt(p_219287_);
   }

   public Optional<LivingEntity> getActiveEntity() {
      return Optional.ofNullable(this.getTopSuspect()).filter(p_219293_ -> p_219293_ instanceof LivingEntity).map(p_219290_ -> (LivingEntity)p_219290_);
   }

   @VisibleForTesting
   protected record Sorter(AngerManagement angerManagement) implements Comparator<Entity> {
      public int compare(Entity p_219303_, Entity p_219304_) {
         if (p_219303_.equals(p_219304_)) {
            return 0;
         } else {
            int $$2 = this.angerManagement.angerBySuspect.getOrDefault(p_219303_, 0);
            int $$3 = this.angerManagement.angerBySuspect.getOrDefault(p_219304_, 0);
            this.angerManagement.highestAnger = Math.max(this.angerManagement.highestAnger, Math.max($$2, $$3));
            boolean $$4 = AngerLevel.byAnger($$2).isAngry();
            boolean $$5 = AngerLevel.byAnger($$3).isAngry();
            if ($$4 != $$5) {
               return $$4 ? -1 : 1;
            } else {
               boolean $$6 = p_219303_ instanceof Player;
               boolean $$7 = p_219304_ instanceof Player;
               if ($$6 != $$7) {
                  return $$6 ? -1 : 1;
               } else {
                  return Integer.compare($$3, $$2);
               }
            }
         }
      }
   }
}
