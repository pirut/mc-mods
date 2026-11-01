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
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class DataCommands {
   private static final SimpleCommandExceptionType ERROR_MERGE_UNCHANGED = new SimpleCommandExceptionType(Component.translatable("commands.data.merge.failed"));
   private static final DynamicCommandExceptionType ERROR_GET_NOT_NUMBER = new DynamicCommandExceptionType(
      p_304320_ -> Component.translatableEscape("commands.data.get.invalid", p_304320_)
   );
   private static final DynamicCommandExceptionType ERROR_GET_NON_EXISTENT = new DynamicCommandExceptionType(
      p_304315_ -> Component.translatableEscape("commands.data.get.unknown", p_304315_)
   );
   private static final SimpleCommandExceptionType ERROR_MULTIPLE_TAGS = new SimpleCommandExceptionType(Component.translatable("commands.data.get.multiple"));
   private static final DynamicCommandExceptionType ERROR_EXPECTED_OBJECT = new DynamicCommandExceptionType(
      p_304316_ -> Component.translatableEscape("commands.data.modify.expected_object", p_304316_)
   );
   private static final DynamicCommandExceptionType ERROR_EXPECTED_VALUE = new DynamicCommandExceptionType(
      p_304317_ -> Component.translatableEscape("commands.data.modify.expected_value", p_304317_)
   );
   private static final Dynamic2CommandExceptionType ERROR_INVALID_SUBSTRING = new Dynamic2CommandExceptionType(
      (p_304318_, p_304319_) -> Component.translatableEscape("commands.data.modify.invalid_substring", p_304318_, p_304319_)
   );
   public static final List<Function<String, DataCommands.DataProvider>> ALL_PROVIDERS = ImmutableList.of(
      EntityDataAccessor.PROVIDER, BlockDataAccessor.PROVIDER, StorageDataAccessor.PROVIDER
   );
   public static final List<DataCommands.DataProvider> TARGET_PROVIDERS = ALL_PROVIDERS.stream()
      .map(p_139450_ -> p_139450_.apply("target"))
      .collect(ImmutableList.toImmutableList());
   public static final List<DataCommands.DataProvider> SOURCE_PROVIDERS = ALL_PROVIDERS.stream()
      .map(p_139410_ -> p_139410_.apply("source"))
      .collect(ImmutableList.toImmutableList());

   public static void register(CommandDispatcher<CommandSourceStack> p_139366_) {
      LiteralArgumentBuilder<CommandSourceStack> $$1 = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("data")
         .requires(p_139381_ -> p_139381_.hasPermission(2));

      for (DataCommands.DataProvider $$2 : TARGET_PROVIDERS) {
         ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)$$1.then(
                     $$2.wrap(
                        Commands.literal("merge"),
                        p_139471_ -> p_139471_.then(
                           Commands.argument("nbt", CompoundTagArgument.compoundTag())
                              .executes(
                                 p_142857_ -> mergeData(
                                    (CommandSourceStack)p_142857_.getSource(), $$2.access(p_142857_), CompoundTagArgument.getCompoundTag(p_142857_, "nbt")
                                 )
                              )
                        )
                     )
                  ))
                  .then(
                     $$2.wrap(
                        Commands.literal("get"),
                        p_139453_ -> p_139453_.executes(p_142849_ -> getData((CommandSourceStack)p_142849_.getSource(), $$2.access(p_142849_)))
                           .then(
                              ((RequiredArgumentBuilder)Commands.argument("path", NbtPathArgument.nbtPath())
                                    .executes(
                                       p_142841_ -> getData(
                                          (CommandSourceStack)p_142841_.getSource(), $$2.access(p_142841_), NbtPathArgument.getPath(p_142841_, "path")
                                       )
                                    ))
                                 .then(
                                    Commands.argument("scale", DoubleArgumentType.doubleArg())
                                       .executes(
                                          p_142833_ -> getNumeric(
                                             (CommandSourceStack)p_142833_.getSource(),
                                             $$2.access(p_142833_),
                                             NbtPathArgument.getPath(p_142833_, "path"),
                                             DoubleArgumentType.getDouble(p_142833_, "scale")
                                          )
                                       )
                                 )
                           )
                     )
                  ))
               .then(
                  $$2.wrap(
                     Commands.literal("remove"),
                     p_139413_ -> p_139413_.then(
                        Commands.argument("path", NbtPathArgument.nbtPath())
                           .executes(
                              p_142820_ -> removeData(
                                 (CommandSourceStack)p_142820_.getSource(), $$2.access(p_142820_), NbtPathArgument.getPath(p_142820_, "path")
                              )
                           )
                     )
                  )
               ))
            .then(
               decorateModification(
                  (p_139368_, p_139369_) -> p_139368_.then(
                        Commands.literal("insert")
                           .then(
                              Commands.argument("index", IntegerArgumentType.integer())
                                 .then(
                                    p_139369_.create(
                                       (p_142859_, p_142860_, p_142861_, p_142862_) -> p_142861_.insert(
                                          IntegerArgumentType.getInteger(p_142859_, "index"), p_142860_, p_142862_
                                       )
                                    )
                                 )
                           )
                     )
                     .then(
                        Commands.literal("prepend")
                           .then(p_139369_.create((p_142851_, p_142852_, p_142853_, p_142854_) -> p_142853_.insert(0, p_142852_, p_142854_)))
                     )
                     .then(
                        Commands.literal("append")
                           .then(p_139369_.create((p_142843_, p_142844_, p_142845_, p_142846_) -> p_142845_.insert(-1, p_142844_, p_142846_)))
                     )
                     .then(
                        Commands.literal("set")
                           .then(p_139369_.create((p_142835_, p_142836_, p_142837_, p_142838_) -> p_142837_.set(p_142836_, (Tag)Iterables.getLast(p_142838_))))
                     )
                     .then(Commands.literal("merge").then(p_139369_.create((p_142822_, p_142823_, p_142824_, p_142825_) -> {
                        CompoundTag $$4 = new CompoundTag();

                        for (Tag $$5 : p_142825_) {
                           if (NbtPathArgument.NbtPath.isTooDeep($$5, 0)) {
                              throw NbtPathArgument.ERROR_DATA_TOO_DEEP.create();
                           }

                           if (!($$5 instanceof CompoundTag $$6)) {
                              throw ERROR_EXPECTED_OBJECT.create($$5);
                           }

                           $$4.merge($$6);
                        }

                        Collection<Tag> $$7 = p_142824_.getOrCreate(p_142823_, CompoundTag::new);
                        int $$8 = 0;

                        for (Tag $$9 : $$7) {
                           if (!($$9 instanceof CompoundTag $$10)) {
                              throw ERROR_EXPECTED_OBJECT.create($$9);
                           }

                           CompoundTag $$12 = $$10.copy();
                           $$10.merge($$4);
                           $$8 += $$12.equals($$10) ? 0 : 1;
                        }

                        return $$8;
                     })))
               )
            );
      }

      p_139366_.register($$1);
   }

   private static String getAsText(Tag p_265255_) throws CommandSyntaxException {
      if (p_265255_.getType().isValue()) {
         return p_265255_.getAsString();
      } else {
         throw ERROR_EXPECTED_VALUE.create(p_265255_);
      }
   }

   private static List<Tag> stringifyTagList(List<Tag> p_288980_, DataCommands.StringProcessor p_289012_) throws CommandSyntaxException {
      List<Tag> $$2 = new ArrayList<>(p_288980_.size());

      for (Tag $$3 : p_288980_) {
         String $$4 = getAsText($$3);
         $$2.add(StringTag.valueOf(p_289012_.process($$4)));
      }

      return $$2;
   }

   private static ArgumentBuilder<CommandSourceStack, ?> decorateModification(
      BiConsumer<ArgumentBuilder<CommandSourceStack, ?>, DataCommands.DataManipulatorDecorator> p_139404_
   ) {
      LiteralArgumentBuilder<CommandSourceStack> $$1 = Commands.literal("modify");

      for (DataCommands.DataProvider $$2 : TARGET_PROVIDERS) {
         $$2.wrap(
            $$1,
            p_264816_ -> {
               ArgumentBuilder<CommandSourceStack, ?> $$3 = Commands.argument("targetPath", NbtPathArgument.nbtPath());

               for (DataCommands.DataProvider $$4 : SOURCE_PROVIDERS) {
                  p_139404_.accept(
                     $$3,
                     p_142807_ -> $$4.wrap(
                        Commands.literal("from"),
                        p_142812_ -> p_142812_.executes(p_264829_ -> manipulateData(p_264829_, $$2, p_142807_, getSingletonSource(p_264829_, $$4)))
                           .then(
                              Commands.argument("sourcePath", NbtPathArgument.nbtPath())
                                 .executes(p_264842_ -> manipulateData(p_264842_, $$2, p_142807_, resolveSourcePath(p_264842_, $$4)))
                           )
                     )
                  );
                  p_139404_.accept(
                     $$3,
                     p_264836_ -> $$4.wrap(
                        Commands.literal("string"),
                        p_287357_ -> p_287357_.executes(
                              p_288732_ -> manipulateData(
                                 p_288732_, $$2, p_264836_, stringifyTagList(getSingletonSource(p_288732_, $$4), p_264813_ -> p_264813_)
                              )
                           )
                           .then(
                              ((RequiredArgumentBuilder)Commands.argument("sourcePath", NbtPathArgument.nbtPath())
                                    .executes(
                                       p_288737_ -> manipulateData(
                                          p_288737_, $$2, p_264836_, stringifyTagList(resolveSourcePath(p_288737_, $$4), p_264821_ -> p_264821_)
                                       )
                                    ))
                                 .then(
                                    ((RequiredArgumentBuilder)Commands.argument("start", IntegerArgumentType.integer())
                                          .executes(
                                             p_288753_ -> manipulateData(
                                                p_288753_,
                                                $$2,
                                                p_264836_,
                                                stringifyTagList(
                                                   resolveSourcePath(p_288753_, $$4),
                                                   p_287353_ -> substring(p_287353_, IntegerArgumentType.getInteger(p_288753_, "start"))
                                                )
                                             )
                                          ))
                                       .then(
                                          Commands.argument("end", IntegerArgumentType.integer())
                                             .executes(
                                                p_288749_ -> manipulateData(
                                                   p_288749_,
                                                   $$2,
                                                   p_264836_,
                                                   stringifyTagList(
                                                      resolveSourcePath(p_288749_, $$4),
                                                      p_287359_ -> substring(
                                                         p_287359_,
                                                         IntegerArgumentType.getInteger(p_288749_, "start"),
                                                         IntegerArgumentType.getInteger(p_288749_, "end")
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

               p_139404_.accept($$3, p_142799_ -> Commands.literal("value").then(Commands.argument("value", NbtTagArgument.nbtTag()).executes(p_142803_ -> {
                  List<Tag> $$3x = Collections.singletonList(NbtTagArgument.getNbtTag(p_142803_, "value"));
                  return manipulateData(p_142803_, $$2, p_142799_, $$3x);
               })));
               return p_264816_.then($$3);
            }
         );
      }

      return $$1;
   }

   private static String validatedSubstring(String p_288976_, int p_288968_, int p_289018_) throws CommandSyntaxException {
      if (p_288968_ >= 0 && p_289018_ <= p_288976_.length() && p_288968_ <= p_289018_) {
         return p_288976_.substring(p_288968_, p_289018_);
      } else {
         throw ERROR_INVALID_SUBSTRING.create(p_288968_, p_289018_);
      }
   }

   private static String substring(String p_287625_, int p_287772_, int p_287598_) throws CommandSyntaxException {
      int $$3 = p_287625_.length();
      int $$4 = getOffset(p_287772_, $$3);
      int $$5 = getOffset(p_287598_, $$3);
      return validatedSubstring(p_287625_, $$4, $$5);
   }

   private static String substring(String p_287744_, int p_287741_) throws CommandSyntaxException {
      int $$2 = p_287744_.length();
      return validatedSubstring(p_287744_, getOffset(p_287741_, $$2), $$2);
   }

   private static int getOffset(int p_287638_, int p_287600_) {
      return p_287638_ >= 0 ? p_287638_ : p_287600_ + p_287638_;
   }

   private static List<Tag> getSingletonSource(CommandContext<CommandSourceStack> p_265108_, DataCommands.DataProvider p_265370_) throws CommandSyntaxException {
      DataAccessor $$2 = p_265370_.access(p_265108_);
      return Collections.singletonList($$2.getData());
   }

   private static List<Tag> resolveSourcePath(CommandContext<CommandSourceStack> p_265468_, DataCommands.DataProvider p_265670_) throws CommandSyntaxException {
      DataAccessor $$2 = p_265670_.access(p_265468_);
      NbtPathArgument.NbtPath $$3 = NbtPathArgument.getPath(p_265468_, "sourcePath");
      return $$3.get($$2.getData());
   }

   private static int manipulateData(
      CommandContext<CommandSourceStack> p_139376_, DataCommands.DataProvider p_139377_, DataCommands.DataManipulator p_139378_, List<Tag> p_139379_
   ) throws CommandSyntaxException {
      DataAccessor $$4 = p_139377_.access(p_139376_);
      NbtPathArgument.NbtPath $$5 = NbtPathArgument.getPath(p_139376_, "targetPath");
      CompoundTag $$6 = $$4.getData();
      int $$7 = p_139378_.modify(p_139376_, $$6, $$5, p_139379_);
      if ($$7 == 0) {
         throw ERROR_MERGE_UNCHANGED.create();
      } else {
         $$4.setData($$6);
         ((CommandSourceStack)p_139376_.getSource()).sendSuccess(() -> $$4.getModifiedSuccess(), true);
         return $$7;
      }
   }

   private static int removeData(CommandSourceStack p_139386_, DataAccessor p_139387_, NbtPathArgument.NbtPath p_139388_) throws CommandSyntaxException {
      CompoundTag $$3 = p_139387_.getData();
      int $$4 = p_139388_.remove($$3);
      if ($$4 == 0) {
         throw ERROR_MERGE_UNCHANGED.create();
      } else {
         p_139387_.setData($$3);
         p_139386_.sendSuccess(() -> p_139387_.getModifiedSuccess(), true);
         return $$4;
      }
   }

   public static Tag getSingleTag(NbtPathArgument.NbtPath p_139399_, DataAccessor p_139400_) throws CommandSyntaxException {
      Collection<Tag> $$2 = p_139399_.get(p_139400_.getData());
      Iterator<Tag> $$3 = $$2.iterator();
      Tag $$4 = $$3.next();
      if ($$3.hasNext()) {
         throw ERROR_MULTIPLE_TAGS.create();
      } else {
         return $$4;
      }
   }

   private static int getData(CommandSourceStack p_139444_, DataAccessor p_139445_, NbtPathArgument.NbtPath p_139446_) throws CommandSyntaxException {
      Tag $$3 = getSingleTag(p_139446_, p_139445_);
      int $$4;
      if ($$3 instanceof NumericTag) {
         $$4 = Mth.floor(((NumericTag)$$3).getAsDouble());
      } else if ($$3 instanceof CollectionTag) {
         $$4 = ((CollectionTag)$$3).size();
      } else if ($$3 instanceof CompoundTag) {
         $$4 = ((CompoundTag)$$3).size();
      } else {
         if (!($$3 instanceof StringTag)) {
            throw ERROR_GET_NON_EXISTENT.create(p_139446_.toString());
         }

         $$4 = $$3.getAsString().length();
      }

      p_139444_.sendSuccess(() -> p_139445_.getPrintSuccess($$3), false);
      return $$4;
   }

   private static int getNumeric(CommandSourceStack p_139390_, DataAccessor p_139391_, NbtPathArgument.NbtPath p_139392_, double p_139393_) throws CommandSyntaxException {
      Tag $$4 = getSingleTag(p_139392_, p_139391_);
      if (!($$4 instanceof NumericTag)) {
         throw ERROR_GET_NOT_NUMBER.create(p_139392_.toString());
      } else {
         int $$5 = Mth.floor(((NumericTag)$$4).getAsDouble() * p_139393_);
         p_139390_.sendSuccess(() -> p_139391_.getPrintSuccess(p_139392_, p_139393_, $$5), false);
         return $$5;
      }
   }

   private static int getData(CommandSourceStack p_139383_, DataAccessor p_139384_) throws CommandSyntaxException {
      CompoundTag $$2 = p_139384_.getData();
      p_139383_.sendSuccess(() -> p_139384_.getPrintSuccess($$2), false);
      return 1;
   }

   private static int mergeData(CommandSourceStack p_139395_, DataAccessor p_139396_, CompoundTag p_139397_) throws CommandSyntaxException {
      CompoundTag $$3 = p_139396_.getData();
      if (NbtPathArgument.NbtPath.isTooDeep(p_139397_, 0)) {
         throw NbtPathArgument.ERROR_DATA_TOO_DEEP.create();
      } else {
         CompoundTag $$4 = $$3.copy().merge(p_139397_);
         if ($$3.equals($$4)) {
            throw ERROR_MERGE_UNCHANGED.create();
         } else {
            p_139396_.setData($$4);
            p_139395_.sendSuccess(() -> p_139396_.getModifiedSuccess(), true);
            return 1;
         }
      }
   }

   @FunctionalInterface
   interface DataManipulator {
      int modify(CommandContext<CommandSourceStack> var1, CompoundTag var2, NbtPathArgument.NbtPath var3, List<Tag> var4) throws CommandSyntaxException;
   }

   @FunctionalInterface
   interface DataManipulatorDecorator {
      ArgumentBuilder<CommandSourceStack, ?> create(DataCommands.DataManipulator var1);
   }

   public interface DataProvider {
      DataAccessor access(CommandContext<CommandSourceStack> var1) throws CommandSyntaxException;

      ArgumentBuilder<CommandSourceStack, ?> wrap(
         ArgumentBuilder<CommandSourceStack, ?> var1, Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> var2
      );
   }

   @FunctionalInterface
   interface StringProcessor {
      String process(String var1) throws CommandSyntaxException;
   }
}
