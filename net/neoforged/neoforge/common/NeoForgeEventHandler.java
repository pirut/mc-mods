package net.neoforged.neoforge.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.loot.LootModifierManager;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.common.util.LogicalSidedProvider;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.payload.RegistryDataMapSyncPayload;
import net.neoforged.neoforge.registries.DataMapLoader;
import net.neoforged.neoforge.registries.DataPackRegistriesHooks;
import net.neoforged.neoforge.registries.RegistryManager;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.server.command.ConfigCommand;
import net.neoforged.neoforge.server.command.NeoForgeCommand;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class NeoForgeEventHandler {
   private static LootModifierManager INSTANCE;
   private static DataMapLoader DATA_MAPS;

   @SubscribeEvent(
      priority = EventPriority.HIGH
   )
   public void onEntityJoinWorld(EntityJoinLevelEvent event) {
      Entity entity = event.getEntity();
      if (entity.getClass().equals(ItemEntity.class)) {
         ItemStack stack = ((ItemEntity)entity).getItem();
         Item item = stack.getItem();
         if (item.hasCustomEntity(stack)) {
            Entity newEntity = item.createEntity(event.getLevel(), entity, stack);
            if (newEntity != null) {
               entity.discard();
               event.setCanceled(true);
               BlockableEventLoop<? super TickTask> executor = LogicalSidedProvider.WORKQUEUE
                  .get(event.getLevel().isClientSide ? LogicalSide.CLIENT : LogicalSide.SERVER);
               executor.tell(new TickTask(0, () -> event.getLevel().addFreshEntity(newEntity)));
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public void onDimensionUnload(LevelEvent.Unload event) {
      if (event.getLevel() instanceof ServerLevel) {
         FakePlayerFactory.unloadLevel((ServerLevel)event.getLevel());
      }
   }

   @SubscribeEvent
   public void preServerTick(ServerTickEvent.Pre event) {
      WorldWorkerManager.tick(true);
   }

   @SubscribeEvent
   public void postServerTick(ServerTickEvent.Post event) {
      WorldWorkerManager.tick(false);
   }

   @SubscribeEvent
   public void onChunkUnload(ChunkEvent.Unload event) {
      if (!event.getLevel().isClientSide()) {
         FarmlandWaterManager.removeTickets(event.getChunk());
      }
   }

   @SubscribeEvent
   public void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
      UsernameCache.setUsername(event.getEntity().getUUID(), event.getEntity().getGameProfile().getName());
   }

   @SubscribeEvent
   public void tagsUpdated(TagsUpdatedEvent event) {
      if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
         DATA_MAPS.apply();
      }
   }

   @SubscribeEvent
   public void onDpSync(OnDatapackSyncEvent event) {
      RegistryManager.getDataMaps()
         .forEach(
            (registry, values) -> {
               Optional<Registry<Object>> regOpt = event.getPlayerList()
                  .getServer()
                  .overworld()
                  .registryAccess()
                  .registry((ResourceKey<? extends Registry<? extends Object>>)registry);
               if (!regOpt.isEmpty()) {
                  event.getRelevantPlayers()
                     .forEach(
                        player -> {
                           if (player.connection.hasChannel(RegistryDataMapSyncPayload.TYPE)) {
                              if (!player.connection.getConnection().isMemoryConnection() || DataPackRegistriesHooks.getSyncedRegistry(registry) != null) {
                                 Map<ResourceKey<? extends Registry<?>>, Collection<ResourceLocation>> playerMaps = (Map<ResourceKey<? extends Registry<?>>, Collection<ResourceLocation>>)player.connection
                                    .getConnection()
                                    .channel()
                                    .attr(RegistryManager.ATTRIBUTE_KNOWN_DATA_MAPS)
                                    .get();
                                 if (playerMaps != null) {
                                    this.handleSync(player, regOpt.get(), playerMaps.getOrDefault(registry, List.of()));
                                 }
                              }
                           }
                        }
                     );
               }
            }
         );
   }

   private <T> void handleSync(ServerPlayer player, Registry<T> registry, Collection<ResourceLocation> attachments) {
      if (!attachments.isEmpty()) {
         Map<ResourceLocation, Map<ResourceKey<T>, ?>> att = new HashMap<>();
         attachments.forEach(key -> {
            DataMapType<T, ?> attach = RegistryManager.getDataMap(registry.key(), key);
            if (attach != null && attach.networkCodec() != null) {
               att.put(key, registry.getDataMap(attach));
            }
         });
         if (!att.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new RegistryDataMapSyncPayload<>(registry.key(), att));
         }
      }
   }

   @SubscribeEvent
   public void onCommandsRegister(RegisterCommandsEvent event) {
      NeoForgeCommand.register(event.getDispatcher());
      ConfigCommand.register(event.getDispatcher());
   }

   @SubscribeEvent
   public void onResourceReload(AddReloadListenerEvent event) {
      INSTANCE = new LootModifierManager();
      event.addListener(INSTANCE);
      event.addListener(DATA_MAPS = new DataMapLoader(event.getRegistryAccess()));
   }

   static LootModifierManager getLootModifierManager() {
      if (INSTANCE == null) {
         throw new IllegalStateException("Can not retrieve LootModifierManager until resources have loaded once.");
      } else {
         return INSTANCE;
      }
   }

   @SubscribeEvent
   public void resourceReloadListeners(AddReloadListenerEvent event) {
      event.addListener(CreativeModeTabRegistry.getReloadListener());
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public void builtinMobSpawnBlocker(EntityJoinLevelEvent event) {
      if (event.getEntity() instanceof Mob mob && mob.isSpawnCancelled()) {
         event.setCanceled(true);
      }
   }
}
