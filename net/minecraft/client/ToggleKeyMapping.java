package net.minecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.BooleanSupplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ToggleKeyMapping extends KeyMapping {
   private final BooleanSupplier needsToggle;
   private boolean releasedByScreenWhenDown;
   private final boolean shouldRestore;

   public ToggleKeyMapping(String p_435047_, int p_435994_, KeyMapping.Category p_446428_, BooleanSupplier p_433451_, boolean p_449970_) {
      this(p_435047_, InputConstants.Type.KEYSYM, p_435994_, p_446428_, p_433451_, p_449970_);
   }

   public ToggleKeyMapping(
      String p_92529_, InputConstants.Type p_449948_, int p_92530_, KeyMapping.Category p_447252_, BooleanSupplier p_92532_, boolean p_449692_
   ) {
      super(p_92529_, p_449948_, p_92530_, p_447252_);
      this.needsToggle = p_92532_;
      this.shouldRestore = p_449692_;
   }

   @Override
   protected boolean shouldSetOnIngameFocus() {
      return super.shouldSetOnIngameFocus() && !this.needsToggle.getAsBoolean();
   }

   @Override
   public void setDown(boolean p_92534_) {
      if (this.needsToggle.getAsBoolean()) {
         if (p_92534_ && this.isConflictContextAndModifierActive()) {
            super.setDown(!this.isDown());
         }
      } else {
         super.setDown(p_92534_);
      }
   }

   @Override
   public boolean isDown() {
      return this.isDown && (this.isConflictContextAndModifierActive() || this.needsToggle.getAsBoolean());
   }

   @Override
   protected void release() {
      if (this.needsToggle.getAsBoolean() && this.isDown() || this.releasedByScreenWhenDown) {
         this.releasedByScreenWhenDown = true;
      }

      this.reset();
   }

   public boolean shouldRestoreStateOnScreenClosed() {
      boolean flag = this.shouldRestore && this.needsToggle.getAsBoolean() && this.key.getType() == InputConstants.Type.KEYSYM && this.releasedByScreenWhenDown;
      this.releasedByScreenWhenDown = false;
      return flag;
   }

   protected void reset() {
      super.setDown(false);
   }
}
