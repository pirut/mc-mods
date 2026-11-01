package net.neoforged.neoforge.common.world;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.payload.AuxiliaryLightDataPayload;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

@Internal
public final class LevelChunkAuxiliaryLightManager implements AuxiliaryLightManager {
   public static final String LIGHT_NBT_KEY = "neoforge:aux_lights";
   private final LevelChunk owner;
   private final Map<BlockPos, Byte> lights = new ConcurrentHashMap<>();

   public LevelChunkAuxiliaryLightManager(LevelChunk owner) {
      this.owner = owner;
   }

   @Override
   public void setLightAt(BlockPos pos, int value) {
      pos = pos.immutable();
      value = Mth.clamp(value, 0, 15);
      Byte oldValue;
      if (value > 0) {
         oldValue = this.lights.put(pos, (byte)value);
      } else {
         oldValue = this.lights.remove(pos);
      }

      if (Objects.requireNonNullElse(oldValue, (byte)0) != value) {
         this.owner.getLevel().getChunkSource().getLightEngine().checkBlock(pos);
         this.owner.markUnsaved();
      }
   }

   @Override
   public int getLightAt(BlockPos pos) {
      return this.lights.getOrDefault(pos, Byte.valueOf((byte)0));
   }

   @Nullable
   public ListTag serializeNBT() {
      if (this.lights.isEmpty()) {
         return null;
      } else {
         ListTag list = new ListTag();
         this.lights.forEach((pos, light) -> {
            CompoundTag tag = new CompoundTag();
            tag.putLong("pos", pos.asLong());
            tag.putByte("level", light);
            list.add(tag);
         });
         return list;
      }
   }

   public void deserializeNBT(ListTag list) {
      list.compoundStream().forEach(tag -> this.lights.put(BlockPos.of(tag.getLongOr("pos", 0L)), tag.getByteOr("level", (byte)0)));
   }

   public Packet<?> sendLightDataTo(ClientboundLevelChunkWithLightPacket chunkPacket) {
      return new ClientboundBundlePacket(
         List.of(chunkPacket, new ClientboundCustomPayloadPacket(new AuxiliaryLightDataPayload(this.owner.getPos(), Map.copyOf(this.lights))))
      );
   }

   public void handleLightDataSync(Map<BlockPos, Byte> lights) {
      this.lights.clear();
      this.lights.putAll(lights);
   }
}
