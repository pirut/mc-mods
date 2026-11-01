package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HorseInventoryScreen extends AbstractContainerScreen<HorseInventoryMenu> {
   private static final ResourceLocation CHEST_SLOTS_SPRITE = ResourceLocation.withDefaultNamespace("container/horse/chest_slots");
   private static final ResourceLocation SADDLE_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/horse/saddle_slot");
   private static final ResourceLocation LLAMA_ARMOR_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/horse/llama_armor_slot");
   private static final ResourceLocation ARMOR_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/horse/armor_slot");
   private static final ResourceLocation HORSE_INVENTORY_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/horse.png");
   private final AbstractHorse horse;
   private final int inventoryColumns;
   private float xMouse;
   private float yMouse;

   public HorseInventoryScreen(HorseInventoryMenu p_98817_, Inventory p_98818_, AbstractHorse p_98819_, int p_352203_) {
      super(p_98817_, p_98818_, p_98819_.getDisplayName());
      this.horse = p_98819_;
      this.inventoryColumns = p_352203_;
   }

   @Override
   protected void renderBg(GuiGraphics p_282553_, float p_282998_, int p_282929_, int p_283133_) {
      int $$4 = (this.width - this.imageWidth) / 2;
      int $$5 = (this.height - this.imageHeight) / 2;
      p_282553_.blit(HORSE_INVENTORY_LOCATION, $$4, $$5, 0, 0, this.imageWidth, this.imageHeight);
      if (this.inventoryColumns > 0) {
         p_282553_.blitSprite(CHEST_SLOTS_SPRITE, 90, 54, 0, 0, $$4 + 79, $$5 + 17, this.inventoryColumns * 18, 54);
      }

      if (this.horse.isSaddleable()) {
         p_282553_.blitSprite(SADDLE_SLOT_SPRITE, $$4 + 7, $$5 + 35 - 18, 18, 18);
      }

      if (this.horse.canUseSlot(EquipmentSlot.BODY)) {
         if (this.horse instanceof Llama) {
            p_282553_.blitSprite(LLAMA_ARMOR_SLOT_SPRITE, $$4 + 7, $$5 + 35, 18, 18);
         } else {
            p_282553_.blitSprite(ARMOR_SLOT_SPRITE, $$4 + 7, $$5 + 35, 18, 18);
         }
      }

      InventoryScreen.renderEntityInInventoryFollowsMouse(p_282553_, $$4 + 26, $$5 + 18, $$4 + 78, $$5 + 70, 17, 0.25F, this.xMouse, this.yMouse, this.horse);
   }

   @Override
   public void render(GuiGraphics p_281697_, int p_282103_, int p_283529_, float p_283079_) {
      this.xMouse = p_282103_;
      this.yMouse = p_283529_;
      super.render(p_281697_, p_282103_, p_283529_, p_283079_);
      this.renderTooltip(p_281697_, p_282103_, p_283529_);
   }
}
