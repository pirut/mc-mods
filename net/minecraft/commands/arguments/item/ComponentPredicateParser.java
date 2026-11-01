package net.minecraft.commands.arguments.item;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.Dictionary;
import net.minecraft.util.parsing.packrat.Term;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.util.parsing.packrat.commands.ResourceLocationParseRule;
import net.minecraft.util.parsing.packrat.commands.ResourceLookupRule;
import net.minecraft.util.parsing.packrat.commands.StringReaderTerms;
import net.minecraft.util.parsing.packrat.commands.TagParseRule;

public class ComponentPredicateParser {
   public static <T, C, P> Grammar<List<T>> createGrammar(ComponentPredicateParser.Context<T, C, P> p_335727_) {
      Atom<List<T>> $$1 = Atom.of("top");
      Atom<Optional<T>> $$2 = Atom.of("type");
      Atom<Unit> $$3 = Atom.of("any_type");
      Atom<T> $$4 = Atom.of("element_type");
      Atom<T> $$5 = Atom.of("tag_type");
      Atom<List<T>> $$6 = Atom.of("conditions");
      Atom<List<T>> $$7 = Atom.of("alternatives");
      Atom<T> $$8 = Atom.of("term");
      Atom<T> $$9 = Atom.of("negation");
      Atom<T> $$10 = Atom.of("test");
      Atom<C> $$11 = Atom.of("component_type");
      Atom<P> $$12 = Atom.of("predicate_type");
      Atom<ResourceLocation> $$13 = Atom.of("id");
      Atom<Tag> $$14 = Atom.of("tag");
      Dictionary<StringReader> $$15 = new Dictionary<>();
      $$15.put(
         $$1,
         Term.alternative(
            Term.sequence(Term.named($$2), StringReaderTerms.character('['), Term.cut(), Term.optional(Term.named($$6)), StringReaderTerms.character(']')),
            Term.named($$2)
         ),
         p_336103_ -> {
            Builder<T> $$3x = ImmutableList.builder();
            p_336103_.getOrThrow($$2).ifPresent($$3x::add);
            List<T> $$4x = p_336103_.get($$6);
            if ($$4x != null) {
               $$3x.addAll($$4x);
            }

            return $$3x.build();
         }
      );
      $$15.put(
         $$2,
         Term.alternative(Term.named($$4), Term.sequence(StringReaderTerms.character('#'), Term.cut(), Term.named($$5)), Term.named($$3)),
         p_335800_ -> Optional.ofNullable(p_335800_.getAny($$4, $$5))
      );
      $$15.put($$3, StringReaderTerms.character('*'), p_335962_ -> Unit.INSTANCE);
      $$15.put($$4, new ComponentPredicateParser.ElementLookupRule<>($$13, p_335727_));
      $$15.put($$5, new ComponentPredicateParser.TagLookupRule<>($$13, p_335727_));
      $$15.put($$6, Term.sequence(Term.named($$7), Term.optional(Term.sequence(StringReaderTerms.character(','), Term.named($$6)))), p_336093_ -> {
         T $$4x = p_335727_.anyOf(p_336093_.getOrThrow($$7));
         return Optional.ofNullable(p_336093_.get($$6)).map(p_335806_ -> Util.copyAndAdd($$4x, (List<T>)p_335806_)).orElse(List.of($$4x));
      });
      $$15.put($$7, Term.sequence(Term.named($$8), Term.optional(Term.sequence(StringReaderTerms.character('|'), Term.named($$7)))), p_335542_ -> {
         T $$3x = p_335542_.getOrThrow($$8);
         return Optional.ofNullable(p_335542_.get($$7)).map(p_335939_ -> Util.copyAndAdd($$3x, (List<T>)p_335939_)).orElse(List.of($$3x));
      });
      $$15.put(
         $$8,
         Term.alternative(Term.named($$10), Term.sequence(StringReaderTerms.character('!'), Term.named($$9))),
         p_335647_ -> p_335647_.getAnyOrThrow($$10, $$9)
      );
      $$15.put($$9, Term.named($$10), p_335793_ -> p_335727_.negate(p_335793_.getOrThrow($$10)));
      $$15.put(
         $$10,
         Term.alternative(
            Term.sequence(Term.named($$11), StringReaderTerms.character('='), Term.cut(), Term.named($$14)),
            Term.sequence(Term.named($$12), StringReaderTerms.character('~'), Term.cut(), Term.named($$14)),
            Term.named($$11)
         ),
         (p_335570_, p_336062_) -> {
            P $$6x = p_336062_.get($$12);

            try {
               if ($$6x != null) {
                  Tag $$7x = p_336062_.getOrThrow($$14);
                  return Optional.of(p_335727_.createPredicateTest((ImmutableStringReader)p_335570_.input(), $$6x, $$7x));
               } else {
                  C $$8x = p_336062_.getOrThrow($$11);
                  Tag $$9x = p_336062_.get($$14);
                  return Optional.of(
                     $$9x != null
                        ? p_335727_.createComponentTest((ImmutableStringReader)p_335570_.input(), $$8x, $$9x)
                        : p_335727_.createComponentTest((ImmutableStringReader)p_335570_.input(), $$8x)
                  );
               }
            } catch (CommandSyntaxException var9x) {
               p_335570_.errorCollector().store(p_335570_.mark(), var9x);
               return Optional.empty();
            }
         }
      );
      $$15.put($$11, new ComponentPredicateParser.ComponentLookupRule<>($$13, p_335727_));
      $$15.put($$12, new ComponentPredicateParser.PredicateLookupRule<>($$13, p_335727_));
      $$15.put($$14, TagParseRule.INSTANCE);
      $$15.put($$13, ResourceLocationParseRule.INSTANCE);
      return new Grammar<>($$15, $$1);
   }

   static class ComponentLookupRule<T, C, P> extends ResourceLookupRule<ComponentPredicateParser.Context<T, C, P>, C> {
      ComponentLookupRule(Atom<ResourceLocation> p_335669_, ComponentPredicateParser.Context<T, C, P> p_335692_) {
         super(p_335669_, p_335692_);
      }

      @Override
      protected C validateElement(ImmutableStringReader p_335604_, ResourceLocation p_335964_) throws Exception {
         return this.context.lookupComponentType(p_335604_, p_335964_);
      }

      @Override
      public Stream<ResourceLocation> possibleResources() {
         return this.context.listComponentTypes();
      }
   }

   public interface Context<T, C, P> {
      T forElementType(ImmutableStringReader var1, ResourceLocation var2) throws CommandSyntaxException;

      Stream<ResourceLocation> listElementTypes();

      T forTagType(ImmutableStringReader var1, ResourceLocation var2) throws CommandSyntaxException;

      Stream<ResourceLocation> listTagTypes();

      C lookupComponentType(ImmutableStringReader var1, ResourceLocation var2) throws CommandSyntaxException;

      Stream<ResourceLocation> listComponentTypes();

      T createComponentTest(ImmutableStringReader var1, C var2, Tag var3) throws CommandSyntaxException;

      T createComponentTest(ImmutableStringReader var1, C var2);

      P lookupPredicateType(ImmutableStringReader var1, ResourceLocation var2) throws CommandSyntaxException;

      Stream<ResourceLocation> listPredicateTypes();

      T createPredicateTest(ImmutableStringReader var1, P var2, Tag var3) throws CommandSyntaxException;

      T negate(T var1);

      T anyOf(List<T> var1);
   }

   static class ElementLookupRule<T, C, P> extends ResourceLookupRule<ComponentPredicateParser.Context<T, C, P>, T> {
      ElementLookupRule(Atom<ResourceLocation> p_335457_, ComponentPredicateParser.Context<T, C, P> p_335797_) {
         super(p_335457_, p_335797_);
      }

      @Override
      protected T validateElement(ImmutableStringReader p_335629_, ResourceLocation p_335530_) throws Exception {
         return this.context.forElementType(p_335629_, p_335530_);
      }

      @Override
      public Stream<ResourceLocation> possibleResources() {
         return this.context.listElementTypes();
      }
   }

   static class PredicateLookupRule<T, C, P> extends ResourceLookupRule<ComponentPredicateParser.Context<T, C, P>, P> {
      PredicateLookupRule(Atom<ResourceLocation> p_335495_, ComponentPredicateParser.Context<T, C, P> p_335572_) {
         super(p_335495_, p_335572_);
      }

      @Override
      protected P validateElement(ImmutableStringReader p_336035_, ResourceLocation p_335697_) throws Exception {
         return this.context.lookupPredicateType(p_336035_, p_335697_);
      }

      @Override
      public Stream<ResourceLocation> possibleResources() {
         return this.context.listPredicateTypes();
      }
   }

   static class TagLookupRule<T, C, P> extends ResourceLookupRule<ComponentPredicateParser.Context<T, C, P>, T> {
      TagLookupRule(Atom<ResourceLocation> p_335960_, ComponentPredicateParser.Context<T, C, P> p_335934_) {
         super(p_335960_, p_335934_);
      }

      @Override
      protected T validateElement(ImmutableStringReader p_335858_, ResourceLocation p_335888_) throws Exception {
         return this.context.forTagType(p_335858_, p_335888_);
      }

      @Override
      public Stream<ResourceLocation> possibleResources() {
         return this.context.listTagTypes();
      }
   }
}
