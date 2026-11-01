package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CompassAngleState extends NeedleDirectionHelper {
   public static final MapCodec<CompassAngleState> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_387422_ -> p_387422_.group(
            Codec.BOOL.optionalFieldOf("wobble", true).forGetter(NeedleDirectionHelper::wobble),
            CompassAngleState.CompassTarget.CODEC.fieldOf("target").forGetter(CompassAngleState::target)
         )
         .apply(p_387422_, CompassAngleState::new)
   );
   private final NeedleDirectionHelper.Wobbler wobbler;
   private final NeedleDirectionHelper.Wobbler noTargetWobbler;
   private final CompassAngleState.CompassTarget compassTarget;
   private final RandomSource random = RandomSource.create();

   public CompassAngleState(boolean p_388844_, CompassAngleState.CompassTarget p_387613_) {
      super(p_388844_);
      this.wobbler = this.newWobbler(0.8F);
      this.noTargetWobbler = this.newWobbler(0.8F);
      this.compassTarget = p_387613_;
   }

   @Override
   protected float calculate(ItemStack p_388108_, ClientLevel p_387750_, int p_388073_, ItemOwner p_435973_) {
      GlobalPos globalpos = this.compassTarget.get(p_387750_, p_388108_, p_435973_);
      long i = p_387750_.getGameTime();
      return !isValidCompassTargetPos(p_435973_, globalpos)
         ? this.getRandomlySpinningRotation(p_388073_, i)
         : this.getRotationTowardsCompassTarget(p_435973_, i, globalpos.pos());
   }

   private float getRandomlySpinningRotation(int p_388932_, long p_387198_) {
      if (this.noTargetWobbler.shouldUpdate(p_387198_)) {
         this.noTargetWobbler.update(p_387198_, this.random.nextFloat());
      }

      float f = this.noTargetWobbler.rotation() + hash(p_388932_) / 2.1474836E9F;
      return Mth.positiveModulo(f, 1.0F);
   }

   private float getRotationTowardsCompassTarget(ItemOwner p_432821_, long p_387654_, BlockPos p_388263_) {
      float f = (float)getAngleFromEntityToPos(p_432821_, p_388263_);
      float f1 = getWrappedVisualRotationY(p_432821_);
      float f2;
      if (p_432821_.asLivingEntity() instanceof Player player && player.isLocalPlayer() && player.level().tickRateManager().runsNormally()) {
         if (this.wobbler.shouldUpdate(p_387654_)) {
            this.wobbler.update(p_387654_, 0.5F - (f1 - 0.25F));
         }

         f2 = f + this.wobbler.rotation();
      } else {
         f2 = 0.5F - (f1 - 0.25F - f);
      }

      return Mth.positiveModulo(f2, 1.0F);
   }

   private static boolean isValidCompassTargetPos(ItemOwner p_434341_, @Nullable GlobalPos p_387891_) {
      return p_387891_ != null && p_387891_.dimension() == p_434341_.level().dimension() && !(p_387891_.pos().distToCenterSqr(p_434341_.position()) < 1.0E-5F);
   }

   private static double getAngleFromEntityToPos(ItemOwner p_432840_, BlockPos p_387426_) {
      Vec3 vec3 = Vec3.atCenterOf(p_387426_);
      Vec3 vec31 = p_432840_.position();
      return Math.atan2(vec3.z() - vec31.z(), vec3.x() - vec31.x()) / (float) (Math.PI * 2);
   }

   private static float getWrappedVisualRotationY(ItemOwner p_432979_) {
      return Mth.positiveModulo(p_432979_.getVisualRotationYInDegrees() / 360.0F, 1.0F);
   }

   private static int hash(int p_387430_) {
      return p_387430_ * 1327217883;
   }

   protected CompassAngleState.CompassTarget target() {
      return this.compassTarget;
   }

   @OnlyIn(Dist.CLIENT)
   public static enum CompassTarget implements StringRepresentable {
      NONE("none") {
         @Nullable
         @Override
         public GlobalPos get(ClientLevel p_388090_, ItemStack p_388593_, @Nullable ItemOwner p_434343_) {
            return null;
         }
      },
      LODESTONE("lodestone") {
         @Nullable
         @Override
         public GlobalPos get(ClientLevel p_388590_, ItemStack p_387822_, @Nullable ItemOwner p_436068_) {
            LodestoneTracker lodestonetracker = p_387822_.get(DataComponents.LODESTONE_TRACKER);
            return lodestonetracker != null ? lodestonetracker.target().orElse(null) : null;
         }
      },
      SPAWN("spawn") {
         @Override
         public GlobalPos get(ClientLevel p_386460_, ItemStack p_387401_, @Nullable ItemOwner p_434467_) {
            return p_386460_.getRespawnData().globalPos();
         }
      },
      RECOVERY("recovery") {
         @Nullable
         @Override
         public GlobalPos get(ClientLevel p_390456_, ItemStack p_390419_, @Nullable ItemOwner p_435538_) {
            return (p_435538_ == null ? null : p_435538_.asLivingEntity()) instanceof Player player ? player.getLastDeathLocation().orElse(null) : null;
         }
      };

      public static final Codec<CompassAngleState.CompassTarget> CODEC = StringRepresentable.fromEnum(CompassAngleState.CompassTarget::values);
      private final String name;

      private CompassTarget(String p_388295_) {
         this.name = p_388295_;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      @Nullable
      abstract GlobalPos get(ClientLevel var1, ItemStack var2, @Nullable ItemOwner var3);
   }
}
