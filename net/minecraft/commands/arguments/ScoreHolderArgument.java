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
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.ScoreHolder;

public class ScoreHolderArgument implements ArgumentType<ScoreHolderArgument.Result> {
   public static final SuggestionProvider<CommandSourceStack> SUGGEST_SCORE_HOLDERS = (commandContext, suggestionsBuilder) -> {
      StringReader stringReader = new StringReader(suggestionsBuilder.getInput());
      stringReader.setCursor(suggestionsBuilder.getStart());
      EntitySelectorParser entitySelectorParser = new EntitySelectorParser(
         stringReader, ((CommandSourceStack)commandContext.getSource()).permissions().hasPermission(Permissions.COMMANDS_ENTITY_SELECTORS)
      );

      try {
         entitySelectorParser.parse();
      } catch (CommandSyntaxException var5) {
      }

      return entitySelectorParser.fillSuggestions(
         suggestionsBuilder,
         suggestionsBuilderx -> SharedSuggestionProvider.suggest(((CommandSourceStack)commandContext.getSource()).getOnlinePlayerNames(), suggestionsBuilderx)
      );
   };
   private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "*", "@e");
   private static final SimpleCommandExceptionType ERROR_NO_RESULTS = new SimpleCommandExceptionType(Component.translatable("argument.scoreHolder.empty"));
   final boolean multiple;

   public ScoreHolderArgument(boolean multiple) {
      this.multiple = multiple;
   }

   public static ScoreHolder getName(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
      return getNames(context, name).iterator().next();
   }

   public static Collection<ScoreHolder> getNames(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
      return getNames(context, name, Collections::emptyList);
   }

   public static Collection<ScoreHolder> getNamesWithDefaultWildcard(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
      return getNames(context, name, ((CommandSourceStack)context.getSource()).getServer().getScoreboard()::getTrackedPlayers);
   }

   public static Collection<ScoreHolder> getNames(CommandContext<CommandSourceStack> context, String name, Supplier<Collection<ScoreHolder>> objectives) throws CommandSyntaxException {
      Collection<ScoreHolder> collection = ((ScoreHolderArgument.Result)context.getArgument(name, ScoreHolderArgument.Result.class))
         .getNames((CommandSourceStack)context.getSource(), objectives);
      if (collection.isEmpty()) {
         throw EntityArgument.NO_ENTITIES_FOUND.create();
      } else {
         return collection;
      }
   }

   public static ScoreHolderArgument scoreHolder() {
      return new ScoreHolderArgument(false);
   }

   public static ScoreHolderArgument scoreHolders() {
      return new ScoreHolderArgument(true);
   }

   public ScoreHolderArgument.Result parse(StringReader reader) throws CommandSyntaxException {
      return this.parse(reader, true);
   }

   public <S> ScoreHolderArgument.Result parse(StringReader reader, S suggestionProvider) throws CommandSyntaxException {
      return this.parse(reader, EntitySelectorParser.allowSelectors(suggestionProvider));
   }

   private ScoreHolderArgument.Result parse(StringReader reader, boolean allowSelectors) throws CommandSyntaxException {
      if (reader.canRead() && reader.peek() == '@') {
         EntitySelectorParser entitySelectorParser = new EntitySelectorParser(reader, allowSelectors);
         EntitySelector entitySelector = entitySelectorParser.parse();
         if (!this.multiple && entitySelector.getMaxResults() > 1) {
            throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.createWithContext(reader);
         } else {
            return new ScoreHolderArgument.SelectorResult(entitySelector);
         }
      } else {
         int i = reader.getCursor();

         while (reader.canRead() && reader.peek() != ' ') {
            reader.skip();
         }

         String string = reader.getString().substring(i, reader.getCursor());
         if (string.equals("*")) {
            return (commandSourceStack, supplier) -> {
               Collection<ScoreHolder> collection = supplier.get();
               if (collection.isEmpty()) {
                  throw ERROR_NO_RESULTS.create();
               } else {
                  return collection;
               }
            };
         } else {
            List<ScoreHolder> list = List.of(ScoreHolder.forNameOnly(string));
            if (string.startsWith("#")) {
               return (commandSourceStack, supplier) -> list;
            } else {
               try {
                  UUID uUID = UUID.fromString(string);
                  return (commandSourceStack, supplier) -> {
                     MinecraftServer minecraftServer = commandSourceStack.getServer();
                     ScoreHolder scoreHolder = null;
                     List<ScoreHolder> list2 = null;

                     for (ServerLevel serverLevel : minecraftServer.getAllLevels()) {
                        Entity entity = serverLevel.getEntity(uUID);
                        if (entity != null) {
                           if (scoreHolder == null) {
                              scoreHolder = entity;
                           } else {
                              if (list2 == null) {
                                 list2 = new ArrayList<>();
                                 list2.add(scoreHolder);
                              }

                              list2.add(entity);
                           }
                        }
                     }

                     if (list2 != null) {
                        return list2;
                     } else {
                        return scoreHolder != null ? List.of(scoreHolder) : list;
                     }
                  };
               } catch (IllegalArgumentException var7) {
                  return (commandSourceStack, supplier) -> {
                     MinecraftServer minecraftServer = commandSourceStack.getServer();
                     ServerPlayer serverPlayer = minecraftServer.getPlayerList().getPlayerByName(string);
                     return serverPlayer != null ? List.of(serverPlayer) : list;
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

      public void serializeToNetwork(ScoreHolderArgument.Info.Template template, FriendlyByteBuf friendlyByteBuf) {
         int i = 0;
         if (template.multiple) {
            i |= 1;
         }

         friendlyByteBuf.writeByte(i);
      }

      public ScoreHolderArgument.Info.Template deserializeFromNetwork(FriendlyByteBuf friendlyByteBuf) {
         byte b = friendlyByteBuf.readByte();
         boolean bl = (b & 1) != 0;
         return new ScoreHolderArgument.Info.Template(bl);
      }

      public void serializeToJson(ScoreHolderArgument.Info.Template template, JsonObject jsonObject) {
         jsonObject.addProperty("amount", template.multiple ? "multiple" : "single");
      }

      public ScoreHolderArgument.Info.Template unpack(ScoreHolderArgument scoreHolderArgument) {
         return new ScoreHolderArgument.Info.Template(scoreHolderArgument.multiple);
      }

      public final class Template implements ArgumentTypeInfo.Template<ScoreHolderArgument> {
         final boolean multiple;

         Template(final boolean multiple) {
            this.multiple = multiple;
         }

         public ScoreHolderArgument instantiate(CommandBuildContext commandBuildContext) {
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
      Collection<ScoreHolder> getNames(CommandSourceStack commandSourceStack, Supplier<Collection<ScoreHolder>> supplier) throws CommandSyntaxException;
   }

   public static class SelectorResult implements ScoreHolderArgument.Result {
      private final EntitySelector selector;

      public SelectorResult(EntitySelector selector) {
         this.selector = selector;
      }

      @Override
      public Collection<ScoreHolder> getNames(CommandSourceStack commandSourceStack, Supplier<Collection<ScoreHolder>> supplier) throws CommandSyntaxException {
         List<? extends Entity> list = this.selector.findEntities(commandSourceStack);
         if (list.isEmpty()) {
            throw EntityArgument.NO_ENTITIES_FOUND.create();
         } else {
            return List.copyOf(list);
         }
      }
   }
}
