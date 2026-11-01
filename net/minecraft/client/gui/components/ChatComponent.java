package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
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

   public ChatComponent(Minecraft minecraft) {
      this.minecraft = minecraft;
      this.recentChat.addAll(minecraft.commandHistory().history());
   }

   public void tick() {
      if (!this.messageDeletionQueue.isEmpty()) {
         this.processMessageDeletionQueue();
      }
   }

   private int forEachLine(ChatComponent.AlphaCalculator alphaCalculator, ChatComponent.LineConsumer action) {
      int i = this.getLinesPerPage();
      int j = 0;

      for (int k = Math.min(this.trimmedMessages.size() - this.chatScrollbarPos, i) - 1; k >= 0; k--) {
         int l = k + this.chatScrollbarPos;
         GuiMessage.Line line = this.trimmedMessages.get(l);
         float f = alphaCalculator.calculate(line);
         if (f > 1.0E-5F) {
            j++;
            action.accept(line, k, f);
         }
      }

      return j;
   }

   public void render(GuiGraphics graphics, Font font, int tickCount, int globalMouseX, int globalMouseY, boolean focused, boolean changeCursorOnInsertions) {
      graphics.pose().pushMatrix();
      this.render(
         (ChatComponent.ChatGraphicsAccess)(focused
            ? new ChatComponent.DrawingFocusedGraphicsAccess(graphics, font, globalMouseX, globalMouseY, changeCursorOnInsertions)
            : new ChatComponent.DrawingBackgroundGraphicsAccess(graphics)),
         graphics.guiHeight(),
         tickCount,
         focused
      );
      graphics.pose().popMatrix();
   }

   public void captureClickableText(ActiveTextCollector activeTextCollector, int height, int tickCount, boolean focused) {
      this.render(new ChatComponent.ClickableTextOnlyGraphicsAccess(activeTextCollector), height, tickCount, focused);
   }

   private void render(ChatComponent.ChatGraphicsAccess chatGraphicsAccess, int height, int tickCount, boolean focused) {
      if (!this.isChatHidden()) {
         int i = this.trimmedMessages.size();
         if (i > 0) {
            ProfilerFiller profilerFiller = Profiler.get();
            profilerFiller.push("chat");
            float f = (float)this.getScale();
            int j = Mth.ceil(this.getWidth() / f);
            final int k = Mth.floor((height - 40) / f);
            final float g = this.minecraft.options.chatOpacity().get().floatValue() * 0.9F + 0.1F;
            float h = this.minecraft.options.textBackgroundOpacity().get().floatValue();
            final int l = 9;
            int m = 8;
            double d = this.minecraft.options.chatLineSpacing().get();
            final int n = (int)(l * (d + 1.0));
            final int o = (int)Math.round(8.0 * (d + 1.0) - 4.0 * d);
            long p = this.minecraft.getChatListener().queueSize();
            ChatComponent.AlphaCalculator alphaCalculator = focused
               ? ChatComponent.AlphaCalculator.FULLY_VISIBLE
               : ChatComponent.AlphaCalculator.timeBased(tickCount);
            chatGraphicsAccess.updatePose(matrix3x2f -> {
               matrix3x2f.scale(f, f);
               matrix3x2f.translate(4.0F, 0.0F);
            });
            this.forEachLine(alphaCalculator, (line, lx, gx) -> {
               int mx = k - lx * n;
               int nx = mx - n;
               chatGraphicsAccess.fill(-4, nx, j + 4 + 4, mx, ARGB.black(gx * h));
            });
            if (p > 0L) {
               chatGraphicsAccess.fill(-2, k, j + 4, k + l, ARGB.black(h));
            }

            int q = this.forEachLine(alphaCalculator, new ChatComponent.LineConsumer() {
               boolean hoveredOverCurrentMessage;

               @Override
               public void accept(GuiMessage.Line line, int ix, float fx) {
                  int jx = k - ix * n;
                  int kx = jx - n;
                  int lx = jx - o;
                  boolean bl = chatGraphicsAccess.handleMessage(lx, fx * g, line.content());
                  this.hoveredOverCurrentMessage |= bl;
                  boolean bl2;
                  if (line.endOfEntry()) {
                     bl2 = this.hoveredOverCurrentMessage;
                     this.hoveredOverCurrentMessage = false;
                  } else {
                     bl2 = false;
                  }

                  GuiMessageTag guiMessageTag = line.tag();
                  if (guiMessageTag != null) {
                     chatGraphicsAccess.handleTag(-4, kx, -2, jx, fx * g, guiMessageTag);
                     if (guiMessageTag.icon() != null) {
                        int mx = line.getTagIconLeft(ChatComponent.this.minecraft.font);
                        int nx = lx + l;
                        chatGraphicsAccess.handleTagIcon(mx, nx, bl2, guiMessageTag, guiMessageTag.icon());
                     }
                  }
               }
            });
            if (p > 0L) {
               int r = k + l;
               Component component = Component.translatable("chat.queue", p).setStyle(QUEUE_EXPAND_TEXT_STYLE);
               chatGraphicsAccess.handleMessage(r - 8, 0.5F * g, component.getVisualOrderText());
            }

            if (focused) {
               int r = i * n;
               int s = q * n;
               int t = this.chatScrollbarPos * s / i - k;
               int u = s * s / r;
               if (r != s) {
                  int v = t > 0 ? 170 : 96;
                  int w = this.newMessageSinceScroll ? 13382451 : 3355562;
                  int x = j + 4;
                  chatGraphicsAccess.fill(x, -t, x + 2, -t - u, ARGB.color(v, w));
                  chatGraphicsAccess.fill(x + 2, -t, x + 1, -t - u, ARGB.color(v, 13421772));
               }
            }

            profilerFiller.pop();
         }
      }
   }

   private boolean isChatHidden() {
      return this.minecraft.options.chatVisibility().get() == ChatVisiblity.HIDDEN;
   }

   public void clearMessages(boolean clearSentMsgHistory) {
      this.minecraft.getChatListener().flushQueue();
      this.messageDeletionQueue.clear();
      this.trimmedMessages.clear();
      this.allMessages.clear();
      if (clearSentMsgHistory) {
         this.recentChat.clear();
         this.recentChat.addAll(this.minecraft.commandHistory().history());
      }
   }

   public void addMessage(Component chatComponent) {
      this.addMessage(chatComponent, null, this.minecraft.isSingleplayer() ? GuiMessageTag.systemSinglePlayer() : GuiMessageTag.system());
   }

   public void addMessage(Component chatComponent, @Nullable MessageSignature headerSignature, @Nullable GuiMessageTag tag) {
      GuiMessage guiMessage = new GuiMessage(this.minecraft.gui.getGuiTicks(), chatComponent, headerSignature, tag);
      this.logChatMessage(guiMessage);
      this.addMessageToDisplayQueue(guiMessage);
      this.addMessageToQueue(guiMessage);
   }

   private void logChatMessage(GuiMessage message) {
      String string = message.content().getString().replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n");
      String string2 = Optionull.map(message.tag(), GuiMessageTag::logTag);
      if (string2 != null) {
         LOGGER.info("[{}] [CHAT] {}", string2, string);
      } else {
         LOGGER.info("[CHAT] {}", string);
      }
   }

   private void addMessageToDisplayQueue(GuiMessage message) {
      int i = Mth.floor(this.getWidth() / this.getScale());
      List<FormattedCharSequence> list = message.splitLines(this.minecraft.font, i);
      boolean bl = this.isChatFocused();

      for (int j = 0; j < list.size(); j++) {
         FormattedCharSequence formattedCharSequence = list.get(j);
         if (bl && this.chatScrollbarPos > 0) {
            this.newMessageSinceScroll = true;
            this.scrollChat(1);
         }

         boolean bl2 = j == list.size() - 1;
         this.trimmedMessages.addFirst(new GuiMessage.Line(message.addedTime(), formattedCharSequence, message.tag(), bl2));
      }

      while (this.trimmedMessages.size() > 100) {
         this.trimmedMessages.removeLast();
      }
   }

   private void addMessageToQueue(GuiMessage message) {
      this.allMessages.addFirst(message);

      while (this.allMessages.size() > 100) {
         this.allMessages.removeLast();
      }
   }

   private void processMessageDeletionQueue() {
      int i = this.minecraft.gui.getGuiTicks();
      this.messageDeletionQueue
         .removeIf(
            delayedMessageDeletion -> i >= delayedMessageDeletion.deletableAfter()
               ? this.deleteMessageOrDelay(delayedMessageDeletion.signature()) == null
               : false
         );
   }

   public void deleteMessage(MessageSignature messageSignature) {
      ChatComponent.DelayedMessageDeletion delayedMessageDeletion = this.deleteMessageOrDelay(messageSignature);
      if (delayedMessageDeletion != null) {
         this.messageDeletionQueue.add(delayedMessageDeletion);
      }
   }

   @Nullable
   private ChatComponent.DelayedMessageDeletion deleteMessageOrDelay(MessageSignature messageSignature) {
      int i = this.minecraft.gui.getGuiTicks();
      ListIterator<GuiMessage> listIterator = this.allMessages.listIterator();

      while (listIterator.hasNext()) {
         GuiMessage guiMessage = listIterator.next();
         if (messageSignature.equals(guiMessage.signature())) {
            int j = guiMessage.addedTime() + 60;
            if (i >= j) {
               listIterator.set(this.createDeletedMarker(guiMessage));
               this.refreshTrimmedMessages();
               return null;
            }

            return new ChatComponent.DelayedMessageDeletion(messageSignature, j);
         }
      }

      return null;
   }

   private GuiMessage createDeletedMarker(GuiMessage message) {
      return new GuiMessage(message.addedTime(), DELETED_CHAT_MESSAGE, null, GuiMessageTag.system());
   }

   public void rescaleChat() {
      this.resetChatScroll();
      this.refreshTrimmedMessages();
   }

   private void refreshTrimmedMessages() {
      this.trimmedMessages.clear();

      for (GuiMessage guiMessage : Lists.reverse(this.allMessages)) {
         this.addMessageToDisplayQueue(guiMessage);
      }
   }

   public ArrayListDeque<String> getRecentChat() {
      return this.recentChat;
   }

   public void addRecentChat(String message) {
      if (!message.equals(this.recentChat.peekLast())) {
         if (this.recentChat.size() >= 100) {
            this.recentChat.removeFirst();
         }

         this.recentChat.addLast(message);
      }

      if (message.startsWith("/")) {
         this.minecraft.commandHistory().addCommand(message);
      }
   }

   public void resetChatScroll() {
      this.chatScrollbarPos = 0;
      this.newMessageSinceScroll = false;
   }

   public void scrollChat(int posInc) {
      this.chatScrollbarPos += posInc;
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

   private double getScale() {
      return this.minecraft.options.chatScale().get();
   }

   public static int getWidth(double width) {
      int i = 320;
      int j = 40;
      return Mth.floor(width * 280.0 + 40.0);
   }

   public static int getHeight(double height) {
      int i = 180;
      int j = 20;
      return Mth.floor(height * 160.0 + 20.0);
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

   public void saveAsDraft(String text) {
      boolean bl = text.startsWith("/");
      this.latestDraft = new ChatComponent.Draft(text, bl ? ChatComponent.ChatMethod.COMMAND : ChatComponent.ChatMethod.MESSAGE);
   }

   public void discardDraft() {
      this.latestDraft = null;
   }

   public <T extends ChatScreen> T createScreen(ChatComponent.ChatMethod method, ChatScreen.ChatConstructor<T> constructor) {
      return this.latestDraft != null && method.isDraftRestorable(this.latestDraft)
         ? constructor.create(this.latestDraft.text(), true)
         : constructor.create(method.prefix(), false);
   }

   public void openScreen(ChatComponent.ChatMethod method, ChatScreen.ChatConstructor<?> constructor) {
      this.minecraft.setScreen(this.createScreen(method, (ChatScreen.ChatConstructor<Screen>)constructor));
   }

   public void preserveCurrentChatScreen() {
      if (this.minecraft.screen instanceof ChatScreen chatScreen) {
         this.preservedScreen = chatScreen;
      }
   }

   @Nullable
   public ChatScreen restoreChatScreen() {
      ChatScreen chatScreen = this.preservedScreen;
      this.preservedScreen = null;
      return chatScreen;
   }

   public ChatComponent.State storeState() {
      return new ChatComponent.State(List.copyOf(this.allMessages), List.copyOf(this.recentChat), List.copyOf(this.messageDeletionQueue));
   }

   public void restoreState(ChatComponent.State state) {
      this.recentChat.clear();
      this.recentChat.addAll(state.history);
      this.messageDeletionQueue.clear();
      this.messageDeletionQueue.addAll(state.delayedMessageDeletions);
      this.allMessages.clear();
      this.allMessages.addAll(state.messages);
      this.refreshTrimmedMessages();
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   interface AlphaCalculator {
      ChatComponent.AlphaCalculator FULLY_VISIBLE = line -> 1.0F;

      static ChatComponent.AlphaCalculator timeBased(int i) {
         return line -> {
            int j = i - line.addedTime();
            double d = j / 200.0;
            d = 1.0 - d;
            d *= 10.0;
            d = Mth.clamp(d, 0.0, 1.0);
            d *= d;
            return (float)d;
         };
      }

      float calculate(GuiMessage.Line line);
   }

   @Environment(EnvType.CLIENT)
   public interface ChatGraphicsAccess {
      void updatePose(Consumer<Matrix3x2f> updater);

      void fill(int minX, int minY, int maxX, int maxY, int color);

      boolean handleMessage(int y, float opacity, FormattedCharSequence text);

      void handleTag(int minX, int minY, int maxX, int maxY, float opacity, GuiMessageTag tag);

      void handleTagIcon(int x, int y, boolean drawIcon, GuiMessageTag tag, GuiMessageTag.Icon icon);
   }

   @Environment(EnvType.CLIENT)
   public static enum ChatMethod {
      MESSAGE("") {
         @Override
         public boolean isDraftRestorable(ChatComponent.Draft draft) {
            return true;
         }
      },
      COMMAND("/") {
         @Override
         public boolean isDraftRestorable(ChatComponent.Draft draft) {
            return this == draft.chatMethod;
         }
      };

      private final String prefix;

      ChatMethod(final String prefix) {
         this.prefix = prefix;
      }

      public String prefix() {
         return this.prefix;
      }

      public abstract boolean isDraftRestorable(ChatComponent.Draft draft);
   }

   @Environment(EnvType.CLIENT)
   static class ClickableTextOnlyGraphicsAccess implements ChatComponent.ChatGraphicsAccess {
      private final ActiveTextCollector output;

      public ClickableTextOnlyGraphicsAccess(ActiveTextCollector output) {
         this.output = output;
      }

      @Override
      public void updatePose(Consumer<Matrix3x2f> updater) {
         ActiveTextCollector.Parameters parameters = this.output.defaultParameters();
         Matrix3x2f matrix3x2f = new Matrix3x2f(parameters.pose());
         updater.accept(matrix3x2f);
         this.output.defaultParameters(parameters.withPose(matrix3x2f));
      }

      @Override
      public void fill(int minX, int minY, int maxX, int maxY, int color) {
      }

      @Override
      public boolean handleMessage(int y, float opacity, FormattedCharSequence text) {
         this.output.accept(TextAlignment.LEFT, 0, y, text);
         return false;
      }

      @Override
      public void handleTag(int minX, int minY, int maxX, int maxY, float opacity, GuiMessageTag tag) {
      }

      @Override
      public void handleTagIcon(int x, int y, boolean drawIcon, GuiMessageTag tag, GuiMessageTag.Icon icon) {
      }
   }

   @Environment(EnvType.CLIENT)
   record DelayedMessageDeletion(MessageSignature signature, int deletableAfter) {
   }

   @Environment(EnvType.CLIENT)
   public record Draft(String text, ChatComponent.ChatMethod chatMethod) {
   }

   @Environment(EnvType.CLIENT)
   static class DrawingBackgroundGraphicsAccess implements ChatComponent.ChatGraphicsAccess {
      private final GuiGraphics graphics;
      private final ActiveTextCollector textRenderer;
      private ActiveTextCollector.Parameters parameters;

      public DrawingBackgroundGraphicsAccess(GuiGraphics graphics) {
         this.graphics = graphics;
         this.textRenderer = graphics.textRenderer(GuiGraphics.HoveredTextEffects.NONE, null);
         this.parameters = this.textRenderer.defaultParameters();
      }

      @Override
      public void updatePose(Consumer<Matrix3x2f> updater) {
         updater.accept(this.graphics.pose());
         this.parameters = this.parameters.withPose(new Matrix3x2f(this.graphics.pose()));
      }

      @Override
      public void fill(int minX, int minY, int maxX, int maxY, int color) {
         this.graphics.fill(minX, minY, maxX, maxY, color);
      }

      @Override
      public boolean handleMessage(int y, float opacity, FormattedCharSequence text) {
         this.textRenderer.accept(TextAlignment.LEFT, 0, y, this.parameters.withOpacity(opacity), text);
         return false;
      }

      @Override
      public void handleTag(int minX, int minY, int maxX, int maxY, float opacity, GuiMessageTag tag) {
         int i = ARGB.color(opacity, tag.indicatorColor());
         this.graphics.fill(minX, minY, maxX, maxY, i);
      }

      @Override
      public void handleTagIcon(int x, int y, boolean drawIcon, GuiMessageTag tag, GuiMessageTag.Icon icon) {
      }
   }

   @Environment(EnvType.CLIENT)
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

      public DrawingFocusedGraphicsAccess(GuiGraphics graphics, Font font, int globalMouseX, int globalMouseY, boolean changeCursorOnInsertions) {
         this.graphics = graphics;
         this.font = font;
         this.textRenderer = graphics.textRenderer(GuiGraphics.HoveredTextEffects.TOOLTIP_AND_CURSOR, this);
         this.globalMouseX = globalMouseX;
         this.globalMouseY = globalMouseY;
         this.changeCursorOnInsertions = changeCursorOnInsertions;
         this.parameters = this.textRenderer.defaultParameters();
         this.updateLocalMousePos();
      }

      private void updateLocalMousePos() {
         this.graphics.pose().invert(new Matrix3x2f()).transformPosition(this.globalMouseX, this.globalMouseY, this.localMousePos);
      }

      @Override
      public void updatePose(Consumer<Matrix3x2f> updater) {
         updater.accept(this.graphics.pose());
         this.parameters = this.parameters.withPose(new Matrix3x2f(this.graphics.pose()));
         this.updateLocalMousePos();
      }

      @Override
      public void fill(int minX, int minY, int maxX, int maxY, int color) {
         this.graphics.fill(minX, minY, maxX, maxY, color);
      }

      public void accept(Style style) {
         this.hoveredStyle = style;
      }

      @Override
      public boolean handleMessage(int y, float opacity, FormattedCharSequence text) {
         this.hoveredStyle = null;
         this.textRenderer.accept(TextAlignment.LEFT, 0, y, this.parameters.withOpacity(opacity), text);
         if (this.changeCursorOnInsertions && this.hoveredStyle != null && this.hoveredStyle.getInsertion() != null) {
            this.graphics.requestCursor(CursorTypes.POINTING_HAND);
         }

         return this.hoveredStyle != null;
      }

      private boolean isMouseOver(int minX, int minY, int maxX, int maxY) {
         return ActiveTextCollector.isPointInRectangle(this.localMousePos.x, this.localMousePos.y, minX, minY, maxX, maxY);
      }

      @Override
      public void handleTag(int minX, int minY, int maxX, int maxY, float opacity, GuiMessageTag tag) {
         int i = ARGB.color(opacity, tag.indicatorColor());
         this.graphics.fill(minX, minY, maxX, maxY, i);
         if (this.isMouseOver(minX, minY, maxX, maxY)) {
            this.showTooltip(tag);
         }
      }

      @Override
      public void handleTagIcon(int x, int y, boolean drawIcon, GuiMessageTag tag, GuiMessageTag.Icon icon) {
         int i = y - icon.height - 1;
         int j = x + icon.width;
         boolean bl = this.isMouseOver(x, i, j, y);
         if (bl) {
            this.showTooltip(tag);
         }

         if (drawIcon || bl) {
            icon.draw(this.graphics, x, i);
         }
      }

      private void showTooltip(GuiMessageTag tag) {
         if (tag.text() != null) {
            this.graphics.setTooltipForNextFrame(this.font, this.font.split(tag.text(), 210), this.globalMouseX, this.globalMouseY);
         }
      }
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   interface LineConsumer {
      void accept(GuiMessage.Line line, int i, float f);
   }

   @Environment(EnvType.CLIENT)
   public static class State {
      final List<GuiMessage> messages;
      final List<String> history;
      final List<ChatComponent.DelayedMessageDeletion> delayedMessageDeletions;

      public State(List<GuiMessage> messages, List<String> history, List<ChatComponent.DelayedMessageDeletion> delayedMessageDeletions) {
         this.messages = messages;
         this.history = history;
         this.delayedMessageDeletions = delayedMessageDeletions;
      }
   }
}
