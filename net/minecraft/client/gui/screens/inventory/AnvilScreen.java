package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AnvilScreen extends ItemCombinerScreen<AnvilMenu> {
   private static final ResourceLocation TEXT_FIELD_SPRITE = ResourceLocation.withDefaultNamespace("container/anvil/text_field");
   private static final ResourceLocation TEXT_FIELD_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("container/anvil/text_field_disabled");
   private static final ResourceLocation ERROR_SPRITE = ResourceLocation.withDefaultNamespace("container/anvil/error");
   private static final ResourceLocation ANVIL_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/anvil.png");
   private static final Component TOO_EXPENSIVE_TEXT = Component.translatable("container.repair.expensive");
   private EditBox name;
   private final Player player;

   public AnvilScreen(AnvilMenu p_97874_, Inventory p_97875_, Component p_97876_) {
      super(p_97874_, p_97875_, p_97876_, ANVIL_LOCATION);
      this.player = p_97875_.player;
      this.titleLabelX = 60;
   }

   @Override
   protected void subInit() {
      int $$0 = (this.width - this.imageWidth) / 2;
      int $$1 = (this.height - this.imageHeight) / 2;
      this.name = new EditBox(this.font, $$0 + 62, $$1 + 24, 103, 12, Component.translatable("container.repair"));
      this.name.setCanLoseFocus(false);
      this.name.setTextColor(-1);
      this.name.setTextColorUneditable(-1);
      this.name.setBordered(false);
      this.name.setMaxLength(50);
      this.name.setResponder(this::onNameChanged);
      this.name.setValue("");
      this.addWidget(this.name);
      this.name.setEditable(this.menu.getSlot(0).hasItem());
   }

   @Override
   protected void setInitialFocus() {
      this.setInitialFocus(this.name);
   }

   @Override
   public void resize(Minecraft p_97886_, int p_97887_, int p_97888_) {
      String $$3 = this.name.getValue();
      this.init(p_97886_, p_97887_, p_97888_);
      this.name.setValue($$3);
   }

   @Override
   public boolean keyPressed(int p_97878_, int p_97879_, int p_97880_) {
      if (p_97878_ == 256) {
         this.minecraft.player.closeContainer();
      }

      return !this.name.keyPressed(p_97878_, p_97879_, p_97880_) && !this.name.canConsumeInput() ? super.keyPressed(p_97878_, p_97879_, p_97880_) : true;
   }

   private void onNameChanged(String p_97899_) {
      Slot $$1 = this.menu.getSlot(0);
      if ($$1.hasItem()) {
         String $$2 = p_97899_;
         if (!$$1.getItem().has(DataComponents.CUSTOM_NAME) && p_97899_.equals($$1.getItem().getHoverName().getString())) {
            $$2 = "";
         }

         if (this.menu.setItemName($$2)) {
            this.minecraft.player.connection.send(new ServerboundRenameItemPacket($$2));
         }
      }
   }

   @Override
   protected void renderLabels(GuiGraphics p_281442_, int p_282417_, int p_283022_) {
      super.renderLabels(p_281442_, p_282417_, p_283022_);
      int $$3 = this.menu.getCost();
      if ($$3 > 0) {
         int $$4 = 8453920;
         Component $$5;
         if ($$3 >= 40 && !this.minecraft.player.getAbilities().instabuild) {
            $$5 = TOO_EXPENSIVE_TEXT;
            $$4 = 16736352;
         } else if (!this.menu.getSlot(2).hasItem()) {
            $$5 = null;
         } else {
            $$5 = Component.translatable("container.repair.cost", $$3);
            if (!this.menu.getSlot(2).mayPickup(this.player)) {
               $$4 = 16736352;
            }
         }

         if ($$5 != null) {
            int $$8 = this.imageWidth - 8 - this.font.width($$5) - 2;
            int $$9 = 69;
            p_281442_.fill($$8 - 2, 67, this.imageWidth - 8, 79, 1325400064);
            p_281442_.drawString(this.font, $$5, $$8, 69, $$4);
         }
      }
   }

   @Override
   protected void renderBg(GuiGraphics p_283345_, float p_283412_, int p_282871_, int p_281306_) {
      super.renderBg(p_283345_, p_283412_, p_282871_, p_281306_);
      p_283345_.blitSprite(this.menu.getSlot(0).hasItem() ? TEXT_FIELD_SPRITE : TEXT_FIELD_DISABLED_SPRITE, this.leftPos + 59, this.topPos + 20, 110, 16);
   }

   @Override
   public void renderFg(GuiGraphics p_283449_, int p_283263_, int p_281526_, float p_282957_) {
      this.name.render(p_283449_, p_283263_, p_281526_, p_282957_);
   }

   @Override
   protected void renderErrorIcon(GuiGraphics p_282905_, int p_283237_, int p_282237_) {
      if ((this.menu.getSlot(0).hasItem() || this.menu.getSlot(1).hasItem()) && !this.menu.getSlot(this.menu.getResultSlot()).hasItem()) {
         p_282905_.blitSprite(ERROR_SPRITE, p_283237_ + 99, p_282237_ + 45, 28, 21);
      }
   }

   @Override
   public void slotChanged(AbstractContainerMenu p_97882_, int p_97883_, ItemStack p_97884_) {
      if (p_97883_ == 0) {
         this.name.setValue(p_97884_.isEmpty() ? "" : p_97884_.getHoverName().getString());
         this.name.setEditable(!p_97884_.isEmpty());
         this.setFocused(this.name);
      }
   }
}
