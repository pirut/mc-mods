package net.neoforged.neoforge.common;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

public class BooleanAttribute extends Attribute {
   public BooleanAttribute(String descriptionId, boolean defaultValue) {
      super(descriptionId, defaultValue ? 1.0 : 0.0);
   }

   @Override
   public double sanitizeValue(double value) {
      if (Double.isNaN(value)) {
         return 0.0;
      } else {
         return value > 0.0 ? 1.0 : 0.0;
      }
   }

   @Override
   public MutableComponent toValueComponent(@Nullable AttributeModifier.Operation op, double value, TooltipFlag flag) {
      if (op == null) {
         return Component.translatable("neoforge.value.boolean." + (value > 0.0 ? "enabled" : "disabled"));
      } else if (op == AttributeModifier.Operation.ADD_VALUE && value > 0.0) {
         return Component.translatable("neoforge.value.boolean.enable");
      } else {
         return op == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL && (int)value == -1
            ? Component.translatable("neoforge.value.boolean.disable")
            : Component.translatable("neoforge.value.boolean.invalid");
      }
   }

   @Override
   public MutableComponent toComponent(AttributeModifier modif, TooltipFlag flag) {
      double value = modif.amount();
      ChatFormatting color = this.getStyle(value > 0.0);
      MutableComponent comp = Component.translatable(
            "neoforge.modifier.bool", this.toValueComponent(modif.operation(), value, flag), Component.translatable(this.getDescriptionId())
         )
         .withStyle(color);
      return comp.append(this.getDebugInfo(modif, flag));
   }
}
