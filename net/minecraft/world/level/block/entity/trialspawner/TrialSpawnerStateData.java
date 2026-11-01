package net.minecraft.world.level.block.entity.trialspawner;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.jspecify.annotations.Nullable;

public class TrialSpawnerStateData {
   private static final String TAG_SPAWN_DATA = "spawn_data";
   private static final String TAG_NEXT_MOB_SPAWNS_AT = "next_mob_spawns_at";
   private static final int DELAY_BETWEEN_PLAYER_SCANS = 20;
   private static final int TRIAL_OMEN_PER_BAD_OMEN_LEVEL = 18000;
   final Set<UUID> detectedPlayers = new HashSet<>();
   final Set<UUID> currentMobs = new HashSet<>();
   long cooldownEndsAt;
   long nextMobSpawnsAt;
   int totalMobsSpawned;
   Optional<SpawnData> nextSpawnData = Optional.empty();
   Optional<ResourceKey<LootTable>> ejectingLootTable = Optional.empty();
   @Nullable
   private Entity displayEntity;
   @Nullable
   private WeightedList<ItemStack> dispensing;
   double spin;
   double oSpin;

   public TrialSpawnerStateData.Packed pack() {
      return new TrialSpawnerStateData.Packed(
         Set.copyOf(this.detectedPlayers),
         Set.copyOf(this.currentMobs),
         this.cooldownEndsAt,
         this.nextMobSpawnsAt,
         this.totalMobsSpawned,
         this.nextSpawnData,
         this.ejectingLootTable
      );
   }

   public void apply(TrialSpawnerStateData.Packed p_421478_) {
      this.detectedPlayers.clear();
      this.detectedPlayers.addAll(p_421478_.detectedPlayers);
      this.currentMobs.clear();
      this.currentMobs.addAll(p_421478_.currentMobs);
      this.cooldownEndsAt = p_421478_.cooldownEndsAt;
      this.nextMobSpawnsAt = p_421478_.nextMobSpawnsAt;
      this.totalMobsSpawned = p_421478_.totalMobsSpawned;
      this.nextSpawnData = p_421478_.nextSpawnData;
      this.ejectingLootTable = p_421478_.ejectingLootTable;
   }

   public void reset() {
      this.currentMobs.clear();
      this.nextSpawnData = Optional.empty();
      this.resetStatistics();
   }

   public void resetStatistics() {
      this.detectedPlayers.clear();
      this.totalMobsSpawned = 0;
      this.nextMobSpawnsAt = 0L;
      this.cooldownEndsAt = 0L;
   }

   public boolean hasMobToSpawn(TrialSpawner p_422351_, RandomSource p_421482_) {
      boolean flag = this.getOrCreateNextSpawnData(p_422351_, p_421482_).getEntityToSpawn().getString("id").isPresent();
      return flag || !p_422351_.activeConfig().spawnPotentialsDefinition().isEmpty();
   }

   public boolean hasFinishedSpawningAllMobs(TrialSpawnerConfig p_422354_, int p_422183_) {
      return this.totalMobsSpawned >= p_422354_.calculateTargetTotalMobs(p_422183_);
   }

   public boolean haveAllCurrentMobsDied() {
      return this.currentMobs.isEmpty();
   }

   public boolean isReadyToSpawnNextMob(ServerLevel p_422162_, TrialSpawnerConfig p_422263_, int p_422207_) {
      return p_422162_.getGameTime() >= this.nextMobSpawnsAt && this.currentMobs.size() < p_422263_.calculateTargetSimultaneousMobs(p_422207_);
   }

   public int countAdditionalPlayers(BlockPos p_422699_) {
      if (this.detectedPlayers.isEmpty()) {
         Util.logAndPauseIfInIde("Trial Spawner at " + p_422699_ + " has no detected players");
      }

      return Math.max(0, this.detectedPlayers.size() - 1);
   }

   public void tryDetectPlayers(ServerLevel p_422381_, BlockPos p_421956_, TrialSpawner p_421925_) {
      boolean flag = (p_421956_.asLong() + p_422381_.getGameTime()) % 20L != 0L;
      if (!flag && (!p_421925_.getState().equals(TrialSpawnerState.COOLDOWN) || !p_421925_.isOminous())) {
         List<UUID> list = p_421925_.getPlayerDetector().detect(p_422381_, p_421925_.getEntitySelector(), p_421956_, p_421925_.getRequiredPlayerRange(), true);
         boolean flag1;
         if (!p_421925_.isOminous() && !list.isEmpty()) {
            Optional<Pair<Player, Holder<MobEffect>>> optional = findPlayerWithOminousEffect(p_422381_, list);
            optional.ifPresent(p_427219_ -> {
               Player player = (Player)p_427219_.getFirst();
               if (p_427219_.getSecond() == MobEffects.BAD_OMEN) {
                  transformBadOmenIntoTrialOmen(player);
               }

               p_422381_.levelEvent(3020, BlockPos.containing(player.getEyePosition()), 0);
               p_421925_.applyOminous(p_422381_, p_421956_);
            });
            flag1 = optional.isPresent();
         } else {
            flag1 = false;
         }

         if (!p_421925_.getState().equals(TrialSpawnerState.COOLDOWN) || flag1) {
            boolean flag2 = p_421925_.getStateData().detectedPlayers.isEmpty();
            List<UUID> list1 = flag2
               ? list
               : p_421925_.getPlayerDetector().detect(p_422381_, p_421925_.getEntitySelector(), p_421956_, p_421925_.getRequiredPlayerRange(), false);
            if (this.detectedPlayers.addAll(list1)) {
               this.nextMobSpawnsAt = Math.max(p_422381_.getGameTime() + 40L, this.nextMobSpawnsAt);
               if (!flag1) {
                  int i = p_421925_.isOminous() ? 3019 : 3013;
                  p_422381_.levelEvent(i, p_421956_, this.detectedPlayers.size());
               }
            }
         }
      }
   }

   private static Optional<Pair<Player, Holder<MobEffect>>> findPlayerWithOminousEffect(ServerLevel p_422672_, List<UUID> p_422201_) {
      Player player = null;

      for (UUID uuid : p_422201_) {
         Player player1 = p_422672_.getPlayerByUUID(uuid);
         if (player1 != null) {
            Holder<MobEffect> holder = MobEffects.TRIAL_OMEN;
            if (player1.hasEffect(holder)) {
               return Optional.of(Pair.of(player1, holder));
            }

            if (player1.hasEffect(MobEffects.BAD_OMEN)) {
               player = player1;
            }
         }
      }

      return Optional.ofNullable(player).map(p_421755_ -> Pair.of(p_421755_, MobEffects.BAD_OMEN));
   }

   public void resetAfterBecomingOminous(TrialSpawner p_421806_, ServerLevel p_422006_) {
      this.currentMobs.stream().map(p_422006_::getEntity).forEach(p_421890_ -> {
         if (p_421890_ != null) {
            p_422006_.levelEvent(3012, p_421890_.blockPosition(), TrialSpawner.FlameParticle.NORMAL.encode());
            if (p_421890_ instanceof Mob mob) {
               mob.dropPreservedEquipment(p_422006_);
            }

            p_421890_.remove(Entity.RemovalReason.DISCARDED);
         }
      });
      if (!p_421806_.ominousConfig().spawnPotentialsDefinition().isEmpty()) {
         this.nextSpawnData = Optional.empty();
      }

      this.totalMobsSpawned = 0;
      this.currentMobs.clear();
      this.nextMobSpawnsAt = p_422006_.getGameTime() + p_421806_.ominousConfig().ticksBetweenSpawn();
      p_421806_.markUpdated();
      this.cooldownEndsAt = p_422006_.getGameTime() + p_421806_.ominousConfig().ticksBetweenItemSpawners();
   }

   private static void transformBadOmenIntoTrialOmen(Player p_422355_) {
      MobEffectInstance mobeffectinstance = p_422355_.getEffect(MobEffects.BAD_OMEN);
      if (mobeffectinstance != null) {
         int i = mobeffectinstance.getAmplifier() + 1;
         int j = 18000 * i;
         p_422355_.removeEffect(MobEffects.BAD_OMEN);
         p_422355_.addEffect(new MobEffectInstance(MobEffects.TRIAL_OMEN, j, 0));
      }
   }

   public boolean isReadyToOpenShutter(ServerLevel p_421776_, float p_422030_, int p_422651_) {
      long i = this.cooldownEndsAt - p_422651_;
      return (float)p_421776_.getGameTime() >= (float)i + p_422030_;
   }

   public boolean isReadyToEjectItems(ServerLevel p_422356_, float p_422031_, int p_422141_) {
      long i = this.cooldownEndsAt - p_422141_;
      return (float)(p_422356_.getGameTime() - i) % p_422031_ == 0.0F;
   }

   public boolean isCooldownFinished(ServerLevel p_421790_) {
      return p_421790_.getGameTime() >= this.cooldownEndsAt;
   }

   protected SpawnData getOrCreateNextSpawnData(TrialSpawner p_422364_, RandomSource p_422656_) {
      if (this.nextSpawnData.isPresent()) {
         return this.nextSpawnData.get();
      } else {
         WeightedList<SpawnData> weightedlist = p_422364_.activeConfig().spawnPotentialsDefinition();
         Optional<SpawnData> optional = weightedlist.isEmpty() ? this.nextSpawnData : weightedlist.getRandom(p_422656_);
         this.nextSpawnData = Optional.of(optional.orElseGet(SpawnData::new));
         p_422364_.markUpdated();
         return this.nextSpawnData.get();
      }
   }

   @Nullable
   public Entity getOrCreateDisplayEntity(TrialSpawner p_422419_, Level p_421870_, TrialSpawnerState p_422215_) {
      if (!p_422215_.hasSpinningMob()) {
         return null;
      } else {
         if (this.displayEntity == null) {
            CompoundTag compoundtag = this.getOrCreateNextSpawnData(p_422419_, p_421870_.getRandom()).getEntityToSpawn();
            if (compoundtag.getString("id").isPresent()) {
               this.displayEntity = EntityType.loadEntityRecursive(compoundtag, p_421870_, EntitySpawnReason.TRIAL_SPAWNER, EntityProcessor.NOP);
            }
         }

         return this.displayEntity;
      }
   }

   public CompoundTag getUpdateTag(TrialSpawnerState p_422094_) {
      CompoundTag compoundtag = new CompoundTag();
      if (p_422094_ == TrialSpawnerState.ACTIVE) {
         compoundtag.putLong("next_mob_spawns_at", this.nextMobSpawnsAt);
      }

      this.nextSpawnData.ifPresent(p_422694_ -> compoundtag.store("spawn_data", SpawnData.CODEC, p_422694_));
      return compoundtag;
   }

   public double getSpin() {
      return this.spin;
   }

   public double getOSpin() {
      return this.oSpin;
   }

   WeightedList<ItemStack> getDispensingItems(ServerLevel p_421873_, TrialSpawnerConfig p_421889_, BlockPos p_422242_) {
      if (this.dispensing != null) {
         return this.dispensing;
      } else {
         LootTable loottable = p_421873_.getServer().reloadableRegistries().getLootTable(p_421889_.itemsToDropWhenOminous());
         LootParams lootparams = new LootParams.Builder(p_421873_).create(LootContextParamSets.EMPTY);
         long i = lowResolutionPosition(p_421873_, p_422242_);
         ObjectArrayList<ItemStack> objectarraylist = loottable.getRandomItems(lootparams, i);
         if (objectarraylist.isEmpty()) {
            return WeightedList.of();
         } else {
            WeightedList.Builder<ItemStack> builder = WeightedList.builder();
            ObjectListIterator var10 = objectarraylist.iterator();

            while (var10.hasNext()) {
               ItemStack itemstack = (ItemStack)var10.next();
               builder.add(itemstack.copyWithCount(1), itemstack.getCount());
            }

            this.dispensing = builder.build();
            return this.dispensing;
         }
      }
   }

   private static long lowResolutionPosition(ServerLevel p_422056_, BlockPos p_421660_) {
      BlockPos blockpos = new BlockPos(Mth.floor(p_421660_.getX() / 30.0F), Mth.floor(p_421660_.getY() / 20.0F), Mth.floor(p_421660_.getZ() / 30.0F));
      return p_422056_.getSeed() + blockpos.asLong();
   }

   public record Packed(
      Set<UUID> detectedPlayers,
      Set<UUID> currentMobs,
      long cooldownEndsAt,
      long nextMobSpawnsAt,
      int totalMobsSpawned,
      Optional<SpawnData> nextSpawnData,
      Optional<ResourceKey<LootTable>> ejectingLootTable
   ) {
      public static final MapCodec<TrialSpawnerStateData.Packed> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_421723_ -> p_421723_.group(
               UUIDUtil.CODEC_SET.lenientOptionalFieldOf("registered_players", Set.of()).forGetter(TrialSpawnerStateData.Packed::detectedPlayers),
               UUIDUtil.CODEC_SET.lenientOptionalFieldOf("current_mobs", Set.of()).forGetter(TrialSpawnerStateData.Packed::currentMobs),
               Codec.LONG.lenientOptionalFieldOf("cooldown_ends_at", 0L).forGetter(TrialSpawnerStateData.Packed::cooldownEndsAt),
               Codec.LONG.lenientOptionalFieldOf("next_mob_spawns_at", 0L).forGetter(TrialSpawnerStateData.Packed::nextMobSpawnsAt),
               Codec.intRange(0, Integer.MAX_VALUE).lenientOptionalFieldOf("total_mobs_spawned", 0).forGetter(TrialSpawnerStateData.Packed::totalMobsSpawned),
               SpawnData.CODEC.lenientOptionalFieldOf("spawn_data").forGetter(TrialSpawnerStateData.Packed::nextSpawnData),
               LootTable.KEY_CODEC.lenientOptionalFieldOf("ejecting_loot_table").forGetter(TrialSpawnerStateData.Packed::ejectingLootTable)
            )
            .apply(p_421723_, TrialSpawnerStateData.Packed::new)
      );
   }
}
