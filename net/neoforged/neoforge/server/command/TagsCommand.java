package net.neoforged.neoforge.server.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;

class TagsCommand {
   private static final long CLIPBOARD_TEXT_LIMIT = 32600L;
   private static final long PAGE_SIZE = 8L;
   private static final ResourceKey<Registry<Registry<?>>> ROOT_REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("root"));
   private static final DynamicCommandExceptionType UNKNOWN_REGISTRY = new DynamicCommandExceptionType(
      key -> CommandUtils.makeTranslatableWithFallback("commands.neoforge.tags.error.unknown_registry", key.toString())
   );
   private static final Dynamic2CommandExceptionType UNKNOWN_TAG = new Dynamic2CommandExceptionType(
      (tag, registry) -> CommandUtils.makeTranslatableWithFallback("commands.neoforge.tags.error.unknown_tag", tag.toString(), registry.toString())
   );
   private static final Dynamic2CommandExceptionType UNKNOWN_ELEMENT = new Dynamic2CommandExceptionType(
      (tag, registry) -> CommandUtils.makeTranslatableWithFallback("commands.neoforge.tags.error.unknown_element", tag.toString(), registry.toString())
   );

   public static ArgumentBuilder<CommandSourceStack, ?> register() {
      return ((LiteralArgumentBuilder)Commands.literal("tags").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
         .then(
            ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("registry", ResourceKeyArgument.key(ROOT_REGISTRY_KEY))
                     .suggests(CommandUtils::suggestRegistries)
                     .then(
                        ((LiteralArgumentBuilder)Commands.literal("list").executes(ctx -> listTags(ctx, 1)))
                           .then(
                              Commands.argument("page", IntegerArgumentType.integer(1))
                                 .executes(ctx -> listTags(ctx, IntegerArgumentType.getInteger(ctx, "page")))
                           )
                     ))
                  .then(
                     Commands.literal("get")
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("tag", IdentifierArgument.id())
                                 .suggests(
                                    CommandUtils.suggestFromRegistry(
                                       r -> r.getTags().map(HolderSet.Named::key).map(TagKey::location)::iterator, "registry", ROOT_REGISTRY_KEY
                                    )
                                 )
                                 .executes(ctx -> listTagElements(ctx, 1)))
                              .then(
                                 Commands.argument("page", IntegerArgumentType.integer(1))
                                    .executes(ctx -> listTagElements(ctx, IntegerArgumentType.getInteger(ctx, "page")))
                              )
                        )
                  ))
               .then(
                  Commands.literal("query")
                     .then(
                        ((RequiredArgumentBuilder)Commands.argument("element", IdentifierArgument.id())
                              .suggests(CommandUtils.suggestFromRegistry(Registry::keySet, "registry", ROOT_REGISTRY_KEY))
                              .executes(ctx -> queryElementTags(ctx, 1)))
                           .then(
                              Commands.argument("page", IntegerArgumentType.integer(1))
                                 .executes(ctx -> queryElementTags(ctx, IntegerArgumentType.getInteger(ctx, "page")))
                           )
                     )
               )
         );
   }

   private static int listTags(CommandContext<CommandSourceStack> ctx, int page) throws CommandSyntaxException {
      ResourceKey<? extends Registry<?>> registryKey = CommandUtils.getResourceKey(ctx, "registry", ROOT_REGISTRY_KEY).orElseThrow();
      Registry<?> registry = ((CommandSourceStack)ctx.getSource())
         .getServer()
         .registryAccess()
         .lookup(registryKey)
         .orElseThrow(() -> UNKNOWN_REGISTRY.create(registryKey.identifier()));
      long tagCount = registry.getTags().count();
      ((CommandSourceStack)ctx.getSource())
         .sendSuccess(
            () -> createMessage(
               CommandUtils.makeTranslatableWithFallback(
                  "commands.neoforge.tags.registry_key", Component.literal(registryKey.identifier().toString()).withStyle(ChatFormatting.GOLD)
               ),
               "commands.neoforge.tags.tag_count",
               "commands.neoforge.tags.copy_tag_names",
               tagCount,
               page,
               ChatFormatting.DARK_GREEN,
               () -> registry.getTags().map(s -> (String)s.unwrap().map(k -> k.location().toString(), Object::toString))
            ),
            false
         );
      return (int)tagCount;
   }

   private static int listTagElements(CommandContext<CommandSourceStack> ctx, int page) throws CommandSyntaxException {
      ResourceKey<? extends Registry<?>> registryKey = CommandUtils.getResourceKey(ctx, "registry", ROOT_REGISTRY_KEY).orElseThrow();
      Registry<?> registry = ((CommandSourceStack)ctx.getSource())
         .getServer()
         .registryAccess()
         .lookup(registryKey)
         .orElseThrow(() -> UNKNOWN_REGISTRY.create(registryKey.identifier()));
      Identifier tagLocation = IdentifierArgument.getId(ctx, "tag");
      TagKey<?> tagKey = TagKey.create(cast(registryKey), tagLocation);
      Optional<HolderSet.Named<?>> optional = registry.get(cast(tagKey));
      HolderSet.Named<?> tag = optional.orElseThrow(() -> UNKNOWN_TAG.create(tagKey.location(), registryKey.identifier()));
      ((CommandSourceStack)ctx.getSource())
         .sendSuccess(
            () -> createMessage(
               CommandUtils.makeTranslatableWithFallback(
                  "commands.neoforge.tags.tag_key",
                  Component.literal(tagKey.registry().identifier().toString()).withStyle(ChatFormatting.GOLD),
                  Component.literal(tagKey.location().toString()).withStyle(ChatFormatting.DARK_GREEN)
               ),
               "commands.neoforge.tags.element_count",
               "commands.neoforge.tags.copy_element_names",
               tag.size(),
               page,
               ChatFormatting.YELLOW,
               () -> tag.stream().map(s -> (String)s.unwrap().map(k -> k.identifier().toString(), Object::toString))
            ),
            false
         );
      return tag.size();
   }

   private static int queryElementTags(CommandContext<CommandSourceStack> ctx, int page) throws CommandSyntaxException {
      ResourceKey<? extends Registry<?>> registryKey = CommandUtils.getResourceKey(ctx, "registry", ROOT_REGISTRY_KEY).orElseThrow();
      Registry<?> registry = ((CommandSourceStack)ctx.getSource())
         .getServer()
         .registryAccess()
         .lookup(registryKey)
         .orElseThrow(() -> UNKNOWN_REGISTRY.create(registryKey.identifier()));
      Identifier elementLocation = IdentifierArgument.getId(ctx, "element");
      ResourceKey<?> elementKey = ResourceKey.create(cast(registryKey), elementLocation);
      Optional<Holder<?>> elementHolderOpt = registry.get(cast(elementKey));
      Holder<?> elementHolder = elementHolderOpt.orElseThrow(() -> UNKNOWN_ELEMENT.create(elementLocation, registryKey.identifier()));
      long containingTagsCount = elementHolder.tags().count();
      ((CommandSourceStack)ctx.getSource())
         .sendSuccess(
            () -> createMessage(
               CommandUtils.makeTranslatableWithFallback(
                  "commands.neoforge.tags.element",
                  Component.literal(registryKey.identifier().toString()).withStyle(ChatFormatting.GOLD),
                  Component.literal(elementLocation.toString()).withStyle(ChatFormatting.YELLOW)
               ),
               "commands.neoforge.tags.containing_tag_count",
               "commands.neoforge.tags.copy_tag_names",
               containingTagsCount,
               page,
               ChatFormatting.DARK_GREEN,
               () -> elementHolder.tags().map(k -> k.location().toString())
            ),
            false
         );
      return (int)containingTagsCount;
   }

   private static MutableComponent createMessage(
      MutableComponent header,
      String containsText,
      String copyHoverText,
      long count,
      long currentPage,
      ChatFormatting elementColor,
      Supplier<Stream<String>> names
   ) {
      long totalPages = (count - 1L) / 8L + 1L;
      long actualPage = Mth.clamp(currentPage, 1L, totalPages);
      MutableComponent containsComponent = CommandUtils.makeTranslatableWithFallback(containsText, count);
      if (count > 0L) {
         StringBuilder clipboardTextBuilder = new StringBuilder();
         boolean reachedLimit = false;
         int countedLines = 0;

         for (Iterator<String> iterator = names.get().sorted().iterator(); iterator.hasNext(); countedLines++) {
            String line = iterator.next();
            if (clipboardTextBuilder.length() + line.length() > 32600L) {
               reachedLimit = true;
               break;
            }

            clipboardTextBuilder.append(line).append('\n');
         }

         if (!clipboardTextBuilder.isEmpty()) {
            clipboardTextBuilder.deleteCharAt(clipboardTextBuilder.length() - 1);
         }

         String clipboardText;
         if (reachedLimit) {
            clipboardText = "(Too many entries to fit in clipboard, showing only first "
               + countedLines
               + " entries...)\n"
               + clipboardTextBuilder
               + "\n(..."
               + (count - countedLines)
               + " more entries not shown)";
         } else {
            clipboardText = clipboardTextBuilder.toString();
         }

         containsComponent = ComponentUtils.wrapInSquareBrackets(
            containsComponent.withStyle(
               s -> s.withColor(ChatFormatting.GREEN)
                  .withClickEvent(new ClickEvent.CopyToClipboard(clipboardText))
                  .withHoverEvent(new HoverEvent.ShowText(CommandUtils.makeTranslatableWithFallback(copyHoverText)))
            )
         );
         containsComponent = CommandUtils.makeTranslatableWithFallback("commands.neoforge.tags.page_info", containsComponent, actualPage, totalPages);
      }

      MutableComponent tagElements = Component.literal("").append(containsComponent);
      names.get()
         .sorted()
         .skip(8L * (actualPage - 1L))
         .limit(8L)
         .map(Component::literal)
         .map(t -> t.withStyle(elementColor))
         .map(t -> CommandUtils.makeTranslatableWithFallback("\n - ").append(t))
         .forEach(tagElements::append);
      return header.append("\n").append(tagElements);
   }

   private static <O> O cast(Object input) {
      return (O)input;
   }
}
