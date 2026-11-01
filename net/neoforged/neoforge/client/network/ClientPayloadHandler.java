package net.neoforged.neoforge.client.network;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.netty.buffer.Unpooled;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentSync;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.registries.ClientRegistryManager;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.LevelChunkAuxiliaryLightManager;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.network.ConfigSync;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.payload.AdvancedAddEntityPayload;
import net.neoforged.neoforge.network.payload.AdvancedContainerSetDataPayload;
import net.neoforged.neoforge.network.payload.AdvancedOpenScreenPayload;
import net.neoforged.neoforge.network.payload.AuxiliaryLightDataPayload;
import net.neoforged.neoforge.network.payload.ClientboundCustomSetTimePayload;
import net.neoforged.neoforge.network.payload.ConfigFilePayload;
import net.neoforged.neoforge.network.payload.FrozenRegistryPayload;
import net.neoforged.neoforge.network.payload.FrozenRegistrySyncCompletedPayload;
import net.neoforged.neoforge.network.payload.FrozenRegistrySyncStartPayload;
import net.neoforged.neoforge.network.payload.KnownRegistryDataMapsPayload;
import net.neoforged.neoforge.network.payload.RecipeContentPayload;
import net.neoforged.neoforge.network.payload.RegistryDataMapSyncPayload;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.registries.RegistryManager;
import net.neoforged.neoforge.registries.RegistrySnapshot;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(
   modid = "neoforge",
   value = {Dist.CLIENT}
)
@Internal
final class ClientPayloadHandler {
   private static final Logger LOGGER = LoggerFactory.getLogger(ClientPayloadHandler.class);
   private static final Set<Identifier> toSynchronize = Sets.newConcurrentHashSet();
   private static final Map<Identifier, RegistrySnapshot> synchronizedRegistries = Maps.newConcurrentMap();

   private ClientPayloadHandler() {
   }

   @SubscribeEvent
   private static void register(RegisterClientPayloadHandlersEvent event) {
      event.register(ConfigFilePayload.TYPE, ClientPayloadHandler::handle);
      event.register(FrozenRegistrySyncStartPayload.TYPE, ClientPayloadHandler::handle);
      event.register(FrozenRegistryPayload.TYPE, ClientPayloadHandler::handle);
      event.register(FrozenRegistrySyncCompletedPayload.TYPE, ClientPayloadHandler::handle);
      event.register(KnownRegistryDataMapsPayload.TYPE, ClientRegistryManager::handleKnownDataMaps);
      event.register(AdvancedAddEntityPayload.TYPE, ClientPayloadHandler::handle);
      event.register(AdvancedOpenScreenPayload.TYPE, ClientPayloadHandler::handle);
      event.register(AuxiliaryLightDataPayload.TYPE, ClientPayloadHandler::handle);
      event.register(RegistryDataMapSyncPayload.TYPE, ClientRegistryManager::handleDataMapSync);
      event.register(AdvancedContainerSetDataPayload.TYPE, ClientPayloadHandler::handle);
      event.register(ClientboundCustomSetTimePayload.TYPE, ClientPayloadHandler::handle);
      event.register(RecipeContentPayload.TYPE, ClientPayloadHandler::handle);
      event.register(SyncAttachmentsPayload.TYPE, ClientPayloadHandler::handle);
   }

   private static void handle(FrozenRegistryPayload payload, IPayloadContext context) {
      synchronizedRegistries.put(payload.registryName(), payload.snapshot());
      toSynchronize.remove(payload.registryName());
   }

   private static void handle(FrozenRegistrySyncStartPayload payload, IPayloadContext context) {
      toSynchronize.addAll(payload.toAccess());
      synchronizedRegistries.clear();
   }

   private static void handle(FrozenRegistrySyncCompletedPayload payload, IPayloadContext context) {
      if (!toSynchronize.isEmpty()) {
         context.disconnect(
            Component.translatable("neoforge.network.registries.sync.missing", toSynchronize.stream().map(Object::toString).collect(Collectors.joining(", ")))
         );
      } else {
         try {
            Set<ResourceKey<?>> keysUnknownToClient = RegistryManager.applySnapshot(synchronizedRegistries, false);
            if (!keysUnknownToClient.isEmpty()) {
               context.disconnect(
                  Component.translatable(
                     "neoforge.network.registries.sync.server-with-unknown-keys",
                     keysUnknownToClient.stream().map(Object::toString).collect(Collectors.joining(", "))
                  )
               );
               return;
            }

            toSynchronize.clear();
            synchronizedRegistries.clear();
            context.reply(FrozenRegistrySyncCompletedPayload.INSTANCE);
         } catch (Throwable var3) {
            LOGGER.error("Failed to handle registry sync from server.", var3);
            context.disconnect(Component.translatable("neoforge.network.registries.sync.failed", var3.toString()));
         }
      }
   }

   private static void handle(ConfigFilePayload payload, IPayloadContext context) {
      if (!context.connection().isMemoryConnection()) {
         ConfigSync.receiveSyncedConfig(payload.contents(), payload.fileName());
      }
   }

   private static void handle(AdvancedAddEntityPayload advancedAddEntityPayload, IPayloadContext context) {
      try {
         Entity entity = context.player().level().getEntity(advancedAddEntityPayload.entityId());
         if (entity instanceof IEntityWithComplexSpawn entityAdditionalSpawnData) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
               Unpooled.wrappedBuffer(advancedAddEntityPayload.customPayload()), entity.registryAccess(), context.listener().getConnectionType()
            );

            try {
               entityAdditionalSpawnData.readSpawnData(buf);
            } finally {
               buf.release();
            }
         }
      } catch (Throwable var9) {
         LOGGER.error("Failed to handle advanced add entity from server.", var9);
         context.disconnect(Component.translatable("neoforge.network.advanced_add_entity.failed", var9.toString()));
      }
   }

   private static void handle(AdvancedOpenScreenPayload msg, IPayloadContext context) {
      Minecraft mc = Minecraft.getInstance();
      RegistryAccess registryAccess = mc.player.registryAccess();
      RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
         Unpooled.wrappedBuffer(msg.additionalData()), registryAccess, context.listener().getConnectionType()
      );

      try {
         createMenuScreen(msg.name(), msg.menuType(), msg.windowId(), buf);
      } catch (Throwable var9) {
         LOGGER.error("Failed to handle advanced open screen from server.", var9);
         context.disconnect(Component.translatable("neoforge.network.advanced_open_screen.failed", var9.toString()));
      } finally {
         buf.release();
      }
   }

   private static <T extends AbstractContainerMenu> void createMenuScreen(Component name, MenuType<T> menuType, int windowId, RegistryFriendlyByteBuf buf) {
      Minecraft mc = Minecraft.getInstance();
      MenuScreens.getScreenFactory(menuType).ifPresent(f -> {
         Screen s = f.create(menuType.create(windowId, mc.player.getInventory(), buf), mc.player.getInventory(), name);
         mc.player.containerMenu = ((MenuAccess)s).getMenu();
         mc.setScreen(s);
      });
   }

   private static void handle(AuxiliaryLightDataPayload msg, IPayloadContext context) {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level == null) {
            return;
         }

         if (mc.level.getAuxLightManager(msg.pos()) instanceof LevelChunkAuxiliaryLightManager manager) {
            manager.handleLightDataSync(msg.entries());
         }
      } catch (Throwable var5) {
         LOGGER.error("Failed to handle auxiliary light data from server.", var5);
         context.disconnect(Component.translatable("neoforge.network.aux_light_data.failed", msg.pos().toString(), var5.toString()));
      }
   }

   private static void handle(AdvancedContainerSetDataPayload msg, IPayloadContext context) {
      context.handle(msg.toVanillaPacket());
   }

   private static void handle(ClientboundCustomSetTimePayload payload, IPayloadContext context) {
      ClientLevel level = Minecraft.getInstance().level;
      level.setTimeFromServer(payload.gameTime(), payload.dayTime(), payload.gameRule());
      level.setDayTimeFraction(payload.dayTimeFraction());
      level.setDayTimePerTick(payload.dayTimePerTick());
   }

   private static void handle(RecipeContentPayload payload, IPayloadContext context) {
      RecipeMap recipeMap = RecipeMap.create(payload.recipes());
      NeoForge.EVENT_BUS.post(new RecipesReceivedEvent(payload.recipeTypes(), recipeMap));
   }

   private static void handle(SyncAttachmentsPayload payload, IPayloadContext context) {
      switch (payload.target()) {
         case SyncAttachmentsPayload.BlockEntityTarget(BlockPos var25):
            BlockPos var14 = var25;
            BlockEntity blockEntity = context.player().level().getBlockEntity(var14);
            if (blockEntity == null) {
               LOGGER.warn("Received synced attachments from unknown block entity");
            } else {
               AttachmentSync.receiveSyncedDataAttachments(blockEntity, context.player().registryAccess(), payload.types(), payload.syncPayload());
            }
            break;
         case SyncAttachmentsPayload.ChunkTarget(ChunkPos var23):
            ChunkPos var16 = var23;
            ChunkAccess chunk = context.player().level().getChunk(var16.x, var16.z, ChunkStatus.FULL, false);
            if (chunk == null) {
               LOGGER.warn("Received synced attachments from unknown chunk");
            } else {
               AttachmentSync.receiveSyncedDataAttachments(
                  chunk.getAttachmentHolder(), chunk.getLevel().registryAccess(), payload.types(), payload.syncPayload()
               );
            }
            break;
         case SyncAttachmentsPayload.EntityTarget(int var21):
            int var18 = var21;
            Entity entityx = context.player().level().getEntity(var18);
            if (entityx == null) {
               LOGGER.warn("Received synced attachments from unknown entity");
            } else {
               AttachmentSync.receiveSyncedDataAttachments(entityx, entityx.registryAccess(), payload.types(), payload.syncPayload());
            }
            break;
         case SyncAttachmentsPayload.LevelTarget entity:
            AttachmentSync.receiveSyncedDataAttachments(context.player().level(), context.player().registryAccess(), payload.types(), payload.syncPayload());
            break;
         default:
            throw new MatchException(null, null);
      }
   }
}
