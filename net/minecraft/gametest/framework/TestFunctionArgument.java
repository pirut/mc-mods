package net.minecraft.gametest.framework;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public class TestFunctionArgument implements ArgumentType<TestFunction> {
   private static final Collection<String> EXAMPLES = Arrays.asList("techtests.piston", "techtests");

   public TestFunction parse(StringReader p_128090_) throws CommandSyntaxException {
      String $$1 = p_128090_.readUnquotedString();
      Optional<TestFunction> $$2 = GameTestRegistry.findTestFunction($$1);
      if ($$2.isPresent()) {
         return $$2.get();
      } else {
         Message $$3 = Component.literal("No such test: " + $$1);
         throw new CommandSyntaxException(new SimpleCommandExceptionType($$3), $$3);
      }
   }

   public static TestFunctionArgument testFunctionArgument() {
      return new TestFunctionArgument();
   }

   public static TestFunction getTestFunction(CommandContext<CommandSourceStack> p_128092_, String p_128093_) {
      return (TestFunction)p_128092_.getArgument(p_128093_, TestFunction.class);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_128096_, SuggestionsBuilder p_128097_) {
      return suggestTestFunction(p_128096_, p_128097_);
   }

   public static <S> CompletableFuture<Suggestions> suggestTestFunction(CommandContext<S> p_319977_, SuggestionsBuilder p_319835_) {
      Stream<String> $$2 = GameTestRegistry.getAllTestFunctions().stream().map(TestFunction::testName);
      return SharedSuggestionProvider.suggest($$2, p_319835_);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
