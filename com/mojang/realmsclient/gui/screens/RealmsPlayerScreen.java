package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.Ops;
import com.mojang.realmsclient.dto.PlayerInfo;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.RealmsUtil;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsPlayerScreen extends RealmsScreen {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.configure.world.players.title");
   static final Component QUESTION_TITLE = Component.translatable("mco.question");
   private static final int PADDING = 8;
   final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final RealmsConfigureWorldScreen lastScreen;
   final RealmsServer serverData;
   @Nullable
   private RealmsPlayerScreen.InvitedObjectSelectionList invitedList;
   boolean stateChanged;

   public RealmsPlayerScreen(RealmsConfigureWorldScreen p_89089_, RealmsServer p_89090_) {
      super(TITLE);
      this.lastScreen = p_89089_;
      this.serverData = p_89090_;
   }

   @Override
   public void init() {
      this.layout.addTitleHeader(TITLE, this.font);
      this.invitedList = this.layout.addToContents(new RealmsPlayerScreen.InvitedObjectSelectionList());
      this.repopulateInvitedList();
      LinearLayout $$0 = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      $$0.addChild(
         Button.builder(
               Component.translatable("mco.configure.world.buttons.invite"),
               p_280732_ -> this.minecraft.setScreen(new RealmsInviteScreen(this.lastScreen, this, this.serverData))
            )
            .build()
      );
      $$0.addChild(Button.builder(CommonComponents.GUI_BACK, p_329657_ -> this.onClose()).build());
      this.layout.visitWidgets(p_329656_ -> {
         AbstractWidget var10000 = this.addRenderableWidget(p_329656_);
      });
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      if (this.invitedList != null) {
         this.invitedList.updateSize(this.width, this.layout);
      }
   }

   void repopulateInvitedList() {
      if (this.invitedList != null) {
         this.invitedList.children().clear();

         for (PlayerInfo $$0 : this.serverData.players) {
            this.invitedList.children().add(new RealmsPlayerScreen.Entry($$0));
         }
      }
   }

   @Override
   public void onClose() {
      this.backButtonClicked();
   }

   private void backButtonClicked() {
      if (this.stateChanged) {
         this.minecraft.setScreen(this.lastScreen.getNewScreen());
      } else {
         this.minecraft.setScreen(this.lastScreen);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class Entry extends ContainerObjectSelectionList.Entry<RealmsPlayerScreen.Entry> {
      private static final Component NORMAL_USER_TEXT = Component.translatable("mco.configure.world.invites.normal.tooltip");
      private static final Component OP_TEXT = Component.translatable("mco.configure.world.invites.ops.tooltip");
      private static final Component REMOVE_TEXT = Component.translatable("mco.configure.world.invites.remove.tooltip");
      private static final ResourceLocation MAKE_OP_SPRITE = ResourceLocation.withDefaultNamespace("player_list/make_operator");
      private static final ResourceLocation REMOVE_OP_SPRITE = ResourceLocation.withDefaultNamespace("player_list/remove_operator");
      private static final ResourceLocation REMOVE_PLAYER_SPRITE = ResourceLocation.withDefaultNamespace("player_list/remove_player");
      private static final int ICON_WIDTH = 8;
      private static final int ICON_HEIGHT = 7;
      private final PlayerInfo playerInfo;
      private final Button removeButton;
      private final Button makeOpButton;
      private final Button removeOpButton;

      public Entry(PlayerInfo p_89204_) {
         this.playerInfo = p_89204_;
         int $$1 = RealmsPlayerScreen.this.serverData.players.indexOf(this.playerInfo);
         this.makeOpButton = SpriteIconButton.builder(NORMAL_USER_TEXT, p_329669_ -> this.op($$1), false)
            .sprite(MAKE_OP_SPRITE, 8, 7)
            .width(16 + RealmsPlayerScreen.this.font.width(NORMAL_USER_TEXT))
            .narration(
               p_329661_ -> CommonComponents.joinForNarration(
                  Component.translatable("mco.invited.player.narration", p_89204_.getName()),
                  p_329661_.get(),
                  Component.translatable("narration.cycle_button.usage.focused", OP_TEXT)
               )
            )
            .build();
         this.removeOpButton = SpriteIconButton.builder(OP_TEXT, p_329663_ -> this.deop($$1), false)
            .sprite(REMOVE_OP_SPRITE, 8, 7)
            .width(16 + RealmsPlayerScreen.this.font.width(OP_TEXT))
            .narration(
               p_329659_ -> CommonComponents.joinForNarration(
                  Component.translatable("mco.invited.player.narration", p_89204_.getName()),
                  p_329659_.get(),
                  Component.translatable("narration.cycle_button.usage.focused", NORMAL_USER_TEXT)
               )
            )
            .build();
         this.removeButton = SpriteIconButton.builder(REMOVE_TEXT, p_329665_ -> this.uninvite($$1), false)
            .sprite(REMOVE_PLAYER_SPRITE, 8, 7)
            .width(16 + RealmsPlayerScreen.this.font.width(REMOVE_TEXT))
            .narration(
               p_329667_ -> CommonComponents.joinForNarration(Component.translatable("mco.invited.player.narration", p_89204_.getName()), p_329667_.get())
            )
            .build();
         this.updateOpButtons();
      }

      private void op(int p_331202_) {
         RealmsClient $$1 = RealmsClient.create();
         UUID $$2 = RealmsPlayerScreen.this.serverData.players.get(p_331202_).getUuid();

         try {
            this.updateOps($$1.op(RealmsPlayerScreen.this.serverData.id, $$2));
         } catch (RealmsServiceException var5) {
            RealmsPlayerScreen.LOGGER.error("Couldn't op the user", var5);
         }

         this.updateOpButtons();
      }

      private void deop(int p_331659_) {
         RealmsClient $$1 = RealmsClient.create();
         UUID $$2 = RealmsPlayerScreen.this.serverData.players.get(p_331659_).getUuid();

         try {
            this.updateOps($$1.deop(RealmsPlayerScreen.this.serverData.id, $$2));
         } catch (RealmsServiceException var5) {
            RealmsPlayerScreen.LOGGER.error("Couldn't deop the user", var5);
         }

         this.updateOpButtons();
      }

      private void uninvite(int p_330606_) {
         if (p_330606_ >= 0 && p_330606_ < RealmsPlayerScreen.this.serverData.players.size()) {
            PlayerInfo $$1 = RealmsPlayerScreen.this.serverData.players.get(p_330606_);
            RealmsConfirmScreen $$2 = new RealmsConfirmScreen(p_335150_ -> {
               if (p_335150_) {
                  RealmsClient $$3 = RealmsClient.create();

                  try {
                     $$3.uninvite(RealmsPlayerScreen.this.serverData.id, $$1.getUuid());
                  } catch (RealmsServiceException var6) {
                     RealmsPlayerScreen.LOGGER.error("Couldn't uninvite user", var6);
                  }

                  RealmsPlayerScreen.this.serverData.players.remove(p_330606_);
                  RealmsPlayerScreen.this.repopulateInvitedList();
               }

               RealmsPlayerScreen.this.stateChanged = true;
               RealmsPlayerScreen.this.minecraft.setScreen(RealmsPlayerScreen.this);
            }, RealmsPlayerScreen.QUESTION_TITLE, Component.translatable("mco.configure.world.uninvite.player", $$1.getName()));
            RealmsPlayerScreen.this.minecraft.setScreen($$2);
         }
      }

      private void updateOps(Ops p_330984_) {
         for (PlayerInfo $$1 : RealmsPlayerScreen.this.serverData.players) {
            $$1.setOperator(p_330984_.ops.contains($$1.getName()));
         }
      }

      private void updateOpButtons() {
         this.makeOpButton.visible = !this.playerInfo.isOperator();
         this.removeOpButton.visible = !this.makeOpButton.visible;
      }

      private Button activeOpButton() {
         return this.makeOpButton.visible ? this.makeOpButton : this.removeOpButton;
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return ImmutableList.of(this.activeOpButton(), this.removeButton);
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return ImmutableList.of(this.activeOpButton(), this.removeButton);
      }

      @Override
      public void render(
         GuiGraphics p_282985_,
         int p_281343_,
         int p_283042_,
         int p_282863_,
         int p_281381_,
         int p_282692_,
         int p_283240_,
         int p_282706_,
         boolean p_283067_,
         float p_282230_
      ) {
         int $$10;
         if (!this.playerInfo.getAccepted()) {
            $$10 = -6250336;
         } else if (this.playerInfo.getOnline()) {
            $$10 = 8388479;
         } else {
            $$10 = -1;
         }

         int $$13 = p_283042_ + p_282692_ / 2 - 16;
         RealmsUtil.renderPlayerFace(p_282985_, p_282863_, $$13, 32, this.playerInfo.getUuid());
         int $$14 = p_283042_ + p_282692_ / 2 - 9 / 2;
         p_282985_.drawString(RealmsPlayerScreen.this.font, this.playerInfo.getName(), p_282863_ + 8 + 32, $$14, $$10, false);
         int $$15 = p_283042_ + p_282692_ / 2 - 10;
         int $$16 = p_282863_ + p_281381_ - this.removeButton.getWidth();
         this.removeButton.setPosition($$16, $$15);
         this.removeButton.render(p_282985_, p_283240_, p_282706_, p_282230_);
         int $$17 = $$16 - this.activeOpButton().getWidth() - 8;
         this.makeOpButton.setPosition($$17, $$15);
         this.makeOpButton.render(p_282985_, p_283240_, p_282706_, p_282230_);
         this.removeOpButton.setPosition($$17, $$15);
         this.removeOpButton.render(p_282985_, p_283240_, p_282706_, p_282230_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class InvitedObjectSelectionList extends ContainerObjectSelectionList<RealmsPlayerScreen.Entry> {
      private static final int ITEM_HEIGHT = 36;

      public InvitedObjectSelectionList() {
         super(
            Minecraft.getInstance(),
            RealmsPlayerScreen.this.width,
            RealmsPlayerScreen.this.layout.getContentHeight(),
            RealmsPlayerScreen.this.layout.getHeaderHeight(),
            36
         );
         this.setRenderHeader(true, (int)(9.0F * 1.5F));
      }

      @Override
      protected void renderHeader(GuiGraphics p_331651_, int p_330785_, int p_331045_) {
         String $$3 = RealmsPlayerScreen.this.serverData.players != null ? Integer.toString(RealmsPlayerScreen.this.serverData.players.size()) : "0";
         Component $$4 = Component.translatable("mco.configure.world.invited.number", $$3).withStyle(ChatFormatting.UNDERLINE);
         p_331651_.drawString(
            RealmsPlayerScreen.this.font, $$4, p_330785_ + this.getRowWidth() / 2 - RealmsPlayerScreen.this.font.width($$4) / 2, p_331045_, -1, false
         );
      }

      @Override
      public int getMaxPosition() {
         return this.getItemCount() * this.itemHeight + this.headerHeight;
      }

      @Override
      public int getRowWidth() {
         return 300;
      }
   }
}
