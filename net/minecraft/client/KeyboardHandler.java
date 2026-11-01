package net.minecraft.client;

import com.google.common.base.MoreObjects;
import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.platform.ClipboardManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;
import java.nio.file.Path;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.debug.DebugOptionsScreen;
import net.minecraft.client.gui.screens.debug.GameModeSwitcherScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChangeGameModePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.commands.GameModeCommand;
import net.minecraft.server.commands.VersionCommand;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Mth;
import net.minecraft.util.NativeModuleLister;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeatureCountTracker;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class KeyboardHandler {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int DEBUG_CRASH_TIME = 10000;
   private final Minecraft minecraft;
   private final ClipboardManager clipboardManager = new ClipboardManager();
   private long debugCrashKeyTime = -1L;
   private long debugCrashKeyReportedTime = -1L;
   private long debugCrashKeyReportedCount = -1L;
   private boolean usedDebugKeyAsModifier;

   public KeyboardHandler(Minecraft p_90875_) {
      this.minecraft = p_90875_;
   }

   private boolean handleChunkDebugKeys(KeyEvent p_447161_) {
      switch (p_447161_.key()) {
         case 69:
            if (this.minecraft.player == null) {
               return false;
            }

            boolean flag = this.minecraft.debugEntries.toggleStatus(DebugScreenEntries.CHUNK_SECTION_PATHS);
            this.debugFeedback("SectionPath: " + (flag ? "shown" : "hidden"));
            return true;
         case 70:
            boolean flag2 = FogRenderer.toggleFog();
            this.debugFeedbackEnabledStatus("Fog: ", flag2);
            return true;
         case 71:
         case 72:
         case 73:
         case 74:
         case 75:
         case 77:
         case 78:
         case 80:
         case 81:
         case 82:
         case 83:
         case 84:
         default:
            return false;
         case 76:
            this.minecraft.smartCull = !this.minecraft.smartCull;
            this.debugFeedbackEnabledStatus("SmartCull: ", this.minecraft.smartCull);
            return true;
         case 79:
            if (this.minecraft.player == null) {
               return false;
            }

            boolean flag1 = this.minecraft.debugEntries.toggleStatus(DebugScreenEntries.CHUNK_SECTION_OCTREE);
            this.debugFeedbackEnabledStatus("Frustum culling Octree: ", flag1);
            return true;
         case 85:
            if (p_447161_.hasShiftDown()) {
               this.minecraft.levelRenderer.killFrustum();
               this.debugFeedback("Killed frustum");
            } else {
               this.minecraft.levelRenderer.captureFrustum();
               this.debugFeedback("Captured frustum");
            }

            return true;
         case 86:
            if (this.minecraft.player == null) {
               return false;
            }

            boolean flag3 = this.minecraft.debugEntries.toggleStatus(DebugScreenEntries.CHUNK_SECTION_VISIBILITY);
            this.debugFeedbackEnabledStatus("SectionVisibility: ", flag3);
            return true;
         case 87:
            this.minecraft.wireframe = !this.minecraft.wireframe;
            this.debugFeedbackEnabledStatus("WireFrame: ", this.minecraft.wireframe);
            return true;
      }
   }

   private void debugFeedbackEnabledStatus(String p_459193_, boolean p_458938_) {
      this.debugFeedback(p_459193_ + (p_458938_ ? "enabled" : "disabled"));
   }

   private void showDebugChat(Component p_415869_) {
      this.minecraft.gui.getChat().addMessage(p_415869_);
      this.minecraft.getNarrator().saySystemQueued(p_415869_);
   }

   private static Component decorateDebugComponent(ChatFormatting p_415900_, Component p_415904_) {
      return Component.empty()
         .append(Component.translatable("debug.prefix").withStyle(p_415900_, ChatFormatting.BOLD))
         .append(CommonComponents.SPACE)
         .append(p_415904_);
   }

   private void debugWarningComponent(Component p_415765_) {
      this.showDebugChat(decorateDebugComponent(ChatFormatting.RED, p_415765_));
   }

   private void debugFeedbackComponent(Component p_167823_) {
      this.showDebugChat(decorateDebugComponent(ChatFormatting.YELLOW, p_167823_));
   }

   private void debugFeedbackTranslated(String p_90914_, Object... p_455244_) {
      this.debugFeedbackComponent(Component.translatable(p_90914_, p_455244_));
   }

   private void debugFeedback(String p_459176_) {
      this.debugFeedbackComponent(Component.literal(p_459176_));
   }

   private boolean handleDebugKeys(KeyEvent p_446305_) {
      if (this.debugCrashKeyTime > 0L && this.debugCrashKeyTime < Util.getMillis() - 100L) {
         return true;
      } else if (SharedConstants.DEBUG_HOTKEYS && this.handleChunkDebugKeys(p_446305_)) {
         return true;
      } else {
         if (SharedConstants.DEBUG_FEATURE_COUNT) {
            switch (p_446305_.key()) {
               case 76:
                  FeatureCountTracker.logCounts();
                  return true;
               case 82:
                  FeatureCountTracker.clearCounts();
                  return true;
            }
         }

         Options options = this.minecraft.options;
         boolean flag = false;
         if (options.keyDebugReloadChunk.matches(p_446305_)) {
            this.minecraft.levelRenderer.allChanged();
            this.debugFeedbackTranslated("debug.reload_chunks.message");
            flag = true;
         }

         if (options.keyDebugShowHitboxes.matches(p_446305_) && this.minecraft.player != null && !this.minecraft.player.isReducedDebugInfo()) {
            boolean flag1 = this.minecraft.debugEntries.toggleStatus(DebugScreenEntries.ENTITY_HITBOXES);
            this.debugFeedbackTranslated(flag1 ? "debug.show_hitboxes.on" : "debug.show_hitboxes.off");
            flag = true;
         }

         if (options.keyDebugClearChat.matches(p_446305_)) {
            this.minecraft.gui.getChat().clearMessages(false);
            flag = true;
         }

         if (options.keyDebugShowChunkBorders.matches(p_446305_) && this.minecraft.player != null && !this.minecraft.player.isReducedDebugInfo()) {
            boolean flag2 = this.minecraft.debugEntries.toggleStatus(DebugScreenEntries.CHUNK_BORDERS);
            this.debugFeedbackTranslated(flag2 ? "debug.chunk_boundaries.on" : "debug.chunk_boundaries.off");
            flag = true;
         }

         if (options.keyDebugShowAdvancedTooltips.matches(p_446305_)) {
            options.advancedItemTooltips = !options.advancedItemTooltips;
            this.debugFeedbackTranslated(options.advancedItemTooltips ? "debug.advanced_tooltips.on" : "debug.advanced_tooltips.off");
            options.save();
            flag = true;
         }

         if (options.keyDebugCopyRecreateCommand.matches(p_446305_)) {
            if (this.minecraft.player != null && !this.minecraft.player.isReducedDebugInfo()) {
               this.copyRecreateCommand(this.minecraft.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER), !p_446305_.hasShiftDown());
            }

            flag = true;
         }

         if (options.keyDebugSpectate.matches(p_446305_)) {
            if (this.minecraft.player == null || !GameModeCommand.PERMISSION_CHECK.check(this.minecraft.player.permissions())) {
               this.debugFeedbackTranslated("debug.creative_spectator.error");
            } else if (!this.minecraft.player.isSpectator()) {
               this.minecraft.player.connection.send(new ServerboundChangeGameModePacket(GameType.SPECTATOR));
            } else {
               GameType gametype = (GameType)MoreObjects.firstNonNull(this.minecraft.gameMode.getPreviousPlayerMode(), GameType.CREATIVE);
               this.minecraft.player.connection.send(new ServerboundChangeGameModePacket(gametype));
            }

            flag = true;
         }

         if (options.keyDebugSwitchGameMode.matches(p_446305_) && this.minecraft.level != null && this.minecraft.screen == null) {
            if (this.minecraft.canSwitchGameMode() && GameModeCommand.PERMISSION_CHECK.check(this.minecraft.player.permissions())) {
               this.minecraft.setScreen(new GameModeSwitcherScreen());
            } else {
               this.debugFeedbackTranslated("debug.gamemodes.error");
            }

            flag = true;
         }

         if (options.keyDebugDebugOptions.matches(p_446305_)) {
            if (this.minecraft.screen instanceof DebugOptionsScreen) {
               this.minecraft.screen.onClose();
            } else if (this.minecraft.canInterruptScreen()) {
               if (this.minecraft.screen != null) {
                  this.minecraft.screen.onClose();
               }

               this.minecraft.setScreen(new DebugOptionsScreen());
            }

            flag = true;
         }

         if (options.keyDebugFocusPause.matches(p_446305_)) {
            options.pauseOnLostFocus = !options.pauseOnLostFocus;
            options.save();
            this.debugFeedbackTranslated(options.pauseOnLostFocus ? "debug.pause_focus.on" : "debug.pause_focus.off");
            flag = true;
         }

         if (options.keyDebugDumpDynamicTextures.matches(p_446305_)) {
            Path path1 = this.minecraft.gameDirectory.toPath().toAbsolutePath();
            Path path = TextureUtil.getDebugTexturePath(path1);
            this.minecraft.getTextureManager().dumpAllSheets(path);
            Component component = Component.literal(path1.relativize(path).toString())
               .withStyle(ChatFormatting.UNDERLINE)
               .withStyle(p_392486_ -> p_392486_.withClickEvent(new ClickEvent.OpenFile(path)));
            this.debugFeedbackComponent(Component.translatable("debug.dump_dynamic_textures", component));
            flag = true;
         }

         if (options.keyDebugReloadResourcePacks.matches(p_446305_)) {
            this.debugFeedbackTranslated("debug.reload_resourcepacks.message");
            this.minecraft.reloadResourcePacks();
            flag = true;
         }

         if (options.keyDebugProfiling.matches(p_446305_)) {
            if (this.minecraft.debugClientMetricsStart(this::debugFeedbackComponent)) {
               this.debugFeedbackComponent(
                  Component.translatable(
                     "debug.profiling.start", 10, options.keyDebugModifier.getTranslatedKeyMessage(), options.keyDebugProfiling.getTranslatedKeyMessage()
                  )
               );
            }

            flag = true;
         }

         if (options.keyDebugCopyLocation.matches(p_446305_) && this.minecraft.player != null && !this.minecraft.player.isReducedDebugInfo()) {
            this.debugFeedbackTranslated("debug.copy_location.message");
            this.setClipboard(
               String.format(
                  Locale.ROOT,
                  "/execute in %s run tp @s %.2f %.2f %.2f %.2f %.2f",
                  this.minecraft.player.level().dimension().identifier(),
                  this.minecraft.player.getX(),
                  this.minecraft.player.getY(),
                  this.minecraft.player.getZ(),
                  this.minecraft.player.getYRot(),
                  this.minecraft.player.getXRot()
               )
            );
            flag = true;
         }

         if (options.keyDebugDumpVersion.matches(p_446305_)) {
            this.debugFeedbackTranslated("debug.version.header");
            VersionCommand.dumpVersion(this::showDebugChat);
            flag = true;
         }

         if (options.keyDebugPofilingChart.matches(p_446305_)) {
            this.minecraft.getDebugOverlay().toggleProfilerChart();
            flag = true;
         }

         if (options.keyDebugFpsCharts.matches(p_446305_)) {
            this.minecraft.getDebugOverlay().toggleFpsCharts();
            flag = true;
         }

         if (options.keyDebugNetworkCharts.matches(p_446305_)) {
            this.minecraft.getDebugOverlay().toggleNetworkCharts();
            flag = true;
         }

         return flag;
      }
   }

   private void copyRecreateCommand(boolean p_90929_, boolean p_90930_) {
      HitResult hitresult = this.minecraft.hitResult;
      if (hitresult != null) {
         switch (hitresult.getType()) {
            case BLOCK:
               BlockPos blockpos = ((BlockHitResult)hitresult).getBlockPos();
               Level level = this.minecraft.player.level();
               BlockState blockstate = level.getBlockState(blockpos);
               if (p_90929_) {
                  if (p_90930_) {
                     this.minecraft.player.connection.getDebugQueryHandler().queryBlockEntityTag(blockpos, p_454127_ -> {
                        this.copyCreateBlockCommand(blockstate, blockpos, p_454127_);
                        this.debugFeedbackTranslated("debug.inspect.server.block");
                     });
                  } else {
                     BlockEntity blockentity = level.getBlockEntity(blockpos);
                     CompoundTag compoundtag = blockentity != null ? blockentity.saveWithoutMetadata(level.registryAccess()) : null;
                     this.copyCreateBlockCommand(blockstate, blockpos, compoundtag);
                     this.debugFeedbackTranslated("debug.inspect.client.block");
                  }
               } else {
                  this.copyCreateBlockCommand(blockstate, blockpos, null);
                  this.debugFeedbackTranslated("debug.inspect.client.block");
               }
               break;
            case ENTITY:
               Entity entity = ((EntityHitResult)hitresult).getEntity();
               Identifier identifier = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
               if (p_90929_) {
                  if (p_90930_) {
                     this.minecraft.player.connection.getDebugQueryHandler().queryEntityTag(entity.getId(), p_465327_ -> {
                        this.copyCreateEntityCommand(identifier, entity.position(), p_465327_);
                        this.debugFeedbackTranslated("debug.inspect.server.entity");
                     });
                  } else {
                     try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(entity.problemPath(), LOGGER)) {
                        TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, entity.registryAccess());
                        entity.saveWithoutId(tagvalueoutput);
                        this.copyCreateEntityCommand(identifier, entity.position(), tagvalueoutput.buildResult());
                     }

                     this.debugFeedbackTranslated("debug.inspect.client.entity");
                  }
               } else {
                  this.copyCreateEntityCommand(identifier, entity.position(), null);
                  this.debugFeedbackTranslated("debug.inspect.client.entity");
               }
         }
      }
   }

   private void copyCreateBlockCommand(BlockState p_90900_, BlockPos p_90901_, @Nullable CompoundTag p_90902_) {
      StringBuilder stringbuilder = new StringBuilder(BlockStateParser.serialize(p_90900_));
      if (p_90902_ != null) {
         stringbuilder.append(p_90902_);
      }

      String s = String.format(Locale.ROOT, "/setblock %d %d %d %s", p_90901_.getX(), p_90901_.getY(), p_90901_.getZ(), stringbuilder);
      this.setClipboard(s);
   }

   private void copyCreateEntityCommand(Identifier p_469383_, Vec3 p_90924_, @Nullable CompoundTag p_90925_) {
      String s;
      if (p_90925_ != null) {
         p_90925_.remove("UUID");
         p_90925_.remove("Pos");
         String s1 = NbtUtils.toPrettyComponent(p_90925_).getString();
         s = String.format(Locale.ROOT, "/summon %s %.2f %.2f %.2f %s", p_469383_, p_90924_.x, p_90924_.y, p_90924_.z, s1);
      } else {
         s = String.format(Locale.ROOT, "/summon %s %.2f %.2f %.2f", p_469383_, p_90924_.x, p_90924_.y, p_90924_.z);
      }

      this.setClipboard(s);
   }

   private void keyPress(long p_90894_, @KeyEvent.Action int p_90895_, KeyEvent p_446050_) {
      Window window = this.minecraft.getWindow();
      if (p_90894_ == window.handle()) {
         this.minecraft.getFramerateLimitTracker().onInputReceived();
         Options options = this.minecraft.options;
         boolean flag = options.keyDebugModifier.key.getValue() == options.keyDebugOverlay.key.getValue();
         boolean flag1 = options.keyDebugModifier.isDown();
         boolean flag2 = !options.keyDebugCrash.isUnbound() && InputConstants.isKeyDown(this.minecraft.getWindow(), options.keyDebugCrash.key.getValue());
         if (this.debugCrashKeyTime > 0L) {
            if (!flag2 || !flag1) {
               this.debugCrashKeyTime = -1L;
            }
         } else if (flag2 && flag1) {
            this.usedDebugKeyAsModifier = flag;
            this.debugCrashKeyTime = Util.getMillis();
            this.debugCrashKeyReportedTime = Util.getMillis();
            this.debugCrashKeyReportedCount = 0L;
         }

         Screen screen = this.minecraft.screen;
         if (screen != null) {
            switch (p_446050_.key()) {
               case 258:
                  this.minecraft.setLastInputType(InputType.KEYBOARD_TAB);
               case 259:
               case 260:
               case 261:
               default:
                  break;
               case 262:
               case 263:
               case 264:
               case 265:
                  this.minecraft.setLastInputType(InputType.KEYBOARD_ARROW);
            }
         }

         if (p_90895_ == 1 && (!(this.minecraft.screen instanceof KeyBindsScreen) || ((KeyBindsScreen)screen).lastKeySelection <= Util.getMillis() - 20L)) {
            if (options.keyFullscreen.matches(p_446050_)) {
               window.toggleFullScreen();
               boolean flag5 = window.isFullscreen();
               options.fullscreen().set(flag5);
               options.save();
               if (this.minecraft.screen instanceof VideoSettingsScreen videosettingsscreen) {
                  videosettingsscreen.updateFullscreenButton(flag5);
               }

               return;
            }

            if (options.keyScreenshot.matches(p_446050_)) {
               if (p_446050_.hasControlDownWithQuirk() && SharedConstants.DEBUG_PANORAMA_SCREENSHOT) {
                  this.showDebugChat(this.minecraft.grabPanoramixScreenshot(this.minecraft.gameDirectory));
               } else {
                  Screenshot.grab(
                     this.minecraft.gameDirectory, this.minecraft.getMainRenderTarget(), p_90917_ -> this.minecraft.execute(() -> this.showDebugChat(p_90917_))
                  );
               }

               return;
            }
         }

         if (p_90895_ != 0) {
            boolean flag3 = screen == null || !(screen.getFocused() instanceof EditBox) || !((EditBox)screen.getFocused()).canConsumeInput();
            if (flag3) {
               if (p_446050_.hasControlDownWithQuirk() && p_446050_.key() == 66 && this.minecraft.getNarrator().isActive() && options.narratorHotkey().get()) {
                  boolean flag4 = options.narrator().get() == NarratorStatus.OFF;
                  options.narrator().set(NarratorStatus.byId(options.narrator().get().getId() + 1));
                  options.save();
                  if (screen != null) {
                     screen.updateNarratorStatus(flag4);
                  }
               }

               LocalPlayer var21 = this.minecraft.player;
            }
         }

         if (screen != null) {
            try {
               if (p_90895_ != 1 && p_90895_ != 2) {
                  if (p_90895_ == 0
                     && (
                        ClientHooks.onScreenKeyReleasedPre(screen, p_446050_)
                           || screen.keyReleased(p_446050_)
                           || ClientHooks.onScreenKeyReleasedPost(screen, p_446050_)
                     )) {
                     if (options.keyDebugModifier.matches(p_446050_)) {
                        this.usedDebugKeyAsModifier = false;
                     }

                     return;
                  }
               } else {
                  screen.afterKeyboardAction();
                  if (ClientHooks.onScreenKeyPressedPre(screen, p_446050_)
                     || screen.keyPressed(p_446050_)
                     || ClientHooks.onScreenKeyPressedPost(screen, p_446050_)) {
                     if (this.minecraft.screen == null) {
                        InputConstants.Key inputconstants$key = InputConstants.getKey(p_446050_);
                        KeyMapping.set(inputconstants$key, false);
                     }

                     return;
                  }
               }
            } catch (Throwable var17) {
               CrashReport crashreport = CrashReport.forThrowable(var17, "keyPressed event handler");
               screen.fillCrashDetails(crashreport);
               CrashReportCategory crashreportcategory = crashreport.addCategory("Key");
               crashreportcategory.setDetail("Key", p_446050_.key());
               crashreportcategory.setDetail("Scancode", p_446050_.scancode());
               crashreportcategory.setDetail("Mods", p_446050_.modifiers());
               throw new ReportedException(crashreport);
            }
         }

         InputConstants.Key inputconstants$key1 = InputConstants.getKey(p_446050_);
         boolean flag6 = this.minecraft.screen == null;
         boolean flag7 = flag6
            || this.minecraft.screen instanceof PauseScreen pausescreen && !pausescreen.showsPauseMenu()
            || this.minecraft.screen instanceof GameModeSwitcherScreen;
         if (flag && options.keyDebugModifier.matches(p_446050_) && p_90895_ == 0) {
            if (this.usedDebugKeyAsModifier) {
               this.usedDebugKeyAsModifier = false;
            } else {
               this.minecraft.debugEntries.toggleDebugOverlay();
            }
         } else if (!flag && options.keyDebugOverlay.matches(p_446050_) && p_90895_ == 1) {
            this.minecraft.debugEntries.toggleDebugOverlay();
         }

         if (p_90895_ == 0) {
            KeyMapping.set(inputconstants$key1, false);
         } else {
            boolean flag8 = false;
            if (flag7 && p_446050_.isEscape()) {
               this.minecraft.pauseGame(flag1);
               flag8 = flag1;
            } else if (flag1) {
               flag8 = this.handleDebugKeys(p_446050_);
               if (flag8 && screen instanceof DebugOptionsScreen debugoptionsscreen) {
                  DebugOptionsScreen.OptionList debugoptionsscreen$optionlist = debugoptionsscreen.getOptionList();
                  if (debugoptionsscreen$optionlist != null) {
                     debugoptionsscreen$optionlist.children().forEach(DebugOptionsScreen.AbstractOptionEntry::refreshEntry);
                  }
               }
            } else if (flag7 && options.keyToggleGui.matches(p_446050_)) {
               options.hideGui = !options.hideGui;
            } else if (flag7 && options.keyToggleSpectatorShaderEffects.matches(p_446050_)) {
               this.minecraft.gameRenderer.togglePostEffect();
            }

            if (flag) {
               this.usedDebugKeyAsModifier |= flag8;
            }

            if (this.minecraft.getDebugOverlay().showProfilerChart() && !flag1) {
               int i = p_446050_.getDigit();
               if (i != -1) {
                  this.minecraft.getDebugOverlay().getProfilerPieChart().profilerPieChartKeyPress(i);
               }
            }

            if (flag6 || inputconstants$key1 == options.keyDebugModifier.key) {
               if (flag8) {
                  KeyMapping.set(inputconstants$key1, false);
               } else {
                  KeyMapping.set(inputconstants$key1, true);
                  KeyMapping.click(inputconstants$key1);
               }
            }
         }

         ClientHooks.onKeyInput(p_446050_, p_90895_);
      }
   }

   private void charTyped(long p_90890_, CharacterEvent p_445415_) {
      if (p_90890_ == this.minecraft.getWindow().handle()) {
         Screen screen = this.minecraft.screen;
         if (screen != null && this.minecraft.getOverlay() == null) {
            try {
               if (ClientHooks.onScreenCharTypedPre(screen, p_445415_)) {
                  return;
               }

               if (screen.charTyped(p_445415_)) {
                  return;
               }

               ClientHooks.onScreenCharTypedPost(screen, p_445415_);
            } catch (Throwable var8) {
               CrashReport crashreport = CrashReport.forThrowable(var8, "charTyped event handler");
               screen.fillCrashDetails(crashreport);
               CrashReportCategory crashreportcategory = crashreport.addCategory("Key");
               crashreportcategory.setDetail("Codepoint", p_445415_.codepoint());
               crashreportcategory.setDetail("Mods", p_445415_.modifiers());
               throw new ReportedException(crashreport);
            }
         }
      }
   }

   public void setup(Window p_447031_) {
      InputConstants.setupKeyboardCallbacks(p_447031_, (p_445137_, p_445138_, p_445139_, p_445140_, p_445141_) -> {
         KeyEvent keyevent = new KeyEvent(p_445138_, p_445139_, p_445141_);
         this.minecraft.execute(() -> this.keyPress(p_445137_, p_445140_, keyevent));
      }, (p_445132_, p_445133_, p_445134_) -> {
         CharacterEvent characterevent = new CharacterEvent(p_445133_, p_445134_);
         this.minecraft.execute(() -> this.charTyped(p_445132_, characterevent));
      });
   }

   public String getClipboard() {
      return this.clipboardManager.getClipboard(this.minecraft.getWindow(), (p_90878_, p_90879_) -> {
         if (p_90878_ != 65545) {
            this.minecraft.getWindow().defaultErrorCallback(p_90878_, p_90879_);
         }
      });
   }

   public void setClipboard(String p_90912_) {
      if (!p_90912_.isEmpty()) {
         this.clipboardManager.setClipboard(this.minecraft.getWindow(), p_90912_);
      }
   }

   public void tick() {
      if (this.debugCrashKeyTime > 0L) {
         long i = Util.getMillis();
         long j = 10000L - (i - this.debugCrashKeyTime);
         long k = i - this.debugCrashKeyReportedTime;
         if (j < 0L) {
            if (this.minecraft.hasControlDown()) {
               Blaze3D.youJustLostTheGame();
            }

            String s = "Manually triggered debug crash";
            CrashReport crashreport = new CrashReport("Manually triggered debug crash", new Throwable("Manually triggered debug crash"));
            CrashReportCategory crashreportcategory = crashreport.addCategory("Manual crash details");
            NativeModuleLister.addCrashSection(crashreportcategory);
            throw new ReportedException(crashreport);
         }

         if (k >= 1000L) {
            if (this.debugCrashKeyReportedCount == 0L) {
               this.debugFeedbackTranslated(
                  "debug.crash.message",
                  this.minecraft.options.keyDebugModifier.getTranslatedKeyMessage().getString(),
                  this.minecraft.options.keyDebugCrash.getTranslatedKeyMessage().getString()
               );
            } else {
               this.debugWarningComponent(Component.translatable("debug.crash.warning", Mth.ceil((float)j / 1000.0F)));
            }

            this.debugCrashKeyReportedTime = i;
            this.debugCrashKeyReportedCount++;
         }
      }
   }
}
