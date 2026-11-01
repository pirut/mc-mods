package net.minecraft.client.gui.screens;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DeathScreen extends Screen {
   private static final int TITLE_SCALE = 2;
   private static final Identifier DRAFT_REPORT_SPRITE = Identifier.withDefaultNamespace("icon/draft_report");
   private int delayTicker;
   @Nullable
   private final Component causeOfDeath;
   private final boolean hardcore;
   private final LocalPlayer player;
   private final Component deathScore;
   private final List<Button> exitButtons = Lists.newArrayList();
   @Nullable
   private Button exitToTitleButton;

   public DeathScreen(@Nullable Component p_95911_, boolean p_95912_, LocalPlayer p_458222_) {
      super(Component.translatable(p_95912_ ? "deathScreen.title.hardcore" : "deathScreen.title"));
      this.causeOfDeath = p_95911_;
      this.hardcore = p_95912_;
      this.player = p_458222_;
      Component component = Component.literal(Integer.toString(p_458222_.getScore())).withStyle(ChatFormatting.YELLOW);
      this.deathScore = Component.translatable("deathScreen.score.value", component);
   }

   @Override
   protected void init() {
      this.delayTicker = 0;
      this.exitButtons.clear();
      Component component = this.hardcore ? Component.translatable("deathScreen.spectate") : Component.translatable("deathScreen.respawn");
      this.exitButtons.add(this.addRenderableWidget(Button.builder(component, p_457347_ -> {
         this.player.respawn();
         p_457347_.active = false;
      }).bounds(this.width / 2 - 100, this.height / 4 + 72, 200, 20).build()));
      this.exitToTitleButton = this.addRenderableWidget(
         Button.builder(
               Component.translatable("deathScreen.titleScreen"),
               p_280796_ -> this.minecraft.getReportingContext().draftReportHandled(this.minecraft, this, this::handleExitToTitleScreen, true)
            )
            .bounds(this.width / 2 - 100, this.height / 4 + 96, 200, 20)
            .build()
      );
      this.exitButtons.add(this.exitToTitleButton);
      this.setButtonsActive(false);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   private void handleExitToTitleScreen() {
      if (this.hardcore) {
         this.exitToTitleScreen();
      } else {
         ConfirmScreen confirmscreen = new DeathScreen.TitleConfirmScreen(
            p_457346_ -> {
               if (p_457346_) {
                  this.exitToTitleScreen();
               } else {
                  this.player.respawn();
                  this.minecraft.setScreen(null);
               }
            },
            Component.translatable("deathScreen.quit.confirm"),
            CommonComponents.EMPTY,
            Component.translatable("deathScreen.titleScreen"),
            Component.translatable("deathScreen.respawn")
         );
         this.minecraft.setScreen(confirmscreen);
         confirmscreen.setDelay(20);
      }
   }

   private void exitToTitleScreen() {
      if (this.minecraft.level != null) {
         this.minecraft.level.disconnect(ClientLevel.DEFAULT_QUIT_MESSAGE);
      }

      this.minecraft.disconnectWithSavingScreen();
      this.minecraft.setScreen(new TitleScreen());
   }

   @Override
   public void render(GuiGraphics p_283488_, int p_283551_, int p_283002_, float p_281981_) {
      super.render(p_283488_, p_283551_, p_283002_, p_281981_);
      this.visitText(p_283488_.textRenderer(GuiGraphics.HoveredTextEffects.TOOLTIP_AND_CURSOR));
      if (this.exitToTitleButton != null && this.minecraft.getReportingContext().hasDraftReport()) {
         p_283488_.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            DRAFT_REPORT_SPRITE,
            this.exitToTitleButton.getX() + this.exitToTitleButton.getWidth() - 17,
            this.exitToTitleButton.getY() + 3,
            15,
            15
         );
      }
   }

   private void visitText(ActiveTextCollector p_458074_) {
      ActiveTextCollector.Parameters activetextcollector$parameters = p_458074_.defaultParameters();
      int i = this.width / 2;
      p_458074_.defaultParameters(activetextcollector$parameters.withScale(2.0F));
      p_458074_.accept(TextAlignment.CENTER, i / 2, 30, this.title);
      p_458074_.defaultParameters(activetextcollector$parameters);
      if (this.causeOfDeath != null) {
         p_458074_.accept(TextAlignment.CENTER, i, 85, this.causeOfDeath);
      }

      p_458074_.accept(TextAlignment.CENTER, i, 100, this.deathScore);
   }

   @Override
   public void renderBackground(GuiGraphics p_296411_, int p_295765_, int p_295221_, float p_295625_) {
      renderDeathBackground(p_296411_, this.width, this.height);
   }

   static void renderDeathBackground(GuiGraphics p_339647_, int p_339641_, int p_339673_) {
      p_339647_.fillGradient(0, 0, p_339641_, p_339673_, 1615855616, -1602211792);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent p_446287_, boolean p_433128_) {
      ActiveTextCollector.ClickableStyleFinder activetextcollector$clickablestylefinder = new ActiveTextCollector.ClickableStyleFinder(
         this.getFont(), (int)p_446287_.x(), (int)p_446287_.y()
      );
      this.visitText(activetextcollector$clickablestylefinder);
      Style style = activetextcollector$clickablestylefinder.result();
      return style != null && style.getClickEvent() instanceof ClickEvent.OpenUrl clickevent$openurl
         ? clickUrlAction(this.minecraft, this, clickevent$openurl.uri())
         : super.mouseClicked(p_446287_, p_433128_);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public boolean isAllowedInPortal() {
      return true;
   }

   @Override
   public void tick() {
      super.tick();
      this.delayTicker++;
      if (this.delayTicker == 20) {
         this.setButtonsActive(true);
      }
   }

   private void setButtonsActive(boolean p_273413_) {
      for (Button button : this.exitButtons) {
         button.active = p_273413_;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class TitleConfirmScreen extends ConfirmScreen {
      public TitleConfirmScreen(BooleanConsumer p_273707_, Component p_273255_, Component p_273747_, Component p_273434_, Component p_273416_) {
         super(p_273707_, p_273255_, p_273747_, p_273434_, p_273416_);
      }

      @Override
      public void renderBackground(GuiGraphics p_339588_, int p_339615_, int p_339620_, float p_339674_) {
         DeathScreen.renderDeathBackground(p_339588_, this.width, this.height);
      }
   }
}
