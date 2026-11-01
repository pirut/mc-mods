package net.neoforged.neoforge.common.extensions;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.NeoForgeConfig;
import net.neoforged.neoforge.common.util.AttributeUtil;
import org.jetbrains.annotations.Nullable;

public interface IAttributeExtension {
   DecimalFormat FORMAT = Util.make(new DecimalFormat("#.##"), fmt -> fmt.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT)));

   default MutableComponent toValueComponent(@Nullable AttributeModifier.Operation op, double value, TooltipFlag flag) {
      return isNullOrAddition(op)
         ? Component.translatable("neoforge.value.flat", FORMAT.format(value))
         : Component.translatable("neoforge.value.percent", FORMAT.format(value * 100.0));
   }

   default MutableComponent toComponent(AttributeModifier modif, TooltipFlag flag) {
      Attribute attr = this.self();
      double value = modif.amount();
      String key = value > 0.0 ? "neoforge.modifier.plus" : "neoforge.modifier.take";
      ChatFormatting color = attr.getStyle(value > 0.0);
      Component attrDesc = Component.translatable(attr.getDescriptionId());
      Component valueComp = this.toValueComponent(modif.operation(), value, flag);
      MutableComponent comp = Component.translatable(key, valueComp, attrDesc).withStyle(color);
      return comp.append(this.getDebugInfo(modif, flag));
   }

   default Component getDebugInfo(AttributeModifier modif, TooltipFlag flag) {
      Component debugInfo = CommonComponents.EMPTY;
      if (flag.isAdvanced() && NeoForgeConfig.COMMON.attributeAdvancedTooltipDebugInfo.get()) {
         double advValue = (modif.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL ? 1 : 0) + modif.amount();
         String valueStr = FORMAT.format(advValue);

         String txt = switch (modif.operation()) {
            case ADD_VALUE -> String.format(Locale.ROOT, advValue > 0.0 ? "[+%s]" : "[%s]", valueStr);
            case ADD_MULTIPLIED_BASE -> String.format(Locale.ROOT, advValue > 0.0 ? "[+%sx]" : "[%sx]", valueStr);
            case ADD_MULTIPLIED_TOTAL -> String.format(Locale.ROOT, "[x%s]", valueStr);
         };
         debugInfo = Component.literal(" ").append(Component.literal(txt).withStyle(ChatFormatting.GRAY));
      }

      return debugInfo;
   }

   @Nullable
   default ResourceLocation getBaseId() {
      if (this == Attributes.ATTACK_DAMAGE.value()) {
         return AttributeUtil.BASE_ATTACK_DAMAGE_ID;
      } else if (this == Attributes.ATTACK_SPEED.value()) {
         return AttributeUtil.BASE_ATTACK_SPEED_ID;
      } else {
         return this == Attributes.ENTITY_INTERACTION_RANGE.value() ? AttributeUtil.BASE_ENTITY_REACH_ID : null;
      }
   }

   default MutableComponent toBaseComponent(double value, double entityBase, boolean merged, TooltipFlag flag) {
      Attribute attr = this.self();
      MutableComponent comp = Component.translatable(
         "attribute.modifier.equals.0", this.toValueComponent(null, value, flag), Component.translatable(attr.getDescriptionId())
      );
      if (flag.isAdvanced() && !merged && NeoForgeConfig.COMMON.attributeAdvancedTooltipDebugInfo.get()) {
         double baseBonus = value - entityBase;
         String baseBonusText = String.format(Locale.ROOT, baseBonus > 0.0 ? " + %s" : " - %s", FORMAT.format(Math.abs(baseBonus)));
         Component debugInfo = Component.translatable("neoforge.attribute.debug.base", FORMAT.format(entityBase), baseBonusText).withStyle(ChatFormatting.GRAY);
         comp.append(CommonComponents.SPACE).append(debugInfo);
      }

      return comp;
   }

   TextColor getMergedStyle(boolean var1);

   static boolean isNullOrAddition(@Nullable AttributeModifier.Operation op) {
      return op == null || op == AttributeModifier.Operation.ADD_VALUE;
   }

   private Attribute self() {
      return (Attribute)this;
   }
}
