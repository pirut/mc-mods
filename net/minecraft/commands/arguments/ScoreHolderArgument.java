package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.ScoreHolder;

public class ScoreHolderArgument implements ArgumentType<ScoreHolderArgument.Result> {
   public static final SuggestionProvider<CommandSourceStack> SUGGEST_SCORE_HOLDERS = (p_353117_, p_353118_) -> {
      StringReader $$2 = new StringReader(p_353118_.getInput());
      $$2.setCursor(p_353118_.getStart());
      EntitySelectorParser $$3 = new EntitySelectorParser($$2, EntitySelectorParser.allowSelectors((CommandSourceStack)p_353117_.getSource()));

      try {
         $$3.parse();
      } catch (CommandSyntaxException var5) {
      }

      return $$3.fillSuggestions(
         p_353118_, p_171606_ -> SharedSuggestionProvider.suggest(((CommandSourceStack)p_353117_.getSource()).getOnlinePlayerNames(), p_171606_)
      );
   };
   private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "*", "@e");
   private static final SimpleCommandExceptionType ERROR_NO_RESULTS = new SimpleCommandExceptionType(Component.translatable("argument.scoreHolder.empty"));
   final boolean multiple;

   public ScoreHolderArgument(boolean p_108216_) {
      this.multiple = p_108216_;
   }

   public static ScoreHolder getName(CommandContext<CommandSourceStack> p_108224_, String p_108225_) throws CommandSyntaxException {
      return getNames(p_108224_, p_108225_).iterator().next();
   }

   public static Collection<ScoreHolder> getNames(CommandContext<CommandSourceStack> p_108244_, String p_108245_) throws CommandSyntaxException {
      return getNames(p_108244_, p_108245_, Collections::emptyList);
   }

   public static Collection<ScoreHolder> getNamesWithDefaultWildcard(CommandContext<CommandSourceStack> p_108247_, String p_108248_) throws CommandSyntaxException {
      return getNames(p_108247_, p_108248_, ((CommandSourceStack)p_108247_.getSource()).getServer().getScoreboard()::getTrackedPlayers);
   }

   public static Collection<ScoreHolder> getNames(CommandContext<CommandSourceStack> p_108227_, String p_108228_, Supplier<Collection<ScoreHolder>> p_108229_) throws CommandSyntaxException {
      Collection<ScoreHolder> $$3 = ((ScoreHolderArgument.Result)p_108227_.getArgument(p_108228_, ScoreHolderArgument.Result.class))
         .getNames((CommandSourceStack)p_108227_.getSource(), p_108229_);
      if ($$3.isEmpty()) {
         throw EntityArgument.NO_ENTITIES_FOUND.create();
      } else {
         return $$3;
      }
   }

   public static ScoreHolderArgument scoreHolder() {
      return new ScoreHolderArgument(false);
   }

   public static ScoreHolderArgument scoreHolders() {
      return new ScoreHolderArgument(true);
   }

   public ScoreHolderArgument.Result parse(StringReader p_108219_) throws CommandSyntaxException {
      return this.parse(p_108219_, true);
   }

   public <S> ScoreHolderArgument.Result parse(StringReader p_353125_, S p_353126_) throws CommandSyntaxException {
      return this.parse(p_353125_, EntitySelectorParser.allowSelectors(p_353126_));
   }

   private ScoreHolderArgument.Result parse(StringReader p_353140_, boolean p_353127_) throws CommandSyntaxException {
      if (p_353140_.canRead() && p_353140_.peek() == '@') {
         EntitySelectorParser $$2 = new EntitySelectorParser(p_353140_, p_353127_);
         EntitySelector $$3 = $$2.parse();
         if (!this.multiple && $$3.getMaxResults() > 1) {
            throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.createWithContext(p_353140_);
         } else {
            return new ScoreHolderArgument.SelectorResult($$3);
         }
      } else {
         int $$4 = p_353140_.getCursor();

         while (p_353140_.canRead() && p_353140_.peek() != ' ') {
            p_353140_.skip();
         }

         String $$5 = p_353140_.getString().substring($$4, p_353140_.getCursor());
         if ($$5.equals("*")) {
            return (p_108231_, p_108232_) -> {
               Collection<ScoreHolder> $$2 = p_108232_.get();
               if ($$2.isEmpty()) {
                  throw ERROR_NO_RESULTS.create();
               } else {
                  return $$2;
               }
            };
         } else {
            List<ScoreHolder> $$6 = List.of(ScoreHolder.forNameOnly($$5));
            if ($$5.startsWith("#")) {
               return (p_108237_, p_108238_) -> $$6;
            } else {
               try {
                  UUID $$7 = UUID.fromString($$5);
                  return (p_314703_, p_314704_) -> {
                     MinecraftServer $$4x = p_314703_.getServer();
                     ScoreHolder $$5x = null;
                     List<ScoreHolder> $$6x = null;

                     for (ServerLevel $$7x : $$4x.getAllLevels()) {
                        Entity $$8 = $$7x.getEntity($$7);
                        if ($$8 != null) {
                           if ($$5x == null) {
                              $$5x = $$8;
                           } else {
                              if ($$6x == null) {
                                 $$6x = new ArrayList<>();
                                 $$6x.add($$5x);
                              }

                              $$6x.add($$8);
                           }
                        }
                     }

                     if ($$6x != null) {
                        return $$6x;
                     } else {
                        return $$5x != null ? List.of($$5x) : $$6;
                     }
                  };
               } catch (IllegalArgumentException var7) {
                  return (p_314699_, p_314700_) -> {
                     MinecraftServer $$4x = p_314699_.getServer();
                     ServerPlayer $$5x = $$4x.getPlayerList().getPlayerByName($$5);
                     return $$5x != null ? List.of($$5x) : $$6;
                  };
               }
            }
         }
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static class Info implements ArgumentTypeInfo<ScoreHolderArgument, ScoreHolderArgument.Info.Template> {
      private static final byte FLAG_MULTIPLE = 1;

      public void serializeToNetwork(ScoreHolderArgument.Info.Template p_233469_, FriendlyByteBuf p_233470_) {
         int $$2 = 0;
         if (p_233469_.multiple) {
            $$2 |= 1;
         }

         p_233470_.writeByte($$2);
      }

      public ScoreHolderArgument.Info.Template deserializeFromNetwork(FriendlyByteBuf p_233480_) {
         byte $$1 = p_233480_.readByte();
         boolean $$2 = ($$1 & 1) != 0;
         return new ScoreHolderArgument.Info.Template($$2);
      }

      public void serializeToJson(ScoreHolderArgument.Info.Template p_233466_, JsonObject p_233467_) {
         p_233467_.addProperty("amount", p_233466_.multiple ? "multiple" : "single");
      }

      public ScoreHolderArgument.Info.Template unpack(ScoreHolderArgument p_233472_) {
         return new ScoreHolderArgument.Info.Template(p_233472_.multiple);
      }

      public final class Template implements ArgumentTypeInfo.Template<ScoreHolderArgument> {
         final boolean multiple;

         Template(boolean p_233487_) {
            this.multiple = p_233487_;
         }

         public ScoreHolderArgument instantiate(CommandBuildContext p_233490_) {
            return new ScoreHolderArgument(this.multiple);
         }

         @Override
         public ArgumentTypeInfo<ScoreHolderArgument, ?> type() {
            return Info.this;
         }
      }
   }

   @FunctionalInterface
   public interface Result {
      Collection<ScoreHolder> getNames(CommandSourceStack var1, Supplier<Collection<ScoreHolder>> var2) throws CommandSyntaxException;
   }

   public static class SelectorResult implements ScoreHolderArgument.Result {
      private final EntitySelector selector;

      public SelectorResult(EntitySelector p_108256_) {
         this.selector = p_108256_;
      }

      @Override
      public Collection<ScoreHolder> getNames(CommandSourceStack p_108258_, Supplier<Collection<ScoreHolder>> p_108259_) throws CommandSyntaxException {
         List<? extends Entity> $$2 = this.selector.findEntities(p_108258_);
         if ($$2.isEmpty()) {
            throw EntityArgument.NO_ENTITIES_FOUND.create();
         } else {
            return List.copyOf($$2);
         }
      }
   }
}
