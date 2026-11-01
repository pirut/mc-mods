package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class NbtPathArgument implements ArgumentType<NbtPathArgument.NbtPath> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo.bar", "foo[0]", "[0]", "[]", "{foo=bar}");
   public static final SimpleCommandExceptionType ERROR_INVALID_NODE = new SimpleCommandExceptionType(Component.translatable("arguments.nbtpath.node.invalid"));
   public static final SimpleCommandExceptionType ERROR_DATA_TOO_DEEP = new SimpleCommandExceptionType(Component.translatable("arguments.nbtpath.too_deep"));
   public static final DynamicCommandExceptionType ERROR_NOTHING_FOUND = new DynamicCommandExceptionType(
      p_304087_ -> Component.translatableEscape("arguments.nbtpath.nothing_found", p_304087_)
   );
   static final DynamicCommandExceptionType ERROR_EXPECTED_LIST = new DynamicCommandExceptionType(
      p_304088_ -> Component.translatableEscape("commands.data.modify.expected_list", p_304088_)
   );
   static final DynamicCommandExceptionType ERROR_INVALID_INDEX = new DynamicCommandExceptionType(
      p_304089_ -> Component.translatableEscape("commands.data.modify.invalid_index", p_304089_)
   );
   private static final char INDEX_MATCH_START = '[';
   private static final char INDEX_MATCH_END = ']';
   private static final char KEY_MATCH_START = '{';
   private static final char KEY_MATCH_END = '}';
   private static final char QUOTED_KEY_START = '"';
   private static final char SINGLE_QUOTED_KEY_START = '\'';

   public static NbtPathArgument nbtPath() {
      return new NbtPathArgument();
   }

   public static NbtPathArgument.NbtPath getPath(CommandContext<CommandSourceStack> p_99499_, String p_99500_) {
      return (NbtPathArgument.NbtPath)p_99499_.getArgument(p_99500_, NbtPathArgument.NbtPath.class);
   }

   public NbtPathArgument.NbtPath parse(StringReader p_99491_) throws CommandSyntaxException {
      List<NbtPathArgument.Node> $$1 = Lists.newArrayList();
      int $$2 = p_99491_.getCursor();
      Object2IntMap<NbtPathArgument.Node> $$3 = new Object2IntOpenHashMap();
      boolean $$4 = true;

      while (p_99491_.canRead() && p_99491_.peek() != ' ') {
         NbtPathArgument.Node $$5 = parseNode(p_99491_, $$4);
         $$1.add($$5);
         $$3.put($$5, p_99491_.getCursor() - $$2);
         $$4 = false;
         if (p_99491_.canRead()) {
            char $$6 = p_99491_.peek();
            if ($$6 != ' ' && $$6 != '[' && $$6 != '{') {
               p_99491_.expect('.');
            }
         }
      }

      return new NbtPathArgument.NbtPath(p_99491_.getString().substring($$2, p_99491_.getCursor()), $$1.toArray(new NbtPathArgument.Node[0]), $$3);
   }

   private static NbtPathArgument.Node parseNode(StringReader p_99496_, boolean p_99497_) throws CommandSyntaxException {
      return (NbtPathArgument.Node)(switch (p_99496_.peek()) {
         case '"', '\'' -> readObjectNode(p_99496_, p_99496_.readString());
         case '[' -> {
            p_99496_.skip();
            int $$3 = p_99496_.peek();
            if ($$3 == 123) {
               CompoundTag $$4 = new TagParser(p_99496_).readStruct();
               p_99496_.expect(']');
               yield new NbtPathArgument.MatchElementNode($$4);
            } else if ($$3 == 93) {
               p_99496_.skip();
               yield NbtPathArgument.AllElementsNode.INSTANCE;
            } else {
               int $$5 = p_99496_.readInt();
               p_99496_.expect(']');
               yield new NbtPathArgument.IndexedElementNode($$5);
            }
         }
         case '{' -> {
            if (!p_99497_) {
               throw ERROR_INVALID_NODE.createWithContext(p_99496_);
            }

            CompoundTag $$2 = new TagParser(p_99496_).readStruct();
            yield new NbtPathArgument.MatchRootObjectNode($$2);
         }
         default -> readObjectNode(p_99496_, readUnquotedName(p_99496_));
      });
   }

   private static NbtPathArgument.Node readObjectNode(StringReader p_99493_, String p_99494_) throws CommandSyntaxException {
      if (p_99493_.canRead() && p_99493_.peek() == '{') {
         CompoundTag $$2 = new TagParser(p_99493_).readStruct();
         return new NbtPathArgument.MatchObjectNode(p_99494_, $$2);
      } else {
         return new NbtPathArgument.CompoundChildNode(p_99494_);
      }
   }

   private static String readUnquotedName(StringReader p_99509_) throws CommandSyntaxException {
      int $$1 = p_99509_.getCursor();

      while (p_99509_.canRead() && isAllowedInUnquotedName(p_99509_.peek())) {
         p_99509_.skip();
      }

      if (p_99509_.getCursor() == $$1) {
         throw ERROR_INVALID_NODE.createWithContext(p_99509_);
      } else {
         return p_99509_.getString().substring($$1, p_99509_.getCursor());
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   private static boolean isAllowedInUnquotedName(char p_99489_) {
      return p_99489_ != ' '
         && p_99489_ != '"'
         && p_99489_ != '\''
         && p_99489_ != '['
         && p_99489_ != ']'
         && p_99489_ != '.'
         && p_99489_ != '{'
         && p_99489_ != '}';
   }

   static Predicate<Tag> createTagPredicate(CompoundTag p_99511_) {
      return p_99507_ -> NbtUtils.compareNbt(p_99511_, p_99507_, true);
   }

   static class AllElementsNode implements NbtPathArgument.Node {
      public static final NbtPathArgument.AllElementsNode INSTANCE = new NbtPathArgument.AllElementsNode();

      private AllElementsNode() {
      }

      @Override
      public void getTag(Tag p_99522_, List<Tag> p_99523_) {
         if (p_99522_ instanceof CollectionTag) {
            p_99523_.addAll((CollectionTag)p_99522_);
         }
      }

      @Override
      public void getOrCreateTag(Tag p_99528_, Supplier<Tag> p_99529_, List<Tag> p_99530_) {
         if (p_99528_ instanceof CollectionTag<?> $$3) {
            if ($$3.isEmpty()) {
               Tag $$4 = p_99529_.get();
               if ($$3.addTag(0, $$4)) {
                  p_99530_.add($$4);
               }
            } else {
               p_99530_.addAll((Collection<? extends Tag>)$$3);
            }
         }
      }

      @Override
      public Tag createPreferredParentTag() {
         return new ListTag();
      }

      @Override
      public int setTag(Tag p_99525_, Supplier<Tag> p_99526_) {
         if (!(p_99525_ instanceof CollectionTag<?> $$2)) {
            return 0;
         } else {
            int $$3 = $$2.size();
            if ($$3 == 0) {
               $$2.addTag(0, p_99526_.get());
               return 1;
            } else {
               Tag $$4 = p_99526_.get();
               int $$5 = $$3 - (int)$$2.stream().filter($$4::equals).count();
               if ($$5 == 0) {
                  return 0;
               } else {
                  $$2.clear();
                  if (!$$2.addTag(0, $$4)) {
                     return 0;
                  } else {
                     for (int $$6 = 1; $$6 < $$3; $$6++) {
                        $$2.addTag($$6, p_99526_.get());
                     }

                     return $$5;
                  }
               }
            }
         }
      }

      @Override
      public int removeTag(Tag p_99520_) {
         if (p_99520_ instanceof CollectionTag<?> $$1) {
            int $$2 = $$1.size();
            if ($$2 > 0) {
               $$1.clear();
               return $$2;
            }
         }

         return 0;
      }
   }

   static class CompoundChildNode implements NbtPathArgument.Node {
      private final String name;

      public CompoundChildNode(String p_99533_) {
         this.name = p_99533_;
      }

      @Override
      public void getTag(Tag p_99538_, List<Tag> p_99539_) {
         if (p_99538_ instanceof CompoundTag) {
            Tag $$2 = ((CompoundTag)p_99538_).get(this.name);
            if ($$2 != null) {
               p_99539_.add($$2);
            }
         }
      }

      @Override
      public void getOrCreateTag(Tag p_99544_, Supplier<Tag> p_99545_, List<Tag> p_99546_) {
         if (p_99544_ instanceof CompoundTag $$3) {
            Tag $$4;
            if ($$3.contains(this.name)) {
               $$4 = $$3.get(this.name);
            } else {
               $$4 = p_99545_.get();
               $$3.put(this.name, $$4);
            }

            p_99546_.add($$4);
         }
      }

      @Override
      public Tag createPreferredParentTag() {
         return new CompoundTag();
      }

      @Override
      public int setTag(Tag p_99541_, Supplier<Tag> p_99542_) {
         if (p_99541_ instanceof CompoundTag $$2) {
            Tag $$3 = p_99542_.get();
            Tag $$4 = $$2.put(this.name, $$3);
            if (!$$3.equals($$4)) {
               return 1;
            }
         }

         return 0;
      }

      @Override
      public int removeTag(Tag p_99536_) {
         if (p_99536_ instanceof CompoundTag $$1 && $$1.contains(this.name)) {
            $$1.remove(this.name);
            return 1;
         } else {
            return 0;
         }
      }
   }

   static class IndexedElementNode implements NbtPathArgument.Node {
      private final int index;

      public IndexedElementNode(int p_99549_) {
         this.index = p_99549_;
      }

      @Override
      public void getTag(Tag p_99554_, List<Tag> p_99555_) {
         if (p_99554_ instanceof CollectionTag<?> $$2) {
            int $$3 = $$2.size();
            int $$4 = this.index < 0 ? $$3 + this.index : this.index;
            if (0 <= $$4 && $$4 < $$3) {
               p_99555_.add($$2.get($$4));
            }
         }
      }

      @Override
      public void getOrCreateTag(Tag p_99560_, Supplier<Tag> p_99561_, List<Tag> p_99562_) {
         this.getTag(p_99560_, p_99562_);
      }

      @Override
      public Tag createPreferredParentTag() {
         return new ListTag();
      }

      @Override
      public int setTag(Tag p_99557_, Supplier<Tag> p_99558_) {
         if (p_99557_ instanceof CollectionTag<?> $$2) {
            int $$3 = $$2.size();
            int $$4 = this.index < 0 ? $$3 + this.index : this.index;
            if (0 <= $$4 && $$4 < $$3) {
               Tag $$5 = $$2.get($$4);
               Tag $$6 = p_99558_.get();
               if (!$$6.equals($$5) && $$2.setTag($$4, $$6)) {
                  return 1;
               }
            }
         }

         return 0;
      }

      @Override
      public int removeTag(Tag p_99552_) {
         if (p_99552_ instanceof CollectionTag<?> $$1) {
            int $$2 = $$1.size();
            int $$3 = this.index < 0 ? $$2 + this.index : this.index;
            if (0 <= $$3 && $$3 < $$2) {
               $$1.remove($$3);
               return 1;
            }
         }

         return 0;
      }
   }

   static class MatchElementNode implements NbtPathArgument.Node {
      private final CompoundTag pattern;
      private final Predicate<Tag> predicate;

      public MatchElementNode(CompoundTag p_99566_) {
         this.pattern = p_99566_;
         this.predicate = NbtPathArgument.createTagPredicate(p_99566_);
      }

      @Override
      public void getTag(Tag p_99575_, List<Tag> p_99576_) {
         if (p_99575_ instanceof ListTag $$2) {
            $$2.stream().filter(this.predicate).forEach(p_99576_::add);
         }
      }

      @Override
      public void getOrCreateTag(Tag p_99581_, Supplier<Tag> p_99582_, List<Tag> p_99583_) {
         MutableBoolean $$3 = new MutableBoolean();
         if (p_99581_ instanceof ListTag $$4) {
            $$4.stream().filter(this.predicate).forEach(p_99571_ -> {
               p_99583_.add(p_99571_);
               $$3.setTrue();
            });
            if ($$3.isFalse()) {
               CompoundTag $$5 = this.pattern.copy();
               $$4.add($$5);
               p_99583_.add($$5);
            }
         }
      }

      @Override
      public Tag createPreferredParentTag() {
         return new ListTag();
      }

      @Override
      public int setTag(Tag p_99578_, Supplier<Tag> p_99579_) {
         int $$2 = 0;
         if (p_99578_ instanceof ListTag $$3) {
            int $$4 = $$3.size();
            if ($$4 == 0) {
               $$3.add(p_99579_.get());
               $$2++;
            } else {
               for (int $$5 = 0; $$5 < $$4; $$5++) {
                  Tag $$6 = $$3.get($$5);
                  if (this.predicate.test($$6)) {
                     Tag $$7 = p_99579_.get();
                     if (!$$7.equals($$6) && $$3.setTag($$5, $$7)) {
                        $$2++;
                     }
                  }
               }
            }
         }

         return $$2;
      }

      @Override
      public int removeTag(Tag p_99573_) {
         int $$1 = 0;
         if (p_99573_ instanceof ListTag $$2) {
            for (int $$3 = $$2.size() - 1; $$3 >= 0; $$3--) {
               if (this.predicate.test($$2.get($$3))) {
                  $$2.remove($$3);
                  $$1++;
               }
            }
         }

         return $$1;
      }
   }

   static class MatchObjectNode implements NbtPathArgument.Node {
      private final String name;
      private final CompoundTag pattern;
      private final Predicate<Tag> predicate;

      public MatchObjectNode(String p_99588_, CompoundTag p_99589_) {
         this.name = p_99588_;
         this.pattern = p_99589_;
         this.predicate = NbtPathArgument.createTagPredicate(p_99589_);
      }

      @Override
      public void getTag(Tag p_99594_, List<Tag> p_99595_) {
         if (p_99594_ instanceof CompoundTag) {
            Tag $$2 = ((CompoundTag)p_99594_).get(this.name);
            if (this.predicate.test($$2)) {
               p_99595_.add($$2);
            }
         }
      }

      @Override
      public void getOrCreateTag(Tag p_99600_, Supplier<Tag> p_99601_, List<Tag> p_99602_) {
         if (p_99600_ instanceof CompoundTag $$3) {
            Tag $$4 = $$3.get(this.name);
            if ($$4 == null) {
               Tag var6 = this.pattern.copy();
               $$3.put(this.name, var6);
               p_99602_.add(var6);
            } else if (this.predicate.test($$4)) {
               p_99602_.add($$4);
            }
         }
      }

      @Override
      public Tag createPreferredParentTag() {
         return new CompoundTag();
      }

      @Override
      public int setTag(Tag p_99597_, Supplier<Tag> p_99598_) {
         if (p_99597_ instanceof CompoundTag $$2) {
            Tag $$3 = $$2.get(this.name);
            if (this.predicate.test($$3)) {
               Tag $$4 = p_99598_.get();
               if (!$$4.equals($$3)) {
                  $$2.put(this.name, $$4);
                  return 1;
               }
            }
         }

         return 0;
      }

      @Override
      public int removeTag(Tag p_99592_) {
         if (p_99592_ instanceof CompoundTag $$1) {
            Tag $$2 = $$1.get(this.name);
            if (this.predicate.test($$2)) {
               $$1.remove(this.name);
               return 1;
            }
         }

         return 0;
      }
   }

   static class MatchRootObjectNode implements NbtPathArgument.Node {
      private final Predicate<Tag> predicate;

      public MatchRootObjectNode(CompoundTag p_99605_) {
         this.predicate = NbtPathArgument.createTagPredicate(p_99605_);
      }

      @Override
      public void getTag(Tag p_99610_, List<Tag> p_99611_) {
         if (p_99610_ instanceof CompoundTag && this.predicate.test(p_99610_)) {
            p_99611_.add(p_99610_);
         }
      }

      @Override
      public void getOrCreateTag(Tag p_99616_, Supplier<Tag> p_99617_, List<Tag> p_99618_) {
         this.getTag(p_99616_, p_99618_);
      }

      @Override
      public Tag createPreferredParentTag() {
         return new CompoundTag();
      }

      @Override
      public int setTag(Tag p_99613_, Supplier<Tag> p_99614_) {
         return 0;
      }

      @Override
      public int removeTag(Tag p_99608_) {
         return 0;
      }
   }

   public static class NbtPath {
      private final String original;
      private final Object2IntMap<NbtPathArgument.Node> nodeToOriginalPosition;
      private final NbtPathArgument.Node[] nodes;
      public static final Codec<NbtPathArgument.NbtPath> CODEC = Codec.STRING.comapFlatMap(p_335166_ -> {
         try {
            NbtPathArgument.NbtPath $$1 = new NbtPathArgument().parse(new StringReader(p_335166_));
            return DataResult.success($$1);
         } catch (CommandSyntaxException var2) {
            return DataResult.error(() -> "Failed to parse path " + p_335166_ + ": " + var2.getMessage());
         }
      }, NbtPathArgument.NbtPath::asString);

      public static NbtPathArgument.NbtPath of(String p_336101_) throws CommandSyntaxException {
         return new NbtPathArgument().parse(new StringReader(p_336101_));
      }

      public NbtPath(String p_99623_, NbtPathArgument.Node[] p_99624_, Object2IntMap<NbtPathArgument.Node> p_99625_) {
         this.original = p_99623_;
         this.nodes = p_99624_;
         this.nodeToOriginalPosition = p_99625_;
      }

      public List<Tag> get(Tag p_99639_) throws CommandSyntaxException {
         List<Tag> $$1 = Collections.singletonList(p_99639_);

         for (NbtPathArgument.Node $$2 : this.nodes) {
            $$1 = $$2.get($$1);
            if ($$1.isEmpty()) {
               throw this.createNotFoundException($$2);
            }
         }

         return $$1;
      }

      public int countMatching(Tag p_99644_) {
         List<Tag> $$1 = Collections.singletonList(p_99644_);

         for (NbtPathArgument.Node $$2 : this.nodes) {
            $$1 = $$2.get($$1);
            if ($$1.isEmpty()) {
               return 0;
            }
         }

         return $$1.size();
      }

      private List<Tag> getOrCreateParents(Tag p_99651_) throws CommandSyntaxException {
         List<Tag> $$1 = Collections.singletonList(p_99651_);

         for (int $$2 = 0; $$2 < this.nodes.length - 1; $$2++) {
            NbtPathArgument.Node $$3 = this.nodes[$$2];
            int $$4 = $$2 + 1;
            $$1 = $$3.getOrCreate($$1, this.nodes[$$4]::createPreferredParentTag);
            if ($$1.isEmpty()) {
               throw this.createNotFoundException($$3);
            }
         }

         return $$1;
      }

      public List<Tag> getOrCreate(Tag p_99641_, Supplier<Tag> p_99642_) throws CommandSyntaxException {
         List<Tag> $$2 = this.getOrCreateParents(p_99641_);
         NbtPathArgument.Node $$3 = this.nodes[this.nodes.length - 1];
         return $$3.getOrCreate($$2, p_99642_);
      }

      private static int apply(List<Tag> p_99636_, Function<Tag, Integer> p_99637_) {
         return p_99636_.stream().map(p_99637_).reduce(0, (p_99633_, p_99634_) -> p_99633_ + p_99634_);
      }

      public static boolean isTooDeep(Tag p_263392_, int p_263386_) {
         if (p_263386_ >= 512) {
            return true;
         } else {
            if (p_263392_ instanceof CompoundTag $$2) {
               for (String $$3 : $$2.getAllKeys()) {
                  Tag $$4 = $$2.get($$3);
                  if ($$4 != null && isTooDeep($$4, p_263386_ + 1)) {
                     return true;
                  }
               }
            } else if (p_263392_ instanceof ListTag) {
               for (Tag $$6 : (ListTag)p_263392_) {
                  if (isTooDeep($$6, p_263386_ + 1)) {
                     return true;
                  }
               }
            }

            return false;
         }
      }

      public int set(Tag p_169536_, Tag p_169537_) throws CommandSyntaxException {
         if (isTooDeep(p_169537_, this.estimatePathDepth())) {
            throw NbtPathArgument.ERROR_DATA_TOO_DEEP.create();
         } else {
            Tag $$2 = p_169537_.copy();
            List<Tag> $$3 = this.getOrCreateParents(p_169536_);
            if ($$3.isEmpty()) {
               return 0;
            } else {
               NbtPathArgument.Node $$4 = this.nodes[this.nodes.length - 1];
               MutableBoolean $$5 = new MutableBoolean(false);
               return apply($$3, p_263259_ -> $$4.setTag(p_263259_, () -> {
                  if ($$5.isFalse()) {
                     $$5.setTrue();
                     return $$2;
                  } else {
                     return $$2.copy();
                  }
               }));
            }
         }
      }

      private int estimatePathDepth() {
         return this.nodes.length;
      }

      public int insert(int p_263397_, CompoundTag p_263348_, List<Tag> p_263419_) throws CommandSyntaxException {
         List<Tag> $$3 = new ArrayList<>(p_263419_.size());

         for (Tag $$4 : p_263419_) {
            Tag $$5 = $$4.copy();
            $$3.add($$5);
            if (isTooDeep($$5, this.estimatePathDepth())) {
               throw NbtPathArgument.ERROR_DATA_TOO_DEEP.create();
            }
         }

         Collection<Tag> $$6 = this.getOrCreate(p_263348_, ListTag::new);
         int $$7 = 0;
         boolean $$8 = false;

         for (Tag $$9 : $$6) {
            if (!($$9 instanceof CollectionTag<?> $$10)) {
               throw NbtPathArgument.ERROR_EXPECTED_LIST.create($$9);
            }

            boolean $$11 = false;
            int $$12 = p_263397_ < 0 ? $$10.size() + p_263397_ + 1 : p_263397_;

            for (Tag $$13 : $$3) {
               try {
                  if ($$10.addTag($$12, $$8 ? $$13.copy() : $$13)) {
                     $$12++;
                     $$11 = true;
                  }
               } catch (IndexOutOfBoundsException var16) {
                  throw NbtPathArgument.ERROR_INVALID_INDEX.create($$12);
               }
            }

            $$8 = true;
            $$7 += $$11 ? 1 : 0;
         }

         return $$7;
      }

      public int remove(Tag p_99649_) {
         List<Tag> $$1 = Collections.singletonList(p_99649_);

         for (int $$2 = 0; $$2 < this.nodes.length - 1; $$2++) {
            $$1 = this.nodes[$$2].get($$1);
         }

         NbtPathArgument.Node $$3 = this.nodes[this.nodes.length - 1];
         return apply($$1, $$3::removeTag);
      }

      private CommandSyntaxException createNotFoundException(NbtPathArgument.Node p_99627_) {
         int $$1 = this.nodeToOriginalPosition.getInt(p_99627_);
         return NbtPathArgument.ERROR_NOTHING_FOUND.create(this.original.substring(0, $$1));
      }

      @Override
      public String toString() {
         return this.original;
      }

      public String asString() {
         return this.original;
      }
   }

   interface Node {
      void getTag(Tag var1, List<Tag> var2);

      void getOrCreateTag(Tag var1, Supplier<Tag> var2, List<Tag> var3);

      Tag createPreferredParentTag();

      int setTag(Tag var1, Supplier<Tag> var2);

      int removeTag(Tag var1);

      default List<Tag> get(List<Tag> p_99654_) {
         return this.collect(p_99654_, this::getTag);
      }

      default List<Tag> getOrCreate(List<Tag> p_99659_, Supplier<Tag> p_99660_) {
         return this.collect(p_99659_, (p_99663_, p_99664_) -> this.getOrCreateTag(p_99663_, p_99660_, p_99664_));
      }

      default List<Tag> collect(List<Tag> p_99656_, BiConsumer<Tag, List<Tag>> p_99657_) {
         List<Tag> $$2 = Lists.newArrayList();

         for (Tag $$3 : p_99656_) {
            p_99657_.accept($$3, $$2);
         }

         return $$2;
      }
   }
}
