package net.neoforged.neoforge.common;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.extensions.IAttributeExtension;

public class PercentageAttribute extends RangedAttribute {
   protected final double scaleFactor;

   public PercentageAttribute(String pDescriptionId, double pDefaultValue, double pMin, double pMax, double scaleFactor) {
      super(pDescriptionId, pDefaultValue, pMin, pMax);
      this.scaleFactor = scaleFactor;
   }

   public PercentageAttribute(String pDescriptionId, double pDefaultValue, double pMin, double pMax) {
      this(pDescriptionId, pDefaultValue, pMin, pMax, 100.0);
   }

   @Override
   public MutableComponent toValueComponent(AttributeModifier.Operation op, double value, TooltipFlag flag) {
      return IAttributeExtension.isNullOrAddition(op)
         ? Component.translatable("neoforge.value.percent", FORMAT.format(value * this.scaleFactor))
         : Component.translatable("neoforge.value.percent", FORMAT.format(value * 100.0));
   }
}
