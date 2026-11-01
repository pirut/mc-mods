package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.shorts.ShortIterator;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class ClientboundSectionBlocksUpdatePacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundSectionBlocksUpdatePacket> STREAM_CODEC = Packet.codec(
      ClientboundSectionBlocksUpdatePacket::write, ClientboundSectionBlocksUpdatePacket::new
   );
   private static final int POS_IN_SECTION_BITS = 12;
   private final SectionPos sectionPos;
   private final short[] positions;
   private final BlockState[] states;

   public ClientboundSectionBlocksUpdatePacket(SectionPos p_284963_, ShortSet p_285027_, LevelChunkSection p_285414_) {
      this.sectionPos = p_284963_;
      int $$3 = p_285027_.size();
      this.positions = new short[$$3];
      this.states = new BlockState[$$3];
      int $$4 = 0;

      for (ShortIterator var6 = p_285027_.iterator(); var6.hasNext(); $$4++) {
         short $$5 = (Short)var6.next();
         this.positions[$$4] = $$5;
         this.states[$$4] = p_285414_.getBlockState(SectionPos.sectionRelativeX($$5), SectionPos.sectionRelativeY($$5), SectionPos.sectionRelativeZ($$5));
      }
   }

   private ClientboundSectionBlocksUpdatePacket(FriendlyByteBuf p_179196_) {
      this.sectionPos = SectionPos.of(p_179196_.readLong());
      int $$1 = p_179196_.readVarInt();
      this.positions = new short[$$1];
      this.states = new BlockState[$$1];

      for (int $$2 = 0; $$2 < $$1; $$2++) {
         long $$3 = p_179196_.readVarLong();
         this.positions[$$2] = (short)($$3 & 4095L);
         this.states[$$2] = Block.BLOCK_STATE_REGISTRY.byId((int)($$3 >>> 12));
      }
   }

   private void write(FriendlyByteBuf p_133002_) {
      p_133002_.writeLong(this.sectionPos.asLong());
      p_133002_.writeVarInt(this.positions.length);

      for (int $$1 = 0; $$1 < this.positions.length; $$1++) {
         p_133002_.writeVarLong((long)Block.getId(this.states[$$1]) << 12 | this.positions[$$1]);
      }
   }

   @Override
   public PacketType<ClientboundSectionBlocksUpdatePacket> type() {
      return GamePacketTypes.CLIENTBOUND_SECTION_BLOCKS_UPDATE;
   }

   public void handle(ClientGamePacketListener p_132999_) {
      p_132999_.handleChunkBlocksUpdate(this);
   }

   public void runUpdates(BiConsumer<BlockPos, BlockState> p_132993_) {
      BlockPos.MutableBlockPos $$1 = new BlockPos.MutableBlockPos();

      for (int $$2 = 0; $$2 < this.positions.length; $$2++) {
         short $$3 = this.positions[$$2];
         $$1.set(this.sectionPos.relativeToBlockX($$3), this.sectionPos.relativeToBlockY($$3), this.sectionPos.relativeToBlockZ($$3));
         p_132993_.accept($$1, this.states[$$2]);
      }
   }
}
