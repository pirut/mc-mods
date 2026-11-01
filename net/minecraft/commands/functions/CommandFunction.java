package net.minecraft.commands.functions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.commands.Commands;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.commands.execution.tasks.BuildContexts;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public interface CommandFunction<T> {
   ResourceLocation id();

   InstantiatedFunction<T> instantiate(@Nullable CompoundTag var1, CommandDispatcher<T> var2) throws FunctionInstantiationException;

   private static boolean shouldConcatenateNextLine(CharSequence p_306338_) {
      int $$1 = p_306338_.length();
      return $$1 > 0 && p_306338_.charAt($$1 - 1) == '\\';
   }

   static <T extends ExecutionCommandSource<T>> CommandFunction<T> fromLines(
      ResourceLocation p_306082_, CommandDispatcher<T> p_306046_, T p_305973_, List<String> p_306307_
   ) {
      FunctionBuilder<T> $$4 = new FunctionBuilder<>();

      for (int $$5 = 0; $$5 < p_306307_.size(); $$5++) {
         int $$6 = $$5 + 1;
         String $$7 = p_306307_.get($$5).trim();
         String $$10;
         if (shouldConcatenateNextLine($$7)) {
            StringBuilder $$8 = new StringBuilder($$7);

            do {
               if (++$$5 == p_306307_.size()) {
                  throw new IllegalArgumentException("Line continuation at end of file");
               }

               $$8.deleteCharAt($$8.length() - 1);
               String $$9 = p_306307_.get($$5).trim();
               $$8.append($$9);
               checkCommandLineLength($$8);
            } while (shouldConcatenateNextLine($$8));

            $$10 = $$8.toString();
         } else {
            $$10 = $$7;
         }

         checkCommandLineLength($$10);
         StringReader $$12 = new StringReader($$10);
         if ($$12.canRead() && $$12.peek() != '#') {
            if ($$12.peek() == '/') {
               $$12.skip();
               if ($$12.peek() == '/') {
                  throw new IllegalArgumentException(
                     "Unknown or invalid command '" + $$10 + "' on line " + $$6 + " (if you intended to make a comment, use '#' not '//')"
                  );
               }

               String $$13 = $$12.readUnquotedString();
               throw new IllegalArgumentException(
                  "Unknown or invalid command '" + $$10 + "' on line " + $$6 + " (did you mean '" + $$13 + "'? Do not use a preceding forwards slash.)"
               );
            }

            if ($$12.peek() == '$') {
               $$4.addMacro($$10.substring(1), $$6, p_305973_);
            } else {
               try {
                  $$4.addCommand(parseCommand(p_306046_, p_305973_, $$12));
               } catch (CommandSyntaxException var11) {
                  throw new IllegalArgumentException("Whilst parsing command on line " + $$6 + ": " + var11.getMessage());
               }
            }
         }
      }

      return $$4.build(p_306082_);
   }

   static void checkCommandLineLength(CharSequence p_326091_) {
      if (p_326091_.length() > 2000000) {
         CharSequence $$1 = p_326091_.subSequence(0, Math.min(512, 2000000));
         throw new IllegalStateException("Command too long: " + p_326091_.length() + " characters, contents: " + $$1 + "...");
      }
   }

   static <T extends ExecutionCommandSource<T>> UnboundEntryAction<T> parseCommand(CommandDispatcher<T> p_306213_, T p_305785_, StringReader p_306140_) throws CommandSyntaxException {
      ParseResults<T> $$3 = p_306213_.parse(p_306140_, p_305785_);
      Commands.validateParseResults($$3);
      Optional<ContextChain<T>> $$4 = ContextChain.tryFlatten($$3.getContext().build(p_306140_.getString()));
      if ($$4.isEmpty()) {
         throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext($$3.getReader());
      } else {
         return new BuildContexts.Unbound<>(p_306140_.getString(), $$4.get());
      }
   }
}
