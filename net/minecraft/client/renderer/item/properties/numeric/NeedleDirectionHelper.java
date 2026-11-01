package net.minecraft.client.renderer.item.properties.numeric;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class NeedleDirectionHelper {
   private final boolean wobble;

   protected NeedleDirectionHelper(boolean wobble) {
      this.wobble = wobble;
   }

   public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
      if (owner == null) {
         owner = stack.getEntityRepresentation();
      }

      if (owner == null) {
         return 0.0F;
      } else {
         if (level == null && owner.level() instanceof ClientLevel clientLevel) {
            level = clientLevel;
         }

         return level == null ? 0.0F : this.calculate(stack, level, seed, owner);
      }
   }

   protected abstract float calculate(ItemStack stack, ClientLevel level, int seed, ItemOwner owner);

   protected boolean wobble() {
      return this.wobble;
   }

   protected NeedleDirectionHelper.Wobbler newWobbler(float scale) {
      return this.wobble ? standardWobbler(scale) : nonWobbler();
   }

   public static NeedleDirectionHelper.Wobbler standardWobbler(float scale) {
      return new NeedleDirectionHelper.Wobbler() {
         private float rotation;
         private float deltaRotation;
         private long lastUpdateTick;

         @Override
         public float rotation() {
            return this.rotation;
         }

         @Override
         public boolean shouldUpdate(long gameTime) {
            return this.lastUpdateTick != gameTime;
         }

         @Override
         public void update(long gameTime, float targetValue) {
            this.lastUpdateTick = gameTime;
            float f = Mth.positiveModulo(targetValue - this.rotation + 0.5F, 1.0F) - 0.5F;
            this.deltaRotation += f * 0.1F;
            this.deltaRotation = this.deltaRotation * scale;
            this.rotation = Mth.positiveModulo(this.rotation + this.deltaRotation, 1.0F);
         }
      };
   }

   public static NeedleDirectionHelper.Wobbler nonWobbler() {
      return new NeedleDirectionHelper.Wobbler() {
         private float targetValue;

         @Override
         public float rotation() {
            return this.targetValue;
         }

         @Override
         public boolean shouldUpdate(long gameTime) {
            return true;
         }

         @Override
         public void update(long gameTime, float targetValue) {
            this.targetValue = targetValue;
         }
      };
   }

   @Environment(EnvType.CLIENT)
   public interface Wobbler {
      float rotation();

      boolean shouldUpdate(long gameTime);

      void update(long gameTime, float targetValue);
   }
}
