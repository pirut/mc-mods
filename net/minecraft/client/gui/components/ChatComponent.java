package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ArrayListDeque;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ChatComponent {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_CHAT_HISTORY = 100;
   private static final int MESSAGE_NOT_FOUND = -1;
   private static final int MESSAGE_INDENT = 4;
   private static final int MESSAGE_TAG_MARGIN_LEFT = 4;
   private static final int BOTTOM_MARGIN = 40;
   private static final int TIME_BEFORE_MESSAGE_DELETION = 60;
   private static final Component DELETED_CHAT_MESSAGE = Component.translatable("chat.deleted_marker").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
   private final Minecraft minecraft;
   private final ArrayListDeque<String> recentChat = new ArrayListDeque<>(100);
   private final List<GuiMessage> allMessages = Lists.newArrayList();
   private final List<GuiMessage.Line> trimmedMessages = Lists.newArrayList();
   private int chatScrollbarPos;
   private boolean newMessageSinceScroll;
   private final List<ChatComponent.DelayedMessageDeletion> messageDeletionQueue = new ArrayList<>();

   public ChatComponent(Minecraft p_93768_) {
      this.minecraft = p_93768_;
      this.recentChat.addAll(p_93768_.commandHistory().history());
   }

   public void tick() {
      if (!this.messageDeletionQueue.isEmpty()) {
         this.processMessageDeletionQueue();
      }
   }

   public void render(GuiGraphics p_282077_, int p_283491_, int p_282406_, int p_283111_, boolean p_316855_) {
      if (!this.isChatHidden()) {
         int $$5 = this.getLinesPerPage();
         int $$6 = this.trimmedMessages.size();
         if ($$6 > 0) {
            this.minecraft.getProfiler().push("chat");
            float $$7 = (float)this.getScale();
            int $$8 = Mth.ceil(this.getWidth() / $$7);
            int $$9 = p_282077_.guiHeight();
            p_282077_.pose().pushPose();
            p_282077_.pose().scale($$7, $$7, 1.0F);
            p_282077_.pose().translate(4.0F, 0.0F, 0.0F);
            int $$10 = Mth.floor(($$9 - 40) / $$7);
            int $$11 = this.getMessageEndIndexAt(this.screenToChatX(p_282406_), this.screenToChatY(p_283111_));
            double $$12 = this.minecraft.options.chatOpacity().get() * 0.9F + 0.1F;
            double $$13 = this.minecraft.options.textBackgroundOpacity().get();
            double $$14 = this.minecraft.options.chatLineSpacing().get();
            int $$15 = this.getLineHeight();
            int $$16 = (int)Math.round(-8.0 * ($$14 + 1.0) + 4.0 * $$14);
            int $$17 = 0;

            for (int $$18 = 0; $$18 + this.chatScrollbarPos < this.trimmedMessages.size() && $$18 < $$5; $$18++) {
               int $$19 = $$18 + this.chatScrollbarPos;
               GuiMessage.Line $$20 = this.trimmedMessages.get($$19);
               if ($$20 != null) {
                  int $$21 = p_283491_ - $$20.addedTime();
                  if ($$21 < 200 || p_316855_) {
                     double $$22 = p_316855_ ? 1.0 : getTimeFactor($$21);
                     int $$23 = (int)(255.0 * $$22 * $$12);
                     int $$24 = (int)(255.0 * $$22 * $$13);
                     $$17++;
                     if ($$23 > 3) {
                        int $$25 = 0;
                        int $$26 = $$10 - $$18 * $$15;
                        int $$27 = $$26 + $$16;
                        p_282077_.fill(-4, $$26 - $$15, 0 + $$8 + 4 + 4, $$26, $$24 << 24);
                        GuiMessageTag $$28 = $$20.tag();
                        if ($$28 != null) {
                           int $$29 = $$28.indicatorColor() | $$23 << 24;
                           p_282077_.fill(-4, $$26 - $$15, -2, $$26, $$29);
                           if ($$19 == $$11 && $$28.icon() != null) {
                              int $$30 = this.getTagIconLeft($$20);
                              int $$31 = $$27 + 9;
                              this.drawTagIcon(p_282077_, $$30, $$31, $$28.icon());
                           }
                        }

                        p_282077_.pose().pushPose();
                        p_282077_.pose().translate(0.0F, 0.0F, 50.0F);
                        p_282077_.drawString(this.minecraft.font, $$20.content(), 0, $$27, 16777215 + ($$23 << 24));
                        p_282077_.pose().popPose();
                     }
                  }
               }
            }

            long $$32 = this.minecraft.getChatListener().queueSize();
            if ($$32 > 0L) {
               int $$33 = (int)(128.0 * $$12);
               int $$34 = (int)(255.0 * $$13);
               p_282077_.pose().pushPose();
               p_282077_.pose().translate(0.0F, (float)$$10, 0.0F);
               p_282077_.fill(-2, 0, $$8 + 4, 9, $$34 << 24);
               p_282077_.pose().translate(0.0F, 0.0F, 50.0F);
               p_282077_.drawString(this.minecraft.font, Component.translatable("chat.queue", $$32), 0, 1, 16777215 + ($$33 << 24));
               p_282077_.pose().popPose();
            }

            if (p_316855_) {
               int $$35 = this.getLineHeight();
               int $$36 = $$6 * $$35;
               int $$37 = $$17 * $$35;
               int $$38 = this.chatScrollbarPos * $$37 / $$6 - $$10;
               int $$39 = $$37 * $$37 / $$36;
               if ($$36 != $$37) {
                  int $$40 = $$38 > 0 ? 170 : 96;
                  int $$41 = this.newMessageSinceScroll ? 13382451 : 3355562;
                  int $$42 = $$8 + 4;
                  p_282077_.fill($$42, -$$38, $$42 + 2, -$$38 - $$39, 100, $$41 + ($$40 << 24));
                  p_282077_.fill($$42 + 2, -$$38, $$42 + 1, -$$38 - $$39, 100, 13421772 + ($$40 << 24));
               }
            }

            p_282077_.pose().popPose();
            this.minecraft.getProfiler().pop();
         }
      }
   }

   private void drawTagIcon(GuiGraphics p_283206_, int p_281677_, int p_281878_, GuiMessageTag.Icon p_282783_) {
      int $$4 = p_281878_ - p_282783_.height - 1;
      p_282783_.draw(p_283206_, p_281677_, $$4);
   }

   private int getTagIconLeft(GuiMessage.Line p_240622_) {
      return this.minecraft.font.width(p_240622_.content()) + 4;
   }

   private boolean isChatHidden() {
      return this.minecraft.options.chatVisibility().get() == ChatVisiblity.HIDDEN;
   }

   private static double getTimeFactor(int p_93776_) {
      double $$1 = p_93776_ / 200.0;
      $$1 = 1.0 - $$1;
      $$1 *= 10.0;
      $$1 = Mth.clamp($$1, 0.0, 1.0);
      return $$1 * $$1;
   }

   public void clearMessages(boolean p_93796_) {
      this.minecraft.getChatListener().clearQueue();
      this.messageDeletionQueue.clear();
      this.trimmedMessages.clear();
      this.allMessages.clear();
      if (p_93796_) {
         this.recentChat.clear();
         this.recentChat.addAll(this.minecraft.commandHistory().history());
      }
   }

   public void addMessage(Component p_93786_) {
      this.addMessage(p_93786_, null, this.minecraft.isSingleplayer() ? GuiMessageTag.systemSinglePlayer() : GuiMessageTag.system());
   }

   public void addMessage(Component p_241484_, @Nullable MessageSignature p_241323_, @Nullable GuiMessageTag p_241297_) {
      GuiMessage $$3 = new GuiMessage(this.minecraft.gui.getGuiTicks(), p_241484_, p_241323_, p_241297_);
      this.logChatMessage($$3);
      this.addMessageToDisplayQueue($$3);
      this.addMessageToQueue($$3);
   }

   private void logChatMessage(GuiMessage p_338237_) {
      String $$1 = p_338237_.content().getString().replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n");
      String $$2 = Optionull.map(p_338237_.tag(), GuiMessageTag::logTag);
      if ($$2 != null) {
         LOGGER.info("[{}] [CHAT] {}", $$2, $$1);
      } else {
         LOGGER.info("[CHAT] {}", $$1);
      }
   }

   private void addMessageToDisplayQueue(GuiMessage p_338816_) {
      int $$1 = Mth.floor(this.getWidth() / this.getScale());
      GuiMessageTag.Icon $$2 = p_338816_.icon();
      if ($$2 != null) {
         $$1 -= $$2.width + 4 + 2;
      }

      List<FormattedCharSequence> $$3 = ComponentRenderUtils.wrapComponents(p_338816_.content(), $$1, this.minecraft.font);
      boolean $$4 = this.isChatFocused();

      for (int $$5 = 0; $$5 < $$3.size(); $$5++) {
         FormattedCharSequence $$6 = $$3.get($$5);
         if ($$4 && this.chatScrollbarPos > 0) {
            this.newMessageSinceScroll = true;
            this.scrollChat(1);
         }

         boolean $$7 = $$5 == $$3.size() - 1;
         this.trimmedMessages.add(0, new GuiMessage.Line(p_338816_.addedTime(), $$6, p_338816_.tag(), $$7));
      }

      while (this.trimmedMessages.size() > 100) {
         this.trimmedMessages.remove(this.trimmedMessages.size() - 1);
      }
   }

   private void addMessageToQueue(GuiMessage p_338828_) {
      this.allMessages.add(0, p_338828_);

      while (this.allMessages.size() > 100) {
         this.allMessages.remove(this.allMessages.size() - 1);
      }
   }

   private void processMessageDeletionQueue() {
      int $$0 = this.minecraft.gui.getGuiTicks();
      this.messageDeletionQueue.removeIf(p_250713_ -> $$0 >= p_250713_.deletableAfter() ? this.deleteMessageOrDelay(p_250713_.signature()) == null : false);
   }

   public void deleteMessage(MessageSignature p_241324_) {
      ChatComponent.DelayedMessageDeletion $$1 = this.deleteMessageOrDelay(p_241324_);
      if ($$1 != null) {
         this.messageDeletionQueue.add($$1);
      }
   }

   @Nullable
   private ChatComponent.DelayedMessageDeletion deleteMessageOrDelay(MessageSignature p_251812_) {
      int $$1 = this.minecraft.gui.getGuiTicks();
      ListIterator<GuiMessage> $$2 = this.allMessages.listIterator();

      while ($$2.hasNext()) {
         GuiMessage $$3 = $$2.next();
         if (p_251812_.equals($$3.signature())) {
            int $$4 = $$3.addedTime() + 60;
            if ($$1 >= $$4) {
               $$2.set(this.createDeletedMarker($$3));
               this.refreshTrimmedMessages();
               return null;
            }

            return new ChatComponent.DelayedMessageDeletion(p_251812_, $$4);
         }
      }

      return null;
   }

   private GuiMessage createDeletedMarker(GuiMessage p_249789_) {
      return new GuiMessage(p_249789_.addedTime(), DELETED_CHAT_MESSAGE, null, GuiMessageTag.system());
   }

   public void rescaleChat() {
      this.resetChatScroll();
      this.refreshTrimmedMessages();
   }

   private void refreshTrimmedMessages() {
      this.trimmedMessages.clear();

      for (GuiMessage $$0 : Lists.reverse(this.allMessages)) {
         this.addMessageToDisplayQueue($$0);
      }
   }

   public ArrayListDeque<String> getRecentChat() {
      return this.recentChat;
   }

   public void addRecentChat(String p_93784_) {
      if (!p_93784_.equals(this.recentChat.peekLast())) {
         if (this.recentChat.size() >= 100) {
            this.recentChat.removeFirst();
         }

         this.recentChat.addLast(p_93784_);
      }

      if (p_93784_.startsWith("/")) {
         this.minecraft.commandHistory().addCommand(p_93784_);
      }
   }

   public void resetChatScroll() {
      this.chatScrollbarPos = 0;
      this.newMessageSinceScroll = false;
   }

   public void scrollChat(int p_205361_) {
      this.chatScrollbarPos += p_205361_;
      int $$1 = this.trimmedMessages.size();
      if (this.chatScrollbarPos > $$1 - this.getLinesPerPage()) {
         this.chatScrollbarPos = $$1 - this.getLinesPerPage();
      }

      if (this.chatScrollbarPos <= 0) {
         this.chatScrollbarPos = 0;
         this.newMessageSinceScroll = false;
      }
   }

   public boolean handleChatQueueClicked(double p_93773_, double p_93774_) {
      if (this.isChatFocused() && !this.minecraft.options.hideGui && !this.isChatHidden()) {
         ChatListener $$2 = this.minecraft.getChatListener();
         if ($$2.queueSize() == 0L) {
            return false;
         } else {
            double $$3 = p_93773_ - 2.0;
            double $$4 = this.minecraft.getWindow().getGuiScaledHeight() - p_93774_ - 40.0;
            if ($$3 <= Mth.floor(this.getWidth() / this.getScale()) && $$4 < 0.0 && $$4 > Mth.floor(-9.0 * this.getScale())) {
               $$2.acceptNextDelayedMessage();
               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   @Nullable
   public Style getClickedComponentStyleAt(double p_93801_, double p_93802_) {
      double $$2 = this.screenToChatX(p_93801_);
      double $$3 = this.screenToChatY(p_93802_);
      int $$4 = this.getMessageLineIndexAt($$2, $$3);
      if ($$4 >= 0 && $$4 < this.trimmedMessages.size()) {
         GuiMessage.Line $$5 = this.trimmedMessages.get($$4);
         return this.minecraft.font.getSplitter().componentStyleAtWidth($$5.content(), Mth.floor($$2));
      } else {
         return null;
      }
   }

   @Nullable
   public GuiMessageTag getMessageTagAt(double p_240576_, double p_240554_) {
      double $$2 = this.screenToChatX(p_240576_);
      double $$3 = this.screenToChatY(p_240554_);
      int $$4 = this.getMessageEndIndexAt($$2, $$3);
      if ($$4 >= 0 && $$4 < this.trimmedMessages.size()) {
         GuiMessage.Line $$5 = this.trimmedMessages.get($$4);
         GuiMessageTag $$6 = $$5.tag();
         if ($$6 != null && this.hasSelectedMessageTag($$2, $$5, $$6)) {
            return $$6;
         }
      }

      return null;
   }

   private boolean hasSelectedMessageTag(double p_240619_, GuiMessage.Line p_240547_, GuiMessageTag p_240637_) {
      if (p_240619_ < 0.0) {
         return true;
      } else {
         GuiMessageTag.Icon $$3 = p_240637_.icon();
         if ($$3 == null) {
            return false;
         } else {
            int $$4 = this.getTagIconLeft(p_240547_);
            int $$5 = $$4 + $$3.width;
            return p_240619_ >= $$4 && p_240619_ <= $$5;
         }
      }
   }

   private double screenToChatX(double p_240580_) {
      return p_240580_ / this.getScale() - 4.0;
   }

   private double screenToChatY(double p_240548_) {
      double $$1 = this.minecraft.getWindow().getGuiScaledHeight() - p_240548_ - 40.0;
      return $$1 / (this.getScale() * this.getLineHeight());
   }

   private int getMessageEndIndexAt(double p_249245_, double p_252282_) {
      int $$2 = this.getMessageLineIndexAt(p_249245_, p_252282_);
      if ($$2 == -1) {
         return -1;
      } else {
         while ($$2 >= 0) {
            if (this.trimmedMessages.get($$2).endOfEntry()) {
               return $$2;
            }

            $$2--;
         }

         return $$2;
      }
   }

   private int getMessageLineIndexAt(double p_249099_, double p_250008_) {
      if (this.isChatFocused() && !this.isChatHidden()) {
         if (!(p_249099_ < -4.0) && !(p_249099_ > Mth.floor(this.getWidth() / this.getScale()))) {
            int $$2 = Math.min(this.getLinesPerPage(), this.trimmedMessages.size());
            if (p_250008_ >= 0.0 && p_250008_ < $$2) {
               int $$3 = Mth.floor(p_250008_ + this.chatScrollbarPos);
               if ($$3 >= 0 && $$3 < this.trimmedMessages.size()) {
                  return $$3;
               }
            }

            return -1;
         } else {
            return -1;
         }
      } else {
         return -1;
      }
   }

   public boolean isChatFocused() {
      return this.minecraft.screen instanceof ChatScreen;
   }

   public int getWidth() {
      return getWidth(this.minecraft.options.chatWidth().get());
   }

   public int getHeight() {
      return getHeight(this.isChatFocused() ? this.minecraft.options.chatHeightFocused().get() : this.minecraft.options.chatHeightUnfocused().get());
   }

   public double getScale() {
      return this.minecraft.options.chatScale().get();
   }

   public static int getWidth(double p_93799_) {
      int $$1 = 320;
      int $$2 = 40;
      return Mth.floor(p_93799_ * 280.0 + 40.0);
   }

   public static int getHeight(double p_93812_) {
      int $$1 = 180;
      int $$2 = 20;
      return Mth.floor(p_93812_ * 160.0 + 20.0);
   }

   public static double defaultUnfocusedPct() {
      int $$0 = 180;
      int $$1 = 20;
      return 70.0 / (getHeight(1.0) - 20);
   }

   public int getLinesPerPage() {
      return this.getHeight() / this.getLineHeight();
   }

   private int getLineHeight() {
      return (int)(9.0 * (this.minecraft.options.chatLineSpacing().get() + 1.0));
   }

   public ChatComponent.State storeState() {
      return new ChatComponent.State(List.copyOf(this.allMessages), List.copyOf(this.recentChat), List.copyOf(this.messageDeletionQueue));
   }

   public void restoreState(ChatComponent.State p_338814_) {
      this.recentChat.clear();
      this.recentChat.addAll(p_338814_.history);
      this.messageDeletionQueue.clear();
      this.messageDeletionQueue.addAll(p_338814_.delayedMessageDeletions);
      this.allMessages.clear();
      this.allMessages.addAll(p_338814_.messages);
      this.refreshTrimmedMessages();
   }

   @OnlyIn(Dist.CLIENT)
   record DelayedMessageDeletion(MessageSignature signature, int deletableAfter) {
   }

   @OnlyIn(Dist.CLIENT)
   public static class State {
      final List<GuiMessage> messages;
      final List<String> history;
      final List<ChatComponent.DelayedMessageDeletion> delayedMessageDeletions;

      public State(List<GuiMessage> p_338549_, List<String> p_338655_, List<ChatComponent.DelayedMessageDeletion> p_338701_) {
         this.messages = p_338549_;
         this.history = p_338655_;
         this.delayedMessageDeletions = p_338701_;
      }
   }
}
