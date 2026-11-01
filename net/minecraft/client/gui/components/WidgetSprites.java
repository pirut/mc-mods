package net.minecraft.client.gui.components;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record WidgetSprites(Identifier enabled, Identifier disabled, Identifier enabledFocused, Identifier disabledFocused) {
   public WidgetSprites(Identifier p_467594_) {
      this(p_467594_, p_467594_, p_467594_, p_467594_);
   }

   public WidgetSprites(Identifier p_469582_, Identifier p_467412_) {
      this(p_469582_, p_469582_, p_467412_, p_467412_);
   }

   public WidgetSprites(Identifier p_468307_, Identifier p_468954_, Identifier p_467537_) {
      this(p_468307_, p_468954_, p_467537_, p_468954_);
   }

   public Identifier get(boolean p_296269_, boolean p_295773_) {
      if (p_296269_) {
         return p_295773_ ? this.enabledFocused : this.enabled;
      } else {
         return p_295773_ ? this.disabledFocused : this.disabled;
      }
   }
}
