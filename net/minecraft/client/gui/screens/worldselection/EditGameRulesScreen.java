package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.DataResult;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.gamerules.GameRuleEntryFactoryManager;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class EditGameRulesScreen extends Screen {
   private static final Component TITLE = Component.translatable("editGamerule.title");
   private static final int SPACING = 8;
   final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final Consumer<Optional<GameRules>> exitCallback;
   private final Set<EditGameRulesScreen.RuleEntry> invalidEntries = Sets.newHashSet();
   public final GameRules gameRules;
   private EditGameRulesScreen.RuleList ruleList;
   @Nullable
   private Button doneButton;

   public EditGameRulesScreen(GameRules p_461047_, Consumer<Optional<GameRules>> p_101052_) {
      super(TITLE);
      this.gameRules = p_461047_;
      this.exitCallback = p_101052_;
   }

   @Override
   protected void init() {
      this.layout.addTitleHeader(TITLE, this.font);
      this.ruleList = this.layout.addToContents(new EditGameRulesScreen.RuleList(this.gameRules));
      LinearLayout linearlayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      this.doneButton = linearlayout.addChild(
         Button.builder(CommonComponents.GUI_DONE, p_460251_ -> this.exitCallback.accept(Optional.of(this.gameRules))).build()
      );
      linearlayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, p_329749_ -> this.onClose()).build());
      this.layout.visitWidgets(p_321377_ -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_321377_);
      });
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      if (this.ruleList != null) {
         this.ruleList.updateSize(this.width, this.layout);
      }
   }

   @Override
   public void onClose() {
      this.exitCallback.accept(Optional.empty());
   }

   private void updateDoneButton() {
      if (this.doneButton != null) {
         this.doneButton.active = this.invalidEntries.isEmpty();
      }
   }

   public void markInvalid(EditGameRulesScreen.RuleEntry p_101061_) {
      this.invalidEntries.add(p_101061_);
      this.updateDoneButton();
   }

   public void clearInvalid(EditGameRulesScreen.RuleEntry p_101075_) {
      this.invalidEntries.remove(p_101075_);
      this.updateDoneButton();
   }

   @OnlyIn(Dist.CLIENT)
   public class BooleanRuleEntry extends EditGameRulesScreen.GameRuleEntry {
      private final CycleButton<Boolean> checkbox;

      public BooleanRuleEntry(Component p_101101_, List<FormattedCharSequence> p_101102_, String p_101103_, GameRule<Boolean> p_460720_) {
         super(p_101102_, p_101101_);
         this.checkbox = CycleButton.onOffBuilder(EditGameRulesScreen.this.gameRules.get(p_460720_))
            .displayOnlyValue()
            .withCustomNarration(p_170219_ -> p_170219_.createDefaultNarrationMessage().append("\n").append(p_101103_))
            .create(10, 5, 44, 20, p_101101_, (p_460253_, p_460254_) -> EditGameRulesScreen.this.gameRules.set(p_460720_, p_460254_, null));
         this.children.add(this.checkbox);
      }

      @Override
      public void renderContent(GuiGraphics p_439057_, int p_439789_, int p_439072_, boolean p_440132_, float p_440063_) {
         this.renderLabel(p_439057_, this.getContentY(), this.getContentX());
         this.checkbox.setX(this.getContentRight() - 45);
         this.checkbox.setY(this.getContentY());
         this.checkbox.render(p_439057_, p_439789_, p_439072_, p_440063_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class CategoryRuleEntry extends EditGameRulesScreen.RuleEntry {
      final Component label;

      public CategoryRuleEntry(Component p_101141_) {
         super(null);
         this.label = p_101141_;
      }

      @Override
      public void renderContent(GuiGraphics p_440317_, int p_439215_, int p_440653_, boolean p_439060_, float p_439068_) {
         p_440317_.drawCenteredString(EditGameRulesScreen.this.minecraft.font, this.label, this.getContentXMiddle(), this.getContentY() + 5, -1);
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return ImmutableList.of();
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return ImmutableList.of(new NarratableEntry() {
            @Override
            public NarratableEntry.NarrationPriority narrationPriority() {
               return NarratableEntry.NarrationPriority.HOVERED;
            }

            @Override
            public void updateNarration(NarrationElementOutput p_170225_) {
               p_170225_.add(NarratedElementType.TITLE, CategoryRuleEntry.this.label);
            }
         });
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface EntryFactory<T> {
      EditGameRulesScreen.RuleEntry create(Component var1, List<FormattedCharSequence> var2, String var3, GameRule<T> var4);
   }

   @OnlyIn(Dist.CLIENT)
   public abstract class GameRuleEntry extends EditGameRulesScreen.RuleEntry {
      private final List<FormattedCharSequence> label;
      protected final List<AbstractWidget> children = Lists.newArrayList();

      public GameRuleEntry(@Nullable List<FormattedCharSequence> p_101164_, Component p_101165_) {
         super(p_101164_);
         this.label = EditGameRulesScreen.this.minecraft.font.split(p_101165_, 175);
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return this.children;
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return this.children;
      }

      protected void renderLabel(GuiGraphics p_282711_, int p_281539_, int p_281414_) {
         if (this.label.size() == 1) {
            p_282711_.drawString(EditGameRulesScreen.this.minecraft.font, this.label.get(0), p_281414_, p_281539_ + 5, -1);
         } else if (this.label.size() >= 2) {
            p_282711_.drawString(EditGameRulesScreen.this.minecraft.font, this.label.get(0), p_281414_, p_281539_, -1);
            p_282711_.drawString(EditGameRulesScreen.this.minecraft.font, this.label.get(1), p_281414_, p_281539_ + 10, -1);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class IntegerRuleEntry extends EditGameRulesScreen.GameRuleEntry {
      private final EditBox input;

      public IntegerRuleEntry(Component p_101175_, List<FormattedCharSequence> p_101176_, String p_101177_, GameRule<Integer> p_460758_) {
         super(p_101176_, p_101175_);
         this.input = new EditBox(EditGameRulesScreen.this.minecraft.font, 10, 5, 44, 20, p_101175_.copy().append("\n").append(p_101177_).append("\n"));
         this.input.setValue(EditGameRulesScreen.this.gameRules.getAsString(p_460758_));
         this.input.setResponder(p_460256_ -> {
            DataResult<Integer> dataresult = p_460758_.deserialize(p_460256_);
            if (dataresult.isSuccess()) {
               this.input.setTextColor(-2039584);
               EditGameRulesScreen.this.clearInvalid(this);
               EditGameRulesScreen.this.gameRules.set(p_460758_, (Integer)dataresult.getOrThrow(), null);
            } else {
               this.input.setTextColor(-65536);
               EditGameRulesScreen.this.markInvalid(this);
            }
         });
         this.children.add(this.input);
      }

      @Override
      public void renderContent(GuiGraphics p_439470_, int p_438942_, int p_439225_, boolean p_440092_, float p_440277_) {
         this.renderLabel(p_439470_, this.getContentY(), this.getContentX());
         this.input.setX(this.getContentRight() - 45);
         this.input.setY(this.getContentY());
         this.input.render(p_439470_, p_438942_, p_439225_, p_440277_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public abstract static class RuleEntry extends ContainerObjectSelectionList.Entry<EditGameRulesScreen.RuleEntry> {
      @Nullable
      final List<FormattedCharSequence> tooltip;

      public RuleEntry(@Nullable List<FormattedCharSequence> p_194062_) {
         this.tooltip = p_194062_;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class RuleList extends ContainerObjectSelectionList<EditGameRulesScreen.RuleEntry> {
      private static final int ITEM_HEIGHT = 24;

      public RuleList(GameRules p_460700_) {
         super(
            Minecraft.getInstance(),
            EditGameRulesScreen.this.width,
            EditGameRulesScreen.this.layout.getContentHeight(),
            EditGameRulesScreen.this.layout.getHeaderHeight(),
            24
         );
         final Map<GameRuleCategory, Map<GameRule<?>, EditGameRulesScreen.RuleEntry>> map = Maps.newHashMap();
         p_460700_.visitGameRuleTypes(
            new GameRuleTypeVisitor() {
               @Override
               public void visitBoolean(GameRule<Boolean> p_460836_) {
                  this.addEntry(
                     p_460836_,
                     (p_460262_, p_460263_, p_460264_, p_460265_) -> EditGameRulesScreen.this.new BooleanRuleEntry(p_460262_, p_460263_, p_460264_, p_460265_)
                  );
               }

               @Override
               public void visitInteger(GameRule<Integer> p_461172_) {
                  this.addEntry(
                     p_461172_,
                     (p_460258_, p_460259_, p_460260_, p_460261_) -> EditGameRulesScreen.this.new IntegerRuleEntry(p_460258_, p_460259_, p_460260_, p_460261_)
                  );
               }

               @Override
               public <T> void visit(GameRule<T> p_461081_) {
                  GameRuleEntryFactoryManager.appendGameRuleEntry(EditGameRulesScreen.this, p_461081_, this::addEntry);
               }

               private <T> void addEntry(GameRule<T> p_461117_, EditGameRulesScreen.EntryFactory<T> p_101226_) {
                  Component component = Component.translatable(p_461117_.getDescriptionId());
                  Component component1 = Component.literal(p_461117_.id()).withStyle(ChatFormatting.YELLOW);
                  Component component2 = Component.translatable("editGamerule.default", Component.literal(p_461117_.serialize(p_461117_.defaultValue())))
                     .withStyle(ChatFormatting.GRAY);
                  String s = p_461117_.getDescriptionId() + ".description";
                  List<FormattedCharSequence> list;
                  String s1;
                  if (I18n.exists(s)) {
                     Builder<FormattedCharSequence> builder = ImmutableList.builder().add(component1.getVisualOrderText());
                     Component component3 = Component.translatable(s);
                     EditGameRulesScreen.this.font.split(component3, 150).forEach(builder::add);
                     list = builder.add(component2.getVisualOrderText()).build();
                     s1 = component3.getString() + "\n" + component2.getString();
                  } else {
                     list = ImmutableList.of(component1.getVisualOrderText(), component2.getVisualOrderText());
                     s1 = component2.getString();
                  }

                  map.computeIfAbsent(p_461117_.category(), p_461130_ -> Maps.newHashMap()).put(p_461117_, p_101226_.create(component, list, s1, p_461117_));
               }
            }
         );
         map.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey(Comparator.comparing(GameRuleCategory::getDescriptionId)))
            .forEach(
               p_460257_ -> {
                  this.addEntry(
                     EditGameRulesScreen.this.new CategoryRuleEntry(p_460257_.getKey().label().withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW))
                  );
                  p_460257_.getValue()
                     .entrySet()
                     .stream()
                     .sorted(Map.Entry.comparingByKey(Comparator.comparing(GameRule::getDescriptionId)))
                     .forEach(p_170229_ -> this.addEntry(p_170229_.getValue()));
               }
            );
      }

      @Override
      public void renderWidget(GuiGraphics p_313903_, int p_313824_, int p_313867_, float p_313845_) {
         super.renderWidget(p_313903_, p_313824_, p_313867_, p_313845_);
         EditGameRulesScreen.RuleEntry editgamerulesscreen$ruleentry = this.getHovered();
         if (editgamerulesscreen$ruleentry != null && editgamerulesscreen$ruleentry.tooltip != null) {
            p_313903_.setTooltipForNextFrame(editgamerulesscreen$ruleentry.tooltip, p_313824_, p_313867_);
         }
      }
   }
}
