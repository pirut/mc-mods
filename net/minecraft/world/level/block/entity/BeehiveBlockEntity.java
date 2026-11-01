package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.slf4j.Logger;

public class BeehiveBlockEntity extends BlockEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String TAG_FLOWER_POS = "flower_pos";
   private static final String BEES = "bees";
   static final List<String> IGNORED_BEE_TAGS = Arrays.asList(
      "Air",
      "ArmorDropChances",
      "ArmorItems",
      "Brain",
      "CanPickUpLoot",
      "DeathTime",
      "FallDistance",
      "FallFlying",
      "Fire",
      "HandDropChances",
      "HandItems",
      "HurtByTimestamp",
      "HurtTime",
      "LeftHanded",
      "Motion",
      "NoGravity",
      "OnGround",
      "PortalCooldown",
      "Pos",
      "Rotation",
      "SleepingX",
      "SleepingY",
      "SleepingZ",
      "CannotEnterHiveTicks",
      "TicksSincePollination",
      "CropsGrownSincePollination",
      "hive_pos",
      "Passengers",
      "leash",
      "UUID"
   );
   public static final int MAX_OCCUPANTS = 3;
   private static final int MIN_TICKS_BEFORE_REENTERING_HIVE = 400;
   private static final int MIN_OCCUPATION_TICKS_NECTAR = 2400;
   public static final int MIN_OCCUPATION_TICKS_NECTARLESS = 600;
   private final List<BeehiveBlockEntity.BeeData> stored = Lists.newArrayList();
   @Nullable
   private BlockPos savedFlowerPos;

   public BeehiveBlockEntity(BlockPos p_155134_, BlockState p_155135_) {
      super(BlockEntityType.BEEHIVE, p_155134_, p_155135_);
   }

   @Override
   public void setChanged() {
      if (this.isFireNearby()) {
         this.emptyAllLivingFromHive(null, this.level.getBlockState(this.getBlockPos()), BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
      }

      super.setChanged();
   }

   public boolean isFireNearby() {
      if (this.level == null) {
         return false;
      } else {
         for (BlockPos $$0 : BlockPos.betweenClosed(this.worldPosition.offset(-1, -1, -1), this.worldPosition.offset(1, 1, 1))) {
            if (this.level.getBlockState($$0).getBlock() instanceof FireBlock) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean isEmpty() {
      return this.stored.isEmpty();
   }

   public boolean isFull() {
      return this.stored.size() == 3;
   }

   public void emptyAllLivingFromHive(@Nullable Player p_58749_, BlockState p_58750_, BeehiveBlockEntity.BeeReleaseStatus p_58751_) {
      List<Entity> $$3 = this.releaseAllOccupants(p_58750_, p_58751_);
      if (p_58749_ != null) {
         for (Entity $$4 : $$3) {
            if ($$4 instanceof Bee $$5 && p_58749_.position().distanceToSqr($$4.position()) <= 16.0) {
               if (!this.isSedated()) {
                  $$5.setTarget(p_58749_);
               } else {
                  $$5.setStayOutOfHiveCountdown(400);
               }
            }
         }
      }
   }

   private List<Entity> releaseAllOccupants(BlockState p_58760_, BeehiveBlockEntity.BeeReleaseStatus p_58761_) {
      List<Entity> $$2 = Lists.newArrayList();
      this.stored.removeIf(p_330138_ -> releaseOccupant(this.level, this.worldPosition, p_58760_, p_330138_.toOccupant(), $$2, p_58761_, this.savedFlowerPos));
      if (!$$2.isEmpty()) {
         super.setChanged();
      }

      return $$2;
   }

   @VisibleForDebug
   public int getOccupantCount() {
      return this.stored.size();
   }

   public static int getHoneyLevel(BlockState p_58753_) {
      return p_58753_.getValue(BeehiveBlock.HONEY_LEVEL);
   }

   @VisibleForDebug
   public boolean isSedated() {
      return CampfireBlock.isSmokeyPos(this.level, this.getBlockPos());
   }

   public void addOccupant(Entity p_58742_) {
      if (this.stored.size() < 3) {
         p_58742_.stopRiding();
         p_58742_.ejectPassengers();
         this.storeBee(BeehiveBlockEntity.Occupant.of(p_58742_));
         if (this.level != null) {
            if (p_58742_ instanceof Bee $$1 && $$1.hasSavedFlowerPos() && (!this.hasSavedFlowerPos() || this.level.random.nextBoolean())) {
               this.savedFlowerPos = $$1.getSavedFlowerPos();
            }

            BlockPos $$2 = this.getBlockPos();
            this.level.playSound(null, (double)$$2.getX(), (double)$$2.getY(), (double)$$2.getZ(), SoundEvents.BEEHIVE_ENTER, SoundSource.BLOCKS, 1.0F, 1.0F);
            this.level.gameEvent(GameEvent.BLOCK_CHANGE, $$2, GameEvent.Context.of(p_58742_, this.getBlockState()));
         }

         p_58742_.discard();
         super.setChanged();
      }
   }

   public void storeBee(BeehiveBlockEntity.Occupant p_330820_) {
      this.stored.add(new BeehiveBlockEntity.BeeData(p_330820_));
   }

   private static boolean releaseOccupant(
      Level p_155137_,
      BlockPos p_155138_,
      BlockState p_155139_,
      BeehiveBlockEntity.Occupant p_332184_,
      @Nullable List<Entity> p_155141_,
      BeehiveBlockEntity.BeeReleaseStatus p_155142_,
      @Nullable BlockPos p_155143_
   ) {
      if ((p_155137_.isNight() || p_155137_.isRaining()) && p_155142_ != BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY) {
         return false;
      } else {
         Direction $$7 = p_155139_.getValue(BeehiveBlock.FACING);
         BlockPos $$8 = p_155138_.relative($$7);
         boolean $$9 = !p_155137_.getBlockState($$8).getCollisionShape(p_155137_, $$8).isEmpty();
         if ($$9 && p_155142_ != BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY) {
            return false;
         } else {
            Entity $$10 = p_332184_.createEntity(p_155137_, p_155138_);
            if ($$10 != null) {
               if ($$10 instanceof Bee $$11) {
                  if (p_155143_ != null && !$$11.hasSavedFlowerPos() && p_155137_.random.nextFloat() < 0.9F) {
                     $$11.setSavedFlowerPos(p_155143_);
                  }

                  if (p_155142_ == BeehiveBlockEntity.BeeReleaseStatus.HONEY_DELIVERED) {
                     $$11.dropOffNectar();
                     if (p_155139_.is(BlockTags.BEEHIVES, p_202037_ -> p_202037_.hasProperty(BeehiveBlock.HONEY_LEVEL))) {
                        int $$12 = getHoneyLevel(p_155139_);
                        if ($$12 < 5) {
                           int $$13 = p_155137_.random.nextInt(100) == 0 ? 2 : 1;
                           if ($$12 + $$13 > 5) {
                              $$13--;
                           }

                           p_155137_.setBlockAndUpdate(p_155138_, p_155139_.setValue(BeehiveBlock.HONEY_LEVEL, $$12 + $$13));
                        }
                     }
                  }

                  if (p_155141_ != null) {
                     p_155141_.add($$11);
                  }

                  float $$14 = $$10.getBbWidth();
                  double $$15 = $$9 ? 0.0 : 0.55 + $$14 / 2.0F;
                  double $$16 = p_155138_.getX() + 0.5 + $$15 * $$7.getStepX();
                  double $$17 = p_155138_.getY() + 0.5 - $$10.getBbHeight() / 2.0F;
                  double $$18 = p_155138_.getZ() + 0.5 + $$15 * $$7.getStepZ();
                  $$10.moveTo($$16, $$17, $$18, $$10.getYRot(), $$10.getXRot());
               }

               p_155137_.playSound(null, p_155138_, SoundEvents.BEEHIVE_EXIT, SoundSource.BLOCKS, 1.0F, 1.0F);
               p_155137_.gameEvent(GameEvent.BLOCK_CHANGE, p_155138_, GameEvent.Context.of($$10, p_155137_.getBlockState(p_155138_)));
               return p_155137_.addFreshEntity($$10);
            } else {
               return false;
            }
         }
      }
   }

   private boolean hasSavedFlowerPos() {
      return this.savedFlowerPos != null;
   }

   private static void tickOccupants(
      Level p_155150_, BlockPos p_155151_, BlockState p_155152_, List<BeehiveBlockEntity.BeeData> p_155153_, @Nullable BlockPos p_155154_
   ) {
      boolean $$5 = false;
      Iterator<BeehiveBlockEntity.BeeData> $$6 = p_155153_.iterator();

      while ($$6.hasNext()) {
         BeehiveBlockEntity.BeeData $$7 = $$6.next();
         if ($$7.tick()) {
            BeehiveBlockEntity.BeeReleaseStatus $$8 = $$7.hasNectar()
               ? BeehiveBlockEntity.BeeReleaseStatus.HONEY_DELIVERED
               : BeehiveBlockEntity.BeeReleaseStatus.BEE_RELEASED;
            if (releaseOccupant(p_155150_, p_155151_, p_155152_, $$7.toOccupant(), null, $$8, p_155154_)) {
               $$5 = true;
               $$6.remove();
            }
         }
      }

      if ($$5) {
         setChanged(p_155150_, p_155151_, p_155152_);
      }
   }

   public static void serverTick(Level p_155145_, BlockPos p_155146_, BlockState p_155147_, BeehiveBlockEntity p_155148_) {
      tickOccupants(p_155145_, p_155146_, p_155147_, p_155148_.stored, p_155148_.savedFlowerPos);
      if (!p_155148_.stored.isEmpty() && p_155145_.getRandom().nextDouble() < 0.005) {
         double $$4 = p_155146_.getX() + 0.5;
         double $$5 = p_155146_.getY();
         double $$6 = p_155146_.getZ() + 0.5;
         p_155145_.playSound(null, $$4, $$5, $$6, SoundEvents.BEEHIVE_WORK, SoundSource.BLOCKS, 1.0F, 1.0F);
      }

      DebugPackets.sendHiveInfo(p_155145_, p_155146_, p_155147_, p_155148_);
   }

   @Override
   protected void loadAdditional(CompoundTag p_338675_, HolderLookup.Provider p_338666_) {
      super.loadAdditional(p_338675_, p_338666_);
      this.stored.clear();
      if (p_338675_.contains("bees")) {
         BeehiveBlockEntity.Occupant.LIST_CODEC
            .parse(NbtOps.INSTANCE, p_338675_.get("bees"))
            .resultOrPartial(p_330133_ -> LOGGER.error("Failed to parse bees: '{}'", p_330133_))
            .ifPresent(p_330134_ -> p_330134_.forEach(this::storeBee));
      }

      this.savedFlowerPos = NbtUtils.readBlockPos(p_338675_, "flower_pos").orElse(null);
   }

   @Override
   protected void saveAdditional(CompoundTag p_187467_, HolderLookup.Provider p_324426_) {
      super.saveAdditional(p_187467_, p_324426_);
      p_187467_.put("bees", (Tag)BeehiveBlockEntity.Occupant.LIST_CODEC.encodeStart(NbtOps.INSTANCE, this.getBees()).getOrThrow());
      if (this.hasSavedFlowerPos()) {
         p_187467_.put("flower_pos", NbtUtils.writeBlockPos(this.savedFlowerPos));
      }
   }

   @Override
   protected void applyImplicitComponents(BlockEntity.DataComponentInput p_338335_) {
      super.applyImplicitComponents(p_338335_);
      this.stored.clear();
      List<BeehiveBlockEntity.Occupant> $$1 = p_338335_.getOrDefault(DataComponents.BEES, List.of());
      $$1.forEach(this::storeBee);
   }

   @Override
   protected void collectImplicitComponents(DataComponentMap.Builder p_338773_) {
      super.collectImplicitComponents(p_338773_);
      p_338773_.set(DataComponents.BEES, this.getBees());
   }

   @Override
   public void removeComponentsFromTag(CompoundTag p_331127_) {
      super.removeComponentsFromTag(p_331127_);
      p_331127_.remove("bees");
   }

   private List<BeehiveBlockEntity.Occupant> getBees() {
      return this.stored.stream().map(BeehiveBlockEntity.BeeData::toOccupant).toList();
   }

   static class BeeData {
      private final BeehiveBlockEntity.Occupant occupant;
      private int ticksInHive;

      BeeData(BeehiveBlockEntity.Occupant p_331832_) {
         this.occupant = p_331832_;
         this.ticksInHive = p_331832_.ticksInHive();
      }

      public boolean tick() {
         return this.ticksInHive++ > this.occupant.minTicksInHive;
      }

      public BeehiveBlockEntity.Occupant toOccupant() {
         return new BeehiveBlockEntity.Occupant(this.occupant.entityData, this.ticksInHive, this.occupant.minTicksInHive);
      }

      public boolean hasNectar() {
         return this.occupant.entityData.getUnsafe().getBoolean("HasNectar");
      }
   }

   public static enum BeeReleaseStatus {
      HONEY_DELIVERED,
      BEE_RELEASED,
      EMERGENCY;
   }

   public record Occupant(CustomData entityData, int ticksInHive, int minTicksInHive) {
      public static final Codec<BeehiveBlockEntity.Occupant> CODEC = RecordCodecBuilder.create(
         p_337984_ -> p_337984_.group(
               CustomData.CODEC.optionalFieldOf("entity_data", CustomData.EMPTY).forGetter(BeehiveBlockEntity.Occupant::entityData),
               Codec.INT.fieldOf("ticks_in_hive").forGetter(BeehiveBlockEntity.Occupant::ticksInHive),
               Codec.INT.fieldOf("min_ticks_in_hive").forGetter(BeehiveBlockEntity.Occupant::minTicksInHive)
            )
            .apply(p_337984_, BeehiveBlockEntity.Occupant::new)
      );
      public static final Codec<List<BeehiveBlockEntity.Occupant>> LIST_CODEC = CODEC.listOf();
      public static final StreamCodec<ByteBuf, BeehiveBlockEntity.Occupant> STREAM_CODEC = StreamCodec.composite(
         CustomData.STREAM_CODEC,
         BeehiveBlockEntity.Occupant::entityData,
         ByteBufCodecs.VAR_INT,
         BeehiveBlockEntity.Occupant::ticksInHive,
         ByteBufCodecs.VAR_INT,
         BeehiveBlockEntity.Occupant::minTicksInHive,
         BeehiveBlockEntity.Occupant::new
      );

      public static BeehiveBlockEntity.Occupant of(Entity p_331485_) {
         CompoundTag $$1 = new CompoundTag();
         p_331485_.save($$1);
         BeehiveBlockEntity.IGNORED_BEE_TAGS.forEach($$1::remove);
         boolean $$2 = $$1.getBoolean("HasNectar");
         return new BeehiveBlockEntity.Occupant(CustomData.of($$1), 0, $$2 ? 2400 : 600);
      }

      public static BeehiveBlockEntity.Occupant create(int p_331115_) {
         CompoundTag $$1 = new CompoundTag();
         $$1.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.BEE).toString());
         return new BeehiveBlockEntity.Occupant(CustomData.of($$1), p_331115_, 600);
      }

      @Nullable
      public Entity createEntity(Level p_331790_, BlockPos p_330712_) {
         CompoundTag $$2 = this.entityData.copyTag();
         BeehiveBlockEntity.IGNORED_BEE_TAGS.forEach($$2::remove);
         Entity $$3 = EntityType.loadEntityRecursive($$2, p_331790_, p_331097_ -> p_331097_);
         if ($$3 != null && $$3.getType().is(EntityTypeTags.BEEHIVE_INHABITORS)) {
            $$3.setNoGravity(true);
            if ($$3 instanceof Bee $$4) {
               $$4.setHivePos(p_330712_);
               setBeeReleaseData(this.ticksInHive, $$4);
            }

            return $$3;
         } else {
            return null;
         }
      }

      private static void setBeeReleaseData(int p_331728_, Bee p_331988_) {
         int $$2 = p_331988_.getAge();
         if ($$2 < 0) {
            p_331988_.setAge(Math.min(0, $$2 + p_331728_));
         } else if ($$2 > 0) {
            p_331988_.setAge(Math.max(0, $$2 - p_331728_));
         }

         p_331988_.setInLoveTime(Math.max(0, p_331988_.getInLoveTime() - p_331728_));
      }
   }
}
