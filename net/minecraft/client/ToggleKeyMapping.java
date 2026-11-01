package net.minecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.BooleanSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ToggleKeyMapping extends KeyMapping {
   private final BooleanSupplier needsToggle;
   private boolean releasedByScreenWhenDown;
   private final boolean shouldRestore;

   public ToggleKeyMapping(String name, int key, KeyMapping.Category category, BooleanSupplier needsToggle, boolean shouldRestore) {
      this(name, InputConstants.Type.KEYSYM, key, category, needsToggle, shouldRestore);
   }

   public ToggleKeyMapping(String name, InputConstants.Type type, int key, KeyMapping.Category category, BooleanSupplier needsToggle, boolean shouldRestore) {
      super(name, type, key, category);
      this.needsToggle = needsToggle;
      this.shouldRestore = shouldRestore;
   }

   @Override
   protected boolean shouldSetOnIngameFocus() {
      return super.shouldSetOnIngameFocus() && !this.needsToggle.getAsBoolean();
   }

   @Override
   public void setDown(boolean value) {
      if (this.needsToggle.getAsBoolean()) {
         if (value) {
            super.setDown(!this.isDown());
         }
      } else {
         super.setDown(value);
      }
   }

   @Override
   protected void release() {
      if (this.needsToggle.getAsBoolean() && this.isDown() || this.releasedByScreenWhenDown) {
         this.releasedByScreenWhenDown = true;
      }

      this.reset();
   }

   public boolean shouldRestoreStateOnScreenClosed() {
      boolean bl = this.shouldRestore && this.needsToggle.getAsBoolean() && this.key.getType() == InputConstants.Type.KEYSYM && this.releasedByScreenWhenDown;
      this.releasedByScreenWhenDown = false;
      return bl;
   }

   protected void reset() {
      super.setDown(false);
   }
}
