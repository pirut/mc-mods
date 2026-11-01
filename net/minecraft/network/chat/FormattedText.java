package net.minecraft.network.chat;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.Unit;

public interface FormattedText {
   Optional<Unit> STOP_ITERATION = Optional.of(Unit.INSTANCE);
   FormattedText EMPTY = new FormattedText() {
      @Override
      public <T> Optional<T> visit(FormattedText.ContentConsumer<T> acceptor) {
         return Optional.empty();
      }

      @Override
      public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> acceptor, Style style) {
         return Optional.empty();
      }
   };

   <T> Optional<T> visit(FormattedText.ContentConsumer<T> acceptor);

   <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> acceptor, Style style);

   static FormattedText of(String text) {
      return new FormattedText() {
         @Override
         public <T> Optional<T> visit(FormattedText.ContentConsumer<T> acceptor) {
            return acceptor.accept(text);
         }

         @Override
         public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> acceptor, Style style) {
            return acceptor.accept(style, text);
         }
      };
   }

   static FormattedText of(String text, Style style) {
      return new FormattedText() {
         @Override
         public <T> Optional<T> visit(FormattedText.ContentConsumer<T> acceptor) {
            return acceptor.accept(text);
         }

         @Override
         public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> acceptor, Style style) {
            return acceptor.accept(style.applyTo(style), text);
         }
      };
   }

   static FormattedText composite(FormattedText... elements) {
      return composite(ImmutableList.copyOf(elements));
   }

   static FormattedText composite(List<? extends FormattedText> elements) {
      return new FormattedText() {
         @Override
         public <T> Optional<T> visit(FormattedText.ContentConsumer<T> acceptor) {
            for (FormattedText formattedText : elements) {
               Optional<T> optional = formattedText.visit(acceptor);
               if (optional.isPresent()) {
                  return optional;
               }
            }

            return Optional.empty();
         }

         @Override
         public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> acceptor, Style style) {
            for (FormattedText formattedText : elements) {
               Optional<T> optional = formattedText.visit(acceptor, style);
               if (optional.isPresent()) {
                  return optional;
               }
            }

            return Optional.empty();
         }
      };
   }

   default String getString() {
      StringBuilder stringBuilder = new StringBuilder();
      this.visit(string -> {
         stringBuilder.append(string);
         return Optional.empty();
      });
      return stringBuilder.toString();
   }

   public interface ContentConsumer<T> {
      Optional<T> accept(String string);
   }

   public interface StyledContentConsumer<T> {
      Optional<T> accept(Style style, String string);
   }
}
