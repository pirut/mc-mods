package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class Vec2Argument implements ArgumentType<Coordinates> {
   private static final Collection<String> EXAMPLES = Arrays.asList("0 0", "~ ~", "0.1 -0.5", "~1 ~-2");
   public static final SimpleCommandExceptionType ERROR_NOT_COMPLETE = new SimpleCommandExceptionType(Component.translatable("argument.pos2d.incomplete"));
   private final boolean centerCorrect;

   public Vec2Argument(boolean p_120821_) {
      this.centerCorrect = p_120821_;
   }

   public static Vec2Argument vec2() {
      return new Vec2Argument(true);
   }

   public static Vec2Argument vec2(boolean p_174955_) {
      return new Vec2Argument(p_174955_);
   }

   public static Vec2 getVec2(CommandContext<CommandSourceStack> p_120826_, String p_120827_) {
      Vec3 $$2 = ((Coordinates)p_120826_.getArgument(p_120827_, Coordinates.class)).getPosition((CommandSourceStack)p_120826_.getSource());
      return new Vec2((float)$$2.x, (float)$$2.z);
   }

   public Coordinates parse(StringReader p_120824_) throws CommandSyntaxException {
      int $$1 = p_120824_.getCursor();
      if (!p_120824_.canRead()) {
         throw ERROR_NOT_COMPLETE.createWithContext(p_120824_);
      } else {
         WorldCoordinate $$2 = WorldCoordinate.parseDouble(p_120824_, this.centerCorrect);
         if (p_120824_.canRead() && p_120824_.peek() == ' ') {
            p_120824_.skip();
            WorldCoordinate $$3 = WorldCoordinate.parseDouble(p_120824_, this.centerCorrect);
            return new WorldCoordinates($$2, new WorldCoordinate(true, 0.0), $$3);
         } else {
            p_120824_.setCursor($$1);
            throw ERROR_NOT_COMPLETE.createWithContext(p_120824_);
         }
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_120830_, SuggestionsBuilder p_120831_) {
      if (!(p_120830_.getSource() instanceof SharedSuggestionProvider)) {
         return Suggestions.empty();
      } else {
         String $$2 = p_120831_.getRemaining();
         Collection<SharedSuggestionProvider.TextCoordinates> $$3;
         if (!$$2.isEmpty() && $$2.charAt(0) == '^') {
            $$3 = Collections.singleton(SharedSuggestionProvider.TextCoordinates.DEFAULT_LOCAL);
         } else {
            $$3 = ((SharedSuggestionProvider)p_120830_.getSource()).getAbsoluteCoordinates();
         }

         return SharedSuggestionProvider.suggest2DCoordinates($$2, $$3, p_120831_, Commands.createValidator(this::parse));
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
