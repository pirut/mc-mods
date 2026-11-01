package net.neoforged.neoforge.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.VersionChecker;
import net.neoforged.fml.VersionChecker.CheckResult;
import net.neoforged.fml.i18n.MavenVersionTranslator;
import net.neoforged.neoforge.client.gui.ModListScreen;
import net.neoforged.neoforgespi.language.IModInfo;

public class ModListWidget extends ObjectSelectionList<ModListWidget.ModEntry> {
   private static final ResourceLocation VERSION_CHECK_ICONS = ResourceLocation.fromNamespaceAndPath("neoforge", "textures/gui/version_check_icons.png");
   private final int listWidth;
   private ModListScreen parent;

   private static String stripControlCodes(String value) {
      return StringUtil.stripColor(value);
   }

   public ModListWidget(ModListScreen parent, int listWidth, int top, int bottom) {
      super(parent.getMinecraftInstance(), listWidth, bottom - top, top, 9 * 2 + 8);
      this.parent = parent;
      this.listWidth = listWidth;
      this.refreshList();
   }

   @Override
   protected int getScrollbarPosition() {
      return this.listWidth;
   }

   @Override
   public int getRowWidth() {
      return this.listWidth;
   }

   public void refreshList() {
      this.clearEntries();
      this.parent.buildModList(x$0 -> this.addEntry(x$0), mod -> new ModListWidget.ModEntry(mod, this.parent));
   }

   public class ModEntry extends ObjectSelectionList.Entry<ModListWidget.ModEntry> {
      private final ModContainer container;
      private final ModListScreen parent;

      ModEntry(ModContainer info, ModListScreen parent) {
         this.container = info;
         this.parent = parent;
      }

      @Override
      public Component getNarration() {
         return Component.translatable("narrator.select", this.container.getModInfo().getDisplayName());
      }

      @Override
      public void render(
         GuiGraphics guiGraphics,
         int entryIdx,
         int top,
         int left,
         int entryWidth,
         int entryHeight,
         int mouseX,
         int mouseY,
         boolean isMouseOver,
         float partialTick
      ) {
         Component name = Component.literal(ModListWidget.stripControlCodes(this.container.getModInfo().getDisplayName()));
         Component version = Component.literal(
            ModListWidget.stripControlCodes(MavenVersionTranslator.artifactVersionToString(this.container.getModInfo().getVersion()))
         );
         CheckResult vercheck = VersionChecker.getResult(this.container.getModInfo());
         Font font = this.parent.getFontRenderer();
         guiGraphics.drawString(
            font,
            Language.getInstance().getVisualOrder(FormattedText.composite(font.substrByWidth(name, ModListWidget.this.listWidth))),
            left + 3,
            top + 2,
            16777215,
            false
         );
         guiGraphics.drawString(
            font,
            Language.getInstance().getVisualOrder(FormattedText.composite(font.substrByWidth(version, ModListWidget.this.listWidth))),
            left + 3,
            top + 2 + 9,
            13421772,
            false
         );
         if (vercheck.status().shouldDraw()) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.pose().pushPose();
            guiGraphics.blit(
               ModListWidget.VERSION_CHECK_ICONS,
               ModListWidget.this.getX() + ModListWidget.this.width - 12,
               top + entryHeight / 4,
               vercheck.status().getSheetOffset() * 8,
               vercheck.status().isAnimated() && (System.currentTimeMillis() / 800L & 1L) == 1L ? 8.0F : 0.0F,
               8,
               8,
               64,
               16
            );
            guiGraphics.pose().popPose();
         }
      }

      @Override
      public boolean mouseClicked(double p_mouseClicked_1_, double p_mouseClicked_3_, int p_mouseClicked_5_) {
         this.parent.setSelected(this.isFocused() ? null : this);
         ModListWidget.this.setSelected(this.isFocused() ? null : this);
         return false;
      }

      @Override
      public void setFocused(boolean focused) {
         if (focused) {
            this.parent.setSelected(this);
            ModListWidget.this.setSelected(this);
         }
      }

      @Override
      public boolean isFocused() {
         return ModListWidget.this.getSelected() == this;
      }

      public IModInfo getInfo() {
         return this.container.getModInfo();
      }

      public ModContainer getContainer() {
         return this.container;
      }
   }
}
