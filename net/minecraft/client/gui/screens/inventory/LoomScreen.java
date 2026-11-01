package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class LoomScreen extends AbstractContainerScreen<LoomMenu> {
   private static final Identifier BANNER_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/banner");
   private static final Identifier DYE_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/dye");
   private static final Identifier PATTERN_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/banner_pattern");
   private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/loom/scroller");
   private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/loom/scroller_disabled");
   private static final Identifier PATTERN_SELECTED_SPRITE = Identifier.withDefaultNamespace("container/loom/pattern_selected");
   private static final Identifier PATTERN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("container/loom/pattern_highlighted");
   private static final Identifier PATTERN_SPRITE = Identifier.withDefaultNamespace("container/loom/pattern");
   private static final Identifier ERROR_SPRITE = Identifier.withDefaultNamespace("container/loom/error");
   private static final Identifier BG_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/loom.png");
   private static final int PATTERN_COLUMNS = 4;
   private static final int PATTERN_ROWS = 4;
   private static final int SCROLLER_WIDTH = 12;
   private static final int SCROLLER_HEIGHT = 15;
   private static final int PATTERN_IMAGE_SIZE = 14;
   private static final int SCROLLER_FULL_HEIGHT = 56;
   private static final int PATTERNS_X = 60;
   private static final int PATTERNS_Y = 13;
   private static final float BANNER_PATTERN_TEXTURE_SIZE = 64.0F;
   private static final float BANNER_PATTERN_WIDTH = 21.0F;
   private static final float BANNER_PATTERN_HEIGHT = 40.0F;
   private BannerFlagModel flag;
   @Nullable
   private BannerPatternLayers resultBannerPatterns;
   private ItemStack bannerStack = ItemStack.EMPTY;
   private ItemStack dyeStack = ItemStack.EMPTY;
   private ItemStack patternStack = ItemStack.EMPTY;
   private boolean displayPatterns;
   private boolean hasMaxPatterns;
   private float scrollOffs;
   private boolean scrolling;
   private int startRow;

   public LoomScreen(LoomMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      menu.registerUpdateListener(this::containerChanged);
      this.titleLabelY -= 2;
   }

   @Override
   protected void init() {
      super.init();
      ModelPart modelPart = this.minecraft.getEntityModels().bakeLayer(ModelLayers.STANDING_BANNER_FLAG);
      this.flag = new BannerFlagModel(modelPart);
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      this.renderTooltip(guiGraphics, mouseX, mouseY);
   }

   private int totalRowCount() {
      return Mth.positiveCeilDiv(this.menu.getSelectablePatterns().size(), 4);
   }

   @Override
   protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
      int i = this.leftPos;
      int j = this.topPos;
      guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
      Slot slot = this.menu.getBannerSlot();
      Slot slot2 = this.menu.getDyeSlot();
      Slot slot3 = this.menu.getPatternSlot();
      Slot slot4 = this.menu.getResultSlot();
      if (!slot.hasItem()) {
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BANNER_SLOT_SPRITE, i + slot.x, j + slot.y, 16, 16);
      }

      if (!slot2.hasItem()) {
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, DYE_SLOT_SPRITE, i + slot2.x, j + slot2.y, 16, 16);
      }

      if (!slot3.hasItem()) {
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, PATTERN_SLOT_SPRITE, i + slot3.x, j + slot3.y, 16, 16);
      }

      int k = (int)(41.0F * this.scrollOffs);
      Identifier identifier = this.displayPatterns ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
      int l = i + 119;
      int m = j + 13 + k;
      guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, l, m, 12, 15);
      if (mouseX >= l && mouseX < l + 12 && mouseY >= m && mouseY < m + 15) {
         guiGraphics.requestCursor(this.scrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
      }

      if (this.resultBannerPatterns != null && !this.hasMaxPatterns) {
         DyeColor dyeColor = ((BannerItem)slot4.getItem().getItem()).getColor();
         int n = i + 141;
         int o = j + 8;
         guiGraphics.submitBannerPatternRenderState(this.flag, dyeColor, this.resultBannerPatterns, n, o, n + 20, o + 40);
      } else if (this.hasMaxPatterns) {
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, ERROR_SPRITE, i + slot4.x - 5, j + slot4.y - 5, 26, 26);
      }

      if (this.displayPatterns) {
         int p = i + 60;
         int n = j + 13;
         List<Holder<BannerPattern>> list = this.menu.getSelectablePatterns();

         label79:
         for (int q = 0; q < 4; q++) {
            for (int r = 0; r < 4; r++) {
               int s = q + this.startRow;
               int t = s * 4 + r;
               if (t >= list.size()) {
                  break label79;
               }

               int u = p + r * 14;
               int v = n + q * 14;
               Holder<BannerPattern> holder = list.get(t);
               boolean bl = mouseX >= u && mouseY >= v && mouseX < u + 14 && mouseY < v + 14;
               Identifier identifier2;
               if (t == this.menu.getSelectedBannerPatternIndex()) {
                  identifier2 = PATTERN_SELECTED_SPRITE;
               } else if (bl) {
                  identifier2 = PATTERN_HIGHLIGHTED_SPRITE;
                  DyeColor dyeColor2 = ((DyeItem)this.dyeStack.getItem()).getDyeColor();
                  guiGraphics.setTooltipForNextFrame(Component.translatable(holder.value().translationKey() + "." + dyeColor2.getName()), mouseX, mouseY);
                  guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
               } else {
                  identifier2 = PATTERN_SPRITE;
               }

               guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier2, u, v, 14, 14);
               TextureAtlasSprite textureAtlasSprite = guiGraphics.getSprite(Sheets.getBannerMaterial(holder));
               this.renderBannerOnButton(guiGraphics, u, v, textureAtlasSprite);
            }
         }
      }

      Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
   }

   private void renderBannerOnButton(GuiGraphics guiGraphics, int x, int y, TextureAtlasSprite sprite) {
      guiGraphics.pose().pushMatrix();
      guiGraphics.pose().translate(x + 4, y + 2);
      float f = sprite.getU0();
      float g = f + (sprite.getU1() - sprite.getU0()) * 21.0F / 64.0F;
      float h = sprite.getV1() - sprite.getV0();
      float i = sprite.getV0() + h / 64.0F;
      float j = i + h * 40.0F / 64.0F;
      int k = 5;
      int l = 10;
      guiGraphics.fill(0, 0, 5, 10, DyeColor.GRAY.getTextureDiffuseColor());
      guiGraphics.blit(sprite.atlasLocation(), 0, 0, 5, 10, f, g, i, j);
      guiGraphics.pose().popMatrix();
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
      if (this.displayPatterns) {
         int i = this.leftPos + 60;
         int j = this.topPos + 13;

         for (int k = 0; k < 4; k++) {
            for (int l = 0; l < 4; l++) {
               double d = event.x() - (i + l * 14);
               double e = event.y() - (j + k * 14);
               int m = k + this.startRow;
               int n = m * 4 + l;
               if (d >= 0.0 && e >= 0.0 && d < 14.0 && e < 14.0 && this.menu.clickMenuButton(this.minecraft.player, n)) {
                  Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_LOOM_SELECT_PATTERN, 1.0F));
                  this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, n);
                  return true;
               }
            }
         }

         i = this.leftPos + 119;
         j = this.topPos + 9;
         if (event.x() >= i && event.x() < i + 12 && event.y() >= j && event.y() < j + 56) {
            this.scrolling = true;
         }
      }

      return super.mouseClicked(event, isDoubleClick);
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
      int i = this.totalRowCount() - 4;
      if (this.scrolling && this.displayPatterns && i > 0) {
         int j = this.topPos + 13;
         int k = j + 56;
         this.scrollOffs = ((float)event.y() - j - 7.5F) / (k - j - 15.0F);
         this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
         this.startRow = Math.max((int)(this.scrollOffs * i + 0.5), 0);
         return true;
      } else {
         return super.mouseDragged(event, mouseX, mouseY);
      }
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      this.scrolling = false;
      return super.mouseReleased(event);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
         return true;
      } else {
         int i = this.totalRowCount() - 4;
         if (this.displayPatterns && i > 0) {
            float f = (float)scrollY / i;
            this.scrollOffs = Mth.clamp(this.scrollOffs - f, 0.0F, 1.0F);
            this.startRow = Math.max((int)(this.scrollOffs * i + 0.5F), 0);
         }

         return true;
      }
   }

   @Override
   protected boolean hasClickedOutside(double x, double y, int left, int top) {
      return x < left || y < top || x >= left + this.imageWidth || y >= top + this.imageHeight;
   }

   private void containerChanged() {
      ItemStack itemStack = this.menu.getResultSlot().getItem();
      if (itemStack.isEmpty()) {
         this.resultBannerPatterns = null;
      } else {
         this.resultBannerPatterns = itemStack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
      }

      ItemStack itemStack2 = this.menu.getBannerSlot().getItem();
      ItemStack itemStack3 = this.menu.getDyeSlot().getItem();
      ItemStack itemStack4 = this.menu.getPatternSlot().getItem();
      BannerPatternLayers bannerPatternLayers = itemStack2.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
      this.hasMaxPatterns = bannerPatternLayers.layers().size() >= 6;
      if (this.hasMaxPatterns) {
         this.resultBannerPatterns = null;
      }

      if (!ItemStack.matches(itemStack2, this.bannerStack)
         || !ItemStack.matches(itemStack3, this.dyeStack)
         || !ItemStack.matches(itemStack4, this.patternStack)) {
         this.displayPatterns = !itemStack2.isEmpty() && !itemStack3.isEmpty() && !this.hasMaxPatterns && !this.menu.getSelectablePatterns().isEmpty();
      }

      if (this.startRow >= this.totalRowCount()) {
         this.startRow = 0;
         this.scrollOffs = 0.0F;
      }

      this.bannerStack = itemStack2.copy();
      this.dyeStack = itemStack3.copy();
      this.patternStack = itemStack4.copy();
   }
}
