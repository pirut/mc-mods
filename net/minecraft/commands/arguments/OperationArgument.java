package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.scores.ScoreAccess;

public class OperationArgument implements ArgumentType<OperationArgument.Operation> {
   private static final Collection<String> EXAMPLES = Arrays.asList("=", ">", "<");
   private static final SimpleCommandExceptionType ERROR_INVALID_OPERATION = new SimpleCommandExceptionType(
      Component.translatable("arguments.operation.invalid")
   );
   private static final SimpleCommandExceptionType ERROR_DIVIDE_BY_ZERO = new SimpleCommandExceptionType(Component.translatable("arguments.operation.div0"));

   public static OperationArgument operation() {
      return new OperationArgument();
   }

   public static OperationArgument.Operation getOperation(CommandContext<CommandSourceStack> context, String name) {
      return (OperationArgument.Operation)context.getArgument(name, OperationArgument.Operation.class);
   }

   public OperationArgument.Operation parse(StringReader reader) throws CommandSyntaxException {
      if (!reader.canRead()) {
         throw ERROR_INVALID_OPERATION.createWithContext(reader);
      } else {
         int i = reader.getCursor();

         while (reader.canRead() && reader.peek() != ' ') {
            reader.skip();
         }

         return getOperation(reader.getString().substring(i, reader.getCursor()));
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
      return SharedSuggestionProvider.suggest(new String[]{"=", "+=", "-=", "*=", "/=", "%=", "<", ">", "><"}, suggestionsBuilder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   private static OperationArgument.Operation getOperation(String name) throws CommandSyntaxException {
      return (OperationArgument.Operation)(name.equals("><") ? (scoreAccess, scoreAccess2) -> {
         int i = scoreAccess.get();
         scoreAccess.set(scoreAccess2.get());
         scoreAccess2.set(i);
      } : getSimpleOperation(name));
   }

   private static OperationArgument.SimpleOperation getSimpleOperation(String name) throws CommandSyntaxException {
      return switch (name) {
         case "=" -> (i, j) -> j;
         case "+=" -> Integer::sum;
         case "-=" -> (i, j) -> i - j;
         case "*=" -> (i, j) -> i * j;
         case "/=" -> (i, j) -> {
            if (j == 0) {
               throw ERROR_DIVIDE_BY_ZERO.create();
            } else {
               return Mth.floorDiv(i, j);
            }
         };
         case "%=" -> (i, j) -> {
            if (j == 0) {
               throw ERROR_DIVIDE_BY_ZERO.create();
            } else {
               return Mth.positiveModulo(i, j);
            }
         };
         case "<" -> Math::min;
         case ">" -> Math::max;
         default -> throw ERROR_INVALID_OPERATION.create();
      };
   }

   @FunctionalInterface
   public interface Operation {
      void apply(ScoreAccess scoreAccess, ScoreAccess scoreAccess2) throws CommandSyntaxException;
   }

   @FunctionalInterface
   interface SimpleOperation extends OperationArgument.Operation {
      int apply(int i, int j) throws CommandSyntaxException;

      @Override
      default void apply(ScoreAccess scoreAccess, ScoreAccess scoreAccess2) throws CommandSyntaxException {
         scoreAccess.set(this.apply(scoreAccess.get(), scoreAccess2.get()));
      }
   }
}
