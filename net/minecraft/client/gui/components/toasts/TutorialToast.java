package net.minecraft.client.gui.components.toasts;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TutorialToast implements Toast {
   private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/tutorial");
   public static final int PROGRESS_BAR_WIDTH = 154;
   public static final int PROGRESS_BAR_HEIGHT = 1;
   public static final int PROGRESS_BAR_X = 3;
   public static final int PROGRESS_BAR_MARGIN_BOTTOM = 4;
   private static final int PADDING_TOP = 7;
   private static final int PADDING_BOTTOM = 3;
   private static final int LINE_SPACING = 11;
   private static final int TEXT_LEFT = 30;
   private static final int TEXT_WIDTH = 126;
   private final TutorialToast.Icons icon;
   private final List<FormattedCharSequence> lines;
   private Toast.Visibility visibility = Toast.Visibility.SHOW;
   private long lastSmoothingTime;
   private float smoothedProgress;
   private float progress;
   private final boolean progressable;
   private final int timeToDisplayMs;

   public TutorialToast(Font p_383078_, TutorialToast.Icons p_365013_, Component p_363481_, @Nullable Component p_365235_, boolean p_365276_, int p_363245_) {
      this.icon = p_365013_;
      this.lines = new ArrayList<>(2);
      this.lines.addAll(p_383078_.split(p_363481_.copy().withColor(-11534256), 126));
      if (p_365235_ != null) {
         this.lines.addAll(p_383078_.split(p_365235_, 126));
      }

      this.progressable = p_365276_;
      this.timeToDisplayMs = p_363245_;
   }

   public TutorialToast(Font p_382988_, TutorialToast.Icons p_94958_, Component p_94959_, @Nullable Component p_94960_, boolean p_94961_) {
      this(p_382988_, p_94958_, p_94959_, p_94960_, p_94961_, 0);
   }

   @Override
   public Toast.Visibility getWantedVisibility() {
      return this.visibility;
   }

   @Override
   public void update(ToastManager p_361676_, long p_362771_) {
      if (this.timeToDisplayMs > 0) {
         this.progress = Math.min((float)p_362771_ / this.timeToDisplayMs, 1.0F);
         this.smoothedProgress = this.progress;
         this.lastSmoothingTime = p_362771_;
         if (p_362771_ > this.timeToDisplayMs) {
            this.hide();
         }
      } else if (this.progressable) {
         this.smoothedProgress = Mth.clampedLerp((float)(p_362771_ - this.lastSmoothingTime) / 100.0F, this.smoothedProgress, this.progress);
         this.lastSmoothingTime = p_362771_;
      }
   }

   @Override
   public int height() {
      return 7 + this.contentHeight() + 3;
   }

   private int contentHeight() {
      return Math.max(this.lines.size(), 2) * 11;
   }

   @Override
   public void render(GuiGraphics p_283197_, Font p_364452_, long p_281902_) {
      int i = this.height();
      p_283197_.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, this.width(), i);
      this.icon.render(p_283197_, 6, 6);
      int j = this.lines.size() * 11;
      int k = 7 + (this.contentHeight() - j) / 2;

      for (int l = 0; l < this.lines.size(); l++) {
         p_283197_.drawString(p_364452_, this.lines.get(l), 30, k + l * 11, -16777216, false);
      }

      if (this.progressable) {
         int j1 = i - 4;
         p_283197_.fill(3, j1, 157, j1 + 1, -1);
         int i1;
         if (this.progress >= this.smoothedProgress) {
            i1 = -16755456;
         } else {
            i1 = -11206656;
         }

         p_283197_.fill(3, j1, (int)(3.0F + 154.0F * this.smoothedProgress), j1 + 1, i1);
      }
   }

   public void hide() {
      this.visibility = Toast.Visibility.HIDE;
   }

   public void updateProgress(float p_94963_) {
      this.progress = p_94963_;
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Icons {
      MOVEMENT_KEYS(Identifier.withDefaultNamespace("toast/movement_keys")),
      MOUSE(Identifier.withDefaultNamespace("toast/mouse")),
      TREE(Identifier.withDefaultNamespace("toast/tree")),
      RECIPE_BOOK(Identifier.withDefaultNamespace("toast/recipe_book")),
      WOODEN_PLANKS(Identifier.withDefaultNamespace("toast/wooden_planks")),
      SOCIAL_INTERACTIONS(Identifier.withDefaultNamespace("toast/social_interactions")),
      RIGHT_CLICK(Identifier.withDefaultNamespace("toast/right_click"));

      private final Identifier sprite;

      private Icons(Identifier p_467649_) {
         this.sprite = p_467649_;
      }

      public void render(GuiGraphics p_282818_, int p_283064_, int p_282765_) {
         p_282818_.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, p_283064_, p_282765_, 20, 20);
      }
   }
}
