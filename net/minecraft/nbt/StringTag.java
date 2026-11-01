package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Optional;

public record StringTag(String value) implements PrimitiveTag {
   private static final int SELF_SIZE_IN_BYTES = 36;
   public static final TagType<StringTag> TYPE = new TagType.VariableSize<StringTag>() {
      public StringTag load(DataInput p_129320_, NbtAccounter p_129322_) throws IOException {
         return StringTag.valueOf(readAccounted(p_129320_, p_129322_));
      }

      @Override
      public StreamTagVisitor.ValueResult parse(DataInput p_197570_, StreamTagVisitor p_197571_, NbtAccounter p_302336_) throws IOException {
         return p_197571_.visit(readAccounted(p_197570_, p_302336_));
      }

      private static String readAccounted(DataInput p_302398_, NbtAccounter p_302365_) throws IOException {
         p_302365_.accountBytes(36L);
         String s = p_302398_.readUTF();
         p_302365_.readUTF(s);
         return s;
      }

      @Override
      public void skip(DataInput p_197568_, NbtAccounter p_302356_) throws IOException {
         StringTag.skipString(p_197568_);
      }

      @Override
      public String getName() {
         return "STRING";
      }

      @Override
      public String getPrettyName() {
         return "TAG_String";
      }
   };
   private static final StringTag EMPTY = new StringTag("");
   private static final char DOUBLE_QUOTE = '"';
   private static final char SINGLE_QUOTE = '\'';
   private static final char ESCAPE = '\\';
   private static final char NOT_SET = '\u0000';

   @Deprecated(
      forRemoval = true
   )
   public StringTag(String value) {
      this.value = value;
   }

   public static void skipString(DataInput p_197564_) throws IOException {
      p_197564_.skipBytes(p_197564_.readUnsignedShort());
   }

   public static StringTag valueOf(String p_129298_) {
      return p_129298_.isEmpty() ? EMPTY : new StringTag(p_129298_);
   }

   @Override
   public void write(DataOutput p_129296_) throws IOException {
      p_129296_.writeUTF(this.value);
   }

   @Override
   public int sizeInBytes() {
      return 36 + 2 * this.value.length();
   }

   @Override
   public byte getId() {
      return 8;
   }

   @Override
   public TagType<StringTag> getType() {
      return TYPE;
   }

   @Override
   public String toString() {
      StringTagVisitor stringtagvisitor = new StringTagVisitor();
      stringtagvisitor.visitString(this);
      return stringtagvisitor.build();
   }

   public StringTag copy() {
      return this;
   }

   @Override
   public Optional<String> asString() {
      return Optional.of(this.value);
   }

   @Override
   public void accept(TagVisitor p_178154_) {
      p_178154_.visitString(this);
   }

   public static String quoteAndEscape(String p_129304_) {
      StringBuilder stringbuilder = new StringBuilder();
      quoteAndEscape(p_129304_, stringbuilder);
      return stringbuilder.toString();
   }

   public static void quoteAndEscape(String p_410719_, StringBuilder p_409607_) {
      int i = p_409607_.length();
      p_409607_.append(' ');
      char c0 = 0;

      for (int j = 0; j < p_410719_.length(); j++) {
         char c1 = p_410719_.charAt(j);
         if (c1 == '\\') {
            p_409607_.append("\\\\");
         } else if (c1 != '"' && c1 != '\'') {
            String s = SnbtGrammar.escapeControlCharacters(c1);
            if (s != null) {
               p_409607_.append('\\');
               p_409607_.append(s);
            } else {
               p_409607_.append(c1);
            }
         } else {
            if (c0 == 0) {
               c0 = (char)(c1 == '"' ? 39 : 34);
            }

            if (c0 == c1) {
               p_409607_.append('\\');
            }

            p_409607_.append(c1);
         }
      }

      if (c0 == 0) {
         c0 = '"';
      }

      p_409607_.setCharAt(i, c0);
      p_409607_.append(c0);
   }

   public static String escapeWithoutQuotes(String p_428687_) {
      StringBuilder stringbuilder = new StringBuilder();
      escapeWithoutQuotes(p_428687_, stringbuilder);
      return stringbuilder.toString();
   }

   public static void escapeWithoutQuotes(String p_428715_, StringBuilder p_428716_) {
      for (int i = 0; i < p_428715_.length(); i++) {
         char c0 = p_428715_.charAt(i);
         switch (c0) {
            case '"':
            case '\'':
            case '\\':
               p_428716_.append('\\');
               p_428716_.append(c0);
               break;
            default:
               String s = SnbtGrammar.escapeControlCharacters(c0);
               if (s != null) {
                  p_428716_.append('\\');
                  p_428716_.append(s);
               } else {
                  p_428716_.append(c0);
               }
         }
      }
   }

   @Override
   public StreamTagVisitor.ValueResult accept(StreamTagVisitor p_197566_) {
      return p_197566_.visit(this.value);
   }
}
