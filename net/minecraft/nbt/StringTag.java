package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Optional;

public record StringTag(String value) implements PrimitiveTag {
   private static final int SELF_SIZE_IN_BYTES = 36;
   public static final TagType<StringTag> TYPE = new TagType.VariableSize<StringTag>() {
      public StringTag load(DataInput dataInput, NbtAccounter nbtAccounter) throws IOException {
         return StringTag.valueOf(readAccounted(dataInput, nbtAccounter));
      }

      @Override
      public StreamTagVisitor.ValueResult parse(DataInput input, StreamTagVisitor visitor, NbtAccounter accounter) throws IOException {
         return visitor.visit(readAccounted(input, accounter));
      }

      private static String readAccounted(DataInput input, NbtAccounter nbtAccounter) throws IOException {
         nbtAccounter.accountBytes(36L);
         String string = input.readUTF();
         nbtAccounter.accountBytes(2L, string.length());
         return string;
      }

      @Override
      public void skip(DataInput input, NbtAccounter accounter) throws IOException {
         StringTag.skipString(input);
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

   public static void skipString(DataInput input) throws IOException {
      input.skipBytes(input.readUnsignedShort());
   }

   public static StringTag valueOf(String data) {
      return data.isEmpty() ? EMPTY : new StringTag(data);
   }

   @Override
   public void write(DataOutput output) throws IOException {
      output.writeUTF(this.value);
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
      StringTagVisitor stringTagVisitor = new StringTagVisitor();
      stringTagVisitor.visitString(this);
      return stringTagVisitor.build();
   }

   public StringTag copy() {
      return this;
   }

   @Override
   public Optional<String> asString() {
      return Optional.of(this.value);
   }

   @Override
   public void accept(TagVisitor visitor) {
      visitor.visitString(this);
   }

   public static String quoteAndEscape(String text) {
      StringBuilder stringBuilder = new StringBuilder();
      quoteAndEscape(text, stringBuilder);
      return stringBuilder.toString();
   }

   public static void quoteAndEscape(String text, StringBuilder stringBuilder) {
      int i = stringBuilder.length();
      stringBuilder.append(' ');
      char c = 0;

      for (int j = 0; j < text.length(); j++) {
         char d = text.charAt(j);
         if (d == '\\') {
            stringBuilder.append("\\\\");
         } else if (d != '"' && d != '\'') {
            String string = SnbtGrammar.escapeControlCharacters(d);
            if (string != null) {
               stringBuilder.append('\\');
               stringBuilder.append(string);
            } else {
               stringBuilder.append(d);
            }
         } else {
            if (c == 0) {
               c = (char)(d == '"' ? 39 : 34);
            }

            if (c == d) {
               stringBuilder.append('\\');
            }

            stringBuilder.append(d);
         }
      }

      if (c == 0) {
         c = '"';
      }

      stringBuilder.setCharAt(i, c);
      stringBuilder.append(c);
   }

   public static String escapeWithoutQuotes(String input) {
      StringBuilder stringBuilder = new StringBuilder();
      escapeWithoutQuotes(input, stringBuilder);
      return stringBuilder.toString();
   }

   public static void escapeWithoutQuotes(String input, StringBuilder stringBuilder) {
      for (int i = 0; i < input.length(); i++) {
         char c = input.charAt(i);
         switch (c) {
            case '"':
            case '\'':
            case '\\':
               stringBuilder.append('\\');
               stringBuilder.append(c);
               break;
            default:
               String string = SnbtGrammar.escapeControlCharacters(c);
               if (string != null) {
                  stringBuilder.append('\\');
                  stringBuilder.append(string);
               } else {
                  stringBuilder.append(c);
               }
         }
      }
   }

   @Override
   public StreamTagVisitor.ValueResult accept(StreamTagVisitor visitor) {
      return visitor.visit(this.value);
   }
}
