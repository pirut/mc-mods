package net.neoforged.neoforge.common.util;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.extensions.IHolderExtension;
import net.neoforged.neoforge.event.AddAttributeTooltipsEvent;
import net.neoforged.neoforge.event.GatherSkippedAttributeTooltipsEvent;
import net.neoforged.neoforge.internal.NeoForgeProxy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AttributeUtil {
   public static final Identifier BASE_ATTACK_DAMAGE_ID = Item.BASE_ATTACK_DAMAGE_ID;
   public static final Identifier BASE_ATTACK_SPEED_ID = Item.BASE_ATTACK_SPEED_ID;
   public static final Identifier BASE_ENTITY_REACH_ID = Identifier.withDefaultNamespace("base_entity_reach");
   public static final Identifier FAKE_MERGED_ID = Identifier.fromNamespaceAndPath("neoforge", "fake_merged_modifier");
   public static final Comparator<AttributeModifier> ATTRIBUTE_MODIFIER_COMPARATOR = Comparator.comparing(AttributeModifier::operation)
      .thenComparingDouble(a -> -Math.abs(a.amount()))
      .thenComparing(AttributeModifier::id);
   private static final Logger LOGGER = LogManager.getLogger();

   public static void addAttributeTooltips(ItemStack stack, Consumer<Component> tooltip, TooltipDisplay tooltipDisplay, AttributeTooltipContext ctx) {
      if (tooltipDisplay.shows(DataComponents.ATTRIBUTE_MODIFIERS)) {
         applyModifierTooltips(stack, tooltip, ctx);
      }

      NeoForge.EVENT_BUS.post(new AddAttributeTooltipsEvent(stack, tooltip, ctx));
   }

   public static void applyModifierTooltips(ItemStack stack, Consumer<Component> tooltip, AttributeTooltipContext ctx) {
      GatherSkippedAttributeTooltipsEvent event = (GatherSkippedAttributeTooltipsEvent)NeoForge.EVENT_BUS
         .post(new GatherSkippedAttributeTooltipsEvent(stack, ctx));
      if (!event.isSkippingAll()) {
         for (EquipmentSlotGroup group : EquipmentSlotGroup.values()) {
            if (!event.isSkipped(group)) {
               Multimap<Holder<Attribute>, AttributeModifier> modifiers = getSortedModifiers(stack, group);
               modifiers.values().removeIf(m -> event.isSkipped(m.id()));
               if (!modifiers.isEmpty()) {
                  tooltip.accept(Component.empty());
                  tooltip.accept(Component.translatable("item.modifiers." + group.getSerializedName()).withStyle(ChatFormatting.GRAY));
                  applyTextFor(stack, tooltip, modifiers, ctx);
               }
            }
         }
      }
   }

   public static void applyTextFor(
      ItemStack stack, Consumer<Component> tooltip, Multimap<Holder<Attribute>, AttributeModifier> modifierMap, AttributeTooltipContext ctx
   ) {
      if (!modifierMap.isEmpty()) {
         Map<Holder<Attribute>, AttributeUtil.BaseModifier> baseModifs = new Reference2ReferenceLinkedOpenHashMap();
         Iterator<Entry<Holder<Attribute>, AttributeModifier>> it = modifierMap.entries().iterator();

         while (it.hasNext()) {
            Entry<Holder<Attribute>, AttributeModifier> entry = it.next();
            Holder<Attribute> attr = entry.getKey();
            AttributeModifier modif = entry.getValue();
            if (modif.id().equals(attr.value().getBaseId())) {
               baseModifs.put(attr, new AttributeUtil.BaseModifier(modif, new ArrayList<>()));
               it.remove();
            }
         }

         for (Entry<Holder<Attribute>, AttributeModifier> entry : modifierMap.entries()) {
            AttributeUtil.BaseModifier base = baseModifs.get(entry.getKey());
            if (base != null) {
               base.children.add(entry.getValue());
            }
         }

         for (Entry<Holder<Attribute>, AttributeUtil.BaseModifier> entryx : baseModifs.entrySet()) {
            Holder<Attribute> attr = entryx.getKey();
            AttributeUtil.BaseModifier baseModif = entryx.getValue();
            double entityBase = ctx.player() == null ? 0.0 : ctx.player().getAttributeBaseValue(attr);
            double base = baseModif.base.amount() + entityBase;
            double rawBase = base;
            double amt = base;
            if (NeoForgeMod.shouldMergeAttributeTooltips()) {
               for (AttributeModifier modif : baseModif.children) {
                  switch (modif.operation()) {
                     case ADD_VALUE:
                        base = amt += modif.amount();
                        break;
                     case ADD_MULTIPLIED_BASE:
                        amt += modif.amount() * base;
                        break;
                     case ADD_MULTIPLIED_TOTAL:
                        amt *= 1.0 + modif.amount();
                  }
               }
            }

            boolean isMerged = NeoForgeMod.shouldMergeAttributeTooltips() && !baseModif.children.isEmpty();
            MutableComponent text = attr.value().toBaseComponent(amt, entityBase, isMerged, ctx.flag());
            tooltip.accept(Component.literal(" ").append(text).withStyle(isMerged ? ChatFormatting.GOLD : ChatFormatting.DARK_GREEN));
            if (ctx.flag().hasShiftDown() && isMerged) {
               text = attr.value().toBaseComponent(rawBase, entityBase, false, ctx.flag());
               tooltip.accept(listHeader().append(text.withStyle(ChatFormatting.DARK_GREEN)));

               for (AttributeModifier modifier : baseModif.children) {
                  tooltip.accept(listHeader().append(attr.value().toComponent(modifier, ctx.flag())));
               }
            }
         }

         for (Holder<Attribute> attrx : modifierMap.keySet()) {
            if (!NeoForgeMod.shouldMergeAttributeTooltips() || !baseModifs.containsKey(attrx)) {
               Collection<AttributeModifier> modifs = modifierMap.get(attrx);
               if (NeoForgeMod.shouldMergeAttributeTooltips() && modifs.size() > 1) {
                  Map<AttributeModifier.Operation, AttributeUtil.MergedModifierData> mergeData = new EnumMap<>(AttributeModifier.Operation.class);

                  for (AttributeModifier modifier : modifs) {
                     if (modifier.amount() != 0.0) {
                        AttributeUtil.MergedModifierData data = mergeData.computeIfAbsent(modifier.operation(), opx -> new AttributeUtil.MergedModifierData());
                        if (data.sum != 0.0) {
                           data.isMerged = true;
                        }

                        data.sum = data.sum + modifier.amount();
                        data.children.add(modifier);
                     }
                  }

                  for (AttributeModifier.Operation op : AttributeModifier.Operation.values()) {
                     AttributeUtil.MergedModifierData data = mergeData.get(op);
                     if (data != null && data.sum != 0.0) {
                        if (data.isMerged) {
                           TextColor color = attrx.value().getMergedStyle(data.sum > 0.0);
                           AttributeModifier fakeModif = new AttributeModifier(FAKE_MERGED_ID, data.sum, op);
                           MutableComponent comp = attrx.value().toComponent(fakeModif, ctx.flag());
                           tooltip.accept(comp.withStyle(comp.getStyle().withColor(color)));
                           if (ctx.flag().hasShiftDown()) {
                              data.children.forEach(modif -> tooltip.accept(listHeader().append(attr.value().toComponent(modif, ctx.flag()))));
                           }
                        } else {
                           AttributeModifier fakeModif = new AttributeModifier(FAKE_MERGED_ID, data.sum, op);
                           tooltip.accept(attrx.value().toComponent(fakeModif, ctx.flag()));
                        }
                     }
                  }
               } else {
                  for (AttributeModifier m : modifs) {
                     if (m.amount() != 0.0) {
                        tooltip.accept(attrx.value().toComponent(m, ctx.flag()));
                     }
                  }
               }
            }
         }
      }
   }

   public static void addPotionTooltip(List<Pair<Holder<Attribute>, AttributeModifier>> list, Consumer<Component> tooltips) {
      for (Pair<Holder<Attribute>, AttributeModifier> pair : list) {
         tooltips.accept(
            ((Attribute)((Holder)pair.getFirst()).value()).toComponent((AttributeModifier)pair.getSecond(), NeoForgeProxy.INSTANCE.getTooltipFlag())
         );
      }
   }

   public static Multimap<Holder<Attribute>, AttributeModifier> sortedMap() {
      return TreeMultimap.create(Comparator.comparing(IHolderExtension::getKey), ATTRIBUTE_MODIFIER_COMPARATOR);
   }

   public static Multimap<Holder<Attribute>, AttributeModifier> getSortedModifiers(ItemStack stack, EquipmentSlotGroup slot) {
      Multimap<Holder<Attribute>, AttributeModifier> map = LinkedListMultimap.create();
      stack.forEachModifier(slot, (attr, modif, display) -> {
         if (attr != null && modif != null) {
            map.put(attr, modif);
         } else {
            LOGGER.debug("Detected broken attribute modifier entry on item {}.  Attr={}, Modif={}", stack, attr, modif);
         }
      });
      return map;
   }

   private static MutableComponent listHeader() {
      return Component.literal(" ┇ ").withStyle(ChatFormatting.GRAY);
   }

   private record BaseModifier(AttributeModifier base, List<AttributeModifier> children) {
   }

   private static class MergedModifierData {
      double sum = 0.0;
      boolean isMerged = false;
      private List<AttributeModifier> children = new LinkedList<>();
   }
}
