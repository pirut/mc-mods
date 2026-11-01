package com.mojang.realmsclient.dto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsNotification {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final String NOTIFICATION_UUID = "notificationUuid";
   private static final String DISMISSABLE = "dismissable";
   private static final String SEEN = "seen";
   private static final String TYPE = "type";
   private static final String VISIT_URL = "visitUrl";
   private static final String INFO_POPUP = "infoPopup";
   static final Component BUTTON_TEXT_FALLBACK = Component.translatable("mco.notification.visitUrl.buttonText.default");
   final UUID uuid;
   final boolean dismissable;
   final boolean seen;
   final String type;

   RealmsNotification(UUID p_275316_, boolean p_275303_, boolean p_275497_, String p_275401_) {
      this.uuid = p_275316_;
      this.dismissable = p_275303_;
      this.seen = p_275497_;
      this.type = p_275401_;
   }

   public boolean seen() {
      return this.seen;
   }

   public boolean dismissable() {
      return this.dismissable;
   }

   public UUID uuid() {
      return this.uuid;
   }

   public static List<RealmsNotification> parseList(String p_275464_) {
      List<RealmsNotification> $$1 = new ArrayList<>();

      try {
         for (JsonElement $$3 : JsonParser.parseString(p_275464_).getAsJsonObject().get("notifications").getAsJsonArray()) {
            $$1.add(parse($$3.getAsJsonObject()));
         }
      } catch (Exception var5) {
         LOGGER.error("Could not parse list of RealmsNotifications", var5);
      }

      return $$1;
   }

   private static RealmsNotification parse(JsonObject p_275549_) {
      UUID $$1 = JsonUtils.getUuidOr("notificationUuid", p_275549_, null);
      if ($$1 == null) {
         throw new IllegalStateException("Missing required property notificationUuid");
      } else {
         boolean $$2 = JsonUtils.getBooleanOr("dismissable", p_275549_, true);
         boolean $$3 = JsonUtils.getBooleanOr("seen", p_275549_, false);
         String $$4 = JsonUtils.getRequiredString("type", p_275549_);
         RealmsNotification $$5 = new RealmsNotification($$1, $$2, $$3, $$4);

         return (RealmsNotification)(switch ($$4) {
            case "visitUrl" -> RealmsNotification.VisitUrl.parse($$5, p_275549_);
            case "infoPopup" -> RealmsNotification.InfoPopup.parse($$5, p_275549_);
            default -> $$5;
         });
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class InfoPopup extends RealmsNotification {
      private static final String TITLE = "title";
      private static final String MESSAGE = "message";
      private static final String IMAGE = "image";
      private static final String URL_BUTTON = "urlButton";
      private final RealmsText title;
      private final RealmsText message;
      private final ResourceLocation image;
      @Nullable
      private final RealmsNotification.UrlButton urlButton;

      private InfoPopup(
         RealmsNotification p_304992_, RealmsText p_304968_, RealmsText p_304788_, ResourceLocation p_304749_, @Nullable RealmsNotification.UrlButton p_304471_
      ) {
         super(p_304992_.uuid, p_304992_.dismissable, p_304992_.seen, p_304992_.type);
         this.title = p_304968_;
         this.message = p_304788_;
         this.image = p_304749_;
         this.urlButton = p_304471_;
      }

      public static RealmsNotification.InfoPopup parse(RealmsNotification p_304647_, JsonObject p_304686_) {
         RealmsText $$2 = JsonUtils.getRequired("title", p_304686_, RealmsText::parse);
         RealmsText $$3 = JsonUtils.getRequired("message", p_304686_, RealmsText::parse);
         ResourceLocation $$4 = ResourceLocation.parse(JsonUtils.getRequiredString("image", p_304686_));
         RealmsNotification.UrlButton $$5 = JsonUtils.getOptional("urlButton", p_304686_, RealmsNotification.UrlButton::parse);
         return new RealmsNotification.InfoPopup(p_304647_, $$2, $$3, $$4, $$5);
      }

      @Nullable
      public PopupScreen buildScreen(Screen p_304415_, Consumer<UUID> p_304962_) {
         Component $$2 = this.title.createComponent();
         if ($$2 == null) {
            RealmsNotification.LOGGER.warn("Realms info popup had title with no available translation: {}", this.title);
            return null;
         } else {
            PopupScreen.Builder $$3 = new PopupScreen.Builder(p_304415_, $$2)
               .setImage(this.image)
               .setMessage(this.message.createComponent(CommonComponents.EMPTY));
            if (this.urlButton != null) {
               $$3.addButton(this.urlButton.urlText.createComponent(RealmsNotification.BUTTON_TEXT_FALLBACK), p_304766_ -> {
                  Minecraft $$3x = Minecraft.getInstance();
                  $$3x.setScreen(new ConfirmLinkScreen(p_304708_ -> {
                     if (p_304708_) {
                        Util.getPlatform().openUri(this.urlButton.url);
                        $$3x.setScreen(p_304415_);
                     } else {
                        $$3x.setScreen(p_304766_);
                     }
                  }, this.urlButton.url, true));
                  p_304962_.accept(this.uuid());
               });
            }

            $$3.addButton(CommonComponents.GUI_OK, p_304476_ -> {
               p_304476_.onClose();
               p_304962_.accept(this.uuid());
            });
            $$3.onClose(() -> p_304962_.accept(this.uuid()));
            return $$3.build();
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   record UrlButton(String url, RealmsText urlText) {
      private static final String URL = "url";
      private static final String URL_TEXT = "urlText";

      public static RealmsNotification.UrlButton parse(JsonObject p_304518_) {
         String $$1 = JsonUtils.getRequiredString("url", p_304518_);
         RealmsText $$2 = JsonUtils.getRequired("urlText", p_304518_, RealmsText::parse);
         return new RealmsNotification.UrlButton($$1, $$2);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class VisitUrl extends RealmsNotification {
      private static final String URL = "url";
      private static final String BUTTON_TEXT = "buttonText";
      private static final String MESSAGE = "message";
      private final String url;
      private final RealmsText buttonText;
      private final RealmsText message;

      private VisitUrl(RealmsNotification p_275564_, String p_275312_, RealmsText p_275433_, RealmsText p_275541_) {
         super(p_275564_.uuid, p_275564_.dismissable, p_275564_.seen, p_275564_.type);
         this.url = p_275312_;
         this.buttonText = p_275433_;
         this.message = p_275541_;
      }

      public static RealmsNotification.VisitUrl parse(RealmsNotification p_275651_, JsonObject p_275278_) {
         String $$2 = JsonUtils.getRequiredString("url", p_275278_);
         RealmsText $$3 = JsonUtils.getRequired("buttonText", p_275278_, RealmsText::parse);
         RealmsText $$4 = JsonUtils.getRequired("message", p_275278_, RealmsText::parse);
         return new RealmsNotification.VisitUrl(p_275651_, $$2, $$3, $$4);
      }

      public Component getMessage() {
         return this.message.createComponent(Component.translatable("mco.notification.visitUrl.message.default"));
      }

      public Button buildOpenLinkButton(Screen p_275412_) {
         Component $$1 = this.buttonText.createComponent(RealmsNotification.BUTTON_TEXT_FALLBACK);
         return Button.builder($$1, ConfirmLinkScreen.confirmLink(p_275412_, this.url)).build();
      }
   }
}
