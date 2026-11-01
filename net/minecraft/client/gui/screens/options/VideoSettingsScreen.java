package net.minecraft.client.gui.screens.options;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VideoSettingsScreen extends OptionsSubScreen {
   private static final Component TITLE = Component.translatable("options.videoTitle");
   private static final Component FABULOUS = Component.translatable("options.graphics.fabulous").withStyle(ChatFormatting.ITALIC);
   private static final Component WARNING_MESSAGE = Component.translatable("options.graphics.warning.message", FABULOUS, FABULOUS);
   private static final Component WARNING_TITLE = Component.translatable("options.graphics.warning.title").withStyle(ChatFormatting.RED);
   private static final Component BUTTON_ACCEPT = Component.translatable("options.graphics.warning.accept");
   private static final Component BUTTON_CANCEL = Component.translatable("options.graphics.warning.cancel");
   private final GpuWarnlistManager gpuWarnlistManager;
   private final int oldMipmaps;

   private static OptionInstance<?>[] options(Options p_345645_) {
      return new OptionInstance[]{
         p_345645_.graphicsMode(),
         p_345645_.renderDistance(),
         p_345645_.prioritizeChunkUpdates(),
         p_345645_.simulationDistance(),
         p_345645_.ambientOcclusion(),
         p_345645_.framerateLimit(),
         p_345645_.enableVsync(),
         p_345645_.bobView(),
         p_345645_.guiScale(),
         p_345645_.attackIndicator(),
         p_345645_.gamma(),
         p_345645_.cloudStatus(),
         p_345645_.fullscreen(),
         p_345645_.particles(),
         p_345645_.mipmapLevels(),
         p_345645_.entityShadows(),
         p_345645_.screenEffectScale(),
         p_345645_.entityDistanceScaling(),
         p_345645_.fovEffectScale(),
         p_345645_.showAutosaveIndicator(),
         p_345645_.glintSpeed(),
         p_345645_.glintStrength(),
         p_345645_.menuBackgroundBlurriness()
      };
   }

   public VideoSettingsScreen(Screen p_345053_, Minecraft p_346149_, Options p_346198_) {
      super(p_345053_, p_346198_, TITLE);
      this.gpuWarnlistManager = p_346149_.getGpuWarnlistManager();
      this.gpuWarnlistManager.resetWarnings();
      if (p_346198_.graphicsMode().get() == GraphicsStatus.FABULOUS) {
         this.gpuWarnlistManager.dismissWarning();
      }

      this.oldMipmaps = p_346198_.mipmapLevels().get();
   }

   @Override
   protected void addOptions() {
      int $$0 = -1;
      Window $$1 = this.minecraft.getWindow();
      Monitor $$2 = $$1.findBestMonitor();
      int $$3;
      if ($$2 == null) {
         $$3 = -1;
      } else {
         Optional<VideoMode> $$4 = $$1.getPreferredFullscreenVideoMode();
         $$3 = $$4.map($$2::getVideoModeIndex).orElse(-1);
      }

      OptionInstance<Integer> $$6 = new OptionInstance<>(
         "options.fullscreen.resolution",
         OptionInstance.noTooltip(),
         (p_346436_, p_344747_) -> {
            if ($$2 == null) {
               return Component.translatable("options.fullscreen.unavailable");
            } else if (p_344747_ == -1) {
               return Options.genericValueLabel(p_346436_, Component.translatable("options.fullscreen.current"));
            } else {
               VideoMode $$3x = $$2.getMode(p_344747_);
               return Options.genericValueLabel(
                  p_346436_,
                  Component.translatable(
                     "options.fullscreen.entry",
                     $$3x.getWidth(),
                     $$3x.getHeight(),
                     $$3x.getRefreshRate(),
                     $$3x.getRedBits() + $$3x.getGreenBits() + $$3x.getBlueBits()
                  )
               );
            }
         },
         new OptionInstance.IntRange(-1, $$2 != null ? $$2.getModeCount() - 1 : -1),
         $$3,
         p_345666_ -> {
            if ($$2 != null) {
               $$1.setPreferredFullscreenVideoMode(p_345666_ == -1 ? Optional.empty() : Optional.of($$2.getMode(p_345666_)));
            }
         }
      );
      this.list.addBig($$6);
      this.list.addBig(this.options.biomeBlendRadius());
      this.list.addSmall(options(this.options));
   }

   @Override
   public void onClose() {
      this.minecraft.getWindow().changeFullscreenVideoMode();
      super.onClose();
   }

   @Override
   public void removed() {
      if (this.options.mipmapLevels().get() != this.oldMipmaps) {
         this.minecraft.updateMaxMipLevel(this.options.mipmapLevels().get());
         this.minecraft.delayTextureReload();
      }

      super.removed();
   }

   @Override
   public boolean mouseClicked(double p_345069_, double p_346434_, int p_344934_) {
      if (super.mouseClicked(p_345069_, p_346434_, p_344934_)) {
         if (this.gpuWarnlistManager.isShowingWarning()) {
            List<Component> $$3 = Lists.newArrayList(new Component[]{WARNING_MESSAGE, CommonComponents.NEW_LINE});
            String $$4 = this.gpuWarnlistManager.getRendererWarnings();
            if ($$4 != null) {
               $$3.add(CommonComponents.NEW_LINE);
               $$3.add(Component.translatable("options.graphics.warning.renderer", $$4).withStyle(ChatFormatting.GRAY));
            }

            String $$5 = this.gpuWarnlistManager.getVendorWarnings();
            if ($$5 != null) {
               $$3.add(CommonComponents.NEW_LINE);
               $$3.add(Component.translatable("options.graphics.warning.vendor", $$5).withStyle(ChatFormatting.GRAY));
            }

            String $$6 = this.gpuWarnlistManager.getVersionWarnings();
            if ($$6 != null) {
               $$3.add(CommonComponents.NEW_LINE);
               $$3.add(Component.translatable("options.graphics.warning.version", $$6).withStyle(ChatFormatting.GRAY));
            }

            this.minecraft
               .setScreen(
                  new UnsupportedGraphicsWarningScreen(
                     WARNING_TITLE, $$3, ImmutableList.of(new UnsupportedGraphicsWarningScreen.ButtonOption(BUTTON_ACCEPT, p_346113_ -> {
                        this.options.graphicsMode().set(GraphicsStatus.FABULOUS);
                        Minecraft.getInstance().levelRenderer.allChanged();
                        this.gpuWarnlistManager.dismissWarning();
                        this.minecraft.setScreen(this);
                     }), new UnsupportedGraphicsWarningScreen.ButtonOption(BUTTON_CANCEL, p_345929_ -> {
                        this.gpuWarnlistManager.dismissWarningAndSkipFabulous();
                        this.minecraft.setScreen(this);
                     }))
                  )
               );
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseScrolled(double p_344913_, double p_346159_, double p_345166_, double p_345130_) {
      if (Screen.hasControlDown()) {
         OptionInstance<Integer> $$4 = this.options.guiScale();
         if ($$4.values() instanceof OptionInstance.ClampingLazyMaxIntRange $$5) {
            int $$6 = $$4.get();
            int $$7 = $$6 == 0 ? $$5.maxInclusive() + 1 : $$6;
            int $$8 = $$7 + (int)Math.signum(p_345130_);
            if ($$8 != 0 && $$8 <= $$5.maxInclusive() && $$8 >= $$5.minInclusive()) {
               CycleButton<Integer> $$9 = (CycleButton<Integer>)this.list.findOption($$4);
               if ($$9 != null) {
                  $$4.set($$8);
                  $$9.setValue($$8);
                  this.list.setScrollAmount(0.0);
                  return true;
               }
            }
         }

         return false;
      } else {
         return super.mouseScrolled(p_344913_, p_346159_, p_345166_, p_345130_);
      }
   }
}
