package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

public class ClientboundLevelChunkPacketData {
   private static final int TWO_MEGABYTES = 2097152;
   private final CompoundTag heightmaps;
   private final byte[] buffer;
   private final List<ClientboundLevelChunkPacketData.BlockEntityInfo> blockEntitiesData;

   public ClientboundLevelChunkPacketData(LevelChunk p_195651_) {
      this.heightmaps = new CompoundTag();

      for (Entry<Heightmap.Types, Heightmap> $$1 : p_195651_.getHeightmaps()) {
         if ($$1.getKey().sendToClient()) {
            this.heightmaps.put($$1.getKey().getSerializationKey(), new LongArrayTag($$1.getValue().getRawData()));
         }
      }

      this.buffer = new byte[calculateChunkSize(p_195651_)];
      extractChunkData(new FriendlyByteBuf(this.getWriteBuffer()), p_195651_);
      this.blockEntitiesData = Lists.newArrayList();

      for (Entry<BlockPos, BlockEntity> $$2 : p_195651_.getBlockEntities().entrySet()) {
         this.blockEntitiesData.add(ClientboundLevelChunkPacketData.BlockEntityInfo.create($$2.getValue()));
      }
   }

   public ClientboundLevelChunkPacketData(RegistryFriendlyByteBuf p_320343_, int p_195654_, int p_195655_) {
      this.heightmaps = p_320343_.readNbt();
      if (this.heightmaps == null) {
         throw new RuntimeException("Can't read heightmap in packet for [" + p_195654_ + ", " + p_195655_ + "]");
      } else {
         int $$3 = p_320343_.readVarInt();
         if ($$3 > 2097152) {
            throw new RuntimeException("Chunk Packet trying to allocate too much memory on read.");
         } else {
            this.buffer = new byte[$$3];
            p_320343_.readBytes(this.buffer);
            this.blockEntitiesData = ClientboundLevelChunkPacketData.BlockEntityInfo.LIST_STREAM_CODEC.decode(p_320343_);
         }
      }
   }

   public void write(RegistryFriendlyByteBuf p_320863_) {
      p_320863_.writeNbt(this.heightmaps);
      p_320863_.writeVarInt(this.buffer.length);
      p_320863_.writeBytes(this.buffer);
      ClientboundLevelChunkPacketData.BlockEntityInfo.LIST_STREAM_CODEC.encode(p_320863_, this.blockEntitiesData);
   }

   private static int calculateChunkSize(LevelChunk p_195665_) {
      int $$1 = 0;

      for (LevelChunkSection $$2 : p_195665_.getSections()) {
         $$1 += $$2.getSerializedSize();
      }

      return $$1;
   }

   private ByteBuf getWriteBuffer() {
      ByteBuf $$0 = Unpooled.wrappedBuffer(this.buffer);
      $$0.writerIndex(0);
      return $$0;
   }

   public static void extractChunkData(FriendlyByteBuf p_195669_, LevelChunk p_195670_) {
      for (LevelChunkSection $$2 : p_195670_.getSections()) {
         $$2.write(p_195669_);
      }
   }

   public Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> getBlockEntitiesTagsConsumer(int p_195658_, int p_195659_) {
      return p_195663_ -> this.getBlockEntitiesTags(p_195663_, p_195658_, p_195659_);
   }

   private void getBlockEntitiesTags(ClientboundLevelChunkPacketData.BlockEntityTagOutput p_195675_, int p_195676_, int p_195677_) {
      int $$3 = 16 * p_195676_;
      int $$4 = 16 * p_195677_;
      BlockPos.MutableBlockPos $$5 = new BlockPos.MutableBlockPos();

      for (ClientboundLevelChunkPacketData.BlockEntityInfo $$6 : this.blockEntitiesData) {
         int $$7 = $$3 + SectionPos.sectionRelative($$6.packedXZ >> 4);
         int $$8 = $$4 + SectionPos.sectionRelative($$6.packedXZ);
         $$5.set($$7, $$6.y, $$8);
         p_195675_.accept($$5, $$6.type, $$6.tag);
      }
   }

   public FriendlyByteBuf getReadBuffer() {
      return new FriendlyByteBuf(Unpooled.wrappedBuffer(this.buffer));
   }

   public CompoundTag getHeightmaps() {
      return this.heightmaps;
   }

   static class BlockEntityInfo {
      public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundLevelChunkPacketData.BlockEntityInfo> STREAM_CODEC = StreamCodec.ofMember(
         ClientboundLevelChunkPacketData.BlockEntityInfo::write, ClientboundLevelChunkPacketData.BlockEntityInfo::new
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, List<ClientboundLevelChunkPacketData.BlockEntityInfo>> LIST_STREAM_CODEC = STREAM_CODEC.apply(
         ByteBufCodecs.list()
      );
      final int packedXZ;
      final int y;
      final BlockEntityType<?> type;
      @Nullable
      final CompoundTag tag;

      private BlockEntityInfo(int p_195685_, int p_195686_, BlockEntityType<?> p_195687_, @Nullable CompoundTag p_195688_) {
         this.packedXZ = p_195685_;
         this.y = p_195686_;
         this.type = p_195687_;
         this.tag = p_195688_;
      }

      private BlockEntityInfo(RegistryFriendlyByteBuf p_319780_) {
         this.packedXZ = p_319780_.readByte();
         this.y = p_319780_.readShort();
         this.type = ByteBufCodecs.registry(Registries.BLOCK_ENTITY_TYPE).decode(p_319780_);
         this.tag = p_319780_.readNbt();
      }

      private void write(RegistryFriendlyByteBuf p_320914_) {
         p_320914_.writeByte(this.packedXZ);
         p_320914_.writeShort(this.y);
         ByteBufCodecs.registry(Registries.BLOCK_ENTITY_TYPE).encode(p_320914_, this.type);
         p_320914_.writeNbt(this.tag);
      }

      static ClientboundLevelChunkPacketData.BlockEntityInfo create(BlockEntity p_195692_) {
         CompoundTag $$1 = p_195692_.getUpdateTag(p_195692_.getLevel().registryAccess());
         BlockPos $$2 = p_195692_.getBlockPos();
         int $$3 = SectionPos.sectionRelative($$2.getX()) << 4 | SectionPos.sectionRelative($$2.getZ());
         return new ClientboundLevelChunkPacketData.BlockEntityInfo($$3, $$2.getY(), p_195692_.getType(), $$1.isEmpty() ? null : $$1);
      }
   }

   @FunctionalInterface
   public interface BlockEntityTagOutput {
      void accept(BlockPos var1, BlockEntityType<?> var2, @Nullable CompoundTag var3);
   }
}
