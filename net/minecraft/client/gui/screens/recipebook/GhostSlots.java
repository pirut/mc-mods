package net.minecraft.client.gui.screens.recipebook;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class GhostSlots {
   private final Reference2ObjectMap<Slot, GhostSlots.GhostSlot> ingredients = new Reference2ObjectArrayMap();
   private final SlotSelectTime slotSelectTime;

   public GhostSlots(SlotSelectTime p_361863_) {
      this.slotSelectTime = p_361863_;
   }

   public void clear() {
      this.ingredients.clear();
   }

   private void setSlot(Slot p_380315_, ContextMap p_380942_, SlotDisplay p_379948_, boolean p_379880_) {
      List<ItemStack> list = p_379948_.resolveForStacks(p_380942_);
      if (!list.isEmpty()) {
         this.ingredients.put(p_380315_, new GhostSlots.GhostSlot(list, p_379880_));
      }
   }

   public void setInput(Slot p_379329_, ContextMap p_381026_, SlotDisplay p_379367_) {
      this.setSlot(p_379329_, p_381026_, p_379367_, false);
   }

   public void setResult(Slot p_379733_, ContextMap p_381037_, SlotDisplay p_380406_) {
      this.setSlot(p_379733_, p_381037_, p_380406_, true);
   }

   public void render(GuiGraphics p_360912_, Minecraft p_363356_, boolean p_361600_) {
      this.ingredients.forEach((p_421286_, p_421287_) -> {
         int i = p_421286_.x;
         int j = p_421286_.y;
         if (p_421287_.isResultSlot && p_361600_) {
            p_360912_.fill(i - 4, j - 4, i + 20, j + 20, 822018048);
         } else {
            p_360912_.fill(i, j, i + 16, j + 16, 822018048);
         }

         ItemStack itemstack = p_421287_.getItem(this.slotSelectTime.currentIndex());
         p_360912_.renderFakeItem(itemstack, i, j);
         p_360912_.fill(i, j, i + 16, j + 16, 822083583);
         if (p_421287_.isResultSlot) {
            p_360912_.renderItemDecorations(p_363356_.font, itemstack, i, j);
         }
      });
   }

   public void renderTooltip(GuiGraphics p_363721_, Minecraft p_365392_, int p_363797_, int p_363310_, @Nullable Slot p_360907_) {
      if (p_360907_ != null) {
         GhostSlots.GhostSlot ghostslots$ghostslot = (GhostSlots.GhostSlot)this.ingredients.get(p_360907_);
         if (ghostslots$ghostslot != null) {
            ItemStack itemstack = ghostslots$ghostslot.getItem(this.slotSelectTime.currentIndex());
            p_363721_.setComponentTooltipForNextFrame(
               p_365392_.font, Screen.getTooltipFromItem(p_365392_, itemstack), p_363797_, p_363310_, itemstack, itemstack.get(DataComponents.TOOLTIP_STYLE)
            );
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   record GhostSlot(List<ItemStack> items, boolean isResultSlot) {
      public ItemStack getItem(int p_361727_) {
         int i = this.items.size();
         return i == 0 ? ItemStack.EMPTY : this.items.get(p_361727_ % i);
      }
   }
}
