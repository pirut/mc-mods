package net.minecraft.world.waypoints;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public interface WaypointTransmitter extends Waypoint {
   int REALLY_FAR_DISTANCE = 332;

   boolean isTransmittingWaypoint();

   Optional<WaypointTransmitter.Connection> makeWaypointConnectionWith(ServerPlayer var1);

   Waypoint.Icon waypointIcon();

   static boolean doesSourceIgnoreReceiver(LivingEntity p_416687_, ServerPlayer p_416170_) {
      if (p_416170_.isSpectator()) {
         return false;
      } else if (!p_416687_.isSpectator() && !p_416687_.hasIndirectPassenger(p_416170_)) {
         double d0 = Math.min(p_416687_.getAttributeValue(Attributes.WAYPOINT_TRANSMIT_RANGE), p_416170_.getAttributeValue(Attributes.WAYPOINT_RECEIVE_RANGE));
         return p_416687_.distanceTo(p_416170_) >= d0;
      } else {
         return true;
      }
   }

   static boolean isChunkVisible(ChunkPos p_415770_, ServerPlayer p_416305_) {
      return p_416305_.getChunkTrackingView().isInViewDistance(p_415770_.x, p_415770_.z);
   }

   static boolean isReallyFar(LivingEntity p_416338_, ServerPlayer p_415768_) {
      return p_416338_.distanceTo(p_415768_) > 332.0F;
   }

   public interface BlockConnection extends WaypointTransmitter.Connection {
      int distanceManhattan();

      @Override
      default boolean isBroken() {
         return this.distanceManhattan() > 1;
      }
   }

   public interface ChunkConnection extends WaypointTransmitter.Connection {
      int distanceChessboard();

      @Override
      default boolean isBroken() {
         return this.distanceChessboard() > 1;
      }
   }

   public interface Connection {
      void connect();

      void disconnect();

      void update();

      boolean isBroken();
   }

   public static class EntityAzimuthConnection implements WaypointTransmitter.Connection {
      private final LivingEntity source;
      private final Waypoint.Icon icon;
      private final ServerPlayer receiver;
      private float lastAngle;

      public EntityAzimuthConnection(LivingEntity p_415689_, Waypoint.Icon p_416024_, ServerPlayer p_416162_) {
         this.source = p_415689_;
         this.icon = p_416024_;
         this.receiver = p_416162_;
         Vec3 vec3 = p_416162_.position().subtract(p_415689_.position()).rotateClockwise90();
         this.lastAngle = (float)Mth.atan2(vec3.z(), vec3.x());
      }

      @Override
      public boolean isBroken() {
         return WaypointTransmitter.doesSourceIgnoreReceiver(this.source, this.receiver)
            || WaypointTransmitter.isChunkVisible(this.source.chunkPosition(), this.receiver)
            || !WaypointTransmitter.isReallyFar(this.source, this.receiver);
      }

      @Override
      public void connect() {
         this.receiver.connection.send(ClientboundTrackedWaypointPacket.addWaypointAzimuth(this.source.getUUID(), this.icon, this.lastAngle));
      }

      @Override
      public void disconnect() {
         this.receiver.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(this.source.getUUID()));
      }

      @Override
      public void update() {
         Vec3 vec3 = this.receiver.position().subtract(this.source.position()).rotateClockwise90();
         float f = (float)Mth.atan2(vec3.z(), vec3.x());
         if (Mth.abs(f - this.lastAngle) > 0.008726646F) {
            this.receiver.connection.send(ClientboundTrackedWaypointPacket.updateWaypointAzimuth(this.source.getUUID(), this.icon, f));
            this.lastAngle = f;
         }
      }
   }

   public static class EntityBlockConnection implements WaypointTransmitter.BlockConnection {
      private final LivingEntity source;
      private final Waypoint.Icon icon;
      private final ServerPlayer receiver;
      private BlockPos lastPosition;

      public EntityBlockConnection(LivingEntity p_415843_, Waypoint.Icon p_416708_, ServerPlayer p_416050_) {
         this.source = p_415843_;
         this.receiver = p_416050_;
         this.icon = p_416708_;
         this.lastPosition = p_415843_.blockPosition();
      }

      @Override
      public void connect() {
         this.receiver.connection.send(ClientboundTrackedWaypointPacket.addWaypointPosition(this.source.getUUID(), this.icon, this.lastPosition));
      }

      @Override
      public void disconnect() {
         this.receiver.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(this.source.getUUID()));
      }

      @Override
      public void update() {
         BlockPos blockpos = this.source.blockPosition();
         if (blockpos.distManhattan(this.lastPosition) > 0) {
            this.receiver.connection.send(ClientboundTrackedWaypointPacket.updateWaypointPosition(this.source.getUUID(), this.icon, blockpos));
            this.lastPosition = blockpos;
         }
      }

      @Override
      public int distanceManhattan() {
         return this.lastPosition.distManhattan(this.source.blockPosition());
      }

      @Override
      public boolean isBroken() {
         return WaypointTransmitter.BlockConnection.super.isBroken() || WaypointTransmitter.doesSourceIgnoreReceiver(this.source, this.receiver);
      }
   }

   public static class EntityChunkConnection implements WaypointTransmitter.ChunkConnection {
      private final LivingEntity source;
      private final Waypoint.Icon icon;
      private final ServerPlayer receiver;
      private ChunkPos lastPosition;

      public EntityChunkConnection(LivingEntity p_416003_, Waypoint.Icon p_416383_, ServerPlayer p_416725_) {
         this.source = p_416003_;
         this.icon = p_416383_;
         this.receiver = p_416725_;
         this.lastPosition = p_416003_.chunkPosition();
      }

      @Override
      public int distanceChessboard() {
         return this.lastPosition.getChessboardDistance(this.source.chunkPosition());
      }

      @Override
      public void connect() {
         this.receiver.connection.send(ClientboundTrackedWaypointPacket.addWaypointChunk(this.source.getUUID(), this.icon, this.lastPosition));
      }

      @Override
      public void disconnect() {
         this.receiver.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(this.source.getUUID()));
      }

      @Override
      public void update() {
         ChunkPos chunkpos = this.source.chunkPosition();
         if (chunkpos.getChessboardDistance(this.lastPosition) > 0) {
            this.receiver.connection.send(ClientboundTrackedWaypointPacket.updateWaypointChunk(this.source.getUUID(), this.icon, chunkpos));
            this.lastPosition = chunkpos;
         }
      }

      @Override
      public boolean isBroken() {
         return !WaypointTransmitter.ChunkConnection.super.isBroken() && !WaypointTransmitter.doesSourceIgnoreReceiver(this.source, this.receiver)
            ? WaypointTransmitter.isChunkVisible(this.lastPosition, this.receiver)
            : true;
      }
   }
}
