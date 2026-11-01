package net.minecraft.world.entity.vehicle.minecart;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractMinecart extends VehicleEntity {
   private static final Vec3 LOWERED_PASSENGER_ATTACHMENT = new Vec3(0.0, 0.0, 0.0);
   private static final EntityDataAccessor<Optional<BlockState>> DATA_ID_CUSTOM_DISPLAY_BLOCK = SynchedEntityData.defineId(
      AbstractMinecart.class, EntityDataSerializers.OPTIONAL_BLOCK_STATE
   );
   private static final EntityDataAccessor<Integer> DATA_ID_DISPLAY_OFFSET = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);
   private static final ImmutableMap<Pose, ImmutableList<Integer>> POSE_DISMOUNT_HEIGHTS = ImmutableMap.of(
      Pose.STANDING, ImmutableList.of(0, 1, -1), Pose.CROUCHING, ImmutableList.of(0, 1, -1), Pose.SWIMMING, ImmutableList.of(0, 1)
   );
   protected static final float WATER_SLOWDOWN_FACTOR = 0.95F;
   private static final boolean DEFAULT_FLIPPED_ROTATION = false;
   private boolean onRails;
   private boolean flipped = false;
   private final MinecartBehavior behavior;
   private static final Map<RailShape, Pair<Vec3i, Vec3i>> EXITS = Maps.newEnumMap(
      (Map)Util.make(
         () -> {
            Vec3i vec3i = Direction.WEST.getUnitVec3i();
            Vec3i vec3i2 = Direction.EAST.getUnitVec3i();
            Vec3i vec3i3 = Direction.NORTH.getUnitVec3i();
            Vec3i vec3i4 = Direction.SOUTH.getUnitVec3i();
            Vec3i vec3i5 = vec3i.below();
            Vec3i vec3i6 = vec3i2.below();
            Vec3i vec3i7 = vec3i3.below();
            Vec3i vec3i8 = vec3i4.below();
            return ImmutableMap.of(
               RailShape.NORTH_SOUTH,
               Pair.of(vec3i3, vec3i4),
               RailShape.EAST_WEST,
               Pair.of(vec3i, vec3i2),
               RailShape.ASCENDING_EAST,
               Pair.of(vec3i5, vec3i2),
               RailShape.ASCENDING_WEST,
               Pair.of(vec3i, vec3i6),
               RailShape.ASCENDING_NORTH,
               Pair.of(vec3i3, vec3i8),
               RailShape.ASCENDING_SOUTH,
               Pair.of(vec3i7, vec3i4),
               RailShape.SOUTH_EAST,
               Pair.of(vec3i4, vec3i2),
               RailShape.SOUTH_WEST,
               Pair.of(vec3i4, vec3i),
               RailShape.NORTH_WEST,
               Pair.of(vec3i3, vec3i),
               RailShape.NORTH_EAST,
               Pair.of(vec3i3, vec3i2)
            );
         }
      )
   );

   protected AbstractMinecart(EntityType<?> entityType, Level level) {
      super(entityType, level);
      this.blocksBuilding = true;
      if (useExperimentalMovement(level)) {
         this.behavior = new NewMinecartBehavior(this);
      } else {
         this.behavior = new OldMinecartBehavior(this);
      }
   }

   protected AbstractMinecart(EntityType<?> entityType, Level level, double x, double y, double z) {
      this(entityType, level);
      this.setInitialPos(x, y, z);
   }

   public void setInitialPos(double x, double y, double z) {
      this.setPos(x, y, z);
      this.xo = x;
      this.yo = y;
      this.zo = z;
   }

   @Nullable
   public static <T extends AbstractMinecart> T createMinecart(
      Level level, double x, double y, double z, EntityType<T> entityType, EntitySpawnReason spawnReason, ItemStack stack, @Nullable Player player
   ) {
      T abstractMinecart = (T)entityType.create(level, spawnReason);
      if (abstractMinecart != null) {
         abstractMinecart.setInitialPos(x, y, z);
         EntityType.createDefaultStackConfig(level, stack, player).accept(abstractMinecart);
         if (abstractMinecart.getBehavior() instanceof NewMinecartBehavior newMinecartBehavior) {
            BlockPos blockPos = abstractMinecart.getCurrentBlockPosOrRailBelow();
            BlockState blockState = level.getBlockState(blockPos);
            newMinecartBehavior.adjustToRails(blockPos, blockState, true);
         }
      }

      return abstractMinecart;
   }

   public MinecartBehavior getBehavior() {
      return this.behavior;
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.EVENTS;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_ID_CUSTOM_DISPLAY_BLOCK, Optional.empty());
      builder.define(DATA_ID_DISPLAY_OFFSET, this.getDefaultDisplayOffset());
   }

   @Override
   public boolean canCollideWith(Entity entity) {
      return AbstractBoat.canVehicleCollide(this, entity);
   }

   @Override
   public boolean isPushable() {
      return true;
   }

   @Override
   public Vec3 getRelativePortalPosition(Direction.Axis axis, BlockUtil.FoundRectangle rectangle) {
      return LivingEntity.resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(axis, rectangle));
   }

   @Override
   protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float partialTick) {
      boolean bl = entity instanceof Villager || entity instanceof WanderingTrader;
      return bl ? LOWERED_PASSENGER_ATTACHMENT : super.getPassengerAttachmentPoint(entity, dimensions, partialTick);
   }

   @Override
   public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
      Direction direction = this.getMotionDirection();
      if (direction.getAxis() == Direction.Axis.Y) {
         return super.getDismountLocationForPassenger(passenger);
      } else {
         int[][] is = DismountHelper.offsetsForDirection(direction);
         BlockPos blockPos = this.blockPosition();
         BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
         ImmutableList<Pose> immutableList = passenger.getDismountPoses();
         UnmodifiableIterator e = immutableList.iterator();

         while (e.hasNext()) {
            Pose pose = (Pose)e.next();
            EntityDimensions entityDimensions = passenger.getDimensions(pose);
            float f = Math.min(entityDimensions.width(), 1.0F) / 2.0F;
            UnmodifiableIterator g = ((ImmutableList)POSE_DISMOUNT_HEIGHTS.get(pose)).iterator();

            while (g.hasNext()) {
               int i = (Integer)g.next();

               for (int[] js : is) {
                  mutableBlockPos.set(blockPos.getX() + js[0], blockPos.getY() + i, blockPos.getZ() + js[1]);
                  double d = this.level()
                     .getBlockFloorHeight(
                        DismountHelper.nonClimbableShape(this.level(), mutableBlockPos),
                        () -> DismountHelper.nonClimbableShape(this.level(), mutableBlockPos.below())
                     );
                  if (DismountHelper.isBlockFloorValid(d)) {
                     AABB aABB = new AABB(-f, 0.0, -f, f, entityDimensions.height(), f);
                     Vec3 vec3 = Vec3.upFromBottomCenterOf(mutableBlockPos, d);
                     if (DismountHelper.canDismountTo(this.level(), passenger, aABB.move(vec3))) {
                        passenger.setPose(pose);
                        return vec3;
                     }
                  }
               }
            }
         }

         double ex = this.getBoundingBox().maxY;
         mutableBlockPos.set((double)blockPos.getX(), ex, (double)blockPos.getZ());
         UnmodifiableIterator var22 = immutableList.iterator();

         while (var22.hasNext()) {
            Pose pose2 = (Pose)var22.next();
            double g = passenger.getDimensions(pose2).height();
            int j = Mth.ceil(ex - mutableBlockPos.getY() + g);
            double h = DismountHelper.findCeilingFrom(
               mutableBlockPos, j, blockPosx -> this.level().getBlockState(blockPosx).getCollisionShape(this.level(), blockPosx)
            );
            if (ex + g <= h) {
               passenger.setPose(pose2);
               break;
            }
         }

         return super.getDismountLocationForPassenger(passenger);
      }
   }

   @Override
   protected float getBlockSpeedFactor() {
      BlockState blockState = this.level().getBlockState(this.blockPosition());
      return blockState.is(BlockTags.RAILS) ? 1.0F : super.getBlockSpeedFactor();
   }

   @Override
   public void animateHurt(float yaw) {
      this.setHurtDir(-this.getHurtDir());
      this.setHurtTime(10);
      this.setDamage(this.getDamage() + this.getDamage() * 10.0F);
   }

   @Override
   public boolean isPickable() {
      return !this.isRemoved();
   }

   public static Pair<Vec3i, Vec3i> exits(RailShape shape) {
      return EXITS.get(shape);
   }

   @Override
   public Direction getMotionDirection() {
      return this.behavior.getMotionDirection();
   }

   @Override
   protected double getDefaultGravity() {
      return this.isInWater() ? 0.005 : 0.04;
   }

   @Override
   public void tick() {
      if (this.getHurtTime() > 0) {
         this.setHurtTime(this.getHurtTime() - 1);
      }

      if (this.getDamage() > 0.0F) {
         this.setDamage(this.getDamage() - 1.0F);
      }

      this.checkBelowWorld();
      this.computeSpeed();
      this.handlePortal();
      this.behavior.tick();
      this.updateInWaterStateAndDoFluidPushing();
      if (this.isInLava()) {
         this.lavaIgnite();
         this.lavaHurt();
         this.fallDistance *= 0.5;
      }

      this.firstTick = false;
   }

   public boolean isFirstTick() {
      return this.firstTick;
   }

   public BlockPos getCurrentBlockPosOrRailBelow() {
      int i = Mth.floor(this.getX());
      int j = Mth.floor(this.getY());
      int k = Mth.floor(this.getZ());
      if (useExperimentalMovement(this.level())) {
         double d = this.getY() - 0.1 - 1.0E-5F;
         if (this.level().getBlockState(BlockPos.containing(i, d, k)).is(BlockTags.RAILS)) {
            j = Mth.floor(d);
         }
      } else if (this.level().getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS)) {
         j--;
      }

      return new BlockPos(i, j, k);
   }

   protected double getMaxSpeed(ServerLevel level) {
      return this.behavior.getMaxSpeed(level);
   }

   public void activateMinecart(ServerLevel level, int x, int y, int z, boolean isPowered) {
   }

   @Override
   public void lerpPositionAndRotationStep(int steps, double targetX, double targetY, double targetZ, double targetYRot, double targetXRot) {
      super.lerpPositionAndRotationStep(steps, targetX, targetY, targetZ, targetYRot, targetXRot);
   }

   @Override
   public void applyGravity() {
      super.applyGravity();
   }

   @Override
   public void reapplyPosition() {
      super.reapplyPosition();
   }

   @Override
   public boolean updateInWaterStateAndDoFluidPushing() {
      return super.updateInWaterStateAndDoFluidPushing();
   }

   @Override
   public Vec3 getKnownMovement() {
      return this.behavior.getKnownMovement(super.getKnownMovement());
   }

   @Override
   public InterpolationHandler getInterpolation() {
      return this.behavior.getInterpolation();
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket packet) {
      super.recreateFromPacket(packet);
      this.behavior.lerpMotion(this.getDeltaMovement());
   }

   @Override
   public void lerpMotion(Vec3 deltaMovement) {
      this.behavior.lerpMotion(deltaMovement);
   }

   protected void moveAlongTrack(ServerLevel level) {
      this.behavior.moveAlongTrack(level);
   }

   protected void comeOffTrack(ServerLevel level) {
      double d = this.getMaxSpeed(level);
      Vec3 vec3 = this.getDeltaMovement();
      this.setDeltaMovement(Mth.clamp(vec3.x, -d, d), vec3.y, Mth.clamp(vec3.z, -d, d));
      if (this.onGround()) {
         this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
      }

      this.move(MoverType.SELF, this.getDeltaMovement());
      if (!this.onGround()) {
         this.setDeltaMovement(this.getDeltaMovement().scale(0.95));
      }
   }

   protected double makeStepAlongTrack(BlockPos pos, RailShape railShape, double speed) {
      return this.behavior.stepAlongTrack(pos, railShape, speed);
   }

   @Override
   public void move(MoverType type, Vec3 movement) {
      if (useExperimentalMovement(this.level())) {
         Vec3 vec3 = this.position().add(movement);
         super.move(type, movement);
         boolean bl = this.behavior.pushAndPickupEntities();
         if (bl) {
            super.move(type, vec3.subtract(this.position()));
         }

         if (type.equals(MoverType.PISTON)) {
            this.onRails = false;
         }
      } else {
         super.move(type, movement);
         this.applyEffectsFromBlocks();
      }
   }

   @Override
   public void applyEffectsFromBlocks() {
      if (useExperimentalMovement(this.level())) {
         super.applyEffectsFromBlocks();
      } else {
         this.applyEffectsFromBlocks(this.position(), this.position());
         this.clearMovementThisTick();
      }
   }

   @Override
   public boolean isOnRails() {
      return this.onRails;
   }

   public void setOnRails(boolean onRails) {
      this.onRails = onRails;
   }

   public boolean isFlipped() {
      return this.flipped;
   }

   public void setFlipped(boolean flipped) {
      this.flipped = flipped;
   }

   public Vec3 getRedstoneDirection(BlockPos pos) {
      BlockState blockState = this.level().getBlockState(pos);
      if (blockState.is(Blocks.POWERED_RAIL) && blockState.getValue(PoweredRailBlock.POWERED)) {
         RailShape railShape = blockState.getValue(((BaseRailBlock)blockState.getBlock()).getShapeProperty());
         if (railShape == RailShape.EAST_WEST) {
            if (this.isRedstoneConductor(pos.west())) {
               return new Vec3(1.0, 0.0, 0.0);
            }

            if (this.isRedstoneConductor(pos.east())) {
               return new Vec3(-1.0, 0.0, 0.0);
            }
         } else if (railShape == RailShape.NORTH_SOUTH) {
            if (this.isRedstoneConductor(pos.north())) {
               return new Vec3(0.0, 0.0, 1.0);
            }

            if (this.isRedstoneConductor(pos.south())) {
               return new Vec3(0.0, 0.0, -1.0);
            }
         }

         return Vec3.ZERO;
      } else {
         return Vec3.ZERO;
      }
   }

   public boolean isRedstoneConductor(BlockPos pos) {
      return this.level().getBlockState(pos).isRedstoneConductor(this.level(), pos);
   }

   protected Vec3 applyNaturalSlowdown(Vec3 speed) {
      double d = this.behavior.getSlowdownFactor();
      Vec3 vec3 = speed.multiply(d, 0.0, d);
      if (this.isInWater()) {
         vec3 = vec3.scale(0.95F);
      }

      return vec3;
   }

   @Override
   protected void readAdditionalSaveData(ValueInput input) {
      this.setCustomDisplayBlockState(input.read("DisplayState", BlockState.CODEC));
      this.setDisplayOffset(input.getIntOr("DisplayOffset", this.getDefaultDisplayOffset()));
      this.flipped = input.getBooleanOr("FlippedRotation", false);
      this.firstTick = input.getBooleanOr("HasTicked", false);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput output) {
      this.getCustomDisplayBlockState().ifPresent(blockState -> output.store("DisplayState", BlockState.CODEC, blockState));
      int i = this.getDisplayOffset();
      if (i != this.getDefaultDisplayOffset()) {
         output.putInt("DisplayOffset", i);
      }

      output.putBoolean("FlippedRotation", this.flipped);
      output.putBoolean("HasTicked", this.firstTick);
   }

   @Override
   public void push(Entity entity) {
      if (!this.level().isClientSide()) {
         if (!entity.noPhysics && !this.noPhysics) {
            if (!this.hasPassenger(entity)) {
               double d = entity.getX() - this.getX();
               double e = entity.getZ() - this.getZ();
               double f = d * d + e * e;
               if (f >= 1.0E-4F) {
                  f = Math.sqrt(f);
                  d /= f;
                  e /= f;
                  double g = 1.0 / f;
                  if (g > 1.0) {
                     g = 1.0;
                  }

                  d *= g;
                  e *= g;
                  d *= 0.1F;
                  e *= 0.1F;
                  d *= 0.5;
                  e *= 0.5;
                  if (entity instanceof AbstractMinecart abstractMinecart) {
                     this.pushOtherMinecart(abstractMinecart, d, e);
                  } else {
                     this.push(-d, 0.0, -e);
                     entity.push(d / 4.0, 0.0, e / 4.0);
                  }
               }
            }
         }
      }
   }

   private void pushOtherMinecart(AbstractMinecart otherMinecart, double deltaX, double deltaZ) {
      double d;
      double e;
      if (useExperimentalMovement(this.level())) {
         d = this.getDeltaMovement().x;
         e = this.getDeltaMovement().z;
      } else {
         d = otherMinecart.getX() - this.getX();
         e = otherMinecart.getZ() - this.getZ();
      }

      Vec3 vec3 = new Vec3(d, 0.0, e).normalize();
      Vec3 vec32 = new Vec3(Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)), 0.0, Mth.sin(this.getYRot() * (float) (Math.PI / 180.0))).normalize();
      double f = Math.abs(vec3.dot(vec32));
      if (!(f < 0.8F) || useExperimentalMovement(this.level())) {
         Vec3 vec33 = this.getDeltaMovement();
         Vec3 vec34 = otherMinecart.getDeltaMovement();
         if (otherMinecart.isFurnace() && !this.isFurnace()) {
            this.setDeltaMovement(vec33.multiply(0.2, 1.0, 0.2));
            this.push(vec34.x - deltaX, 0.0, vec34.z - deltaZ);
            otherMinecart.setDeltaMovement(vec34.multiply(0.95, 1.0, 0.95));
         } else if (!otherMinecart.isFurnace() && this.isFurnace()) {
            otherMinecart.setDeltaMovement(vec34.multiply(0.2, 1.0, 0.2));
            otherMinecart.push(vec33.x + deltaX, 0.0, vec33.z + deltaZ);
            this.setDeltaMovement(vec33.multiply(0.95, 1.0, 0.95));
         } else {
            double g = (vec34.x + vec33.x) / 2.0;
            double h = (vec34.z + vec33.z) / 2.0;
            this.setDeltaMovement(vec33.multiply(0.2, 1.0, 0.2));
            this.push(g - deltaX, 0.0, h - deltaZ);
            otherMinecart.setDeltaMovement(vec34.multiply(0.2, 1.0, 0.2));
            otherMinecart.push(g + deltaX, 0.0, h + deltaZ);
         }
      }
   }

   public BlockState getDisplayBlockState() {
      return this.getCustomDisplayBlockState().orElseGet(this::getDefaultDisplayBlockState);
   }

   private Optional<BlockState> getCustomDisplayBlockState() {
      return this.getEntityData().get(DATA_ID_CUSTOM_DISPLAY_BLOCK);
   }

   public BlockState getDefaultDisplayBlockState() {
      return Blocks.AIR.defaultBlockState();
   }

   public int getDisplayOffset() {
      return this.getEntityData().get(DATA_ID_DISPLAY_OFFSET);
   }

   public int getDefaultDisplayOffset() {
      return 6;
   }

   public void setCustomDisplayBlockState(Optional<BlockState> customDisplayBlockState) {
      this.getEntityData().set(DATA_ID_CUSTOM_DISPLAY_BLOCK, customDisplayBlockState);
   }

   public void setDisplayOffset(int displayOffset) {
      this.getEntityData().set(DATA_ID_DISPLAY_OFFSET, displayOffset);
   }

   public static boolean useExperimentalMovement(Level level) {
      return level.enabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS);
   }

   @Override
   public abstract ItemStack getPickResult();

   public boolean isRideable() {
      return false;
   }

   public boolean isFurnace() {
      return false;
   }
}
