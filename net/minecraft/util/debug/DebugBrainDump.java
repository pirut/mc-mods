package net.minecraft.util.debug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import org.jspecify.annotations.Nullable;

public record DebugBrainDump(
   String name,
   String profession,
   int xp,
   float health,
   float maxHealth,
   String inventory,
   boolean wantsGolem,
   int angerLevel,
   List<String> activities,
   List<String> behaviors,
   List<String> memories,
   List<String> gossips,
   Set<BlockPos> pois,
   Set<BlockPos> potentialPois
) {
   public static final StreamCodec<FriendlyByteBuf, DebugBrainDump> STREAM_CODEC = StreamCodec.of(
      (p_449610_, p_449173_) -> p_449173_.write(p_449610_), DebugBrainDump::new
   );

   public DebugBrainDump(FriendlyByteBuf p_449299_) {
      this(
         p_449299_.readUtf(),
         p_449299_.readUtf(),
         p_449299_.readInt(),
         p_449299_.readFloat(),
         p_449299_.readFloat(),
         p_449299_.readUtf(),
         p_449299_.readBoolean(),
         p_449299_.readInt(),
         p_449299_.readList(FriendlyByteBuf::readUtf),
         p_449299_.readList(FriendlyByteBuf::readUtf),
         p_449299_.readList(FriendlyByteBuf::readUtf),
         p_449299_.readList(FriendlyByteBuf::readUtf),
         p_449299_.readCollection(HashSet::new, BlockPos.STREAM_CODEC),
         p_449299_.readCollection(HashSet::new, BlockPos.STREAM_CODEC)
      );
   }

   public void write(FriendlyByteBuf p_449881_) {
      p_449881_.writeUtf(this.name);
      p_449881_.writeUtf(this.profession);
      p_449881_.writeInt(this.xp);
      p_449881_.writeFloat(this.health);
      p_449881_.writeFloat(this.maxHealth);
      p_449881_.writeUtf(this.inventory);
      p_449881_.writeBoolean(this.wantsGolem);
      p_449881_.writeInt(this.angerLevel);
      p_449881_.writeCollection(this.activities, FriendlyByteBuf::writeUtf);
      p_449881_.writeCollection(this.behaviors, FriendlyByteBuf::writeUtf);
      p_449881_.writeCollection(this.memories, FriendlyByteBuf::writeUtf);
      p_449881_.writeCollection(this.gossips, FriendlyByteBuf::writeUtf);
      p_449881_.writeCollection(this.pois, BlockPos.STREAM_CODEC);
      p_449881_.writeCollection(this.potentialPois, BlockPos.STREAM_CODEC);
   }

   public static DebugBrainDump takeBrainDump(ServerLevel p_449836_, LivingEntity p_449723_) {
      String s = DebugEntityNameGenerator.getEntityName(p_449723_);
      String s1;
      int i;
      if (p_449723_ instanceof Villager villager) {
         s1 = villager.getVillagerData().profession().getRegisteredName();
         i = villager.getVillagerXp();
      } else {
         s1 = "";
         i = 0;
      }

      float f1 = p_449723_.getHealth();
      float f = p_449723_.getMaxHealth();
      Brain<?> brain = p_449723_.getBrain();
      long j = p_449723_.level().getGameTime();
      String s2;
      if (p_449723_ instanceof InventoryCarrier inventorycarrier) {
         Container container = inventorycarrier.getInventory();
         s2 = container.isEmpty() ? "" : container.toString();
      } else {
         s2 = "";
      }

      boolean flag = p_449723_ instanceof Villager villager2 && villager2.wantsToSpawnGolem(j);
      int k = p_449723_ instanceof Warden warden ? warden.getClientAngerLevel() : -1;
      List<String> list3 = brain.getActiveActivities().stream().map(Activity::getName).toList();
      List<String> list = brain.getRunningBehaviors().stream().map(BehaviorControl::debugString).toList();
      List<String> list1 = getMemoryDescriptions(p_449836_, p_449723_, j).map(p_449405_ -> StringUtil.truncateStringIfNecessary(p_449405_, 255, true)).toList();
      Set<BlockPos> set = getKnownBlockPositions(brain, MemoryModuleType.JOB_SITE, MemoryModuleType.HOME, MemoryModuleType.MEETING_POINT);
      Set<BlockPos> set1 = getKnownBlockPositions(brain, MemoryModuleType.POTENTIAL_JOB_SITE);
      List<String> list2 = p_449723_ instanceof Villager villager1 ? getVillagerGossips(villager1) : List.of();
      return new DebugBrainDump(s, s1, i, f1, f, s2, flag, k, list3, list, list1, list2, set, set1);
   }

   @SafeVarargs
   private static Set<BlockPos> getKnownBlockPositions(Brain<?> p_449931_, MemoryModuleType<GlobalPos>... p_449049_) {
      return Stream.of(p_449049_)
         .filter(p_449931_::hasMemoryValue)
         .map(p_449931_::getMemory)
         .flatMap(Optional::stream)
         .map(GlobalPos::pos)
         .collect(Collectors.toSet());
   }

   private static List<String> getVillagerGossips(Villager p_480709_) {
      List<String> list = new ArrayList<>();
      p_480709_.getGossips().getGossipEntries().forEach((p_458843_, p_458844_) -> {
         String s = DebugEntityNameGenerator.getEntityName(p_458843_);
         p_458844_.forEach((p_458847_, p_458848_) -> list.add(s + ": " + p_458847_ + ": " + p_458848_));
      });
      return list;
   }

   private static Stream<String> getMemoryDescriptions(ServerLevel p_449124_, LivingEntity p_449439_, long p_449203_) {
      return p_449439_.getBrain().getMemories().entrySet().stream().map(p_449300_ -> {
         MemoryModuleType<?> memorymoduletype = p_449300_.getKey();
         Optional<? extends ExpirableValue<?>> optional = p_449300_.getValue();
         return getMemoryDescription(p_449124_, p_449203_, memorymoduletype, optional);
      }).sorted();
   }

   private static String getMemoryDescription(
      ServerLevel p_449039_, long p_449318_, MemoryModuleType<?> p_449251_, Optional<? extends ExpirableValue<?>> p_449065_
   ) {
      String s;
      if (p_449065_.isPresent()) {
         ExpirableValue<?> expirablevalue = (ExpirableValue<?>)p_449065_.get();
         Object object = expirablevalue.getValue();
         if (p_449251_ == MemoryModuleType.HEARD_BELL_TIME) {
            long i = p_449318_ - (Long)object;
            s = i + " ticks ago";
         } else if (expirablevalue.canExpire()) {
            s = getShortDescription(p_449039_, object) + " (ttl: " + expirablevalue.getTimeToLive() + ")";
         } else {
            s = getShortDescription(p_449039_, object);
         }
      } else {
         s = "-";
      }

      return BuiltInRegistries.MEMORY_MODULE_TYPE.getKey(p_449251_).getPath() + ": " + s;
   }

   private static String getShortDescription(ServerLevel p_449983_, @Nullable Object p_449809_) {
      return switch (p_449809_) {
         case null -> "-";
         case UUID uuid -> getShortDescription(p_449983_, p_449983_.getEntity(uuid));
         case Entity entity -> DebugEntityNameGenerator.getEntityName(entity);
         case WalkTarget walktarget -> getShortDescription(p_449983_, walktarget.getTarget());
         case EntityTracker entitytracker -> getShortDescription(p_449983_, entitytracker.getEntity());
         case GlobalPos globalpos -> getShortDescription(p_449983_, globalpos.pos());
         case BlockPosTracker blockpostracker -> getShortDescription(p_449983_, blockpostracker.currentBlockPosition());
         case DamageSource damagesource -> {
            Entity entity1 = damagesource.getEntity();
            yield entity1 == null ? p_449809_.toString() : getShortDescription(p_449983_, entity1);
         }
         case Collection<?> collection -> "["
            + (String)collection.stream().map(p_449928_ -> getShortDescription(p_449983_, p_449928_)).collect(Collectors.joining(", "))
            + "]";
         default -> p_449809_.toString();
      };
   }

   public boolean hasPoi(BlockPos p_449096_) {
      return this.pois.contains(p_449096_);
   }

   public boolean hasPotentialPoi(BlockPos p_449198_) {
      return this.potentialPois.contains(p_449198_);
   }
}
