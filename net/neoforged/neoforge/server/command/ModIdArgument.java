package net.neoforged.neoforge.server.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.SharedSuggestionProvider;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;

public class ModIdArgument implements ArgumentType<String> {
   private static final List<String> EXAMPLES = Arrays.asList("neoforge", "inventorysorter");

   public static ModIdArgument modIdArgument() {
      return new ModIdArgument();
   }

   public String parse(StringReader reader) throws CommandSyntaxException {
      return reader.readUnquotedString();
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return SharedSuggestionProvider.suggest(ModList.get().applyForEachModContainer(ModContainer::getModId), builder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
