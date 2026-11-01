package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.SelectableEntry;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ErrorScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class WorldSelectionList extends ObjectSelectionList<WorldSelectionList.Entry> {
   public static final DateTimeFormatter DATE_FORMAT = Util.localizedDateFormatter(FormatStyle.SHORT);
   static final Identifier ERROR_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("world_list/error_highlighted");
   static final Identifier ERROR_SPRITE = Identifier.withDefaultNamespace("world_list/error");
   static final Identifier MARKED_JOIN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("world_list/marked_join_highlighted");
   static final Identifier MARKED_JOIN_SPRITE = Identifier.withDefaultNamespace("world_list/marked_join");
   static final Identifier WARNING_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("world_list/warning_highlighted");
   static final Identifier WARNING_SPRITE = Identifier.withDefaultNamespace("world_list/warning");
   static final Identifier JOIN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("world_list/join_highlighted");
   static final Identifier JOIN_SPRITE = Identifier.withDefaultNamespace("world_list/join");
   static final Logger LOGGER = LogUtils.getLogger();
   static final Component FROM_NEWER_TOOLTIP_1 = Component.translatable("selectWorld.tooltip.fromNewerVersion1").withStyle(ChatFormatting.RED);
   static final Component FROM_NEWER_TOOLTIP_2 = Component.translatable("selectWorld.tooltip.fromNewerVersion2").withStyle(ChatFormatting.RED);
   static final Component SNAPSHOT_TOOLTIP_1 = Component.translatable("selectWorld.tooltip.snapshot1").withStyle(ChatFormatting.GOLD);
   static final Component SNAPSHOT_TOOLTIP_2 = Component.translatable("selectWorld.tooltip.snapshot2").withStyle(ChatFormatting.GOLD);
   static final Component WORLD_LOCKED_TOOLTIP = Component.translatable("selectWorld.locked").withStyle(ChatFormatting.RED);
   static final Component WORLD_REQUIRES_CONVERSION = Component.translatable("selectWorld.conversion.tooltip").withStyle(ChatFormatting.RED);
   static final Component INCOMPATIBLE_VERSION_TOOLTIP = Component.translatable("selectWorld.incompatible.tooltip").withStyle(ChatFormatting.RED);
   static final Component WORLD_EXPERIMENTAL = Component.translatable("selectWorld.experimental");
   private final Screen screen;
   private CompletableFuture<List<LevelSummary>> pendingLevels;
   @Nullable
   private List<LevelSummary> currentlyDisplayedLevels;
   private final WorldSelectionList.LoadingHeader loadingHeader;
   final WorldSelectionList.EntryType entryType;
   private String filter;
   private boolean hasPolled;
   @Nullable
   private final Consumer<LevelSummary> onEntrySelect;
   @Nullable
   final Consumer<WorldSelectionList.WorldListEntry> onEntryInteract;

   WorldSelectionList(
      Screen screen,
      Minecraft minecraft,
      int width,
      int height,
      String filter,
      @Nullable WorldSelectionList oldList,
      @Nullable Consumer<LevelSummary> onEntrySelect,
      @Nullable Consumer<WorldSelectionList.WorldListEntry> onEntryInteract,
      WorldSelectionList.EntryType entryType
   ) {
      super(minecraft, width, height, 0, 36);
      this.screen = screen;
      this.loadingHeader = new WorldSelectionList.LoadingHeader(minecraft);
      this.filter = filter;
      this.onEntrySelect = onEntrySelect;
      this.onEntryInteract = onEntryInteract;
      this.entryType = entryType;
      if (oldList != null) {
         this.pendingLevels = oldList.pendingLevels;
      } else {
         this.pendingLevels = this.loadLevels();
      }

      this.addEntry(this.loadingHeader);
      this.handleNewLevels(this.pollLevelsIgnoreErrors());
   }

   @Override
   protected void clearEntries() {
      this.children().forEach(WorldSelectionList.Entry::close);
      super.clearEntries();
   }

   @Nullable
   private List<LevelSummary> pollLevelsIgnoreErrors() {
      try {
         List<LevelSummary> list = this.pendingLevels.getNow(null);
         if (this.entryType == WorldSelectionList.EntryType.UPLOAD_WORLD) {
            if (list == null || this.hasPolled) {
               return null;
            }

            this.hasPolled = true;
            list = list.stream().filter(LevelSummary::canUpload).toList();
         }

         return list;
      } catch (CancellationException | CompletionException var2) {
         return null;
      }
   }

   public void reloadWorldList() {
      this.pendingLevels = this.loadLevels();
   }

   @Override
   public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      List<LevelSummary> list = this.pollLevelsIgnoreErrors();
      if (list != this.currentlyDisplayedLevels) {
         this.handleNewLevels(list);
      }

      super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
   }

   private void handleNewLevels(@Nullable List<LevelSummary> levels) {
      if (levels != null) {
         if (levels.isEmpty()) {
            switch (this.entryType) {
               case SINGLEPLAYER:
                  CreateWorldScreen.openFresh(this.minecraft, () -> this.minecraft.setScreen(null));
                  break;
               case UPLOAD_WORLD:
                  this.clearEntries();
                  this.addEntry(new WorldSelectionList.NoWorldsEntry(Component.translatable("mco.upload.select.world.none"), this.screen.getFont()));
            }
         } else {
            this.fillLevels(this.filter, levels);
            this.currentlyDisplayedLevels = levels;
         }
      }
   }

   public void updateFilter(String filter) {
      if (this.currentlyDisplayedLevels != null && !filter.equals(this.filter)) {
         this.fillLevels(filter, this.currentlyDisplayedLevels);
      }

      this.filter = filter;
   }

   private CompletableFuture<List<LevelSummary>> loadLevels() {
      LevelStorageSource.LevelCandidates levelCandidates;
      try {
         levelCandidates = this.minecraft.getLevelSource().findLevelCandidates();
      } catch (LevelStorageException var3) {
         LOGGER.error("Couldn't load level list", var3);
         this.handleLevelLoadFailure(var3.getMessageComponent());
         return CompletableFuture.completedFuture(List.of());
      }

      return this.minecraft.getLevelSource().loadLevelSummaries(levelCandidates).exceptionally(throwable -> {
         this.minecraft.delayCrash(CrashReport.forThrowable(throwable, "Couldn't load level list"));
         return List.of();
      });
   }

   private void fillLevels(String filter, List<LevelSummary> levels) {
      List<WorldSelectionList.Entry> list = new ArrayList<>();
      Optional<WorldSelectionList.WorldListEntry> optional = this.getSelectedOpt();
      WorldSelectionList.WorldListEntry worldListEntry = null;

      for (LevelSummary levelSummary : levels.stream().filter(levelSummaryx -> this.filterAccepts(filter.toLowerCase(Locale.ROOT), levelSummaryx)).toList()) {
         WorldSelectionList.WorldListEntry worldListEntry2 = new WorldSelectionList.WorldListEntry(this, levelSummary);
         if (optional.isPresent() && optional.get().getLevelSummary().getLevelId().equals(worldListEntry2.getLevelSummary().getLevelId())) {
            worldListEntry = worldListEntry2;
         }

         list.add(worldListEntry2);
      }

      this.removeEntries(this.children().stream().filter(entry -> !list.contains(entry)).toList());
      list.forEach(entry -> {
         if (!this.children().contains(entry)) {
            this.addEntry(entry);
         }
      });
      this.setSelected((WorldSelectionList.Entry)worldListEntry);
      this.notifyListUpdated();
   }

   private boolean filterAccepts(String filter, LevelSummary level) {
      return level.getLevelName().toLowerCase(Locale.ROOT).contains(filter) || level.getLevelId().toLowerCase(Locale.ROOT).contains(filter);
   }

   private void notifyListUpdated() {
      this.refreshScrollAmount();
      this.screen.triggerImmediateNarration(true);
   }

   private void handleLevelLoadFailure(Component exceptionMessage) {
      this.minecraft.setScreen(new ErrorScreen(Component.translatable("selectWorld.unable_to_load"), exceptionMessage));
   }

   @Override
   public int getRowWidth() {
      return 270;
   }

   public void setSelected(@Nullable WorldSelectionList.Entry entry) {
      super.setSelected(entry);
      if (this.onEntrySelect != null) {
         this.onEntrySelect.accept(entry instanceof WorldSelectionList.WorldListEntry worldListEntry ? worldListEntry.summary : null);
      }
   }

   public Optional<WorldSelectionList.WorldListEntry> getSelectedOpt() {
      WorldSelectionList.Entry entry = this.getSelected();
      return entry instanceof WorldSelectionList.WorldListEntry worldListEntry ? Optional.of(worldListEntry) : Optional.empty();
   }

   public void returnToScreen() {
      this.reloadWorldList();
      this.minecraft.setScreen(this.screen);
   }

   public Screen getScreen() {
      return this.screen;
   }

   @Override
   public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
      if (this.children().contains(this.loadingHeader)) {
         this.loadingHeader.updateNarration(narrationElementOutput);
      } else {
         super.updateWidgetNarration(narrationElementOutput);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class Builder {
      private final Minecraft minecraft;
      private final Screen screen;
      private int width;
      private int height;
      private String filter = "";
      private WorldSelectionList.EntryType type = WorldSelectionList.EntryType.SINGLEPLAYER;
      @Nullable
      private WorldSelectionList oldList = null;
      @Nullable
      private Consumer<LevelSummary> onEntrySelect = null;
      @Nullable
      private Consumer<WorldSelectionList.WorldListEntry> onEntryInteract = null;

      public Builder(Minecraft minecraft, Screen screen) {
         this.minecraft = minecraft;
         this.screen = screen;
      }

      public WorldSelectionList.Builder width(int width) {
         this.width = width;
         return this;
      }

      public WorldSelectionList.Builder height(int height) {
         this.height = height;
         return this;
      }

      public WorldSelectionList.Builder filter(String filter) {
         this.filter = filter;
         return this;
      }

      public WorldSelectionList.Builder oldList(@Nullable WorldSelectionList oldList) {
         this.oldList = oldList;
         return this;
      }

      public WorldSelectionList.Builder onEntrySelect(Consumer<LevelSummary> onEntrySelect) {
         this.onEntrySelect = onEntrySelect;
         return this;
      }

      public WorldSelectionList.Builder onEntryInteract(Consumer<WorldSelectionList.WorldListEntry> onEntryInteract) {
         this.onEntryInteract = onEntryInteract;
         return this;
      }

      public WorldSelectionList.Builder uploadWorld() {
         this.type = WorldSelectionList.EntryType.UPLOAD_WORLD;
         return this;
      }

      public WorldSelectionList build() {
         return new WorldSelectionList(
            this.screen, this.minecraft, this.width, this.height, this.filter, this.oldList, this.onEntrySelect, this.onEntryInteract, this.type
         );
      }
   }

   @Environment(EnvType.CLIENT)
   public abstract static class Entry extends ObjectSelectionList.Entry<WorldSelectionList.Entry> implements AutoCloseable {
      @Override
      public void close() {
      }

      @Nullable
      public LevelSummary getLevelSummary() {
         return null;
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum EntryType {
      SINGLEPLAYER,
      UPLOAD_WORLD;
   }

   @Environment(EnvType.CLIENT)
   public static class LoadingHeader extends WorldSelectionList.Entry {
      private static final Component LOADING_LABEL = Component.translatable("selectWorld.loading_list");
      private final Minecraft minecraft;

      public LoadingHeader(Minecraft minecraft) {
         this.minecraft = minecraft;
      }

      @Override
      public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
         int i = (this.minecraft.screen.width - this.minecraft.font.width(LOADING_LABEL)) / 2;
         int j = this.getContentY() + (this.getContentHeight() - 9) / 2;
         guiGraphics.drawString(this.minecraft.font, LOADING_LABEL, i, j, -1);
         String string = LoadingDotsText.get(Util.getMillis());
         int k = (this.minecraft.screen.width - this.minecraft.font.width(string)) / 2;
         int l = j + 9;
         guiGraphics.drawString(this.minecraft.font, string, k, l, -8355712);
      }

      @Override
      public Component getNarration() {
         return LOADING_LABEL;
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class NoWorldsEntry extends WorldSelectionList.Entry {
      private final StringWidget stringWidget;

      public NoWorldsEntry(Component message, Font font) {
         this.stringWidget = new StringWidget(message, font);
      }

      @Override
      public Component getNarration() {
         return this.stringWidget.getMessage();
      }

      @Override
      public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
         this.stringWidget
            .setPosition(this.getContentXMiddle() - this.stringWidget.getWidth() / 2, this.getContentYMiddle() - this.stringWidget.getHeight() / 2);
         this.stringWidget.render(guiGraphics, mouseX, mouseY, partialTick);
      }
   }

   @Environment(EnvType.CLIENT)
   public final class WorldListEntry extends WorldSelectionList.Entry implements SelectableEntry {
      private static final int ICON_SIZE = 32;
      private final WorldSelectionList list;
      private final Minecraft minecraft;
      private final Screen screen;
      final LevelSummary summary;
      private final FaviconTexture icon;
      private final StringWidget worldNameText;
      private final StringWidget idAndLastPlayedText;
      private final StringWidget infoText;
      @Nullable
      private Path iconFile;

      public WorldListEntry(final WorldSelectionList list, final LevelSummary summary) {
         this.list = list;
         this.minecraft = list.minecraft;
         this.screen = list.getScreen();
         this.summary = summary;
         this.icon = FaviconTexture.forWorld(this.minecraft.getTextureManager(), summary.getLevelId());
         this.iconFile = summary.getIcon();
         int i = list.getRowWidth() - this.getTextX() - 2;
         Component component = Component.literal(summary.getLevelName());
         this.worldNameText = new StringWidget(component, this.minecraft.font);
         this.worldNameText.setMaxWidth(i);
         if (this.minecraft.font.width(component) > i) {
            this.worldNameText.setTooltip(Tooltip.create(component));
         }

         String string = summary.getLevelId();
         long l = summary.getLastPlayed();
         if (l != -1L) {
            ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(l), ZoneId.systemDefault());
            string = string + " (" + WorldSelectionList.DATE_FORMAT.format(zonedDateTime) + ")";
         }

         Component component2 = Component.literal(string).withColor(-8355712);
         this.idAndLastPlayedText = new StringWidget(component2, this.minecraft.font);
         this.idAndLastPlayedText.setMaxWidth(i);
         if (this.minecraft.font.width(string) > i) {
            this.idAndLastPlayedText.setTooltip(Tooltip.create(component2));
         }

         Component component3 = ComponentUtils.mergeStyles(summary.getInfo(), Style.EMPTY.withColor(-8355712));
         this.infoText = new StringWidget(component3, this.minecraft.font);
         this.infoText.setMaxWidth(i);
         if (this.minecraft.font.width(component3) > i) {
            this.infoText.setTooltip(Tooltip.create(component3));
         }

         this.validateIconFile();
         this.loadIcon();
      }

      private void validateIconFile() {
         if (this.iconFile != null) {
            try {
               BasicFileAttributes basicFileAttributes = Files.readAttributes(this.iconFile, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
               if (basicFileAttributes.isSymbolicLink()) {
                  List<ForbiddenSymlinkInfo> list = this.minecraft.directoryValidator().validateSymlink(this.iconFile);
                  if (!list.isEmpty()) {
                     WorldSelectionList.LOGGER.warn("{}", ContentValidationException.getMessage(this.iconFile, list));
                     this.iconFile = null;
                  } else {
                     basicFileAttributes = Files.readAttributes(this.iconFile, BasicFileAttributes.class);
                  }
               }

               if (!basicFileAttributes.isRegularFile()) {
                  this.iconFile = null;
               }
            } catch (NoSuchFileException var3) {
               this.iconFile = null;
            } catch (IOException var4) {
               WorldSelectionList.LOGGER.error("could not validate symlink", var4);
               this.iconFile = null;
            }
         }
      }

      @Override
      public Component getNarration() {
         Component component = Component.translatable(
            "narrator.select.world_info", this.summary.getLevelName(), Component.translationArg(new Date(this.summary.getLastPlayed())), this.summary.getInfo()
         );
         if (this.summary.isLocked()) {
            component = CommonComponents.joinForNarration(component, WorldSelectionList.WORLD_LOCKED_TOOLTIP);
         }

         if (this.summary.isExperimental()) {
            component = CommonComponents.joinForNarration(component, WorldSelectionList.WORLD_EXPERIMENTAL);
         }

         return Component.translatable("narrator.select", component);
      }

      @Override
      public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
         int i = this.getTextX();
         this.worldNameText.setPosition(i, this.getContentY() + 1);
         this.worldNameText.render(guiGraphics, mouseX, mouseY, partialTick);
         this.idAndLastPlayedText.setPosition(i, this.getContentY() + 9 + 3);
         this.idAndLastPlayedText.render(guiGraphics, mouseX, mouseY, partialTick);
         this.infoText.setPosition(i, this.getContentY() + 9 + 9 + 3);
         this.infoText.render(guiGraphics, mouseX, mouseY, partialTick);
         guiGraphics.blit(RenderPipelines.GUI_TEXTURED, this.icon.textureLocation(), this.getContentX(), this.getContentY(), 0.0F, 0.0F, 32, 32, 32, 32);
         if (this.list.entryType == WorldSelectionList.EntryType.SINGLEPLAYER && (this.minecraft.options.touchscreen().get() || isHovering)) {
            guiGraphics.fill(this.getContentX(), this.getContentY(), this.getContentX() + 32, this.getContentY() + 32, -1601138544);
            int j = mouseX - this.getContentX();
            int k = mouseY - this.getContentY();
            boolean bl = this.mouseOverIcon(j, k, 32);
            Identifier identifier = bl ? WorldSelectionList.JOIN_HIGHLIGHTED_SPRITE : WorldSelectionList.JOIN_SPRITE;
            Identifier identifier2 = bl ? WorldSelectionList.WARNING_HIGHLIGHTED_SPRITE : WorldSelectionList.WARNING_SPRITE;
            Identifier identifier3 = bl ? WorldSelectionList.ERROR_HIGHLIGHTED_SPRITE : WorldSelectionList.ERROR_SPRITE;
            Identifier identifier4 = bl ? WorldSelectionList.MARKED_JOIN_HIGHLIGHTED_SPRITE : WorldSelectionList.MARKED_JOIN_SPRITE;
            if (this.summary instanceof LevelSummary.SymlinkLevelSummary || this.summary instanceof LevelSummary.CorruptedLevelSummary) {
               guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier3, this.getContentX(), this.getContentY(), 32, 32);
               guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier4, this.getContentX(), this.getContentY(), 32, 32);
               return;
            }

            if (this.summary.isLocked()) {
               guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier3, this.getContentX(), this.getContentY(), 32, 32);
               if (bl) {
                  guiGraphics.setTooltipForNextFrame(this.minecraft.font.split(WorldSelectionList.WORLD_LOCKED_TOOLTIP, 175), mouseX, mouseY);
               }
            } else if (this.summary.requiresManualConversion()) {
               guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier3, this.getContentX(), this.getContentY(), 32, 32);
               if (bl) {
                  guiGraphics.setTooltipForNextFrame(this.minecraft.font.split(WorldSelectionList.WORLD_REQUIRES_CONVERSION, 175), mouseX, mouseY);
               }
            } else if (!this.summary.isCompatible()) {
               guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier3, this.getContentX(), this.getContentY(), 32, 32);
               if (bl) {
                  guiGraphics.setTooltipForNextFrame(this.minecraft.font.split(WorldSelectionList.INCOMPATIBLE_VERSION_TOOLTIP, 175), mouseX, mouseY);
               }
            } else if (this.summary.shouldBackup()) {
               guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier4, this.getContentX(), this.getContentY(), 32, 32);
               if (this.summary.isDowngrade()) {
                  guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier3, this.getContentX(), this.getContentY(), 32, 32);
                  if (bl) {
                     guiGraphics.setTooltipForNextFrame(
                        ImmutableList.of(
                           WorldSelectionList.FROM_NEWER_TOOLTIP_1.getVisualOrderText(), WorldSelectionList.FROM_NEWER_TOOLTIP_2.getVisualOrderText()
                        ),
                        mouseX,
                        mouseY
                     );
                  }
               } else if (!SharedConstants.getCurrentVersion().stable()) {
                  guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier2, this.getContentX(), this.getContentY(), 32, 32);
                  if (bl) {
                     guiGraphics.setTooltipForNextFrame(
                        ImmutableList.of(WorldSelectionList.SNAPSHOT_TOOLTIP_1.getVisualOrderText(), WorldSelectionList.SNAPSHOT_TOOLTIP_2.getVisualOrderText()),
                        mouseX,
                        mouseY
                     );
                  }
               }

               if (bl) {
                  WorldSelectionList.this.handleCursor(guiGraphics);
               }
            } else {
               guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getContentX(), this.getContentY(), 32, 32);
               if (bl) {
                  WorldSelectionList.this.handleCursor(guiGraphics);
               }
            }
         }
      }

      private int getTextX() {
         return this.getContentX() + 32 + 3;
      }

      @Override
      public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
         if (this.canInteract()) {
            int i = (int)event.x() - this.getContentX();
            int j = (int)event.y() - this.getContentY();
            if (isDoubleClick || this.mouseOverIcon(i, j, 32) && this.list.entryType == WorldSelectionList.EntryType.SINGLEPLAYER) {
               this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
               Consumer<WorldSelectionList.WorldListEntry> consumer = this.list.onEntryInteract;
               if (consumer != null) {
                  consumer.accept(this);
                  return true;
               }
            }
         }

         return super.mouseClicked(event, isDoubleClick);
      }

      @Override
      public boolean keyPressed(KeyEvent event) {
         if (event.isSelection() && this.canInteract()) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            Consumer<WorldSelectionList.WorldListEntry> consumer = this.list.onEntryInteract;
            if (consumer != null) {
               consumer.accept(this);
               return true;
            }
         }

         return super.keyPressed(event);
      }

      public boolean canInteract() {
         return this.summary.primaryActionActive() || this.list.entryType == WorldSelectionList.EntryType.UPLOAD_WORLD;
      }

      public void joinWorld() {
         if (this.summary.primaryActionActive()) {
            if (this.summary instanceof LevelSummary.SymlinkLevelSummary) {
               this.minecraft.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.setScreen(this.screen)));
            } else {
               this.minecraft.createWorldOpenFlows().openWorld(this.summary.getLevelId(), this.list::returnToScreen);
            }
         }
      }

      public void deleteWorld() {
         this.minecraft
            .setScreen(
               new ConfirmScreen(
                  bl -> {
                     if (bl) {
                        this.minecraft.setScreen(new ProgressScreen(true));
                        this.doDeleteWorld();
                     }

                     this.list.returnToScreen();
                  },
                  Component.translatable("selectWorld.deleteQuestion"),
                  Component.translatable("selectWorld.deleteWarning", this.summary.getLevelName()),
                  Component.translatable("selectWorld.deleteButton"),
                  CommonComponents.GUI_CANCEL
               )
            );
      }

      public void doDeleteWorld() {
         LevelStorageSource levelStorageSource = this.minecraft.getLevelSource();
         String string = this.summary.getLevelId();

         try (LevelStorageSource.LevelStorageAccess levelStorageAccess = levelStorageSource.createAccess(string)) {
            levelStorageAccess.deleteLevel();
         } catch (IOException var8) {
            SystemToast.onWorldDeleteFailure(this.minecraft, string);
            WorldSelectionList.LOGGER.error("Failed to delete world {}", string, var8);
         }
      }

      public void editWorld() {
         this.queueLoadScreen();
         String string = this.summary.getLevelId();

         LevelStorageSource.LevelStorageAccess levelStorageAccess;
         try {
            levelStorageAccess = this.minecraft.getLevelSource().validateAndCreateAccess(string);
         } catch (IOException var6) {
            SystemToast.onWorldAccessFailure(this.minecraft, string);
            WorldSelectionList.LOGGER.error("Failed to access level {}", string, var6);
            this.list.reloadWorldList();
            return;
         } catch (ContentValidationException var7) {
            WorldSelectionList.LOGGER.warn("{}", var7.getMessage());
            this.minecraft.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.setScreen(this.screen)));
            return;
         }

         EditWorldScreen editWorldScreen;
         try {
            editWorldScreen = EditWorldScreen.create(this.minecraft, levelStorageAccess, bl -> {
               levelStorageAccess.safeClose();
               this.list.returnToScreen();
            });
         } catch (NbtException | ReportedNbtException | IOException var5) {
            levelStorageAccess.safeClose();
            SystemToast.onWorldAccessFailure(this.minecraft, string);
            WorldSelectionList.LOGGER.error("Failed to load world data {}", string, var5);
            this.list.reloadWorldList();
            return;
         }

         this.minecraft.setScreen(editWorldScreen);
      }

      public void recreateWorld() {
         this.queueLoadScreen();

         try (LevelStorageSource.LevelStorageAccess levelStorageAccess = this.minecraft.getLevelSource().validateAndCreateAccess(this.summary.getLevelId())) {
            Pair<LevelSettings, WorldCreationContext> pair = this.minecraft.createWorldOpenFlows().recreateWorldData(levelStorageAccess);
            LevelSettings levelSettings = (LevelSettings)pair.getFirst();
            WorldCreationContext worldCreationContext = (WorldCreationContext)pair.getSecond();
            Path path = CreateWorldScreen.createTempDataPackDirFromExistingWorld(levelStorageAccess.getLevelPath(LevelResource.DATAPACK_DIR), this.minecraft);
            worldCreationContext.validate();
            if (worldCreationContext.options().isOldCustomizedWorld()) {
               this.minecraft
                  .setScreen(
                     new ConfirmScreen(
                        bl -> this.minecraft
                           .setScreen(
                              (Screen)(bl
                                 ? CreateWorldScreen.createFromExisting(this.minecraft, this.list::returnToScreen, levelSettings, worldCreationContext, path)
                                 : this.screen)
                           ),
                        Component.translatable("selectWorld.recreate.customized.title"),
                        Component.translatable("selectWorld.recreate.customized.text"),
                        CommonComponents.GUI_PROCEED,
                        CommonComponents.GUI_CANCEL
                     )
                  );
            } else {
               this.minecraft
                  .setScreen(CreateWorldScreen.createFromExisting(this.minecraft, this.list::returnToScreen, levelSettings, worldCreationContext, path));
            }
         } catch (ContentValidationException var8) {
            WorldSelectionList.LOGGER.warn("{}", var8.getMessage());
            this.minecraft.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.setScreen(this.screen)));
         } catch (Exception var9) {
            WorldSelectionList.LOGGER.error("Unable to recreate world", var9);
            this.minecraft
               .setScreen(
                  new AlertScreen(
                     () -> this.minecraft.setScreen(this.screen),
                     Component.translatable("selectWorld.recreate.error.title"),
                     Component.translatable("selectWorld.recreate.error.text")
                  )
               );
         }
      }

      private void queueLoadScreen() {
         this.minecraft.setScreenAndShow(new GenericMessageScreen(Component.translatable("selectWorld.data_read")));
      }

      private void loadIcon() {
         boolean bl = this.iconFile != null && Files.isRegularFile(this.iconFile);
         if (bl) {
            try (InputStream inputStream = Files.newInputStream(this.iconFile)) {
               this.icon.upload(NativeImage.read(inputStream));
            } catch (Throwable var7) {
               WorldSelectionList.LOGGER.error("Invalid icon for world {}", this.summary.getLevelId(), var7);
               this.iconFile = null;
            }
         } else {
            this.icon.clear();
         }
      }

      @Override
      public void close() {
         if (!this.icon.isClosed()) {
            this.icon.close();
         }
      }

      public String getLevelName() {
         return this.summary.getLevelName();
      }

      @Override
      public LevelSummary getLevelSummary() {
         return this.summary;
      }
   }
}
