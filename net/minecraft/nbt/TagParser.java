package net.minecraft.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.network.chat.Component;
import net.minecraft.util.parsing.packrat.commands.Grammar;

public class TagParser<T> {
   public static final SimpleCommandExceptionType ERROR_TRAILING_DATA = new SimpleCommandExceptionType(Component.translatable("argument.nbt.trailing"));
   public static final SimpleCommandExceptionType ERROR_EXPECTED_COMPOUND = new SimpleCommandExceptionType(
      Component.translatable("argument.nbt.expected.compound")
   );
   public static final char ELEMENT_SEPARATOR = ',';
   public static final char NAME_VALUE_SEPARATOR = ':';
   private static final TagParser<Tag> NBT_OPS_PARSER = create(NbtOps.INSTANCE);
   public static final Codec<CompoundTag> FLATTENED_CODEC = Codec.STRING
      .comapFlatMap(
         p_409150_ -> {
            try {
               Tag tag = NBT_OPS_PARSER.parseFully(p_409150_);
               return tag instanceof CompoundTag compoundtag
                  ? DataResult.success(compoundtag, Lifecycle.stable())
                  : DataResult.error(() -> "Expected compound tag, got " + tag);
            } catch (CommandSyntaxException var3) {
               return DataResult.error(var3::getMessage);
            }
         },
         CompoundTag::toString
      );
   public static final Codec<CompoundTag> LENIENT_CODEC = Codec.withAlternative(FLATTENED_CODEC, CompoundTag.CODEC);
   private final DynamicOps<T> ops;
   private final Grammar<T> grammar;

   private TagParser(DynamicOps<T> p_412361_, Grammar<T> p_410792_) {
      this.ops = p_412361_;
      this.grammar = p_410792_;
   }

   public DynamicOps<T> getOps() {
      return this.ops;
   }

   public static <T> TagParser<T> create(DynamicOps<T> p_410391_) {
      return new TagParser<>(p_410391_, SnbtGrammar.createParser(p_410391_));
   }

   private static CompoundTag castToCompoundOrThrow(StringReader p_400179_, Tag p_399751_) throws CommandSyntaxException {
      if (p_399751_ instanceof CompoundTag compoundtag) {
         return compoundtag;
      } else {
         throw ERROR_EXPECTED_COMPOUND.createWithContext(p_400179_);
      }
   }

   public static CompoundTag parseCompoundFully(String p_399485_) throws CommandSyntaxException {
      StringReader stringreader = new StringReader(p_399485_);
      return castToCompoundOrThrow(stringreader, NBT_OPS_PARSER.parseFully(stringreader));
   }

   public T parseFully(String p_400269_) throws CommandSyntaxException {
      return this.parseFully(new StringReader(p_400269_));
   }

   public T parseFully(StringReader p_399809_) throws CommandSyntaxException {
      T t = this.grammar.parseForCommands(p_399809_);
      p_399809_.skipWhitespace();
      if (p_399809_.canRead()) {
         throw ERROR_TRAILING_DATA.createWithContext(p_399809_);
      } else {
         return t;
      }
   }

   public T parseAsArgument(StringReader p_399865_) throws CommandSyntaxException {
      return this.grammar.parseForCommands(p_399865_);
   }

   public static CompoundTag parseCompoundAsArgument(StringReader p_399536_) throws CommandSyntaxException {
      Tag tag = NBT_OPS_PARSER.parseAsArgument(p_399536_);
      return castToCompoundOrThrow(p_399536_, tag);
   }
}
