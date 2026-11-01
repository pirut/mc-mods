package net.minecraft.world.waypoints;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.function.TriFunction;
import org.slf4j.Logger;

public abstract class TrackedWaypoint implements Waypoint {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final StreamCodec<ByteBuf, TrackedWaypoint> STREAM_CODEC = StreamCodec.ofMember(TrackedWaypoint::write, TrackedWaypoint::read);
   protected final Either<UUID, String> identifier;
   private final Waypoint.Icon icon;
   private final TrackedWaypoint.Type type;

   TrackedWaypoint(Either<UUID, String> p_416104_, Waypoint.Icon p_416164_, TrackedWaypoint.Type p_415645_) {
      this.identifier = p_416104_;
      this.icon = p_416164_;
      this.type = p_415645_;
   }

   public Either<UUID, String> id() {
      return this.identifier;
   }

   public abstract void update(TrackedWaypoint var1);

   public void write(ByteBuf p_416238_) {
      FriendlyByteBuf friendlybytebuf = new FriendlyByteBuf(p_416238_);
      friendlybytebuf.writeEither(this.identifier, UUIDUtil.STREAM_CODEC, FriendlyByteBuf::writeUtf);
      Waypoint.Icon.STREAM_CODEC.encode(friendlybytebuf, this.icon);
      friendlybytebuf.writeEnum(this.type);
      this.writeContents(p_416238_);
   }

   public abstract void writeContents(ByteBuf var1);

   private static TrackedWaypoint read(ByteBuf p_415861_) {
      FriendlyByteBuf friendlybytebuf = new FriendlyByteBuf(p_415861_);
      Either<UUID, String> either = friendlybytebuf.readEither(UUIDUtil.STREAM_CODEC, FriendlyByteBuf::readUtf);
      Waypoint.Icon waypoint$icon = Waypoint.Icon.STREAM_CODEC.decode(friendlybytebuf);
      TrackedWaypoint.Type trackedwaypoint$type = friendlybytebuf.readEnum(TrackedWaypoint.Type.class);
      return (TrackedWaypoint)trackedwaypoint$type.constructor.apply(either, waypoint$icon, friendlybytebuf);
   }

   public static TrackedWaypoint setPosition(UUID p_415858_, Waypoint.Icon p_416632_, Vec3i p_415947_) {
      return new TrackedWaypoint.Vec3iWaypoint(p_415858_, p_416632_, p_415947_);
   }

   public static TrackedWaypoint setChunk(UUID p_416640_, Waypoint.Icon p_415916_, ChunkPos p_416044_) {
      return new TrackedWaypoint.ChunkWaypoint(p_416640_, p_415916_, p_416044_);
   }

   public static TrackedWaypoint setAzimuth(UUID p_415851_, Waypoint.Icon p_416296_, float p_416696_) {
      return new TrackedWaypoint.AzimuthWaypoint(p_415851_, p_416296_, p_416696_);
   }

   public static TrackedWaypoint empty(UUID p_416251_) {
      return new TrackedWaypoint.EmptyWaypoint(p_416251_);
   }

   public abstract double yawAngleToCamera(Level var1, TrackedWaypoint.Camera var2, PartialTickSupplier var3);

   public abstract TrackedWaypoint.PitchDirection pitchDirectionToCamera(Level var1, TrackedWaypoint.Projector var2, PartialTickSupplier var3);

   public abstract double distanceSquared(Entity var1);

   public Waypoint.Icon icon() {
      return this.icon;
   }

   static class AzimuthWaypoint extends TrackedWaypoint {
      private float angle;

      public AzimuthWaypoint(UUID p_416086_, Waypoint.Icon p_416213_, float p_416138_) {
         super(Either.left(p_416086_), p_416213_, TrackedWaypoint.Type.AZIMUTH);
         this.angle = p_416138_;
      }

      public AzimuthWaypoint(Either<UUID, String> p_416692_, Waypoint.Icon p_415806_, FriendlyByteBuf p_415614_) {
         super(p_416692_, p_415806_, TrackedWaypoint.Type.AZIMUTH);
         this.angle = p_415614_.readFloat();
      }

      @Override
      public void update(TrackedWaypoint p_416744_) {
         if (p_416744_ instanceof TrackedWaypoint.AzimuthWaypoint trackedwaypoint$azimuthwaypoint) {
            this.angle = trackedwaypoint$azimuthwaypoint.angle;
         } else {
            TrackedWaypoint.LOGGER.warn("Unsupported Waypoint update operation: {}", p_416744_.getClass());
         }
      }

      @Override
      public void writeContents(ByteBuf p_416592_) {
         p_416592_.writeFloat(this.angle);
      }

      @Override
      public double yawAngleToCamera(Level p_419593_, TrackedWaypoint.Camera p_419977_, PartialTickSupplier p_437329_) {
         return Mth.degreesDifference(p_419977_.yaw(), this.angle * (180.0F / (float)Math.PI));
      }

      @Override
      public TrackedWaypoint.PitchDirection pitchDirectionToCamera(Level p_419920_, TrackedWaypoint.Projector p_416265_, PartialTickSupplier p_437258_) {
         double d0 = p_416265_.projectHorizonToScreen();
         if (d0 < -1.0) {
            return TrackedWaypoint.PitchDirection.DOWN;
         } else {
            return d0 > 1.0 ? TrackedWaypoint.PitchDirection.UP : TrackedWaypoint.PitchDirection.NONE;
         }
      }

      @Override
      public double distanceSquared(Entity p_415870_) {
         return Double.POSITIVE_INFINITY;
      }
   }

   public interface Camera {
      float yaw();

      Vec3 position();
   }

   static class ChunkWaypoint extends TrackedWaypoint {
      private ChunkPos chunkPos;

      public ChunkWaypoint(UUID p_415927_, Waypoint.Icon p_416553_, ChunkPos p_416069_) {
         super(Either.left(p_415927_), p_416553_, TrackedWaypoint.Type.CHUNK);
         this.chunkPos = p_416069_;
      }

      public ChunkWaypoint(Either<UUID, String> p_415787_, Waypoint.Icon p_415737_, FriendlyByteBuf p_416433_) {
         super(p_415787_, p_415737_, TrackedWaypoint.Type.CHUNK);
         this.chunkPos = new ChunkPos(p_416433_.readVarInt(), p_416433_.readVarInt());
      }

      @Override
      public void update(TrackedWaypoint p_416415_) {
         if (p_416415_ instanceof TrackedWaypoint.ChunkWaypoint trackedwaypoint$chunkwaypoint) {
            this.chunkPos = trackedwaypoint$chunkwaypoint.chunkPos;
         } else {
            TrackedWaypoint.LOGGER.warn("Unsupported Waypoint update operation: {}", p_416415_.getClass());
         }
      }

      @Override
      public void writeContents(ByteBuf p_416369_) {
         VarInt.write(p_416369_, this.chunkPos.x);
         VarInt.write(p_416369_, this.chunkPos.z);
      }

      private Vec3 position(double p_419928_) {
         return Vec3.atCenterOf(this.chunkPos.getMiddleBlockPosition((int)p_419928_));
      }

      @Override
      public double yawAngleToCamera(Level p_420006_, TrackedWaypoint.Camera p_419530_, PartialTickSupplier p_437285_) {
         Vec3 vec3 = p_419530_.position();
         Vec3 vec31 = vec3.subtract(this.position(vec3.y())).rotateClockwise90();
         float f = (float)Mth.atan2(vec31.z(), vec31.x()) * (180.0F / (float)Math.PI);
         return Mth.degreesDifference(p_419530_.yaw(), f);
      }

      @Override
      public TrackedWaypoint.PitchDirection pitchDirectionToCamera(Level p_419513_, TrackedWaypoint.Projector p_416711_, PartialTickSupplier p_437246_) {
         double d0 = p_416711_.projectHorizonToScreen();
         if (d0 < -1.0) {
            return TrackedWaypoint.PitchDirection.DOWN;
         } else {
            return d0 > 1.0 ? TrackedWaypoint.PitchDirection.UP : TrackedWaypoint.PitchDirection.NONE;
         }
      }

      @Override
      public double distanceSquared(Entity p_415648_) {
         return p_415648_.distanceToSqr(Vec3.atCenterOf(this.chunkPos.getMiddleBlockPosition(p_415648_.getBlockY())));
      }
   }

   static class EmptyWaypoint extends TrackedWaypoint {
      private EmptyWaypoint(Either<UUID, String> p_416008_, Waypoint.Icon p_416267_, FriendlyByteBuf p_415647_) {
         super(p_416008_, p_416267_, TrackedWaypoint.Type.EMPTY);
      }

      EmptyWaypoint(UUID p_416299_) {
         super(Either.left(p_416299_), Waypoint.Icon.NULL, TrackedWaypoint.Type.EMPTY);
      }

      @Override
      public void update(TrackedWaypoint p_416695_) {
      }

      @Override
      public void writeContents(ByteBuf p_415596_) {
      }

      @Override
      public double yawAngleToCamera(Level p_419897_, TrackedWaypoint.Camera p_419457_, PartialTickSupplier p_437411_) {
         return Double.NaN;
      }

      @Override
      public TrackedWaypoint.PitchDirection pitchDirectionToCamera(Level p_419455_, TrackedWaypoint.Projector p_416651_, PartialTickSupplier p_437225_) {
         return TrackedWaypoint.PitchDirection.NONE;
      }

      @Override
      public double distanceSquared(Entity p_415940_) {
         return Double.POSITIVE_INFINITY;
      }
   }

   public static enum PitchDirection {
      NONE,
      UP,
      DOWN;
   }

   public interface Projector {
      Vec3 projectPointToScreen(Vec3 var1);

      double projectHorizonToScreen();
   }

   static enum Type {
      EMPTY(TrackedWaypoint.EmptyWaypoint::new),
      VEC3I(TrackedWaypoint.Vec3iWaypoint::new),
      CHUNK(TrackedWaypoint.ChunkWaypoint::new),
      AZIMUTH(TrackedWaypoint.AzimuthWaypoint::new);

      final TriFunction<Either<UUID, String>, Waypoint.Icon, FriendlyByteBuf, TrackedWaypoint> constructor;

      private Type(TriFunction<Either<UUID, String>, Waypoint.Icon, FriendlyByteBuf, TrackedWaypoint> p_416626_) {
         this.constructor = p_416626_;
      }
   }

   static class Vec3iWaypoint extends TrackedWaypoint {
      private Vec3i vector;

      public Vec3iWaypoint(UUID p_416462_, Waypoint.Icon p_416478_, Vec3i p_416594_) {
         super(Either.left(p_416462_), p_416478_, TrackedWaypoint.Type.VEC3I);
         this.vector = p_416594_;
      }

      public Vec3iWaypoint(Either<UUID, String> p_416300_, Waypoint.Icon p_415893_, FriendlyByteBuf p_416679_) {
         super(p_416300_, p_415893_, TrackedWaypoint.Type.VEC3I);
         this.vector = new Vec3i(p_416679_.readVarInt(), p_416679_.readVarInt(), p_416679_.readVarInt());
      }

      @Override
      public void update(TrackedWaypoint p_416312_) {
         if (p_416312_ instanceof TrackedWaypoint.Vec3iWaypoint trackedwaypoint$vec3iwaypoint) {
            this.vector = trackedwaypoint$vec3iwaypoint.vector;
         } else {
            TrackedWaypoint.LOGGER.warn("Unsupported Waypoint update operation: {}", p_416312_.getClass());
         }
      }

      @Override
      public void writeContents(ByteBuf p_416442_) {
         VarInt.write(p_416442_, this.vector.getX());
         VarInt.write(p_416442_, this.vector.getY());
         VarInt.write(p_416442_, this.vector.getZ());
      }

      private Vec3 position(Level p_416641_, PartialTickSupplier p_437189_) {
         return this.identifier
            .left()
            .map(p_416641_::getEntity)
            .map(p_437185_ -> p_437185_.blockPosition().distManhattan(this.vector) > 3 ? null : p_437185_.getEyePosition(p_437189_.apply(p_437185_)))
            .orElseGet(() -> Vec3.atCenterOf(this.vector));
      }

      @Override
      public double yawAngleToCamera(Level p_419799_, TrackedWaypoint.Camera p_419893_, PartialTickSupplier p_437238_) {
         Vec3 vec3 = p_419893_.position().subtract(this.position(p_419799_, p_437238_)).rotateClockwise90();
         float f = (float)Mth.atan2(vec3.z(), vec3.x()) * (180.0F / (float)Math.PI);
         return Mth.degreesDifference(p_419893_.yaw(), f);
      }

      @Override
      public TrackedWaypoint.PitchDirection pitchDirectionToCamera(Level p_419459_, TrackedWaypoint.Projector p_419783_, PartialTickSupplier p_437301_) {
         Vec3 vec3 = p_419783_.projectPointToScreen(this.position(p_419459_, p_437301_));
         boolean flag = vec3.z > 1.0;
         double d0 = flag ? -vec3.y : vec3.y;
         if (d0 < -1.0) {
            return TrackedWaypoint.PitchDirection.DOWN;
         } else if (d0 > 1.0) {
            return TrackedWaypoint.PitchDirection.UP;
         } else {
            if (flag) {
               if (vec3.y > 0.0) {
                  return TrackedWaypoint.PitchDirection.UP;
               }

               if (vec3.y < 0.0) {
                  return TrackedWaypoint.PitchDirection.DOWN;
               }
            }

            return TrackedWaypoint.PitchDirection.NONE;
         }
      }

      @Override
      public double distanceSquared(Entity p_416747_) {
         return p_416747_.distanceToSqr(Vec3.atCenterOf(this.vector));
      }
   }
}
