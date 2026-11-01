package net.minecraft.client.gui.screens.reporting;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatEvent;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.client.multiplayer.chat.report.ChatReportContextBuilder;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.SignedMessageLink;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChatSelectionLogFiller {
   private final ChatLog log;
   private final ChatReportContextBuilder contextBuilder;
   private final Predicate<LoggedChatMessage.Player> canReport;
   @Nullable
   private SignedMessageLink previousLink = null;
   private int eventId;
   private int missedCount;
   @Nullable
   private PlayerChatMessage lastMessage;

   public ChatSelectionLogFiller(ReportingContext p_251076_, Predicate<LoggedChatMessage.Player> p_250367_) {
      this.log = p_251076_.chatLog();
      this.contextBuilder = new ChatReportContextBuilder(p_251076_.sender().reportLimits().leadingContextMessageCount());
      this.canReport = p_250367_;
      this.eventId = this.log.end();
   }

   public void fillNextPage(int p_239016_, ChatSelectionLogFiller.Output p_239017_) {
      int $$2 = 0;

      while ($$2 < p_239016_) {
         LoggedChatEvent $$3 = this.log.lookup(this.eventId);
         if ($$3 == null) {
            break;
         }

         int $$4 = this.eventId--;
         if ($$3 instanceof LoggedChatMessage.Player $$5 && !$$5.message().equals(this.lastMessage)) {
            if (this.acceptMessage(p_239017_, $$5)) {
               if (this.missedCount > 0) {
                  p_239017_.acceptDivider(Component.translatable("gui.chatSelection.fold", this.missedCount));
                  this.missedCount = 0;
               }

               p_239017_.acceptMessage($$4, $$5);
               $$2++;
            } else {
               this.missedCount++;
            }

            this.lastMessage = $$5.message();
         }
      }
   }

   private boolean acceptMessage(ChatSelectionLogFiller.Output p_254300_, LoggedChatMessage.Player p_253803_) {
      PlayerChatMessage $$2 = p_253803_.message();
      boolean $$3 = this.contextBuilder.acceptContext($$2);
      if (this.canReport.test(p_253803_)) {
         this.contextBuilder.trackContext($$2);
         if (this.previousLink != null && !this.previousLink.isDescendantOf($$2.link())) {
            p_254300_.acceptDivider(Component.translatable("gui.chatSelection.join", p_253803_.profile().getName()).withStyle(ChatFormatting.YELLOW));
         }

         this.previousLink = $$2.link();
         return true;
      } else {
         return $$3;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface Output {
      void acceptMessage(int var1, LoggedChatMessage.Player var2);

      void acceptDivider(Component var1);
   }
}
