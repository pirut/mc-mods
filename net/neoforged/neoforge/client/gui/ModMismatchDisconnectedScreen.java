package net.neoforged.neoforge.client.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.logging.LogUtils;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.fml.ModList;
import net.neoforged.fml.i18n.FMLTranslations;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.widget.ScrollPanel;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

public class ModMismatchDisconnectedScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Component reason;
   private MultiLineLabel message = MultiLineLabel.EMPTY;
   private ModMismatchDisconnectedScreen.MismatchInfoPanel scrollList;
   private final Screen parent;
   private int textHeight;
   private final Path modsDir;
   private final Path logFile;
   private final int listHeight = 140;
   private final Map<ResourceLocation, Component> mismatchedChannelData;

   public ModMismatchDisconnectedScreen(Screen parentScreen, Component reason, Map<ResourceLocation, Component> mismatchedChannelData) {
      super(Component.translatable("disconnect.lost"));
      this.reason = reason;
      this.parent = parentScreen;
      this.modsDir = FMLPaths.MODSDIR.get();
      this.logFile = FMLPaths.GAMEDIR.get().resolve(Paths.get("logs", "latest.log"));
      this.mismatchedChannelData = mismatchedChannelData;
      this.mismatchedChannelData
         .replaceAll(
            (id, r) -> {
               Optional<String> modDisplayName = ModList.get().getModContainerById(id.getNamespace()).map(mod -> mod.getModInfo().getDisplayName());
               return (Component)(!modDisplayName.isPresent()
                     || r.getContents() instanceof TranslatableContents c && c.getKey().equals("neoforge.network.negotiation.failure.mod")
                  ? r
                  : Component.translatable("neoforge.network.negotiation.failure.mod", modDisplayName.get(), r));
            }
         );
      this.mismatchedChannelData.forEach((id, r) -> LOGGER.warn("Channel [{}] failed to connect: {}", id, r.getString()));
   }

   @Override
   protected void init() {
      int listLeft = Math.max(8, this.width / 2 - 220);
      int listWidth = Math.min(440, this.width - 16);
      this.message = MultiLineLabel.create(this.font, this.reason, this.width - 50);
      this.textHeight = this.message.getLineCount() * 9;
      int upperButtonHeight = Math.min((this.height + 140) / 2 + 25, this.height - 50);
      int lowerButtonHeight = Math.min((this.height + 140) / 2 + 50, this.height - 25);
      this.addRenderableWidget(
         this.scrollList = new ModMismatchDisconnectedScreen.MismatchInfoPanel(this.minecraft, listWidth, 140, (this.height - 140) / 2, listLeft)
      );
      int buttonWidth = Math.min(210, this.width / 2 - 20);
      this.addRenderableWidget(
         CycleButton.onOffBuilder(true)
            .create(
               Math.max(this.width / 4 - buttonWidth / 2, listLeft),
               upperButtonHeight,
               buttonWidth,
               20,
               Component.translatable("fml.modmismatchscreen.simplifiedview"),
               (b, v) -> this.scrollList.toggleSimplifiedView()
            )
      );
      this.addRenderableWidget(
         Button.builder(
               Component.literal(FMLTranslations.parseMessage("fml.button.open.file", new Object[]{this.logFile.getFileName()})),
               button -> Util.getPlatform().openFile(this.logFile.toFile())
            )
            .bounds(Math.min(this.width * 3 / 4 - buttonWidth / 2, listLeft + listWidth - buttonWidth), upperButtonHeight, buttonWidth, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(
               Component.literal(FMLTranslations.parseMessage("fml.button.open.mods.folder", new Object[0])),
               button -> Util.getPlatform().openFile(this.modsDir.toFile())
            )
            .bounds(Math.max(this.width / 4 - buttonWidth / 2, listLeft), lowerButtonHeight, buttonWidth, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.toMenu"), button -> this.minecraft.setScreen(this.parent))
            .bounds(Math.min(this.width * 3 / 4 - buttonWidth / 2, listLeft + listWidth - buttonWidth), lowerButtonHeight, buttonWidth, 20)
            .build()
      );
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
      super.render(guiGraphics, mouseX, mouseY, partialTicks);
      guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, (this.height - 140) / 2 - this.textHeight - 36, 11184810);
      this.message.renderCentered(guiGraphics, this.width / 2, (this.height - 140) / 2 - this.textHeight - 18);
   }

   class MismatchInfoPanel extends ScrollPanel {
      private final int nameIndent = 10;
      private final int tableWidth = this.width - this.border * 2 - 6 - 10;
      private final int nameWidth = this.tableWidth / 2;
      private final int versionWidth = this.tableWidth - this.nameWidth;
      private List<Pair<FormattedCharSequence, FormattedCharSequence>> lineTable;
      private int contentSize;
      private boolean oneChannelPerEntry = true;

      public MismatchInfoPanel(Minecraft client, int width, int height, int top, int left) {
         super(client, width, height, top, left);
         this.updateListContent();
      }

      private void updateListContent() {
         Map<List<ResourceLocation>, Component> mergedChannelData = this.sortAndMergeChannelData(ModMismatchDisconnectedScreen.this.mismatchedChannelData);

         record Row(MutableComponent name, MutableComponent reason) {
         }

         List<Row> rows = new ArrayList<>();
         if (!mergedChannelData.isEmpty()) {
            rows.add(new Row(Component.translatable("fml.modmismatchscreen.table.channelname"), Component.translatable("fml.modmismatchscreen.table.reason")));
            int i = 0;

            for (Entry<List<ResourceLocation>, Component> channelData : mergedChannelData.entrySet()) {
               rows.add(
                  new Row(
                     this.toChannelComponent(channelData.getKey(), i % 2 == 0 ? ChatFormatting.GOLD : ChatFormatting.YELLOW), channelData.getValue().copy()
                  )
               );
               if (++i == 30 && mergedChannelData.size() > 30) {
                  rows.add(
                     new Row(
                        Component.literal(""),
                        Component.translatable("fml.modmismatchscreen.additional", mergedChannelData.size() - i).withStyle(ChatFormatting.ITALIC)
                     )
                  );
                  break;
               }
            }

            rows.add(new Row(Component.literal(""), Component.literal("")));
         }

         this.lineTable = rows.stream().flatMap(p -> this.splitLineToWidth(p.name(), p.reason()).stream()).collect(Collectors.toList());
         this.contentSize = this.lineTable.size();
         this.scrollDistance = 0.0F;
      }

      private Map<List<ResourceLocation>, Component> sortAndMergeChannelData(Map<ResourceLocation, Component> mismatchedChannelData) {
         Map<Component, List<ResourceLocation>> channelsByReason = new LinkedHashMap<>();

         for (ResourceLocation channel : mismatchedChannelData.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList()) {
            Component channelMismatchReason = mismatchedChannelData.get(channel);
            if (channelsByReason.containsKey(channelMismatchReason)) {
               channelsByReason.get(channelMismatchReason).add(channel);
            } else {
               channelsByReason.put(channelMismatchReason, Lists.newArrayList(new ResourceLocation[]{channel}));
            }
         }

         Map<List<ResourceLocation>, Component> channelMismatchEntries = new LinkedHashMap<>();

         for (Component mismatchReason : channelsByReason.entrySet()
            .stream()
            .sorted(Comparator.comparing(entry -> entry.getValue().get(0).toString()))
            .map(Entry::getKey)
            .toList()) {
            channelMismatchEntries.put(channelsByReason.get(mismatchReason), mismatchReason);
         }

         return channelMismatchEntries;
      }

      private List<Pair<FormattedCharSequence, FormattedCharSequence>> splitLineToWidth(MutableComponent name, MutableComponent reason) {
         Style style = name.getStyle();
         List<FormattedCharSequence> nameLines = ModMismatchDisconnectedScreen.this.font.split(name, this.nameWidth - 4);
         List<FormattedCharSequence> reasonLines = ModMismatchDisconnectedScreen.this.font.split(reason.setStyle(style), this.versionWidth - 4);
         List<Pair<FormattedCharSequence, FormattedCharSequence>> splitLines = new ArrayList<>();
         int rowsOccupied = Math.max(nameLines.size(), reasonLines.size());

         for (int i = 0; i < rowsOccupied; i++) {
            splitLines.add(
               Pair.of(
                  i < nameLines.size() ? nameLines.get(i) : FormattedCharSequence.EMPTY,
                  i < reasonLines.size() ? reasonLines.get(i) : FormattedCharSequence.EMPTY
               )
            );
         }

         return splitLines;
      }

      private MutableComponent toChannelComponent(List<ResourceLocation> ids, ChatFormatting color) {
         MutableComponent namespaceComponent;
         if (this.oneChannelPerEntry) {
            namespaceComponent = Component.literal(ids.get(0).toString()).withStyle(color);
            if (ids.size() > 1) {
               namespaceComponent.append(Component.literal("\n[+%s more]".formatted(ids.size() - 1)).withStyle(ChatFormatting.DARK_GRAY));
            }
         } else {
            namespaceComponent = ComponentUtils.formatList(ids, ComponentUtils.DEFAULT_SEPARATOR, r -> Component.literal(r.toString())).withStyle(color);
         }

         return namespaceComponent;
      }

      public void toggleSimplifiedView() {
         this.oneChannelPerEntry = !this.oneChannelPerEntry;
         this.updateListContent();
      }

      @Override
      protected int getContentHeight() {
         int height = this.contentSize * (9 + 3);
         if (height < this.bottom - this.top - 4) {
            height = this.bottom - this.top - 4;
         }

         return height;
      }

      @Override
      protected void drawPanel(GuiGraphics guiGraphics, int entryRight, int relativeY, Tesselator tess, int mouseX, int mouseY) {
         int i = 0;

         for (Pair<FormattedCharSequence, FormattedCharSequence> line : this.lineTable) {
            FormattedCharSequence name = (FormattedCharSequence)line.getLeft();
            FormattedCharSequence reasons = (FormattedCharSequence)line.getRight();
            int color = Optional.ofNullable(ModMismatchDisconnectedScreen.this.font.getSplitter().componentStyleAtWidth(name, 0))
               .map(Style::getColor)
               .map(TextColor::getValue)
               .orElse(16777215);
            int nameLeft = this.left + this.border + (reasons == null ? 0 : 10);
            guiGraphics.drawString(ModMismatchDisconnectedScreen.this.font, name, nameLeft, relativeY + i * 12, color, false);
            if (reasons != null) {
               guiGraphics.drawString(
                  ModMismatchDisconnectedScreen.this.font, reasons, this.left + this.border + 10 + this.nameWidth, relativeY + i * 12, color, false
               );
            }

            i++;
         }
      }

      @Override
      public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
         super.render(guiGraphics, mouseX, mouseY, partialTicks);
         Style style = this.getComponentStyleAt(mouseX, mouseY);
         if (style != null && style.getHoverEvent() != null) {
            guiGraphics.renderComponentHoverEffect(ModMismatchDisconnectedScreen.this.font, style, mouseX, mouseY);
         }
      }

      public Style getComponentStyleAt(double x, double y) {
         if (this.isMouseOver(x, y)) {
            double relativeY = y - this.top + this.scrollDistance - this.border;
            int slotIndex = (int)(relativeY + this.border / 2) / 12;
            if (slotIndex < this.contentSize) {
               double relativeX = x - this.left - this.border - (this.lineTable.get(slotIndex).getRight() == null ? 0 : 10);
               if (relativeX >= 0.0) {
                  return ModMismatchDisconnectedScreen.this.font
                     .getSplitter()
                     .componentStyleAtWidth((FormattedCharSequence)this.lineTable.get(slotIndex).getLeft(), (int)relativeX);
               }
            }
         }

         return null;
      }

      @Override
      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         Style style = this.getComponentStyleAt(mouseX, mouseY);
         if (style != null) {
            ModMismatchDisconnectedScreen.this.handleComponentClicked(style);
            return true;
         } else {
            return super.mouseClicked(mouseX, mouseY, button);
         }
      }

      @Override
      public NarratableEntry.NarrationPriority narrationPriority() {
         return NarratableEntry.NarrationPriority.NONE;
      }

      @Override
      public void updateNarration(NarrationElementOutput output) {
      }
   }
}
