package net.minecraft.nbt;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.Util;

public class SnbtPrinterTagVisitor implements TagVisitor {
   private static final Map<String, List<String>> KEY_ORDER = Util.make(Maps.newHashMap(), p_178114_ -> {
      p_178114_.put("{}", Lists.newArrayList(new String[]{"DataVersion", "author", "size", "data", "entities", "palette", "palettes"}));
      p_178114_.put("{}.data.[].{}", Lists.newArrayList(new String[]{"pos", "state", "nbt"}));
      p_178114_.put("{}.entities.[].{}", Lists.newArrayList(new String[]{"blockPos", "pos"}));
   });
   private static final Set<String> NO_INDENTATION = Sets.newHashSet(new String[]{"{}.size.[]", "{}.data.[].{}", "{}.palette.[].{}", "{}.entities.[].{}"});
   private static final Pattern SIMPLE_VALUE = Pattern.compile("[A-Za-z0-9._+-]+");
   private static final String NAME_VALUE_SEPARATOR = String.valueOf(':');
   private static final String ELEMENT_SEPARATOR = String.valueOf(',');
   private static final String LIST_OPEN = "[";
   private static final String LIST_CLOSE = "]";
   private static final String LIST_TYPE_SEPARATOR = ";";
   private static final String ELEMENT_SPACING = " ";
   private static final String STRUCT_OPEN = "{";
   private static final String STRUCT_CLOSE = "}";
   private static final String NEWLINE = "\n";
   private final String indentation;
   private final int depth;
   private final List<String> path;
   private String result = "";

   public SnbtPrinterTagVisitor() {
      this("    ", 0, Lists.newArrayList());
   }

   public SnbtPrinterTagVisitor(String p_178107_, int p_178108_, List<String> p_178109_) {
      this.indentation = p_178107_;
      this.depth = p_178108_;
      this.path = p_178109_;
   }

   public String visit(Tag p_178142_) {
      p_178142_.accept(this);
      return this.result;
   }

   @Override
   public void visitString(StringTag p_178140_) {
      this.result = StringTag.quoteAndEscape(p_178140_.getAsString());
   }

   @Override
   public void visitByte(ByteTag p_178118_) {
      this.result = p_178118_.getAsNumber() + "b";
   }

   @Override
   public void visitShort(ShortTag p_178138_) {
      this.result = p_178138_.getAsNumber() + "s";
   }

   @Override
   public void visitInt(IntTag p_178130_) {
      this.result = String.valueOf(p_178130_.getAsNumber());
   }

   @Override
   public void visitLong(LongTag p_178136_) {
      this.result = p_178136_.getAsNumber() + "L";
   }

   @Override
   public void visitFloat(FloatTag p_178126_) {
      this.result = p_178126_.getAsFloat() + "f";
   }

   @Override
   public void visitDouble(DoubleTag p_178122_) {
      this.result = p_178122_.getAsDouble() + "d";
   }

   @Override
   public void visitByteArray(ByteArrayTag p_178116_) {
      StringBuilder $$1 = new StringBuilder("[").append("B").append(";");
      byte[] $$2 = p_178116_.getAsByteArray();

      for (int $$3 = 0; $$3 < $$2.length; $$3++) {
         $$1.append(" ").append($$2[$$3]).append("B");
         if ($$3 != $$2.length - 1) {
            $$1.append(ELEMENT_SEPARATOR);
         }
      }

      $$1.append("]");
      this.result = $$1.toString();
   }

   @Override
   public void visitIntArray(IntArrayTag p_178128_) {
      StringBuilder $$1 = new StringBuilder("[").append("I").append(";");
      int[] $$2 = p_178128_.getAsIntArray();

      for (int $$3 = 0; $$3 < $$2.length; $$3++) {
         $$1.append(" ").append($$2[$$3]);
         if ($$3 != $$2.length - 1) {
            $$1.append(ELEMENT_SEPARATOR);
         }
      }

      $$1.append("]");
      this.result = $$1.toString();
   }

   @Override
   public void visitLongArray(LongArrayTag p_178134_) {
      String $$1 = "L";
      StringBuilder $$2 = new StringBuilder("[").append("L").append(";");
      long[] $$3 = p_178134_.getAsLongArray();

      for (int $$4 = 0; $$4 < $$3.length; $$4++) {
         $$2.append(" ").append($$3[$$4]).append("L");
         if ($$4 != $$3.length - 1) {
            $$2.append(ELEMENT_SEPARATOR);
         }
      }

      $$2.append("]");
      this.result = $$2.toString();
   }

   @Override
   public void visitList(ListTag p_178132_) {
      if (p_178132_.isEmpty()) {
         this.result = "[]";
      } else {
         StringBuilder $$1 = new StringBuilder("[");
         this.pushPath("[]");
         String $$2 = NO_INDENTATION.contains(this.pathString()) ? "" : this.indentation;
         if (!$$2.isEmpty()) {
            $$1.append("\n");
         }

         for (int $$3 = 0; $$3 < p_178132_.size(); $$3++) {
            $$1.append(Strings.repeat($$2, this.depth + 1));
            $$1.append(new SnbtPrinterTagVisitor($$2, this.depth + 1, this.path).visit(p_178132_.get($$3)));
            if ($$3 != p_178132_.size() - 1) {
               $$1.append(ELEMENT_SEPARATOR).append($$2.isEmpty() ? " " : "\n");
            }
         }

         if (!$$2.isEmpty()) {
            $$1.append("\n").append(Strings.repeat($$2, this.depth));
         }

         $$1.append("]");
         this.result = $$1.toString();
         this.popPath();
      }
   }

   @Override
   public void visitCompound(CompoundTag p_178120_) {
      if (p_178120_.isEmpty()) {
         this.result = "{}";
      } else {
         StringBuilder $$1 = new StringBuilder("{");
         this.pushPath("{}");
         String $$2 = NO_INDENTATION.contains(this.pathString()) ? "" : this.indentation;
         if (!$$2.isEmpty()) {
            $$1.append("\n");
         }

         Collection<String> $$3 = this.getKeys(p_178120_);
         Iterator<String> $$4 = $$3.iterator();

         while ($$4.hasNext()) {
            String $$5 = $$4.next();
            Tag $$6 = p_178120_.get($$5);
            this.pushPath($$5);
            $$1.append(Strings.repeat($$2, this.depth + 1))
               .append(handleEscapePretty($$5))
               .append(NAME_VALUE_SEPARATOR)
               .append(" ")
               .append(new SnbtPrinterTagVisitor($$2, this.depth + 1, this.path).visit($$6));
            this.popPath();
            if ($$4.hasNext()) {
               $$1.append(ELEMENT_SEPARATOR).append($$2.isEmpty() ? " " : "\n");
            }
         }

         if (!$$2.isEmpty()) {
            $$1.append("\n").append(Strings.repeat($$2, this.depth));
         }

         $$1.append("}");
         this.result = $$1.toString();
         this.popPath();
      }
   }

   private void popPath() {
      this.path.remove(this.path.size() - 1);
   }

   private void pushPath(String p_178145_) {
      this.path.add(p_178145_);
   }

   protected List<String> getKeys(CompoundTag p_178147_) {
      Set<String> $$1 = Sets.newHashSet(p_178147_.getAllKeys());
      List<String> $$2 = Lists.newArrayList();
      List<String> $$3 = KEY_ORDER.get(this.pathString());
      if ($$3 != null) {
         for (String $$4 : $$3) {
            if ($$1.remove($$4)) {
               $$2.add($$4);
            }
         }

         if (!$$1.isEmpty()) {
            $$1.stream().sorted().forEach($$2::add);
         }
      } else {
         $$2.addAll($$1);
         Collections.sort($$2);
      }

      return $$2;
   }

   public String pathString() {
      return String.join(".", this.path);
   }

   protected static String handleEscapePretty(String p_178112_) {
      return SIMPLE_VALUE.matcher(p_178112_).matches() ? p_178112_ : StringTag.quoteAndEscape(p_178112_);
   }

   @Override
   public void visitEnd(EndTag p_178124_) {
   }
}
