package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.Subscription;
import com.mojang.realmsclient.exception.RealmsServiceException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import javax.annotation.Nullable;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.FittingMultiLineTextWidget;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.CommonLinks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsSubscriptionInfoScreen extends RealmsScreen {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final Component SUBSCRIPTION_TITLE = Component.translatable("mco.configure.world.subscription.title");
   private static final Component SUBSCRIPTION_START_LABEL = Component.translatable("mco.configure.world.subscription.start");
   private static final Component TIME_LEFT_LABEL = Component.translatable("mco.configure.world.subscription.timeleft");
   private static final Component DAYS_LEFT_LABEL = Component.translatable("mco.configure.world.subscription.recurring.daysleft");
   private static final Component SUBSCRIPTION_EXPIRED_TEXT = Component.translatable("mco.configure.world.subscription.expired");
   private static final Component SUBSCRIPTION_LESS_THAN_A_DAY_TEXT = Component.translatable("mco.configure.world.subscription.less_than_a_day");
   private static final Component UNKNOWN = Component.translatable("mco.configure.world.subscription.unknown");
   private static final Component RECURRING_INFO = Component.translatable("mco.configure.world.subscription.recurring.info");
   private final Screen lastScreen;
   final RealmsServer serverData;
   final Screen mainScreen;
   private Component daysLeft = UNKNOWN;
   private Component startDate = UNKNOWN;
   @Nullable
   private Subscription.SubscriptionType type;

   public RealmsSubscriptionInfoScreen(Screen p_89979_, RealmsServer p_89980_, Screen p_89981_) {
      super(GameNarrator.NO_TITLE);
      this.lastScreen = p_89979_;
      this.serverData = p_89980_;
      this.mainScreen = p_89981_;
   }

   @Override
   public void init() {
      this.getSubscription(this.serverData.id);
      this.addRenderableWidget(
         Button.builder(
               Component.translatable("mco.configure.world.subscription.extend"),
               p_307031_ -> ConfirmLinkScreen.confirmLinkNow(
                  this, CommonLinks.extendRealms(this.serverData.remoteSubscriptionId, this.minecraft.getUser().getProfileId())
               )
            )
            .bounds(this.width / 2 - 100, row(6), 200, 20)
            .build()
      );
      if (this.serverData.expired) {
         this.addRenderableWidget(
            Button.builder(
                  Component.translatable("mco.configure.world.delete.button"),
                  p_344132_ -> this.minecraft
                     .setScreen(
                        RealmsPopups.warningPopupScreen(
                           this, Component.translatable("mco.configure.world.delete.question.line1"), p_344131_ -> this.deleteRealm()
                        )
                     )
               )
               .bounds(this.width / 2 - 100, row(10), 200, 20)
               .build()
         );
      } else if (RealmsMainScreen.isSnapshot() && this.serverData.parentWorldName != null) {
         this.addRenderableWidget(
            new FittingMultiLineTextWidget(
               this.width / 2 - 100, row(8), 200, 46, Component.translatable("mco.snapshot.subscription.info", this.serverData.parentWorldName), this.font
            )
         );
      } else {
         this.addRenderableWidget(new FittingMultiLineTextWidget(this.width / 2 - 100, row(8), 200, 46, RECURRING_INFO, this.font));
      }

      this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, p_307032_ -> this.onClose()).bounds(this.width / 2 - 100, row(12), 200, 20).build());
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinLines(SUBSCRIPTION_TITLE, SUBSCRIPTION_START_LABEL, this.startDate, TIME_LEFT_LABEL, this.daysLeft);
   }

   private void deleteRealm() {
      (new Thread("Realms-delete-realm") {
            @Override
            public void run() {
               try {
                  RealmsClient $$0 = RealmsClient.create();
                  $$0.deleteRealm(RealmsSubscriptionInfoScreen.this.serverData.id);
               } catch (RealmsServiceException var2) {
                  RealmsSubscriptionInfoScreen.LOGGER.error("Couldn't delete world", var2);
               }

               RealmsSubscriptionInfoScreen.this.minecraft
                  .execute(() -> RealmsSubscriptionInfoScreen.this.minecraft.setScreen(RealmsSubscriptionInfoScreen.this.mainScreen));
            }
         })
         .start();
      this.minecraft.setScreen(this);
   }

   private void getSubscription(long p_89990_) {
      RealmsClient $$1 = RealmsClient.create();

      try {
         Subscription $$2 = $$1.subscriptionFor(p_89990_);
         this.daysLeft = this.daysLeftPresentation($$2.daysLeft);
         this.startDate = localPresentation($$2.startDate);
         this.type = $$2.type;
      } catch (RealmsServiceException var5) {
         LOGGER.error("Couldn't get subscription", var5);
         this.minecraft.setScreen(new RealmsGenericErrorScreen(var5, this.lastScreen));
      }
   }

   private static Component localPresentation(long p_182539_) {
      Calendar $$1 = new GregorianCalendar(TimeZone.getDefault());
      $$1.setTimeInMillis(p_182539_);
      return Component.literal(DateFormat.getDateTimeInstance().format($$1.getTime()));
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   @Override
   public void render(GuiGraphics p_282831_, int p_281266_, int p_281799_, float p_282330_) {
      super.render(p_282831_, p_281266_, p_281799_, p_282330_);
      int $$4 = this.width / 2 - 100;
      p_282831_.drawCenteredString(this.font, SUBSCRIPTION_TITLE, this.width / 2, 17, -1);
      p_282831_.drawString(this.font, SUBSCRIPTION_START_LABEL, $$4, row(0), -6250336, false);
      p_282831_.drawString(this.font, this.startDate, $$4, row(1), -1, false);
      if (this.type == Subscription.SubscriptionType.NORMAL) {
         p_282831_.drawString(this.font, TIME_LEFT_LABEL, $$4, row(3), -6250336, false);
      } else if (this.type == Subscription.SubscriptionType.RECURRING) {
         p_282831_.drawString(this.font, DAYS_LEFT_LABEL, $$4, row(3), -6250336, false);
      }

      p_282831_.drawString(this.font, this.daysLeft, $$4, row(4), -1, false);
   }

   private Component daysLeftPresentation(int p_89984_) {
      if (p_89984_ < 0 && this.serverData.expired) {
         return SUBSCRIPTION_EXPIRED_TEXT;
      } else if (p_89984_ <= 1) {
         return SUBSCRIPTION_LESS_THAN_A_DAY_TEXT;
      } else {
         int $$1 = p_89984_ / 30;
         int $$2 = p_89984_ % 30;
         boolean $$3 = $$1 > 0;
         boolean $$4 = $$2 > 0;
         if ($$3 && $$4) {
            return Component.translatable("mco.configure.world.subscription.remaining.months.days", $$1, $$2);
         } else if ($$3) {
            return Component.translatable("mco.configure.world.subscription.remaining.months", $$1);
         } else {
            return $$4 ? Component.translatable("mco.configure.world.subscription.remaining.days", $$2) : Component.empty();
         }
      }
   }
}
