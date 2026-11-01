package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.ArrayListDeque;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ChatComponent {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_CHAT_HISTORY = 100;
   private static final int MESSAGE_INDENT = 4;
   private static final int BOTTOM_MARGIN = 40;
   private static final int TOOLTIP_MAX_WIDTH = 210;
   private static final int TIME_BEFORE_MESSAGE_DELETION = 60;
   private static final Component DELETED_CHAT_MESSAGE = Component.translatable("chat.deleted_marker").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
   public static final int MESSAGE_BOTTOM_TO_MESSAGE_TOP = 8;
   public static final Identifier QUEUE_EXPAND_ID = Identifier.withDefaultNamespace("internal/expand_chat_queue");
   private static final Style QUEUE_EXPAND_TEXT_STYLE = Style.EMPTY
      .withClickEvent(new ClickEvent.Custom(QUEUE_EXPAND_ID, Optional.empty()))
      .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.queue.tooltip")));
   final Minecraft minecraft;
   private final ArrayListDeque<String> recentChat = new ArrayListDeque<>(100);
   private final List<GuiMessage> allMessages = Lists.newArrayList();
   private final List<GuiMessage.Line> trimmedMessages = Lists.newArrayList();
   private int chatScrollbarPos;
   private boolean newMessageSinceScroll;
   @Nullable
   private ChatComponent.Draft latestDraft;
   @Nullable
   private ChatScreen preservedScreen;
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

   private int forEachLine(ChatComponent.AlphaCalculator p_457552_, ChatComponent.LineConsumer p_427273_) {
      int i = this.getLinesPerPage();
      int j = 0;

      for (int k = Math.min(this.trimmedMessages.size() - this.chatScrollbarPos, i) - 1; k >= 0; k--) {
         int l = k + this.chatScrollbarPos;
         GuiMessage.Line guimessage$line = this.trimmedMessages.get(l);
         float f = p_457552_.calculate(guimessage$line);
         if (f > 1.0E-5F) {
            j++;
            p_427273_.accept(guimessage$line, k, f);
         }
      }

      return j;
   }

   public void render(GuiGraphics p_282077_, Font p_458144_, int p_283491_, int p_282406_, int p_283111_, boolean p_316855_, boolean p_480243_) {
      p_282077_.pose().pushMatrix();
      this.render(
         (ChatComponent.ChatGraphicsAccess)(p_316855_
            ? new ChatComponent.DrawingFocusedGraphicsAccess(p_282077_, p_458144_, p_282406_, p_283111_, p_480243_)
            : new ChatComponent.DrawingBackgroundGraphicsAccess(p_282077_)),
         p_282077_.guiHeight(),
         p_283491_,
         p_316855_
      );
      p_282077_.pose().popMatrix();
   }

   public void captureClickableText(ActiveTextCollector p_457758_, int p_457681_, int p_457530_, boolean p_458147_) {
      this.render(new ChatComponent.ClickableTextOnlyGraphicsAccess(p_457758_), p_457681_, p_457530_, p_458147_);
   }

   private void render(final ChatComponent.ChatGraphicsAccess p_457875_, int p_457839_, int p_457862_, boolean p_457930_) {
      if (!this.isChatHidden()) {
         int i = this.trimmedMessages.size();
         if (i > 0) {
            ProfilerFiller profilerfiller = Profiler.get();
            profilerfiller.push("chat");
            float f = (float)this.getScale();
            int j = Mth.ceil(this.getWidth() / f);
            final int k = Mth.floor((p_457839_ - 40) / f);
            final float f1 = this.minecraft.options.chatOpacity().get().floatValue() * 0.9F + 0.1F;
            float f2 = this.minecraft.options.textBackgroundOpacity().get().floatValue();
            int l = 9;
            int i1 = 8;
            double d0 = this.minecraft.options.chatLineSpacing().get();
            final int j1 = (int)(9.0 * (d0 + 1.0));
            final int k1 = (int)Math.round(8.0 * (d0 + 1.0) - 4.0 * d0);
            long l1 = this.minecraft.getChatListener().queueSize();
            ChatComponent.AlphaCalculator chatcomponent$alphacalculator = p_457930_
               ? ChatComponent.AlphaCalculator.FULLY_VISIBLE
               : ChatComponent.AlphaCalculator.timeBased(p_457862_);
            p_457875_.updatePose(p_457333_ -> {
               p_457333_.scale(f, f);
               p_457333_.translate(4.0F, 0.0F);
            });
            this.forEachLine(chatcomponent$alphacalculator, (p_457339_, p_457340_, p_457341_) -> {
               int j4 = k - p_457340_ * j1;
               int k4 = j4 - j1;
               p_457875_.fill(-4, k4, j + 4 + 4, j4, ARGB.black(p_457341_ * f2));
            });
            if (l1 > 0L) {
               p_457875_.fill(-2, k, j + 4, k + 9, ARGB.black(f2));
            }

            int i2 = this.forEachLine(chatcomponent$alphacalculator, new ChatComponent.LineConsumer() {
               boolean hoveredOverCurrentMessage;

               @Override
               public void accept(GuiMessage.Line p_457674_, int p_457678_, float p_458251_) {
                  int j4 = k - p_457678_ * j1;
                  int k4 = j4 - j1;
                  int l4 = j4 - k1;
                  boolean flag = p_457875_.handleMessage(l4, p_458251_ * f1, p_457674_.content());
                  this.hoveredOverCurrentMessage |= flag;
                  boolean flag1;
                  if (p_457674_.endOfEntry()) {
                     flag1 = this.hoveredOverCurrentMessage;
                     this.hoveredOverCurrentMessage = false;
                  } else {
                     flag1 = false;
                  }

                  GuiMessageTag guimessagetag = p_457674_.tag();
                  if (guimessagetag != null) {
                     p_457875_.handleTag(-4, k4, -2, j4, p_458251_ * f1, guimessagetag);
                     if (guimessagetag.icon() != null) {
                        int i5 = p_457674_.getTagIconLeft(ChatComponent.this.minecraft.font);
                        int j5 = l4 + 9;
                        p_457875_.handleTagIcon(i5, j5, flag1, guimessagetag, guimessagetag.icon());
                     }
                  }
               }
            });
            if (l1 > 0L) {
               int j2 = k + 9;
               Component component = Component.translatable("chat.queue", l1).setStyle(QUEUE_EXPAND_TEXT_STYLE);
               p_457875_.handleMessage(j2 - 8, 0.5F * f1, component.getVisualOrderText());
            }

            if (p_457930_) {
               int l3 = i * j1;
               int i4 = i2 * j1;
               int k2 = this.chatScrollbarPos * i4 / i - k;
               int l2 = i4 * i4 / l3;
               if (l3 != i4) {
                  int i3 = k2 > 0 ? 170 : 96;
                  int j3 = this.newMessageSinceScroll ? 13382451 : 3355562;
                  int k3 = j + 4;
                  p_457875_.fill(k3, -k2, k3 + 2, -k2 - l2, ARGB.color(i3, j3));
                  p_457875_.fill(k3 + 2, -k2, k3 + 1, -k2 - l2, ARGB.color(i3, 13421772));
               }
            }

            profilerfiller.pop();
         }
      }
   }

   private boolean isChatHidden() {
      return this.minecraft.options.chatVisibility().get() == ChatVisiblity.HIDDEN;
   }

   public void clearMessages(boolean p_93796_) {
      this.minecraft.getChatListener().flushQueue();
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
      GuiMessage guimessage = new GuiMessage(this.minecraft.gui.getGuiTicks(), p_241484_, p_241323_, p_241297_);
      this.logChatMessage(guimessage);
      this.addMessageToDisplayQueue(guimessage);
      this.addMessageToQueue(guimessage);
   }

   private void logChatMessage(GuiMessage p_338237_) {
      String s = p_338237_.content().getString().replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n");
      String s1 = Optionull.map(p_338237_.tag(), GuiMessageTag::logTag);
      if (s1 != null) {
         LOGGER.info("[{}] [CHAT] {}", s1, s);
      } else {
         LOGGER.info("[CHAT] {}", s);
      }
   }

   private void addMessageToDisplayQueue(GuiMessage p_338816_) {
      int i = Mth.floor(this.getWidth() / this.getScale());
      List<FormattedCharSequence> list = p_338816_.splitLines(this.minecraft.font, i);
      boolean flag = this.isChatFocused();

      for (int j = 0; j < list.size(); j++) {
         FormattedCharSequence formattedcharsequence = list.get(j);
         if (flag && this.chatScrollbarPos > 0) {
            this.newMessageSinceScroll = true;
            this.scrollChat(1);
         }

         boolean flag1 = j == list.size() - 1;
         this.trimmedMessages.addFirst(new GuiMessage.Line(p_338816_.addedTime(), formattedcharsequence, p_338816_.tag(), flag1));
      }

      while (this.trimmedMessages.size() > 100) {
         this.trimmedMessages.removeLast();
      }
   }

   private void addMessageToQueue(GuiMessage p_338828_) {
      this.allMessages.addFirst(p_338828_);

      while (this.allMessages.size() > 100) {
         this.allMessages.removeLast();
      }
   }

   private void processMessageDeletionQueue() {
      int i = this.minecraft.gui.getGuiTicks();
      this.messageDeletionQueue.removeIf(p_250713_ -> i >= p_250713_.deletableAfter() ? this.deleteMessageOrDelay(p_250713_.signature()) == null : false);
   }

   public void deleteMessage(MessageSignature p_241324_) {
      ChatComponent.DelayedMessageDeletion chatcomponent$delayedmessagedeletion = this.deleteMessageOrDelay(p_241324_);
      if (chatcomponent$delayedmessagedeletion != null) {
         this.messageDeletionQueue.add(chatcomponent$delayedmessagedeletion);
      }
   }

   @Nullable
   private ChatComponent.DelayedMessageDeletion deleteMessageOrDelay(MessageSignature p_251812_) {
      int i = this.minecraft.gui.getGuiTicks();
      ListIterator<GuiMessage> listiterator = this.allMessages.listIterator();

      while (listiterator.hasNext()) {
         GuiMessage guimessage = listiterator.next();
         if (p_251812_.equals(guimessage.signature())) {
            int j = guimessage.addedTime() + 60;
            if (i >= j) {
               listiterator.set(this.createDeletedMarker(guimessage));
               this.refreshTrimmedMessages();
               return null;
            }

            return new ChatComponent.DelayedMessageDeletion(p_251812_, j);
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

      for (GuiMessage guimessage : Lists.reverse(this.allMessages)) {
         this.addMessageToDisplayQueue(guimessage);
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
      int i = this.trimmedMessages.size();
      if (this.chatScrollbarPos > i - this.getLinesPerPage()) {
         this.chatScrollbarPos = i - this.getLinesPerPage();
      }

      if (this.chatScrollbarPos <= 0) {
         this.chatScrollbarPos = 0;
         this.newMessageSinceScroll = false;
      }
   }

   public boolean isChatFocused() {
      return this.minecraft.screen instanceof ChatScreen;
   }

   private int getWidth() {
      return getWidth(this.minecraft.options.chatWidth().get());
   }

   private int getHeight() {
      return getHeight(this.isChatFocused() ? this.minecraft.options.chatHeightFocused().get() : this.minecraft.options.chatHeightUnfocused().get());
   }

   public double getScale() {
      return this.minecraft.options.chatScale().get();
   }

   public static int getWidth(double p_93799_) {
      int i = 320;
      int j = 40;
      return Mth.floor(p_93799_ * 280.0 + 40.0);
   }

   public static int getHeight(double p_93812_) {
      int i = 180;
      int j = 20;
      return Mth.floor(p_93812_ * 160.0 + 20.0);
   }

   public static double defaultUnfocusedPct() {
      int i = 180;
      int j = 20;
      return 70.0 / (getHeight(1.0) - 20);
   }

   public int getLinesPerPage() {
      return this.getHeight() / this.getLineHeight();
   }

   private int getLineHeight() {
      return (int)(9.0 * (this.minecraft.options.chatLineSpacing().get() + 1.0));
   }

   public void saveAsDraft(String p_437236_) {
      boolean flag = p_437236_.startsWith("/");
      this.latestDraft = new ChatComponent.Draft(p_437236_, flag ? ChatComponent.ChatMethod.COMMAND : ChatComponent.ChatMethod.MESSAGE);
   }

   public void discardDraft() {
      this.latestDraft = null;
   }

   public <T extends ChatScreen> T createScreen(ChatComponent.ChatMethod p_437379_, ChatScreen.ChatConstructor<T> p_437409_) {
      return this.latestDraft != null && p_437379_.isDraftRestorable(this.latestDraft)
         ? p_437409_.create(this.latestDraft.text(), true)
         : p_437409_.create(p_437379_.prefix(), false);
   }

   public void openScreen(ChatComponent.ChatMethod p_437427_, ChatScreen.ChatConstructor<?> p_437274_) {
      this.minecraft.setScreen(this.createScreen(p_437427_, (ChatScreen.ChatConstructor<Screen>)p_437274_));
   }

   public void preserveCurrentChatScreen() {
      if (this.minecraft.screen instanceof ChatScreen chatscreen) {
         this.preservedScreen = chatscreen;
      }
   }

   @Nullable
   public ChatScreen restoreChatScreen() {
      ChatScreen chatscreen = this.preservedScreen;
      this.preservedScreen = null;
      return chatscreen;
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

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   interface AlphaCalculator {
      ChatComponent.AlphaCalculator FULLY_VISIBLE = p_458184_ -> 1.0F;

      static ChatComponent.AlphaCalculator timeBased(int p_457730_) {
         return p_458056_ -> {
            int i = p_457730_ - p_458056_.addedTime();
            double d0 = i / 200.0;
            d0 = 1.0 - d0;
            d0 *= 10.0;
            d0 = Mth.clamp(d0, 0.0, 1.0);
            d0 *= d0;
            return (float)d0;
         };
      }

      float calculate(GuiMessage.Line var1);
   }

   @OnlyIn(Dist.CLIENT)
   public interface ChatGraphicsAccess {
      void updatePose(Consumer<Matrix3x2f> var1);

      void fill(int var1, int var2, int var3, int var4, int var5);

      boolean handleMessage(int var1, float var2, FormattedCharSequence var3);

      void handleTag(int var1, int var2, int var3, int var4, float var5, GuiMessageTag var6);

      void handleTagIcon(int var1, int var2, boolean var3, GuiMessageTag var4, GuiMessageTag.Icon var5);
   }

   @OnlyIn(Dist.CLIENT)
   public static enum ChatMethod {
      MESSAGE("") {
         @Override
         public boolean isDraftRestorable(ChatComponent.Draft p_437208_) {
            return true;
         }
      },
      COMMAND("/") {
         @Override
         public boolean isDraftRestorable(ChatComponent.Draft p_437306_) {
            return this == p_437306_.chatMethod();
         }
      };

      private final String prefix;

      private ChatMethod(String p_437288_) {
         this.prefix = p_437288_;
      }

      public String prefix() {
         return this.prefix;
      }

      public abstract boolean isDraftRestorable(ChatComponent.Draft var1);
   }

   @OnlyIn(Dist.CLIENT)
   static class ClickableTextOnlyGraphicsAccess implements ChatComponent.ChatGraphicsAccess {
      private final ActiveTextCollector output;

      public ClickableTextOnlyGraphicsAccess(ActiveTextCollector p_457632_) {
         this.output = p_457632_;
      }

      @Override
      public void updatePose(Consumer<Matrix3x2f> p_458155_) {
         ActiveTextCollector.Parameters activetextcollector$parameters = this.output.defaultParameters();
         Matrix3x2f matrix3x2f = new Matrix3x2f(activetextcollector$parameters.pose());
         p_458155_.accept(matrix3x2f);
         this.output.defaultParameters(activetextcollector$parameters.withPose(matrix3x2f));
      }

      @Override
      public void fill(int p_458287_, int p_457905_, int p_458000_, int p_457740_, int p_458112_) {
      }

      @Override
      public boolean handleMessage(int p_457906_, float p_458178_, FormattedCharSequence p_457578_) {
         this.output.accept(TextAlignment.LEFT, 0, p_457906_, p_457578_);
         return false;
      }

      @Override
      public void handleTag(int p_457827_, int p_457944_, int p_457877_, int p_458280_, float p_458186_, GuiMessageTag p_458254_) {
      }

      @Override
      public void handleTagIcon(int p_457913_, int p_457716_, boolean p_458037_, GuiMessageTag p_458041_, GuiMessageTag.Icon p_458129_) {
      }
   }

   @OnlyIn(Dist.CLIENT)
   record DelayedMessageDeletion(MessageSignature signature, int deletableAfter) {
   }

   @OnlyIn(Dist.CLIENT)
   public record Draft(String text, ChatComponent.ChatMethod chatMethod) {
   }

   @OnlyIn(Dist.CLIENT)
   static class DrawingBackgroundGraphicsAccess implements ChatComponent.ChatGraphicsAccess {
      private final GuiGraphics graphics;
      private final ActiveTextCollector textRenderer;
      private ActiveTextCollector.Parameters parameters;

      public DrawingBackgroundGraphicsAccess(GuiGraphics p_467256_) {
         this.graphics = p_467256_;
         this.textRenderer = p_467256_.textRenderer(GuiGraphics.HoveredTextEffects.NONE, null);
         this.parameters = this.textRenderer.defaultParameters();
      }

      @Override
      public void updatePose(Consumer<Matrix3x2f> p_466976_) {
         p_466976_.accept(this.graphics.pose());
         this.parameters = this.parameters.withPose(new Matrix3x2f(this.graphics.pose()));
      }

      @Override
      public void fill(int p_468326_, int p_469160_, int p_468044_, int p_469611_, int p_469579_) {
         this.graphics.fill(p_468326_, p_469160_, p_468044_, p_469611_, p_469579_);
      }

      @Override
      public boolean handleMessage(int p_467398_, float p_468512_, FormattedCharSequence p_467937_) {
         this.textRenderer.accept(TextAlignment.LEFT, 0, p_467398_, this.parameters.withOpacity(p_468512_), p_467937_);
         return false;
      }

      @Override
      public void handleTag(int p_469205_, int p_467121_, int p_469773_, int p_467576_, float p_467290_, GuiMessageTag p_469281_) {
         int i = ARGB.color(p_467290_, p_469281_.indicatorColor());
         this.graphics.fill(p_469205_, p_467121_, p_469773_, p_467576_, i);
      }

      @Override
      public void handleTagIcon(int p_468887_, int p_469864_, boolean p_467710_, GuiMessageTag p_467238_, GuiMessageTag.Icon p_469770_) {
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class DrawingFocusedGraphicsAccess implements ChatComponent.ChatGraphicsAccess, Consumer<Style> {
      private final GuiGraphics graphics;
      private final Font font;
      private final ActiveTextCollector textRenderer;
      private ActiveTextCollector.Parameters parameters;
      private final int globalMouseX;
      private final int globalMouseY;
      private final Vector2f localMousePos = new Vector2f();
      @Nullable
      private Style hoveredStyle;
      private final boolean changeCursorOnInsertions;

      public DrawingFocusedGraphicsAccess(GuiGraphics p_469057_, Font p_467247_, int p_468541_, int p_468931_, boolean p_480853_) {
         this.graphics = p_469057_;
         this.font = p_467247_;
         this.textRenderer = p_469057_.textRenderer(GuiGraphics.HoveredTextEffects.TOOLTIP_AND_CURSOR, this);
         this.globalMouseX = p_468541_;
         this.globalMouseY = p_468931_;
         this.changeCursorOnInsertions = p_480853_;
         this.parameters = this.textRenderer.defaultParameters();
         this.updateLocalMousePos();
      }

      private void updateLocalMousePos() {
         this.graphics.pose().invert(new Matrix3x2f()).transformPosition(this.globalMouseX, this.globalMouseY, this.localMousePos);
      }

      @Override
      public void updatePose(Consumer<Matrix3x2f> p_467899_) {
         p_467899_.accept(this.graphics.pose());
         this.parameters = this.parameters.withPose(new Matrix3x2f(this.graphics.pose()));
         this.updateLocalMousePos();
      }

      @Override
      public void fill(int p_467756_, int p_467968_, int p_469302_, int p_467383_, int p_468283_) {
         this.graphics.fill(p_467756_, p_467968_, p_469302_, p_467383_, p_468283_);
      }

      public void accept(Style p_466846_) {
         this.hoveredStyle = p_466846_;
      }

      @Override
      public boolean handleMessage(int p_467745_, float p_469150_, FormattedCharSequence p_467444_) {
         this.hoveredStyle = null;
         this.textRenderer.accept(TextAlignment.LEFT, 0, p_467745_, this.parameters.withOpacity(p_469150_), p_467444_);
         if (this.changeCursorOnInsertions && this.hoveredStyle != null && this.hoveredStyle.getInsertion() != null) {
            this.graphics.requestCursor(CursorTypes.POINTING_HAND);
         }

         return this.hoveredStyle != null;
      }

      private boolean isMouseOver(int p_467811_, int p_469012_, int p_468622_, int p_467234_) {
         return ActiveTextCollector.isPointInRectangle(this.localMousePos.x, this.localMousePos.y, p_467811_, p_469012_, p_468622_, p_467234_);
      }

      @Override
      public void handleTag(int p_467877_, int p_466920_, int p_468297_, int p_466977_, float p_468579_, GuiMessageTag p_468230_) {
         int i = ARGB.color(p_468579_, p_468230_.indicatorColor());
         this.graphics.fill(p_467877_, p_466920_, p_468297_, p_466977_, i);
         if (this.isMouseOver(p_467877_, p_466920_, p_468297_, p_466977_)) {
            this.showTooltip(p_468230_);
         }
      }

      @Override
      public void handleTagIcon(int p_469427_, int p_469103_, boolean p_467801_, GuiMessageTag p_467777_, GuiMessageTag.Icon p_469613_) {
         int i = p_469103_ - p_469613_.height - 1;
         int j = p_469427_ + p_469613_.width;
         boolean flag = this.isMouseOver(p_469427_, i, j, p_469103_);
         if (flag) {
            this.showTooltip(p_467777_);
         }

         if (p_467801_ || flag) {
            p_469613_.draw(this.graphics, p_469427_, i);
         }
      }

      private void showTooltip(GuiMessageTag p_467111_) {
         if (p_467111_.text() != null) {
            this.graphics.setTooltipForNextFrame(this.font, this.font.split(p_467111_.text(), 210), this.globalMouseX, this.globalMouseY);
         }
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   interface LineConsumer {
      void accept(GuiMessage.Line var1, int var2, float var3);
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
