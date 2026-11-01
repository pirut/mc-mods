package com.mojang.realmsclient.gui.screens;

import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.util.task.CreateSnapshotRealmTask;
import com.mojang.realmsclient.util.task.RealmCreationTask;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.StringUtil;
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

   public RealmsCreateRealmScreen(RealmsMainScreen p_88575_, RealmsServer p_88574_) {
      super(CREATE_REALM_TEXT);
      this.lastScreen = p_88575_;
      this.createWorldRunnable = () -> this.createWorld(p_88574_);
   }

   public RealmsCreateRealmScreen(RealmsMainScreen p_306218_, long p_305942_) {
      super(CREATE_REALM_TEXT);
      this.lastScreen = p_306218_;
      this.createWorldRunnable = () -> this.createSnapshotWorld(p_305942_);
   }

   @Override
   public void init() {
      this.layout.addTitleHeader(this.title, this.font);
      LinearLayout $$0 = this.layout.addToContents(LinearLayout.vertical()).spacing(10);
      Button $$1 = Button.builder(CommonComponents.GUI_CONTINUE, p_305625_ -> this.createWorldRunnable.run()).build();
      $$1.active = false;
      this.nameBox = new EditBox(this.font, 210, 20, NAME_LABEL);
      this.nameBox.setResponder(p_329650_ -> $$1.active = !StringUtil.isBlank(p_329650_));
      this.descriptionBox = new EditBox(this.font, 210, 20, DESCRIPTION_LABEL);
      $$0.addChild(CommonLayouts.labeledElement(this.font, this.nameBox, NAME_LABEL));
      $$0.addChild(CommonLayouts.labeledElement(this.font, this.descriptionBox, DESCRIPTION_LABEL));
      LinearLayout $$2 = this.layout.addToFooter(LinearLayout.horizontal().spacing(10));
      $$2.addChild($$1);
      $$2.addChild(Button.builder(CommonComponents.GUI_BACK, p_293570_ -> this.onClose()).build());
      this.layout.visitWidgets(p_321338_ -> {
         AbstractWidget var10000 = this.addRenderableWidget(p_321338_);
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

   private void createWorld(RealmsServer p_305781_) {
      RealmCreationTask $$1 = new RealmCreationTask(p_305781_.id, this.nameBox.getValue(), this.descriptionBox.getValue());
      RealmsResetWorldScreen $$2 = RealmsResetWorldScreen.forNewRealm(this, p_305781_, $$1, () -> this.minecraft.execute(() -> {
         RealmsMainScreen.refreshServerList();
         this.minecraft.setScreen(this.lastScreen);
      }));
      this.minecraft.setScreen($$2);
   }

   private void createSnapshotWorld(long p_305809_) {
      Screen $$1 = new RealmsResetNormalWorldScreen(
         p_305627_ -> {
            if (p_305627_ == null) {
               this.minecraft.setScreen(this);
            } else {
               this.minecraft
                  .setScreen(
                     new RealmsLongRunningMcoTaskScreen(
                        this, new CreateSnapshotRealmTask(this.lastScreen, p_305809_, p_305627_, this.nameBox.getValue(), this.descriptionBox.getValue())
                     )
                  );
            }
         },
         CREATE_REALM_TEXT
      );
      this.minecraft.setScreen($$1);
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }
}
