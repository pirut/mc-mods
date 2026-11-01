package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.TrackedWaypointManager;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointManager;

public record ClientboundTrackedWaypointPacket(ClientboundTrackedWaypointPacket.Operation operation, TrackedWaypoint waypoint)
   implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundTrackedWaypointPacket> STREAM_CODEC = StreamCodec.composite(
      ClientboundTrackedWaypointPacket.Operation.STREAM_CODEC,
      ClientboundTrackedWaypointPacket::operation,
      TrackedWaypoint.STREAM_CODEC,
      ClientboundTrackedWaypointPacket::waypoint,
      ClientboundTrackedWaypointPacket::new
   );

   public static ClientboundTrackedWaypointPacket removeWaypoint(UUID p_416625_) {
      return new ClientboundTrackedWaypointPacket(ClientboundTrackedWaypointPacket.Operation.UNTRACK, TrackedWaypoint.empty(p_416625_));
   }

   public static ClientboundTrackedWaypointPacket addWaypointPosition(UUID p_416047_, Waypoint.Icon p_416575_, Vec3i p_415572_) {
      return new ClientboundTrackedWaypointPacket(
         ClientboundTrackedWaypointPacket.Operation.TRACK, TrackedWaypoint.setPosition(p_416047_, p_416575_, p_415572_)
      );
   }

   public static ClientboundTrackedWaypointPacket updateWaypointPosition(UUID p_415700_, Waypoint.Icon p_415808_, Vec3i p_416349_) {
      return new ClientboundTrackedWaypointPacket(
         ClientboundTrackedWaypointPacket.Operation.UPDATE, TrackedWaypoint.setPosition(p_415700_, p_415808_, p_416349_)
      );
   }

   public static ClientboundTrackedWaypointPacket addWaypointChunk(UUID p_415984_, Waypoint.Icon p_416123_, ChunkPos p_416387_) {
      return new ClientboundTrackedWaypointPacket(ClientboundTrackedWaypointPacket.Operation.TRACK, TrackedWaypoint.setChunk(p_415984_, p_416123_, p_416387_));
   }

   public static ClientboundTrackedWaypointPacket updateWaypointChunk(UUID p_416600_, Waypoint.Icon p_416189_, ChunkPos p_415819_) {
      return new ClientboundTrackedWaypointPacket(ClientboundTrackedWaypointPacket.Operation.UPDATE, TrackedWaypoint.setChunk(p_416600_, p_416189_, p_415819_));
   }

   public static ClientboundTrackedWaypointPacket addWaypointAzimuth(UUID p_416113_, Waypoint.Icon p_415805_, float p_416624_) {
      return new ClientboundTrackedWaypointPacket(ClientboundTrackedWaypointPacket.Operation.TRACK, TrackedWaypoint.setAzimuth(p_416113_, p_415805_, p_416624_));
   }

   public static ClientboundTrackedWaypointPacket updateWaypointAzimuth(UUID p_415856_, Waypoint.Icon p_416583_, float p_416087_) {
      return new ClientboundTrackedWaypointPacket(
         ClientboundTrackedWaypointPacket.Operation.UPDATE, TrackedWaypoint.setAzimuth(p_415856_, p_416583_, p_416087_)
      );
   }

   @Override
   public PacketType<ClientboundTrackedWaypointPacket> type() {
      return GamePacketTypes.CLIENTBOUND_WAYPOINT;
   }

   public void handle(ClientGamePacketListener p_416105_) {
      p_416105_.handleWaypoint(this);
   }

   public void apply(TrackedWaypointManager p_415774_) {
      this.operation.action.accept(p_415774_, this.waypoint);
   }

   static enum Operation {
      TRACK(WaypointManager::trackWaypoint),
      UNTRACK(WaypointManager::untrackWaypoint),
      UPDATE(WaypointManager::updateWaypoint);

      final BiConsumer<TrackedWaypointManager, TrackedWaypoint> action;
      public static final IntFunction<ClientboundTrackedWaypointPacket.Operation> BY_ID = ByIdMap.continuous(
         Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP
      );
      public static final StreamCodec<ByteBuf, ClientboundTrackedWaypointPacket.Operation> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

      private Operation(BiConsumer<TrackedWaypointManager, TrackedWaypoint> p_415617_) {
         this.action = p_415617_;
      }
   }
}
