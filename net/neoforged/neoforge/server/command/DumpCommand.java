package net.neoforged.neoforge.server.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.logging.LogUtils;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLLoader;
import org.slf4j.Logger;

class DumpCommand {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceKey<Registry<Registry<?>>> ROOT_REGISTRY_KEY = ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("root"));
   private static final String ALPHABETICAL_SORT_PARAM = "alphabetical_sort";
   private static final String PRINT_NUMERIC_ID_PARAM = "print_numeric_ids";
   private static final DynamicCommandExceptionType UNKNOWN_REGISTRY = new DynamicCommandExceptionType(
      key -> CommandUtils.makeTranslatableWithFallback("commands.neoforge.dump.error.unknown_registry", key)
   );

   public static ArgumentBuilder<CommandSourceStack, ?> register() {
      return ((LiteralArgumentBuilder)Commands.literal("dump").requires(cs -> cs.hasPermission(4)))
         .then(
            Commands.literal("registry")
               .then(
                  ((RequiredArgumentBuilder)Commands.argument("registry", ResourceKeyArgument.key(ROOT_REGISTRY_KEY))
                        .suggests(CommandUtils::suggestRegistries)
                        .executes(context -> dumpRegistry(context, false, false)))
                     .then(
                        ((RequiredArgumentBuilder)Commands.argument("alphabetical_sort", BoolArgumentType.bool())
                              .executes(context -> dumpRegistry(context, BoolArgumentType.getBool(context, "alphabetical_sort"), false)))
                           .then(
                              Commands.argument("print_numeric_ids", BoolArgumentType.bool())
                                 .executes(
                                    context -> dumpRegistry(
                                       context, BoolArgumentType.getBool(context, "alphabetical_sort"), BoolArgumentType.getBool(context, "print_numeric_ids")
                                    )
                                 )
                           )
                     )
               )
         );
   }

   private static int dumpRegistry(CommandContext<CommandSourceStack> ctx, boolean alphabeticalSort, boolean printNumericIds) throws CommandSyntaxException {
      ResourceKey<? extends Registry<?>> registryKey = CommandUtils.getResourceKey(ctx, "registry", ROOT_REGISTRY_KEY).orElseThrow();
      Registry<?> registry = ((CommandSourceStack)ctx.getSource())
         .getServer()
         .registryAccess()
         .registry(registryKey)
         .orElseThrow(() -> UNKNOWN_REGISTRY.create(registryKey.location()));
      String fileLocationForErrorReporting = "";

      try {
         Path registryDumpDirectory = FMLLoader.getGamePath().resolve("dumps").resolve("registry");
         Path registryNamespaceDirectory = registryDumpDirectory.resolve(registryKey.location().getNamespace().replaceAll("[/:.]", "_"));
         Files.createDirectories(registryNamespaceDirectory);
         String fileName = registryKey.location().getPath().replaceAll("[/:.]", "_") + ".txt";
         Path registryDumpFile = registryNamespaceDirectory.resolve(fileName);
         fileLocationForErrorReporting = registryDumpFile.toString();

         try (OutputStream outputStream = Files.newOutputStream(registryDumpFile)) {
            for (ResourceLocation registryKeys : getSortedRegistryKeys(alphabeticalSort, printNumericIds, registry)) {
               String results = registryKeys.toString();
               if (printNumericIds) {
                  results = registry.getId(registryKeys) + " - " + results;
               }

               outputStream.write((results + "\n").getBytes());
            }
         }

         MutableComponent filePathComponent = Component.literal("..." + FMLLoader.getGamePath().relativize(registryDumpFile))
            .withStyle(ChatFormatting.UNDERLINE)
            .withStyle(ChatFormatting.GOLD);
         if (!FMLLoader.getDist().isDedicatedServer()) {
            filePathComponent.withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, registryDumpFile.toString())));
         }

         ((CommandSourceStack)ctx.getSource())
            .sendSuccess(
               () -> CommandUtils.makeTranslatableWithFallback(
                  "commands.neoforge.dump.success", Component.literal(registryKey.location().toString()).withStyle(ChatFormatting.YELLOW), filePathComponent
               ),
               false
            );
         return 1;
      } catch (Exception var17) {
         ((CommandSourceStack)ctx.getSource())
            .sendFailure(
               CommandUtils.makeTranslatableWithFallback(
                  "commands.neoforge.dump.failure",
                  Component.literal(registryKey.location().toString()).withStyle(ChatFormatting.YELLOW),
                  Component.literal(fileLocationForErrorReporting).withStyle(ChatFormatting.GOLD)
               )
            );
         LOGGER.error("Failed to create new file with " + registryKey + " registry's contents at " + fileLocationForErrorReporting, var17);
         return 0;
      }
   }

   private static List<ResourceLocation> getSortedRegistryKeys(boolean alphabeticalSort, boolean printNumericIds, Registry<?> registry) {
      List<ResourceLocation> sortedRegistryNames = new ArrayList<>(registry.keySet());
      if (alphabeticalSort) {
         sortedRegistryNames.sort(ResourceLocation::compareNamespaced);
      } else if (printNumericIds) {
         sortedRegistryNames = sortedRegistryNames.stream().sorted(Comparator.comparingInt(registry::getId)).toList();
      }

      return sortedRegistryNames;
   }
}
