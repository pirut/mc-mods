package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.realmsclient.dto.RealmsRegion;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RegionSelectionPreference;
import com.mojang.realmsclient.dto.RegionSelectionPreferenceDto;
import com.mojang.realmsclient.dto.ServiceQuality;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RealmsSettingsTab extends GridLayoutTab implements RealmsConfigurationTab {
   private static final int COMPONENT_WIDTH = 212;
   private static final int EXTRA_SPACING = 2;
   private static final int DEFAULT_SPACING = 6;
   static final Component TITLE = Component.translatable("mco.configure.world.settings.title");
   private static final Component NAME_LABEL = Component.translatable("mco.configure.world.name");
   private static final Component DESCRIPTION_LABEL = Component.translatable("mco.configure.world.description");
   private static final Component REGION_PREFERENCE_LABEL = Component.translatable("mco.configure.world.region_preference");
   private static final Tooltip REALM_NAME_VALIDATION_ERROR_TOOLTIP = Tooltip.create(Component.translatable("mco.configure.world.name.validation.whitespace"));
   private final RealmsConfigureWorldScreen configurationScreen;
   private final Minecraft minecraft;
   private RealmsServer serverData;
   private final Map<RealmsRegion, ServiceQuality> regionServiceQuality;
   final Button closeOpenButton;
   private final EditBox descEdit;
   private final EditBox nameEdit;
   private final StringWidget selectedRegionStringWidget;
   private final ImageWidget selectedRegionImageWidget;
   private RealmsSettingsTab.RegionSelection preferredRegionSelection;

   RealmsSettingsTab(RealmsConfigureWorldScreen p_420066_, Minecraft p_419867_, RealmsServer p_419921_, Map<RealmsRegion, ServiceQuality> p_419755_) {
      super(TITLE);
      this.configurationScreen = p_420066_;
      this.minecraft = p_419867_;
      this.serverData = p_419921_;
      this.regionServiceQuality = p_419755_;
      GridLayout.RowHelper gridlayout$rowhelper = this.layout.rowSpacing(6).createRowHelper(1);
      gridlayout$rowhelper.addChild(new StringWidget(NAME_LABEL, p_420066_.getFont()));
      this.nameEdit = new EditBox(p_419867_.font, 0, 0, 212, 20, Component.translatable("mco.configure.world.name"));
      this.nameEdit.setMaxLength(32);
      this.nameEdit.setResponder(p_454112_ -> {
         if (!this.isRealmNameValid()) {
            this.nameEdit.setTextColor(-2142128);
            this.nameEdit.setTooltip(REALM_NAME_VALIDATION_ERROR_TOOLTIP);
         } else {
            this.nameEdit.setTooltip(null);
            this.nameEdit.setTextColor(-2039584);
         }
      });
      gridlayout$rowhelper.addChild(this.nameEdit);
      gridlayout$rowhelper.addChild(SpacerElement.height(2));
      gridlayout$rowhelper.addChild(new StringWidget(DESCRIPTION_LABEL, p_420066_.getFont()));
      this.descEdit = new EditBox(p_419867_.font, 0, 0, 212, 20, Component.translatable("mco.configure.world.description"));
      this.descEdit.setMaxLength(32);
      gridlayout$rowhelper.addChild(this.descEdit);
      gridlayout$rowhelper.addChild(SpacerElement.height(2));
      gridlayout$rowhelper.addChild(new StringWidget(REGION_PREFERENCE_LABEL, p_420066_.getFont()));
      EqualSpacingLayout equalspacinglayout = new EqualSpacingLayout(0, 0, 212, 9, EqualSpacingLayout.Orientation.HORIZONTAL);
      this.selectedRegionStringWidget = equalspacinglayout.addChild(new StringWidget(192, 9, Component.empty(), p_420066_.getFont()));
      this.selectedRegionImageWidget = equalspacinglayout.addChild(ImageWidget.sprite(10, 8, ServiceQuality.UNKNOWN.getIcon()));
      gridlayout$rowhelper.addChild(equalspacinglayout);
      gridlayout$rowhelper.addChild(
         Button.builder(Component.translatable("mco.configure.world.buttons.region_preference"), p_419851_ -> this.openPreferenceSelector())
            .bounds(0, 0, 212, 20)
            .build()
      );
      gridlayout$rowhelper.addChild(SpacerElement.height(2));
      this.closeOpenButton = gridlayout$rowhelper.addChild(
         Button.builder(
               Component.empty(),
               p_428746_ -> {
                  if (p_419921_.state == RealmsServer.State.OPEN) {
                     p_419867_.setScreen(
                        RealmsPopups.customPopupScreen(
                           p_420066_,
                           Component.translatable("mco.configure.world.close.question.title"),
                           Component.translatable("mco.configure.world.close.question.line1"),
                           p_428748_ -> {
                              this.save();
                              p_420066_.closeTheWorld();
                           }
                        )
                     );
                  } else {
                     this.save();
                     p_420066_.openTheWorld(false);
                  }
               }
            )
            .bounds(0, 0, 212, 20)
            .build()
      );
      this.closeOpenButton.active = false;
      this.updateData(p_419921_);
   }

   private static MutableComponent getTranslatableFromPreference(RealmsSettingsTab.RegionSelection p_419485_) {
      return (p_419485_.preference().equals(RegionSelectionPreference.MANUAL) && p_419485_.region() != null
            ? Component.translatable(p_419485_.region().translationKey)
            : Component.translatable(p_419485_.preference().translationKey))
         .withStyle(ChatFormatting.GRAY);
   }

   private static Identifier getServiceQualityIcon(RealmsSettingsTab.RegionSelection p_419469_, Map<RealmsRegion, ServiceQuality> p_419588_) {
      if (p_419469_.region() != null && p_419588_.containsKey(p_419469_.region())) {
         ServiceQuality servicequality = p_419588_.getOrDefault(p_419469_.region(), ServiceQuality.UNKNOWN);
         return servicequality.getIcon();
      } else {
         return ServiceQuality.UNKNOWN.getIcon();
      }
   }

   private boolean isRealmNameValid() {
      String s = this.nameEdit.getValue();
      String s1 = s.trim();
      return !s1.isEmpty() && s.length() == s1.length();
   }

   private void openPreferenceSelector() {
      this.minecraft
         .setScreen(
            new RealmsPreferredRegionSelectionScreen(
               this.configurationScreen, this::applyRegionPreferenceSelection, this.regionServiceQuality, this.preferredRegionSelection
            )
         );
   }

   private void applyRegionPreferenceSelection(RegionSelectionPreference p_426076_, RealmsRegion p_419988_) {
      this.preferredRegionSelection = new RealmsSettingsTab.RegionSelection(p_426076_, p_419988_);
      this.updateRegionPreferenceValues();
   }

   private void updateRegionPreferenceValues() {
      this.selectedRegionStringWidget.setMessage(getTranslatableFromPreference(this.preferredRegionSelection));
      this.selectedRegionImageWidget.updateResource(getServiceQualityIcon(this.preferredRegionSelection, this.regionServiceQuality));
      this.selectedRegionImageWidget.visible = this.preferredRegionSelection.preference == RegionSelectionPreference.MANUAL;
   }

   @Override
   public void onSelected(RealmsServer p_419845_) {
      this.updateData(p_419845_);
   }

   @Override
   public void updateData(RealmsServer p_419803_) {
      this.serverData = p_419803_;
      if (p_419803_.regionSelectionPreference == null) {
         p_419803_.regionSelectionPreference = RegionSelectionPreferenceDto.DEFAULT;
      }

      if (p_419803_.regionSelectionPreference.regionSelectionPreference == RegionSelectionPreference.MANUAL
         && p_419803_.regionSelectionPreference.preferredRegion == null) {
         Optional<RealmsRegion> optional = this.regionServiceQuality.keySet().stream().findFirst();
         optional.ifPresent(p_419577_ -> p_419803_.regionSelectionPreference.preferredRegion = p_419577_);
      }

      String s = p_419803_.state == RealmsServer.State.OPEN ? "mco.configure.world.buttons.close" : "mco.configure.world.buttons.open";
      this.closeOpenButton.setMessage(Component.translatable(s));
      this.closeOpenButton.active = true;
      this.preferredRegionSelection = new RealmsSettingsTab.RegionSelection(
         p_419803_.regionSelectionPreference.regionSelectionPreference, p_419803_.regionSelectionPreference.preferredRegion
      );
      this.nameEdit.setValue(Objects.requireNonNullElse(p_419803_.getName(), ""));
      this.descEdit.setValue(p_419803_.getDescription());
      this.updateRegionPreferenceValues();
   }

   @Override
   public void onDeselected(RealmsServer p_419747_) {
      this.save();
   }

   public void save() {
      String s = this.nameEdit.getValue().trim();
      if (this.serverData.regionSelectionPreference == null
         || !Objects.equals(s, this.serverData.name)
         || !Objects.equals(this.descEdit.getValue(), this.serverData.motd)
         || this.preferredRegionSelection.preference() != this.serverData.regionSelectionPreference.regionSelectionPreference
         || this.preferredRegionSelection.region() != this.serverData.regionSelectionPreference.preferredRegion) {
         this.configurationScreen.saveSettings(s, this.descEdit.getValue(), this.preferredRegionSelection.preference(), this.preferredRegionSelection.region());
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record RegionSelection(RegionSelectionPreference preference, @Nullable RealmsRegion region) {
   }
}
