package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
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
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
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
   private static final Identifier FORGE_EXPERIMENTAL_WARNING_ICON = Identifier.fromNamespaceAndPath("neoforge", "textures/gui/experimental_warning.png");
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
      Screen p_438874_,
      Minecraft p_239541_,
      int p_239542_,
      int p_239543_,
      String p_239547_,
      @Nullable WorldSelectionList p_239548_,
      @Nullable Consumer<LevelSummary> p_440010_,
      @Nullable Consumer<WorldSelectionList.WorldListEntry> p_439062_,
      WorldSelectionList.EntryType p_440549_
   ) {
      super(p_239541_, p_239542_, p_239543_, 0, 36);
      this.screen = p_438874_;
      this.loadingHeader = new WorldSelectionList.LoadingHeader(p_239541_);
      this.filter = p_239547_;
      this.onEntrySelect = p_440010_;
      this.onEntryInteract = p_439062_;
      this.entryType = p_440549_;
      if (p_239548_ != null) {
         this.pendingLevels = p_239548_.pendingLevels;
      } else {
         this.pendingLevels = this.loadLevels();
      }

      this.addEntry(this.loadingHeader);
      this.handleNewLevels(this.pollLevelsIgnoreErrors());
   }

   @Override
   public void clearEntries() {
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
      } catch (CompletionException | CancellationException var2) {
         return null;
      }
   }

   public void reloadWorldList() {
      this.pendingLevels = this.loadLevels();
   }

   @Override
   public void renderWidget(GuiGraphics p_313849_, int p_313762_, int p_313812_, float p_313854_) {
      List<LevelSummary> list = this.pollLevelsIgnoreErrors();
      if (list != this.currentlyDisplayedLevels) {
         this.handleNewLevels(list);
      }

      super.renderWidget(p_313849_, p_313762_, p_313812_, p_313854_);
   }

   private void handleNewLevels(@Nullable List<LevelSummary> p_239665_) {
      if (p_239665_ != null) {
         if (p_239665_.isEmpty()) {
            switch (this.entryType) {
               case SINGLEPLAYER:
                  CreateWorldScreen.openFresh(this.minecraft, () -> this.minecraft.setScreen(null));
                  break;
               case UPLOAD_WORLD:
                  this.clearEntries();
                  this.addEntry(new WorldSelectionList.NoWorldsEntry(Component.translatable("mco.upload.select.world.none"), this.screen.getFont()));
            }
         } else {
            this.fillLevels(this.filter, p_239665_);
            this.currentlyDisplayedLevels = p_239665_;
         }
      }
   }

   public void updateFilter(String p_239901_) {
      if (this.currentlyDisplayedLevels != null && !p_239901_.equals(this.filter)) {
         this.fillLevels(p_239901_, this.currentlyDisplayedLevels);
      }

      this.filter = p_239901_;
   }

   private CompletableFuture<List<LevelSummary>> loadLevels() {
      LevelStorageSource.LevelCandidates levelstoragesource$levelcandidates;
      try {
         levelstoragesource$levelcandidates = this.minecraft.getLevelSource().findLevelCandidates();
      } catch (LevelStorageException var3) {
         LOGGER.error("Couldn't load level list", var3);
         this.handleLevelLoadFailure(var3.getMessageComponent());
         return CompletableFuture.completedFuture(List.of());
      }

      return this.minecraft.getLevelSource().loadLevelSummaries(levelstoragesource$levelcandidates).exceptionally(p_233202_ -> {
         this.minecraft.delayCrash(CrashReport.forThrowable(p_233202_, "Couldn't load level list"));
         return List.of();
      });
   }

   private void fillLevels(String p_233199_, List<LevelSummary> p_233200_) {
      List<WorldSelectionList.Entry> list = new ArrayList<>();
      Optional<WorldSelectionList.WorldListEntry> optional = this.getSelectedOpt();
      WorldSelectionList.WorldListEntry worldselectionlist$worldlistentry = null;

      for (LevelSummary levelsummary : p_233200_.stream().filter(p_438758_ -> this.filterAccepts(p_233199_.toLowerCase(Locale.ROOT), p_438758_)).toList()) {
         WorldSelectionList.WorldListEntry worldselectionlist$worldlistentry1 = new WorldSelectionList.WorldListEntry(this, levelsummary);
         if (optional.isPresent() && optional.get().getLevelSummary().getLevelId().equals(worldselectionlist$worldlistentry1.getLevelSummary().getLevelId())) {
            worldselectionlist$worldlistentry = worldselectionlist$worldlistentry1;
         }

         list.add(worldselectionlist$worldlistentry1);
      }

      this.removeEntries(this.children().stream().filter(p_438755_ -> !list.contains(p_438755_)).toList());
      list.forEach(p_438756_ -> {
         if (!this.children().contains(p_438756_)) {
            this.addEntry(p_438756_);
         }
      });
      this.setSelected((WorldSelectionList.Entry)worldselectionlist$worldlistentry);
      this.notifyListUpdated();
   }

   private boolean filterAccepts(String p_233196_, LevelSummary p_233197_) {
      return p_233197_.getLevelName().toLowerCase(Locale.ROOT).contains(p_233196_) || p_233197_.getLevelId().toLowerCase(Locale.ROOT).contains(p_233196_);
   }

   private void notifyListUpdated() {
      this.refreshScrollAmount();
      this.screen.triggerImmediateNarration(true);
   }

   private void handleLevelLoadFailure(Component p_233212_) {
      this.minecraft.setScreen(new ErrorScreen(Component.translatable("selectWorld.unable_to_load"), p_233212_));
   }

   @Override
   public int getRowWidth() {
      return 270;
   }

   public void setSelected(@Nullable WorldSelectionList.Entry p_233190_) {
      super.setSelected(p_233190_);
      if (this.onEntrySelect != null) {
         this.onEntrySelect
            .accept(p_233190_ instanceof WorldSelectionList.WorldListEntry worldselectionlist$worldlistentry ? worldselectionlist$worldlistentry.summary : null);
      }
   }

   public Optional<WorldSelectionList.WorldListEntry> getSelectedOpt() {
      WorldSelectionList.Entry worldselectionlist$entry = this.getSelected();
      return worldselectionlist$entry instanceof WorldSelectionList.WorldListEntry worldselectionlist$worldlistentry
         ? Optional.of(worldselectionlist$worldlistentry)
         : Optional.empty();
   }

   public void returnToScreen() {
      this.reloadWorldList();
      this.minecraft.setScreen(this.screen);
   }

   public Screen getScreen() {
      return this.screen;
   }

   @Override
   public void updateWidgetNarration(NarrationElementOutput p_313871_) {
      if (this.children().contains(this.loadingHeader)) {
         this.loadingHeader.updateNarration(p_313871_);
      } else {
         super.updateWidgetNarration(p_313871_);
      }
   }

   @OnlyIn(Dist.CLIENT)
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

      public Builder(Minecraft p_439915_, Screen p_440517_) {
         this.minecraft = p_439915_;
         this.screen = p_440517_;
      }

      public WorldSelectionList.Builder width(int p_440180_) {
         this.width = p_440180_;
         return this;
      }

      public WorldSelectionList.Builder height(int p_440505_) {
         this.height = p_440505_;
         return this;
      }

      public WorldSelectionList.Builder filter(String p_440113_) {
         this.filter = p_440113_;
         return this;
      }

      public WorldSelectionList.Builder oldList(@Nullable WorldSelectionList p_439132_) {
         this.oldList = p_439132_;
         return this;
      }

      public WorldSelectionList.Builder onEntrySelect(Consumer<LevelSummary> p_439817_) {
         this.onEntrySelect = p_439817_;
         return this;
      }

      public WorldSelectionList.Builder onEntryInteract(Consumer<WorldSelectionList.WorldListEntry> p_439082_) {
         this.onEntryInteract = p_439082_;
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

   @OnlyIn(Dist.CLIENT)
   public abstract static class Entry extends ObjectSelectionList.Entry<WorldSelectionList.Entry> implements AutoCloseable {
      @Override
      public void close() {
      }

      @Nullable
      public LevelSummary getLevelSummary() {
         return null;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum EntryType {
      SINGLEPLAYER,
      UPLOAD_WORLD;
   }

   @OnlyIn(Dist.CLIENT)
   public static class LoadingHeader extends WorldSelectionList.Entry {
      private static final Component LOADING_LABEL = Component.translatable("selectWorld.loading_list");
      private final Minecraft minecraft;

      public LoadingHeader(Minecraft p_233222_) {
         this.minecraft = p_233222_;
      }

      @Override
      public void renderContent(GuiGraphics p_440681_, int p_439545_, int p_440044_, boolean p_440358_, float p_439550_) {
         int i = (this.minecraft.screen.width - this.minecraft.font.width(LOADING_LABEL)) / 2;
         int j = this.getContentY() + (this.getContentHeight() - 9) / 2;
         p_440681_.drawString(this.minecraft.font, LOADING_LABEL, i, j, -1);
         String s = LoadingDotsText.get(Util.getMillis());
         int k = (this.minecraft.screen.width - this.minecraft.font.width(s)) / 2;
         int l = j + 9;
         p_440681_.drawString(this.minecraft.font, s, k, l, -8355712);
      }

      @Override
      public Component getNarration() {
         return LOADING_LABEL;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static final class NoWorldsEntry extends WorldSelectionList.Entry {
      private final StringWidget stringWidget;

      public NoWorldsEntry(Component p_439471_, Font p_439733_) {
         this.stringWidget = new StringWidget(p_439471_, p_439733_);
      }

      @Override
      public Component getNarration() {
         return this.stringWidget.getMessage();
      }

      @Override
      public void renderContent(GuiGraphics p_439096_, int p_440638_, int p_439304_, boolean p_439417_, float p_440392_) {
         this.stringWidget
            .setPosition(this.getContentXMiddle() - this.stringWidget.getWidth() / 2, this.getContentYMiddle() - this.stringWidget.getHeight() / 2);
         this.stringWidget.render(p_439096_, p_440638_, p_439304_, p_440392_);
      }
   }

   @OnlyIn(Dist.CLIENT)
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

      public WorldListEntry(WorldSelectionList p_460812_, LevelSummary p_101703_) {
         this.list = p_460812_;
         this.minecraft = p_460812_.minecraft;
         this.screen = p_460812_.getScreen();
         this.summary = p_101703_;
         this.icon = FaviconTexture.forWorld(this.minecraft.getTextureManager(), p_101703_.getLevelId());
         this.iconFile = p_101703_.getIcon();
         int i = p_460812_.getRowWidth() - this.getTextX() - 2;
         Component component = Component.literal(p_101703_.getLevelName());
         this.worldNameText = new StringWidget(component, this.minecraft.font);
         this.worldNameText.setMaxWidth(i);
         if (this.minecraft.font.width(component) > i) {
            this.worldNameText.setTooltip(Tooltip.create(component));
         }

         String s = p_101703_.getLevelId();
         long j = p_101703_.getLastPlayed();
         if (j != -1L) {
            ZonedDateTime zoneddatetime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(j), ZoneId.systemDefault());
            s = s + " (" + WorldSelectionList.DATE_FORMAT.format(zoneddatetime) + ")";
         }

         Component component2 = Component.literal(s).withColor(-8355712);
         this.idAndLastPlayedText = new StringWidget(component2, this.minecraft.font);
         this.idAndLastPlayedText.setMaxWidth(i);
         if (this.minecraft.font.width(s) > i) {
            this.idAndLastPlayedText.setTooltip(Tooltip.create(component2));
         }

         Component component1 = ComponentUtils.mergeStyles(p_101703_.getInfo(), Style.EMPTY.withColor(-8355712));
         this.infoText = new StringWidget(component1, this.minecraft.font);
         this.infoText.setMaxWidth(i);
         if (this.minecraft.font.width(component1) > i) {
            this.infoText.setTooltip(Tooltip.create(component1));
         }

         this.validateIconFile();
         this.loadIcon();
      }

      private void validateIconFile() {
         if (this.iconFile != null) {
            try {
               BasicFileAttributes basicfileattributes = Files.readAttributes(this.iconFile, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
               if (basicfileattributes.isSymbolicLink()) {
                  List<ForbiddenSymlinkInfo> list = this.minecraft.directoryValidator().validateSymlink(this.iconFile);
                  if (!list.isEmpty()) {
                     WorldSelectionList.LOGGER.warn("{}", ContentValidationException.getMessage(this.iconFile, list));
                     this.iconFile = null;
                  } else {
                     basicfileattributes = Files.readAttributes(this.iconFile, BasicFileAttributes.class);
                  }
               }

               if (!basicfileattributes.isRegularFile()) {
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
      public void renderContent(GuiGraphics p_439856_, int p_439492_, int p_440095_, boolean p_438893_, float p_439455_) {
         int i = this.getTextX();
         this.worldNameText.setPosition(i, this.getContentY() + 1);
         this.worldNameText.render(p_439856_, p_439492_, p_440095_, p_439455_);
         this.idAndLastPlayedText.setPosition(i, this.getContentY() + 9 + 3);
         this.idAndLastPlayedText.render(p_439856_, p_439492_, p_440095_, p_439455_);
         this.infoText.setPosition(i, this.getContentY() + 9 + 9 + 3);
         this.infoText.render(p_439856_, p_439492_, p_440095_, p_439455_);
         p_439856_.blit(RenderPipelines.GUI_TEXTURED, this.icon.textureLocation(), this.getContentX(), this.getContentY(), 0.0F, 0.0F, 32, 32, 32, 32);
         this.renderExperimentalWarning(p_439856_, p_439492_, p_440095_, p_438893_);
         if (this.list.entryType == WorldSelectionList.EntryType.SINGLEPLAYER && (this.minecraft.options.touchscreen().get() || p_438893_)) {
            p_439856_.fill(this.getContentX(), this.getContentY(), this.getContentX() + 32, this.getContentY() + 32, -1601138544);
            int j = p_439492_ - this.getContentX();
            int k = p_440095_ - this.getContentY();
            boolean flag = this.mouseOverIcon(j, k, 32);
            Identifier identifier = flag ? WorldSelectionList.JOIN_HIGHLIGHTED_SPRITE : WorldSelectionList.JOIN_SPRITE;
            Identifier identifier1 = flag ? WorldSelectionList.WARNING_HIGHLIGHTED_SPRITE : WorldSelectionList.WARNING_SPRITE;
            Identifier identifier2 = flag ? WorldSelectionList.ERROR_HIGHLIGHTED_SPRITE : WorldSelectionList.ERROR_SPRITE;
            Identifier identifier3 = flag ? WorldSelectionList.MARKED_JOIN_HIGHLIGHTED_SPRITE : WorldSelectionList.MARKED_JOIN_SPRITE;
            if (this.summary instanceof LevelSummary.SymlinkLevelSummary || this.summary instanceof LevelSummary.CorruptedLevelSummary) {
               p_439856_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier2, this.getContentX(), this.getContentY(), 32, 32);
               p_439856_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier3, this.getContentX(), this.getContentY(), 32, 32);
               return;
            }

            if (this.summary.isLocked()) {
               p_439856_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier2, this.getContentX(), this.getContentY(), 32, 32);
               if (flag) {
                  p_439856_.setTooltipForNextFrame(this.minecraft.font.split(WorldSelectionList.WORLD_LOCKED_TOOLTIP, 175), p_439492_, p_440095_);
               }
            } else if (this.summary.requiresManualConversion()) {
               p_439856_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier2, this.getContentX(), this.getContentY(), 32, 32);
               if (flag) {
                  p_439856_.setTooltipForNextFrame(this.minecraft.font.split(WorldSelectionList.WORLD_REQUIRES_CONVERSION, 175), p_439492_, p_440095_);
               }
            } else if (!this.summary.isCompatible()) {
               p_439856_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier2, this.getContentX(), this.getContentY(), 32, 32);
               if (flag) {
                  p_439856_.setTooltipForNextFrame(this.minecraft.font.split(WorldSelectionList.INCOMPATIBLE_VERSION_TOOLTIP, 175), p_439492_, p_440095_);
               }
            } else if (this.summary.shouldBackup()) {
               p_439856_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier3, this.getContentX(), this.getContentY(), 32, 32);
               if (this.summary.isDowngrade()) {
                  p_439856_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier2, this.getContentX(), this.getContentY(), 32, 32);
                  if (flag) {
                     p_439856_.setTooltipForNextFrame(
                        ImmutableList.of(
                           WorldSelectionList.FROM_NEWER_TOOLTIP_1.getVisualOrderText(), WorldSelectionList.FROM_NEWER_TOOLTIP_2.getVisualOrderText()
                        ),
                        p_439492_,
                        p_440095_
                     );
                  }
               } else if (!SharedConstants.getCurrentVersion().stable()) {
                  p_439856_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier1, this.getContentX(), this.getContentY(), 32, 32);
                  if (flag) {
                     p_439856_.setTooltipForNextFrame(
                        ImmutableList.of(WorldSelectionList.SNAPSHOT_TOOLTIP_1.getVisualOrderText(), WorldSelectionList.SNAPSHOT_TOOLTIP_2.getVisualOrderText()),
                        p_439492_,
                        p_440095_
                     );
                  }
               }

               if (flag) {
                  WorldSelectionList.this.handleCursor(p_439856_);
               }
            } else {
               p_439856_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getContentX(), this.getContentY(), 32, 32);
               if (flag) {
                  WorldSelectionList.this.handleCursor(p_439856_);
               }
            }
         }
      }

      private int getTextX() {
         return this.getContentX() + 32 + 3;
      }

      @Override
      public boolean mouseClicked(MouseButtonEvent p_447041_, boolean p_435089_) {
         if (this.canInteract()) {
            int i = (int)p_447041_.x() - this.getContentX();
            int j = (int)p_447041_.y() - this.getContentY();
            if (p_435089_ || this.mouseOverIcon(i, j, 32) && this.list.entryType == WorldSelectionList.EntryType.SINGLEPLAYER) {
               this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
               Consumer<WorldSelectionList.WorldListEntry> consumer = this.list.onEntryInteract;
               if (consumer != null) {
                  consumer.accept(this);
                  return true;
               }
            }
         }

         return super.mouseClicked(p_447041_, p_435089_);
      }

      @Override
      public boolean keyPressed(KeyEvent p_445653_) {
         if (p_445653_.isSelection() && this.canInteract()) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            Consumer<WorldSelectionList.WorldListEntry> consumer = this.list.onEntryInteract;
            if (consumer != null) {
               consumer.accept(this);
               return true;
            }
         }

         return super.keyPressed(p_445653_);
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
                  p_438759_ -> {
                     if (p_438759_) {
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
         LevelStorageSource levelstoragesource = this.minecraft.getLevelSource();
         String s = this.summary.getLevelId();

         try (LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = levelstoragesource.createAccess(s)) {
            levelstoragesource$levelstorageaccess.deleteLevel();
         } catch (IOException var8) {
            SystemToast.onWorldDeleteFailure(this.minecraft, s);
            WorldSelectionList.LOGGER.error("Failed to delete world {}", s, var8);
         }
      }

      public void editWorld() {
         this.queueLoadScreen();
         String s = this.summary.getLevelId();

         LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess;
         try {
            levelstoragesource$levelstorageaccess = this.minecraft.getLevelSource().validateAndCreateAccess(s);
         } catch (IOException var6) {
            SystemToast.onWorldAccessFailure(this.minecraft, s);
            WorldSelectionList.LOGGER.error("Failed to access level {}", s, var6);
            this.list.reloadWorldList();
            return;
         } catch (ContentValidationException var7) {
            WorldSelectionList.LOGGER.warn("{}", var7.getMessage());
            this.minecraft.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.setScreen(this.screen)));
            return;
         }

         EditWorldScreen editworldscreen;
         try {
            editworldscreen = EditWorldScreen.create(this.minecraft, levelstoragesource$levelstorageaccess, p_438765_ -> {
               levelstoragesource$levelstorageaccess.safeClose();
               this.list.returnToScreen();
            });
         } catch (ReportedNbtException | IOException | NbtException var5) {
            levelstoragesource$levelstorageaccess.safeClose();
            SystemToast.onWorldAccessFailure(this.minecraft, s);
            WorldSelectionList.LOGGER.error("Failed to load world data {}", s, var5);
            this.list.reloadWorldList();
            return;
         }

         this.minecraft.setScreen(editworldscreen);
      }

      public void recreateWorld() {
         this.queueLoadScreen();

         try (LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = this.minecraft
               .getLevelSource()
               .validateAndCreateAccess(this.summary.getLevelId())) {
            Pair<LevelSettings, WorldCreationContext> pair = this.minecraft.createWorldOpenFlows().recreateWorldData(levelstoragesource$levelstorageaccess);
            LevelSettings levelsettings = (LevelSettings)pair.getFirst();
            WorldCreationContext worldcreationcontext = (WorldCreationContext)pair.getSecond();
            Path path = CreateWorldScreen.createTempDataPackDirFromExistingWorld(
               levelstoragesource$levelstorageaccess.getLevelPath(LevelResource.DATAPACK_DIR), this.minecraft
            );
            worldcreationcontext.validate();
            if (worldcreationcontext.options().isOldCustomizedWorld()) {
               this.minecraft
                  .setScreen(
                     new ConfirmScreen(
                        p_438763_ -> this.minecraft
                           .setScreen(
                              (Screen)(p_438763_
                                 ? CreateWorldScreen.createFromExisting(this.minecraft, this.list::returnToScreen, levelsettings, worldcreationcontext, path)
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
                  .setScreen(CreateWorldScreen.createFromExisting(this.minecraft, this.list::returnToScreen, levelsettings, worldcreationcontext, path));
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
         boolean flag = this.iconFile != null && Files.isRegularFile(this.iconFile);
         if (flag) {
            try (InputStream inputstream = Files.newInputStream(this.iconFile)) {
               this.icon.upload(NativeImage.read(inputstream));
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

      private void renderExperimentalWarning(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered) {
         if (this.summary.getSettings() != null && this.summary.getSettings().getLifecycle().equals(Lifecycle.experimental())) {
            int leftStart = this.getContentX() + this.getContentWidth();
            guiGraphics.blit(
               RenderPipelines.GUI_TEXTURED, WorldSelectionList.FORGE_EXPERIMENTAL_WARNING_ICON, leftStart - 36, this.getContentY(), 0.0F, 0.0F, 32, 32, 32, 32
            );
            if (hovered && mouseX > leftStart - 36 && mouseX < leftStart) {
               Font font = Minecraft.getInstance().font;
               List<FormattedCharSequence> tooltip = font.split(Component.translatable("neoforge.experimentalsettings.tooltip"), 200);
               guiGraphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
            }
         }
      }
   }
}
