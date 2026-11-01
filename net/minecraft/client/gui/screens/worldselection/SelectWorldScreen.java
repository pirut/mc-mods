package net.minecraft.client.gui.screens.worldselection;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FileUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelSummary;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SelectWorldScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final WorldOptions TEST_OPTIONS = new WorldOptions("test1".hashCode(), true, false);
   protected final Screen lastScreen;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 49, 60);
   @Nullable
   private Button deleteButton;
   @Nullable
   private Button selectButton;
   @Nullable
   private Button renameButton;
   @Nullable
   private Button copyButton;
   @Nullable
   protected EditBox searchBox;
   @Nullable
   private WorldSelectionList list;

   public SelectWorldScreen(Screen p_101338_) {
      super(Component.translatable("selectWorld.title"));
      this.lastScreen = p_101338_;
   }

   @Override
   protected void init() {
      LinearLayout linearlayout = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
      linearlayout.defaultCellSetting().alignHorizontallyCenter();
      linearlayout.addChild(new StringWidget(this.title, this.font));
      LinearLayout linearlayout1 = linearlayout.addChild(LinearLayout.horizontal().spacing(4));
      if (SharedConstants.DEBUG_WORLD_RECREATE) {
         linearlayout1.addChild(this.createDebugWorldRecreateButton());
      }

      this.searchBox = linearlayout1.addChild(
         new EditBox(this.font, this.width / 2 - 100, 22, 200, 20, this.searchBox, Component.translatable("selectWorld.search"))
      );
      this.searchBox.setResponder(p_438753_ -> {
         if (this.list != null) {
            this.list.updateFilter(p_438753_);
         }
      });
      this.searchBox.setHint(Component.translatable("gui.selectWorld.search").setStyle(EditBox.SEARCH_HINT_STYLE));
      Consumer<WorldSelectionList.WorldListEntry> consumer = WorldSelectionList.WorldListEntry::joinWorld;
      this.list = this.layout
         .addToContents(
            new WorldSelectionList.Builder(this.minecraft, this)
               .width(this.width)
               .height(this.layout.getContentHeight())
               .filter(this.searchBox.getValue())
               .oldList(this.list)
               .onEntrySelect(this::updateButtonStatus)
               .onEntryInteract(consumer)
               .build()
         );
      this.createFooterButtons(consumer, this.list);
      this.layout.visitWidgets(p_438751_ -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_438751_);
      });
      this.repositionElements();
      this.updateButtonStatus(null);
   }

   private void createFooterButtons(Consumer<WorldSelectionList.WorldListEntry> p_447232_, WorldSelectionList p_446939_) {
      GridLayout gridlayout = this.layout.addToFooter(new GridLayout().columnSpacing(8).rowSpacing(4));
      gridlayout.defaultCellSetting().alignHorizontallyCenter();
      GridLayout.RowHelper gridlayout$rowhelper = gridlayout.createRowHelper(4);
      this.selectButton = gridlayout$rowhelper.addChild(
         Button.builder(LevelSummary.PLAY_WORLD, p_445169_ -> p_446939_.getSelectedOpt().ifPresent(p_447232_)).build(), 2
      );
      gridlayout$rowhelper.addChild(
         Button.builder(Component.translatable("selectWorld.create"), p_445173_ -> CreateWorldScreen.openFresh(this.minecraft, p_446939_::returnToScreen))
            .build(),
         2
      );
      this.renameButton = gridlayout$rowhelper.addChild(
         Button.builder(
               Component.translatable("selectWorld.edit"), p_445171_ -> p_446939_.getSelectedOpt().ifPresent(WorldSelectionList.WorldListEntry::editWorld)
            )
            .width(71)
            .build()
      );
      this.deleteButton = gridlayout$rowhelper.addChild(
         Button.builder(
               Component.translatable("selectWorld.delete"), p_445166_ -> p_446939_.getSelectedOpt().ifPresent(WorldSelectionList.WorldListEntry::deleteWorld)
            )
            .width(71)
            .build()
      );
      this.copyButton = gridlayout$rowhelper.addChild(
         Button.builder(
               Component.translatable("selectWorld.recreate"),
               p_445175_ -> p_446939_.getSelectedOpt().ifPresent(WorldSelectionList.WorldListEntry::recreateWorld)
            )
            .width(71)
            .build()
      );
      gridlayout$rowhelper.addChild(Button.builder(CommonComponents.GUI_BACK, p_280917_ -> this.minecraft.setScreen(this.lastScreen)).width(71).build());
   }

   private Button createDebugWorldRecreateButton() {
      return Button.builder(
            Component.literal("DEBUG recreate"),
            p_465545_ -> {
               try {
                  String s = "DEBUG world";
                  if (this.list != null && !this.list.children().isEmpty()) {
                     WorldSelectionList.Entry worldselectionlist$entry = this.list.children().getFirst();
                     if (worldselectionlist$entry instanceof WorldSelectionList.WorldListEntry worldselectionlist$worldlistentry
                        && worldselectionlist$worldlistentry.getLevelName().equals("DEBUG world")) {
                        worldselectionlist$worldlistentry.doDeleteWorld();
                     }
                  }

                  LevelSettings levelsettings = new LevelSettings(
                     "DEBUG world",
                     GameType.SPECTATOR,
                     false,
                     Difficulty.NORMAL,
                     true,
                     new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures()),
                     WorldDataConfiguration.DEFAULT
                  );
                  String s1 = FileUtil.findAvailableName(this.minecraft.getLevelSource().getBaseDir(), "DEBUG world", "");
                  this.minecraft.createWorldOpenFlows().createFreshLevel(s1, levelsettings, TEST_OPTIONS, WorldPresets::createNormalWorldDimensions, this);
               } catch (IOException var5) {
                  LOGGER.error("Failed to recreate the debug world", var5);
               }
            }
         )
         .width(72)
         .build();
   }

   @Override
   protected void repositionElements() {
      if (this.list != null) {
         this.list.updateSize(this.width, this.layout);
      }

      this.layout.arrangeElements();
   }

   @Override
   protected void setInitialFocus() {
      if (this.searchBox != null) {
         this.setInitialFocus(this.searchBox);
      }
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   public void updateButtonStatus(@Nullable LevelSummary p_307245_) {
      if (this.selectButton != null && this.renameButton != null && this.copyButton != null && this.deleteButton != null) {
         if (p_307245_ == null) {
            this.selectButton.setMessage(LevelSummary.PLAY_WORLD);
            this.selectButton.active = false;
            this.renameButton.active = false;
            this.copyButton.active = false;
            this.deleteButton.active = false;
         } else {
            this.selectButton.setMessage(p_307245_.primaryActionMessage());
            this.selectButton.active = p_307245_.primaryActionActive();
            this.renameButton.active = p_307245_.canEdit();
            this.copyButton.active = p_307245_.canRecreate();
            this.deleteButton.active = p_307245_.canDelete();
         }
      }
   }

   @Override
   public void removed() {
      if (this.list != null) {
         this.list.children().forEach(WorldSelectionList.Entry::close);
      }
   }
}
