package net.minecraft.nbt;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.bytes.ByteCollection;
import it.unimi.dsi.fastutil.bytes.ByteOpenHashSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.slf4j.Logger;

public class TextComponentTagVisitor implements TagVisitor {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int INLINE_LIST_THRESHOLD = 8;
   private static final int MAX_DEPTH = 64;
   private static final int MAX_LENGTH = 128;
   private static final ByteCollection INLINE_ELEMENT_TYPES = new ByteOpenHashSet(Arrays.asList((byte)1, (byte)2, (byte)3, (byte)4, (byte)5, (byte)6));
   private static final ChatFormatting SYNTAX_HIGHLIGHTING_KEY = ChatFormatting.AQUA;
   private static final ChatFormatting SYNTAX_HIGHLIGHTING_STRING = ChatFormatting.GREEN;
   private static final ChatFormatting SYNTAX_HIGHLIGHTING_NUMBER = ChatFormatting.GOLD;
   private static final ChatFormatting SYNTAX_HIGHLIGHTING_NUMBER_TYPE = ChatFormatting.RED;
   private static final Pattern SIMPLE_VALUE = Pattern.compile("[A-Za-z0-9._+-]+");
   private static final String LIST_OPEN = "[";
   private static final String LIST_CLOSE = "]";
   private static final String LIST_TYPE_SEPARATOR = ";";
   private static final String ELEMENT_SPACING = " ";
   private static final String STRUCT_OPEN = "{";
   private static final String STRUCT_CLOSE = "}";
   private static final String NEWLINE = "\n";
   private static final String NAME_VALUE_SEPARATOR = ": ";
   private static final String ELEMENT_SEPARATOR = String.valueOf(',');
   private static final String WRAPPED_ELEMENT_SEPARATOR = ELEMENT_SEPARATOR + "\n";
   private static final String SPACED_ELEMENT_SEPARATOR = ELEMENT_SEPARATOR + " ";
   private static final Component FOLDED = Component.literal("<...>").withStyle(ChatFormatting.GRAY);
   private static final Component BYTE_TYPE = Component.literal("b").withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
   private static final Component SHORT_TYPE = Component.literal("s").withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
   private static final Component INT_TYPE = Component.literal("I").withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
   private static final Component LONG_TYPE = Component.literal("L").withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
   private static final Component FLOAT_TYPE = Component.literal("f").withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
   private static final Component DOUBLE_TYPE = Component.literal("d").withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
   private static final Component BYTE_ARRAY_TYPE = Component.literal("B").withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
   private final String indentation;
   private int indentDepth;
   private int depth;
   private final MutableComponent result = Component.empty();

   public TextComponentTagVisitor(String p_178251_) {
      this.indentation = p_178251_;
   }

   public Component visit(Tag p_178282_) {
      p_178282_.accept(this);
      return this.result;
   }

   @Override
   public void visitString(StringTag p_178280_) {
      String $$1 = StringTag.quoteAndEscape(p_178280_.getAsString());
      String $$2 = $$1.substring(0, 1);
      Component $$3 = Component.literal($$1.substring(1, $$1.length() - 1)).withStyle(SYNTAX_HIGHLIGHTING_STRING);
      this.result.append($$2).append($$3).append($$2);
   }

   @Override
   public void visitByte(ByteTag p_178258_) {
      this.result.append(Component.literal(String.valueOf(p_178258_.getAsNumber())).withStyle(SYNTAX_HIGHLIGHTING_NUMBER)).append(BYTE_TYPE);
   }

   @Override
   public void visitShort(ShortTag p_178278_) {
      this.result.append(Component.literal(String.valueOf(p_178278_.getAsNumber())).withStyle(SYNTAX_HIGHLIGHTING_NUMBER)).append(SHORT_TYPE);
   }

   @Override
   public void visitInt(IntTag p_178270_) {
      this.result.append(Component.literal(String.valueOf(p_178270_.getAsNumber())).withStyle(SYNTAX_HIGHLIGHTING_NUMBER));
   }

   @Override
   public void visitLong(LongTag p_178276_) {
      this.result.append(Component.literal(String.valueOf(p_178276_.getAsNumber())).withStyle(SYNTAX_HIGHLIGHTING_NUMBER)).append(LONG_TYPE);
   }

   @Override
   public void visitFloat(FloatTag p_178266_) {
      this.result.append(Component.literal(String.valueOf(p_178266_.getAsFloat())).withStyle(SYNTAX_HIGHLIGHTING_NUMBER)).append(FLOAT_TYPE);
   }

   @Override
   public void visitDouble(DoubleTag p_178262_) {
      this.result.append(Component.literal(String.valueOf(p_178262_.getAsDouble())).withStyle(SYNTAX_HIGHLIGHTING_NUMBER)).append(DOUBLE_TYPE);
   }

   @Override
   public void visitByteArray(ByteArrayTag p_178256_) {
      this.result.append("[").append(BYTE_ARRAY_TYPE).append(";");
      byte[] $$1 = p_178256_.getAsByteArray();

      for (int $$2 = 0; $$2 < $$1.length && $$2 < 128; $$2++) {
         MutableComponent $$3 = Component.literal(String.valueOf($$1[$$2])).withStyle(SYNTAX_HIGHLIGHTING_NUMBER);
         this.result.append(" ").append($$3).append(BYTE_ARRAY_TYPE);
         if ($$2 != $$1.length - 1) {
            this.result.append(ELEMENT_SEPARATOR);
         }
      }

      if ($$1.length > 128) {
         this.result.append(FOLDED);
      }

      this.result.append("]");
   }

   @Override
   public void visitIntArray(IntArrayTag p_178268_) {
      this.result.append("[").append(INT_TYPE).append(";");
      int[] $$1 = p_178268_.getAsIntArray();

      for (int $$2 = 0; $$2 < $$1.length && $$2 < 128; $$2++) {
         this.result.append(" ").append(Component.literal(String.valueOf($$1[$$2])).withStyle(SYNTAX_HIGHLIGHTING_NUMBER));
         if ($$2 != $$1.length - 1) {
            this.result.append(ELEMENT_SEPARATOR);
         }
      }

      if ($$1.length > 128) {
         this.result.append(FOLDED);
      }

      this.result.append("]");
   }

   @Override
   public void visitLongArray(LongArrayTag p_178274_) {
      this.result.append("[").append(LONG_TYPE).append(";");
      long[] $$1 = p_178274_.getAsLongArray();

      for (int $$2 = 0; $$2 < $$1.length && $$2 < 128; $$2++) {
         Component $$3 = Component.literal(String.valueOf($$1[$$2])).withStyle(SYNTAX_HIGHLIGHTING_NUMBER);
         this.result.append(" ").append($$3).append(LONG_TYPE);
         if ($$2 != $$1.length - 1) {
            this.result.append(ELEMENT_SEPARATOR);
         }
      }

      if ($$1.length > 128) {
         this.result.append(FOLDED);
      }

      this.result.append("]");
   }

   @Override
   public void visitList(ListTag p_178272_) {
      if (p_178272_.isEmpty()) {
         this.result.append("[]");
      } else if (this.depth >= 64) {
         this.result.append("[").append(FOLDED).append("]");
      } else if (INLINE_ELEMENT_TYPES.contains(p_178272_.getElementType()) && p_178272_.size() <= 8) {
         this.result.append("[");

         for (int $$1 = 0; $$1 < p_178272_.size(); $$1++) {
            if ($$1 != 0) {
               this.result.append(SPACED_ELEMENT_SEPARATOR);
            }

            this.appendSubTag(p_178272_.get($$1), false);
         }

         this.result.append("]");
      } else {
         this.result.append("[");
         if (!this.indentation.isEmpty()) {
            this.result.append("\n");
         }

         String $$2 = Strings.repeat(this.indentation, this.indentDepth + 1);

         for (int $$3 = 0; $$3 < p_178272_.size() && $$3 < 128; $$3++) {
            this.result.append($$2);
            this.appendSubTag(p_178272_.get($$3), true);
            if ($$3 != p_178272_.size() - 1) {
               this.result.append(this.indentation.isEmpty() ? SPACED_ELEMENT_SEPARATOR : WRAPPED_ELEMENT_SEPARATOR);
            }
         }

         if (p_178272_.size() > 128) {
            this.result.append($$2).append(FOLDED);
         }

         if (!this.indentation.isEmpty()) {
            this.result.append("\n" + Strings.repeat(this.indentation, this.indentDepth));
         }

         this.result.append("]");
      }
   }

   @Override
   public void visitCompound(CompoundTag p_178260_) {
      if (p_178260_.isEmpty()) {
         this.result.append("{}");
      } else if (this.depth >= 64) {
         this.result.append("{").append(FOLDED).append("}");
      } else {
         this.result.append("{");
         Collection<String> $$1 = p_178260_.getAllKeys();
         if (LOGGER.isDebugEnabled()) {
            List<String> $$2 = Lists.newArrayList(p_178260_.getAllKeys());
            Collections.sort($$2);
            $$1 = $$2;
         }

         if (!this.indentation.isEmpty()) {
            this.result.append("\n");
         }

         String $$3 = Strings.repeat(this.indentation, this.indentDepth + 1);
         Iterator<String> $$4 = $$1.iterator();

         while ($$4.hasNext()) {
            String $$5 = $$4.next();
            this.result.append($$3).append(handleEscapePretty($$5)).append(": ");
            this.appendSubTag(p_178260_.get($$5), true);
            if ($$4.hasNext()) {
               this.result.append(this.indentation.isEmpty() ? SPACED_ELEMENT_SEPARATOR : WRAPPED_ELEMENT_SEPARATOR);
            }
         }

         if (!this.indentation.isEmpty()) {
            this.result.append("\n" + Strings.repeat(this.indentation, this.indentDepth));
         }

         this.result.append("}");
      }
   }

   private void appendSubTag(Tag p_347469_, boolean p_347632_) {
      if (p_347632_) {
         this.indentDepth++;
      }

      this.depth++;

      try {
         p_347469_.accept(this);
      } finally {
         if (p_347632_) {
            this.indentDepth--;
         }

         this.depth--;
      }
   }

   protected static Component handleEscapePretty(String p_178254_) {
      if (SIMPLE_VALUE.matcher(p_178254_).matches()) {
         return Component.literal(p_178254_).withStyle(SYNTAX_HIGHLIGHTING_KEY);
      } else {
         String $$1 = StringTag.quoteAndEscape(p_178254_);
         String $$2 = $$1.substring(0, 1);
         Component $$3 = Component.literal($$1.substring(1, $$1.length() - 1)).withStyle(SYNTAX_HIGHLIGHTING_KEY);
         return Component.literal($$2).append($$3).append($$2);
      }
   }

   @Override
   public void visitEnd(EndTag p_178264_) {
   }
}
