package net.neoforged.neoforge.event;

import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jspecify.annotations.Nullable;

public class CommandEvent extends Event implements ICancellableEvent {
   private ParseResults<CommandSourceStack> parse;
   @Nullable
   private Throwable exception;

   public CommandEvent(ParseResults<CommandSourceStack> parse) {
      this.parse = parse;
   }

   public ParseResults<CommandSourceStack> getParseResults() {
      return this.parse;
   }

   public void setParseResults(ParseResults<CommandSourceStack> parse) {
      this.parse = parse;
   }

   @Nullable
   public Throwable getException() {
      return this.exception;
   }

   public void setException(@Nullable Throwable exception) {
      this.exception = exception;
   }
}
