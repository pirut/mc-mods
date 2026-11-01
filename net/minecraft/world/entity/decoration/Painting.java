package net.minecraft.world.entity.decoration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.VariantHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Painting extends HangingEntity implements VariantHolder<Holder<PaintingVariant>> {
   private static final EntityDataAccessor<Holder<PaintingVariant>> DATA_PAINTING_VARIANT_ID = SynchedEntityData.defineId(
      Painting.class, EntityDataSerializers.PAINTING_VARIANT
   );
   public static final MapCodec<Holder<PaintingVariant>> VARIANT_MAP_CODEC = PaintingVariant.CODEC.fieldOf("variant");
   public static final Codec<Holder<PaintingVariant>> VARIANT_CODEC = VARIANT_MAP_CODEC.codec();
   public static final float DEPTH = 0.0625F;

   public Painting(EntityType<? extends Painting> p_31904_, Level p_31905_) {
      super(p_31904_, p_31905_);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_326078_) {
      p_326078_.define(DATA_PAINTING_VARIANT_ID, this.registryAccess().registryOrThrow(Registries.PAINTING_VARIANT).getAny().orElseThrow());
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_218896_) {
      if (DATA_PAINTING_VARIANT_ID.equals(p_218896_)) {
         this.recalculateBoundingBox();
      }
   }

   public void setVariant(Holder<PaintingVariant> p_218892_) {
      this.entityData.set(DATA_PAINTING_VARIANT_ID, p_218892_);
   }

   public Holder<PaintingVariant> getVariant() {
      return this.entityData.get(DATA_PAINTING_VARIANT_ID);
   }

   public static Optional<Painting> create(Level p_218888_, BlockPos p_218889_, Direction p_218890_) {
      Painting $$3 = new Painting(p_218888_, p_218889_);
      List<Holder<PaintingVariant>> $$4 = new ArrayList<>();
      p_218888_.registryAccess().registryOrThrow(Registries.PAINTING_VARIANT).getTagOrEmpty(PaintingVariantTags.PLACEABLE).forEach($$4::add);
      if ($$4.isEmpty()) {
         return Optional.empty();
      } else {
         $$3.setDirection(p_218890_);
         $$4.removeIf(p_344343_ -> {
            $$3.setVariant((Holder<PaintingVariant>)p_344343_);
            return !$$3.survives();
         });
         if ($$4.isEmpty()) {
            return Optional.empty();
         } else {
            int $$5 = $$4.stream().mapToInt(Painting::variantArea).max().orElse(0);
            $$4.removeIf(p_218883_ -> variantArea((Holder<PaintingVariant>)p_218883_) < $$5);
            Optional<Holder<PaintingVariant>> $$6 = Util.getRandomSafe($$4, $$3.random);
            if ($$6.isEmpty()) {
               return Optional.empty();
            } else {
               $$3.setVariant($$6.get());
               $$3.setDirection(p_218890_);
               return Optional.of($$3);
            }
         }
      }
   }

   private static int variantArea(Holder<PaintingVariant> p_218899_) {
      return p_218899_.value().area();
   }

   private Painting(Level p_218874_, BlockPos p_218875_) {
      super(EntityType.PAINTING, p_218874_, p_218875_);
   }

   public Painting(Level p_218877_, BlockPos p_218878_, Direction p_218879_, Holder<PaintingVariant> p_218880_) {
      this(p_218877_, p_218878_);
      this.setVariant(p_218880_);
      this.setDirection(p_218879_);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag p_31935_) {
      VARIANT_CODEC.encodeStart(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), this.getVariant())
         .ifSuccess(p_330061_ -> p_31935_.merge((CompoundTag)p_330061_));
      p_31935_.putByte("facing", (byte)this.direction.get2DDataValue());
      super.addAdditionalSaveData(p_31935_);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag p_31927_) {
      VARIANT_CODEC.parse(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), p_31927_).ifSuccess(this::setVariant);
      this.direction = Direction.from2DDataValue(p_31927_.getByte("facing"));
      super.readAdditionalSaveData(p_31927_);
      this.setDirection(this.direction);
   }

   @Override
   protected AABB calculateBoundingBox(BlockPos p_344831_, Direction p_345917_) {
      float $$2 = 0.46875F;
      Vec3 $$3 = Vec3.atCenterOf(p_344831_).relative(p_345917_, -0.46875);
      PaintingVariant $$4 = this.getVariant().value();
      double $$5 = this.offsetForPaintingSize($$4.width());
      double $$6 = this.offsetForPaintingSize($$4.height());
      Direction $$7 = p_345917_.getCounterClockWise();
      Vec3 $$8 = $$3.relative($$7, $$5).relative(Direction.UP, $$6);
      Direction.Axis $$9 = p_345917_.getAxis();
      double $$10 = $$9 == Direction.Axis.X ? 0.0625 : $$4.width();
      double $$11 = $$4.height();
      double $$12 = $$9 == Direction.Axis.Z ? 0.0625 : $$4.width();
      return AABB.ofSize($$8, $$10, $$11, $$12);
   }

   private double offsetForPaintingSize(int p_345028_) {
      return p_345028_ % 2 == 0 ? 0.5 : 0.0;
   }

   @Override
   public void dropItem(@Nullable Entity p_31925_) {
      if (this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
         this.playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);
         if (!(p_31925_ instanceof Player $$1 && $$1.hasInfiniteMaterials())) {
            this.spawnAtLocation(Items.PAINTING);
         }
      }
   }

   @Override
   public void playPlacementSound() {
      this.playSound(SoundEvents.PAINTING_PLACE, 1.0F, 1.0F);
   }

   @Override
   public void moveTo(double p_31929_, double p_31930_, double p_31931_, float p_31932_, float p_31933_) {
      this.setPos(p_31929_, p_31930_, p_31931_);
   }

   @Override
   public void lerpTo(double p_31917_, double p_31918_, double p_31919_, float p_31920_, float p_31921_, int p_31922_) {
      this.setPos(p_31917_, p_31918_, p_31919_);
   }

   @Override
   public Vec3 trackingPosition() {
      return Vec3.atLowerCornerOf(this.pos);
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity p_352062_) {
      return new ClientboundAddEntityPacket(this, this.direction.get3DDataValue(), this.getPos());
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket p_218894_) {
      super.recreateFromPacket(p_218894_);
      this.setDirection(Direction.from3DDataValue(p_218894_.getData()));
   }

   @Override
   public ItemStack getPickResult() {
      return new ItemStack(Items.PAINTING);
   }
}
