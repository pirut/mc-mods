package com.mojang.blaze3d.shaders;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Locale;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlendMode {
   @Nullable
   private static BlendMode lastApplied;
   private final int srcColorFactor;
   private final int srcAlphaFactor;
   private final int dstColorFactor;
   private final int dstAlphaFactor;
   private final int blendFunc;
   private final boolean separateBlend;
   private final boolean opaque;

   private BlendMode(boolean p_85519_, boolean p_85520_, int p_85521_, int p_85522_, int p_85523_, int p_85524_, int p_85525_) {
      this.separateBlend = p_85519_;
      this.srcColorFactor = p_85521_;
      this.dstColorFactor = p_85522_;
      this.srcAlphaFactor = p_85523_;
      this.dstAlphaFactor = p_85524_;
      this.opaque = p_85520_;
      this.blendFunc = p_85525_;
   }

   public BlendMode() {
      this(false, true, 1, 0, 1, 0, 32774);
   }

   public BlendMode(int p_85509_, int p_85510_, int p_85511_) {
      this(false, false, p_85509_, p_85510_, p_85509_, p_85510_, p_85511_);
   }

   public BlendMode(int p_85513_, int p_85514_, int p_85515_, int p_85516_, int p_85517_) {
      this(true, false, p_85513_, p_85514_, p_85515_, p_85516_, p_85517_);
   }

   public void apply() {
      if (!this.equals(lastApplied)) {
         if (lastApplied == null || this.opaque != lastApplied.isOpaque()) {
            lastApplied = this;
            if (this.opaque) {
               RenderSystem.disableBlend();
               return;
            }

            RenderSystem.enableBlend();
         }

         RenderSystem.blendEquation(this.blendFunc);
         if (this.separateBlend) {
            RenderSystem.blendFuncSeparate(this.srcColorFactor, this.dstColorFactor, this.srcAlphaFactor, this.dstAlphaFactor);
         } else {
            RenderSystem.blendFunc(this.srcColorFactor, this.dstColorFactor);
         }
      }
   }

   @Override
   public boolean equals(Object p_85533_) {
      if (this == p_85533_) {
         return true;
      } else if (!(p_85533_ instanceof BlendMode $$1)) {
         return false;
      } else if (this.blendFunc != $$1.blendFunc) {
         return false;
      } else if (this.dstAlphaFactor != $$1.dstAlphaFactor) {
         return false;
      } else if (this.dstColorFactor != $$1.dstColorFactor) {
         return false;
      } else if (this.opaque != $$1.opaque) {
         return false;
      } else if (this.separateBlend != $$1.separateBlend) {
         return false;
      } else {
         return this.srcAlphaFactor != $$1.srcAlphaFactor ? false : this.srcColorFactor == $$1.srcColorFactor;
      }
   }

   @Override
   public int hashCode() {
      int $$0 = this.srcColorFactor;
      $$0 = 31 * $$0 + this.srcAlphaFactor;
      $$0 = 31 * $$0 + this.dstColorFactor;
      $$0 = 31 * $$0 + this.dstAlphaFactor;
      $$0 = 31 * $$0 + this.blendFunc;
      $$0 = 31 * $$0 + (this.separateBlend ? 1 : 0);
      return 31 * $$0 + (this.opaque ? 1 : 0);
   }

   public boolean isOpaque() {
      return this.opaque;
   }

   public static int stringToBlendFunc(String p_85528_) {
      String $$1 = p_85528_.trim().toLowerCase(Locale.ROOT);
      if ("add".equals($$1)) {
         return 32774;
      } else if ("subtract".equals($$1)) {
         return 32778;
      } else if ("reversesubtract".equals($$1)) {
         return 32779;
      } else if ("reverse_subtract".equals($$1)) {
         return 32779;
      } else if ("min".equals($$1)) {
         return 32775;
      } else {
         return "max".equals($$1) ? 32776 : 32774;
      }
   }

   public static int stringToBlendFactor(String p_85531_) {
      String $$1 = p_85531_.trim().toLowerCase(Locale.ROOT);
      $$1 = $$1.replaceAll("_", "");
      $$1 = $$1.replaceAll("one", "1");
      $$1 = $$1.replaceAll("zero", "0");
      $$1 = $$1.replaceAll("minus", "-");
      if ("0".equals($$1)) {
         return 0;
      } else if ("1".equals($$1)) {
         return 1;
      } else if ("srccolor".equals($$1)) {
         return 768;
      } else if ("1-srccolor".equals($$1)) {
         return 769;
      } else if ("dstcolor".equals($$1)) {
         return 774;
      } else if ("1-dstcolor".equals($$1)) {
         return 775;
      } else if ("srcalpha".equals($$1)) {
         return 770;
      } else if ("1-srcalpha".equals($$1)) {
         return 771;
      } else if ("dstalpha".equals($$1)) {
         return 772;
      } else {
         return "1-dstalpha".equals($$1) ? 773 : -1;
      }
   }
}
