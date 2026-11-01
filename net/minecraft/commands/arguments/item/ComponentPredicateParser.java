package net.minecraft.commands.arguments.item;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.Dictionary;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.Scope;
import net.minecraft.util.parsing.packrat.Term;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.util.parsing.packrat.commands.IdentifierParseRule;
import net.minecraft.util.parsing.packrat.commands.ResourceLookupRule;
import net.minecraft.util.parsing.packrat.commands.StringReaderTerms;
import net.minecraft.util.parsing.packrat.commands.TagParseRule;

public class ComponentPredicateParser {
   public static <T, C, P> Grammar<List<T>> createGrammar(ComponentPredicateParser.Context<T, C, P> context) {
      Atom<List<T>> atom = Atom.of("top");
      Atom<Optional<T>> atom2 = Atom.of("type");
      Atom<Unit> atom3 = Atom.of("any_type");
      Atom<T> atom4 = Atom.of("element_type");
      Atom<T> atom5 = Atom.of("tag_type");
      Atom<List<T>> atom6 = Atom.of("conditions");
      Atom<List<T>> atom7 = Atom.of("alternatives");
      Atom<T> atom8 = Atom.of("term");
      Atom<T> atom9 = Atom.of("negation");
      Atom<T> atom10 = Atom.of("test");
      Atom<C> atom11 = Atom.of("component_type");
      Atom<P> atom12 = Atom.of("predicate_type");
      Atom<Identifier> atom13 = Atom.of("id");
      Atom<Dynamic<?>> atom14 = Atom.of("tag");
      Dictionary<StringReader> dictionary = new Dictionary<>();
      NamedRule<StringReader, Identifier> namedRule = dictionary.put(atom13, IdentifierParseRule.INSTANCE);
      NamedRule<StringReader, List<T>> namedRule2 = dictionary.put(
         atom,
         Term.alternative(
            Term.sequence(
               dictionary.named(atom2), StringReaderTerms.character('['), Term.cut(), Term.optional(dictionary.named(atom6)), StringReaderTerms.character(']')
            ),
            dictionary.named(atom2)
         ),
         scope -> {
            Builder<T> builder = ImmutableList.builder();
            scope.getOrThrow(atom2).ifPresent(builder::add);
            List<T> list = scope.get(atom6);
            if (list != null) {
               builder.addAll(list);
            }

            return builder.build();
         }
      );
      dictionary.put(
         atom2,
         Term.alternative(
            dictionary.named(atom4), Term.sequence(StringReaderTerms.character('#'), Term.cut(), dictionary.named(atom5)), dictionary.named(atom3)
         ),
         scope -> Optional.ofNullable(scope.getAny(atom4, atom5))
      );
      dictionary.put(atom3, StringReaderTerms.character('*'), scope -> Unit.INSTANCE);
      dictionary.put(atom4, new ComponentPredicateParser.ElementLookupRule<>(namedRule, context));
      dictionary.put(atom5, new ComponentPredicateParser.TagLookupRule<>(namedRule, context));
      dictionary.put(
         atom6, Term.sequence(dictionary.named(atom7), Term.optional(Term.sequence(StringReaderTerms.character(','), dictionary.named(atom6)))), scope -> {
            T object = context.anyOf(scope.getOrThrow(atom7));
            return Optional.ofNullable(scope.get(atom6)).map(list -> Util.copyAndAdd(object, (List<T>)list)).orElse(List.of(object));
         }
      );
      dictionary.put(
         atom7, Term.sequence(dictionary.named(atom8), Term.optional(Term.sequence(StringReaderTerms.character('|'), dictionary.named(atom7)))), scope -> {
            T object = scope.getOrThrow(atom8);
            return Optional.ofNullable(scope.get(atom7)).map(list -> Util.copyAndAdd(object, (List<T>)list)).orElse(List.of(object));
         }
      );
      dictionary.put(
         atom8,
         Term.alternative(dictionary.named(atom10), Term.sequence(StringReaderTerms.character('!'), dictionary.named(atom9))),
         scope -> scope.getAnyOrThrow(atom10, atom9)
      );
      dictionary.put(atom9, dictionary.named(atom10), scope -> context.negate(scope.getOrThrow(atom10)));
      dictionary.putComplex(
         atom10,
         Term.alternative(
            Term.sequence(dictionary.named(atom11), StringReaderTerms.character('='), Term.cut(), dictionary.named(atom14)),
            Term.sequence(dictionary.named(atom12), StringReaderTerms.character('~'), Term.cut(), dictionary.named(atom14)),
            dictionary.named(atom11)
         ),
         parseState -> {
            Scope scope = parseState.scope();
            P object = scope.get(atom12);

            try {
               if (object != null) {
                  Dynamic<?> dynamic = scope.getOrThrow(atom14);
                  return context.createPredicateTest((ImmutableStringReader)parseState.input(), object, dynamic);
               } else {
                  C object2 = scope.getOrThrow(atom11);
                  Dynamic<?> dynamic2 = scope.get(atom14);
                  return dynamic2 != null
                     ? context.createComponentTest((ImmutableStringReader)parseState.input(), object2, dynamic2)
                     : context.createComponentTest((ImmutableStringReader)parseState.input(), object2);
               }
            } catch (CommandSyntaxException var9x) {
               parseState.errorCollector().store(parseState.mark(), var9x);
               return null;
            }
         }
      );
      dictionary.put(atom11, new ComponentPredicateParser.ComponentLookupRule<>(namedRule, context));
      dictionary.put(atom12, new ComponentPredicateParser.PredicateLookupRule<>(namedRule, context));
      dictionary.put(atom14, new TagParseRule(NbtOps.INSTANCE));
      return new Grammar<>(dictionary, namedRule2);
   }

   static class ComponentLookupRule<T, C, P> extends ResourceLookupRule<ComponentPredicateParser.Context<T, C, P>, C> {
      ComponentLookupRule(NamedRule<StringReader, Identifier> idParser, ComponentPredicateParser.Context<T, C, P> context) {
         super(idParser, context);
      }

      @Override
      protected C validateElement(ImmutableStringReader reader, Identifier elementType) throws Exception {
         return this.context.lookupComponentType(reader, elementType);
      }

      @Override
      public Stream<Identifier> possibleResources() {
         return this.context.listComponentTypes();
      }
   }

   public interface Context<T, C, P> {
      T forElementType(ImmutableStringReader reader, Identifier elementType) throws CommandSyntaxException;

      Stream<Identifier> listElementTypes();

      T forTagType(ImmutableStringReader reader, Identifier tagType) throws CommandSyntaxException;

      Stream<Identifier> listTagTypes();

      C lookupComponentType(ImmutableStringReader reader, Identifier componentType) throws CommandSyntaxException;

      Stream<Identifier> listComponentTypes();

      T createComponentTest(ImmutableStringReader reader, C context, Dynamic<?> data) throws CommandSyntaxException;

      T createComponentTest(ImmutableStringReader reader, C context);

      P lookupPredicateType(ImmutableStringReader reader, Identifier predicateType) throws CommandSyntaxException;

      Stream<Identifier> listPredicateTypes();

      T createPredicateTest(ImmutableStringReader reader, P predicateType, Dynamic<?> data) throws CommandSyntaxException;

      T negate(T value);

      T anyOf(List<T> values);
   }

   static class ElementLookupRule<T, C, P> extends ResourceLookupRule<ComponentPredicateParser.Context<T, C, P>, T> {
      ElementLookupRule(NamedRule<StringReader, Identifier> idParser, ComponentPredicateParser.Context<T, C, P> context) {
         super(idParser, context);
      }

      @Override
      protected T validateElement(ImmutableStringReader reader, Identifier elementType) throws Exception {
         return this.context.forElementType(reader, elementType);
      }

      @Override
      public Stream<Identifier> possibleResources() {
         return this.context.listElementTypes();
      }
   }

   static class PredicateLookupRule<T, C, P> extends ResourceLookupRule<ComponentPredicateParser.Context<T, C, P>, P> {
      PredicateLookupRule(NamedRule<StringReader, Identifier> idParser, ComponentPredicateParser.Context<T, C, P> context) {
         super(idParser, context);
      }

      @Override
      protected P validateElement(ImmutableStringReader reader, Identifier elementType) throws Exception {
         return this.context.lookupPredicateType(reader, elementType);
      }

      @Override
      public Stream<Identifier> possibleResources() {
         return this.context.listPredicateTypes();
      }
   }

   static class TagLookupRule<T, C, P> extends ResourceLookupRule<ComponentPredicateParser.Context<T, C, P>, T> {
      TagLookupRule(NamedRule<StringReader, Identifier> idParser, ComponentPredicateParser.Context<T, C, P> context) {
         super(idParser, context);
      }

      @Override
      protected T validateElement(ImmutableStringReader reader, Identifier elementType) throws Exception {
         return this.context.forTagType(reader, elementType);
      }

      @Override
      public Stream<Identifier> possibleResources() {
         return this.context.listTagTypes();
      }
   }
}
