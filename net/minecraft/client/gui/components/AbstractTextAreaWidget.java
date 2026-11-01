package net.minecraft.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractTextAreaWidget extends AbstractScrollArea {
   private static final WidgetSprites BACKGROUND_SPRITES = new WidgetSprites(
      Identifier.withDefaultNamespace("widget/text_field"), Identifier.withDefaultNamespace("widget/text_field_highlighted")
   );
   private static final int INNER_PADDING = 4;
   public static final int DEFAULT_TOTAL_PADDING = 8;
   private boolean showBackground = true;
   private boolean showDecorations = true;

   public AbstractTextAreaWidget(int p_388859_, int p_387520_, int p_387683_, int p_387659_, Component p_386737_) {
      super(p_388859_, p_387520_, p_387683_, p_387659_, p_386737_);
   }

   public AbstractTextAreaWidget(int p_422418_, int p_422574_, int p_422531_, int p_422115_, Component p_421550_, boolean p_422563_, boolean p_422431_) {
      this(p_422418_, p_422574_, p_422531_, p_422115_, p_421550_);
      this.showBackground = p_422563_;
      this.showDecorations = p_422431_;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent p_445834_, boolean p_433245_) {
      boolean flag = this.updateScrolling(p_445834_);
      return super.mouseClicked(p_445834_, p_433245_) || flag;
   }

   @Override
   public boolean keyPressed(KeyEvent p_446633_) {
      boolean flag = p_446633_.isUp();
      boolean flag1 = p_446633_.isDown();
      if (flag || flag1) {
         double d0 = this.scrollAmount();
         this.setScrollAmount(this.scrollAmount() + (flag ? -1 : 1) * this.scrollRate());
         if (d0 != this.scrollAmount()) {
            return true;
         }
      }

      return super.keyPressed(p_446633_);
   }

   @Override
   public void renderWidget(GuiGraphics p_386672_, int p_387901_, int p_387577_, float p_387259_) {
      if (this.visible) {
         if (this.showBackground) {
            this.renderBackground(p_386672_);
         }

         p_386672_.enableScissor(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1);
         p_386672_.pose().pushMatrix();
         p_386672_.pose().translate(0.0F, (float)(-this.scrollAmount()));
         this.renderContents(p_386672_, p_387901_, p_387577_, p_387259_);
         p_386672_.pose().popMatrix();
         p_386672_.disableScissor();
         this.renderScrollbar(p_386672_, p_387901_, p_387577_);
         if (this.showDecorations) {
            this.renderDecorations(p_386672_);
         }
      }
   }

   protected void renderDecorations(GuiGraphics p_387032_) {
   }

   protected int innerPadding() {
      return 4;
   }

   protected int totalInnerPadding() {
      return this.innerPadding() * 2;
   }

   @Override
   public boolean isMouseOver(double p_386839_, double p_388246_) {
      return this.active
         && this.visible
         && p_386839_ >= this.getX()
         && p_388246_ >= this.getY()
         && p_386839_ < this.getRight() + 6
         && p_388246_ < this.getBottom();
   }

   @Override
   protected int scrollBarX() {
      return this.getRight();
   }

   @Override
   protected int contentHeight() {
      return this.getInnerHeight() + this.totalInnerPadding();
   }

   protected void renderBackground(GuiGraphics p_387240_) {
      this.renderBorder(p_387240_, this.getX(), this.getY(), this.getWidth(), this.getHeight());
   }

   protected void renderBorder(GuiGraphics p_388574_, int p_387898_, int p_387393_, int p_388028_, int p_387158_) {
      Identifier identifier = BACKGROUND_SPRITES.get(this.isActive(), this.isFocused());
      p_388574_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, p_387898_, p_387393_, p_388028_, p_387158_);
   }

   protected boolean withinContentAreaTopBottom(int p_386967_, int p_387417_) {
      return p_387417_ - this.scrollAmount() >= this.getY() && p_386967_ - this.scrollAmount() <= this.getY() + this.height;
   }

   protected abstract int getInnerHeight();

   protected abstract void renderContents(GuiGraphics var1, int var2, int var3, float var4);

   protected int getInnerLeft() {
      return this.getX() + this.innerPadding();
   }

   protected int getInnerTop() {
      return this.getY() + this.innerPadding();
   }

   @Override
   public void playDownSound(SoundManager p_386774_) {
   }
}
