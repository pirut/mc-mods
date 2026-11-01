package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class CompassAngleState extends NeedleDirectionHelper {
   public static final MapCodec<CompassAngleState> MAP_CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            Codec.BOOL.optionalFieldOf("wobble", true).forGetter(NeedleDirectionHelper::wobble),
            CompassAngleState.CompassTarget.CODEC.fieldOf("target").forGetter(CompassAngleState::target)
         )
         .apply(instance, CompassAngleState::new)
   );
   private final NeedleDirectionHelper.Wobbler wobbler;
   private final NeedleDirectionHelper.Wobbler noTargetWobbler;
   private final CompassAngleState.CompassTarget compassTarget;
   private final RandomSource random = RandomSource.create();

   public CompassAngleState(boolean wobble, CompassAngleState.CompassTarget compassTarget) {
      super(wobble);
      this.wobbler = this.newWobbler(0.8F);
      this.noTargetWobbler = this.newWobbler(0.8F);
      this.compassTarget = compassTarget;
   }

   @Override
   protected float calculate(ItemStack stack, ClientLevel level, int seed, ItemOwner owner) {
      GlobalPos globalPos = this.compassTarget.get(level, stack, owner);
      long l = level.getGameTime();
      return !isValidCompassTargetPos(owner, globalPos)
         ? this.getRandomlySpinningRotation(seed, l)
         : this.getRotationTowardsCompassTarget(owner, l, globalPos.pos());
   }

   private float getRandomlySpinningRotation(int seed, long gameTime) {
      if (this.noTargetWobbler.shouldUpdate(gameTime)) {
         this.noTargetWobbler.update(gameTime, this.random.nextFloat());
      }

      float f = this.noTargetWobbler.rotation() + hash(seed) / 2.1474836E9F;
      return Mth.positiveModulo(f, 1.0F);
   }

   private float getRotationTowardsCompassTarget(ItemOwner owner, long gameTime, BlockPos pos) {
      float f = (float)getAngleFromEntityToPos(owner, pos);
      float g = getWrappedVisualRotationY(owner);
      float h;
      if (owner.asLivingEntity() instanceof Player player && player.isLocalPlayer() && player.level().tickRateManager().runsNormally()) {
         if (this.wobbler.shouldUpdate(gameTime)) {
            this.wobbler.update(gameTime, 0.5F - (g - 0.25F));
         }

         h = f + this.wobbler.rotation();
      } else {
         h = 0.5F - (g - 0.25F - f);
      }

      return Mth.positiveModulo(h, 1.0F);
   }

   private static boolean isValidCompassTargetPos(ItemOwner owner, @Nullable GlobalPos pos) {
      return pos != null && pos.dimension() == owner.level().dimension() && !(pos.pos().distToCenterSqr(owner.position()) < 1.0E-5F);
   }

   private static double getAngleFromEntityToPos(ItemOwner owner, BlockPos pos) {
      Vec3 vec3 = Vec3.atCenterOf(pos);
      Vec3 vec32 = owner.position();
      return Math.atan2(vec3.z() - vec32.z(), vec3.x() - vec32.x()) / (float) (Math.PI * 2);
   }

   private static float getWrappedVisualRotationY(ItemOwner owner) {
      return Mth.positiveModulo(owner.getVisualRotationYInDegrees() / 360.0F, 1.0F);
   }

   private static int hash(int seed) {
      return seed * 1327217883;
   }

   protected CompassAngleState.CompassTarget target() {
      return this.compassTarget;
   }

   @Environment(EnvType.CLIENT)
   public static enum CompassTarget implements StringRepresentable {
      NONE("none") {
         @Nullable
         @Override
         public GlobalPos get(ClientLevel level, ItemStack stack, @Nullable ItemOwner owner) {
            return null;
         }
      },
      LODESTONE("lodestone") {
         @Nullable
         @Override
         public GlobalPos get(ClientLevel level, ItemStack stack, @Nullable ItemOwner owner) {
            LodestoneTracker lodestoneTracker = stack.get(DataComponents.LODESTONE_TRACKER);
            return lodestoneTracker != null ? lodestoneTracker.target().orElse(null) : null;
         }
      },
      SPAWN("spawn") {
         @Override
         public GlobalPos get(ClientLevel level, ItemStack stack, @Nullable ItemOwner owner) {
            return level.getRespawnData().globalPos();
         }
      },
      RECOVERY("recovery") {
         @Nullable
         @Override
         public GlobalPos get(ClientLevel level, ItemStack stack, @Nullable ItemOwner owner) {
            return (owner == null ? null : owner.asLivingEntity()) instanceof Player player ? player.getLastDeathLocation().orElse(null) : null;
         }
      };

      public static final Codec<CompassAngleState.CompassTarget> CODEC = StringRepresentable.fromEnum(CompassAngleState.CompassTarget::values);
      private final String name;

      CompassTarget(final String name) {
         this.name = name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      @Nullable
      abstract GlobalPos get(ClientLevel level, ItemStack stack, @Nullable ItemOwner owner);
   }
}
