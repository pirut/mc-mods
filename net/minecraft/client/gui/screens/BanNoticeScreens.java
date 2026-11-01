package net.minecraft.client.gui.screens;

import com.mojang.authlib.minecraft.BanDetails;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.chat.report.BanReason;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.util.CommonLinks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

@OnlyIn(Dist.CLIENT)
public class BanNoticeScreens {
   private static final Component TEMPORARY_BAN_TITLE = Component.translatable("gui.banned.title.temporary").withStyle(ChatFormatting.BOLD);
   private static final Component PERMANENT_BAN_TITLE = Component.translatable("gui.banned.title.permanent").withStyle(ChatFormatting.BOLD);
   public static final Component NAME_BAN_TITLE = Component.translatable("gui.banned.name.title").withStyle(ChatFormatting.BOLD);
   private static final Component SKIN_BAN_TITLE = Component.translatable("gui.banned.skin.title").withStyle(ChatFormatting.BOLD);
   private static final Component SKIN_BAN_DESCRIPTION = Component.translatable(
      "gui.banned.skin.description", Component.translationArg(CommonLinks.SUSPENSION_HELP)
   );

   public static ConfirmLinkScreen create(BooleanConsumer p_299953_, BanDetails p_299893_) {
      return new ConfirmLinkScreen(
         p_299953_, getBannedTitle(p_299893_), getBannedScreenText(p_299893_), CommonLinks.SUSPENSION_HELP, CommonComponents.GUI_ACKNOWLEDGE, true
      );
   }

   public static ConfirmLinkScreen createSkinBan(Runnable p_300031_) {
      URI $$1 = CommonLinks.SUSPENSION_HELP;
      return new ConfirmLinkScreen(p_351642_ -> {
         if (p_351642_) {
            Util.getPlatform().openUri($$1);
         }

         p_300031_.run();
      }, SKIN_BAN_TITLE, SKIN_BAN_DESCRIPTION, $$1, CommonComponents.GUI_ACKNOWLEDGE, true);
   }

   public static ConfirmLinkScreen createNameBan(String p_299968_, Runnable p_299926_) {
      URI $$2 = CommonLinks.SUSPENSION_HELP;
      return new ConfirmLinkScreen(
         p_351639_ -> {
            if (p_351639_) {
               Util.getPlatform().openUri($$2);
            }

            p_299926_.run();
         },
         NAME_BAN_TITLE,
         Component.translatable(
            "gui.banned.name.description", Component.literal(p_299968_).withStyle(ChatFormatting.YELLOW), Component.translationArg(CommonLinks.SUSPENSION_HELP)
         ),
         $$2,
         CommonComponents.GUI_ACKNOWLEDGE,
         true
      );
   }

   private static Component getBannedTitle(BanDetails p_299957_) {
      return isTemporaryBan(p_299957_) ? TEMPORARY_BAN_TITLE : PERMANENT_BAN_TITLE;
   }

   private static Component getBannedScreenText(BanDetails p_299924_) {
      return Component.translatable(
         "gui.banned.description", getBanReasonText(p_299924_), getBanStatusText(p_299924_), Component.translationArg(CommonLinks.SUSPENSION_HELP)
      );
   }

   private static Component getBanReasonText(BanDetails p_299989_) {
      String $$1 = p_299989_.reason();
      String $$2 = p_299989_.reasonMessage();
      if (StringUtils.isNumeric($$1)) {
         int $$3 = Integer.parseInt($$1);
         BanReason $$4 = BanReason.byId($$3);
         Component $$5;
         if ($$4 != null) {
            $$5 = ComponentUtils.mergeStyles($$4.title().copy(), Style.EMPTY.withBold(true));
         } else if ($$2 != null) {
            $$5 = Component.translatable("gui.banned.description.reason_id_message", $$3, $$2).withStyle(ChatFormatting.BOLD);
         } else {
            $$5 = Component.translatable("gui.banned.description.reason_id", $$3).withStyle(ChatFormatting.BOLD);
         }

         return Component.translatable("gui.banned.description.reason", $$5);
      } else {
         return Component.translatable("gui.banned.description.unknownreason");
      }
   }

   private static Component getBanStatusText(BanDetails p_299862_) {
      if (isTemporaryBan(p_299862_)) {
         Component $$1 = getBanDurationText(p_299862_);
         return Component.translatable(
            "gui.banned.description.temporary", Component.translatable("gui.banned.description.temporary.duration", $$1).withStyle(ChatFormatting.BOLD)
         );
      } else {
         return Component.translatable("gui.banned.description.permanent").withStyle(ChatFormatting.BOLD);
      }
   }

   private static Component getBanDurationText(BanDetails p_299833_) {
      Duration $$1 = Duration.between(Instant.now(), p_299833_.expires());
      long $$2 = $$1.toHours();
      if ($$2 > 72L) {
         return CommonComponents.days($$1.toDays());
      } else {
         return $$2 < 1L ? CommonComponents.minutes($$1.toMinutes()) : CommonComponents.hours($$1.toHours());
      }
   }

   private static boolean isTemporaryBan(BanDetails p_299982_) {
      return p_299982_.expires() != null;
   }
}
