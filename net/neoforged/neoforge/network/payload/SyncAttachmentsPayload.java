package net.neoforged.neoforge.network.payload;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.attachment.AttachmentSync;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record SyncAttachmentsPayload(SyncAttachmentsPayload.Target target, List<AttachmentType<?>> types, byte[] syncPayload) implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<SyncAttachmentsPayload> TYPE = new CustomPacketPayload.Type<>(
      Identifier.fromNamespaceAndPath("neoforge", "sync_attachments")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, SyncAttachmentsPayload> STREAM_CODEC = StreamCodec.composite(
      SyncAttachmentsPayload.Target.STREAM_CODEC,
      SyncAttachmentsPayload::target,
      ByteBufCodecs.registry(AttachmentSync.SYNCED_ATTACHMENT_TYPES.key()).apply(ByteBufCodecs.list()),
      SyncAttachmentsPayload::types,
      NeoForgeStreamCodecs.UNBOUNDED_BYTE_ARRAY,
      SyncAttachmentsPayload::syncPayload,
      SyncAttachmentsPayload::new
   );

   @Override
   public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public record BlockEntityTarget(BlockPos pos) implements SyncAttachmentsPayload.Target {
   }

   public record ChunkTarget(ChunkPos pos) implements SyncAttachmentsPayload.Target {
   }

   public record EntityTarget(int entity) implements SyncAttachmentsPayload.Target {
   }

   public record LevelTarget() implements SyncAttachmentsPayload.Target {
   }

   public sealed interface Target
      permits SyncAttachmentsPayload.BlockEntityTarget,
      SyncAttachmentsPayload.ChunkTarget,
      SyncAttachmentsPayload.EntityTarget,
      SyncAttachmentsPayload.LevelTarget {
      StreamCodec<RegistryFriendlyByteBuf, SyncAttachmentsPayload.Target> STREAM_CODEC = StreamCodec.of((buf, target) -> {
         switch (target) {
            case SyncAttachmentsPayload.BlockEntityTarget(BlockPos patt6$temp):
               buf.writeByte(0);
               buf.writeBlockPos(patt6$temp);
               break;
            case SyncAttachmentsPayload.ChunkTarget(ChunkPos patt7$temp):
               buf.writeByte(1);
               buf.writeChunkPos(patt7$temp);
               break;
            case SyncAttachmentsPayload.EntityTarget(int patt8$temp):
               buf.writeByte(2);
               buf.writeVarInt(patt8$temp);
               break;
            case SyncAttachmentsPayload.LevelTarget $b$3:
               buf.writeByte(3);
               break;
            default:
               throw new MatchException(null, null);
         }
      }, buf -> {
         int type = buf.readByte();
         switch (type) {
            case 0:
               return new SyncAttachmentsPayload.BlockEntityTarget(buf.readBlockPos());
            case 1:
               return new SyncAttachmentsPayload.ChunkTarget(buf.readChunkPos());
            case 2:
               return new SyncAttachmentsPayload.EntityTarget(buf.readVarInt());
            case 3:
               return new SyncAttachmentsPayload.LevelTarget();
            default:
               throw new IllegalArgumentException("Unknown target type: " + type);
         }
      });
   }
}
