package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.Ordering;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class EffectsInInventory {
   private static final Identifier EFFECT_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("container/inventory/effect_background");
   private static final Identifier EFFECT_BACKGROUND_AMBIENT_SPRITE = Identifier.withDefaultNamespace("container/inventory/effect_background_ambient");
   private static final int ICON_SIZE = 18;
   public static final int SPACING = 7;
   private static final int TEXT_X_OFFSET = 32;
   public static final int SPRITE_SQUARE_SIZE = 32;
   private final AbstractContainerScreen<?> screen;
   private final Minecraft minecraft;

   public EffectsInInventory(AbstractContainerScreen<?> p_376708_) {
      this.screen = p_376708_;
      this.minecraft = Minecraft.getInstance();
   }

   public boolean canSeeEffects() {
      int i = this.screen.leftPos + this.screen.imageWidth + 2;
      int j = this.screen.width - i;
      return j >= 32;
   }

   public void render(GuiGraphics p_454890_, int p_455090_, int p_455800_) {
      int i = this.screen.leftPos + this.screen.imageWidth + 2;
      int j = this.screen.width - i;
      Collection<MobEffectInstance> collection = this.minecraft.player.getActiveEffects();
      if (!collection.isEmpty() && j >= 32) {
         boolean flag = j >= 120;
         ScreenEvent.RenderInventoryMobEffects event = ClientHooks.onScreenPotionSize(this.screen, j, !flag, i);
         if (event.isCanceled()) {
            return;
         }

         flag = !event.isCompact();
         i = event.getHorizontalOffset();
         collection = collection.stream().filter(ClientHooks::shouldRenderEffect).sorted().collect(Collectors.toList());
         int k = flag ? j - 7 : 32;
         int l = 33;
         if (collection.size() > 5) {
            l = 132 / (collection.size() - 1);
         }

         this.renderEffects(p_454890_, collection, i, l, p_455090_, p_455800_, k);
      }
   }

   private void renderEffects(
      GuiGraphics p_376884_, Collection<MobEffectInstance> p_456116_, int p_376869_, int p_376740_, int p_455972_, int p_455320_, int p_455280_
   ) {
      Iterable<MobEffectInstance> iterable = Ordering.natural().sortedCopy(p_456116_);
      int i = this.screen.topPos;
      Font font = this.screen.getFont();

      for (MobEffectInstance mobeffectinstance : iterable) {
         IClientMobEffectExtensions renderer = IClientMobEffectExtensions.of(mobeffectinstance);
         boolean flag = mobeffectinstance.isAmbient();
         Component component = this.getEffectName(mobeffectinstance);
         Component component1 = MobEffectUtil.formatDuration(mobeffectinstance, 1.0F, this.minecraft.level.tickRateManager().tickrate());
         int j = this.renderBackground(p_376884_, font, component, component1, p_376869_, i, flag, p_455280_);
         if (!renderer.renderInventoryText(mobeffectinstance, this.screen, p_376884_, p_376869_, i, 0)) {
            this.renderText(p_376884_, component, component1, font, p_376869_, i, j, p_376740_, p_455972_, p_455320_, mobeffectinstance);
         }

         if (!renderer.renderInventoryIcon(mobeffectinstance, this.screen, p_376884_, p_376869_ + 7, i, 0)) {
            p_376884_.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(mobeffectinstance.getEffect()), p_376869_ + 7, i + 7, 18, 18);
         }

         i += p_376740_;
      }
   }

   private int renderBackground(
      GuiGraphics p_455258_, Font p_455170_, Component p_455093_, Component p_468799_, int p_454839_, int p_456281_, boolean p_455567_, int p_454956_
   ) {
      int i = 32 + p_455170_.width(p_455093_) + 7;
      int j = 32 + p_455170_.width(p_468799_) + 7;
      int k = Math.min(p_454956_, Math.max(i, j));
      p_455258_.blitSprite(RenderPipelines.GUI_TEXTURED, p_455567_ ? EFFECT_BACKGROUND_AMBIENT_SPRITE : EFFECT_BACKGROUND_SPRITE, p_454839_, p_456281_, k, 32);
      return k;
   }

   @Deprecated
   private void renderText(
      GuiGraphics p_456196_,
      Component p_455663_,
      Component p_467145_,
      Font p_454693_,
      int p_454754_,
      int p_455546_,
      int p_455809_,
      int p_455668_,
      int p_455390_,
      int p_455363_
   ) {
      this.renderText(p_456196_, p_455663_, p_467145_, p_454693_, p_454754_, p_455546_, p_455809_, p_455668_, p_455390_, p_455363_, null);
   }

   private void renderText(
      GuiGraphics p_456196_,
      Component p_455663_,
      Component p_467145_,
      Font p_454693_,
      int p_454754_,
      int p_455546_,
      int p_455809_,
      int p_455668_,
      int p_455390_,
      int p_455363_,
      @Nullable MobEffectInstance effectInstance
   ) {
      int i = p_454754_ + 32;
      int j = p_455546_ + 7;
      int k = p_455809_ - 32 - 7;
      boolean flag;
      if (k > 0) {
         boolean flag1 = p_454693_.width(p_455663_) > k;
         FormattedCharSequence formattedcharsequence = flag1 ? StringWidget.clipText(p_455663_, p_454693_, k) : p_455663_.getVisualOrderText();
         p_456196_.drawString(p_454693_, formattedcharsequence, i, j, -1);
         p_456196_.drawString(p_454693_, p_467145_, i, j + 9, -8355712);
         flag = flag1;
      } else {
         flag = true;
      }

      if (p_455390_ >= p_454754_ && p_455390_ <= p_454754_ + p_455809_ && p_455363_ >= p_455546_ && p_455363_ <= p_455546_ + p_455668_) {
         List<Component> list = flag ? List.of(p_455663_, p_467145_) : List.of();
         if (effectInstance != null) {
            list = ClientHooks.getEffectTooltip(this.screen, effectInstance, list);
         }

         if (!list.isEmpty()) {
            p_456196_.setTooltipForNextFrame(this.screen.getFont(), list, Optional.empty(), p_455390_, p_455363_);
         }
      }
   }

   private Component getEffectName(MobEffectInstance p_376252_) {
      MutableComponent mutablecomponent = p_376252_.getEffect().value().getDisplayName().copy();
      if (p_376252_.getAmplifier() >= 1 && p_376252_.getAmplifier() <= 9) {
         mutablecomponent.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + (p_376252_.getAmplifier() + 1)));
      }

      return mutablecomponent;
   }
}
