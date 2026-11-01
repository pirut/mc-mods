package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.util.FormattedCharSequence;

public class MutableComponent implements Component {
   private final ComponentContents contents;
   private final List<Component> siblings;
   private Style style;
   private FormattedCharSequence visualOrderText = FormattedCharSequence.EMPTY;
   @Nullable
   private Language decomposedWith;

   MutableComponent(ComponentContents p_237200_, List<Component> p_237201_, Style p_237202_) {
      this.contents = p_237200_;
      this.siblings = p_237201_;
      this.style = p_237202_;
   }

   public static MutableComponent create(ComponentContents p_237205_) {
      return new MutableComponent(p_237205_, Lists.newArrayList(), Style.EMPTY);
   }

   @Override
   public ComponentContents getContents() {
      return this.contents;
   }

   @Override
   public List<Component> getSiblings() {
      return this.siblings;
   }

   public MutableComponent setStyle(Style p_130943_) {
      this.style = p_130943_;
      return this;
   }

   @Override
   public Style getStyle() {
      return this.style;
   }

   public MutableComponent append(String p_130947_) {
      return p_130947_.isEmpty() ? this : this.append(Component.literal(p_130947_));
   }

   public MutableComponent append(Component p_130942_) {
      this.siblings.add(p_130942_);
      return this;
   }

   public MutableComponent withStyle(UnaryOperator<Style> p_130939_) {
      this.setStyle(p_130939_.apply(this.getStyle()));
      return this;
   }

   public MutableComponent withStyle(Style p_130949_) {
      this.setStyle(p_130949_.applyTo(this.getStyle()));
      return this;
   }

   public MutableComponent withStyle(ChatFormatting... p_130945_) {
      this.setStyle(this.getStyle().applyFormats(p_130945_));
      return this;
   }

   public MutableComponent withStyle(ChatFormatting p_130941_) {
      this.setStyle(this.getStyle().applyFormat(p_130941_));
      return this;
   }

   public MutableComponent withColor(int p_307552_) {
      this.setStyle(this.getStyle().withColor(p_307552_));
      return this;
   }

   @Override
   public FormattedCharSequence getVisualOrderText() {
      Language $$0 = Language.getInstance();
      if (this.decomposedWith != $$0) {
         this.visualOrderText = $$0.getVisualOrder(this);
         this.decomposedWith = $$0;
      }

      return this.visualOrderText;
   }

   @Override
   public boolean equals(Object p_237209_) {
      if (this == p_237209_) {
         return true;
      } else {
         return !(p_237209_ instanceof MutableComponent $$1)
            ? false
            : this.contents.equals($$1.contents) && this.style.equals($$1.style) && this.siblings.equals($$1.siblings);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.contents, this.style, this.siblings);
   }

   @Override
   public String toString() {
      StringBuilder $$0 = new StringBuilder(this.contents.toString());
      boolean $$1 = !this.style.isEmpty();
      boolean $$2 = !this.siblings.isEmpty();
      if ($$1 || $$2) {
         $$0.append('[');
         if ($$1) {
            $$0.append("style=");
            $$0.append(this.style);
         }

         if ($$1 && $$2) {
            $$0.append(", ");
         }

         if ($$2) {
            $$0.append("siblings=");
            $$0.append(this.siblings);
         }

         $$0.append(']');
      }

      return $$0.toString();
   }
}
