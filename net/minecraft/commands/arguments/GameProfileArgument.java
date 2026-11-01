package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class GameProfileArgument implements ArgumentType<GameProfileArgument.Result> {
   private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "dd12be42-52a9-4a91-a8a1-11c01849e498", "@e");
   public static final SimpleCommandExceptionType ERROR_UNKNOWN_PLAYER = new SimpleCommandExceptionType(Component.translatable("argument.player.unknown"));

   public static Collection<GameProfile> getGameProfiles(CommandContext<CommandSourceStack> p_94591_, String p_94592_) throws CommandSyntaxException {
      return ((GameProfileArgument.Result)p_94591_.getArgument(p_94592_, GameProfileArgument.Result.class)).getNames((CommandSourceStack)p_94591_.getSource());
   }

   public static GameProfileArgument gameProfile() {
      return new GameProfileArgument();
   }

   public <S> GameProfileArgument.Result parse(StringReader p_353130_, S p_353124_) throws CommandSyntaxException {
      return parse(p_353130_, EntitySelectorParser.allowSelectors(p_353124_));
   }

   public GameProfileArgument.Result parse(StringReader p_94586_) throws CommandSyntaxException {
      return parse(p_94586_, true);
   }

   private static GameProfileArgument.Result parse(StringReader p_353133_, boolean p_353139_) throws CommandSyntaxException {
      if (p_353133_.canRead() && p_353133_.peek() == '@') {
         EntitySelectorParser $$2 = new EntitySelectorParser(p_353133_, p_353139_);
         EntitySelector $$3 = $$2.parse();
         if ($$3.includesEntities()) {
            throw EntityArgument.ERROR_ONLY_PLAYERS_ALLOWED.createWithContext(p_353133_);
         } else {
            return new GameProfileArgument.SelectorResult($$3);
         }
      } else {
         int $$4 = p_353133_.getCursor();

         while (p_353133_.canRead() && p_353133_.peek() != ' ') {
            p_353133_.skip();
         }

         String $$5 = p_353133_.getString().substring($$4, p_353133_.getCursor());
         return p_94595_ -> {
            Optional<GameProfile> $$2 = p_94595_.getServer().getProfileCache().get($$5);
            return Collections.singleton($$2.orElseThrow(ERROR_UNKNOWN_PLAYER::create));
         };
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_94598_, SuggestionsBuilder p_94599_) {
      if (p_94598_.getSource() instanceof SharedSuggestionProvider $$2) {
         StringReader $$3 = new StringReader(p_94599_.getInput());
         $$3.setCursor(p_94599_.getStart());
         EntitySelectorParser $$4 = new EntitySelectorParser($$3, EntitySelectorParser.allowSelectors($$2));

         try {
            $$4.parse();
         } catch (CommandSyntaxException var7) {
         }

         return $$4.fillSuggestions(p_94599_, p_353116_ -> SharedSuggestionProvider.suggest($$2.getOnlinePlayerNames(), p_353116_));
      } else {
         return Suggestions.empty();
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   @FunctionalInterface
   public interface Result {
      Collection<GameProfile> getNames(CommandSourceStack var1) throws CommandSyntaxException;
   }

   public static class SelectorResult implements GameProfileArgument.Result {
      private final EntitySelector selector;

      public SelectorResult(EntitySelector p_94605_) {
         this.selector = p_94605_;
      }

      @Override
      public Collection<GameProfile> getNames(CommandSourceStack p_94607_) throws CommandSyntaxException {
         List<ServerPlayer> $$1 = this.selector.findPlayers(p_94607_);
         if ($$1.isEmpty()) {
            throw EntityArgument.NO_PLAYERS_FOUND.create();
         } else {
            List<GameProfile> $$2 = Lists.newArrayList();

            for (ServerPlayer $$3 : $$1) {
               $$2.add($$3.getGameProfile());
            }

            return $$2;
         }
      }
   }
}
