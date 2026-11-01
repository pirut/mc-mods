package net.minecraft.nbt;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class CompoundTag implements Tag {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<CompoundTag> CODEC = Codec.PASSTHROUGH
      .comapFlatMap(
         p_311527_ -> {
            Tag tag = (Tag)p_311527_.convert(NbtOps.INSTANCE).getValue();
            return tag instanceof CompoundTag compoundtag
               ? DataResult.success(compoundtag == p_311527_.getValue() ? compoundtag.copy() : compoundtag)
               : DataResult.error(() -> "Not a compound tag: " + tag);
         },
         p_311526_ -> new Dynamic(NbtOps.INSTANCE, p_311526_.copy())
      );
   private static final int SELF_SIZE_IN_BYTES = 48;
   private static final int MAP_ENTRY_SIZE_IN_BYTES = 32;
   public static final TagType<CompoundTag> TYPE = new TagType.VariableSize<CompoundTag>() {
      public CompoundTag load(DataInput p_128485_, NbtAccounter p_128487_) throws IOException {
         p_128487_.pushDepth();

         CompoundTag compoundtag;
         try {
            compoundtag = loadCompound(p_128485_, p_128487_);
         } finally {
            p_128487_.popDepth();
         }

         return compoundtag;
      }

      private static byte readNamedTagType(DataInput p_302338_, NbtAccounter p_302362_) throws IOException {
         p_302362_.accountBytes(2L);
         return p_302338_.readByte();
      }

      private static CompoundTag loadCompound(DataInput p_302338_, NbtAccounter p_302362_) throws IOException {
         p_302362_.accountBytes(48L);
         Map<String, Tag> map = Maps.newHashMap();

         byte b0;
         while ((b0 = readNamedTagType(p_302338_, p_302362_)) != 0) {
            String s = p_302362_.readUTF(p_302338_.readUTF());
            p_302362_.accountBytes(4L);
            Tag tag = CompoundTag.readNamedTagData(TagTypes.getType(b0), s, p_302338_, p_302362_);
            if (map.put(s, tag) == null) {
               p_302362_.accountBytes(36L);
            }
         }

         return new CompoundTag(map);
      }

      @Override
      public StreamTagVisitor.ValueResult parse(DataInput p_197446_, StreamTagVisitor p_197447_, NbtAccounter p_302322_) throws IOException {
         p_302322_.pushDepth();

         StreamTagVisitor.ValueResult streamtagvisitor$valueresult;
         try {
            streamtagvisitor$valueresult = parseCompound(p_197446_, p_197447_, p_302322_);
         } finally {
            p_302322_.popDepth();
         }

         return streamtagvisitor$valueresult;
      }

      private static StreamTagVisitor.ValueResult parseCompound(DataInput p_302325_, StreamTagVisitor p_302352_, NbtAccounter p_302355_) throws IOException {
         p_302355_.accountBytes(48L);

         byte b0;
         label35:
         while ((b0 = p_302325_.readByte()) != 0) {
            TagType<?> tagtype = TagTypes.getType(b0);
            switch (p_302352_.visitEntry(tagtype)) {
               case HALT:
                  return StreamTagVisitor.ValueResult.HALT;
               case BREAK:
                  StringTag.skipString(p_302325_);
                  tagtype.skip(p_302325_, p_302355_);
                  break label35;
               case SKIP:
                  StringTag.skipString(p_302325_);
                  tagtype.skip(p_302325_, p_302355_);
                  break;
               default:
                  String s = readString(p_302325_, p_302355_);
                  switch (p_302352_.visitEntry(tagtype, s)) {
                     case HALT:
                        return StreamTagVisitor.ValueResult.HALT;
                     case BREAK:
                        tagtype.skip(p_302325_, p_302355_);
                        break label35;
                     case SKIP:
                        tagtype.skip(p_302325_, p_302355_);
                        break;
                     default:
                        p_302355_.accountBytes(36L);
                        switch (tagtype.parse(p_302325_, p_302352_, p_302355_)) {
                           case HALT:
                              return StreamTagVisitor.ValueResult.HALT;
                           case BREAK:
                        }
                  }
            }
         }

         if (b0 != 0) {
            while ((b0 = p_302325_.readByte()) != 0) {
               StringTag.skipString(p_302325_);
               TagTypes.getType(b0).skip(p_302325_, p_302355_);
            }
         }

         return p_302352_.visitContainerEnd();
      }

      private static String readString(DataInput p_302484_, NbtAccounter p_302494_) throws IOException {
         String s = p_302484_.readUTF();
         p_302494_.accountBytes(28L);
         p_302494_.accountBytes(2L, s.length());
         return s;
      }

      @Override
      public void skip(DataInput p_197444_, NbtAccounter p_302358_) throws IOException {
         p_302358_.pushDepth();

         byte b0;
         try {
            while ((b0 = p_197444_.readByte()) != 0) {
               StringTag.skipString(p_197444_);
               TagTypes.getType(b0).skip(p_197444_, p_302358_);
            }
         } finally {
            p_302358_.popDepth();
         }
      }

      @Override
      public String getName() {
         return "COMPOUND";
      }

      @Override
      public String getPrettyName() {
         return "TAG_Compound";
      }
   };
   private final Map<String, Tag> tags;

   CompoundTag(Map<String, Tag> p_128333_) {
      this.tags = p_128333_;
   }

   public CompoundTag() {
      this(new HashMap<>());
   }

   public CompoundTag(int expectedEntries) {
      this(HashMap.newHashMap(expectedEntries));
   }

   @Override
   public void write(DataOutput p_128341_) throws IOException {
      for (String s : this.tags.keySet()) {
         Tag tag = this.tags.get(s);
         writeNamedTag(s, tag, p_128341_);
      }

      p_128341_.writeByte(0);
   }

   @Override
   public int sizeInBytes() {
      int i = 48;

      for (Entry<String, Tag> entry : this.tags.entrySet()) {
         i += 28 + 2 * entry.getKey().length();
         i += 36;
         i += entry.getValue().sizeInBytes();
      }

      return i;
   }

   public Set<String> keySet() {
      return this.tags.keySet();
   }

   public Set<Entry<String, Tag>> entrySet() {
      return this.tags.entrySet();
   }

   public Collection<Tag> values() {
      return this.tags.values();
   }

   public void forEach(BiConsumer<String, Tag> p_409767_) {
      this.tags.forEach(p_409767_);
   }

   @Override
   public byte getId() {
      return 10;
   }

   @Override
   public TagType<CompoundTag> getType() {
      return TYPE;
   }

   public int size() {
      return this.tags.size();
   }

   @Nullable
   public Tag put(String p_128366_, Tag p_128367_) {
      if (p_128367_ == null) {
         throw new IllegalArgumentException("Invalid null NBT value with key " + p_128366_);
      } else {
         return this.tags.put(p_128366_, p_128367_);
      }
   }

   public void putByte(String p_128345_, byte p_128346_) {
      this.tags.put(p_128345_, ByteTag.valueOf(p_128346_));
   }

   public void putShort(String p_128377_, short p_128378_) {
      this.tags.put(p_128377_, ShortTag.valueOf(p_128378_));
   }

   public void putInt(String p_128406_, int p_128407_) {
      this.tags.put(p_128406_, IntTag.valueOf(p_128407_));
   }

   public void putLong(String p_128357_, long p_128358_) {
      this.tags.put(p_128357_, LongTag.valueOf(p_128358_));
   }

   public void putFloat(String p_128351_, float p_128352_) {
      this.tags.put(p_128351_, FloatTag.valueOf(p_128352_));
   }

   public void putDouble(String p_128348_, double p_128349_) {
      this.tags.put(p_128348_, DoubleTag.valueOf(p_128349_));
   }

   public void putString(String p_128360_, String p_128361_) {
      this.tags.put(p_128360_, StringTag.valueOf(p_128361_));
   }

   public void putByteArray(String p_128383_, byte[] p_128384_) {
      this.tags.put(p_128383_, new ByteArrayTag(p_128384_));
   }

   public void putIntArray(String p_128386_, int[] p_128387_) {
      this.tags.put(p_128386_, new IntArrayTag(p_128387_));
   }

   public void putLongArray(String p_128389_, long[] p_128390_) {
      this.tags.put(p_128389_, new LongArrayTag(p_128390_));
   }

   public void putBoolean(String p_128380_, boolean p_128381_) {
      this.tags.put(p_128380_, ByteTag.valueOf(p_128381_));
   }

   @Nullable
   public Tag get(String p_128424_) {
      return this.tags.get(p_128424_);
   }

   public boolean contains(String p_128442_) {
      return this.tags.containsKey(p_128442_);
   }

   private Optional<Tag> getOptional(String p_409622_) {
      return Optional.ofNullable(this.tags.get(p_409622_));
   }

   public Optional<Byte> getByte(String p_128446_) {
      return this.getOptional(p_128446_).flatMap(Tag::asByte);
   }

   public byte getByteOr(String p_410151_, byte p_409929_) {
      return this.tags.get(p_410151_) instanceof NumericTag numerictag ? numerictag.byteValue() : p_409929_;
   }

   public Optional<Short> getShort(String p_128449_) {
      return this.getOptional(p_128449_).flatMap(Tag::asShort);
   }

   public short getShortOr(String p_409927_, short p_410172_) {
      return this.tags.get(p_409927_) instanceof NumericTag numerictag ? numerictag.shortValue() : p_410172_;
   }

   public Optional<Integer> getInt(String p_128452_) {
      return this.getOptional(p_128452_).flatMap(Tag::asInt);
   }

   public int getIntOr(String p_410742_, int p_410613_) {
      return this.tags.get(p_410742_) instanceof NumericTag numerictag ? numerictag.intValue() : p_410613_;
   }

   public Optional<Long> getLong(String p_128455_) {
      return this.getOptional(p_128455_).flatMap(Tag::asLong);
   }

   public long getLongOr(String p_409903_, long p_409891_) {
      return this.tags.get(p_409903_) instanceof NumericTag numerictag ? numerictag.longValue() : p_409891_;
   }

   public Optional<Float> getFloat(String p_128458_) {
      return this.getOptional(p_128458_).flatMap(Tag::asFloat);
   }

   public float getFloatOr(String p_410440_, float p_410371_) {
      return this.tags.get(p_410440_) instanceof NumericTag numerictag ? numerictag.floatValue() : p_410371_;
   }

   public Optional<Double> getDouble(String p_128460_) {
      return this.getOptional(p_128460_).flatMap(Tag::asDouble);
   }

   public double getDoubleOr(String p_410157_, double p_410495_) {
      return this.tags.get(p_410157_) instanceof NumericTag numerictag ? numerictag.doubleValue() : p_410495_;
   }

   public Optional<String> getString(String p_128462_) {
      return this.getOptional(p_128462_).flatMap(Tag::asString);
   }

   public String getStringOr(String p_410368_, String p_410785_) {
      return this.tags.get(p_410368_) instanceof StringTag(String var6) ? var6 : p_410785_;
   }

   public Optional<byte[]> getByteArray(String p_128464_) {
      return this.tags.get(p_128464_) instanceof ByteArrayTag bytearraytag ? Optional.of(bytearraytag.getAsByteArray()) : Optional.empty();
   }

   public Optional<int[]> getIntArray(String p_128466_) {
      return this.tags.get(p_128466_) instanceof IntArrayTag intarraytag ? Optional.of(intarraytag.getAsIntArray()) : Optional.empty();
   }

   public Optional<long[]> getLongArray(String p_128468_) {
      return this.tags.get(p_128468_) instanceof LongArrayTag longarraytag ? Optional.of(longarraytag.getAsLongArray()) : Optional.empty();
   }

   public Optional<CompoundTag> getCompound(String p_128470_) {
      return this.tags.get(p_128470_) instanceof CompoundTag compoundtag ? Optional.of(compoundtag) : Optional.empty();
   }

   public CompoundTag getCompoundOrEmpty(String p_410263_) {
      return this.getCompound(p_410263_).orElseGet(CompoundTag::new);
   }

   public Optional<ListTag> getList(String p_128438_) {
      return this.tags.get(p_128438_) instanceof ListTag listtag ? Optional.of(listtag) : Optional.empty();
   }

   public ListTag getListOrEmpty(String p_409758_) {
      return this.getList(p_409758_).orElseGet(ListTag::new);
   }

   public Optional<Boolean> getBoolean(String p_128472_) {
      return this.getOptional(p_128472_).flatMap(Tag::asBoolean);
   }

   public boolean getBooleanOr(String p_409730_, boolean p_410716_) {
      return this.getByteOr(p_409730_, (byte)(p_410716_ ? 1 : 0)) != 0;
   }

   @Nullable
   public Tag remove(String p_128474_) {
      return this.tags.remove(p_128474_);
   }

   @Override
   public String toString() {
      StringTagVisitor stringtagvisitor = new StringTagVisitor();
      stringtagvisitor.visitCompound(this);
      return stringtagvisitor.build();
   }

   public boolean isEmpty() {
      return this.tags.isEmpty();
   }

   protected CompoundTag shallowCopy() {
      return new CompoundTag(new HashMap<>(this.tags));
   }

   public CompoundTag copy() {
      HashMap<String, Tag> hashmap = new HashMap<>();
      this.tags.forEach((p_411026_, p_411027_) -> hashmap.put(p_411026_, p_411027_.copy()));
      return new CompoundTag(hashmap);
   }

   @Override
   public Optional<CompoundTag> asCompound() {
      return Optional.of(this);
   }

   @Override
   public boolean equals(Object p_128444_) {
      return this == p_128444_ ? true : p_128444_ instanceof CompoundTag && Objects.equals(this.tags, ((CompoundTag)p_128444_).tags);
   }

   @Override
   public int hashCode() {
      return this.tags.hashCode();
   }

   private static void writeNamedTag(String p_128369_, Tag p_128370_, DataOutput p_128371_) throws IOException {
      p_128371_.writeByte(p_128370_.getId());
      if (p_128370_.getId() != 0) {
         p_128371_.writeUTF(p_128369_);
         p_128370_.write(p_128371_);
      }
   }

   static Tag readNamedTagData(TagType<?> p_128414_, String p_128415_, DataInput p_128416_, NbtAccounter p_128418_) {
      try {
         return p_128414_.load(p_128416_, p_128418_);
      } catch (IOException var7) {
         CrashReport crashreport = CrashReport.forThrowable(var7, "Loading NBT data");
         CrashReportCategory crashreportcategory = crashreport.addCategory("NBT Tag");
         crashreportcategory.setDetail("Tag name", p_128415_);
         crashreportcategory.setDetail("Tag type", p_128414_.getName());
         throw new ReportedNbtException(crashreport);
      }
   }

   public CompoundTag merge(CompoundTag p_128392_) {
      for (String s : p_128392_.tags.keySet()) {
         Tag tag = p_128392_.tags.get(s);
         if (tag instanceof CompoundTag compoundtag && this.tags.get(s) instanceof CompoundTag compoundtag1) {
            compoundtag1.merge(compoundtag);
         } else {
            this.put(s, tag.copy());
         }
      }

      return this;
   }

   @Override
   public void accept(TagVisitor p_177857_) {
      p_177857_.visitCompound(this);
   }

   @Override
   public StreamTagVisitor.ValueResult accept(StreamTagVisitor p_197442_) {
      for (Entry<String, Tag> entry : this.tags.entrySet()) {
         Tag tag = entry.getValue();
         TagType<?> tagtype = tag.getType();
         StreamTagVisitor.EntryResult streamtagvisitor$entryresult = p_197442_.visitEntry(tagtype);
         switch (streamtagvisitor$entryresult) {
            case HALT:
               return StreamTagVisitor.ValueResult.HALT;
            case BREAK:
               return p_197442_.visitContainerEnd();
            case SKIP:
               break;
            default:
               streamtagvisitor$entryresult = p_197442_.visitEntry(tagtype, entry.getKey());
               switch (streamtagvisitor$entryresult) {
                  case HALT:
                     return StreamTagVisitor.ValueResult.HALT;
                  case BREAK:
                     return p_197442_.visitContainerEnd();
                  case SKIP:
                     break;
                  default:
                     StreamTagVisitor.ValueResult streamtagvisitor$valueresult = tag.accept(p_197442_);
                     switch (streamtagvisitor$valueresult) {
                        case HALT:
                           return StreamTagVisitor.ValueResult.HALT;
                        case BREAK:
                           return p_197442_.visitContainerEnd();
                     }
               }
         }
      }

      return p_197442_.visitContainerEnd();
   }

   public <T> void store(String p_401096_, Codec<T> p_401071_, T p_401333_) {
      this.store(p_401096_, p_401071_, NbtOps.INSTANCE, p_401333_);
   }

   public <T> void storeNullable(String p_405110_, Codec<T> p_405799_, @Nullable T p_405604_) {
      if (p_405604_ != null) {
         this.store(p_405110_, p_405799_, p_405604_);
      }
   }

   public <T> void store(String p_401002_, Codec<T> p_401379_, DynamicOps<Tag> p_401063_, T p_401119_) {
      this.put(p_401002_, (Tag)p_401379_.encodeStart(p_401063_, p_401119_).getOrThrow());
   }

   public <T> void storeNullable(String p_404778_, Codec<T> p_405437_, DynamicOps<Tag> p_405108_, @Nullable T p_405353_) {
      if (p_405353_ != null) {
         this.store(p_404778_, p_405437_, p_405108_, p_405353_);
      }
   }

   public <T> void store(MapCodec<T> p_401258_, T p_401416_) {
      this.store(p_401258_, NbtOps.INSTANCE, p_401416_);
   }

   public <T> void store(MapCodec<T> p_401165_, DynamicOps<Tag> p_401224_, T p_401226_) {
      this.merge((CompoundTag)p_401165_.encoder().encodeStart(p_401224_, p_401226_).getOrThrow());
   }

   public <T> Optional<T> read(String p_401324_, Codec<T> p_401364_) {
      return this.read(p_401324_, p_401364_, NbtOps.INSTANCE);
   }

   public <T> Optional<T> read(String p_401362_, Codec<T> p_401434_, DynamicOps<Tag> p_401431_) {
      Tag tag = this.get(p_401362_);
      return tag == null
         ? Optional.empty()
         : p_401434_.parse(p_401431_, tag)
            .resultOrPartial(p_400883_ -> LOGGER.error("Failed to read field ({}={}): {}", new Object[]{p_401362_, tag, p_400883_}));
   }

   public <T> Optional<T> read(MapCodec<T> p_401035_) {
      return this.read(p_401035_, NbtOps.INSTANCE);
   }

   public <T> Optional<T> read(MapCodec<T> p_401418_, DynamicOps<Tag> p_401213_) {
      return p_401418_.decode(p_401213_, (MapLike)p_401213_.getMap(this).getOrThrow())
         .resultOrPartial(p_400884_ -> LOGGER.error("Failed to read value ({}): {}", this, p_400884_));
   }
}
