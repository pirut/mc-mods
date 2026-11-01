package net.minecraft.world.level.storage;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Streams;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.DataResult.Success;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagType;
import net.minecraft.util.ProblemReporter;
import org.jspecify.annotations.Nullable;

public class TagValueInput implements ValueInput {
   private final ProblemReporter problemReporter;
   private final ValueInputContextHelper context;
   private final CompoundTag input;

   private TagValueInput(ProblemReporter p_421784_, ValueInputContextHelper p_422152_, CompoundTag p_421946_) {
      this.problemReporter = p_421784_;
      this.context = p_422152_;
      this.input = p_421946_;
   }

   public static ValueInput create(ProblemReporter p_422128_, HolderLookup.Provider p_421808_, CompoundTag p_421841_) {
      return new TagValueInput(p_422128_, new ValueInputContextHelper(p_421808_, NbtOps.INSTANCE), p_421841_);
   }

   public static ValueInput.ValueInputList create(ProblemReporter p_422238_, HolderLookup.Provider p_421977_, List<CompoundTag> p_422016_) {
      return new TagValueInput.CompoundListWrapper(p_422238_, new ValueInputContextHelper(p_421977_, NbtOps.INSTANCE), p_422016_);
   }

   @Override
   public <T> Optional<T> read(String p_422121_, Codec<T> p_421846_) {
      Tag tag = this.input.get(p_422121_);
      if (tag == null) {
         return Optional.empty();
      } else {
         return switch (p_421846_.parse(this.context.ops(), tag)) {
            case Success<T> success -> Optional.of(success.value());
            case Error<T> error -> {
               this.problemReporter.report(new TagValueInput.DecodeFromFieldFailedProblem(p_422121_, tag, error));
               yield error.partialValue();
            }
            default -> throw new MatchException(null, null);
         };
      }
   }

   @Override
   public <T> Optional<T> read(MapCodec<T> p_422606_) {
      DynamicOps<Tag> dynamicops = this.context.ops();

      return switch (dynamicops.getMap(this.input).flatMap(p_422400_ -> p_422606_.decode(dynamicops, p_422400_))) {
         case Success<T> success -> Optional.of(success.value());
         case Error<T> error -> {
            this.problemReporter.report(new TagValueInput.DecodeFromMapFailedProblem(error));
            yield error.partialValue();
         }
         default -> throw new MatchException(null, null);
      };
   }

   @Nullable
   private <T extends Tag> T getOptionalTypedTag(String p_421947_, TagType<T> p_421665_) {
      Tag tag = this.input.get(p_421947_);
      if (tag == null) {
         return null;
      } else {
         TagType<?> tagtype = tag.getType();
         if (tagtype != p_421665_) {
            this.problemReporter.report(new TagValueInput.UnexpectedTypeProblem(p_421947_, p_421665_, tagtype));
            return null;
         } else {
            return (T)tag;
         }
      }
   }

   @Nullable
   private NumericTag getNumericTag(String p_421843_) {
      Tag tag = this.input.get(p_421843_);
      if (tag == null) {
         return null;
      } else if (tag instanceof NumericTag numerictag) {
         return numerictag;
      } else {
         this.problemReporter.report(new TagValueInput.UnexpectedNonNumberProblem(p_421843_, tag.getType()));
         return null;
      }
   }

   @Override
   public Optional<ValueInput> child(String p_422520_) {
      CompoundTag compoundtag = this.getOptionalTypedTag(p_422520_, CompoundTag.TYPE);
      return compoundtag != null ? Optional.of(this.wrapChild(p_422520_, compoundtag)) : Optional.empty();
   }

   @Override
   public ValueInput childOrEmpty(String p_422555_) {
      CompoundTag compoundtag = this.getOptionalTypedTag(p_422555_, CompoundTag.TYPE);
      return compoundtag != null ? this.wrapChild(p_422555_, compoundtag) : this.context.empty();
   }

   @Override
   public ValueInput rawChildOrEmpty(String key) {
      CompoundTag compoundtag = this.getOptionalTypedTag(key, CompoundTag.TYPE);
      return (ValueInput)(compoundtag != null
         ? new TagValueInput(this.problemReporter.forChild(new ProblemReporter.FieldPathElement(key)), this.context, compoundtag)
         : this.context.empty());
   }

   @Override
   public Optional<ValueInput.ValueInputList> childrenList(String p_421700_) {
      ListTag listtag = this.getOptionalTypedTag(p_421700_, ListTag.TYPE);
      return listtag != null ? Optional.of(this.wrapList(p_421700_, this.context, listtag)) : Optional.empty();
   }

   @Override
   public ValueInput.ValueInputList childrenListOrEmpty(String p_422523_) {
      ListTag listtag = this.getOptionalTypedTag(p_422523_, ListTag.TYPE);
      return listtag != null ? this.wrapList(p_422523_, this.context, listtag) : this.context.emptyList();
   }

   @Override
   public <T> Optional<ValueInput.TypedInputList<T>> list(String p_422068_, Codec<T> p_421633_) {
      ListTag listtag = this.getOptionalTypedTag(p_422068_, ListTag.TYPE);
      return listtag != null ? Optional.of(this.wrapTypedList(p_422068_, listtag, p_421633_)) : Optional.empty();
   }

   @Override
   public <T> ValueInput.TypedInputList<T> listOrEmpty(String p_422509_, Codec<T> p_422700_) {
      ListTag listtag = this.getOptionalTypedTag(p_422509_, ListTag.TYPE);
      return listtag != null ? this.wrapTypedList(p_422509_, listtag, p_422700_) : this.context.emptyTypedList();
   }

   @Override
   public boolean getBooleanOr(String p_422592_, boolean p_422347_) {
      NumericTag numerictag = this.getNumericTag(p_422592_);
      return numerictag != null ? numerictag.byteValue() != 0 : p_422347_;
   }

   @Override
   public byte getByteOr(String p_422035_, byte p_422310_) {
      NumericTag numerictag = this.getNumericTag(p_422035_);
      return numerictag != null ? numerictag.byteValue() : p_422310_;
   }

   @Override
   public int getShortOr(String p_421616_, short p_421751_) {
      NumericTag numerictag = this.getNumericTag(p_421616_);
      return numerictag != null ? numerictag.shortValue() : p_421751_;
   }

   @Override
   public Optional<Integer> getInt(String p_421813_) {
      NumericTag numerictag = this.getNumericTag(p_421813_);
      return numerictag != null ? Optional.of(numerictag.intValue()) : Optional.empty();
   }

   @Override
   public int getIntOr(String p_422549_, int p_422510_) {
      NumericTag numerictag = this.getNumericTag(p_422549_);
      return numerictag != null ? numerictag.intValue() : p_422510_;
   }

   @Override
   public long getLongOr(String p_422010_, long p_421867_) {
      NumericTag numerictag = this.getNumericTag(p_422010_);
      return numerictag != null ? numerictag.longValue() : p_421867_;
   }

   @Override
   public Optional<Long> getLong(String p_422244_) {
      NumericTag numerictag = this.getNumericTag(p_422244_);
      return numerictag != null ? Optional.of(numerictag.longValue()) : Optional.empty();
   }

   @Override
   public float getFloatOr(String p_421957_, float p_422667_) {
      NumericTag numerictag = this.getNumericTag(p_421957_);
      return numerictag != null ? numerictag.floatValue() : p_422667_;
   }

   @Override
   public double getDoubleOr(String p_422065_, double p_421551_) {
      NumericTag numerictag = this.getNumericTag(p_422065_);
      return numerictag != null ? numerictag.doubleValue() : p_421551_;
   }

   @Override
   public Optional<String> getString(String p_421854_) {
      StringTag stringtag = this.getOptionalTypedTag(p_421854_, StringTag.TYPE);
      return stringtag != null ? Optional.of(stringtag.value()) : Optional.empty();
   }

   @Override
   public String getStringOr(String p_422061_, String p_421849_) {
      StringTag stringtag = this.getOptionalTypedTag(p_422061_, StringTag.TYPE);
      return stringtag != null ? stringtag.value() : p_421849_;
   }

   @Override
   public Optional<int[]> getIntArray(String p_422301_) {
      IntArrayTag intarraytag = this.getOptionalTypedTag(p_422301_, IntArrayTag.TYPE);
      return intarraytag != null ? Optional.of(intarraytag.getAsIntArray()) : Optional.empty();
   }

   @Override
   public HolderLookup.Provider lookup() {
      return this.context.lookup();
   }

   @Override
   public Set<String> keySet() {
      return Collections.unmodifiableSet(this.input.keySet());
   }

   private ValueInput wrapChild(String p_422021_, CompoundTag p_422689_) {
      return (ValueInput)(p_422689_.isEmpty()
         ? this.context.empty()
         : new TagValueInput(this.problemReporter.forChild(new ProblemReporter.FieldPathElement(p_422021_)), this.context, p_422689_));
   }

   static ValueInput wrapChild(ProblemReporter p_422473_, ValueInputContextHelper p_421819_, CompoundTag p_422675_) {
      return (ValueInput)(p_422675_.isEmpty() ? p_421819_.empty() : new TagValueInput(p_422473_, p_421819_, p_422675_));
   }

   private ValueInput.ValueInputList wrapList(String p_422657_, ValueInputContextHelper p_421936_, ListTag p_422099_) {
      return (ValueInput.ValueInputList)(p_422099_.isEmpty()
         ? p_421936_.emptyList()
         : new TagValueInput.ListWrapper(this.problemReporter, p_422657_, p_421936_, p_422099_));
   }

   private <T> ValueInput.TypedInputList<T> wrapTypedList(String p_422691_, ListTag p_422064_, Codec<T> p_422638_) {
      return (ValueInput.TypedInputList<T>)(p_422064_.isEmpty()
         ? this.context.emptyTypedList()
         : new TagValueInput.TypedListWrapper<>(this.problemReporter, p_422691_, this.context, p_422638_, p_422064_));
   }

   static class CompoundListWrapper implements ValueInput.ValueInputList {
      private final ProblemReporter problemReporter;
      private final ValueInputContextHelper context;
      private final List<CompoundTag> list;

      public CompoundListWrapper(ProblemReporter p_421583_, ValueInputContextHelper p_421580_, List<CompoundTag> p_421702_) {
         this.problemReporter = p_421583_;
         this.context = p_421580_;
         this.list = p_421702_;
      }

      ValueInput wrapChild(int p_422192_, CompoundTag p_422256_) {
         return TagValueInput.wrapChild(this.problemReporter.forChild(new ProblemReporter.IndexedPathElement(p_422192_)), this.context, p_422256_);
      }

      @Override
      public boolean isEmpty() {
         return this.list.isEmpty();
      }

      @Override
      public Stream<ValueInput> stream() {
         return Streams.mapWithIndex(this.list.stream(), (p_422311_, p_421760_) -> this.wrapChild((int)p_421760_, p_422311_));
      }

      @Override
      public Iterator<ValueInput> iterator() {
         final ListIterator<CompoundTag> listiterator = this.list.listIterator();
         return new AbstractIterator<ValueInput>() {
            @Nullable
            protected ValueInput computeNext() {
               if (listiterator.hasNext()) {
                  int i = listiterator.nextIndex();
                  CompoundTag compoundtag = listiterator.next();
                  return CompoundListWrapper.this.wrapChild(i, compoundtag);
               } else {
                  return (ValueInput)this.endOfData();
               }
            }
         };
      }
   }

   public record DecodeFromFieldFailedProblem(String name, Tag tag, Error<?> error) implements ProblemReporter.Problem {
      @Override
      public String description() {
         return "Failed to decode value '" + this.tag + "' from field '" + this.name + "': " + this.error.message();
      }
   }

   public record DecodeFromListFailedProblem(String name, int index, Tag tag, Error<?> error) implements ProblemReporter.Problem {
      @Override
      public String description() {
         return "Failed to decode value '" + this.tag + "' from field '" + this.name + "' at index " + this.index + "': " + this.error.message();
      }
   }

   public record DecodeFromMapFailedProblem(Error<?> error) implements ProblemReporter.Problem {
      @Override
      public String description() {
         return "Failed to decode from map: " + this.error.message();
      }
   }

   static class ListWrapper implements ValueInput.ValueInputList {
      private final ProblemReporter problemReporter;
      private final String name;
      final ValueInputContextHelper context;
      private final ListTag list;

      ListWrapper(ProblemReporter p_422557_, String p_422571_, ValueInputContextHelper p_422196_, ListTag p_421709_) {
         this.problemReporter = p_422557_;
         this.name = p_422571_;
         this.context = p_422196_;
         this.list = p_421709_;
      }

      @Override
      public boolean isEmpty() {
         return this.list.isEmpty();
      }

      ProblemReporter reporterForChild(int p_421507_) {
         return this.problemReporter.forChild(new ProblemReporter.IndexedFieldPathElement(this.name, p_421507_));
      }

      void reportIndexUnwrapProblem(int p_422314_, Tag p_422447_) {
         this.problemReporter.report(new TagValueInput.UnexpectedListElementTypeProblem(this.name, p_422314_, CompoundTag.TYPE, p_422447_.getType()));
      }

      @Override
      public Stream<ValueInput> stream() {
         return Streams.mapWithIndex(this.list.stream(), (p_421560_, p_421602_) -> {
            if (p_421560_ instanceof CompoundTag compoundtag) {
               return TagValueInput.wrapChild(this.reporterForChild((int)p_421602_), this.context, compoundtag);
            } else {
               this.reportIndexUnwrapProblem((int)p_421602_, p_421560_);
               return null;
            }
         }).filter(Objects::nonNull);
      }

      @Override
      public Iterator<ValueInput> iterator() {
         final Iterator<Tag> iterator = this.list.iterator();
         return new AbstractIterator<ValueInput>() {
            private int index;

            @Nullable
            protected ValueInput computeNext() {
               while (iterator.hasNext()) {
                  Tag tag = iterator.next();
                  int i = this.index++;
                  if (tag instanceof CompoundTag compoundtag) {
                     return TagValueInput.wrapChild(ListWrapper.this.reporterForChild(i), ListWrapper.this.context, compoundtag);
                  }

                  ListWrapper.this.reportIndexUnwrapProblem(i, tag);
               }

               return (ValueInput)this.endOfData();
            }
         };
      }
   }

   static class TypedListWrapper<T> implements ValueInput.TypedInputList<T> {
      private final ProblemReporter problemReporter;
      private final String name;
      final ValueInputContextHelper context;
      final Codec<T> codec;
      private final ListTag list;

      TypedListWrapper(ProblemReporter p_422552_, String p_421835_, ValueInputContextHelper p_422408_, Codec<T> p_421508_, ListTag p_422365_) {
         this.problemReporter = p_422552_;
         this.name = p_421835_;
         this.context = p_422408_;
         this.codec = p_421508_;
         this.list = p_422365_;
      }

      @Override
      public boolean isEmpty() {
         return this.list.isEmpty();
      }

      void reportIndexUnwrapProblem(int p_422123_, Tag p_421922_, Error<?> p_422537_) {
         this.problemReporter.report(new TagValueInput.DecodeFromListFailedProblem(this.name, p_422123_, p_421922_, p_422537_));
      }

      @Override
      public Stream<T> stream() {
         return Streams.mapWithIndex(this.list.stream(), (p_421803_, p_422239_) -> {
            return switch (this.codec.parse(this.context.ops(), p_421803_)) {
               case Success<T> success -> success.value();
               case Error<T> error -> {
                  this.reportIndexUnwrapProblem((int)p_422239_, p_421803_, error);
                  yield error.partialValue().orElse(null);
               }
               default -> throw new MatchException(null, null);
            };
         }).filter(Objects::nonNull);
      }

      @Override
      public Iterator<T> iterator() {
         final ListIterator<Tag> listiterator = this.list.listIterator();
         return new AbstractIterator<T>() {
            @Nullable
            protected T computeNext() {
               while (listiterator.hasNext()) {
                  int i = listiterator.nextIndex();
                  Tag tag = listiterator.next();
                  switch (TypedListWrapper.this.codec.parse(TypedListWrapper.this.context.ops(), tag)) {
                     case Success<T> success:
                        return (T)success.value();
                     case Error<T> error:
                        TypedListWrapper.this.reportIndexUnwrapProblem(i, tag, error);
                        if (!error.partialValue().isPresent()) {
                           break;
                        }

                        return (T)error.partialValue().get();
                     default:
                        throw new MatchException(null, null);
                  }
               }

               return (T)this.endOfData();
            }
         };
      }
   }

   public record UnexpectedListElementTypeProblem(String name, int index, TagType<?> expected, TagType<?> actual) implements ProblemReporter.Problem {
      @Override
      public String description() {
         return "Expected list '"
            + this.name
            + "' to contain at index "
            + this.index
            + " value of type "
            + this.expected.getName()
            + ", but got "
            + this.actual.getName();
      }
   }

   public record UnexpectedNonNumberProblem(String name, TagType<?> actual) implements ProblemReporter.Problem {
      @Override
      public String description() {
         return "Expected field '" + this.name + "' to contain number, but got " + this.actual.getName();
      }
   }

   public record UnexpectedTypeProblem(String name, TagType<?> expected, TagType<?> actual) implements ProblemReporter.Problem {
      @Override
      public String description() {
         return "Expected field '" + this.name + "' to contain value of type " + this.expected.getName() + ", but got " + this.actual.getName();
      }
   }
}
