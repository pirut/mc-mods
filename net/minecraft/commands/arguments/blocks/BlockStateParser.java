package net.minecraft.commands.arguments.blocks;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockStateParser {
   public static final SimpleCommandExceptionType ERROR_NO_TAGS_ALLOWED = new SimpleCommandExceptionType(
      Component.translatable("argument.block.tag.disallowed")
   );
   public static final DynamicCommandExceptionType ERROR_UNKNOWN_BLOCK = new DynamicCommandExceptionType(
      p_304118_ -> Component.translatableEscape("argument.block.id.invalid", p_304118_)
   );
   public static final Dynamic2CommandExceptionType ERROR_UNKNOWN_PROPERTY = new Dynamic2CommandExceptionType(
      (p_304119_, p_304120_) -> Component.translatableEscape("argument.block.property.unknown", p_304119_, p_304120_)
   );
   public static final Dynamic2CommandExceptionType ERROR_DUPLICATE_PROPERTY = new Dynamic2CommandExceptionType(
      (p_304121_, p_304122_) -> Component.translatableEscape("argument.block.property.duplicate", p_304122_, p_304121_)
   );
   public static final Dynamic3CommandExceptionType ERROR_INVALID_VALUE = new Dynamic3CommandExceptionType(
      (p_304124_, p_304125_, p_304126_) -> Component.translatableEscape("argument.block.property.invalid", p_304124_, p_304126_, p_304125_)
   );
   public static final Dynamic2CommandExceptionType ERROR_EXPECTED_VALUE = new Dynamic2CommandExceptionType(
      (p_304127_, p_304128_) -> Component.translatableEscape("argument.block.property.novalue", p_304127_, p_304128_)
   );
   public static final SimpleCommandExceptionType ERROR_EXPECTED_END_OF_PROPERTIES = new SimpleCommandExceptionType(
      Component.translatable("argument.block.property.unclosed")
   );
   public static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType(
      p_304123_ -> Component.translatableEscape("arguments.block.tag.unknown", p_304123_)
   );
   private static final char SYNTAX_START_PROPERTIES = '[';
   private static final char SYNTAX_START_NBT = '{';
   private static final char SYNTAX_END_PROPERTIES = ']';
   private static final char SYNTAX_EQUALS = '=';
   private static final char SYNTAX_PROPERTY_SEPARATOR = ',';
   private static final char SYNTAX_TAG = '#';
   private static final Function<SuggestionsBuilder, CompletableFuture<Suggestions>> SUGGEST_NOTHING = SuggestionsBuilder::buildFuture;
   private final HolderLookup<Block> blocks;
   private final StringReader reader;
   private final boolean forTesting;
   private final boolean allowNbt;
   private final Map<Property<?>, Comparable<?>> properties = Maps.newHashMap();
   private final Map<String, String> vagueProperties = Maps.newHashMap();
   private ResourceLocation id = ResourceLocation.withDefaultNamespace("");
   @Nullable
   private StateDefinition<Block, BlockState> definition;
   @Nullable
   private BlockState state;
   @Nullable
   private CompoundTag nbt;
   @Nullable
   private HolderSet<Block> tag;
   private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestions = SUGGEST_NOTHING;

   private BlockStateParser(HolderLookup<Block> p_234673_, StringReader p_234674_, boolean p_234675_, boolean p_234676_) {
      this.blocks = p_234673_;
      this.reader = p_234674_;
      this.forTesting = p_234675_;
      this.allowNbt = p_234676_;
   }

   public static BlockStateParser.BlockResult parseForBlock(HolderLookup<Block> p_251394_, String p_248677_, boolean p_250430_) throws CommandSyntaxException {
      return parseForBlock(p_251394_, new StringReader(p_248677_), p_250430_);
   }

   public static BlockStateParser.BlockResult parseForBlock(HolderLookup<Block> p_234692_, StringReader p_234693_, boolean p_234694_) throws CommandSyntaxException {
      int $$3 = p_234693_.getCursor();

      try {
         BlockStateParser $$4 = new BlockStateParser(p_234692_, p_234693_, false, p_234694_);
         $$4.parse();
         return new BlockStateParser.BlockResult($$4.state, $$4.properties, $$4.nbt);
      } catch (CommandSyntaxException var5) {
         p_234693_.setCursor($$3);
         throw var5;
      }
   }

   public static Either<BlockStateParser.BlockResult, BlockStateParser.TagResult> parseForTesting(
      HolderLookup<Block> p_252082_, String p_251830_, boolean p_249125_
   ) throws CommandSyntaxException {
      return parseForTesting(p_252082_, new StringReader(p_251830_), p_249125_);
   }

   public static Either<BlockStateParser.BlockResult, BlockStateParser.TagResult> parseForTesting(
      HolderLookup<Block> p_234717_, StringReader p_234718_, boolean p_234719_
   ) throws CommandSyntaxException {
      int $$3 = p_234718_.getCursor();

      try {
         BlockStateParser $$4 = new BlockStateParser(p_234717_, p_234718_, true, p_234719_);
         $$4.parse();
         return $$4.tag != null
            ? Either.right(new BlockStateParser.TagResult($$4.tag, $$4.vagueProperties, $$4.nbt))
            : Either.left(new BlockStateParser.BlockResult($$4.state, $$4.properties, $$4.nbt));
      } catch (CommandSyntaxException var5) {
         p_234718_.setCursor($$3);
         throw var5;
      }
   }

   public static CompletableFuture<Suggestions> fillSuggestions(
      HolderLookup<Block> p_234696_, SuggestionsBuilder p_234697_, boolean p_234698_, boolean p_234699_
   ) {
      StringReader $$4 = new StringReader(p_234697_.getInput());
      $$4.setCursor(p_234697_.getStart());
      BlockStateParser $$5 = new BlockStateParser(p_234696_, $$4, p_234698_, p_234699_);

      try {
         $$5.parse();
      } catch (CommandSyntaxException var7) {
      }

      return $$5.suggestions.apply(p_234697_.createOffset($$4.getCursor()));
   }

   private void parse() throws CommandSyntaxException {
      if (this.forTesting) {
         this.suggestions = this::suggestBlockIdOrTag;
      } else {
         this.suggestions = this::suggestItem;
      }

      if (this.reader.canRead() && this.reader.peek() == '#') {
         this.readTag();
         this.suggestions = this::suggestOpenVaguePropertiesOrNbt;
         if (this.reader.canRead() && this.reader.peek() == '[') {
            this.readVagueProperties();
            this.suggestions = this::suggestOpenNbt;
         }
      } else {
         this.readBlock();
         this.suggestions = this::suggestOpenPropertiesOrNbt;
         if (this.reader.canRead() && this.reader.peek() == '[') {
            this.readProperties();
            this.suggestions = this::suggestOpenNbt;
         }
      }

      if (this.allowNbt && this.reader.canRead() && this.reader.peek() == '{') {
         this.suggestions = SUGGEST_NOTHING;
         this.readNbt();
      }
   }

   private CompletableFuture<Suggestions> suggestPropertyNameOrEnd(SuggestionsBuilder p_234684_) {
      if (p_234684_.getRemaining().isEmpty()) {
         p_234684_.suggest(String.valueOf(']'));
      }

      return this.suggestPropertyName(p_234684_);
   }

   private CompletableFuture<Suggestions> suggestVaguePropertyNameOrEnd(SuggestionsBuilder p_234715_) {
      if (p_234715_.getRemaining().isEmpty()) {
         p_234715_.suggest(String.valueOf(']'));
      }

      return this.suggestVaguePropertyName(p_234715_);
   }

   private CompletableFuture<Suggestions> suggestPropertyName(SuggestionsBuilder p_234729_) {
      String $$1 = p_234729_.getRemaining().toLowerCase(Locale.ROOT);

      for (Property<?> $$2 : this.state.getProperties()) {
         if (!this.properties.containsKey($$2) && $$2.getName().startsWith($$1)) {
            p_234729_.suggest($$2.getName() + "=");
         }
      }

      return p_234729_.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestVaguePropertyName(SuggestionsBuilder p_234731_) {
      String $$1 = p_234731_.getRemaining().toLowerCase(Locale.ROOT);
      if (this.tag != null) {
         for (Holder<Block> $$2 : this.tag) {
            for (Property<?> $$3 : $$2.value().getStateDefinition().getProperties()) {
               if (!this.vagueProperties.containsKey($$3.getName()) && $$3.getName().startsWith($$1)) {
                  p_234731_.suggest($$3.getName() + "=");
               }
            }
         }
      }

      return p_234731_.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestOpenNbt(SuggestionsBuilder p_234733_) {
      if (p_234733_.getRemaining().isEmpty() && this.hasBlockEntity()) {
         p_234733_.suggest(String.valueOf('{'));
      }

      return p_234733_.buildFuture();
   }

   private boolean hasBlockEntity() {
      if (this.state != null) {
         return this.state.hasBlockEntity();
      } else {
         if (this.tag != null) {
            for (Holder<Block> $$0 : this.tag) {
               if ($$0.value().defaultBlockState().hasBlockEntity()) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private CompletableFuture<Suggestions> suggestEquals(SuggestionsBuilder p_234735_) {
      if (p_234735_.getRemaining().isEmpty()) {
         p_234735_.suggest(String.valueOf('='));
      }

      return p_234735_.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestNextPropertyOrEnd(SuggestionsBuilder p_234737_) {
      if (p_234737_.getRemaining().isEmpty()) {
         p_234737_.suggest(String.valueOf(']'));
      }

      if (p_234737_.getRemaining().isEmpty() && this.properties.size() < this.state.getProperties().size()) {
         p_234737_.suggest(String.valueOf(','));
      }

      return p_234737_.buildFuture();
   }

   private static <T extends Comparable<T>> SuggestionsBuilder addSuggestions(SuggestionsBuilder p_116787_, Property<T> p_116788_) {
      for (T $$2 : p_116788_.getPossibleValues()) {
         if ($$2 instanceof Integer $$3) {
            p_116787_.suggest($$3);
         } else {
            p_116787_.suggest(p_116788_.getName($$2));
         }
      }

      return p_116787_;
   }

   private CompletableFuture<Suggestions> suggestVaguePropertyValue(SuggestionsBuilder p_234686_, String p_234687_) {
      boolean $$2 = false;
      if (this.tag != null) {
         for (Holder<Block> $$3 : this.tag) {
            Block $$4 = $$3.value();
            Property<?> $$5 = $$4.getStateDefinition().getProperty(p_234687_);
            if ($$5 != null) {
               addSuggestions(p_234686_, $$5);
            }

            if (!$$2) {
               for (Property<?> $$6 : $$4.getStateDefinition().getProperties()) {
                  if (!this.vagueProperties.containsKey($$6.getName())) {
                     $$2 = true;
                     break;
                  }
               }
            }
         }
      }

      if ($$2) {
         p_234686_.suggest(String.valueOf(','));
      }

      p_234686_.suggest(String.valueOf(']'));
      return p_234686_.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestOpenVaguePropertiesOrNbt(SuggestionsBuilder p_234739_) {
      if (p_234739_.getRemaining().isEmpty() && this.tag != null) {
         boolean $$1 = false;
         boolean $$2 = false;

         for (Holder<Block> $$3 : this.tag) {
            Block $$4 = $$3.value();
            $$1 |= !$$4.getStateDefinition().getProperties().isEmpty();
            $$2 |= $$4.defaultBlockState().hasBlockEntity();
            if ($$1 && $$2) {
               break;
            }
         }

         if ($$1) {
            p_234739_.suggest(String.valueOf('['));
         }

         if ($$2) {
            p_234739_.suggest(String.valueOf('{'));
         }
      }

      return p_234739_.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestOpenPropertiesOrNbt(SuggestionsBuilder p_234741_) {
      if (p_234741_.getRemaining().isEmpty()) {
         if (!this.definition.getProperties().isEmpty()) {
            p_234741_.suggest(String.valueOf('['));
         }

         if (this.state.hasBlockEntity()) {
            p_234741_.suggest(String.valueOf('{'));
         }
      }

      return p_234741_.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestTag(SuggestionsBuilder p_234743_) {
      return SharedSuggestionProvider.suggestResource(this.blocks.listTagIds().map(TagKey::location), p_234743_, String.valueOf('#'));
   }

   private CompletableFuture<Suggestions> suggestItem(SuggestionsBuilder p_234745_) {
      return SharedSuggestionProvider.suggestResource(this.blocks.listElementIds().map(ResourceKey::location), p_234745_);
   }

   private CompletableFuture<Suggestions> suggestBlockIdOrTag(SuggestionsBuilder p_234747_) {
      this.suggestTag(p_234747_);
      this.suggestItem(p_234747_);
      return p_234747_.buildFuture();
   }

   private void readBlock() throws CommandSyntaxException {
      int $$0 = this.reader.getCursor();
      this.id = ResourceLocation.read(this.reader);
      Block $$1 = this.blocks.get(ResourceKey.create(Registries.BLOCK, this.id)).orElseThrow(() -> {
         this.reader.setCursor($$0);
         return ERROR_UNKNOWN_BLOCK.createWithContext(this.reader, this.id.toString());
      }).value();
      this.definition = $$1.getStateDefinition();
      this.state = $$1.defaultBlockState();
   }

   private void readTag() throws CommandSyntaxException {
      if (!this.forTesting) {
         throw ERROR_NO_TAGS_ALLOWED.createWithContext(this.reader);
      } else {
         int $$0 = this.reader.getCursor();
         this.reader.expect('#');
         this.suggestions = this::suggestTag;
         ResourceLocation $$1 = ResourceLocation.read(this.reader);
         this.tag = this.blocks.get(TagKey.create(Registries.BLOCK, $$1)).orElseThrow(() -> {
            this.reader.setCursor($$0);
            return ERROR_UNKNOWN_TAG.createWithContext(this.reader, $$1.toString());
         });
      }
   }

   private void readProperties() throws CommandSyntaxException {
      this.reader.skip();
      this.suggestions = this::suggestPropertyNameOrEnd;
      this.reader.skipWhitespace();

      while (this.reader.canRead() && this.reader.peek() != ']') {
         this.reader.skipWhitespace();
         int $$0 = this.reader.getCursor();
         String $$1 = this.reader.readString();
         Property<?> $$2 = this.definition.getProperty($$1);
         if ($$2 == null) {
            this.reader.setCursor($$0);
            throw ERROR_UNKNOWN_PROPERTY.createWithContext(this.reader, this.id.toString(), $$1);
         }

         if (this.properties.containsKey($$2)) {
            this.reader.setCursor($$0);
            throw ERROR_DUPLICATE_PROPERTY.createWithContext(this.reader, this.id.toString(), $$1);
         }

         this.reader.skipWhitespace();
         this.suggestions = this::suggestEquals;
         if (!this.reader.canRead() || this.reader.peek() != '=') {
            throw ERROR_EXPECTED_VALUE.createWithContext(this.reader, this.id.toString(), $$1);
         }

         this.reader.skip();
         this.reader.skipWhitespace();
         this.suggestions = p_234690_ -> addSuggestions(p_234690_, $$2).buildFuture();
         int $$3 = this.reader.getCursor();
         this.setValue($$2, this.reader.readString(), $$3);
         this.suggestions = this::suggestNextPropertyOrEnd;
         this.reader.skipWhitespace();
         if (this.reader.canRead()) {
            if (this.reader.peek() != ',') {
               if (this.reader.peek() != ']') {
                  throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
               }
               break;
            }

            this.reader.skip();
            this.suggestions = this::suggestPropertyName;
         }
      }

      if (this.reader.canRead()) {
         this.reader.skip();
      } else {
         throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
      }
   }

   private void readVagueProperties() throws CommandSyntaxException {
      this.reader.skip();
      this.suggestions = this::suggestVaguePropertyNameOrEnd;
      int $$0 = -1;
      this.reader.skipWhitespace();

      while (this.reader.canRead() && this.reader.peek() != ']') {
         this.reader.skipWhitespace();
         int $$1 = this.reader.getCursor();
         String $$2 = this.reader.readString();
         if (this.vagueProperties.containsKey($$2)) {
            this.reader.setCursor($$1);
            throw ERROR_DUPLICATE_PROPERTY.createWithContext(this.reader, this.id.toString(), $$2);
         }

         this.reader.skipWhitespace();
         if (!this.reader.canRead() || this.reader.peek() != '=') {
            this.reader.setCursor($$1);
            throw ERROR_EXPECTED_VALUE.createWithContext(this.reader, this.id.toString(), $$2);
         }

         this.reader.skip();
         this.reader.skipWhitespace();
         this.suggestions = p_234712_ -> this.suggestVaguePropertyValue(p_234712_, $$2);
         $$0 = this.reader.getCursor();
         String $$3 = this.reader.readString();
         this.vagueProperties.put($$2, $$3);
         this.reader.skipWhitespace();
         if (this.reader.canRead()) {
            $$0 = -1;
            if (this.reader.peek() != ',') {
               if (this.reader.peek() != ']') {
                  throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
               }
               break;
            }

            this.reader.skip();
            this.suggestions = this::suggestVaguePropertyName;
         }
      }

      if (this.reader.canRead()) {
         this.reader.skip();
      } else {
         if ($$0 >= 0) {
            this.reader.setCursor($$0);
         }

         throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
      }
   }

   private void readNbt() throws CommandSyntaxException {
      this.nbt = new TagParser(this.reader).readStruct();
   }

   private <T extends Comparable<T>> void setValue(Property<T> p_116776_, String p_116777_, int p_116778_) throws CommandSyntaxException {
      Optional<T> $$3 = p_116776_.getValue(p_116777_);
      if ($$3.isPresent()) {
         this.state = this.state.setValue(p_116776_, $$3.get());
         this.properties.put(p_116776_, $$3.get());
      } else {
         this.reader.setCursor(p_116778_);
         throw ERROR_INVALID_VALUE.createWithContext(this.reader, this.id.toString(), p_116776_.getName(), p_116777_);
      }
   }

   public static String serialize(BlockState p_116770_) {
      StringBuilder $$1 = new StringBuilder(p_116770_.getBlockHolder().unwrapKey().map(p_234682_ -> p_234682_.location().toString()).orElse("air"));
      if (!p_116770_.getProperties().isEmpty()) {
         $$1.append('[');
         boolean $$2 = false;

         for (Entry<Property<?>, Comparable<?>> $$3 : p_116770_.getValues().entrySet()) {
            if ($$2) {
               $$1.append(',');
            }

            appendProperty($$1, $$3.getKey(), $$3.getValue());
            $$2 = true;
         }

         $$1.append(']');
      }

      return $$1.toString();
   }

   private static <T extends Comparable<T>> void appendProperty(StringBuilder p_116803_, Property<T> p_116804_, Comparable<?> p_116805_) {
      p_116803_.append(p_116804_.getName());
      p_116803_.append('=');
      p_116803_.append(p_116804_.getName((T)p_116805_));
   }

   public record BlockResult(BlockState blockState, Map<Property<?>, Comparable<?>> properties, @Nullable CompoundTag nbt) {
   }

   public record TagResult(HolderSet<Block> tag, Map<String, String> vagueProperties, @Nullable CompoundTag nbt) {
   }
}
