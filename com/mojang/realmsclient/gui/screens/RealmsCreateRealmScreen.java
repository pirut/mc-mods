package com.mojang.realmsclient.gui.screens;

import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.task.RealmCreationTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsCreateRealmScreen extends RealmsScreen {
   private static final Component CREATE_REALM_TEXT = Component.translatable("mco.selectServer.create");
   private static final Component NAME_LABEL = Component.translatable("mco.configure.world.name");
   private static final Component DESCRIPTION_LABEL = Component.translatable("mco.configure.world.description");
   private static final int BUTTON_SPACING = 10;
   private static final int CONTENT_WIDTH = 210;
   private final RealmsMainScreen lastScreen;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private EditBox nameBox;
   private EditBox descriptionBox;
   private final Runnable createWorldRunnable;

   public RealmsCreateRealmScreen(RealmsMainScreen p_306218_, RealmsServer p_374117_, boolean p_374029_) {
      super(CREATE_REALM_TEXT);
      this.lastScreen = p_306218_;
      this.createWorldRunnable = () -> this.createWorld(p_374117_, p_374029_);
   }

   @Override
   public void init() {
      this.layout.addTitleHeader(this.title, this.font);
      LinearLayout linearlayout = this.layout.addToContents(LinearLayout.vertical()).spacing(10);
      Button button = Button.builder(CommonComponents.GUI_CONTINUE, p_305625_ -> this.createWorldRunnable.run()).build();
      button.active = false;
      this.nameBox = new EditBox(this.font, 210, 20, NAME_LABEL);
      this.nameBox.setResponder(p_329650_ -> button.active = !StringUtil.isBlank(p_329650_));
      this.descriptionBox = new EditBox(this.font, 210, 20, DESCRIPTION_LABEL);
      linearlayout.addChild(CommonLayouts.labeledElement(this.font, this.nameBox, NAME_LABEL));
      linearlayout.addChild(CommonLayouts.labeledElement(this.font, this.descriptionBox, DESCRIPTION_LABEL));
      LinearLayout linearlayout1 = this.layout.addToFooter(LinearLayout.horizontal().spacing(10));
      linearlayout1.addChild(button);
      linearlayout1.addChild(Button.builder(CommonComponents.GUI_BACK, p_293570_ -> this.onClose()).build());
      this.layout.visitWidgets(p_321338_ -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_321338_);
      });
      this.repositionElements();
   }

   @Override
   protected void setInitialFocus() {
      this.setInitialFocus(this.nameBox);
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
   }

   private void createWorld(RealmsServer p_374088_, boolean p_374367_) {
      if (!p_374088_.isSnapshotRealm() && p_374367_) {
         AtomicBoolean atomicboolean = new AtomicBoolean();
         this.minecraft.setScreen(new AlertScreen(() -> {
            atomicboolean.set(true);
            this.lastScreen.resetScreen();
            this.minecraft.setScreen(this.lastScreen);
         }, Component.translatable("mco.upload.preparing"), Component.empty()));
         CompletableFuture.<RealmsServer>supplyAsync(() -> createSnapshotRealm(p_374088_), Util.backgroundExecutor()).thenAcceptAsync(p_373647_ -> {
            if (!atomicboolean.get()) {
               this.showResetWorldScreen(p_373647_);
            }
         }, this.minecraft).exceptionallyAsync(p_373649_ -> {
            this.lastScreen.resetScreen();
            Component component;
            if (p_373649_.getCause() instanceof RealmsServiceException realmsserviceexception) {
               component = realmsserviceexception.realmsError.errorMessage();
            } else {
               component = Component.translatable("mco.errorMessage.initialize.failed");
            }

            this.minecraft.setScreen(new RealmsGenericErrorScreen(component, this.lastScreen));
            return null;
         }, this.minecraft);
      } else {
         this.showResetWorldScreen(p_374088_);
      }
   }

   private static RealmsServer createSnapshotRealm(RealmsServer p_374259_) {
      RealmsClient realmsclient = RealmsClient.getOrCreate();

      try {
         return realmsclient.createSnapshotRealm(p_374259_.id);
      } catch (RealmsServiceException var3) {
         throw new RuntimeException(var3);
      }
   }

   private void showResetWorldScreen(RealmsServer p_305781_) {
      RealmCreationTask realmcreationtask = new RealmCreationTask(p_305781_.id, this.nameBox.getValue(), this.descriptionBox.getValue());
      RealmsResetWorldScreen realmsresetworldscreen = RealmsResetWorldScreen.forNewRealm(
         this, p_305781_, realmcreationtask, () -> this.minecraft.execute(() -> {
            RealmsMainScreen.refreshServerList();
            this.minecraft.setScreen(this.lastScreen);
         })
      );
      this.minecraft.setScreen(realmsresetworldscreen);
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }
}
