package net.minecraft.util;

import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

public class StringDecomposer {
   private static final char REPLACEMENT_CHAR = '�';
   private static final Optional<Object> STOP_ITERATION = Optional.of(Unit.INSTANCE);

   private static boolean feedChar(Style p_14333_, FormattedCharSink p_14334_, int p_14335_, char p_14336_) {
      return Character.isSurrogate(p_14336_) ? p_14334_.accept(p_14335_, p_14333_, 65533) : p_14334_.accept(p_14335_, p_14333_, p_14336_);
   }

   public static boolean iterate(String p_14318_, Style p_14319_, FormattedCharSink p_14320_) {
      int $$3 = p_14318_.length();

      for (int $$4 = 0; $$4 < $$3; $$4++) {
         char $$5 = p_14318_.charAt($$4);
         if (Character.isHighSurrogate($$5)) {
            if ($$4 + 1 >= $$3) {
               if (!p_14320_.accept($$4, p_14319_, 65533)) {
                  return false;
               }
               break;
            }

            char $$6 = p_14318_.charAt($$4 + 1);
            if (Character.isLowSurrogate($$6)) {
               if (!p_14320_.accept($$4, p_14319_, Character.toCodePoint($$5, $$6))) {
                  return false;
               }

               $$4++;
            } else if (!p_14320_.accept($$4, p_14319_, 65533)) {
               return false;
            }
         } else if (!feedChar(p_14319_, p_14320_, $$4, $$5)) {
            return false;
         }
      }

      return true;
   }

   public static boolean iterateBackwards(String p_14338_, Style p_14339_, FormattedCharSink p_14340_) {
      int $$3 = p_14338_.length();

      for (int $$4 = $$3 - 1; $$4 >= 0; $$4--) {
         char $$5 = p_14338_.charAt($$4);
         if (Character.isLowSurrogate($$5)) {
            if ($$4 - 1 < 0) {
               if (!p_14340_.accept(0, p_14339_, 65533)) {
                  return false;
               }
               break;
            }

            char $$6 = p_14338_.charAt($$4 - 1);
            if (Character.isHighSurrogate($$6)) {
               if (!p_14340_.accept(--$$4, p_14339_, Character.toCodePoint($$6, $$5))) {
                  return false;
               }
            } else if (!p_14340_.accept($$4, p_14339_, 65533)) {
               return false;
            }
         } else if (!feedChar(p_14339_, p_14340_, $$4, $$5)) {
            return false;
         }
      }

      return true;
   }

   public static boolean iterateFormatted(String p_14347_, Style p_14348_, FormattedCharSink p_14349_) {
      return iterateFormatted(p_14347_, 0, p_14348_, p_14349_);
   }

   public static boolean iterateFormatted(String p_14307_, int p_14308_, Style p_14309_, FormattedCharSink p_14310_) {
      return iterateFormatted(p_14307_, p_14308_, p_14309_, p_14309_, p_14310_);
   }

   public static boolean iterateFormatted(String p_14312_, int p_14313_, Style p_14314_, Style p_14315_, FormattedCharSink p_14316_) {
      int $$5 = p_14312_.length();
      Style $$6 = p_14314_;

      for (int $$7 = p_14313_; $$7 < $$5; $$7++) {
         char $$8 = p_14312_.charAt($$7);
         if ($$8 == 167) {
            if ($$7 + 1 >= $$5) {
               break;
            }

            char $$9 = p_14312_.charAt($$7 + 1);
            ChatFormatting $$10 = ChatFormatting.getByCode($$9);
            if ($$10 != null) {
               $$6 = $$10 == ChatFormatting.RESET ? p_14315_ : $$6.applyLegacyFormat($$10);
            }

            $$7++;
         } else if (Character.isHighSurrogate($$8)) {
            if ($$7 + 1 >= $$5) {
               if (!p_14316_.accept($$7, $$6, 65533)) {
                  return false;
               }
               break;
            }

            char $$11 = p_14312_.charAt($$7 + 1);
            if (Character.isLowSurrogate($$11)) {
               if (!p_14316_.accept($$7, $$6, Character.toCodePoint($$8, $$11))) {
                  return false;
               }

               $$7++;
            } else if (!p_14316_.accept($$7, $$6, 65533)) {
               return false;
            }
         } else if (!feedChar($$6, p_14316_, $$7, $$8)) {
            return false;
         }
      }

      return true;
   }

   public static boolean iterateFormatted(FormattedText p_14329_, Style p_14330_, FormattedCharSink p_14331_) {
      return p_14329_.visit((p_14302_, p_14303_) -> iterateFormatted(p_14303_, 0, p_14302_, p_14331_) ? Optional.empty() : STOP_ITERATION, p_14330_).isEmpty();
   }

   public static String filterBrokenSurrogates(String p_14305_) {
      StringBuilder $$1 = new StringBuilder();
      iterate(p_14305_, Style.EMPTY, (p_14343_, p_14344_, p_14345_) -> {
         $$1.appendCodePoint(p_14345_);
         return true;
      });
      return $$1.toString();
   }

   public static String getPlainText(FormattedText p_14327_) {
      StringBuilder $$1 = new StringBuilder();
      iterateFormatted(p_14327_, Style.EMPTY, (p_14323_, p_14324_, p_14325_) -> {
         $$1.appendCodePoint(p_14325_);
         return true;
      });
      return $$1.toString();
   }
}
