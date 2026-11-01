package net.minecraft.client.gui.screens;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.CommonLinks;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RecoverWorldDataScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SCREEN_SIDE_MARGIN = 25;
   private static final Component TITLE = Component.translatable("recover_world.title").withStyle(ChatFormatting.BOLD);
   private static final Component BUGTRACKER_BUTTON = Component.translatable("recover_world.bug_tracker");
   private static final Component RESTORE_BUTTON = Component.translatable("recover_world.restore");
   private static final Component NO_FALLBACK_TOOLTIP = Component.translatable("recover_world.no_fallback");
   private static final Component DONE_TITLE = Component.translatable("recover_world.done.title");
   private static final Component DONE_SUCCESS = Component.translatable("recover_world.done.success");
   private static final Component DONE_FAILED = Component.translatable("recover_world.done.failed");
   private static final Component NO_ISSUES = Component.translatable("recover_world.issue.none").withStyle(ChatFormatting.GREEN);
   private static final Component MISSING_FILE = Component.translatable("recover_world.issue.missing_file").withStyle(ChatFormatting.RED);
   private final BooleanConsumer callback;
   private final LinearLayout layout = LinearLayout.vertical().spacing(8);
   private final Component message;
   private final MultiLineTextWidget messageWidget;
   private final MultiLineTextWidget issuesWidget;
   private final LevelStorageSource.LevelStorageAccess storageAccess;

   public RecoverWorldDataScreen(Minecraft p_307263_, BooleanConsumer p_307619_, LevelStorageSource.LevelStorageAccess p_307488_) {
      super(TITLE);
      this.callback = p_307619_;
      this.message = Component.translatable("recover_world.message", Component.literal(p_307488_.getLevelId()).withStyle(ChatFormatting.GRAY));
      this.messageWidget = new MultiLineTextWidget(this.message, p_307263_.font);
      this.storageAccess = p_307488_;
      Exception $$3 = this.collectIssue(p_307488_, false);
      Exception $$4 = this.collectIssue(p_307488_, true);
      Component $$5 = Component.empty().append(this.buildInfo(p_307488_, false, $$3)).append("\n").append(this.buildInfo(p_307488_, true, $$4));
      this.issuesWidget = new MultiLineTextWidget($$5, p_307263_.font);
      boolean $$6 = $$3 != null && $$4 == null;
      this.layout.defaultCellSetting().alignHorizontallyCenter();
      this.layout.addChild(new StringWidget(this.title, p_307263_.font));
      this.layout.addChild(this.messageWidget.setCentered(true));
      this.layout.addChild(this.issuesWidget);
      LinearLayout $$7 = LinearLayout.horizontal().spacing(5);
      $$7.addChild(Button.builder(BUGTRACKER_BUTTON, ConfirmLinkScreen.confirmLink(this, CommonLinks.SNAPSHOT_BUGS_FEEDBACK)).size(120, 20).build());
      $$7.addChild(
            Button.builder(RESTORE_BUTTON, p_307556_ -> this.attemptRestore(p_307263_))
               .size(120, 20)
               .tooltip($$6 ? null : Tooltip.create(NO_FALLBACK_TOOLTIP))
               .build()
         )
         .active = $$6;
      this.layout.addChild($$7);
      this.layout.addChild(Button.builder(CommonComponents.GUI_BACK, p_307281_ -> this.onClose()).size(120, 20).build());
      this.layout.visitWidgets(this::addRenderableWidget);
   }

   private void attemptRestore(Minecraft p_307224_) {
      Exception $$1 = this.collectIssue(this.storageAccess, false);
      Exception $$2 = this.collectIssue(this.storageAccess, true);
      if ($$1 != null && $$2 == null) {
         p_307224_.forceSetScreen(new GenericMessageScreen(Component.translatable("recover_world.restoring")));
         EditWorldScreen.makeBackupAndShowToast(this.storageAccess);
         if (this.storageAccess.restoreLevelDataFromOld()) {
            p_307224_.setScreen(new ConfirmScreen(this.callback, DONE_TITLE, DONE_SUCCESS, CommonComponents.GUI_CONTINUE, CommonComponents.GUI_BACK));
         } else {
            p_307224_.setScreen(new AlertScreen(() -> this.callback.accept(false), DONE_TITLE, DONE_FAILED));
         }
      } else {
         LOGGER.error(
            "Failed to recover world, files not as expected. level.dat: {}, level.dat_old: {}",
            $$1 != null ? $$1.getMessage() : "no issues",
            $$2 != null ? $$2.getMessage() : "no issues"
         );
         p_307224_.setScreen(new AlertScreen(() -> this.callback.accept(false), DONE_TITLE, DONE_FAILED));
      }
   }

   private Component buildInfo(LevelStorageSource.LevelStorageAccess p_307659_, boolean p_307410_, @Nullable Exception p_307637_) {
      if (p_307410_ && p_307637_ instanceof FileNotFoundException) {
         return Component.empty();
      } else {
         MutableComponent $$3 = Component.empty();
         Instant $$4 = p_307659_.getFileModificationTime(p_307410_);
         MutableComponent $$5 = $$4 != null
            ? Component.literal(WorldSelectionList.DATE_FORMAT.format($$4))
            : Component.translatable("recover_world.state_entry.unknown");
         $$3.append(Component.translatable("recover_world.state_entry", $$5.withStyle(ChatFormatting.GRAY)));
         if (p_307637_ == null) {
            $$3.append(NO_ISSUES);
         } else if (p_307637_ instanceof FileNotFoundException) {
            $$3.append(MISSING_FILE);
         } else if (p_307637_ instanceof ReportedNbtException) {
            $$3.append(Component.literal(p_307637_.getCause().toString()).withStyle(ChatFormatting.RED));
         } else {
            $$3.append(Component.literal(p_307637_.toString()).withStyle(ChatFormatting.RED));
         }

         return $$3;
      }
   }

   @Nullable
   private Exception collectIssue(LevelStorageSource.LevelStorageAccess p_307217_, boolean p_307519_) {
      try {
         if (!p_307519_) {
            p_307217_.getSummary(p_307217_.getDataTag());
         } else {
            p_307217_.getSummary(p_307217_.getDataTagFallback());
         }

         return null;
      } catch (NbtException | ReportedNbtException | IOException var4) {
         return var4;
      }
   }

   @Override
   protected void init() {
      super.init();
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.issuesWidget.setMaxWidth(this.width - 50);
      this.messageWidget.setMaxWidth(this.width - 50);
      this.layout.arrangeElements();
      FrameLayout.centerInRectangle(this.layout, this.getRectangle());
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), this.message);
   }

   @Override
   public void onClose() {
      this.callback.accept(false);
   }
}
