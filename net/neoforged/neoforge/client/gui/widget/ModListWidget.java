package net.neoforged.neoforge.client.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringUtil;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.VersionChecker;
import net.neoforged.fml.VersionChecker.CheckResult;
import net.neoforged.fml.i18n.MavenVersionTranslator;
import net.neoforged.neoforge.client.gui.ModListScreen;
import net.neoforged.neoforgespi.language.IModInfo;

public class ModListWidget extends ObjectSelectionList<ModListWidget.ModEntry> {
   private static final Identifier VERSION_CHECK_ICONS = Identifier.fromNamespaceAndPath("neoforge", "textures/gui/version_check_icons.png");
   private final int listWidth;
   private ModListScreen parent;

   private static String stripControlCodes(String value) {
      return StringUtil.stripColor(value);
   }

   public ModListWidget(ModListScreen parent, int listWidth, int top, int bottom) {
      super(parent.getMinecraftInstance(), listWidth, bottom - top, top, 9 * 2 + 8);
      this.parent = parent;
      this.listWidth = listWidth;
   }

   @Override
   protected int scrollBarX() {
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
      public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
         Component name = Component.literal(ModListWidget.stripControlCodes(this.container.getModInfo().getDisplayName()));
         Component version = Component.literal(
            ModListWidget.stripControlCodes(MavenVersionTranslator.artifactVersionToString(this.container.getModInfo().getVersion()))
         );
         CheckResult vercheck = VersionChecker.getResult(this.container.getModInfo());
         int left = this.getContentX();
         int top = this.getContentY();
         Font font = this.parent.getFontRenderer();
         guiGraphics.drawString(
            font,
            Language.getInstance().getVisualOrder(FormattedText.composite(font.substrByWidth(name, ModListWidget.this.listWidth))),
            left + 3,
            top + 2,
            -1,
            false
         );
         guiGraphics.drawString(
            font,
            Language.getInstance().getVisualOrder(FormattedText.composite(font.substrByWidth(version, ModListWidget.this.listWidth))),
            left + 3,
            top + 2 + 9,
            -3355444,
            false
         );
         if (vercheck.status().shouldDraw()) {
            int entryHeight = this.getContentHeight();
            guiGraphics.blit(
               RenderPipelines.GUI_TEXTURED,
               ModListWidget.VERSION_CHECK_ICONS,
               this.getX() + ModListWidget.this.width - 12,
               top + entryHeight / 4,
               vercheck.status().getSheetOffset() * 8,
               vercheck.status().isAnimated() && (System.currentTimeMillis() / 800L & 1L) == 1L ? 8.0F : 0.0F,
               8,
               8,
               64,
               16
            );
         }
      }

      @Override
      public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
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
