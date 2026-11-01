package net.minecraft.client.gui.screens.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class InventoryScreen extends AbstractRecipeBookScreen<InventoryMenu> {
   private float xMouse;
   private float yMouse;
   private boolean buttonClicked;
   private final EffectsInInventory effects;

   public InventoryScreen(Player player) {
      super(player.inventoryMenu, new CraftingRecipeBookComponent(player.inventoryMenu), player.getInventory(), Component.translatable("container.crafting"));
      this.titleLabelX = 97;
      this.effects = new EffectsInInventory(this);
   }

   @Override
   public void containerTick() {
      super.containerTick();
      if (this.minecraft.player.hasInfiniteMaterials()) {
         this.minecraft
            .setScreen(
               new CreativeModeInventoryScreen(
                  this.minecraft.player, this.minecraft.player.connection.enabledFeatures(), this.minecraft.options.operatorItemsTab().get()
               )
            );
      }
   }

   @Override
   protected void init() {
      if (this.minecraft.player.hasInfiniteMaterials()) {
         this.minecraft
            .setScreen(
               new CreativeModeInventoryScreen(
                  this.minecraft.player, this.minecraft.player.connection.enabledFeatures(), this.minecraft.options.operatorItemsTab().get()
               )
            );
      } else {
         super.init();
      }
   }

   @Override
   protected ScreenPosition getRecipeBookButtonPosition() {
      return new ScreenPosition(this.leftPos + 104, this.height / 2 - 22);
   }

   @Override
   protected void onRecipeBookButtonClick() {
      this.buttonClicked = true;
   }

   @Override
   protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      this.effects.render(guiGraphics, mouseX, mouseY);
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      this.xMouse = mouseX;
      this.yMouse = mouseY;
   }

   @Override
   public boolean showsActiveEffects() {
      return this.effects.canSeeEffects();
   }

   @Override
   protected boolean isBiggerResultSlot() {
      return false;
   }

   @Override
   protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
      int i = this.leftPos;
      int j = this.topPos;
      guiGraphics.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_LOCATION, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
      renderEntityInInventoryFollowsMouse(guiGraphics, i + 26, j + 8, i + 75, j + 78, 30, 0.0625F, this.xMouse, this.yMouse, this.minecraft.player);
   }

   public static void renderEntityInInventoryFollowsMouse(
      GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int scale, float yOffset, float mouseX, float mouseY, LivingEntity entity
   ) {
      float f = (x1 + x2) / 2.0F;
      float g = (y1 + y2) / 2.0F;
      float h = (float)Math.atan((f - mouseX) / 40.0F);
      float i = (float)Math.atan((g - mouseY) / 40.0F);
      Quaternionf quaternionf = new Quaternionf().rotateZ((float) Math.PI);
      Quaternionf quaternionf2 = new Quaternionf().rotateX(i * 20.0F * (float) (Math.PI / 180.0));
      quaternionf.mul(quaternionf2);
      EntityRenderState entityRenderState = extractRenderState(entity);
      if (entityRenderState instanceof LivingEntityRenderState livingEntityRenderState) {
         livingEntityRenderState.bodyRot = 180.0F + h * 20.0F;
         livingEntityRenderState.yRot = h * 20.0F;
         if (livingEntityRenderState.pose != Pose.FALL_FLYING) {
            livingEntityRenderState.xRot = -i * 20.0F;
         } else {
            livingEntityRenderState.xRot = 0.0F;
         }

         livingEntityRenderState.boundingBoxWidth = livingEntityRenderState.boundingBoxWidth / livingEntityRenderState.scale;
         livingEntityRenderState.boundingBoxHeight = livingEntityRenderState.boundingBoxHeight / livingEntityRenderState.scale;
         livingEntityRenderState.scale = 1.0F;
      }

      Vector3f vector3f = new Vector3f(0.0F, entityRenderState.boundingBoxHeight / 2.0F + yOffset, 0.0F);
      guiGraphics.submitEntityRenderState(entityRenderState, scale, vector3f, quaternionf, quaternionf2, x1, y1, x2, y2);
   }

   private static EntityRenderState extractRenderState(LivingEntity entity) {
      EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
      EntityRenderer<? super LivingEntity, ?> entityRenderer = entityRenderDispatcher.getRenderer(entity);
      EntityRenderState entityRenderState = entityRenderer.createRenderState(entity, 1.0F);
      entityRenderState.lightCoords = 15728880;
      entityRenderState.shadowPieces.clear();
      entityRenderState.outlineColor = 0;
      return entityRenderState;
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      if (this.buttonClicked) {
         this.buttonClicked = false;
         return true;
      } else {
         return super.mouseReleased(event);
      }
   }
}
