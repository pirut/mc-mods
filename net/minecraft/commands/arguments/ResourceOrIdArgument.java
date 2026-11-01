package net.minecraft.commands.arguments;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtGrammar;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.Dictionary;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.Term;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.util.parsing.packrat.commands.IdentifierParseRule;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jspecify.annotations.Nullable;

public class ResourceOrIdArgument<T> implements ArgumentType<Holder<T>> {
   private static final Collection<String> EXAMPLES = List.of("foo", "foo:bar", "012", "{}", "true");
   public static final DynamicCommandExceptionType ERROR_FAILED_TO_PARSE = new DynamicCommandExceptionType(
      p_335811_ -> Component.translatableEscape("argument.resource_or_id.failed_to_parse", p_335811_)
   );
   public static final Dynamic2CommandExceptionType ERROR_NO_SUCH_ELEMENT = new Dynamic2CommandExceptionType(
      (p_421294_, p_421295_) -> Component.translatableEscape("argument.resource_or_id.no_such_element", p_421294_, p_421295_)
   );
   public static final DynamicOps<Tag> OPS = NbtOps.INSTANCE;
   private final HolderLookup.Provider registryLookup;
   private final Optional<? extends HolderLookup.RegistryLookup<T>> elementLookup;
   private final Codec<T> codec;
   private final Grammar<ResourceOrIdArgument.Result<T, Tag>> grammar;
   private final ResourceKey<? extends Registry<T>> registryKey;

   protected ResourceOrIdArgument(CommandBuildContext p_335864_, ResourceKey<? extends Registry<T>> p_335475_, Codec<T> p_335632_) {
      this.registryLookup = p_335864_;
      this.elementLookup = p_335864_.lookup(p_335475_);
      this.registryKey = p_335475_;
      this.codec = p_335632_;
      this.grammar = createGrammar(p_335475_, OPS);
   }

   public static <T, O> Grammar<ResourceOrIdArgument.Result<T, O>> createGrammar(ResourceKey<? extends Registry<T>> p_422103_, DynamicOps<O> p_421543_) {
      Grammar<O> grammar = SnbtGrammar.createParser((DynamicOps<T>)p_421543_);
      Dictionary<StringReader> dictionary = new Dictionary<>();
      Atom<ResourceOrIdArgument.Result<T, O>> atom = Atom.of("result");
      Atom<Identifier> atom1 = Atom.of("id");
      Atom<O> atom2 = Atom.of("value");
      dictionary.put(atom1, IdentifierParseRule.INSTANCE);
      dictionary.put(atom2, grammar.top().value());
      NamedRule<StringReader, ResourceOrIdArgument.Result<T, O>> namedrule = dictionary.put(
         atom, Term.alternative(dictionary.named(atom1), dictionary.named(atom2)), p_465825_ -> {
            Identifier identifier = p_465825_.get(atom1);
            if (identifier != null) {
               return new ResourceOrIdArgument.ReferenceResult<>(ResourceKey.create(p_422103_, identifier));
            } else {
               O o = p_465825_.getOrThrow(atom2);
               return new ResourceOrIdArgument.InlineResult<>(o);
            }
         }
      );
      return new Grammar<>(dictionary, namedrule);
   }

   public static ResourceOrIdArgument.LootTableArgument lootTable(CommandBuildContext p_335938_) {
      return new ResourceOrIdArgument.LootTableArgument(p_335938_);
   }

   public static Holder<LootTable> getLootTable(CommandContext<CommandSourceStack> p_335373_, String p_336017_) throws CommandSyntaxException {
      return getResource(p_335373_, p_336017_);
   }

   public static ResourceOrIdArgument.LootModifierArgument lootModifier(CommandBuildContext p_335731_) {
      return new ResourceOrIdArgument.LootModifierArgument(p_335731_);
   }

   public static Holder<LootItemFunction> getLootModifier(CommandContext<CommandSourceStack> p_335678_, String p_336178_) {
      return getResource(p_335678_, p_336178_);
   }

   public static ResourceOrIdArgument.LootPredicateArgument lootPredicate(CommandBuildContext p_335891_) {
      return new ResourceOrIdArgument.LootPredicateArgument(p_335891_);
   }

   public static Holder<LootItemCondition> getLootPredicate(CommandContext<CommandSourceStack> p_336183_, String p_336098_) {
      return getResource(p_336183_, p_336098_);
   }

   public static ResourceOrIdArgument.DialogArgument dialog(CommandBuildContext p_425646_) {
      return new ResourceOrIdArgument.DialogArgument(p_425646_);
   }

   public static Holder<Dialog> getDialog(CommandContext<CommandSourceStack> p_426065_, String p_426142_) {
      return getResource(p_426065_, p_426142_);
   }

   private static <T> Holder<T> getResource(CommandContext<CommandSourceStack> p_336122_, String p_335417_) {
      return (Holder<T>)p_336122_.getArgument(p_335417_, Holder.class);
   }

   @Nullable
   public Holder<T> parse(StringReader p_335906_) throws CommandSyntaxException {
      return this.parse(p_335906_, this.grammar, OPS);
   }

   @Nullable
   private <O> Holder<T> parse(StringReader p_412491_, Grammar<ResourceOrIdArgument.Result<T, O>> p_421894_, DynamicOps<O> p_421523_) throws CommandSyntaxException {
      ResourceOrIdArgument.Result<T, O> result = p_421894_.parseForCommands(p_412491_);
      return this.elementLookup.isEmpty()
         ? null
         : result.parse(p_412491_, this.registryLookup, p_421523_, this.codec, (HolderLookup.RegistryLookup<T>)this.elementLookup.get());
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_425917_, SuggestionsBuilder p_425995_) {
      return SharedSuggestionProvider.listSuggestions(p_425917_, p_425995_, this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static class DialogArgument extends ResourceOrIdArgument<Dialog> {
      protected DialogArgument(CommandBuildContext p_426177_) {
         super(p_426177_, Registries.DIALOG, Dialog.DIRECT_CODEC);
      }
   }

   public record InlineResult<T, O>(O value) implements ResourceOrIdArgument.Result<T, O> {
      @Override
      public Holder<T> parse(
         ImmutableStringReader p_421858_,
         HolderLookup.Provider p_422417_,
         DynamicOps<O> p_421627_,
         Codec<T> p_422188_,
         HolderLookup.RegistryLookup<T> p_421743_
      ) throws CommandSyntaxException {
         return Holder.direct(
            (T)p_422188_.parse(p_422417_.createSerializationContext(p_421627_), this.value)
               .getOrThrow(p_421517_ -> ResourceOrIdArgument.ERROR_FAILED_TO_PARSE.createWithContext(p_421858_, p_421517_))
         );
      }
   }

   public static class LootModifierArgument extends ResourceOrIdArgument<LootItemFunction> {
      protected LootModifierArgument(CommandBuildContext p_335547_) {
         super(p_335547_, Registries.ITEM_MODIFIER, LootItemFunctions.ROOT_CODEC);
      }
   }

   public static class LootPredicateArgument extends ResourceOrIdArgument<LootItemCondition> {
      protected LootPredicateArgument(CommandBuildContext p_336020_) {
         super(p_336020_, Registries.PREDICATE, LootItemCondition.DIRECT_CODEC);
      }
   }

   public static class LootTableArgument extends ResourceOrIdArgument<LootTable> {
      protected LootTableArgument(CommandBuildContext p_335769_) {
         super(p_335769_, Registries.LOOT_TABLE, LootTable.DIRECT_CODEC);
      }
   }

   public record ReferenceResult<T, O>(ResourceKey<T> key) implements ResourceOrIdArgument.Result<T, O> {
      @Override
      public Holder<T> parse(
         ImmutableStringReader p_422449_,
         HolderLookup.Provider p_422045_,
         DynamicOps<O> p_421653_,
         Codec<T> p_422608_,
         HolderLookup.RegistryLookup<T> p_421710_
      ) throws CommandSyntaxException {
         return p_421710_.get(this.key)
            .orElseThrow(() -> ResourceOrIdArgument.ERROR_NO_SUCH_ELEMENT.createWithContext(p_422449_, this.key.identifier(), this.key.registry()));
      }
   }

   public sealed interface Result<T, O> permits ResourceOrIdArgument.InlineResult, ResourceOrIdArgument.ReferenceResult {
      Holder<T> parse(ImmutableStringReader var1, HolderLookup.Provider var2, DynamicOps<O> var3, Codec<T> var4, HolderLookup.RegistryLookup<T> var5) throws CommandSyntaxException;
   }
}
