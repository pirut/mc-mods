package net.minecraft.client.gui.screens.options;

import java.util.Arrays;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonLinks;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AccessibilityOptionsScreen extends OptionsSubScreen {
   public static final Component TITLE = Component.translatable("options.accessibility.title");

   private static OptionInstance<?>[] options(Options p_345629_) {
      return new OptionInstance[]{
         p_345629_.narrator(),
         p_345629_.showSubtitles(),
         p_345629_.highContrast(),
         p_345629_.menuBackgroundBlurriness(),
         p_345629_.textBackgroundOpacity(),
         p_345629_.backgroundForChatOnly(),
         p_345629_.chatOpacity(),
         p_345629_.chatLineSpacing(),
         p_345629_.chatDelay(),
         p_345629_.notificationDisplayTime(),
         p_345629_.bobView(),
         p_345629_.screenEffectScale(),
         p_345629_.fovEffectScale(),
         p_345629_.darknessEffectScale(),
         p_345629_.damageTiltStrength(),
         p_345629_.glintSpeed(),
         p_345629_.glintStrength(),
         p_345629_.hideLightningFlash(),
         p_345629_.darkMojangStudiosBackground(),
         p_345629_.panoramaSpeed(),
         p_345629_.hideSplashTexts(),
         p_345629_.narratorHotkey(),
         p_345629_.rotateWithMinecart(),
         p_345629_.highContrastBlockOutline()
      };
   }

   public AccessibilityOptionsScreen(Screen p_344941_, Options p_344986_) {
      super(p_344941_, p_344986_, TITLE);
   }

   @Override
   protected void init() {
      super.init();
      AbstractWidget abstractwidget = this.list.findOption(this.options.highContrast());
      if (abstractwidget != null && !this.minecraft.getResourcePackRepository().getAvailableIds().contains("high_contrast")) {
         abstractwidget.active = false;
         abstractwidget.setTooltip(Tooltip.create(Component.translatable("options.accessibility.high_contrast.error.tooltip")));
      }

      AbstractWidget abstractwidget1 = this.list.findOption(this.options.rotateWithMinecart());
      if (abstractwidget1 != null) {
         abstractwidget1.active = this.isMinecartOptionEnabled();
      }
   }

   @Override
   protected void addOptions() {
      OptionInstance<?>[] optioninstance = options(this.options);
      Button button = Button.builder(OptionsScreen.CONTROLS, p_432226_ -> this.minecraft.setScreen(new ControlsScreen(this, this.options))).build();
      OptionInstance<?> optioninstance1 = optioninstance[0];
      this.list.addSmall(optioninstance1.createButton(this.options), this.options.narrator(), button);
      this.list.addSmall(Arrays.stream(optioninstance).filter(p_432228_ -> p_432228_ != optioninstance1).toArray(OptionInstance[]::new));
   }

   @Override
   protected void addFooter() {
      LinearLayout linearlayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      linearlayout.addChild(
         Button.builder(Component.translatable("options.accessibility.link"), ConfirmLinkScreen.confirmLink(this, CommonLinks.ACCESSIBILITY_HELP)).build()
      );
      linearlayout.addChild(Button.builder(CommonComponents.GUI_DONE, p_345508_ -> this.minecraft.setScreen(this.lastScreen)).build());
   }

   @Override
   protected boolean panoramaShouldSpin() {
      return !(this.lastScreen instanceof AccessibilityOnboardingScreen);
   }

   private boolean isMinecartOptionEnabled() {
      return this.minecraft.level != null && this.minecraft.level.enabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS);
   }
}
