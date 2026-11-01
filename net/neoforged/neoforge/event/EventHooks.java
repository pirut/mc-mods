package net.neoforged.neoforge.event;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IOwnedSpawner;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.InsertableLinkedOpenCustomHashSet;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.brewing.PotionBrewEvent;
import net.neoforged.neoforge.event.enchanting.EnchantmentLevelSetEvent;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSplitEvent;
import net.neoforged.neoforge.event.entity.living.SpawnClusterSizeEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.ArrowNockEvent;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.FluidTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PermissionsChangedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerFlyableFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.neoforged.neoforge.event.level.AlterGroundEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;
import net.neoforged.neoforge.event.level.ChunkTicketLevelUpdatedEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import net.neoforged.neoforge.event.level.GameRuleChangedEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.ModifyCustomSpawnersEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.level.block.CreateFluidSourceEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.resource.ReloadListenerSort;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public class EventHooks {
   private static final WeightedList<MobSpawnSettings.SpawnerData> NO_SPAWNS = WeightedList.of();

   public static boolean onMultiBlockPlace(@Nullable Entity entity, List<BlockSnapshot> blockSnapshots, Direction direction) {
      BlockSnapshot snap = blockSnapshots.get(0);
      BlockState placedAgainst = snap.getLevel().getBlockState(snap.getPos().relative(direction.getOpposite()));
      BlockEvent.EntityMultiPlaceEvent event = new BlockEvent.EntityMultiPlaceEvent(blockSnapshots, placedAgainst, entity);
      return ((BlockEvent.EntityMultiPlaceEvent)NeoForge.EVENT_BUS.post(event)).isCanceled();
   }

   public static boolean onBlockPlace(@Nullable Entity entity, BlockSnapshot blockSnapshot, Direction direction) {
      BlockState placedAgainst = blockSnapshot.getLevel().getBlockState(blockSnapshot.getPos().relative(direction.getOpposite()));
      BlockEvent.EntityPlaceEvent event = new BlockEvent.EntityPlaceEvent(blockSnapshot, placedAgainst, entity);
      return ((BlockEvent.EntityPlaceEvent)NeoForge.EVENT_BUS.post(event)).isCanceled();
   }

   public static BlockEvent.NeighborNotifyEvent onNeighborNotify(
      Level level, BlockPos pos, BlockState state, EnumSet<Direction> notifiedSides, boolean forceRedstoneUpdate
   ) {
      BlockEvent.NeighborNotifyEvent event = new BlockEvent.NeighborNotifyEvent(level, pos, state, notifiedSides, forceRedstoneUpdate);
      NeoForge.EVENT_BUS.post(event);
      return event;
   }

   public static boolean doPlayerHarvestCheck(Player player, BlockState state, BlockGetter level, BlockPos pos) {
      boolean vanillaValue = player.hasCorrectToolForDrops(state);
      PlayerEvent.HarvestCheck event = (PlayerEvent.HarvestCheck)NeoForge.EVENT_BUS.post(new PlayerEvent.HarvestCheck(player, state, level, pos, vanillaValue));
      return event.canHarvest();
   }

   public static float getBreakSpeed(Player player, BlockState state, float original, BlockPos pos) {
      PlayerEvent.BreakSpeed event = new PlayerEvent.BreakSpeed(player, state, original, pos);
      return ((PlayerEvent.BreakSpeed)NeoForge.EVENT_BUS.post(event)).isCanceled() ? -1.0F : event.getNewSpeed();
   }

   public static void onPlayerDestroyItem(Player player, ItemStack stack, @Nullable InteractionHand hand) {
      NeoForge.EVENT_BUS.post(new PlayerDestroyItemEvent(player, stack, hand));
   }

   @Internal
   public static boolean checkSpawnPlacements(
      EntityType<?> entityType, ServerLevelAccessor level, EntitySpawnReason spawnType, BlockPos pos, RandomSource random, boolean defaultResult
   ) {
      MobSpawnEvent.SpawnPlacementCheck event = new MobSpawnEvent.SpawnPlacementCheck(entityType, level, spawnType, pos, random, defaultResult);
      return ((MobSpawnEvent.SpawnPlacementCheck)NeoForge.EVENT_BUS.post(event)).getPlacementCheckResult();
   }

   public static boolean checkSpawnPosition(Mob mob, ServerLevelAccessor level, EntitySpawnReason spawnType) {
      MobSpawnEvent.PositionCheck event = new MobSpawnEvent.PositionCheck(mob, level, spawnType, null);
      NeoForge.EVENT_BUS.post(event);
      return event.getResult() != MobSpawnEvent.PositionCheck.Result.DEFAULT
         ? event.getResult() == MobSpawnEvent.PositionCheck.Result.SUCCEED
         : mob.checkSpawnRules(level, spawnType) && mob.checkSpawnObstruction(level);
   }

   public static boolean checkSpawnPositionSpawner(Mob mob, ServerLevelAccessor level, EntitySpawnReason spawnType, SpawnData spawnData, BaseSpawner spawner) {
      MobSpawnEvent.PositionCheck event = new MobSpawnEvent.PositionCheck(mob, level, spawnType, spawner);
      NeoForge.EVENT_BUS.post(event);
      return event.getResult() != MobSpawnEvent.PositionCheck.Result.DEFAULT
         ? event.getResult() == MobSpawnEvent.PositionCheck.Result.SUCCEED
         : (spawnData.getCustomSpawnRules().isPresent() || mob.checkSpawnRules(level, spawnType)) && mob.checkSpawnObstruction(level);
   }

   @Nullable
   public static SpawnGroupData finalizeMobSpawn(
      Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnType, @Nullable SpawnGroupData spawnData
   ) {
      FinalizeSpawnEvent event = new FinalizeSpawnEvent(mob, level, mob.getX(), mob.getY(), mob.getZ(), difficulty, spawnType, spawnData, null);
      NeoForge.EVENT_BUS.post(event);
      return !event.isCanceled() ? mob.finalizeSpawn(level, event.getDifficulty(), event.getSpawnType(), event.getSpawnData()) : null;
   }

   public static FinalizeSpawnEvent finalizeMobSpawnSpawner(
      Mob mob,
      ServerLevelAccessor level,
      DifficultyInstance difficulty,
      EntitySpawnReason spawnType,
      @Nullable SpawnGroupData spawnData,
      IOwnedSpawner spawner,
      boolean def
   ) {
      FinalizeSpawnEvent event = new FinalizeSpawnEvent(mob, level, mob.getX(), mob.getY(), mob.getZ(), difficulty, spawnType, spawnData, spawner.getOwner());
      NeoForge.EVENT_BUS.post(event);
      if (!event.isCanceled() && def) {
         mob.finalizeSpawn(level, event.getDifficulty(), event.getSpawnType(), event.getSpawnData());
      }

      return event;
   }

   public static PlayerSpawnPhantomsEvent firePlayerSpawnPhantoms(ServerPlayer player, ServerLevel level, BlockPos pos) {
      Difficulty difficulty = level.getCurrentDifficultyAt(pos).getDifficulty();
      PlayerSpawnPhantomsEvent event = new PlayerSpawnPhantomsEvent(player, 1 + level.random.nextInt(difficulty.getId() + 1));
      NeoForge.EVENT_BUS.post(event);
      return event;
   }

   public static boolean checkMobDespawn(Mob mob) {
      MobDespawnEvent event = new MobDespawnEvent(mob, (ServerLevel)mob.level());
      NeoForge.EVENT_BUS.post(event);
      switch (event.getResult()) {
         case ALLOW:
            mob.discard();
         case DEFAULT:
         default:
            break;
         case DENY:
            mob.setNoActionTime(0);
      }

      return event.getResult() != MobDespawnEvent.Result.DEFAULT;
   }

   public static int getItemBurnTime(ItemStack itemStack, int burnTime, @Nullable RecipeType<?> recipeType, FuelValues fuelValues) {
      FurnaceFuelBurnTimeEvent event = new FurnaceFuelBurnTimeEvent(itemStack, burnTime, recipeType, fuelValues);
      NeoForge.EVENT_BUS.post(event);
      return event.getBurnTime();
   }

   public static int getExperienceDrop(LivingEntity entity, @Nullable Player attackingPlayer, int originalExperience) {
      LivingExperienceDropEvent event = new LivingExperienceDropEvent(entity, attackingPlayer, originalExperience);
      return ((LivingExperienceDropEvent)NeoForge.EVENT_BUS.post(event)).isCanceled() ? 0 : event.getDroppedExperience();
   }

   public static int getMaxSpawnClusterSize(Mob entity) {
      SpawnClusterSizeEvent event = new SpawnClusterSizeEvent(entity);
      NeoForge.EVENT_BUS.post(event);
      return event.getSize();
   }

   public static Component getPlayerDisplayName(Player player, Component username) {
      PlayerEvent.NameFormat event = new PlayerEvent.NameFormat(player, username);
      NeoForge.EVENT_BUS.post(event);
      return event.getDisplayname();
   }

   public static Component getPlayerTabListDisplayName(Player player) {
      PlayerEvent.TabListNameFormat event = new PlayerEvent.TabListNameFormat(player);
      NeoForge.EVENT_BUS.post(event);
      return event.getDisplayName();
   }

   public static BlockState fireFluidPlaceBlockEvent(LevelAccessor level, BlockPos pos, BlockPos liquidPos, BlockState state) {
      BlockEvent.FluidPlaceBlockEvent event = new BlockEvent.FluidPlaceBlockEvent(level, pos, liquidPos, state);
      NeoForge.EVENT_BUS.post(event);
      return event.getNewState();
   }

   public static ItemTooltipEvent onItemTooltip(
      ItemStack itemStack, @Nullable Player entityPlayer, List<Component> list, TooltipFlag flags, Item.TooltipContext context
   ) {
      ItemTooltipEvent event = new ItemTooltipEvent(itemStack, entityPlayer, list, flags, context);
      NeoForge.EVENT_BUS.post(event);
      return event;
   }

   public static FluidTooltipEvent onFluidTooltip(
      FluidStack fluidStack, @Nullable Player entityPlayer, List<Component> list, TooltipFlag flags, Item.TooltipContext context
   ) {
      FluidTooltipEvent event = new FluidTooltipEvent(fluidStack, entityPlayer, list, flags, context);
      NeoForge.EVENT_BUS.post(event);
      return event;
   }

   public static boolean onEntityStruckByLightning(Entity entity, LightningBolt bolt) {
      return ((EntityStruckByLightningEvent)NeoForge.EVENT_BUS.post(new EntityStruckByLightningEvent(entity, bolt))).isCanceled();
   }

   public static int onItemUseStart(LivingEntity entity, ItemStack item, InteractionHand hand, int duration) {
      LivingEntityUseItemEvent.Start event = new LivingEntityUseItemEvent.Start(entity, item, hand, duration);
      return ((LivingEntityUseItemEvent.Start)NeoForge.EVENT_BUS.post(event)).isCanceled() ? -1 : event.getDuration();
   }

   public static int onItemUseTick(LivingEntity entity, ItemStack item, int duration) {
      LivingEntityUseItemEvent.Tick event = new LivingEntityUseItemEvent.Tick(entity, item, duration);
      return ((LivingEntityUseItemEvent.Tick)NeoForge.EVENT_BUS.post(event)).isCanceled() ? -1 : event.getDuration();
   }

   public static boolean onUseItemStop(LivingEntity entity, ItemStack item, int duration) {
      return ((LivingEntityUseItemEvent.Stop)NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Stop(entity, item, duration))).isCanceled();
   }

   public static ItemStack onItemUseFinish(LivingEntity entity, ItemStack item, int duration, ItemStack result) {
      LivingEntityUseItemEvent.Finish event = new LivingEntityUseItemEvent.Finish(entity, item, duration, result);
      NeoForge.EVENT_BUS.post(event);
      return event.getResultStack();
   }

   public static void onStartEntityTracking(Entity entity, Player player) {
      NeoForge.EVENT_BUS.post(new PlayerEvent.StartTracking(player, entity));
   }

   public static void onStopEntityTracking(Entity entity, Player player) {
      NeoForge.EVENT_BUS.post(new PlayerEvent.StopTracking(player, entity));
   }

   public static void firePlayerLoadingEvent(Player player, PlayerList playerList, String uuidString) {
      NeoForge.EVENT_BUS.post(new PlayerEvent.LoadFromFile(player, playerList.getPlayerIo().getPlayerDir(), uuidString));
   }

   public static void firePlayerSavingEvent(Player player, File playerDirectory, String uuidString) {
      NeoForge.EVENT_BUS.post(new PlayerEvent.SaveToFile(player, playerDirectory, uuidString));
   }

   @Nullable
   public static BlockState onToolUse(BlockState originalState, UseOnContext context, ItemAbility itemAbility, boolean simulate) {
      BlockEvent.BlockToolModificationEvent event = new BlockEvent.BlockToolModificationEvent(originalState, context, itemAbility, simulate);
      return ((BlockEvent.BlockToolModificationEvent)NeoForge.EVENT_BUS.post(event)).isCanceled() ? null : event.getFinalState();
   }

   public static BonemealEvent fireBonemealEvent(@Nullable Player player, Level level, BlockPos pos, BlockState state, ItemStack stack) {
      return (BonemealEvent)NeoForge.EVENT_BUS.post(new BonemealEvent(player, level, pos, state, stack));
   }

   public static PlayLevelSoundEvent.AtEntity onPlaySoundAtEntity(Entity entity, Holder<SoundEvent> name, SoundSource category, float volume, float pitch) {
      PlayLevelSoundEvent.AtEntity event = new PlayLevelSoundEvent.AtEntity(entity, name, category, volume, pitch);
      NeoForge.EVENT_BUS.post(event);
      return event;
   }

   public static PlayLevelSoundEvent.AtPosition onPlaySoundAtPosition(
      Level level, double x, double y, double z, Holder<SoundEvent> name, SoundSource category, float volume, float pitch
   ) {
      PlayLevelSoundEvent.AtPosition event = new PlayLevelSoundEvent.AtPosition(level, new Vec3(x, y, z), name, category, volume, pitch);
      NeoForge.EVENT_BUS.post(event);
      return event;
   }

   public static int onItemExpire(ItemEntity entity) {
      ItemExpireEvent event = new ItemExpireEvent(entity);
      NeoForge.EVENT_BUS.post(event);
      return event.getExtraLife();
   }

   public static ItemEntityPickupEvent.Pre fireItemPickupPre(ItemEntity itemEntity, Player player) {
      return (ItemEntityPickupEvent.Pre)NeoForge.EVENT_BUS.post(new ItemEntityPickupEvent.Pre(player, itemEntity));
   }

   public static void fireItemPickupPost(ItemEntity itemEntity, Player player, ItemStack copy) {
      NeoForge.EVENT_BUS.post(new ItemEntityPickupEvent.Post(player, itemEntity, copy));
   }

   public static boolean canMountEntity(Entity entityMounting, Entity entityBeingMounted, boolean isMounting) {
      boolean isCanceled = ((EntityMountEvent)NeoForge.EVENT_BUS
            .post(new EntityMountEvent(entityMounting, entityBeingMounted, entityMounting.level(), isMounting)))
         .isCanceled();
      if (isCanceled) {
         entityMounting.absSnapTo(entityMounting.getX(), entityMounting.getY(), entityMounting.getZ(), entityMounting.yRotO, entityMounting.xRotO);
         return false;
      } else {
         return true;
      }
   }

   public static boolean onAnimalTame(Animal animal, Player tamer) {
      return ((AnimalTameEvent)NeoForge.EVENT_BUS.post(new AnimalTameEvent(animal, tamer))).isCanceled();
   }

   public static Either<Player.BedSleepingProblem, Unit> canPlayerStartSleeping(
      ServerPlayer player, BlockPos pos, Either<Player.BedSleepingProblem, Unit> vanillaResult
   ) {
      CanPlayerSleepEvent event = new CanPlayerSleepEvent(player, pos, (Player.BedSleepingProblem)vanillaResult.left().orElse(null));
      NeoForge.EVENT_BUS.post(event);
      return event.getProblem() != null ? Either.left(event.getProblem()) : Either.right(Unit.INSTANCE);
   }

   public static void onPlayerWakeup(Player player, boolean wakeImmediately, boolean updateLevel) {
      NeoForge.EVENT_BUS.post(new PlayerWakeUpEvent(player, wakeImmediately, updateLevel));
   }

   public static void onPlayerFall(Player player, float distance, float multiplier) {
      NeoForge.EVENT_BUS.post(new PlayerFlyableFallEvent(player, distance, multiplier));
   }

   public static boolean onPlayerSpawnSet(Player player, @Nullable ServerPlayer.RespawnConfig respawnConfig) {
      return ((PlayerSetSpawnEvent)NeoForge.EVENT_BUS.post(new PlayerSetSpawnEvent(player, respawnConfig))).isCanceled();
   }

   public static void onPlayerClone(Player player, Player oldPlayer, boolean wasDeath) {
      NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(player, oldPlayer, wasDeath));
   }

   public static boolean onExplosionStart(Level level, ServerExplosion explosion) {
      return ((ExplosionEvent.Start)NeoForge.EVENT_BUS.post(new ExplosionEvent.Start(level, explosion))).isCanceled();
   }

   public static void onExplosionDetonate(Level level, ServerExplosion explosion, List<Entity> entities, List<BlockPos> blocks) {
      NeoForge.EVENT_BUS.post(new ExplosionEvent.Detonate(level, explosion, entities, blocks));
   }

   public static Vec3 getExplosionKnockback(Level level, ServerExplosion explosion, Entity entity, Vec3 initialVelocity, List<BlockPos> blocks) {
      ExplosionKnockbackEvent event = new ExplosionKnockbackEvent(level, explosion, entity, initialVelocity, blocks);
      NeoForge.EVENT_BUS.post(event);
      return event.getKnockbackVelocity();
   }

   public static boolean onCreateWorldSpawn(Level level, ServerLevelData settings) {
      return ((LevelEvent.CreateSpawnPosition)NeoForge.EVENT_BUS.post(new LevelEvent.CreateSpawnPosition(level, settings))).isCanceled();
   }

   public static float onLivingHeal(LivingEntity entity, float amount) {
      LivingHealEvent event = new LivingHealEvent(entity, amount);
      return ((LivingHealEvent)NeoForge.EVENT_BUS.post(event)).isCanceled() ? 0.0F : event.getAmount();
   }

   public static boolean onPotionAttemptBrew(NonNullList<ItemStack> stacks) {
      NonNullList<ItemStack> tmp = NonNullList.withSize(stacks.size(), ItemStack.EMPTY);

      for (int x = 0; x < tmp.size(); x++) {
         tmp.set(x, stacks.get(x).copy());
      }

      PotionBrewEvent.Pre event = new PotionBrewEvent.Pre(tmp);
      if (!((PotionBrewEvent.Pre)NeoForge.EVENT_BUS.post(event)).isCanceled()) {
         return false;
      } else {
         boolean changed = false;

         for (int x = 0; x < stacks.size(); x++) {
            changed |= ItemStack.matches(tmp.get(x), stacks.get(x));
            stacks.set(x, event.getItem(x));
         }

         if (changed) {
            onPotionBrewed(stacks);
         }

         return true;
      }
   }

   public static void onPotionBrewed(NonNullList<ItemStack> brewingItemStacks) {
      NeoForge.EVENT_BUS.post(new PotionBrewEvent.Post(brewingItemStacks));
   }

   public static void onPlayerBrewedPotion(Player player, ItemStack stack) {
      NeoForge.EVENT_BUS.post(new PlayerBrewedPotionEvent(player, stack));
   }

   public static boolean canEntityContinueSleeping(LivingEntity sleeper, @Nullable Player.BedSleepingProblem problem) {
      return ((CanContinueSleepingEvent)NeoForge.EVENT_BUS.post(new CanContinueSleepingEvent(sleeper, problem))).mayContinueSleeping();
   }

   public static InteractionResult onArrowNock(ItemStack item, Level level, Player player, InteractionHand hand, boolean hasAmmo) {
      ArrowNockEvent event = new ArrowNockEvent(player, item, hand, level, hasAmmo);
      return (InteractionResult)(((ArrowNockEvent)NeoForge.EVENT_BUS.post(event)).isCanceled() ? InteractionResult.FAIL : event.getAction());
   }

   public static int onArrowLoose(ItemStack stack, Level level, Player player, int charge, boolean hasAmmo) {
      ArrowLooseEvent event = new ArrowLooseEvent(player, stack, level, charge, hasAmmo);
      return ((ArrowLooseEvent)NeoForge.EVENT_BUS.post(event)).isCanceled() ? -1 : event.getCharge();
   }

   public static boolean onProjectileImpact(Projectile projectile, HitResult ray) {
      return ((ProjectileImpactEvent)NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(projectile, ray))).isCanceled();
   }

   @Internal
   @Nullable
   public static LootTable loadLootTable(HolderLookup.Provider registries, Identifier name, LootTable table) {
      if (table == LootTable.EMPTY) {
         return null;
      } else {
         LootTableLoadEvent event = new LootTableLoadEvent(registries, name, table);
         return !((LootTableLoadEvent)NeoForge.EVENT_BUS.post(event)).isCanceled() && event.getTable() != LootTable.EMPTY ? event.getTable() : null;
      }
   }

   public static boolean canCreateFluidSource(ServerLevel level, BlockPos pos, BlockState state) {
      return ((CreateFluidSourceEvent)NeoForge.EVENT_BUS.post(new CreateFluidSourceEvent(level, pos, state))).canConvert();
   }

   public static Optional<PortalShape> onTrySpawnPortal(LevelAccessor level, BlockPos pos, Optional<PortalShape> size) {
      if (!size.isPresent()) {
         return size;
      } else {
         return !((BlockEvent.PortalSpawnEvent)NeoForge.EVENT_BUS.post(new BlockEvent.PortalSpawnEvent(level, pos, level.getBlockState(pos), size.get())))
               .isCanceled()
            ? size
            : Optional.empty();
      }
   }

   public static int onEnchantmentLevelSet(Level level, BlockPos pos, int enchantRow, int power, ItemStack itemStack, int enchantmentLevel) {
      EnchantmentLevelSetEvent e = new EnchantmentLevelSetEvent(level, pos, enchantRow, power, itemStack, enchantmentLevel);
      NeoForge.EVENT_BUS.post(e);
      return e.getEnchantLevel();
   }

   public static boolean onEntityDestroyBlock(LivingEntity entity, BlockPos pos, BlockState state) {
      return !((LivingDestroyBlockEvent)NeoForge.EVENT_BUS.post(new LivingDestroyBlockEvent(entity, pos, state))).isCanceled();
   }

   public static boolean canEntityGrief(ServerLevel level, @Nullable Entity entity) {
      return entity == null
         ? level.getGameRules().get(GameRules.MOB_GRIEFING)
         : ((EntityMobGriefingEvent)NeoForge.EVENT_BUS.post(new EntityMobGriefingEvent(level, entity))).canGrief();
   }

   public static BlockGrowFeatureEvent fireBlockGrowFeature(
      LevelAccessor level, RandomSource rand, BlockPos pos, @Nullable Holder<ConfiguredFeature<?, ?>> holder
   ) {
      return (BlockGrowFeatureEvent)NeoForge.EVENT_BUS.post(new BlockGrowFeatureEvent(level, rand, pos, holder));
   }

   public static AlterGroundEvent.StateProvider alterGround(TreeDecorator.Context ctx, List<BlockPos> positions, AlterGroundEvent.StateProvider provider) {
      if (positions.isEmpty()) {
         return provider;
      } else {
         AlterGroundEvent event = new AlterGroundEvent(ctx, positions, provider);
         NeoForge.EVENT_BUS.post(event);
         return event.getStateProvider();
      }
   }

   public static void fireChunkTicketLevelUpdated(ServerLevel level, long chunkPos, int oldTicketLevel, int newTicketLevel, @Nullable ChunkHolder chunkHolder) {
      if (oldTicketLevel != newTicketLevel) {
         NeoForge.EVENT_BUS.post(new ChunkTicketLevelUpdatedEvent(level, chunkPos, oldTicketLevel, newTicketLevel, chunkHolder));
      }
   }

   public static void fireChunkWatch(ServerPlayer entity, LevelChunk chunk, ServerLevel level) {
      NeoForge.EVENT_BUS.post(new ChunkWatchEvent.Watch(entity, chunk, level));
   }

   public static void fireChunkSent(ServerPlayer entity, LevelChunk chunk, ServerLevel level) {
      NeoForge.EVENT_BUS.post(new ChunkWatchEvent.Sent(entity, chunk, level));
   }

   public static void fireChunkUnWatch(ServerPlayer entity, ChunkPos chunkpos, ServerLevel level) {
      NeoForge.EVENT_BUS.post(new ChunkWatchEvent.UnWatch(entity, chunkpos, level));
   }

   public static boolean onPistonMovePre(Level level, BlockPos pos, Direction direction, boolean extending) {
      return ((PistonEvent.Pre)NeoForge.EVENT_BUS
            .post(new PistonEvent.Pre(level, pos, direction, extending ? PistonEvent.PistonMoveType.EXTEND : PistonEvent.PistonMoveType.RETRACT)))
         .isCanceled();
   }

   public static void onPistonMovePost(Level level, BlockPos pos, Direction direction, boolean extending) {
      NeoForge.EVENT_BUS.post(new PistonEvent.Post(level, pos, direction, extending ? PistonEvent.PistonMoveType.EXTEND : PistonEvent.PistonMoveType.RETRACT));
   }

   public static long onSleepFinished(ServerLevel level, long newTime, long minTime) {
      SleepFinishedTimeEvent event = new SleepFinishedTimeEvent(level, newTime, minTime);
      NeoForge.EVENT_BUS.post(event);
      return event.getNewTime();
   }

   public static List<PreparableReloadListener> onResourceReload(ReloadableServerResources serverResources, RegistryAccess registryAccess) {
      AddServerReloadListenersEvent event = new AddServerReloadListenersEvent(serverResources, registryAccess);
      NeoForge.EVENT_BUS.post(event);
      return ReloadListenerSort.sort(event);
   }

   public static void onCommandRegister(CommandDispatcher<CommandSourceStack> dispatcher, Commands.CommandSelection environment, CommandBuildContext context) {
      RegisterCommandsEvent event = new RegisterCommandsEvent(dispatcher, environment, context);
      NeoForge.EVENT_BUS.post(event);
   }

   public static EntityEvent.Size getEntitySizeForge(Entity entity, Pose pose, EntityDimensions size) {
      EntityEvent.Size evt = new EntityEvent.Size(entity, pose, size);
      NeoForge.EVENT_BUS.post(evt);
      return evt;
   }

   public static EntityEvent.Size getEntitySizeForge(Entity entity, Pose pose, EntityDimensions oldSize, EntityDimensions newSize) {
      EntityEvent.Size evt = new EntityEvent.Size(entity, pose, oldSize, newSize);
      NeoForge.EVENT_BUS.post(evt);
      return evt;
   }

   public static boolean canLivingConvert(LivingEntity entity, EntityType<? extends LivingEntity> outcome, Consumer<Integer> timer) {
      return !((LivingConversionEvent.Pre)NeoForge.EVENT_BUS.post(new LivingConversionEvent.Pre(entity, outcome, timer))).isCanceled();
   }

   public static void onLivingConvert(LivingEntity entity, LivingEntity outcome) {
      NeoForge.EVENT_BUS.post(new LivingConversionEvent.Post(entity, outcome));
   }

   public static EntityTeleportEvent.TeleportCommand onEntityTeleportCommand(Entity entity, double targetX, double targetY, double targetZ) {
      EntityTeleportEvent.TeleportCommand event = new EntityTeleportEvent.TeleportCommand(entity, targetX, targetY, targetZ);
      NeoForge.EVENT_BUS.post(event);
      return event;
   }

   public static EntityTeleportEvent.SpreadPlayersCommand onEntityTeleportSpreadPlayersCommand(Entity entity, double targetX, double targetY, double targetZ) {
      EntityTeleportEvent.SpreadPlayersCommand event = new EntityTeleportEvent.SpreadPlayersCommand(entity, targetX, targetY, targetZ);
      NeoForge.EVENT_BUS.post(event);
      return event;
   }

   public static EntityTeleportEvent.EnderEntity onEnderTeleport(LivingEntity entity, double targetX, double targetY, double targetZ) {
      EntityTeleportEvent.EnderEntity event = new EntityTeleportEvent.EnderEntity(entity, targetX, targetY, targetZ);
      NeoForge.EVENT_BUS.post(event);
      return event;
   }

   @Internal
   public static EntityTeleportEvent.EnderPearl onEnderPearlLand(
      ServerPlayer entity, double targetX, double targetY, double targetZ, ThrownEnderpearl pearlEntity, float attackDamage, HitResult hitResult
   ) {
      EntityTeleportEvent.EnderPearl event = new EntityTeleportEvent.EnderPearl(entity, targetX, targetY, targetZ, pearlEntity, attackDamage, hitResult);
      NeoForge.EVENT_BUS.post(event);
      return event;
   }

   public static EntityTeleportEvent.ItemConsumption onItemConsumptionTeleport(
      LivingEntity entity, ItemStack itemStack, double targetX, double targetY, double targetZ
   ) {
      EntityTeleportEvent.ItemConsumption event = new EntityTeleportEvent.ItemConsumption(entity, itemStack, targetX, targetY, targetZ);
      NeoForge.EVENT_BUS.post(event);
      return event;
   }

   public static boolean onPermissionChanged(NameAndId nameAndId, LevelBasedPermissionSet newPermissions, PlayerList playerList) {
      LevelBasedPermissionSet oldPermissions = playerList.getServer().getProfilePermissions(nameAndId);
      ServerPlayer player = playerList.getPlayer(nameAndId.id());
      return newPermissions.level() != oldPermissions.level() && player != null
         ? ((PermissionsChangedEvent)NeoForge.EVENT_BUS.post(new PermissionsChangedEvent(player, newPermissions, oldPermissions))).isCanceled()
         : false;
   }

   public static void firePlayerChangedDimensionEvent(Player player, ResourceKey<Level> fromDim, ResourceKey<Level> toDim) {
      NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(player, fromDim, toDim));
   }

   public static void firePlayerLoggedIn(Player player) {
      NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
   }

   public static void firePlayerLoggedOut(Player player) {
      NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(player));
   }

   public static PlayerRespawnPositionEvent firePlayerRespawnPositionEvent(ServerPlayer player, TeleportTransition teleportTransition, boolean fromEndFight) {
      return (PlayerRespawnPositionEvent)NeoForge.EVENT_BUS.post(new PlayerRespawnPositionEvent(player, teleportTransition, fromEndFight));
   }

   public static void firePlayerRespawnEvent(ServerPlayer player, boolean fromEndFight) {
      NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(player, fromEndFight));
   }

   public static void firePlayerCraftingEvent(Player player, ItemStack crafted, Container craftMatrix) {
      NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, crafted, craftMatrix));
   }

   public static void firePlayerSmeltedEvent(Player player, ItemStack smelted, int amountRemoved) {
      NeoForge.EVENT_BUS.post(new PlayerEvent.ItemSmeltedEvent(player, smelted, amountRemoved));
   }

   public static EntityTickEvent.Pre fireEntityTickPre(Entity entity) {
      return (EntityTickEvent.Pre)NeoForge.EVENT_BUS.post(new EntityTickEvent.Pre(entity));
   }

   public static void fireEntityTickPost(Entity entity) {
      NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
   }

   public static void firePlayerTickPre(Player player) {
      NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre(player));
   }

   public static void firePlayerTickPost(Player player) {
      NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
   }

   public static void fireLevelTickPre(Level level, BooleanSupplier haveTime) {
      NeoForge.EVENT_BUS.post(new LevelTickEvent.Pre(haveTime, level));
   }

   public static void fireLevelTickPost(Level level, BooleanSupplier haveTime) {
      NeoForge.EVENT_BUS.post(new LevelTickEvent.Post(haveTime, level));
   }

   public static void fireServerTickPre(BooleanSupplier haveTime, MinecraftServer server) {
      NeoForge.EVENT_BUS.post(new ServerTickEvent.Pre(haveTime, server));
   }

   public static void fireServerTickPost(BooleanSupplier haveTime, MinecraftServer server) {
      NeoForge.EVENT_BUS.post(new ServerTickEvent.Post(haveTime, server));
   }

   public static WeightedList<MobSpawnSettings.SpawnerData> getPotentialSpawns(
      LevelAccessor level, MobCategory category, BlockPos pos, WeightedList<MobSpawnSettings.SpawnerData> oldList
   ) {
      LevelEvent.PotentialSpawns event = new LevelEvent.PotentialSpawns(level, category, pos, oldList);
      if (((LevelEvent.PotentialSpawns)NeoForge.EVENT_BUS.post(event)).isCanceled()) {
         return NO_SPAWNS;
      } else {
         return event.getSpawnerDataList() == oldList.unwrap() ? oldList : WeightedList.of(event.getSpawnerDataList());
      }
   }

   public static StatAwardEvent onStatAward(Player player, Stat<?> stat, int value) {
      StatAwardEvent event = new StatAwardEvent(player, stat, value);
      NeoForge.EVENT_BUS.post(event);
      return event;
   }

   @Internal
   public static void onAdvancementEarnedEvent(Player player, AdvancementHolder earned) {
      NeoForge.EVENT_BUS.post(new AdvancementEvent.AdvancementEarnEvent(player, earned));
   }

   @Internal
   public static void onAdvancementProgressedEvent(
      Player player,
      AdvancementHolder progressed,
      AdvancementProgress advancementProgress,
      String criterion,
      AdvancementEvent.AdvancementProgressEvent.ProgressType progressType
   ) {
      NeoForge.EVENT_BUS.post(new AdvancementEvent.AdvancementProgressEvent(player, progressed, advancementProgress, criterion, progressType));
   }

   public static boolean onEffectRemoved(LivingEntity entity, Holder<MobEffect> effect) {
      return ((MobEffectEvent.Remove)NeoForge.EVENT_BUS.post(new MobEffectEvent.Remove(entity, effect))).isCanceled();
   }

   public static boolean onEffectRemoved(LivingEntity entity, MobEffectInstance effectInstance) {
      return ((MobEffectEvent.Remove)NeoForge.EVENT_BUS.post(new MobEffectEvent.Remove(entity, effectInstance))).isCanceled();
   }

   public static int getEnchantmentLevelSpecific(int level, ItemStack stack, Holder<Enchantment> ench) {
      HolderLookup.RegistryLookup<Enchantment> lookup = ench.unwrapLookup();
      if (lookup == null) {
         return level;
      } else {
         ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
         enchantments.set(ench, level);
         GetEnchantmentLevelEvent event = new GetEnchantmentLevelEvent(stack, enchantments, ench, ench.unwrapLookup());
         NeoForge.EVENT_BUS.post(event);
         return enchantments.getLevel(ench);
      }
   }

   public static ItemEnchantments getAllEnchantmentLevels(ItemEnchantments enchantments, ItemStack stack, HolderLookup.RegistryLookup<Enchantment> lookup) {
      ItemEnchantments.Mutable mutableEnchantments = new ItemEnchantments.Mutable(enchantments);
      GetEnchantmentLevelEvent event = new GetEnchantmentLevelEvent(stack, mutableEnchantments, null, lookup);
      NeoForge.EVENT_BUS.post(event);
      return mutableEnchantments.toImmutable();
   }

   @Internal
   public static void onCreativeModeTabBuildContents(
      CreativeModeTab tab,
      ResourceKey<CreativeModeTab> tabKey,
      CreativeModeTab.DisplayItemsGenerator originalGenerator,
      CreativeModeTab.ItemDisplayParameters params,
      CreativeModeTab.Output output
   ) {
      InsertableLinkedOpenCustomHashSet<ItemStack> parentEntries = new InsertableLinkedOpenCustomHashSet<>(ItemStackLinkedSet.TYPE_AND_TAG);
      InsertableLinkedOpenCustomHashSet<ItemStack> searchEntries = new InsertableLinkedOpenCustomHashSet<>(ItemStackLinkedSet.TYPE_AND_TAG);
      originalGenerator.accept(params, (stack, vis) -> {
         if (stack.getCount() != 1) {
            throw new IllegalArgumentException("The stack count must be 1");
         } else {
            if (BuildCreativeModeTabContentsEvent.isParentTab(vis)) {
               parentEntries.add(stack);
            }

            if (BuildCreativeModeTabContentsEvent.isSearchTab(vis)) {
               searchEntries.add(stack);
            }
         }
      });
      ModLoader.postEvent(new BuildCreativeModeTabContentsEvent(tab, tabKey, params, parentEntries, searchEntries));
      ObjectListIterator var7 = parentEntries.iterator();

      while (var7.hasNext()) {
         ItemStack entry = (ItemStack)var7.next();
         output.accept(entry, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
      }

      var7 = searchEntries.iterator();

      while (var7.hasNext()) {
         ItemStack entry = (ItemStack)var7.next();
         output.accept(entry, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
      }
   }

   public static MobSplitEvent onMobSplit(Mob parent, List<Mob> children) {
      MobSplitEvent event = new MobSplitEvent(parent, children);
      NeoForge.EVENT_BUS.post(event);
      return event;
   }

   public static List<CustomSpawner> getCustomSpawners(ServerLevel serverLevel, List<CustomSpawner> customSpawners) {
      ModifyCustomSpawnersEvent event = new ModifyCustomSpawnersEvent(serverLevel, new ArrayList<>(customSpawners));
      NeoForge.EVENT_BUS.post(event);
      return event.getCustomSpawners();
   }

   public static <T> void onGameRuleChanged(MinecraftServer server, GameRule<T> gameRule, T newValue) {
      NeoForge.EVENT_BUS.post(new GameRuleChangedEvent(server, gameRule, newValue));
   }
}
