package net.minecraft.client.renderer.item.properties.numeric;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class NeedleDirectionHelper {
   private final boolean wobble;

   protected NeedleDirectionHelper(boolean p_388777_) {
      this.wobble = p_388777_;
   }

   public float get(ItemStack p_386635_, @Nullable ClientLevel p_387256_, @Nullable ItemOwner p_436053_, int p_387873_) {
      if (p_436053_ == null) {
         p_436053_ = p_386635_.getEntityRepresentation();
      }

      if (p_436053_ == null) {
         return 0.0F;
      } else {
         if (p_387256_ == null && p_436053_.level() instanceof ClientLevel clientlevel) {
            p_387256_ = clientlevel;
         }

         return p_387256_ == null ? 0.0F : this.calculate(p_386635_, p_387256_, p_387873_, p_436053_);
      }
   }

   protected abstract float calculate(ItemStack var1, ClientLevel var2, int var3, ItemOwner var4);

   protected boolean wobble() {
      return this.wobble;
   }

   protected NeedleDirectionHelper.Wobbler newWobbler(float p_387658_) {
      return this.wobble ? standardWobbler(p_387658_) : nonWobbler();
   }

   public static NeedleDirectionHelper.Wobbler standardWobbler(final float p_388906_) {
      return new NeedleDirectionHelper.Wobbler() {
         private float rotation;
         private float deltaRotation;
         private long lastUpdateTick;

         @Override
         public float rotation() {
            return this.rotation;
         }

         @Override
         public boolean shouldUpdate(long p_387925_) {
            return this.lastUpdateTick != p_387925_;
         }

         @Override
         public void update(long p_387682_, float p_388243_) {
            this.lastUpdateTick = p_387682_;
            float f = Mth.positiveModulo(p_388243_ - this.rotation + 0.5F, 1.0F) - 0.5F;
            this.deltaRotation += f * 0.1F;
            this.deltaRotation = this.deltaRotation * p_388906_;
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
         public boolean shouldUpdate(long p_388006_) {
            return true;
         }

         @Override
         public void update(long p_388810_, float p_387609_) {
            this.targetValue = p_387609_;
         }
      };
   }

   @OnlyIn(Dist.CLIENT)
   public interface Wobbler {
      float rotation();

      boolean shouldUpdate(long var1);

      void update(long var1, float var3);
   }
}
