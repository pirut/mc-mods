package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.apache.commons.io.FilenameUtils;

public class ResourceSelectorArgument<T> implements ArgumentType<Collection<Holder.Reference<T>>> {
   private static final Collection<String> EXAMPLES = List.of("minecraft:*", "*:asset", "*");
   public static final Dynamic2CommandExceptionType ERROR_NO_MATCHES = new Dynamic2CommandExceptionType(
      (p_397946_, p_397580_) -> Component.translatableEscape("argument.resource_selector.not_found", p_397946_, p_397580_)
   );
   final ResourceKey<? extends Registry<T>> registryKey;
   private final HolderLookup<T> registryLookup;

   ResourceSelectorArgument(CommandBuildContext p_397758_, ResourceKey<? extends Registry<T>> p_397841_) {
      this.registryKey = p_397841_;
      this.registryLookup = p_397758_.lookupOrThrow(p_397841_);
   }

   public Collection<Holder.Reference<T>> parse(StringReader p_397392_) throws CommandSyntaxException {
      String s = ensureNamespaced(readPattern(p_397392_));
      List<Holder.Reference<T>> list = this.registryLookup.listElements().filter(p_465838_ -> matches(s, p_465838_.key().identifier())).toList();
      if (list.isEmpty()) {
         throw ERROR_NO_MATCHES.createWithContext(p_397392_, s, this.registryKey.identifier());
      } else {
         return list;
      }
   }

   public static <T> Collection<Holder.Reference<T>> parse(StringReader p_397046_, HolderLookup<T> p_397718_) {
      String s = ensureNamespaced(readPattern(p_397046_));
      return p_397718_.listElements().filter(p_465836_ -> matches(s, p_465836_.key().identifier())).toList();
   }

   private static String readPattern(StringReader p_397824_) {
      int i = p_397824_.getCursor();

      while (p_397824_.canRead() && isAllowedPatternCharacter(p_397824_.peek())) {
         p_397824_.skip();
      }

      return p_397824_.getString().substring(i, p_397824_.getCursor());
   }

   private static boolean isAllowedPatternCharacter(char p_397389_) {
      return Identifier.isAllowedInIdentifier(p_397389_) || p_397389_ == '*' || p_397389_ == '?';
   }

   private static String ensureNamespaced(String p_397692_) {
      return !p_397692_.contains(":") ? "minecraft:" + p_397692_ : p_397692_;
   }

   private static boolean matches(String p_397067_, Identifier p_468024_) {
      return FilenameUtils.wildcardMatch(p_468024_.toString(), p_397067_);
   }

   public static <T> ResourceSelectorArgument<T> resourceSelector(CommandBuildContext p_397004_, ResourceKey<? extends Registry<T>> p_397925_) {
      return new ResourceSelectorArgument<>(p_397004_, p_397925_);
   }

   public static <T> Collection<Holder.Reference<T>> getSelectedResources(CommandContext<CommandSourceStack> p_397945_, String p_397186_) {
      return (Collection<Holder.Reference<T>>)p_397945_.getArgument(p_397186_, Collection.class);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_397613_, SuggestionsBuilder p_397790_) {
      return SharedSuggestionProvider.listSuggestions(p_397613_, p_397790_, this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static class Info<T> implements ArgumentTypeInfo<ResourceSelectorArgument<T>, ResourceSelectorArgument.Info<T>.Template> {
      public void serializeToNetwork(ResourceSelectorArgument.Info<T>.Template p_397335_, FriendlyByteBuf p_397499_) {
         p_397499_.writeResourceKey(p_397335_.registryKey);
      }

      public ResourceSelectorArgument.Info<T>.Template deserializeFromNetwork(FriendlyByteBuf p_397826_) {
         return new ResourceSelectorArgument.Info.Template(p_397826_.readRegistryKey());
      }

      public void serializeToJson(ResourceSelectorArgument.Info<T>.Template p_397615_, JsonObject p_396994_) {
         p_396994_.addProperty("registry", p_397615_.registryKey.identifier().toString());
      }

      public ResourceSelectorArgument.Info<T>.Template unpack(ResourceSelectorArgument<T> p_397275_) {
         return new ResourceSelectorArgument.Info.Template(p_397275_.registryKey);
      }

      public final class Template implements ArgumentTypeInfo.Template<ResourceSelectorArgument<T>> {
         final ResourceKey<? extends Registry<T>> registryKey;

         Template(ResourceKey<? extends Registry<T>> p_397772_) {
            this.registryKey = p_397772_;
         }

         public ResourceSelectorArgument<T> instantiate(CommandBuildContext p_397320_) {
            return new ResourceSelectorArgument<>(p_397320_, this.registryKey);
         }

         @Override
         public ArgumentTypeInfo<ResourceSelectorArgument<T>, ?> type() {
            return Info.this;
         }
      }
   }
}
