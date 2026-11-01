package net.neoforged.neoforge.network.filters;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionProviderCheck;
import org.jspecify.annotations.Nullable;

class CommandTreeCleaner {
   private static final Predicate<SharedSuggestionProvider> RESTRICTED = o -> true;
   public static final ClientboundCommandsPacket.NodeBuilder<SharedSuggestionProvider> COMMAND_NODE_BUILDER = new ClientboundCommandsPacket.NodeBuilder<SharedSuggestionProvider>() {
      @Override
      public ArgumentBuilder<SharedSuggestionProvider, ?> createLiteral(String literal) {
         return LiteralArgumentBuilder.literal(literal);
      }

      @Override
      public ArgumentBuilder<SharedSuggestionProvider, ?> createArgument(String name, ArgumentType<?> type, @Nullable Identifier suggests) {
         RequiredArgumentBuilder<SharedSuggestionProvider, ?> builder = RequiredArgumentBuilder.argument(name, type);
         if (suggests != null) {
            builder.suggests(SuggestionProviders.getProvider(suggests));
         }

         return builder;
      }

      @Override
      public ArgumentBuilder<SharedSuggestionProvider, ?> configure(ArgumentBuilder<SharedSuggestionProvider, ?> builder, boolean executes, boolean restricted) {
         if (executes) {
            builder.executes(ctx -> 0);
         }

         if (restricted) {
            builder.requires(CommandTreeCleaner.RESTRICTED);
         }

         return builder;
      }
   };
   public static final ClientboundCommandsPacket.NodeInspector<SharedSuggestionProvider> COMMAND_NODE_INSPECTOR = new ClientboundCommandsPacket.NodeInspector<SharedSuggestionProvider>() {
      @Nullable
      @Override
      public Identifier suggestionId(ArgumentCommandNode<SharedSuggestionProvider, ?> node) {
         SuggestionProvider<SharedSuggestionProvider> suggestions = node.getCustomSuggestions();
         return suggestions != null ? SuggestionProviders.getName(suggestions) : null;
      }

      @Override
      public boolean isExecutable(CommandNode<SharedSuggestionProvider> node) {
         return node.getCommand() != null;
      }

      @Override
      public boolean isRestricted(CommandNode<SharedSuggestionProvider> node) {
         return node.getRequirement() instanceof PermissionProviderCheck(PermissionCheck var5);
      }
   };

   public static <S> RootCommandNode<S> cleanArgumentTypes(RootCommandNode<S> root, Predicate<ArgumentType<?>> argumentTypeFilter) {
      Predicate<CommandNode<?>> nodeFilter = node -> !(node instanceof ArgumentCommandNode) || argumentTypeFilter.test(((ArgumentCommandNode)node).getType());
      return (RootCommandNode<S>)processCommandNode(root, nodeFilter, new HashMap<>());
   }

   private static <S> CommandNode<S> processCommandNode(CommandNode<S> node, Predicate<CommandNode<?>> nodeFilter, Map<CommandNode<S>, CommandNode<S>> newNodes) {
      CommandNode<S> existingNode = newNodes.get(node);
      if (existingNode == null) {
         CommandNode<S> newNode = cloneNode(node, nodeFilter, newNodes);
         newNodes.put(node, newNode);
         node.getChildren()
            .stream()
            .filter(nodeFilter)
            .map(child -> processCommandNode((CommandNode<S>)child, nodeFilter, newNodes))
            .forEach(newNode::addChild);
         return newNode;
      } else {
         return existingNode;
      }
   }

   private static <S> CommandNode<S> cloneNode(CommandNode<S> node, Predicate<CommandNode<?>> nodeFilter, Map<CommandNode<S>, CommandNode<S>> newNodes) {
      if (node instanceof RootCommandNode) {
         return new RootCommandNode();
      } else {
         ArgumentBuilder<S, ?> builder = node.createBuilder();
         if (node.getRedirect() != null) {
            if (nodeFilter.test(node.getRedirect())) {
               builder.forward(processCommandNode(node.getRedirect(), nodeFilter, newNodes), node.getRedirectModifier(), node.isFork());
            } else {
               builder.redirect(null);
            }
         }

         return builder.build();
      }
   }
}
