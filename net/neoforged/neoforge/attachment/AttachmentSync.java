package net.neoforged.neoforge.attachment;

import io.netty.buffer.Unpooled;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FriendlyByteBufUtil;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegistryBuilder;
import net.neoforged.neoforge.registries.callback.AddCallback;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

@EventBusSubscriber(
   modid = "neoforge"
)
@Internal
public final class AttachmentSync {
   public static final Registry<AttachmentType<?>> SYNCED_ATTACHMENT_TYPES = new RegistryBuilder<>(
         ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("neoforge", "synced_attachment_types"))
      )
      .sync(true)
      .callback(
         (AddCallback<AttachmentType<?>>)(registry, id, key, value) -> {
            if (!NeoForgeRegistries.ATTACHMENT_TYPES.containsKey(key.location())
               || !NeoForgeRegistries.ATTACHMENT_TYPES.containsValue(value)
               || NeoForgeRegistries.ATTACHMENT_TYPES.get(key.location()) != value) {
               throw new IllegalStateException("Cannot add entries to the SYNCED_ATTACHMENT_TYPES registry directly.");
            }
         }
      )
      .create();
   public static final AddCallback<AttachmentType<?>> ATTACHMENT_TYPE_ADD_CALLBACK = (registry, id, key, value) -> {
      if (value.syncHandler != null) {
         Registry.register(SYNCED_ATTACHMENT_TYPES, key.location(), value);
      }
   };

   private static SyncAttachmentsPayload.Target syncTarget(AttachmentHolder holder) {
      Objects.requireNonNull(holder);
      AttachmentHolder var1 = holder;
      byte var2 = 0;

      while (true) {
         Object var10000;
         switch (SwitchBootstraps.typeSwitch<"typeSwitch",BlockEntity,AttachmentHolder.AsField,Entity,Level>(var1, var2)) {
            case 0:
               BlockEntity blockEntity = (BlockEntity)var1;
               var10000 = new SyncAttachmentsPayload.BlockEntityTarget(blockEntity.getBlockPos());
               break;
            case 1:
               AttachmentHolder.AsField asField = (AttachmentHolder.AsField)var1;
               if (!(asField.getExposedHolder() instanceof LevelChunk chunk)) {
                  var2 = 2;
                  continue;
               }

               var10000 = new SyncAttachmentsPayload.ChunkTarget(chunk.getPos());
               break;
            case 2:
               Entity entity = (Entity)var1;
               var10000 = new SyncAttachmentsPayload.EntityTarget(entity.getId());
               break;
            case 3:
               Level ignored = (Level)var1;
               var10000 = new SyncAttachmentsPayload.LevelTarget();
               break;
            default:
               throw new UnsupportedOperationException("Attachment holder class is not supported: " + holder);
         }

         return (SyncAttachmentsPayload.Target)var10000;
      }
   }

   private static <T> void syncUpdate(AttachmentHolder holder, AttachmentType<T> type, List<ServerPlayer> players) {
      RegistryAccess registryAccess = null;

      for (ServerPlayer player : players) {
         if (type.syncHandler.sendToPlayer(holder.getExposedHolder(), player)) {
            registryAccess = player.registryAccess();
            break;
         }
      }

      if (registryAccess != null) {
         byte[] data = FriendlyByteBufUtil.writeCustomData(buf -> {
            T existingData = holder.getExistingDataOrNull(type);
            if (existingData != null) {
               buf.writeBoolean(true);
               type.syncHandler.write(buf, holder.getData(type), false);
            } else {
               buf.writeBoolean(false);
            }
         }, registryAccess);
         ClientboundCustomPayloadPacket packet = new SyncAttachmentsPayload(syncTarget(holder), List.of(type), data).toVanillaClientbound();

         for (ServerPlayer playerx : players) {
            if (type.syncHandler.sendToPlayer(holder.getExposedHolder(), playerx)) {
               playerx.connection.send(packet);
            }
         }
      }
   }

   public static void syncBlockEntityUpdate(BlockEntity blockEntity, AttachmentType<?> type) {
      if (type.syncHandler != null && blockEntity.getLevel() instanceof ServerLevel serverLevel) {
         syncUpdate(blockEntity, type, serverLevel.getChunkSource().chunkMap.getPlayers(new ChunkPos(blockEntity.getBlockPos()), false));
      }
   }

   public static void syncChunkUpdate(LevelChunk chunk, AttachmentHolder.AsField holder, AttachmentType<?> type) {
      if (type.syncHandler != null && chunk.getLevel() instanceof ServerLevel serverLevel) {
         syncUpdate(holder, type, serverLevel.getChunkSource().chunkMap.getPlayers(chunk.getPos(), false));
      }
   }

   public static void syncEntityUpdate(Entity entity, AttachmentType<?> type) {
      if (type.syncHandler != null && entity.level() instanceof ServerLevel serverLevel) {
         Object var6 = serverLevel.getChunkSource().chunkMap.getPlayersWatching(entity);
         if (entity instanceof ServerPlayer serverPlayer) {
            ArrayList<ServerPlayer> newPlayers = new ArrayList<>(var6.size() + 1);
            newPlayers.addAll((Collection<? extends ServerPlayer>)var6);
            newPlayers.add(serverPlayer);
            var6 = newPlayers;
         }

         syncUpdate(entity, type, (List<ServerPlayer>)var6);
      }
   }

   public static void syncLevelUpdate(ServerLevel level, AttachmentType<?> type) {
      if (type.syncHandler != null) {
         syncUpdate(level, type, level.players());
      }
   }

   @Nullable
   private static SyncAttachmentsPayload syncInitialAttachments(AttachmentHolder holder, ServerPlayer to) {
      if (holder.attachments == null) {
         return null;
      } else {
         boolean anySyncableAttachment = false;

         for (AttachmentType<?> attachment : holder.attachments.keySet()) {
            anySyncableAttachment |= attachment.syncHandler != null;
         }

         if (!anySyncableAttachment) {
            return null;
         } else {
            List<AttachmentType<?>> syncedTypes = new ArrayList<>();
            byte[] data = FriendlyByteBufUtil.writeCustomData(buf -> {
               for (Entry<AttachmentType<?>, Object> entry : holder.attachments.entrySet()) {
                  AttachmentType<?> type = entry.getKey();
                  AttachmentSyncHandler<Object> syncHandler = (AttachmentSyncHandler<Object>)type.syncHandler;
                  if (syncHandler != null) {
                     int indexBefore = buf.writerIndex();
                     buf.writeBoolean(true);
                     int indexBetween = buf.writerIndex();
                     syncHandler.write(buf, entry.getValue(), true);
                     if (indexBetween < buf.writerIndex()) {
                        syncedTypes.add(type);
                     } else {
                        buf.writerIndex(indexBefore);
                     }
                  }
               }
            }, to.registryAccess());
            return new SyncAttachmentsPayload(syncTarget(holder), syncedTypes, data);
         }
      }
   }

   @SubscribeEvent
   public static void onChunkSent(ChunkWatchEvent.Sent event) {
      List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
      SyncAttachmentsPayload chunkPayload = syncInitialAttachments(event.getChunk().getAttachmentHolder(), event.getPlayer());
      if (chunkPayload != null) {
         packets.add(chunkPayload.toVanillaClientbound());
      }

      for (BlockEntity blockEntity : event.getChunk().getBlockEntities().values()) {
         SyncAttachmentsPayload blockEntityPayload = syncInitialAttachments(blockEntity, event.getPlayer());
         if (blockEntityPayload != null) {
            packets.add(blockEntityPayload.toVanillaClientbound());
         }
      }

      if (!packets.isEmpty()) {
         event.getPlayer().connection.send(new ClientboundBundlePacket(packets));
      }
   }

   public static void syncInitialEntityAttachments(Entity entity, ServerPlayer to, Consumer<Packet<? super ClientGamePacketListener>> packetConsumer) {
      SyncAttachmentsPayload packet = syncInitialAttachments(entity, to);
      if (packet != null) {
         packetConsumer.accept(packet.toVanillaClientbound());
      }
   }

   public static void syncInitialPlayerAttachments(ServerPlayer player) {
      SyncAttachmentsPayload packet = syncInitialAttachments(player, player);
      if (packet != null) {
         player.connection.send(packet.toVanillaClientbound());
      }
   }

   public static void syncInitialLevelAttachments(ServerLevel level, ServerPlayer to) {
      SyncAttachmentsPayload packet = syncInitialAttachments(level, to);
      if (packet != null) {
         to.connection.send(packet.toVanillaClientbound());
      }
   }

   public static void receiveSyncedDataAttachments(AttachmentHolder holder, RegistryAccess registryAccess, List<AttachmentType<?>> types, byte[] bytes) {
      RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes), registryAccess, ConnectionType.NEOFORGE);

      try {
         for (AttachmentType<?> type : types) {
            AttachmentSyncHandler<Object> syncHandler = (AttachmentSyncHandler<Object>)type.syncHandler;
            if (syncHandler == null) {
               throw new IllegalArgumentException(
                  "Received synced attachment type without a sync handler registered: " + NeoForgeRegistries.ATTACHMENT_TYPES.getKey(type)
               );
            }

            Object previousValue = holder.attachments == null ? null : holder.attachments.get(type);
            boolean hasAttachment = buf.readBoolean();
            Object result = hasAttachment ? syncHandler.read(holder.getExposedHolder(), buf, previousValue) : null;
            if (result == null) {
               if (holder.attachments != null) {
                  holder.attachments.remove(type);
               }
            } else {
               holder.getAttachmentMap().put(type, result);
            }
         }
      } catch (Exception var14) {
         throw new RuntimeException("Encountered exception when reading synced data attachments: " + types, var14);
      } finally {
         buf.release();
      }
   }

   private AttachmentSync() {
   }
}
