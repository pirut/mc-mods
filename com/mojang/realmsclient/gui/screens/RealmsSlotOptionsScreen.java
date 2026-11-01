package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.ImmutableList;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsLabel;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsSlotOptionsScreen extends RealmsScreen {
   private static final int DEFAULT_DIFFICULTY = 2;
   public static final List<Difficulty> DIFFICULTIES = ImmutableList.of(Difficulty.PEACEFUL, Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD);
   private static final int DEFAULT_GAME_MODE = 0;
   public static final List<GameType> GAME_MODES = ImmutableList.of(GameType.SURVIVAL, GameType.CREATIVE, GameType.ADVENTURE);
   private static final Component NAME_LABEL = Component.translatable("mco.configure.world.edit.slot.name");
   static final Component SPAWN_PROTECTION_TEXT = Component.translatable("mco.configure.world.spawnProtection");
   private EditBox nameEdit;
   protected final RealmsConfigureWorldScreen parentScreen;
   private int column1X;
   private int columnWidth;
   private final RealmsWorldOptions options;
   private final RealmsServer.WorldType worldType;
   private Difficulty difficulty;
   private GameType gameMode;
   private final String defaultSlotName;
   private String worldName;
   private boolean pvp;
   private boolean spawnNPCs;
   private boolean spawnAnimals;
   private boolean spawnMonsters;
   int spawnProtection;
   private boolean commandBlocks;
   private boolean forceGameMode;
   RealmsSlotOptionsScreen.SettingsSlider spawnProtectionButton;

   public RealmsSlotOptionsScreen(RealmsConfigureWorldScreen p_89886_, RealmsWorldOptions p_89887_, RealmsServer.WorldType p_89888_, int p_89889_) {
      super(Component.translatable("mco.configure.world.buttons.options"));
      this.parentScreen = p_89886_;
      this.options = p_89887_;
      this.worldType = p_89888_;
      this.difficulty = findByIndex(DIFFICULTIES, p_89887_.difficulty, 2);
      this.gameMode = findByIndex(GAME_MODES, p_89887_.gameMode, 0);
      this.defaultSlotName = p_89887_.getDefaultSlotName(p_89889_);
      this.setWorldName(p_89887_.getSlotName(p_89889_));
      if (p_89888_ == RealmsServer.WorldType.NORMAL) {
         this.pvp = p_89887_.pvp;
         this.spawnProtection = p_89887_.spawnProtection;
         this.forceGameMode = p_89887_.forceGameMode;
         this.spawnAnimals = p_89887_.spawnAnimals;
         this.spawnMonsters = p_89887_.spawnMonsters;
         this.spawnNPCs = p_89887_.spawnNPCs;
         this.commandBlocks = p_89887_.commandBlocks;
      } else {
         this.pvp = true;
         this.spawnProtection = 0;
         this.forceGameMode = false;
         this.spawnAnimals = true;
         this.spawnMonsters = true;
         this.spawnNPCs = true;
         this.commandBlocks = true;
      }
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parentScreen);
   }

   private static <T> T findByIndex(List<T> p_167525_, int p_167526_, int p_167527_) {
      try {
         return p_167525_.get(p_167526_);
      } catch (IndexOutOfBoundsException var4) {
         return p_167525_.get(p_167527_);
      }
   }

   private static <T> int findIndex(List<T> p_167529_, T p_167530_, int p_167531_) {
      int $$3 = p_167529_.indexOf(p_167530_);
      return $$3 == -1 ? p_167531_ : $$3;
   }

   @Override
   public void init() {
      this.columnWidth = 170;
      this.column1X = this.width / 2 - this.columnWidth;
      int $$0 = this.width / 2 + 10;
      if (this.worldType != RealmsServer.WorldType.NORMAL) {
         Component $$1;
         if (this.worldType == RealmsServer.WorldType.ADVENTUREMAP) {
            $$1 = Component.translatable("mco.configure.world.edit.subscreen.adventuremap");
         } else if (this.worldType == RealmsServer.WorldType.INSPIRATION) {
            $$1 = Component.translatable("mco.configure.world.edit.subscreen.inspiration");
         } else {
            $$1 = Component.translatable("mco.configure.world.edit.subscreen.experience");
         }

         this.addLabel(new RealmsLabel($$1, this.width / 2, 26, 16711680));
      }

      this.nameEdit = this.addWidget(
         new EditBox(this.minecraft.font, this.column1X, row(1), this.columnWidth, 20, null, Component.translatable("mco.configure.world.edit.slot.name"))
      );
      this.nameEdit.setMaxLength(10);
      this.nameEdit.setValue(this.worldName);
      this.nameEdit.setResponder(this::setWorldName);
      CycleButton<Boolean> $$4 = this.addRenderableWidget(
         CycleButton.onOffBuilder(this.pvp)
            .create($$0, row(1), this.columnWidth, 20, Component.translatable("mco.configure.world.pvp"), (p_167546_, p_167547_) -> this.pvp = p_167547_)
      );
      this.addRenderableWidget(
         CycleButton.builder(GameType::getShortDisplayName)
            .withValues(GAME_MODES)
            .withInitialValue(this.gameMode)
            .create(
               this.column1X, row(3), this.columnWidth, 20, Component.translatable("selectWorld.gameMode"), (p_167515_, p_167516_) -> this.gameMode = p_167516_
            )
      );
      Component $$5 = Component.translatable("mco.configure.world.spawn_toggle.message");
      CycleButton<Boolean> $$6 = this.addRenderableWidget(
         CycleButton.onOffBuilder(this.spawnAnimals)
            .create(
               $$0,
               row(3),
               this.columnWidth,
               20,
               Component.translatable("mco.configure.world.spawnAnimals"),
               this.confirmDangerousOption($$5, p_231329_ -> this.spawnAnimals = p_231329_)
            )
      );
      CycleButton<Boolean> $$7 = CycleButton.onOffBuilder(this.difficulty != Difficulty.PEACEFUL && this.spawnMonsters)
         .create(
            $$0,
            row(5),
            this.columnWidth,
            20,
            Component.translatable("mco.configure.world.spawnMonsters"),
            this.confirmDangerousOption($$5, p_231327_ -> this.spawnMonsters = p_231327_)
         );
      this.addRenderableWidget(
         CycleButton.builder(Difficulty::getDisplayName)
            .withValues(DIFFICULTIES)
            .withInitialValue(this.difficulty)
            .create(this.column1X, row(5), this.columnWidth, 20, Component.translatable("options.difficulty"), (p_167519_, p_167520_) -> {
               this.difficulty = p_167520_;
               if (this.worldType == RealmsServer.WorldType.NORMAL) {
                  boolean $$3 = this.difficulty != Difficulty.PEACEFUL;
                  $$7.active = $$3;
                  $$7.setValue($$3 && this.spawnMonsters);
               }
            })
      );
      this.addRenderableWidget($$7);
      this.spawnProtectionButton = this.addRenderableWidget(
         new RealmsSlotOptionsScreen.SettingsSlider(this.column1X, row(7), this.columnWidth, this.spawnProtection, 0.0F, 16.0F)
      );
      CycleButton<Boolean> $$8 = this.addRenderableWidget(
         CycleButton.onOffBuilder(this.spawnNPCs)
            .create(
               $$0,
               row(7),
               this.columnWidth,
               20,
               Component.translatable("mco.configure.world.spawnNPCs"),
               this.confirmDangerousOption(Component.translatable("mco.configure.world.spawn_toggle.message.npc"), p_231312_ -> this.spawnNPCs = p_231312_)
            )
      );
      CycleButton<Boolean> $$9 = this.addRenderableWidget(
         CycleButton.onOffBuilder(this.forceGameMode)
            .create(
               this.column1X,
               row(9),
               this.columnWidth,
               20,
               Component.translatable("mco.configure.world.forceGameMode"),
               (p_167534_, p_167535_) -> this.forceGameMode = p_167535_
            )
      );
      CycleButton<Boolean> $$10 = this.addRenderableWidget(
         CycleButton.onOffBuilder(this.commandBlocks)
            .create(
               $$0,
               row(9),
               this.columnWidth,
               20,
               Component.translatable("mco.configure.world.commandBlocks"),
               (p_167522_, p_167523_) -> this.commandBlocks = p_167523_
            )
      );
      if (this.worldType != RealmsServer.WorldType.NORMAL) {
         $$4.active = false;
         $$6.active = false;
         $$8.active = false;
         $$7.active = false;
         this.spawnProtectionButton.active = false;
         $$10.active = false;
         $$9.active = false;
      }

      if (this.difficulty == Difficulty.PEACEFUL) {
         $$7.active = false;
      }

      this.addRenderableWidget(
         Button.builder(Component.translatable("mco.configure.world.buttons.done"), p_89910_ -> this.saveSettings())
            .bounds(this.column1X, row(13), this.columnWidth, 20)
            .build()
      );
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, p_349782_ -> this.onClose()).bounds($$0, row(13), this.columnWidth, 20).build());
   }

   private CycleButton.OnValueChange<Boolean> confirmDangerousOption(Component p_231324_, Consumer<Boolean> p_231325_) {
      return (p_349778_, p_349779_) -> {
         if (p_349779_) {
            p_231325_.accept(true);
         } else {
            this.minecraft.setScreen(RealmsPopups.warningPopupScreen(this, p_231324_, p_349781_ -> {
               p_231325_.accept(false);
               p_349781_.onClose();
            }));
         }
      };
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(this.getTitle(), this.createLabelNarration());
   }

   @Override
   public void render(GuiGraphics p_283210_, int p_283172_, int p_281531_, float p_283191_) {
      super.render(p_283210_, p_283172_, p_281531_, p_283191_);
      p_283210_.drawCenteredString(this.font, this.title, this.width / 2, 17, -1);
      p_283210_.drawString(this.font, NAME_LABEL, this.column1X + this.columnWidth / 2 - this.font.width(NAME_LABEL) / 2, row(0) - 5, -1, false);
      this.nameEdit.render(p_283210_, p_283172_, p_281531_, p_283191_);
   }

   private void setWorldName(String p_231314_) {
      if (p_231314_.equals(this.defaultSlotName)) {
         this.worldName = "";
      } else {
         this.worldName = p_231314_;
      }
   }

   private void saveSettings() {
      int $$0 = findIndex(DIFFICULTIES, this.difficulty, 2);
      int $$1 = findIndex(GAME_MODES, this.gameMode, 0);
      if (this.worldType != RealmsServer.WorldType.ADVENTUREMAP
         && this.worldType != RealmsServer.WorldType.EXPERIENCE
         && this.worldType != RealmsServer.WorldType.INSPIRATION) {
         boolean $$2 = this.worldType == RealmsServer.WorldType.NORMAL && this.difficulty != Difficulty.PEACEFUL && this.spawnMonsters;
         this.parentScreen
            .saveSlotSettings(
               new RealmsWorldOptions(
                  this.pvp,
                  this.spawnAnimals,
                  $$2,
                  this.spawnNPCs,
                  this.spawnProtection,
                  this.commandBlocks,
                  $$0,
                  $$1,
                  this.forceGameMode,
                  this.worldName,
                  this.options.version,
                  this.options.compatibility
               )
            );
      } else {
         this.parentScreen
            .saveSlotSettings(
               new RealmsWorldOptions(
                  this.options.pvp,
                  this.options.spawnAnimals,
                  this.options.spawnMonsters,
                  this.options.spawnNPCs,
                  this.options.spawnProtection,
                  this.options.commandBlocks,
                  $$0,
                  $$1,
                  this.options.forceGameMode,
                  this.worldName,
                  this.options.version,
                  this.options.compatibility
               )
            );
      }
   }

   @OnlyIn(Dist.CLIENT)
   class SettingsSlider extends AbstractSliderButton {
      private final double minValue;
      private final double maxValue;

      public SettingsSlider(int p_89946_, int p_89947_, int p_89948_, int p_89949_, float p_89950_, float p_89951_) {
         super(p_89946_, p_89947_, p_89948_, 20, CommonComponents.EMPTY, 0.0);
         this.minValue = p_89950_;
         this.maxValue = p_89951_;
         this.value = (Mth.clamp((float)p_89949_, p_89950_, p_89951_) - p_89950_) / (p_89951_ - p_89950_);
         this.updateMessage();
      }

      @Override
      public void applyValue() {
         if (RealmsSlotOptionsScreen.this.spawnProtectionButton.active) {
            RealmsSlotOptionsScreen.this.spawnProtection = (int)Mth.lerp(Mth.clamp(this.value, 0.0, 1.0), this.minValue, this.maxValue);
         }
      }

      @Override
      protected void updateMessage() {
         this.setMessage(
            CommonComponents.optionNameValue(
               RealmsSlotOptionsScreen.SPAWN_PROTECTION_TEXT,
               (Component)(RealmsSlotOptionsScreen.this.spawnProtection == 0
                  ? CommonComponents.OPTION_OFF
                  : Component.literal(String.valueOf(RealmsSlotOptionsScreen.this.spawnProtection)))
            )
         );
      }
   }
}
