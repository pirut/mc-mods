package net.neoforged.neoforge.common;

import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.ResourceLocationException;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.fixes.StructuresBecomeConfiguredFix;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TippedArrowItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.asm.enumextension.ExtensionInfo;
import net.neoforged.fml.i18n.MavenVersionTranslator;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifierManager;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.DifficultyChangeEvent;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.GrindstoneEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;
import net.neoforged.neoforge.event.ModMismatchEvent;
import net.neoforged.neoforge.event.RegisterStructureConversionsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.ArmorHurtEvent;
import net.neoforged.neoforge.event.entity.living.EnderManAngerEvent;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDrownEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingGetProjectileEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEnchantItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.SweepAttackEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.NoteBlockEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.resource.ResourcePackLoader;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforgespi.language.IModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public class CommonHooks {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Marker WORLDPERSISTENCE = MarkerManager.getMarker("WP");
   static final Pattern URL_PATTERN = Pattern.compile(
      "((?:[a-z0-9]{2,}:\\/\\/)?(?:(?:[0-9]{1,3}\\.){3}[0-9]{1,3}|(?:[-\\w_]{1,}\\.[a-z]{2,}?))(?::[0-9]{1,5})?.*?(?=[!\"§ \n]|$))", 2
   );
   private static ThreadLocal<Player> craftingPlayer = new ThreadLocal<>();
   public static final int VANILLA_SERIALIZER_LIMIT = 256;
   private static final Set<String> VANILLA_DIMS = Sets.newHashSet(new String[]{"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"});
   private static final String DIMENSIONS_KEY = "dimensions";
   private static final String SEED_KEY = "seed";
   private static final Map<EntityType<? extends LivingEntity>, AttributeSupplier> FORGE_ATTRIBUTES = new HashMap<>();
   @Nullable
   private static ListTag modList;
   private static final Lazy<Map<String, StructuresBecomeConfiguredFix.Conversion>> FORGE_CONVERSION_MAP = Lazy.of(() -> {
      Map<String, StructuresBecomeConfiguredFix.Conversion> map = new HashMap<>();
      NeoForge.EVENT_BUS.post(new RegisterStructureConversionsEvent(map));
      return ImmutableMap.copyOf(map);
   });
   private static final Set<Class<?>> checkedComponentClasses = ConcurrentHashMap.newKeySet();

   public static boolean canContinueUsing(ItemStack from, ItemStack to) {
      return !from.isEmpty() && !to.isEmpty() ? from.getItem().canContinueUsing(from, to) : false;
   }

   public static boolean onItemStackedOn(
      ItemStack carriedItem, ItemStack stackedOnItem, Slot slot, ClickAction action, Player player, SlotAccess carriedSlotAccess
   ) {
      return ((ItemStackedOnOtherEvent)NeoForge.EVENT_BUS
            .post(new ItemStackedOnOtherEvent(carriedItem, stackedOnItem, slot, action, player, carriedSlotAccess)))
         .isCanceled();
   }

   public static void onDifficultyChange(Difficulty difficulty, Difficulty oldDifficulty) {
      NeoForge.EVENT_BUS.post(new DifficultyChangeEvent(difficulty, oldDifficulty));
   }

   public static LivingChangeTargetEvent onLivingChangeTarget(
      LivingEntity entity, @Nullable LivingEntity originalTarget, LivingChangeTargetEvent.ILivingTargetType targetType
   ) {
      LivingChangeTargetEvent event = new LivingChangeTargetEvent(entity, originalTarget, targetType);
      NeoForge.EVENT_BUS.post(event);
      return event;
   }

   public static boolean isEntityInvulnerableTo(Entity entity, DamageSource source, boolean isInvul) {
      return ((EntityInvulnerabilityCheckEvent)NeoForge.EVENT_BUS.post(new EntityInvulnerabilityCheckEvent(entity, source, isInvul))).isInvulnerable();
   }

   public static boolean onEntityIncomingDamage(LivingEntity entity, DamageContainer container) {
      return ((LivingIncomingDamageEvent)NeoForge.EVENT_BUS.post(new LivingIncomingDamageEvent(entity, container))).isCanceled();
   }

   public static LivingKnockBackEvent onLivingKnockBack(LivingEntity target, float strength, double ratioX, double ratioZ) {
      LivingKnockBackEvent event = new LivingKnockBackEvent(target, strength, ratioX, ratioZ);
      NeoForge.EVENT_BUS.post(event);
      return event;
   }

   public static boolean onLivingUseTotem(LivingEntity entity, DamageSource damageSource, ItemStack totem, InteractionHand hand) {
      return !((LivingUseTotemEvent)NeoForge.EVENT_BUS.post(new LivingUseTotemEvent(entity, damageSource, totem, hand))).isCanceled();
   }

   public static float onLivingDamagePre(LivingEntity entity, DamageContainer container) {
      return ((LivingDamageEvent.Pre)NeoForge.EVENT_BUS.post(new LivingDamageEvent.Pre(entity, container))).getNewDamage();
   }

   public static void onLivingDamagePost(LivingEntity entity, DamageContainer container) {
      NeoForge.EVENT_BUS.post(new LivingDamageEvent.Post(entity, container));
   }

   public static void onArmorHurt(DamageSource source, EquipmentSlot[] slots, float damage, LivingEntity armoredEntity) {
      EnumMap<EquipmentSlot, ArmorHurtEvent.ArmorEntry> armorMap = new EnumMap<>(EquipmentSlot.class);

      for (EquipmentSlot slot : slots) {
         ItemStack armorPiece = armoredEntity.getItemBySlot(slot);
         if (!armorPiece.isEmpty()) {
            float damageAfterFireResist = armorPiece.getItem() instanceof ArmorItem && armorPiece.canBeHurtBy(source) ? damage : 0.0F;
            armorMap.put(slot, new ArmorHurtEvent.ArmorEntry(armorPiece, damageAfterFireResist));
         }
      }

      ArmorHurtEvent event = (ArmorHurtEvent)NeoForge.EVENT_BUS.post(new ArmorHurtEvent(armorMap, armoredEntity, source));
      if (!event.isCanceled()) {
         event.getArmorMap().forEach((slotx, entry) -> entry.armorItemStack.hurtAndBreak((int)entry.newDamage, armoredEntity, slotx));
      }
   }

   public static boolean onLivingDeath(LivingEntity entity, DamageSource src) {
      return ((LivingDeathEvent)NeoForge.EVENT_BUS.post(new LivingDeathEvent(entity, src))).isCanceled();
   }

   public static boolean onLivingDrops(LivingEntity entity, DamageSource source, Collection<ItemEntity> drops, boolean recentlyHit) {
      return ((LivingDropsEvent)NeoForge.EVENT_BUS.post(new LivingDropsEvent(entity, source, drops, recentlyHit))).isCanceled();
   }

   @Nullable
   public static float[] onLivingFall(LivingEntity entity, float distance, float damageMultiplier) {
      LivingFallEvent event = new LivingFallEvent(entity, distance, damageMultiplier);
      return ((LivingFallEvent)NeoForge.EVENT_BUS.post(event)).isCanceled() ? null : new float[]{event.getDistance(), event.getDamageMultiplier()};
   }

   public static double getEntityVisibilityMultiplier(LivingEntity entity, Entity lookingEntity, double originalMultiplier) {
      LivingEvent.LivingVisibilityEvent event = new LivingEvent.LivingVisibilityEvent(entity, lookingEntity, originalMultiplier);
      NeoForge.EVENT_BUS.post(event);
      return Math.max(0.0, event.getVisibilityModifier());
   }

   public static Optional<BlockPos> isLivingOnLadder(BlockState state, Level level, BlockPos pos, LivingEntity entity) {
      boolean isSpectator = entity instanceof Player && entity.isSpectator();
      if (isSpectator) {
         return Optional.empty();
      } else if (!NeoForgeConfig.SERVER.fullBoundingBoxLadders.get()) {
         return state.isLadder(level, pos, entity) ? Optional.of(pos) : Optional.empty();
      } else {
         AABB bb = entity.getBoundingBox();
         int mX = Mth.floor(bb.minX);
         int mY = Mth.floor(bb.minY);
         int mZ = Mth.floor(bb.minZ);

         for (int y2 = mY; y2 < bb.maxY; y2++) {
            for (int x2 = mX; x2 < bb.maxX; x2++) {
               for (int z2 = mZ; z2 < bb.maxZ; z2++) {
                  BlockPos tmp = new BlockPos(x2, y2, z2);
                  state = level.getBlockState(tmp);
                  if (state.isLadder(level, tmp, entity)) {
                     return Optional.of(tmp);
                  }
               }
            }
         }

         return Optional.empty();
      }
   }

   public static void onLivingJump(LivingEntity entity) {
      NeoForge.EVENT_BUS.post(new LivingEvent.LivingJumpEvent(entity));
   }

   @Nullable
   public static ItemEntity onPlayerTossEvent(Player player, ItemStack item, boolean includeName) {
      player.captureDrops(Lists.newArrayList());
      ItemEntity ret = player.drop(item, false, includeName);
      player.captureDrops(null);
      if (ret == null) {
         return null;
      } else {
         ItemTossEvent event = new ItemTossEvent(ret, player);
         if (((ItemTossEvent)NeoForge.EVENT_BUS.post(event)).isCanceled()) {
            return null;
         } else {
            if (!player.level().isClientSide) {
               player.getCommandSenderWorld().addFreshEntity(event.getEntity());
            }

            return event.getEntity();
         }
      }
   }

   public static boolean onVanillaGameEvent(Level level, Holder<GameEvent> vanillaEvent, Vec3 pos, GameEvent.Context context) {
      return !((VanillaGameEvent)NeoForge.EVENT_BUS.post(new VanillaGameEvent(level, vanillaEvent, pos, context))).isCanceled();
   }

   private static String getRawText(Component message) {
      return message.getContents() instanceof PlainTextContents plainTextContents ? plainTextContents.text() : "";
   }

   @Nullable
   public static Component onServerChatSubmittedEvent(ServerPlayer player, String plain, Component decorated) {
      ServerChatEvent event = new ServerChatEvent(player, plain, decorated);
      return ((ServerChatEvent)NeoForge.EVENT_BUS.post(event)).isCanceled() ? null : event.getMessage();
   }

   public static ChatDecorator getServerChatSubmittedDecorator() {
      return (sender, message) -> sender == null ? message : onServerChatSubmittedEvent(sender, getRawText(message), message);
   }

   public static Component newChatWithLinks(String string) {
      return newChatWithLinks(string, true);
   }

   public static Component newChatWithLinks(String string, boolean allowMissingHeader) {
      MutableComponent ichat = null;
      Matcher matcher = URL_PATTERN.matcher(string);
      int lastEnd = 0;

      while (true) {
         String url;
         MutableComponent link;
         while (true) {
            if (!matcher.find()) {
               String end = string.substring(lastEnd);
               if (ichat == null) {
                  ichat = Component.literal(end);
               } else if (end.length() > 0) {
                  ichat.append(Component.literal(string.substring(lastEnd)));
               }

               return ichat;
            }

            int start = matcher.start();
            int end = matcher.end();
            String part = string.substring(lastEnd, start);
            if (part.length() > 0) {
               if (ichat == null) {
                  ichat = Component.literal(part);
               } else {
                  ichat.append(part);
               }
            }

            lastEnd = end;
            url = string.substring(start, end);
            link = Component.literal(url);

            try {
               if (new URI(url).getScheme() != null) {
                  break;
               }

               if (allowMissingHeader) {
                  url = "http://" + url;
                  break;
               }

               if (ichat == null) {
                  ichat = Component.literal(url);
               } else {
                  ichat.append(url);
               }
            } catch (URISyntaxException var11) {
               if (ichat == null) {
                  ichat = Component.literal(url);
               } else {
                  ichat.append(url);
               }
            }
         }

         ClickEvent click = new ClickEvent(ClickEvent.Action.OPEN_URL, url);
         link.setStyle(link.getStyle().withClickEvent(click).withUnderlined(true).withColor(TextColor.fromLegacyFormat(ChatFormatting.BLUE)));
         if (ichat == null) {
            ichat = Component.literal("");
         }

         ichat.append(link);
      }
   }

   public static void handleBlockDrops(
      ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, List<ItemEntity> drops, @Nullable Entity breaker, ItemStack tool
   ) {
      BlockDropsEvent event = new BlockDropsEvent(level, pos, state, blockEntity, drops, breaker, tool);
      NeoForge.EVENT_BUS.post(event);
      if (!event.isCanceled()) {
         for (ItemEntity entity : event.getDrops()) {
            level.addFreshEntity(entity);
         }

         state.spawnAfterBreak(level, pos, tool, false);
         if (event.getDroppedExperience() > 0) {
            state.getBlock().popExperience(level, pos, event.getDroppedExperience());
         }
      }
   }

   public static BlockEvent.BreakEvent fireBlockBreak(Level level, GameType gameType, ServerPlayer player, BlockPos pos, BlockState state) {
      boolean preCancelEvent = false;
      ItemStack itemstack = player.getMainHandItem();
      if (!itemstack.isEmpty() && !itemstack.getItem().canAttackBlock(state, level, pos, player)) {
         preCancelEvent = true;
      }

      if (player.blockActionRestricted(level, pos, gameType)) {
         preCancelEvent = true;
      }

      if (state.getBlock() instanceof GameMasterBlock && !player.canUseGameMasterBlocks()) {
         preCancelEvent = true;
      }

      BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player);
      event.setCanceled(preCancelEvent);
      NeoForge.EVENT_BUS.post(event);
      if (event.isCanceled()) {
         player.connection.send(new ClientboundBlockUpdatePacket(pos, state));
      }

      return event;
   }

   public static InteractionResult onPlaceItemIntoWorld(UseOnContext context) {
      ItemStack itemstack = context.getItemInHand();
      Level level = context.getLevel();
      Player player = context.getPlayer();
      if (player != null && !player.getAbilities().mayBuild) {
         AdventureModePredicate adventureModePredicate = itemstack.get(DataComponents.CAN_PLACE_ON);
         if (adventureModePredicate == null || !adventureModePredicate.test(new BlockInWorld(level, context.getClickedPos(), false))) {
            return InteractionResult.PASS;
         }
      }

      Item item = itemstack.getItem();
      int size = itemstack.getCount();
      DataComponentMap components = itemstack.getComponents();
      if (!(itemstack.getItem() instanceof BucketItem)) {
         level.captureBlockSnapshots = true;
      }

      ItemStack copy = itemstack.copy();
      InteractionResult ret = itemstack.getItem().useOn(context);
      if (itemstack.isEmpty()) {
         EventHooks.onPlayerDestroyItem(player, copy, context.getHand());
      }

      level.captureBlockSnapshots = false;
      if (ret.consumesAction()) {
         int newSize = itemstack.getCount();
         DataComponentMap newComponents = itemstack.getComponents();
         List<BlockSnapshot> blockSnapshots = (List<BlockSnapshot>)level.capturedBlockSnapshots.clone();
         level.capturedBlockSnapshots.clear();
         itemstack.setCount(size);
         itemstack.applyComponents(components);
         Direction side = context.getClickedFace();
         boolean eventResult = false;
         if (blockSnapshots.size() > 1) {
            eventResult = EventHooks.onMultiBlockPlace(player, blockSnapshots, side);
         } else if (blockSnapshots.size() == 1) {
            eventResult = EventHooks.onBlockPlace(player, blockSnapshots.get(0), side);
         }

         if (eventResult) {
            ret = InteractionResult.FAIL;

            for (BlockSnapshot blocksnapshot : Lists.reverse(blockSnapshots)) {
               level.restoringBlockSnapshots = true;
               blocksnapshot.restore(blocksnapshot.getFlags() | 2);
               level.restoringBlockSnapshots = false;
            }
         } else {
            itemstack.setCount(newSize);
            itemstack.applyComponents(newComponents);

            for (BlockSnapshot snap : blockSnapshots) {
               int updateFlag = snap.getFlags();
               BlockState oldBlock = snap.getState();
               BlockState newBlock = level.getBlockState(snap.getPos());
               newBlock.onPlace(level, snap.getPos(), oldBlock, false);
               level.markAndNotifyBlock(snap.getPos(), level.getChunkAt(snap.getPos()), oldBlock, newBlock, updateFlag, 512);
            }

            if (player != null) {
               player.awardStat(Stats.ITEM_USED.get(item));
            }
         }
      }

      level.capturedBlockSnapshots.clear();
      return ret;
   }

   public static void onPlayerEnchantItem(Player player, ItemStack stack, List<EnchantmentInstance> instances) {
      NeoForge.EVENT_BUS.post(new PlayerEnchantItemEvent(player, stack, instances));
   }

   public static boolean onAnvilChange(AnvilMenu container, ItemStack left, ItemStack right, Container outputSlot, String name, long baseCost, Player player) {
      AnvilUpdateEvent e = new AnvilUpdateEvent(left, right, name, baseCost, player);
      if (((AnvilUpdateEvent)NeoForge.EVENT_BUS.post(e)).isCanceled()) {
         outputSlot.setItem(0, ItemStack.EMPTY);
         container.setMaximumCost(0L);
         container.repairItemCountCost = 0;
         return false;
      } else if (e.getOutput().isEmpty()) {
         return true;
      } else {
         outputSlot.setItem(0, e.getOutput());
         container.setMaximumCost(e.getCost());
         container.repairItemCountCost = e.getMaterialCost();
         return false;
      }
   }

   public static float onAnvilRepair(Player player, ItemStack output, ItemStack left, ItemStack right) {
      AnvilRepairEvent e = new AnvilRepairEvent(player, left, right, output);
      NeoForge.EVENT_BUS.post(e);
      return e.getBreakChance();
   }

   public static int onGrindstoneChange(ItemStack top, ItemStack bottom, Container outputSlot, int xp) {
      GrindstoneEvent.OnPlaceItem e = new GrindstoneEvent.OnPlaceItem(top, bottom, xp);
      if (((GrindstoneEvent.OnPlaceItem)NeoForge.EVENT_BUS.post(e)).isCanceled()) {
         outputSlot.setItem(0, ItemStack.EMPTY);
         return -1;
      } else if (e.getOutput().isEmpty()) {
         return Integer.MIN_VALUE;
      } else {
         outputSlot.setItem(0, e.getOutput());
         return e.getXp();
      }
   }

   @Deprecated(
      forRemoval = true,
      since = "1.21.8"
   )
   public static boolean onGrindstoneTake(Container inputSlots, ContainerLevelAccess access, Function<Level, Integer> xpFunction) {
      return onGrindstoneTake(inputSlots, access, null, xpFunction);
   }

   public static boolean onGrindstoneTake(Container inputSlots, ContainerLevelAccess access, @Nullable Player player, Function<Level, Integer> xpFunction) {
      access.execute((l, p) -> {
         int xp = xpFunction.apply(l);
         GrindstoneEvent.OnTakeItem e = new GrindstoneEvent.OnTakeItem(access, player, inputSlots.getItem(0), inputSlots.getItem(1), xp);
         if (!((GrindstoneEvent.OnTakeItem)NeoForge.EVENT_BUS.post(e)).isCanceled()) {
            if (l instanceof ServerLevel) {
               ExperienceOrb.award((ServerLevel)l, Vec3.atCenterOf(p), e.getXp());
            }

            l.levelEvent(1042, p, 0);
            inputSlots.setItem(0, e.getNewTopItem());
            inputSlots.setItem(1, e.getNewBottomItem());
            inputSlots.setChanged();
         }
      });
      return true;
   }

   public static void setCraftingPlayer(Player player) {
      craftingPlayer.set(player);
   }

   public static Player getCraftingPlayer() {
      return craftingPlayer.get();
   }

   public static ItemStack getCraftingRemainingItem(ItemStack stack) {
      if (stack.getItem().hasCraftingRemainingItem(stack)) {
         stack = stack.getItem().getCraftingRemainingItem(stack);
         if (!stack.isEmpty() && stack.isDamageableItem() && stack.getDamageValue() > stack.getMaxDamage()) {
            EventHooks.onPlayerDestroyItem(craftingPlayer.get(), stack, null);
            return ItemStack.EMPTY;
         } else {
            return stack;
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   public static boolean onPlayerAttackTarget(Player player, Entity target) {
      if (((AttackEntityEvent)NeoForge.EVENT_BUS.post(new AttackEntityEvent(player, target))).isCanceled()) {
         return false;
      } else {
         ItemStack stack = player.getWeaponItem();
         return stack.isEmpty() || !stack.getItem().onLeftClickEntity(stack, player, target);
      }
   }

   public static boolean onTravelToDimension(Entity entity, ResourceKey<Level> dimension) {
      EntityTravelToDimensionEvent event = new EntityTravelToDimensionEvent(entity, dimension);
      NeoForge.EVENT_BUS.post(event);
      return !event.isCanceled();
   }

   @Nullable
   public static InteractionResult onInteractEntityAt(Player player, Entity entity, HitResult ray, InteractionHand hand) {
      Vec3 vec3d = ray.getLocation().subtract(entity.position());
      return onInteractEntityAt(player, entity, vec3d, hand);
   }

   @Nullable
   public static InteractionResult onInteractEntityAt(Player player, Entity entity, Vec3 vec3d, InteractionHand hand) {
      PlayerInteractEvent.EntityInteractSpecific evt = new PlayerInteractEvent.EntityInteractSpecific(player, hand, entity, vec3d);
      NeoForge.EVENT_BUS.post(evt);
      return evt.isCanceled() ? evt.getCancellationResult() : null;
   }

   @Nullable
   public static InteractionResult onInteractEntity(Player player, Entity entity, InteractionHand hand) {
      PlayerInteractEvent.EntityInteract evt = new PlayerInteractEvent.EntityInteract(player, hand, entity);
      NeoForge.EVENT_BUS.post(evt);
      return evt.isCanceled() ? evt.getCancellationResult() : null;
   }

   @Nullable
   public static InteractionResult onItemRightClick(Player player, InteractionHand hand) {
      PlayerInteractEvent.RightClickItem evt = new PlayerInteractEvent.RightClickItem(player, hand);
      NeoForge.EVENT_BUS.post(evt);
      return evt.isCanceled() ? evt.getCancellationResult() : null;
   }

   public static PlayerInteractEvent.LeftClickBlock onLeftClickBlock(Player player, BlockPos pos, Direction face, ServerboundPlayerActionPacket.Action action) {
      PlayerInteractEvent.LeftClickBlock evt = new PlayerInteractEvent.LeftClickBlock(
         player, pos, face, PlayerInteractEvent.LeftClickBlock.Action.convert(action)
      );
      NeoForge.EVENT_BUS.post(evt);
      return evt;
   }

   public static PlayerInteractEvent.LeftClickBlock onClientMineHold(Player player, BlockPos pos, Direction face) {
      PlayerInteractEvent.LeftClickBlock evt = new PlayerInteractEvent.LeftClickBlock(player, pos, face, PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD);
      NeoForge.EVENT_BUS.post(evt);
      return evt;
   }

   public static PlayerInteractEvent.RightClickBlock onRightClickBlock(Player player, InteractionHand hand, BlockPos pos, BlockHitResult hitVec) {
      PlayerInteractEvent.RightClickBlock evt = new PlayerInteractEvent.RightClickBlock(player, hand, pos, hitVec);
      NeoForge.EVENT_BUS.post(evt);
      return evt;
   }

   public static void onEmptyClick(Player player, InteractionHand hand) {
      NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickEmpty(player, hand));
   }

   public static void onEmptyLeftClick(Player player) {
      NeoForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickEmpty(player));
   }

   @Nullable
   public static GameType onChangeGameType(Player player, GameType currentGameType, GameType newGameType) {
      if (currentGameType != newGameType) {
         PlayerEvent.PlayerChangeGameModeEvent evt = new PlayerEvent.PlayerChangeGameModeEvent(player, currentGameType, newGameType);
         NeoForge.EVENT_BUS.post(evt);
         return evt.isCanceled() ? null : evt.getNewGameMode();
      } else {
         return newGameType;
      }
   }

   @Internal
   public static Codec<List<LootPool>> lootPoolsCodec(BiConsumer<LootPool, String> nameSetter) {
      Decoder<List<LootPool>> decoder = ConditionalOps.createConditionalCodec(LootPool.CODEC).listOf().map(pools -> {
         if (pools.size() == 1) {
            if (((Optional)pools.get(0)).isPresent() && ((LootPool)((Optional)pools.get(0)).get()).getName() == null) {
               nameSetter.accept((LootPool)((Optional)pools.get(0)).get(), "main");
            }
         } else {
            for (int i = 0; i < pools.size(); i++) {
               if (((Optional)pools.get(i)).isPresent() && ((LootPool)((Optional)pools.get(i)).get()).getName() == null) {
                  nameSetter.accept((LootPool)((Optional)pools.get(i)).get(), "pool" + i);
               }
            }
         }

         return pools.stream().filter(Optional::isPresent).map(Optional::get).toList();
      });
      return Codec.of(LootPool.CODEC.listOf(), decoder);
   }

   public static FluidType getVanillaFluidType(Fluid fluid) {
      if (fluid == Fluids.EMPTY) {
         return NeoForgeMod.EMPTY_TYPE.value();
      } else if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
         return NeoForgeMod.WATER_TYPE.value();
      } else if (fluid != Fluids.LAVA && fluid != Fluids.FLOWING_LAVA) {
         if (!NeoForgeMod.MILK.asOptional().filter(milk -> milk == fluid).isPresent()
            && !NeoForgeMod.FLOWING_MILK.asOptional().filter(milk -> milk == fluid).isPresent()) {
            throw new RuntimeException("Mod fluids must override getFluidType.");
         } else {
            return NeoForgeMod.MILK_TYPE.value();
         }
      } else {
         return NeoForgeMod.LAVA_TYPE.value();
      }
   }

   public static TagKey<Block> getTagFromVanillaTier(Tiers tier) {
      return switch (tier) {
         case WOOD -> Tags.Blocks.NEEDS_WOOD_TOOL;
         case GOLD -> Tags.Blocks.NEEDS_GOLD_TOOL;
         case STONE -> BlockTags.NEEDS_STONE_TOOL;
         case IRON -> BlockTags.NEEDS_IRON_TOOL;
         case DIAMOND -> BlockTags.NEEDS_DIAMOND_TOOL;
         case NETHERITE -> Tags.Blocks.NEEDS_NETHERITE_TOOL;
      };
   }

   public static Collection<CreativeModeTab> onCheckCreativeTabs(CreativeModeTab... vanillaTabs) {
      List<CreativeModeTab> tabs = new ArrayList<>(Arrays.asList(vanillaTabs));
      return tabs;
   }

   public static boolean canCropGrow(Level level, BlockPos pos, BlockState state, boolean def) {
      CropGrowEvent.Pre ev = new CropGrowEvent.Pre(level, pos, state);
      NeoForge.EVENT_BUS.post(ev);
      return ev.getResult() == CropGrowEvent.Pre.Result.GROW || ev.getResult() == CropGrowEvent.Pre.Result.DEFAULT && def;
   }

   public static void fireCropGrowPost(Level level, BlockPos pos, BlockState state) {
      NeoForge.EVENT_BUS.post(new CropGrowEvent.Post(level, pos, state, level.getBlockState(pos)));
   }

   public static CriticalHitEvent fireCriticalHit(Player player, Entity target, boolean vanillaCritical, float damageModifier) {
      return (CriticalHitEvent)NeoForge.EVENT_BUS.post(new CriticalHitEvent(player, target, damageModifier, vanillaCritical));
   }

   public static SweepAttackEvent fireSweepAttack(Player player, Entity target, boolean isVanillaSweep) {
      return (SweepAttackEvent)NeoForge.EVENT_BUS.post(new SweepAttackEvent(player, target, isVanillaSweep));
   }

   public static ItemAttributeModifiers computeModifiedAttributes(ItemStack stack, ItemAttributeModifiers defaultModifiers) {
      ItemAttributeModifierEvent event = new ItemAttributeModifierEvent(stack, defaultModifiers);
      NeoForge.EVENT_BUS.post(event);
      return event.build();
   }

   public static ItemStack getProjectile(LivingEntity entity, ItemStack projectileWeaponItem, ItemStack projectile) {
      LivingGetProjectileEvent event = new LivingGetProjectileEvent(entity, projectileWeaponItem, projectile);
      NeoForge.EVENT_BUS.post(event);
      return event.getProjectileItemStack();
   }

   @Nullable
   public static String getDefaultCreatorModId(ItemStack itemStack) {
      Item item = itemStack.getItem();
      ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(item);
      String modId = registryName == null ? null : registryName.getNamespace();
      if ("minecraft".equals(modId)) {
         if (item instanceof EnchantedBookItem) {
            Set<Holder<Enchantment>> enchantments = itemStack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).keySet();
            if (enchantments.size() == 1) {
               Holder<Enchantment> enchantmentHolder = enchantments.iterator().next();
               Optional<ResourceKey<Enchantment>> key = enchantmentHolder.unwrapKey();
               if (key.isPresent()) {
                  return key.get().location().getNamespace();
               }
            }
         } else if (item instanceof PotionItem || item instanceof TippedArrowItem) {
            PotionContents potionContents = itemStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            Optional<Holder<Potion>> potionType = potionContents.potion();
            Optional<ResourceKey<Potion>> key = potionType.flatMap(Holder::unwrapKey);
            if (key.isPresent()) {
               return key.get().location().getNamespace();
            }
         } else if (item instanceof SpawnEggItem spawnEggItem) {
            Optional<ResourceKey<EntityType<?>>> key = BuiltInRegistries.ENTITY_TYPE.getResourceKey(spawnEggItem.getType(itemStack));
            if (key.isPresent()) {
               return key.get().location().getNamespace();
            }
         }
      }

      return modId;
   }

   public static boolean onFarmlandTrample(Level level, BlockPos pos, BlockState state, float fallDistance, Entity entity) {
      if (entity.canTrample(state, pos, fallDistance)) {
         BlockEvent.FarmlandTrampleEvent event = new BlockEvent.FarmlandTrampleEvent(level, pos, state, fallDistance, entity);
         NeoForge.EVENT_BUS.post(event);
         return !event.isCanceled();
      } else {
         return false;
      }
   }

   public static int onNoteChange(Level level, BlockPos pos, BlockState state, int old, int _new) {
      NoteBlockEvent.Change event = new NoteBlockEvent.Change(level, pos, state, old, _new);
      return ((NoteBlockEvent.Change)NeoForge.EVENT_BUS.post(event)).isCanceled() ? -1 : event.getVanillaNoteId();
   }

   @Nullable
   public static EntityDataSerializer<?> getSerializer(int id, CrudeIncrementalIntIdentityHashBiMap<EntityDataSerializer<?>> vanilla) {
      EntityDataSerializer<?> serializer = vanilla.byId(id);
      return serializer == null ? NeoForgeRegistries.ENTITY_DATA_SERIALIZERS.byId(id - 256) : serializer;
   }

   public static int getSerializerId(EntityDataSerializer<?> serializer, CrudeIncrementalIntIdentityHashBiMap<EntityDataSerializer<?>> vanilla) {
      int id = vanilla.getId(serializer);
      if (id < 0) {
         id = NeoForgeRegistries.ENTITY_DATA_SERIALIZERS.getId(serializer);
         if (id >= 0) {
            return id + 256;
         }
      }

      return id;
   }

   public static boolean canEntityDestroy(Level level, BlockPos pos, LivingEntity entity) {
      if (!level.isLoaded(pos)) {
         return false;
      } else {
         BlockState state = level.getBlockState(pos);
         return EventHooks.canEntityGrief(level, entity) && state.canEntityDestroy(level, pos, entity) && EventHooks.onEntityDestroyBlock(entity, pos, state);
      }
   }

   @Deprecated
   public static List<ItemStack> modifyLoot(List<ItemStack> list, LootContext context) {
      return modifyLoot(LootTableIdCondition.UNKNOWN_LOOT_TABLE, ObjectArrayList.wrap((ItemStack[])list.toArray()), context);
   }

   public static ObjectArrayList<ItemStack> modifyLoot(ResourceLocation lootTableId, ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
      context.setQueriedLootTableId(lootTableId);
      LootModifierManager man = NeoForgeEventHandler.getLootModifierManager();

      for (IGlobalLootModifier mod : man.getAllLootMods()) {
         generatedLoot = mod.apply(generatedLoot, context);
      }

      return generatedLoot;
   }

   public static List<String> getModDataPacks() {
      List<String> modpacks = ResourcePackLoader.getPackNames(PackType.SERVER_DATA);
      if (modpacks.isEmpty()) {
         throw new IllegalStateException("Attempted to retrieve mod packs before they were loaded in!");
      } else {
         return modpacks;
      }
   }

   public static List<String> getModDataPacksWithVanilla() {
      List<String> modpacks = getModDataPacks();
      modpacks.add("vanilla");
      return modpacks;
   }

   @Deprecated
   public static Map<EntityType<? extends LivingEntity>, AttributeSupplier> getAttributesView() {
      return Collections.unmodifiableMap(FORGE_ATTRIBUTES);
   }

   @Deprecated
   public static void modifyAttributes() {
      ModLoader.postEvent(new EntityAttributeCreationEvent(FORGE_ATTRIBUTES));
      Map<EntityType<? extends LivingEntity>, AttributeSupplier.Builder> finalMap = new HashMap<>();
      ModLoader.postEvent(new EntityAttributeModificationEvent(finalMap));
      finalMap.forEach((k, v) -> {
         AttributeSupplier supplier = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>)k);
         AttributeSupplier.Builder newBuilder = supplier != null ? new AttributeSupplier.Builder(supplier) : new AttributeSupplier.Builder();
         newBuilder.combine(v);
         FORGE_ATTRIBUTES.put((EntityType<? extends LivingEntity>)k, newBuilder.build());
      });
   }

   public static void onEntityEnterSection(Entity entity, long packedOldPos, long packedNewPos) {
      NeoForge.EVENT_BUS.post(new EntityEvent.EnteringSection(entity, packedOldPos, packedNewPos));
   }

   public static LivingShieldBlockEvent onDamageBlock(LivingEntity blocker, DamageContainer container, boolean originalBlocked) {
      LivingShieldBlockEvent e = new LivingShieldBlockEvent(blocker, container, originalBlocked);
      NeoForge.EVENT_BUS.post(e);
      return e;
   }

   public static LivingSwapItemsEvent.Hands onLivingSwapHandItems(LivingEntity livingEntity) {
      LivingSwapItemsEvent.Hands event = new LivingSwapItemsEvent.Hands(livingEntity);
      NeoForge.EVENT_BUS.post(event);
      return event;
   }

   @Internal
   public static void writeAdditionalLevelSaveData(WorldData worldData, CompoundTag levelTag) {
      if (modList == null) {
         List<IModInfo> mods = ModList.get().getMods();
         ListTag modListTag = new ListTag(mods.size());
         mods.forEach(mi -> {
            CompoundTag mod = new CompoundTag(2);
            mod.putString("ModId", mi.getModId());
            mod.putString("ModVersion", MavenVersionTranslator.artifactVersionToString(mi.getVersion()));
            modListTag.add(mod);
         });
         modList = modListTag;
      }

      CompoundTag fmlData = new CompoundTag();
      fmlData.put("LoadingModList", modList);
      LOGGER.debug(WORLDPERSISTENCE, "Gathered mod list to write to world save {}", worldData.getLevelName());
      levelTag.put("fml", fmlData);
   }

   @Internal
   public static void readAdditionalLevelSaveData(CompoundTag rootTag, LevelStorageSource.LevelDirectory levelDirectory) {
      CompoundTag tag = rootTag.getCompound("fml");
      if (tag.contains("LoadingModList")) {
         ListTag modList = tag.getList("LoadingModList", 10);
         Map<String, ArtifactVersion> mismatchedVersions = new HashMap<>(modList.size());
         Map<String, ArtifactVersion> missingVersions = new HashMap<>(modList.size());

         for (int i = 0; i < modList.size(); i++) {
            CompoundTag mod = modList.getCompound(i);
            String modId = mod.getString("ModId");
            if (!Objects.equals("minecraft", modId)) {
               String modVersion = mod.getString("ModVersion");
               DefaultArtifactVersion previousVersion = new DefaultArtifactVersion(modVersion);
               ModList.get().getModContainerById(modId).ifPresentOrElse(container -> {
                  ArtifactVersion loadingVersion = container.getModInfo().getVersion();
                  if (!loadingVersion.equals(previousVersion)) {
                     mismatchedVersions.put(modId, previousVersion);
                  }
               }, () -> missingVersions.put(modId, previousVersion));
            }
         }

         ModMismatchEvent mismatchEvent = new ModMismatchEvent(levelDirectory, mismatchedVersions, missingVersions);
         ModLoader.postEvent(mismatchEvent);
         StringBuilder resolved = new StringBuilder("The following mods have version differences that were marked resolved:");
         StringBuilder unresolved = new StringBuilder("The following mods have version differences that were not resolved:");
         mismatchEvent.getResolved()
            .forEachOrdered(
               res -> {
                  String modid = res.modid();
                  ModMismatchEvent.MismatchedVersionInfo diff = res.versionDifference();
                  if (res.wasSelfResolved()) {
                     resolved.append(System.lineSeparator())
                        .append(
                           diff.isMissing()
                              ? "%s (version %s -> MISSING, self-resolved)".formatted(modid, diff.oldVersion())
                              : "%s (version %s -> %s, self-resolved)".formatted(modid, diff.oldVersion(), diff.newVersion())
                        );
                  } else {
                     String resolver = res.resolver().getModId();
                     resolved.append(System.lineSeparator())
                        .append(
                           diff.isMissing()
                              ? "%s (version %s -> MISSING, resolved by %s)".formatted(modid, diff.oldVersion(), resolver)
                              : "%s (version %s -> %s, resolved by %s)".formatted(modid, diff.oldVersion(), diff.newVersion(), resolver)
                        );
                  }
               }
            );
         mismatchEvent.getUnresolved()
            .forEachOrdered(
               unres -> {
                  String modid = unres.modid();
                  ModMismatchEvent.MismatchedVersionInfo diff = unres.versionDifference();
                  unresolved.append(System.lineSeparator())
                     .append(
                        diff.isMissing()
                           ? "%s (version %s -> MISSING)".formatted(modid, diff.oldVersion())
                           : "%s (version %s -> %s)".formatted(modid, diff.oldVersion(), diff.newVersion())
                     );
               }
            );
         if (mismatchEvent.anyResolved()) {
            resolved.append(System.lineSeparator()).append("Things may not work well.");
            LOGGER.debug(WORLDPERSISTENCE, resolved.toString());
         }

         if (mismatchEvent.anyUnresolved()) {
            unresolved.append(System.lineSeparator()).append("Things may not work well.");
            LOGGER.warn(WORLDPERSISTENCE, unresolved.toString());
         }
      }
   }

   public static String encodeLifecycle(Lifecycle lifecycle) {
      if (lifecycle == Lifecycle.stable()) {
         return "stable";
      } else if (lifecycle == Lifecycle.experimental()) {
         return "experimental";
      } else if (lifecycle instanceof com.mojang.serialization.Lifecycle.Deprecated dep) {
         return "deprecated=" + dep.since();
      } else {
         throw new IllegalArgumentException("Unknown lifecycle.");
      }
   }

   public static Lifecycle parseLifecycle(String lifecycle) {
      if (lifecycle.equals("stable")) {
         return Lifecycle.stable();
      } else if (lifecycle.equals("experimental")) {
         return Lifecycle.experimental();
      } else if (lifecycle.startsWith("deprecated=")) {
         return Lifecycle.deprecated(Integer.parseInt(lifecycle.substring(lifecycle.indexOf(61) + 1)));
      } else {
         throw new IllegalArgumentException("Unknown lifecycle.");
      }
   }

   public static void saveMobEffect(CompoundTag nbt, String key, MobEffect effect) {
      ResourceLocation registryName = BuiltInRegistries.MOB_EFFECT.getKey(effect);
      if (registryName != null) {
         nbt.putString(key, registryName.toString());
      }
   }

   @Nullable
   public static MobEffect loadMobEffect(CompoundTag nbt, String key, @Nullable MobEffect fallback) {
      String registryName = nbt.getString(key);
      if (Strings.isNullOrEmpty(registryName)) {
         return fallback;
      } else {
         try {
            return BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.parse(registryName));
         } catch (ResourceLocationException var5) {
            return fallback;
         }
      }
   }

   public static boolean shouldSuppressEnderManAnger(EnderMan enderMan, Player player, ItemStack mask) {
      return mask.isEnderMask(player, enderMan) || ((EnderManAngerEvent)NeoForge.EVENT_BUS.post(new EnderManAngerEvent(enderMan, player))).isCanceled();
   }

   @Nullable
   public static StructuresBecomeConfiguredFix.Conversion getStructureConversion(String originalBiome) {
      return FORGE_CONVERSION_MAP.get().get(originalBiome);
   }

   public static boolean checkStructureNamespace(String biome) {
      ResourceLocation biomeLocation = ResourceLocation.tryParse(biome);
      return biomeLocation != null && !biomeLocation.getNamespace().equals("minecraft");
   }

   public static String prefixNamespace(ResourceLocation registryKey) {
      return registryKey.getNamespace().equals("minecraft") ? registryKey.getPath() : registryKey.getNamespace() + "/" + registryKey.getPath();
   }

   public static boolean canUseEntitySelectors(SharedSuggestionProvider provider) {
      if (EntitySelectorParser.allowSelectors(provider)) {
         return true;
      } else {
         return provider instanceof CommandSourceStack source && source.source instanceof ServerPlayer player
            ? PermissionAPI.getPermission(player, NeoForgeMod.USE_SELECTORS_PERMISSION)
            : false;
      }
   }

   @Internal
   public static <T> HolderLookup.RegistryLookup<T> wrapRegistryLookup(final HolderLookup.RegistryLookup<T> lookup) {
      return new HolderLookup.RegistryLookup.Delegate<T>() {
         @Override
         public HolderLookup.RegistryLookup<T> parent() {
            return lookup;
         }

         @Override
         public Stream<HolderSet.Named<T>> listTags() {
            return Stream.empty();
         }

         @Override
         public Optional<HolderSet.Named<T>> get(TagKey<T> key) {
            return Optional.of(HolderSet.emptyNamed(lookup, key));
         }
      };
   }

   public static void onLivingBreathe(LivingEntity entity, int consumeAirAmount, int refillAirAmount) {
      boolean isAir = entity.getEyeInFluidType().isAir()
         || entity.level().getBlockState(BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ())).is(Blocks.BUBBLE_COLUMN);
      boolean canBreathe = isAir;
      if (!isAir
         && (
            MobEffectUtil.hasWaterBreathing(entity)
               || !entity.canDrownInFluidType(entity.getEyeInFluidType())
               || entity instanceof Player player && player.getAbilities().invulnerable
         )) {
         canBreathe = true;
         refillAirAmount = 0;
      }

      LivingBreatheEvent breatheEvent = new LivingBreatheEvent(entity, canBreathe, consumeAirAmount, refillAirAmount);
      NeoForge.EVENT_BUS.post(breatheEvent);
      if (breatheEvent.canBreathe()) {
         entity.setAirSupply(Math.min(entity.getAirSupply() + breatheEvent.getRefillAirAmount(), entity.getMaxAirSupply()));
      } else {
         entity.setAirSupply(entity.getAirSupply() - breatheEvent.getConsumeAirAmount());
      }

      if (entity.getAirSupply() <= 0) {
         LivingDrownEvent drownEvent = new LivingDrownEvent(entity);
         if (!((LivingDrownEvent)NeoForge.EVENT_BUS.post(drownEvent)).isCanceled() && drownEvent.isDrowning()) {
            entity.setAirSupply(0);
            Vec3 vec3 = entity.getDeltaMovement();

            for (int i = 0; i < drownEvent.getBubbleCount(); i++) {
               double d2 = entity.getRandom().nextDouble() - entity.getRandom().nextDouble();
               double d3 = entity.getRandom().nextDouble() - entity.getRandom().nextDouble();
               double d4 = entity.getRandom().nextDouble() - entity.getRandom().nextDouble();
               entity.level().addParticle(ParticleTypes.BUBBLE, entity.getX() + d2, entity.getY() + d3, entity.getZ() + d4, vec3.x, vec3.y, vec3.z);
            }

            if (drownEvent.getDamageAmount() > 0.0F) {
               entity.hurt(entity.damageSources().drown(), drownEvent.getDamageAmount());
            }
         }
      }

      if (!isAir
         && !entity.level().isClientSide
         && entity.isPassenger()
         && entity.getVehicle() != null
         && !entity.getVehicle().canBeRiddenUnderFluidType(entity.getEyeInFluidType(), entity)) {
         entity.stopRiding();
      }
   }

   public static void markComponentClassAsValid(Class<?> clazz) {
      if (clazz.isRecord() || clazz.isEnum()) {
         throw new IllegalArgumentException("Records and enums are always valid components");
      } else if (overridesEqualsAndHashCode(clazz)) {
         throw new IllegalArgumentException("Class " + clazz + " already overrides equals and hashCode");
      } else {
         checkedComponentClasses.add(clazz);
      }
   }

   @Internal
   public static void validateComponent(@Nullable Object dataComponent) {
      if (SharedConstants.IS_RUNNING_IN_IDE && dataComponent != null) {
         Class<?> clazz = dataComponent.getClass();
         if (!checkedComponentClasses.contains(clazz)) {
            if (clazz.isRecord() || clazz.isEnum()) {
               checkedComponentClasses.add(clazz);
            } else if (overridesEqualsAndHashCode(clazz)) {
               checkedComponentClasses.add(clazz);
            } else if (isPotentialRegistryObject(dataComponent)) {
               checkedComponentClasses.add(clazz);
            } else {
               throw new IllegalArgumentException(
                  "Data components must implement equals and hashCode. Keep in mind they must also be immutable. Problematic class: " + clazz
               );
            }
         }
      }
   }

   private static boolean isPotentialRegistryObject(Object value) {
      for (Registry registry : BuiltInRegistries.REGISTRY) {
         if (registry.containsValue(value)) {
            return true;
         }
      }

      return false;
   }

   private static boolean overridesEqualsAndHashCode(Class<?> clazz) {
      try {
         Method equals = clazz.getMethod("equals", Object.class);
         Method hashCode = clazz.getMethod("hashCode");
         return equals.getDeclaringClass() != Object.class && hashCode.getDeclaringClass() != Object.class;
      } catch (ReflectiveOperationException var3) {
         throw new RuntimeException("Failed to check for component equals and hashCode", var3);
      }
   }

   public static void onChunkUnload(PoiManager poiManager, ChunkAccess chunkAccess) {
      ChunkPos chunkPos = chunkAccess.getPos();
      poiManager.flush(chunkPos);
      int SectionPosMinY = SectionPos.blockToSectionCoord(chunkAccess.getMinBuildHeight());

      for (int currentSectionY = 0; currentSectionY < chunkAccess.getSectionsCount(); currentSectionY++) {
         long sectionPosKey = SectionPos.asLong(chunkPos.x, SectionPosMinY + currentSectionY, chunkPos.z);
         poiManager.remove(sectionPosKey);
      }
   }

   @Deprecated(
      forRemoval = true
   )
   public static boolean canMobEffectBeApplied(LivingEntity entity, MobEffectInstance effect) {
      return canMobEffectBeApplied(entity, effect, null);
   }

   public static boolean canMobEffectBeApplied(LivingEntity entity, MobEffectInstance effect, @Nullable Entity source) {
      MobEffectEvent.Applicable event = new MobEffectEvent.Applicable(entity, effect, source);
      return ((MobEffectEvent.Applicable)NeoForge.EVENT_BUS.post(event)).getApplicationResult();
   }

   @Nullable
   public static <T> HolderLookup.RegistryLookup<T> resolveLookup(ResourceKey<? extends Registry<T>> key) {
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      if (server != null) {
         return server.registryAccess().lookup(key).orElse(null);
      } else {
         return FMLEnvironment.dist.isClient() ? ClientHooks.resolveLookup(key) : null;
      }
   }

   public static HolderLookup.Provider extractLookupProvider(RegistryOps<?> ops) {
      if (ops.lookupProvider instanceof RegistryOps.HolderLookupAdapter hla) {
         return hla.lookupProvider;
      } else {
         throw new IllegalArgumentException("Registry ops has lookup provider " + ops.lookupProvider + " which is not a HolderLookupAdapter");
      }
   }

   public static UseOnContext dispenseUseOnContext(BlockSource source, ItemStack stack) {
      Direction facing = source.state().getValue(DispenserBlock.FACING);
      BlockPos pos = source.pos().relative(facing);
      Direction blockFace = facing.getOpposite();
      BlockHitResult hitResult = new BlockHitResult(
         new Vec3(pos.getX() + 0.5 + blockFace.getStepX() * 0.5, pos.getY() + 0.5 + blockFace.getStepY() * 0.5, pos.getZ() + 0.5 + blockFace.getStepZ() * 0.5),
         blockFace,
         pos,
         false
      );
      return new UseOnContext(source.level(), null, InteractionHand.MAIN_HAND, stack, hitResult);
   }

   public static boolean tryDispenseShearsHarvestBlock(BlockSource source, ItemStack stack, ServerLevel level, BlockPos pos) {
      BlockState blockstate = source.state().getToolModifiedState(dispenseUseOnContext(source, stack), ItemAbilities.SHEARS_HARVEST, false);
      if (blockstate == null) {
         return false;
      } else {
         level.setBlock(pos, blockstate, 3);
         level.gameEvent(null, GameEvent.SHEAR, pos);
         return true;
      }
   }

   public static Map<RecipeBookType, Pair<String, String>> buildRecipeBookTypeTagFields(Map<RecipeBookType, Pair<String, String>> vanillaMap) {
      ExtensionInfo extInfo = RecipeBookType.getExtensionInfo();
      if (extInfo.extended()) {
         Map<RecipeBookType, Pair<String, String>> var7 = new HashMap<>(vanillaMap);

         for (RecipeBookType type : RecipeBookType.values()) {
            if (type.ordinal() >= extInfo.vanillaCount()) {
               String name = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, type.name());
               var7.put(type, Pair.of("is" + name + "GuiOpen", "is" + name + "FilteringCraftable"));
            }
         }

         vanillaMap = Map.copyOf(var7);
      }

      return vanillaMap;
   }

   public static RecipeBookType[] getFilteredRecipeBookTypeValues() {
      return FMLEnvironment.dist.isClient() ? ClientHooks.getFilteredRecipeBookTypeValues() : RecipeBookType.values();
   }

   static {
      markComponentClassAsValid(BlockState.class);
      markComponentClassAsValid(FluidState.class);
      markComponentClassAsValid(ResourceKey.class);
   }

   @FunctionalInterface
   public interface BiomeCallbackFunction {
      Biome apply(Biome.ClimateSettings var1, BiomeSpecialEffects var2, BiomeGenerationSettings var3, MobSpawnSettings var4);
   }
}
