package net.minecraft.server.commands.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.PrimitiveTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class DataCommands {
   private static final SimpleCommandExceptionType ERROR_MERGE_UNCHANGED = new SimpleCommandExceptionType(Component.translatable("commands.data.merge.failed"));
   private static final DynamicCommandExceptionType ERROR_GET_NOT_NUMBER = new DynamicCommandExceptionType(
      object -> Component.translatableEscape("commands.data.get.invalid", object)
   );
   private static final DynamicCommandExceptionType ERROR_GET_NON_EXISTENT = new DynamicCommandExceptionType(
      object -> Component.translatableEscape("commands.data.get.unknown", object)
   );
   private static final SimpleCommandExceptionType ERROR_MULTIPLE_TAGS = new SimpleCommandExceptionType(Component.translatable("commands.data.get.multiple"));
   private static final DynamicCommandExceptionType ERROR_EXPECTED_OBJECT = new DynamicCommandExceptionType(
      object -> Component.translatableEscape("commands.data.modify.expected_object", object)
   );
   private static final DynamicCommandExceptionType ERROR_EXPECTED_VALUE = new DynamicCommandExceptionType(
      object -> Component.translatableEscape("commands.data.modify.expected_value", object)
   );
   private static final Dynamic2CommandExceptionType ERROR_INVALID_SUBSTRING = new Dynamic2CommandExceptionType(
      (object, object2) -> Component.translatableEscape("commands.data.modify.invalid_substring", object, object2)
   );
   public static final List<Function<String, DataCommands.DataProvider>> ALL_PROVIDERS = ImmutableList.of(
      EntityDataAccessor.PROVIDER, BlockDataAccessor.PROVIDER, StorageDataAccessor.PROVIDER
   );
   public static final List<DataCommands.DataProvider> TARGET_PROVIDERS = ALL_PROVIDERS.stream()
      .map(function -> function.apply("target"))
      .collect(ImmutableList.toImmutableList());
   public static final List<DataCommands.DataProvider> SOURCE_PROVIDERS = ALL_PROVIDERS.stream()
      .map(function -> function.apply("source"))
      .collect(ImmutableList.toImmutableList());

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("data")
         .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

      for (DataCommands.DataProvider dataProvider : TARGET_PROVIDERS) {
         ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)literalArgumentBuilder.then(
                     dataProvider.wrap(
                        Commands.literal("merge"),
                        argumentBuilder -> argumentBuilder.then(
                           Commands.argument("nbt", CompoundTagArgument.compoundTag())
                              .executes(
                                 commandContext -> mergeData(
                                    (CommandSourceStack)commandContext.getSource(),
                                    dataProvider.access(commandContext),
                                    CompoundTagArgument.getCompoundTag(commandContext, "nbt")
                                 )
                              )
                        )
                     )
                  ))
                  .then(
                     dataProvider.wrap(
                        Commands.literal("get"),
                        argumentBuilder -> argumentBuilder.executes(
                              commandContext -> getData((CommandSourceStack)commandContext.getSource(), dataProvider.access(commandContext))
                           )
                           .then(
                              ((RequiredArgumentBuilder)Commands.argument("path", NbtPathArgument.nbtPath())
                                    .executes(
                                       commandContext -> getData(
                                          (CommandSourceStack)commandContext.getSource(),
                                          dataProvider.access(commandContext),
                                          NbtPathArgument.getPath(commandContext, "path")
                                       )
                                    ))
                                 .then(
                                    Commands.argument("scale", DoubleArgumentType.doubleArg())
                                       .executes(
                                          commandContext -> getNumeric(
                                             (CommandSourceStack)commandContext.getSource(),
                                             dataProvider.access(commandContext),
                                             NbtPathArgument.getPath(commandContext, "path"),
                                             DoubleArgumentType.getDouble(commandContext, "scale")
                                          )
                                       )
                                 )
                           )
                     )
                  ))
               .then(
                  dataProvider.wrap(
                     Commands.literal("remove"),
                     argumentBuilder -> argumentBuilder.then(
                        Commands.argument("path", NbtPathArgument.nbtPath())
                           .executes(
                              commandContext -> removeData(
                                 (CommandSourceStack)commandContext.getSource(),
                                 dataProvider.access(commandContext),
                                 NbtPathArgument.getPath(commandContext, "path")
                              )
                           )
                     )
                  )
               ))
            .then(
               decorateModification(
                  (argumentBuilder, dataManipulatorDecorator) -> argumentBuilder.then(
                        Commands.literal("insert")
                           .then(
                              Commands.argument("index", IntegerArgumentType.integer())
                                 .then(
                                    dataManipulatorDecorator.create(
                                       (commandContext, compoundTag, nbtPath, list) -> nbtPath.insert(
                                          IntegerArgumentType.getInteger(commandContext, "index"), compoundTag, list
                                       )
                                    )
                                 )
                           )
                     )
                     .then(
                        Commands.literal("prepend")
                           .then(dataManipulatorDecorator.create((commandContext, compoundTag, nbtPath, list) -> nbtPath.insert(0, compoundTag, list)))
                     )
                     .then(
                        Commands.literal("append")
                           .then(dataManipulatorDecorator.create((commandContext, compoundTag, nbtPath, list) -> nbtPath.insert(-1, compoundTag, list)))
                     )
                     .then(
                        Commands.literal("set")
                           .then(
                              dataManipulatorDecorator.create(
                                 (commandContext, compoundTag, nbtPath, list) -> nbtPath.set(compoundTag, (Tag)Iterables.getLast(list))
                              )
                           )
                     )
                     .then(Commands.literal("merge").then(dataManipulatorDecorator.create((commandContext, compoundTag, nbtPath, list) -> {
                        CompoundTag compoundTag2 = new CompoundTag();

                        for (Tag tag : list) {
                           if (NbtPathArgument.NbtPath.isTooDeep(tag, 0)) {
                              throw NbtPathArgument.ERROR_DATA_TOO_DEEP.create();
                           }

                           if (!(tag instanceof CompoundTag compoundTag3)) {
                              throw ERROR_EXPECTED_OBJECT.create(tag);
                           }

                           compoundTag2.merge(compoundTag3);
                        }

                        Collection<Tag> collection = nbtPath.getOrCreate(compoundTag, CompoundTag::new);
                        int i = 0;

                        for (Tag tag2 : collection) {
                           if (!(tag2 instanceof CompoundTag compoundTag4)) {
                              throw ERROR_EXPECTED_OBJECT.create(tag2);
                           }

                           CompoundTag compoundTag5 = compoundTag4.copy();
                           compoundTag4.merge(compoundTag2);
                           i += compoundTag5.equals(compoundTag4) ? 0 : 1;
                        }

                        return i;
                     })))
               )
            );
      }

      dispatcher.register(literalArgumentBuilder);
   }

   private static String getAsText(Tag tag) throws CommandSyntaxException {
      return switch (tag) {
         case StringTag(String var7) -> var7;
         case PrimitiveTag primitiveTag -> primitiveTag.toString();
         default -> throw ERROR_EXPECTED_VALUE.create(tag);
      };
   }

   private static List<Tag> stringifyTagList(List<Tag> tagList, DataCommands.StringProcessor processor) throws CommandSyntaxException {
      List<Tag> list = new ArrayList<>(tagList.size());

      for (Tag tag : tagList) {
         String string = getAsText(tag);
         list.add(StringTag.valueOf(processor.process(string)));
      }

      return list;
   }

   private static ArgumentBuilder<CommandSourceStack, ?> decorateModification(
      BiConsumer<ArgumentBuilder<CommandSourceStack, ?>, DataCommands.DataManipulatorDecorator> decorator
   ) {
      LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder = Commands.literal("modify");

      for (DataCommands.DataProvider dataProvider : TARGET_PROVIDERS) {
         dataProvider.wrap(
            literalArgumentBuilder,
            argumentBuilder -> {
               ArgumentBuilder<CommandSourceStack, ?> argumentBuilder2 = Commands.argument("targetPath", NbtPathArgument.nbtPath());

               for (DataCommands.DataProvider dataProvider2 : SOURCE_PROVIDERS) {
                  decorator.accept(
                     argumentBuilder2,
                     dataManipulator -> dataProvider2.wrap(
                        Commands.literal("from"),
                        argumentBuilderx -> argumentBuilderx.executes(
                              commandContext -> manipulateData(commandContext, dataProvider, dataManipulator, getSingletonSource(commandContext, dataProvider2))
                           )
                           .then(
                              Commands.argument("sourcePath", NbtPathArgument.nbtPath())
                                 .executes(
                                    commandContext -> manipulateData(
                                       commandContext, dataProvider, dataManipulator, resolveSourcePath(commandContext, dataProvider2)
                                    )
                                 )
                           )
                     )
                  );
                  decorator.accept(
                     argumentBuilder2,
                     dataManipulator -> dataProvider2.wrap(
                        Commands.literal("string"),
                        argumentBuilderx -> argumentBuilderx.executes(
                              commandContext -> manipulateData(
                                 commandContext,
                                 dataProvider,
                                 dataManipulator,
                                 stringifyTagList(getSingletonSource(commandContext, dataProvider2), string -> string)
                              )
                           )
                           .then(
                              ((RequiredArgumentBuilder)Commands.argument("sourcePath", NbtPathArgument.nbtPath())
                                    .executes(
                                       commandContext -> manipulateData(
                                          commandContext,
                                          dataProvider,
                                          dataManipulator,
                                          stringifyTagList(resolveSourcePath(commandContext, dataProvider2), string -> string)
                                       )
                                    ))
                                 .then(
                                    ((RequiredArgumentBuilder)Commands.argument("start", IntegerArgumentType.integer())
                                          .executes(
                                             commandContext -> manipulateData(
                                                commandContext,
                                                dataProvider,
                                                dataManipulator,
                                                stringifyTagList(
                                                   resolveSourcePath(commandContext, dataProvider2),
                                                   string -> substring(string, IntegerArgumentType.getInteger(commandContext, "start"))
                                                )
                                             )
                                          ))
                                       .then(
                                          Commands.argument("end", IntegerArgumentType.integer())
                                             .executes(
                                                commandContext -> manipulateData(
                                                   commandContext,
                                                   dataProvider,
                                                   dataManipulator,
                                                   stringifyTagList(
                                                      resolveSourcePath(commandContext, dataProvider2),
                                                      string -> substring(
                                                         string,
                                                         IntegerArgumentType.getInteger(commandContext, "start"),
                                                         IntegerArgumentType.getInteger(commandContext, "end")
                                                      )
                                                   )
                                                )
                                             )
                                       )
                                 )
                           )
                     )
                  );
               }

               decorator.accept(
                  argumentBuilder2,
                  dataManipulator -> Commands.literal("value").then(Commands.argument("value", NbtTagArgument.nbtTag()).executes(commandContext -> {
                     List<Tag> list = Collections.singletonList(NbtTagArgument.getNbtTag(commandContext, "value"));
                     return manipulateData(commandContext, dataProvider, dataManipulator, list);
                  }))
               );
               return argumentBuilder.then(argumentBuilder2);
            }
         );
      }

      return literalArgumentBuilder;
   }

   private static String validatedSubstring(String source, int start, int end) throws CommandSyntaxException {
      if (start >= 0 && end <= source.length() && start <= end) {
         return source.substring(start, end);
      } else {
         throw ERROR_INVALID_SUBSTRING.create(start, end);
      }
   }

   private static String substring(String source, int start, int end) throws CommandSyntaxException {
      int i = source.length();
      int j = getOffset(start, i);
      int k = getOffset(end, i);
      return validatedSubstring(source, j, k);
   }

   private static String substring(String source, int start) throws CommandSyntaxException {
      int i = source.length();
      return validatedSubstring(source, getOffset(start, i), i);
   }

   private static int getOffset(int index, int length) {
      return index >= 0 ? index : length + index;
   }

   private static List<Tag> getSingletonSource(CommandContext<CommandSourceStack> context, DataCommands.DataProvider dataProvider) throws CommandSyntaxException {
      DataAccessor dataAccessor = dataProvider.access(context);
      return Collections.singletonList(dataAccessor.getData());
   }

   private static List<Tag> resolveSourcePath(CommandContext<CommandSourceStack> context, DataCommands.DataProvider dataProvider) throws CommandSyntaxException {
      DataAccessor dataAccessor = dataProvider.access(context);
      NbtPathArgument.NbtPath nbtPath = NbtPathArgument.getPath(context, "sourcePath");
      return nbtPath.get(dataAccessor.getData());
   }

   private static int manipulateData(
      CommandContext<CommandSourceStack> source, DataCommands.DataProvider dataProvider, DataCommands.DataManipulator dataManipulator, List<Tag> tags
   ) throws CommandSyntaxException {
      DataAccessor dataAccessor = dataProvider.access(source);
      NbtPathArgument.NbtPath nbtPath = NbtPathArgument.getPath(source, "targetPath");
      CompoundTag compoundTag = dataAccessor.getData();
      int i = dataManipulator.modify(source, compoundTag, nbtPath, tags);
      if (i == 0) {
         throw ERROR_MERGE_UNCHANGED.create();
      } else {
         dataAccessor.setData(compoundTag);
         ((CommandSourceStack)source.getSource()).sendSuccess(() -> dataAccessor.getModifiedSuccess(), true);
         return i;
      }
   }

   private static int removeData(CommandSourceStack source, DataAccessor accessor, NbtPathArgument.NbtPath path) throws CommandSyntaxException {
      CompoundTag compoundTag = accessor.getData();
      int i = path.remove(compoundTag);
      if (i == 0) {
         throw ERROR_MERGE_UNCHANGED.create();
      } else {
         accessor.setData(compoundTag);
         source.sendSuccess(() -> accessor.getModifiedSuccess(), true);
         return i;
      }
   }

   public static Tag getSingleTag(NbtPathArgument.NbtPath path, DataAccessor accessor) throws CommandSyntaxException {
      Collection<Tag> collection = path.get(accessor.getData());
      Iterator<Tag> iterator = collection.iterator();
      Tag tag = iterator.next();
      if (iterator.hasNext()) {
         throw ERROR_MULTIPLE_TAGS.create();
      } else {
         return tag;
      }
   }

   private static int getData(CommandSourceStack source, DataAccessor accessor, NbtPathArgument.NbtPath path) throws CommandSyntaxException {
      Tag tag = getSingleTag(path, accessor);

      int i = switch (tag) {
         case NumericTag numericTag -> Mth.floor(numericTag.doubleValue());
         case CollectionTag collectionTag -> collectionTag.size();
         case CompoundTag compoundTag -> compoundTag.size();
         case StringTag(String var14) -> var14.length();
         case EndTag endTag -> throw ERROR_GET_NON_EXISTENT.create(path.toString());
         default -> throw new MatchException(null, null);
      };
      source.sendSuccess(() -> accessor.getPrintSuccess(tag), false);
      return i;
   }

   private static int getNumeric(CommandSourceStack source, DataAccessor accessor, NbtPathArgument.NbtPath path, double scale) throws CommandSyntaxException {
      Tag tag = getSingleTag(path, accessor);
      if (!(tag instanceof NumericTag)) {
         throw ERROR_GET_NOT_NUMBER.create(path.toString());
      } else {
         int i = Mth.floor(((NumericTag)tag).doubleValue() * scale);
         source.sendSuccess(() -> accessor.getPrintSuccess(path, scale, i), false);
         return i;
      }
   }

   private static int getData(CommandSourceStack source, DataAccessor accessor) throws CommandSyntaxException {
      CompoundTag compoundTag = accessor.getData();
      source.sendSuccess(() -> accessor.getPrintSuccess(compoundTag), false);
      return 1;
   }

   private static int mergeData(CommandSourceStack source, DataAccessor accessor, CompoundTag nbt) throws CommandSyntaxException {
      CompoundTag compoundTag = accessor.getData();
      if (NbtPathArgument.NbtPath.isTooDeep(nbt, 0)) {
         throw NbtPathArgument.ERROR_DATA_TOO_DEEP.create();
      } else {
         CompoundTag compoundTag2 = compoundTag.copy().merge(nbt);
         if (compoundTag.equals(compoundTag2)) {
            throw ERROR_MERGE_UNCHANGED.create();
         } else {
            accessor.setData(compoundTag2);
            source.sendSuccess(() -> accessor.getModifiedSuccess(), true);
            return 1;
         }
      }
   }

   @FunctionalInterface
   interface DataManipulator {
      int modify(CommandContext<CommandSourceStack> commandContext, CompoundTag compoundTag, NbtPathArgument.NbtPath nbtPath, List<Tag> list) throws CommandSyntaxException;
   }

   @FunctionalInterface
   interface DataManipulatorDecorator {
      ArgumentBuilder<CommandSourceStack, ?> create(DataCommands.DataManipulator dataManipulator);
   }

   public interface DataProvider {
      DataAccessor access(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;

      ArgumentBuilder<CommandSourceStack, ?> wrap(
         ArgumentBuilder<CommandSourceStack, ?> builder, Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> action
      );
   }

   @FunctionalInterface
   interface StringProcessor {
      String process(String string) throws CommandSyntaxException;
   }
}
