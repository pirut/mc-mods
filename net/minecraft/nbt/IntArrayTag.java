package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import org.apache.commons.lang3.ArrayUtils;

public final class IntArrayTag implements CollectionTag {
   private static final int SELF_SIZE_IN_BYTES = 24;
   public static final TagType<IntArrayTag> TYPE = new TagType.VariableSize<IntArrayTag>() {
      public IntArrayTag load(DataInput dataInput, NbtAccounter nbtAccounter) throws IOException {
         return new IntArrayTag(readAccounted(dataInput, nbtAccounter));
      }

      @Override
      public StreamTagVisitor.ValueResult parse(DataInput input, StreamTagVisitor visitor, NbtAccounter accounter) throws IOException {
         return visitor.visit(readAccounted(input, accounter));
      }

      private static int[] readAccounted(DataInput input, NbtAccounter accounter) throws IOException {
         accounter.accountBytes(24L);
         int i = input.readInt();
         accounter.accountBytes(4L, i);
         int[] is = new int[i];

         for (int j = 0; j < i; j++) {
            is[j] = input.readInt();
         }

         return is;
      }

      @Override
      public void skip(DataInput input, NbtAccounter accounter) throws IOException {
         input.skipBytes(input.readInt() * 4);
      }

      @Override
      public String getName() {
         return "INT[]";
      }

      @Override
      public String getPrettyName() {
         return "TAG_Int_Array";
      }
   };
   private int[] data;

   public IntArrayTag(int[] data) {
      this.data = data;
   }

   @Override
   public void write(DataOutput output) throws IOException {
      output.writeInt(this.data.length);

      for (int i : this.data) {
         output.writeInt(i);
      }
   }

   @Override
   public int sizeInBytes() {
      return 24 + 4 * this.data.length;
   }

   @Override
   public byte getId() {
      return 11;
   }

   @Override
   public TagType<IntArrayTag> getType() {
      return TYPE;
   }

   @Override
   public String toString() {
      StringTagVisitor stringTagVisitor = new StringTagVisitor();
      stringTagVisitor.visitIntArray(this);
      return stringTagVisitor.build();
   }

   public IntArrayTag copy() {
      int[] is = new int[this.data.length];
      System.arraycopy(this.data, 0, is, 0, this.data.length);
      return new IntArrayTag(is);
   }

   @Override
   public boolean equals(Object object) {
      return this == object ? true : object instanceof IntArrayTag && Arrays.equals(this.data, ((IntArrayTag)object).data);
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.data);
   }

   public int[] getAsIntArray() {
      return this.data;
   }

   @Override
   public void accept(TagVisitor visitor) {
      visitor.visitIntArray(this);
   }

   @Override
   public int size() {
      return this.data.length;
   }

   public IntTag get(int index) {
      return IntTag.valueOf(this.data[index]);
   }

   @Override
   public boolean setTag(int index, Tag tag) {
      if (tag instanceof NumericTag numericTag) {
         this.data[index] = numericTag.intValue();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean addTag(int index, Tag tag) {
      if (tag instanceof NumericTag numericTag) {
         this.data = ArrayUtils.add(this.data, index, numericTag.intValue());
         return true;
      } else {
         return false;
      }
   }

   public IntTag remove(int i) {
      int j = this.data[i];
      this.data = ArrayUtils.remove(this.data, i);
      return IntTag.valueOf(j);
   }

   @Override
   public void clear() {
      this.data = new int[0];
   }

   @Override
   public Optional<int[]> asIntArray() {
      return Optional.of(this.data);
   }

   @Override
   public StreamTagVisitor.ValueResult accept(StreamTagVisitor visitor) {
      return visitor.visit(this.data);
   }
}
