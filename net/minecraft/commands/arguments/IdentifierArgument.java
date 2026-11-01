package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;

public class IdentifierArgument implements ArgumentType<Identifier> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");

   public static IdentifierArgument id() {
      return new IdentifierArgument();
   }

   public static Identifier getId(CommandContext<CommandSourceStack> p_467800_, String p_469691_) {
      return (Identifier)p_467800_.getArgument(p_469691_, Identifier.class);
   }

   public Identifier parse(StringReader p_468633_) throws CommandSyntaxException {
      return Identifier.read(p_468633_);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
