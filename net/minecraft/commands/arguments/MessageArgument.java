package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSigningContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.permissions.Permissions;
import org.jspecify.annotations.Nullable;

public class MessageArgument implements SignedArgument<MessageArgument.Message> {
   private static final Collection<String> EXAMPLES = Arrays.asList("Hello world!", "foo", "@e", "Hello @p :)");
   static final Dynamic2CommandExceptionType TOO_LONG = new Dynamic2CommandExceptionType(
      (object, object2) -> Component.translatableEscape("argument.message.too_long", object, object2)
   );

   public static MessageArgument message() {
      return new MessageArgument();
   }

   public static Component getMessage(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
      MessageArgument.Message message = (MessageArgument.Message)context.getArgument(name, MessageArgument.Message.class);
      return message.resolveComponent((CommandSourceStack)context.getSource());
   }

   public static void resolveChatMessage(CommandContext<CommandSourceStack> context, String key, Consumer<PlayerChatMessage> callback) throws CommandSyntaxException {
      MessageArgument.Message message = (MessageArgument.Message)context.getArgument(key, MessageArgument.Message.class);
      CommandSourceStack commandSourceStack = (CommandSourceStack)context.getSource();
      Component component = message.resolveComponent(commandSourceStack);
      CommandSigningContext commandSigningContext = commandSourceStack.getSigningContext();
      PlayerChatMessage playerChatMessage = commandSigningContext.getArgument(key);
      if (playerChatMessage != null) {
         resolveSignedMessage(callback, commandSourceStack, playerChatMessage.withUnsignedContent(component));
      } else {
         resolveDisguisedMessage(callback, commandSourceStack, PlayerChatMessage.system(message.text).withUnsignedContent(component));
      }
   }

   private static void resolveSignedMessage(Consumer<PlayerChatMessage> callback, CommandSourceStack source, PlayerChatMessage message) {
      MinecraftServer minecraftServer = source.getServer();
      CompletableFuture<FilteredText> completableFuture = filterPlainText(source, message);
      Component component = minecraftServer.getChatDecorator().decorate(source.getPlayer(), message.decoratedContent());
      source.getChatMessageChainer().append(completableFuture, filteredText -> {
         PlayerChatMessage playerChatMessage2 = message.withUnsignedContent(component).filter(filteredText.mask());
         callback.accept(playerChatMessage2);
      });
   }

   private static void resolveDisguisedMessage(Consumer<PlayerChatMessage> callback, CommandSourceStack source, PlayerChatMessage message) {
      ChatDecorator chatDecorator = source.getServer().getChatDecorator();
      Component component = chatDecorator.decorate(source.getPlayer(), message.decoratedContent());
      callback.accept(message.withUnsignedContent(component));
   }

   private static CompletableFuture<FilteredText> filterPlainText(CommandSourceStack source, PlayerChatMessage message) {
      ServerPlayer serverPlayer = source.getPlayer();
      return serverPlayer != null && message.hasSignatureFrom(serverPlayer.getUUID())
         ? serverPlayer.getTextFilter().processStreamMessage(message.signedContent())
         : CompletableFuture.completedFuture(FilteredText.passThrough(message.signedContent()));
   }

   public MessageArgument.Message parse(StringReader reader) throws CommandSyntaxException {
      return MessageArgument.Message.parseText(reader, true);
   }

   public <S> MessageArgument.Message parse(StringReader stringReader, @Nullable S object) throws CommandSyntaxException {
      return MessageArgument.Message.parseText(stringReader, EntitySelectorParser.allowSelectors(object));
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public record Message(String text, MessageArgument.Part[] parts) {

      Component resolveComponent(CommandSourceStack source) throws CommandSyntaxException {
         return this.toComponent(source, source.permissions().hasPermission(Permissions.COMMANDS_ENTITY_SELECTORS));
      }

      public Component toComponent(CommandSourceStack source, boolean allowSelectors) throws CommandSyntaxException {
         if (this.parts.length != 0 && allowSelectors) {
            MutableComponent mutableComponent = Component.literal(this.text.substring(0, this.parts[0].start()));
            int i = this.parts[0].start();

            for (MessageArgument.Part part : this.parts) {
               Component component = part.toComponent(source);
               if (i < part.start()) {
                  mutableComponent.append(this.text.substring(i, part.start()));
               }

               mutableComponent.append(component);
               i = part.end();
            }

            if (i < this.text.length()) {
               mutableComponent.append(this.text.substring(i));
            }

            return mutableComponent;
         } else {
            return Component.literal(this.text);
         }
      }

      public static MessageArgument.Message parseText(StringReader reader, boolean allowSelectors) throws CommandSyntaxException {
         if (reader.getRemainingLength() > 256) {
            throw MessageArgument.TOO_LONG.create(reader.getRemainingLength(), 256);
         } else {
            String string = reader.getRemaining();
            if (!allowSelectors) {
               reader.setCursor(reader.getTotalLength());
               return new MessageArgument.Message(string, new MessageArgument.Part[0]);
            } else {
               List<MessageArgument.Part> list = Lists.newArrayList();
               int i = reader.getCursor();

               while (true) {
                  int j;
                  EntitySelector entitySelector;
                  while (true) {
                     if (!reader.canRead()) {
                        return new MessageArgument.Message(string, list.toArray(new MessageArgument.Part[0]));
                     }

                     if (reader.peek() == '@') {
                        j = reader.getCursor();

                        try {
                           EntitySelectorParser entitySelectorParser = new EntitySelectorParser(reader, true);
                           entitySelector = entitySelectorParser.parse();
                           break;
                        } catch (CommandSyntaxException var8) {
                           if (var8.getType() != EntitySelectorParser.ERROR_MISSING_SELECTOR_TYPE
                              && var8.getType() != EntitySelectorParser.ERROR_UNKNOWN_SELECTOR_TYPE) {
                              throw var8;
                           }

                           reader.setCursor(j + 1);
                        }
                     } else {
                        reader.skip();
                     }
                  }

                  list.add(new MessageArgument.Part(j - i, reader.getCursor() - i, entitySelector));
               }
            }
         }
      }
   }

   public record Part(int start, int end, EntitySelector selector) {
      public Component toComponent(CommandSourceStack source) throws CommandSyntaxException {
         return EntitySelector.joinNames(this.selector.findEntities(source));
      }
   }
}
