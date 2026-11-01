package net.neoforged.neoforge.server.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public final class CommandUtils {
   private CommandUtils() {
   }

   public static CompletableFuture<Suggestions> suggestRegistries(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
      return SharedSuggestionProvider.suggestResource(
         ((CommandSourceStack)ctx.getSource()).registryAccess().listRegistryKeys().map(ResourceKey::identifier), builder
      );
   }

   public static <T extends Registry<?>> SuggestionProvider<CommandSourceStack> suggestFromRegistry(
      Function<Registry<?>, Iterable<Identifier>> namesFunction, String argumentString, ResourceKey<Registry<T>> registryKey
   ) {
      return (ctx, builder) -> getResourceKey(ctx, argumentString, registryKey)
         .flatMap(key -> ((CommandSourceStack)ctx.getSource()).registryAccess().lookup(key).map(registry -> {
            SharedSuggestionProvider.suggestResource(namesFunction.apply((Registry<?>)registry), builder);
            return builder.buildFuture();
         }))
         .orElseGet(builder::buildFuture);
   }

   public static <T> Optional<ResourceKey<T>> getResourceKey(CommandContext<CommandSourceStack> ctx, String name, ResourceKey<Registry<T>> registryKey) {
      ResourceKey<?> key = (ResourceKey<?>)ctx.getArgument(name, ResourceKey.class);
      return key.cast(registryKey);
   }

   public static MutableComponent makeTranslatableWithFallback(String key, Object... args) {
      String fallback = Language.getInstance().getOrDefault(key);
      return Component.translatableWithFallback(key, fallback, args);
   }

   public static MutableComponent makeTranslatableWithFallback(String key) {
      String fallback = Language.getInstance().getOrDefault(key);
      return Component.translatableWithFallback(key, fallback);
   }
}
