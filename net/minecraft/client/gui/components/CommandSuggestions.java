package net.minecraft.client.gui.components;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.SuggestionContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CommandSuggestions {
   private static final Pattern WHITESPACE_PATTERN = Pattern.compile("(\\s+)");
   private static final Style UNPARSED_STYLE = Style.EMPTY.withColor(ChatFormatting.RED);
   private static final Style LITERAL_STYLE = Style.EMPTY.withColor(ChatFormatting.GRAY);
   private static final List<Style> ARGUMENT_STYLES = Stream.of(
         ChatFormatting.AQUA, ChatFormatting.YELLOW, ChatFormatting.GREEN, ChatFormatting.LIGHT_PURPLE, ChatFormatting.GOLD
      )
      .map(Style.EMPTY::withColor)
      .collect(ImmutableList.toImmutableList());
   final Minecraft minecraft;
   private final Screen screen;
   final EditBox input;
   final Font font;
   private final boolean commandsOnly;
   private final boolean onlyShowIfCursorPastError;
   final int lineStartOffset;
   final int suggestionLineLimit;
   final boolean anchorToBottom;
   final int fillColor;
   private final List<FormattedCharSequence> commandUsage = Lists.newArrayList();
   private int commandUsagePosition;
   private int commandUsageWidth;
   @Nullable
   private ParseResults<SharedSuggestionProvider> currentParse;
   @Nullable
   private CompletableFuture<Suggestions> pendingSuggestions;
   @Nullable
   private CommandSuggestions.SuggestionsList suggestions;
   private boolean allowSuggestions;
   boolean keepSuggestions;
   private boolean allowHiding = true;

   public CommandSuggestions(
      Minecraft p_93871_,
      Screen p_93872_,
      EditBox p_93873_,
      Font p_93874_,
      boolean p_93875_,
      boolean p_93876_,
      int p_93877_,
      int p_93878_,
      boolean p_93879_,
      int p_93880_
   ) {
      this.minecraft = p_93871_;
      this.screen = p_93872_;
      this.input = p_93873_;
      this.font = p_93874_;
      this.commandsOnly = p_93875_;
      this.onlyShowIfCursorPastError = p_93876_;
      this.lineStartOffset = p_93877_;
      this.suggestionLineLimit = p_93878_;
      this.anchorToBottom = p_93879_;
      this.fillColor = p_93880_;
      p_93873_.setFormatter(this::formatChat);
   }

   public void setAllowSuggestions(boolean p_93923_) {
      this.allowSuggestions = p_93923_;
      if (!p_93923_) {
         this.suggestions = null;
      }
   }

   public void setAllowHiding(boolean p_302228_) {
      this.allowHiding = p_302228_;
   }

   public boolean keyPressed(int p_93889_, int p_93890_, int p_93891_) {
      boolean $$3 = this.suggestions != null;
      if ($$3 && this.suggestions.keyPressed(p_93889_, p_93890_, p_93891_)) {
         return true;
      } else if (this.screen.getFocused() != this.input || p_93889_ != 258 || this.allowHiding && !$$3) {
         return false;
      } else {
         this.showSuggestions(true);
         return true;
      }
   }

   public boolean mouseScrolled(double p_93883_) {
      return this.suggestions != null && this.suggestions.mouseScrolled(Mth.clamp(p_93883_, -1.0, 1.0));
   }

   public boolean mouseClicked(double p_93885_, double p_93886_, int p_93887_) {
      return this.suggestions != null && this.suggestions.mouseClicked((int)p_93885_, (int)p_93886_, p_93887_);
   }

   public void showSuggestions(boolean p_93931_) {
      if (this.pendingSuggestions != null && this.pendingSuggestions.isDone()) {
         Suggestions $$1 = this.pendingSuggestions.join();
         if (!$$1.isEmpty()) {
            int $$2 = 0;

            for (Suggestion $$3 : $$1.getList()) {
               $$2 = Math.max($$2, this.font.width($$3.getText()));
            }

            int $$4 = Mth.clamp(this.input.getScreenX($$1.getRange().getStart()), 0, this.input.getScreenX(0) + this.input.getInnerWidth() - $$2);
            int $$5 = this.anchorToBottom ? this.screen.height - 12 : 72;
            this.suggestions = new CommandSuggestions.SuggestionsList($$4, $$5, $$2, this.sortSuggestions($$1), p_93931_);
         }
      }
   }

   public boolean isVisible() {
      return this.suggestions != null;
   }

   public Component getUsageNarration() {
      if (this.suggestions != null && this.suggestions.tabCycles) {
         return this.allowHiding
            ? Component.translatable("narration.suggestion.usage.cycle.hidable")
            : Component.translatable("narration.suggestion.usage.cycle.fixed");
      } else {
         return this.allowHiding
            ? Component.translatable("narration.suggestion.usage.fill.hidable")
            : Component.translatable("narration.suggestion.usage.fill.fixed");
      }
   }

   public void hide() {
      this.suggestions = null;
   }

   private List<Suggestion> sortSuggestions(Suggestions p_93899_) {
      String $$1 = this.input.getValue().substring(0, this.input.getCursorPosition());
      int $$2 = getLastWordIndex($$1);
      String $$3 = $$1.substring($$2).toLowerCase(Locale.ROOT);
      List<Suggestion> $$4 = Lists.newArrayList();
      List<Suggestion> $$5 = Lists.newArrayList();

      for (Suggestion $$6 : p_93899_.getList()) {
         if (!$$6.getText().startsWith($$3) && !$$6.getText().startsWith("minecraft:" + $$3)) {
            $$5.add($$6);
         } else {
            $$4.add($$6);
         }
      }

      $$4.addAll($$5);
      return $$4;
   }

   public void updateCommandInfo() {
      String $$0 = this.input.getValue();
      if (this.currentParse != null && !this.currentParse.getReader().getString().equals($$0)) {
         this.currentParse = null;
      }

      if (!this.keepSuggestions) {
         this.input.setSuggestion(null);
         this.suggestions = null;
      }

      this.commandUsage.clear();
      StringReader $$1 = new StringReader($$0);
      boolean $$2 = $$1.canRead() && $$1.peek() == '/';
      if ($$2) {
         $$1.skip();
      }

      boolean $$3 = this.commandsOnly || $$2;
      int $$4 = this.input.getCursorPosition();
      if ($$3) {
         CommandDispatcher<SharedSuggestionProvider> $$5 = this.minecraft.player.connection.getCommands();
         if (this.currentParse == null) {
            this.currentParse = $$5.parse($$1, this.minecraft.player.connection.getSuggestionsProvider());
         }

         int $$6 = this.onlyShowIfCursorPastError ? $$1.getCursor() : 1;
         if ($$4 >= $$6 && (this.suggestions == null || !this.keepSuggestions)) {
            this.pendingSuggestions = $$5.getCompletionSuggestions(this.currentParse, $$4);
            this.pendingSuggestions.thenRun(() -> {
               if (this.pendingSuggestions.isDone()) {
                  this.updateUsageInfo();
               }
            });
         }
      } else {
         String $$7 = $$0.substring(0, $$4);
         int $$8 = getLastWordIndex($$7);
         Collection<String> $$9 = this.minecraft.player.connection.getSuggestionsProvider().getCustomTabSugggestions();
         this.pendingSuggestions = SharedSuggestionProvider.suggest($$9, new SuggestionsBuilder($$7, $$8));
      }
   }

   private static int getLastWordIndex(String p_93913_) {
      if (Strings.isNullOrEmpty(p_93913_)) {
         return 0;
      } else {
         int $$1 = 0;
         Matcher $$2 = WHITESPACE_PATTERN.matcher(p_93913_);

         while ($$2.find()) {
            $$1 = $$2.end();
         }

         return $$1;
      }
   }

   private static FormattedCharSequence getExceptionMessage(CommandSyntaxException p_93897_) {
      Component $$1 = ComponentUtils.fromMessage(p_93897_.getRawMessage());
      String $$2 = p_93897_.getContext();
      return $$2 == null
         ? $$1.getVisualOrderText()
         : Component.translatable("command.context.parse_error", $$1, p_93897_.getCursor(), $$2).getVisualOrderText();
   }

   private void updateUsageInfo() {
      boolean $$0 = false;
      if (this.input.getCursorPosition() == this.input.getValue().length()) {
         if (this.pendingSuggestions.join().isEmpty() && !this.currentParse.getExceptions().isEmpty()) {
            int $$1 = 0;

            for (Entry<CommandNode<SharedSuggestionProvider>, CommandSyntaxException> $$2 : this.currentParse.getExceptions().entrySet()) {
               CommandSyntaxException $$3 = $$2.getValue();
               if ($$3.getType() == CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect()) {
                  $$1++;
               } else {
                  this.commandUsage.add(getExceptionMessage($$3));
               }
            }

            if ($$1 > 0) {
               this.commandUsage.add(getExceptionMessage(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create()));
            }
         } else if (this.currentParse.getReader().canRead()) {
            $$0 = true;
         }
      }

      this.commandUsagePosition = 0;
      this.commandUsageWidth = this.screen.width;
      if (this.commandUsage.isEmpty() && !this.fillNodeUsage(ChatFormatting.GRAY) && $$0) {
         this.commandUsage.add(getExceptionMessage(Commands.getParseException(this.currentParse)));
      }

      this.suggestions = null;
      if (this.allowSuggestions && this.minecraft.options.autoSuggestions().get()) {
         this.showSuggestions(false);
      }
   }

   private boolean fillNodeUsage(ChatFormatting p_289002_) {
      CommandContextBuilder<SharedSuggestionProvider> $$1 = this.currentParse.getContext();
      SuggestionContext<SharedSuggestionProvider> $$2 = $$1.findSuggestionContext(this.input.getCursorPosition());
      Map<CommandNode<SharedSuggestionProvider>, String> $$3 = this.minecraft
         .player
         .connection
         .getCommands()
         .getSmartUsage($$2.parent, this.minecraft.player.connection.getSuggestionsProvider());
      List<FormattedCharSequence> $$4 = Lists.newArrayList();
      int $$5 = 0;
      Style $$6 = Style.EMPTY.withColor(p_289002_);

      for (Entry<CommandNode<SharedSuggestionProvider>, String> $$7 : $$3.entrySet()) {
         if (!($$7.getKey() instanceof LiteralCommandNode)) {
            $$4.add(FormattedCharSequence.forward($$7.getValue(), $$6));
            $$5 = Math.max($$5, this.font.width($$7.getValue()));
         }
      }

      if (!$$4.isEmpty()) {
         this.commandUsage.addAll($$4);
         this.commandUsagePosition = Mth.clamp(this.input.getScreenX($$2.startPos), 0, this.input.getScreenX(0) + this.input.getInnerWidth() - $$5);
         this.commandUsageWidth = $$5;
         return true;
      } else {
         return false;
      }
   }

   private FormattedCharSequence formatChat(String p_93915_, int p_93916_) {
      return this.currentParse != null ? formatText(this.currentParse, p_93915_, p_93916_) : FormattedCharSequence.forward(p_93915_, Style.EMPTY);
   }

   @Nullable
   static String calculateSuggestionSuffix(String p_93928_, String p_93929_) {
      return p_93929_.startsWith(p_93928_) ? p_93929_.substring(p_93928_.length()) : null;
   }

   private static FormattedCharSequence formatText(ParseResults<SharedSuggestionProvider> p_93893_, String p_93894_, int p_93895_) {
      List<FormattedCharSequence> $$3 = Lists.newArrayList();
      int $$4 = 0;
      int $$5 = -1;
      CommandContextBuilder<SharedSuggestionProvider> $$6 = p_93893_.getContext().getLastChild();

      for (ParsedArgument<SharedSuggestionProvider, ?> $$7 : $$6.getArguments().values()) {
         if (++$$5 >= ARGUMENT_STYLES.size()) {
            $$5 = 0;
         }

         int $$8 = Math.max($$7.getRange().getStart() - p_93895_, 0);
         if ($$8 >= p_93894_.length()) {
            break;
         }

         int $$9 = Math.min($$7.getRange().getEnd() - p_93895_, p_93894_.length());
         if ($$9 > 0) {
            $$3.add(FormattedCharSequence.forward(p_93894_.substring($$4, $$8), LITERAL_STYLE));
            $$3.add(FormattedCharSequence.forward(p_93894_.substring($$8, $$9), ARGUMENT_STYLES.get($$5)));
            $$4 = $$9;
         }
      }

      if (p_93893_.getReader().canRead()) {
         int $$10 = Math.max(p_93893_.getReader().getCursor() - p_93895_, 0);
         if ($$10 < p_93894_.length()) {
            int $$11 = Math.min($$10 + p_93893_.getReader().getRemainingLength(), p_93894_.length());
            $$3.add(FormattedCharSequence.forward(p_93894_.substring($$4, $$10), LITERAL_STYLE));
            $$3.add(FormattedCharSequence.forward(p_93894_.substring($$10, $$11), UNPARSED_STYLE));
            $$4 = $$11;
         }
      }

      $$3.add(FormattedCharSequence.forward(p_93894_.substring($$4), LITERAL_STYLE));
      return FormattedCharSequence.composite($$3);
   }

   public void render(GuiGraphics p_282650_, int p_282266_, int p_281963_) {
      if (!this.renderSuggestions(p_282650_, p_282266_, p_281963_)) {
         this.renderUsage(p_282650_);
      }
   }

   public boolean renderSuggestions(GuiGraphics p_283503_, int p_281628_, int p_282260_) {
      if (this.suggestions != null) {
         this.suggestions.render(p_283503_, p_281628_, p_282260_);
         return true;
      } else {
         return false;
      }
   }

   public void renderUsage(GuiGraphics p_282763_) {
      int $$1 = 0;

      for (FormattedCharSequence $$2 : this.commandUsage) {
         int $$3 = this.anchorToBottom ? this.screen.height - 14 - 13 - 12 * $$1 : 72 + 12 * $$1;
         p_282763_.fill(this.commandUsagePosition - 1, $$3, this.commandUsagePosition + this.commandUsageWidth + 1, $$3 + 12, this.fillColor);
         p_282763_.drawString(this.font, $$2, this.commandUsagePosition, $$3 + 2, -1);
         $$1++;
      }
   }

   public Component getNarrationMessage() {
      return (Component)(this.suggestions != null ? CommonComponents.NEW_LINE.copy().append(this.suggestions.getNarrationMessage()) : CommonComponents.EMPTY);
   }

   @OnlyIn(Dist.CLIENT)
   public class SuggestionsList {
      private final Rect2i rect;
      private final String originalContents;
      private final List<Suggestion> suggestionList;
      private int offset;
      private int current;
      private Vec2 lastMouse = Vec2.ZERO;
      boolean tabCycles;
      private int lastNarratedEntry;

      SuggestionsList(int p_93957_, int p_93958_, int p_93959_, List<Suggestion> p_93960_, boolean p_93961_) {
         int $$6 = p_93957_ - (CommandSuggestions.this.input.isBordered() ? 0 : 1);
         int $$7 = CommandSuggestions.this.anchorToBottom
            ? p_93958_ - 3 - Math.min(p_93960_.size(), CommandSuggestions.this.suggestionLineLimit) * 12
            : p_93958_ - (CommandSuggestions.this.input.isBordered() ? 1 : 0);
         this.rect = new Rect2i($$6, $$7, p_93959_ + 1, Math.min(p_93960_.size(), CommandSuggestions.this.suggestionLineLimit) * 12);
         this.originalContents = CommandSuggestions.this.input.getValue();
         this.lastNarratedEntry = p_93961_ ? -1 : 0;
         this.suggestionList = p_93960_;
         this.select(0);
      }

      public void render(GuiGraphics p_282264_, int p_283591_, int p_283236_) {
         int $$3 = Math.min(this.suggestionList.size(), CommandSuggestions.this.suggestionLineLimit);
         int $$4 = -5592406;
         boolean $$5 = this.offset > 0;
         boolean $$6 = this.suggestionList.size() > this.offset + $$3;
         boolean $$7 = $$5 || $$6;
         boolean $$8 = this.lastMouse.x != p_283591_ || this.lastMouse.y != p_283236_;
         if ($$8) {
            this.lastMouse = new Vec2(p_283591_, p_283236_);
         }

         if ($$7) {
            p_282264_.fill(this.rect.getX(), this.rect.getY() - 1, this.rect.getX() + this.rect.getWidth(), this.rect.getY(), CommandSuggestions.this.fillColor);
            p_282264_.fill(
               this.rect.getX(),
               this.rect.getY() + this.rect.getHeight(),
               this.rect.getX() + this.rect.getWidth(),
               this.rect.getY() + this.rect.getHeight() + 1,
               CommandSuggestions.this.fillColor
            );
            if ($$5) {
               for (int $$9 = 0; $$9 < this.rect.getWidth(); $$9++) {
                  if ($$9 % 2 == 0) {
                     p_282264_.fill(this.rect.getX() + $$9, this.rect.getY() - 1, this.rect.getX() + $$9 + 1, this.rect.getY(), -1);
                  }
               }
            }

            if ($$6) {
               for (int $$10 = 0; $$10 < this.rect.getWidth(); $$10++) {
                  if ($$10 % 2 == 0) {
                     p_282264_.fill(
                        this.rect.getX() + $$10,
                        this.rect.getY() + this.rect.getHeight(),
                        this.rect.getX() + $$10 + 1,
                        this.rect.getY() + this.rect.getHeight() + 1,
                        -1
                     );
                  }
               }
            }
         }

         boolean $$11 = false;

         for (int $$12 = 0; $$12 < $$3; $$12++) {
            Suggestion $$13 = this.suggestionList.get($$12 + this.offset);
            p_282264_.fill(
               this.rect.getX(),
               this.rect.getY() + 12 * $$12,
               this.rect.getX() + this.rect.getWidth(),
               this.rect.getY() + 12 * $$12 + 12,
               CommandSuggestions.this.fillColor
            );
            if (p_283591_ > this.rect.getX()
               && p_283591_ < this.rect.getX() + this.rect.getWidth()
               && p_283236_ > this.rect.getY() + 12 * $$12
               && p_283236_ < this.rect.getY() + 12 * $$12 + 12) {
               if ($$8) {
                  this.select($$12 + this.offset);
               }

               $$11 = true;
            }

            p_282264_.drawString(
               CommandSuggestions.this.font,
               $$13.getText(),
               this.rect.getX() + 1,
               this.rect.getY() + 2 + 12 * $$12,
               $$12 + this.offset == this.current ? -256 : -5592406
            );
         }

         if ($$11) {
            Message $$14 = this.suggestionList.get(this.current).getTooltip();
            if ($$14 != null) {
               p_282264_.renderTooltip(CommandSuggestions.this.font, ComponentUtils.fromMessage($$14), p_283591_, p_283236_);
            }
         }
      }

      public boolean mouseClicked(int p_93976_, int p_93977_, int p_93978_) {
         if (!this.rect.contains(p_93976_, p_93977_)) {
            return false;
         } else {
            int $$3 = (p_93977_ - this.rect.getY()) / 12 + this.offset;
            if ($$3 >= 0 && $$3 < this.suggestionList.size()) {
               this.select($$3);
               this.useSuggestion();
            }

            return true;
         }
      }

      public boolean mouseScrolled(double p_93972_) {
         int $$1 = (int)(
            CommandSuggestions.this.minecraft.mouseHandler.xpos()
               * CommandSuggestions.this.minecraft.getWindow().getGuiScaledWidth()
               / CommandSuggestions.this.minecraft.getWindow().getScreenWidth()
         );
         int $$2 = (int)(
            CommandSuggestions.this.minecraft.mouseHandler.ypos()
               * CommandSuggestions.this.minecraft.getWindow().getGuiScaledHeight()
               / CommandSuggestions.this.minecraft.getWindow().getScreenHeight()
         );
         if (this.rect.contains($$1, $$2)) {
            this.offset = Mth.clamp((int)(this.offset - p_93972_), 0, Math.max(this.suggestionList.size() - CommandSuggestions.this.suggestionLineLimit, 0));
            return true;
         } else {
            return false;
         }
      }

      public boolean keyPressed(int p_93989_, int p_93990_, int p_93991_) {
         if (p_93989_ == 265) {
            this.cycle(-1);
            this.tabCycles = false;
            return true;
         } else if (p_93989_ == 264) {
            this.cycle(1);
            this.tabCycles = false;
            return true;
         } else if (p_93989_ == 258) {
            if (this.tabCycles) {
               this.cycle(Screen.hasShiftDown() ? -1 : 1);
            }

            this.useSuggestion();
            return true;
         } else if (p_93989_ == 256) {
            CommandSuggestions.this.hide();
            CommandSuggestions.this.input.setSuggestion(null);
            return true;
         } else {
            return false;
         }
      }

      public void cycle(int p_93974_) {
         this.select(this.current + p_93974_);
         int $$1 = this.offset;
         int $$2 = this.offset + CommandSuggestions.this.suggestionLineLimit - 1;
         if (this.current < $$1) {
            this.offset = Mth.clamp(this.current, 0, Math.max(this.suggestionList.size() - CommandSuggestions.this.suggestionLineLimit, 0));
         } else if (this.current > $$2) {
            this.offset = Mth.clamp(
               this.current + CommandSuggestions.this.lineStartOffset - CommandSuggestions.this.suggestionLineLimit,
               0,
               Math.max(this.suggestionList.size() - CommandSuggestions.this.suggestionLineLimit, 0)
            );
         }
      }

      public void select(int p_93987_) {
         this.current = p_93987_;
         if (this.current < 0) {
            this.current = this.current + this.suggestionList.size();
         }

         if (this.current >= this.suggestionList.size()) {
            this.current = this.current - this.suggestionList.size();
         }

         Suggestion $$1 = this.suggestionList.get(this.current);
         CommandSuggestions.this.input
            .setSuggestion(CommandSuggestions.calculateSuggestionSuffix(CommandSuggestions.this.input.getValue(), $$1.apply(this.originalContents)));
         if (this.lastNarratedEntry != this.current) {
            CommandSuggestions.this.minecraft.getNarrator().sayNow(this.getNarrationMessage());
         }
      }

      public void useSuggestion() {
         Suggestion $$0 = this.suggestionList.get(this.current);
         CommandSuggestions.this.keepSuggestions = true;
         CommandSuggestions.this.input.setValue($$0.apply(this.originalContents));
         int $$1 = $$0.getRange().getStart() + $$0.getText().length();
         CommandSuggestions.this.input.setCursorPosition($$1);
         CommandSuggestions.this.input.setHighlightPos($$1);
         this.select(this.current);
         CommandSuggestions.this.keepSuggestions = false;
         this.tabCycles = true;
      }

      Component getNarrationMessage() {
         this.lastNarratedEntry = this.current;
         Suggestion $$0 = this.suggestionList.get(this.current);
         Message $$1 = $$0.getTooltip();
         return $$1 != null
            ? Component.translatable("narration.suggestion.tooltip", this.current + 1, this.suggestionList.size(), $$0.getText(), Component.translationArg($$1))
            : Component.translatable("narration.suggestion", this.current + 1, this.suggestionList.size(), $$0.getText());
      }
   }
}
