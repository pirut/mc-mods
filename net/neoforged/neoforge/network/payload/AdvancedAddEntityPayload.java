package net.neoforged.neoforge.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.util.FriendlyByteBufUtil;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record AdvancedAddEntityPayload(int entityId, byte[] customPayload) implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<AdvancedAddEntityPayload> TYPE = new CustomPacketPayload.Type<>(
      ResourceLocation.fromNamespaceAndPath("neoforge", "advanced_add_entity")
   );
   public static final StreamCodec<FriendlyByteBuf, AdvancedAddEntityPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      AdvancedAddEntityPayload::entityId,
      NeoForgeStreamCodecs.UNBOUNDED_BYTE_ARRAY,
      AdvancedAddEntityPayload::customPayload,
      AdvancedAddEntityPayload::new
   );

   public AdvancedAddEntityPayload(Entity e) {
      this(e.getId(), writeCustomData(e));
   }

   private static byte[] writeCustomData(Entity entity) {
      return entity instanceof IEntityWithComplexSpawn additionalSpawnData
         ? FriendlyByteBufUtil.writeCustomData(additionalSpawnData::writeSpawnData, entity.registryAccess())
         : new byte[0];
   }

   @Override
   public CustomPacketPayload.Type<AdvancedAddEntityPayload> type() {
      return TYPE;
   }
}
