package net.neoforged.neoforge.common.util;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record InsertingContents(int index) implements ComponentContents {
   public static final MapCodec<InsertingContents> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(ExtraCodecs.NON_NEGATIVE_INT.fieldOf("index").forGetter(InsertingContents::index)).apply(i, InsertingContents::new)
   );
   public static final ComponentContents.Type<InsertingContents> TYPE = new ComponentContents.Type<>(CODEC, "neoforge:inserting");
   private static final ThreadLocal<Deque<TranslatableContents>> TRANSLATION_STACK = ThreadLocal.withInitial(ArrayDeque::new);

   @Internal
   public static boolean pushTranslation(TranslatableContents contents) {
      for (TranslatableContents other : TRANSLATION_STACK.get()) {
         if (contents == other) {
            return false;
         }
      }

      TRANSLATION_STACK.get().push(contents);
      return true;
   }

   @Internal
   public static void popTranslation() {
      TRANSLATION_STACK.get().pop();
   }

   @Override
   public <T> Optional<T> visit(FormattedText.ContentConsumer<T> visitor) {
      TranslatableContents translation = TRANSLATION_STACK.get().peek();
      if (translation != null && translation.getArgs().length > this.index) {
         Object arg = translation.getArgs()[this.index];
         return arg instanceof Component component ? component.visit(visitor) : visitor.accept(arg.toString());
      } else {
         return visitor.accept("%" + (this.index + 1) + "$s");
      }
   }

   @Override
   public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> visitor, Style style) {
      TranslatableContents translation = TRANSLATION_STACK.get().peek();
      if (translation != null && translation.getArgs().length > this.index) {
         Object arg = translation.getArgs()[this.index];
         return arg instanceof Component component ? component.visit(visitor, style) : visitor.accept(style, arg.toString());
      } else {
         return visitor.accept(style, "%" + (this.index + 1) + "$s");
      }
   }

   @Override
   public ComponentContents.Type<?> type() {
      return TYPE;
   }
}
