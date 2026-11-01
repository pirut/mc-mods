package net.minecraft.world.level.block.entity.trialspawner;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.extensions.IOwnedSpawner;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class TrialSpawner implements IOwnedSpawner {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int DETECT_PLAYER_SPAWN_BUFFER = 40;
   private static final int DEFAULT_TARGET_COOLDOWN_LENGTH = 36000;
   private static final int DEFAULT_PLAYER_SCAN_RANGE = 14;
   private static final int MAX_MOB_TRACKING_DISTANCE = 47;
   private static final int MAX_MOB_TRACKING_DISTANCE_SQR = Mth.square(47);
   private static final float SPAWNING_AMBIENT_SOUND_CHANCE = 0.02F;
   private final TrialSpawnerStateData data = new TrialSpawnerStateData();
   private TrialSpawner.FullConfig config;
   private final TrialSpawner.StateAccessor stateAccessor;
   private PlayerDetector playerDetector;
   private final PlayerDetector.EntitySelector entitySelector;
   private boolean overridePeacefulAndMobSpawnRule;
   private boolean isOminous;

   public TrialSpawner(
      TrialSpawner.FullConfig p_422371_, TrialSpawner.StateAccessor p_312198_, PlayerDetector p_312690_, PlayerDetector.EntitySelector p_323834_
   ) {
      this.config = p_422371_;
      this.stateAccessor = p_312198_;
      this.playerDetector = p_312690_;
      this.entitySelector = p_323834_;
   }

   public TrialSpawnerConfig activeConfig() {
      return this.isOminous ? this.config.ominous().value() : this.config.normal.value();
   }

   public TrialSpawnerConfig normalConfig() {
      return this.config.normal.value();
   }

   public TrialSpawnerConfig ominousConfig() {
      return this.config.ominous.value();
   }

   public void load(ValueInput p_421696_) {
      p_421696_.<TrialSpawnerStateData.Packed>read(TrialSpawnerStateData.Packed.MAP_CODEC).ifPresent(this.data::apply);
      this.config = p_421696_.<TrialSpawner.FullConfig>read(TrialSpawner.FullConfig.MAP_CODEC).orElse(TrialSpawner.FullConfig.DEFAULT);
   }

   public void store(ValueOutput p_422109_) {
      p_422109_.store(TrialSpawnerStateData.Packed.MAP_CODEC, this.data.pack());
      p_422109_.store(TrialSpawner.FullConfig.MAP_CODEC, this.config);
   }

   public void applyOminous(ServerLevel p_338315_, BlockPos p_338416_) {
      p_338315_.setBlock(p_338416_, p_338315_.getBlockState(p_338416_).setValue(TrialSpawnerBlock.OMINOUS, true), 3);
      p_338315_.levelEvent(3020, p_338416_, 1);
      this.isOminous = true;
      this.data.resetAfterBecomingOminous(this, p_338315_);
   }

   public void removeOminous(ServerLevel p_338672_, BlockPos p_338318_) {
      p_338672_.setBlock(p_338318_, p_338672_.getBlockState(p_338318_).setValue(TrialSpawnerBlock.OMINOUS, false), 3);
      this.isOminous = false;
   }

   public boolean isOminous() {
      return this.isOminous;
   }

   public int getTargetCooldownLength() {
      return this.config.targetCooldownLength;
   }

   public int getRequiredPlayerRange() {
      return this.config.requiredPlayerRange;
   }

   public TrialSpawnerState getState() {
      return this.stateAccessor.getState();
   }

   public TrialSpawnerStateData getStateData() {
      return this.data;
   }

   public void setState(Level p_312688_, TrialSpawnerState p_312718_) {
      this.stateAccessor.setState(p_312688_, p_312718_);
   }

   public void markUpdated() {
      this.stateAccessor.markUpdated();
   }

   public PlayerDetector getPlayerDetector() {
      return this.playerDetector;
   }

   public PlayerDetector.EntitySelector getEntitySelector() {
      return this.entitySelector;
   }

   public boolean canSpawnInLevel(ServerLevel p_376465_) {
      if (!p_376465_.getGameRules().get(GameRules.SPAWNER_BLOCKS_WORK)) {
         return false;
      } else if (this.overridePeacefulAndMobSpawnRule) {
         return true;
      } else {
         return p_376465_.getDifficulty() == Difficulty.PEACEFUL ? false : p_376465_.getGameRules().get(GameRules.SPAWN_MOBS);
      }
   }

   public Optional<UUID> spawnMob(ServerLevel p_312582_, BlockPos p_312518_) {
      RandomSource randomsource = p_312582_.getRandom();
      SpawnData spawndata = this.data.getOrCreateNextSpawnData(this, p_312582_.getRandom());

      Optional optional1;
      try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(() -> "spawner@" + p_312518_, LOGGER)) {
         ValueInput valueinput = TagValueInput.create(problemreporter$scopedcollector, p_312582_.registryAccess(), spawndata.entityToSpawn());
         Optional<EntityType<?>> optional = EntityType.by(valueinput);
         if (optional.isEmpty()) {
            return Optional.empty();
         }

         Vec3 vec3 = valueinput.<Vec3>read("Pos", Vec3.CODEC)
            .orElseGet(
               () -> {
                  TrialSpawnerConfig trialspawnerconfig = this.activeConfig();
                  return new Vec3(
                     p_312518_.getX() + (randomsource.nextDouble() - randomsource.nextDouble()) * trialspawnerconfig.spawnRange() + 0.5,
                     p_312518_.getY() + randomsource.nextInt(3) - 1,
                     p_312518_.getZ() + (randomsource.nextDouble() - randomsource.nextDouble()) * trialspawnerconfig.spawnRange() + 0.5
                  );
               }
            );
         if (!p_312582_.noCollision(optional.get().getSpawnAABB(vec3.x, vec3.y, vec3.z))) {
            return Optional.empty();
         }

         if (!inLineOfSight(p_312582_, p_312518_.getCenter(), vec3)) {
            return Optional.empty();
         }

         BlockPos blockpos = BlockPos.containing(vec3);
         if (!SpawnPlacements.checkSpawnRules(optional.get(), p_312582_, EntitySpawnReason.TRIAL_SPAWNER, blockpos, p_312582_.getRandom())) {
            return Optional.empty();
         }

         if (spawndata.getCustomSpawnRules().isPresent()) {
            SpawnData.CustomSpawnRules spawndata$customspawnrules = spawndata.getCustomSpawnRules().get();
            if (!spawndata$customspawnrules.isValidPosition(blockpos, p_312582_)) {
               return Optional.empty();
            }
         }

         Entity entity = EntityType.loadEntityRecursive(valueinput, p_312582_, EntitySpawnReason.TRIAL_SPAWNER, p_404588_ -> {
            p_404588_.snapTo(vec3.x, vec3.y, vec3.z, randomsource.nextFloat() * 360.0F, 0.0F);
            return p_404588_;
         });
         if (entity == null) {
            return Optional.empty();
         }

         if (entity instanceof Mob mob) {
            if (!mob.checkSpawnObstruction(p_312582_)) {
               return Optional.empty();
            }

            boolean flag = spawndata.getEntityToSpawn().size() == 1 && spawndata.getEntityToSpawn().getString("id").isPresent();
            EventHooks.finalizeMobSpawnSpawner(
               mob, p_312582_, p_312582_.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.TRIAL_SPAWNER, null, this, flag
            );
            mob.setPersistenceRequired();
            spawndata.getEquipment().ifPresent(mob::equip);
         }

         if (!p_312582_.tryAddFreshEntityWithPassengers(entity)) {
            return Optional.empty();
         }

         TrialSpawner.FlameParticle trialspawner$flameparticle = this.isOminous ? TrialSpawner.FlameParticle.OMINOUS : TrialSpawner.FlameParticle.NORMAL;
         p_312582_.levelEvent(3011, p_312518_, trialspawner$flameparticle.encode());
         p_312582_.levelEvent(3012, blockpos, trialspawner$flameparticle.encode());
         p_312582_.gameEvent(entity, GameEvent.ENTITY_PLACE, blockpos);
         optional1 = Optional.of(entity.getUUID());
      }

      return optional1;
   }

   public void ejectReward(ServerLevel p_312892_, BlockPos p_312668_, ResourceKey<LootTable> p_335942_) {
      LootTable loottable = p_312892_.getServer().reloadableRegistries().getLootTable(p_335942_);
      LootParams lootparams = new LootParams.Builder(p_312892_).create(LootContextParamSets.EMPTY);
      ObjectArrayList<ItemStack> objectarraylist = loottable.getRandomItems(lootparams);
      if (!objectarraylist.isEmpty()) {
         ObjectListIterator var7 = objectarraylist.iterator();

         while (var7.hasNext()) {
            ItemStack itemstack = (ItemStack)var7.next();
            DefaultDispenseItemBehavior.spawnItem(p_312892_, itemstack, 2, Direction.UP, Vec3.atBottomCenterOf(p_312668_).relative(Direction.UP, 1.2));
         }

         p_312892_.levelEvent(3014, p_312668_, 0);
      }
   }

   public void tickClient(Level p_312771_, BlockPos p_312484_, boolean p_338382_) {
      TrialSpawnerState trialspawnerstate = this.getState();
      trialspawnerstate.emitParticles(p_312771_, p_312484_, p_338382_);
      if (trialspawnerstate.hasSpinningMob()) {
         double d0 = Math.max(0L, this.data.nextMobSpawnsAt - p_312771_.getGameTime());
         this.data.oSpin = this.data.spin;
         this.data.spin = (this.data.spin + trialspawnerstate.spinningMobSpeed() / (d0 + 200.0)) % 360.0;
      }

      if (trialspawnerstate.isCapableOfSpawning()) {
         RandomSource randomsource = p_312771_.getRandom();
         if (randomsource.nextFloat() <= 0.02F) {
            SoundEvent soundevent = p_338382_ ? SoundEvents.TRIAL_SPAWNER_AMBIENT_OMINOUS : SoundEvents.TRIAL_SPAWNER_AMBIENT;
            p_312771_.playLocalSound(
               p_312484_, soundevent, SoundSource.BLOCKS, randomsource.nextFloat() * 0.25F + 0.75F, randomsource.nextFloat() + 0.5F, false
            );
         }
      }
   }

   public void tickServer(ServerLevel p_312132_, BlockPos p_312062_, boolean p_338207_) {
      this.isOminous = p_338207_;
      TrialSpawnerState trialspawnerstate = this.getState();
      if (this.data.currentMobs.removeIf(p_312870_ -> shouldMobBeUntracked(p_312132_, p_312062_, p_312870_))) {
         this.data.nextMobSpawnsAt = p_312132_.getGameTime() + this.activeConfig().ticksBetweenSpawn();
      }

      TrialSpawnerState trialspawnerstate1 = trialspawnerstate.tickAndGetNext(p_312062_, this, p_312132_);
      if (trialspawnerstate1 != trialspawnerstate) {
         this.setState(p_312132_, trialspawnerstate1);
      }
   }

   private static boolean shouldMobBeUntracked(ServerLevel p_312908_, BlockPos p_312807_, UUID p_311952_) {
      Entity entity = p_312908_.getEntity(p_311952_);
      return entity == null
         || !entity.isAlive()
         || !entity.level().dimension().equals(p_312908_.dimension())
         || entity.blockPosition().distSqr(p_312807_) > MAX_MOB_TRACKING_DISTANCE_SQR;
   }

   private static boolean inLineOfSight(Level p_312623_, Vec3 p_312390_, Vec3 p_312652_) {
      BlockHitResult blockhitresult = p_312623_.clip(
         new ClipContext(p_312652_, p_312390_, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty())
      );
      return blockhitresult.getBlockPos().equals(BlockPos.containing(p_312390_)) || blockhitresult.getType() == HitResult.Type.MISS;
   }

   public static void addSpawnParticles(Level p_312303_, BlockPos p_312392_, RandomSource p_312825_, SimpleParticleType p_338426_) {
      for (int i = 0; i < 20; i++) {
         double d0 = p_312392_.getX() + 0.5 + (p_312825_.nextDouble() - 0.5) * 2.0;
         double d1 = p_312392_.getY() + 0.5 + (p_312825_.nextDouble() - 0.5) * 2.0;
         double d2 = p_312392_.getZ() + 0.5 + (p_312825_.nextDouble() - 0.5) * 2.0;
         p_312303_.addParticle(ParticleTypes.SMOKE, d0, d1, d2, 0.0, 0.0, 0.0);
         p_312303_.addParticle(p_338426_, d0, d1, d2, 0.0, 0.0, 0.0);
      }
   }

   public static void addBecomeOminousParticles(Level p_338657_, BlockPos p_338406_, RandomSource p_338527_) {
      for (int i = 0; i < 20; i++) {
         double d0 = p_338406_.getX() + 0.5 + (p_338527_.nextDouble() - 0.5) * 2.0;
         double d1 = p_338406_.getY() + 0.5 + (p_338527_.nextDouble() - 0.5) * 2.0;
         double d2 = p_338406_.getZ() + 0.5 + (p_338527_.nextDouble() - 0.5) * 2.0;
         double d3 = p_338527_.nextGaussian() * 0.02;
         double d4 = p_338527_.nextGaussian() * 0.02;
         double d5 = p_338527_.nextGaussian() * 0.02;
         p_338657_.addParticle(ParticleTypes.TRIAL_OMEN, d0, d1, d2, d3, d4, d5);
         p_338657_.addParticle(ParticleTypes.SOUL_FIRE_FLAME, d0, d1, d2, d3, d4, d5);
      }
   }

   public static void addDetectPlayerParticles(Level p_312225_, BlockPos p_311759_, RandomSource p_312553_, int p_312188_, ParticleOptions p_338522_) {
      for (int i = 0; i < 30 + Math.min(p_312188_, 10) * 5; i++) {
         double d0 = (2.0F * p_312553_.nextFloat() - 1.0F) * 0.65;
         double d1 = (2.0F * p_312553_.nextFloat() - 1.0F) * 0.65;
         double d2 = p_311759_.getX() + 0.5 + d0;
         double d3 = p_311759_.getY() + 0.1 + p_312553_.nextFloat() * 0.8;
         double d4 = p_311759_.getZ() + 0.5 + d1;
         p_312225_.addParticle(p_338522_, d2, d3, d4, 0.0, 0.0, 0.0);
      }
   }

   public static void addEjectItemParticles(Level p_312009_, BlockPos p_312583_, RandomSource p_312715_) {
      for (int i = 0; i < 20; i++) {
         double d0 = p_312583_.getX() + 0.4 + p_312715_.nextDouble() * 0.2;
         double d1 = p_312583_.getY() + 0.4 + p_312715_.nextDouble() * 0.2;
         double d2 = p_312583_.getZ() + 0.4 + p_312715_.nextDouble() * 0.2;
         double d3 = p_312715_.nextGaussian() * 0.02;
         double d4 = p_312715_.nextGaussian() * 0.02;
         double d5 = p_312715_.nextGaussian() * 0.02;
         p_312009_.addParticle(ParticleTypes.SMALL_FLAME, d0, d1, d2, d3, d4, d5 * 0.25);
         p_312009_.addParticle(ParticleTypes.SMOKE, d0, d1, d2, d3, d4, d5);
      }
   }

   public void overrideEntityToSpawn(EntityType<?> p_387174_, Level p_386596_) {
      this.data.reset();
      this.config = this.config.overrideEntity(p_387174_);
      this.setState(p_386596_, TrialSpawnerState.INACTIVE);
   }

   @Deprecated(
      forRemoval = true
   )
   @VisibleForTesting
   public void setPlayerDetector(PlayerDetector p_312510_) {
      this.playerDetector = p_312510_;
   }

   @Deprecated(
      forRemoval = true
   )
   @VisibleForTesting
   public void overridePeacefulAndMobSpawnRule() {
      this.overridePeacefulAndMobSpawnRule = true;
   }

   @Nullable
   @Override
   public Either<BlockEntity, Entity> getOwner() {
      return this.stateAccessor instanceof TrialSpawnerBlockEntity be ? Either.left(be) : null;
   }

   public static enum FlameParticle {
      NORMAL(ParticleTypes.FLAME),
      OMINOUS(ParticleTypes.SOUL_FIRE_FLAME);

      public final SimpleParticleType particleType;

      private FlameParticle(SimpleParticleType p_338374_) {
         this.particleType = p_338374_;
      }

      public static TrialSpawner.FlameParticle decode(int p_338695_) {
         TrialSpawner.FlameParticle[] atrialspawner$flameparticle = values();
         return p_338695_ <= atrialspawner$flameparticle.length && p_338695_ >= 0 ? atrialspawner$flameparticle[p_338695_] : NORMAL;
      }

      public int encode() {
         return this.ordinal();
      }
   }

   public record FullConfig(Holder<TrialSpawnerConfig> normal, Holder<TrialSpawnerConfig> ominous, int targetCooldownLength, int requiredPlayerRange) {
      public static final MapCodec<TrialSpawner.FullConfig> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_422015_ -> p_422015_.group(
               TrialSpawnerConfig.CODEC.optionalFieldOf("normal_config", Holder.direct(TrialSpawnerConfig.DEFAULT)).forGetter(TrialSpawner.FullConfig::normal),
               TrialSpawnerConfig.CODEC
                  .optionalFieldOf("ominous_config", Holder.direct(TrialSpawnerConfig.DEFAULT))
                  .forGetter(TrialSpawner.FullConfig::ominous),
               ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("target_cooldown_length", 36000).forGetter(TrialSpawner.FullConfig::targetCooldownLength),
               Codec.intRange(1, 128).optionalFieldOf("required_player_range", 14).forGetter(TrialSpawner.FullConfig::requiredPlayerRange)
            )
            .apply(p_422015_, TrialSpawner.FullConfig::new)
      );
      public static final TrialSpawner.FullConfig DEFAULT = new TrialSpawner.FullConfig(
         Holder.direct(TrialSpawnerConfig.DEFAULT), Holder.direct(TrialSpawnerConfig.DEFAULT), 36000, 14
      );

      public TrialSpawner.FullConfig overrideEntity(EntityType<?> p_422573_) {
         return new TrialSpawner.FullConfig(
            Holder.direct(this.normal.value().withSpawning(p_422573_)),
            Holder.direct(this.ominous.value().withSpawning(p_422573_)),
            this.targetCooldownLength,
            this.requiredPlayerRange
         );
      }
   }

   public interface StateAccessor {
      void setState(Level var1, TrialSpawnerState var2);

      TrialSpawnerState getState();

      void markUpdated();
   }
}
