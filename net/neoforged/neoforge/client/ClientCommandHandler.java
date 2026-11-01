package net.neoforged.neoforge.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.command.CommandHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;

public class ClientCommandHandler {
   private static final Identifier ASK_SERVER_ID = Identifier.withDefaultNamespace("ask_server");
   private static final Logger LOGGER = LogManager.getLogger();
   private static CommandDispatcher<CommandSourceStack> commands = null;

   public static void init() {
      NeoForge.EVENT_BUS.addListener(ClientCommandHandler::handleClientPlayerLogin);
   }

   private static void handleClientPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
      ClientPacketListener connection = event.getPlayer().connection;
      connection.commands = mergeServerCommands(new CommandDispatcher(), CommandBuildContext.simple(connection.registryAccess(), connection.enabledFeatures()));
   }

   @Internal
   public static CommandDispatcher<ClientSuggestionProvider> mergeServerCommands(
      CommandDispatcher<ClientSuggestionProvider> serverCommands, CommandBuildContext buildContext
   ) {
      CommandDispatcher<CommandSourceStack> commandsTemp = new CommandDispatcher();
      NeoForge.EVENT_BUS.post(new RegisterClientCommandsEvent(commandsTemp, buildContext));
      commands = new CommandDispatcher();
      copy(commandsTemp.getRoot(), commands.getRoot());
      RootCommandNode<ClientSuggestionProvider> serverCommandsRoot = serverCommands.getRoot();
      CommandDispatcher<ClientSuggestionProvider> newServerCommands = new CommandDispatcher();
      copy(serverCommandsRoot, newServerCommands.getRoot());
      CommandHelper.mergeCommandNode(commands.getRoot(), newServerCommands.getRoot(), new IdentityHashMap(), getSource(), context -> 0, suggestions -> {
         Identifier id = SuggestionProviders.getName(suggestions);
         SuggestionProvider<ClientSuggestionProvider> suggestionProvider;
         if (id.equals(ASK_SERVER_ID)) {
            suggestionProvider = (context, builder) -> {
               ClientCommandSourceStack source = getSource();
               StringReader reader = new StringReader(context.getInput());
               if (reader.canRead() && reader.peek() == '/') {
                  reader.skip();
               }

               ParseResults<CommandSourceStack> parse = commands.parse(reader, source);
               return commands.getCompletionSuggestions(parse);
            };
         } else {
            suggestionProvider = SuggestionProviders.getProvider(id);
         }

         return suggestionProvider;
      });
      return newServerCommands;
   }

   public static CommandDispatcher<CommandSourceStack> getDispatcher() {
      return commands;
   }

   public static ClientCommandSourceStack getSource() {
      LocalPlayer player = Minecraft.getInstance().player;
      CommandSource commandSource = new CommandSource() {
         @Override
         public boolean acceptsSuccess() {
            return true;
         }

         @Override
         public boolean acceptsFailure() {
            return true;
         }

         @Override
         public boolean shouldInformAdmins() {
            return true;
         }

         @Override
         public void sendSystemMessage(Component message) {
            Minecraft.getInstance().gui.getChat().addMessage(message);
         }
      };
      return new ClientCommandSourceStack(
         commandSource, player.position(), player.getRotationVector(), player.permissions(), player.getName().getString(), player.getDisplayName(), player
      );
   }

   private static <S> void copy(CommandNode<S> sourceNode, CommandNode<S> resultNode) {
      Map<CommandNode<S>, CommandNode<S>> newNodes = new IdentityHashMap<>();
      newNodes.put(sourceNode, resultNode);

      for (CommandNode<S> child : sourceNode.getChildren()) {
         CommandNode<S> copy = newNodes.computeIfAbsent(child, innerChild -> {
            ArgumentBuilder<S, ?> builder = innerChild.createBuilder();
            CommandNode<S> innerCopy = builder.build();
            copy((CommandNode<S>)innerChild, innerCopy);
            return innerCopy;
         });
         resultNode.addChild(copy);
      }
   }

   public static boolean runCommand(String command) {
      StringReader reader = new StringReader(command);
      ClientCommandSourceStack source = getSource();

      try {
         commands.execute(reader, source);
      } catch (CommandSyntaxException var6) {
         if (var6.getType() == CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand()
            || var6.getType() == CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument()) {
            return false;
         }

         Minecraft.getInstance()
            .gui
            .getChat()
            .addMessage(Component.literal("").append(ComponentUtils.fromMessage(var6.getRawMessage())).withStyle(ChatFormatting.RED));
         if (var6.getInput() != null && var6.getCursor() >= 0) {
            int position = Math.min(var6.getInput().length(), var6.getCursor());
            MutableComponent details = Component.literal("")
               .withStyle(ChatFormatting.GRAY)
               .withStyle(style -> style.withClickEvent(new ClickEvent.SuggestCommand(reader.getString())));
            if (position > 10) {
               details.append("...");
            }

            details.append(var6.getInput().substring(Math.max(0, position - 10), position));
            if (position < var6.getInput().length()) {
               details.append(Component.literal(var6.getInput().substring(position)).withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE));
            }

            details.append(Component.translatable("command.context.here").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("").append(details).withStyle(ChatFormatting.RED));
         }
      } catch (Exception var7) {
         MutableComponent message = Component.literal(var7.getMessage() == null ? var7.getClass().getName() : var7.getMessage());
         Minecraft.getInstance()
            .gui
            .getChat()
            .addMessage(
               Component.translatable("command.failed")
                  .withStyle(ChatFormatting.RED)
                  .withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(message)))
            );
         LOGGER.error("Error executing client command \"{}\"", command, var7);
      }

      return true;
   }
}
