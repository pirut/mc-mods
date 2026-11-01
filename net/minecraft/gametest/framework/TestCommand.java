package net.minecraft.gametest.framework;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceSelectorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundGameTestHighlightPosPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.commands.InCommandFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import org.apache.commons.lang3.mutable.MutableInt;

public class TestCommand {
   public static final int TEST_NEARBY_SEARCH_RADIUS = 15;
   public static final int TEST_FULL_SEARCH_RADIUS = 250;
   public static final int VERIFY_TEST_GRID_AXIS_SIZE = 10;
   public static final int VERIFY_TEST_BATCH_SIZE = 100;
   private static final int DEFAULT_CLEAR_RADIUS = 250;
   private static final int MAX_CLEAR_RADIUS = 1024;
   private static final int TEST_POS_Z_OFFSET_FROM_PLAYER = 3;
   private static final int DEFAULT_X_SIZE = 5;
   private static final int DEFAULT_Y_SIZE = 5;
   private static final int DEFAULT_Z_SIZE = 5;
   private static final SimpleCommandExceptionType CLEAR_NO_TESTS = new SimpleCommandExceptionType(Component.translatable("commands.test.clear.error.no_tests"));
   private static final SimpleCommandExceptionType RESET_NO_TESTS = new SimpleCommandExceptionType(Component.translatable("commands.test.reset.error.no_tests"));
   private static final SimpleCommandExceptionType TEST_INSTANCE_COULD_NOT_BE_FOUND = new SimpleCommandExceptionType(
      Component.translatable("commands.test.error.test_instance_not_found")
   );
   private static final SimpleCommandExceptionType NO_STRUCTURES_TO_EXPORT = new SimpleCommandExceptionType(
      Component.literal("Could not find any structures to export")
   );
   private static final SimpleCommandExceptionType NO_TEST_INSTANCES = new SimpleCommandExceptionType(
      Component.translatable("commands.test.error.no_test_instances")
   );
   private static final Dynamic3CommandExceptionType NO_TEST_CONTAINING = new Dynamic3CommandExceptionType(
      (object, object2, object3) -> Component.translatableEscape("commands.test.error.no_test_containing_pos", object, object2, object3)
   );
   private static final DynamicCommandExceptionType TOO_LARGE = new DynamicCommandExceptionType(
      object -> Component.translatableEscape("commands.test.error.too_large", object)
   );

   private static int reset(TestFinder testFinder) throws CommandSyntaxException {
      stopTests();
      int i = toGameTestInfos(testFinder.source(), RetryOptions.noRetries(), testFinder)
         .map(gameTestInfo -> resetGameTestInfo(testFinder.source(), gameTestInfo))
         .toList()
         .size();
      if (i == 0) {
         throw CLEAR_NO_TESTS.create();
      } else {
         testFinder.source().sendSuccess(() -> Component.translatable("commands.test.reset.success", i), true);
         return i;
      }
   }

   private static int clear(TestFinder testFinder) throws CommandSyntaxException {
      stopTests();
      CommandSourceStack commandSourceStack = testFinder.source();
      ServerLevel serverLevel = commandSourceStack.getLevel();
      List<TestInstanceBlockEntity> list = testFinder.findTestPos()
         .flatMap(blockPos -> serverLevel.getBlockEntity(blockPos, BlockEntityType.TEST_INSTANCE_BLOCK).stream())
         .toList();

      for (TestInstanceBlockEntity testInstanceBlockEntity : list) {
         StructureUtils.clearSpaceForStructure(testInstanceBlockEntity.getStructureBoundingBox(), serverLevel);
         testInstanceBlockEntity.removeBarriers();
         serverLevel.destroyBlock(testInstanceBlockEntity.getBlockPos(), false);
      }

      if (list.isEmpty()) {
         throw CLEAR_NO_TESTS.create();
      } else {
         commandSourceStack.sendSuccess(() -> Component.translatable("commands.test.clear.success", list.size()), true);
         return list.size();
      }
   }

   private static int export(TestFinder testFinder) throws CommandSyntaxException {
      CommandSourceStack commandSourceStack = testFinder.source();
      ServerLevel serverLevel = commandSourceStack.getLevel();
      int i = 0;
      boolean bl = true;

      for (Iterator<BlockPos> iterator = testFinder.findTestPos().iterator(); iterator.hasNext(); i++) {
         BlockPos blockPos = iterator.next();
         if (!(serverLevel.getBlockEntity(blockPos) instanceof TestInstanceBlockEntity testInstanceBlockEntity)) {
            throw TEST_INSTANCE_COULD_NOT_BE_FOUND.create();
         }

         if (!testInstanceBlockEntity.exportTest(commandSourceStack::sendSystemMessage)) {
            bl = false;
         }
      }

      if (i == 0) {
         throw NO_STRUCTURES_TO_EXPORT.create();
      } else {
         String string = "Exported " + i + " structures";
         testFinder.source().sendSuccess(() -> Component.literal(string), true);
         return bl ? 0 : 1;
      }
   }

   private static int verify(TestFinder testFinder) {
      stopTests();
      CommandSourceStack commandSourceStack = testFinder.source();
      ServerLevel serverLevel = commandSourceStack.getLevel();
      BlockPos blockPos = createTestPositionAround(commandSourceStack);
      Collection<GameTestInfo> collection = Stream.concat(
            toGameTestInfos(commandSourceStack, RetryOptions.noRetries(), testFinder),
            toGameTestInfo(commandSourceStack, RetryOptions.noRetries(), testFinder, 0)
         )
         .toList();
      FailedTestTracker.forgetFailedTests();
      Collection<GameTestBatch> collection2 = new ArrayList<>();

      for (GameTestInfo gameTestInfo : collection) {
         for (Rotation rotation : Rotation.values()) {
            Collection<GameTestInfo> collection3 = new ArrayList<>();

            for (int i = 0; i < 100; i++) {
               GameTestInfo gameTestInfo2 = new GameTestInfo(gameTestInfo.getTestHolder(), rotation, serverLevel, new RetryOptions(1, true));
               gameTestInfo2.setTestBlockPos(gameTestInfo.getTestBlockPos());
               collection3.add(gameTestInfo2);
            }

            GameTestBatch gameTestBatch = GameTestBatchFactory.toGameTestBatch(collection3, gameTestInfo.getTest().batch(), rotation.ordinal());
            collection2.add(gameTestBatch);
         }
      }

      StructureGridSpawner structureGridSpawner = new StructureGridSpawner(blockPos, 10, true);
      GameTestRunner gameTestRunner = GameTestRunner.Builder.fromBatches(collection2, serverLevel)
         .batcher(GameTestBatchFactory.fromGameTestInfo(100))
         .newStructureSpawner(structureGridSpawner)
         .existingStructureSpawner(structureGridSpawner)
         .haltOnError()
         .clearBetweenBatches()
         .build();
      return trackAndStartRunner(commandSourceStack, gameTestRunner);
   }

   private static int run(TestFinder testFinder, RetryOptions retryOptions, int rotationSteps, int testsPerRow) {
      stopTests();
      CommandSourceStack commandSourceStack = testFinder.source();
      ServerLevel serverLevel = commandSourceStack.getLevel();
      BlockPos blockPos = createTestPositionAround(commandSourceStack);
      Collection<GameTestInfo> collection = Stream.concat(
            toGameTestInfos(commandSourceStack, retryOptions, testFinder), toGameTestInfo(commandSourceStack, retryOptions, testFinder, rotationSteps)
         )
         .toList();
      if (collection.isEmpty()) {
         commandSourceStack.sendSuccess(() -> Component.translatable("commands.test.no_tests"), false);
         return 0;
      } else {
         FailedTestTracker.forgetFailedTests();
         commandSourceStack.sendSuccess(() -> Component.translatable("commands.test.run.running", collection.size()), false);
         GameTestRunner gameTestRunner = GameTestRunner.Builder.fromInfo(collection, serverLevel)
            .newStructureSpawner(new StructureGridSpawner(blockPos, testsPerRow, false))
            .build();
         return trackAndStartRunner(commandSourceStack, gameTestRunner);
      }
   }

   private static int locate(TestFinder testFinder) throws CommandSyntaxException {
      testFinder.source().sendSystemMessage(Component.translatable("commands.test.locate.started"));
      MutableInt mutableInt = new MutableInt(0);
      BlockPos blockPos = BlockPos.containing(testFinder.source().getPosition());
      testFinder.findTestPos()
         .forEach(
            blockPos2 -> {
               if (testFinder.source().getLevel().getBlockEntity(blockPos2) instanceof TestInstanceBlockEntity testInstanceBlockEntity) {
                  Direction var13 = testInstanceBlockEntity.getRotation().rotate(Direction.NORTH);
                  BlockPos blockPos3 = testInstanceBlockEntity.getBlockPos().relative(var13, 2);
                  int ix = (int)var13.getOpposite().toYRot();
                  String string = String.format(Locale.ROOT, "/tp @s %d %d %d %d 0", blockPos3.getX(), blockPos3.getY(), blockPos3.getZ(), ix);
                  int j = blockPos.getX() - blockPos2.getX();
                  int k = blockPos.getZ() - blockPos2.getZ();
                  int l = Mth.floor(Mth.sqrt(j * j + k * k));
                  MutableComponent component = ComponentUtils.wrapInSquareBrackets(
                        Component.translatable("chat.coordinates", blockPos2.getX(), blockPos2.getY(), blockPos2.getZ())
                     )
                     .withStyle(
                        style -> style.withColor(ChatFormatting.GREEN)
                           .withClickEvent(new ClickEvent.SuggestCommand(string))
                           .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip")))
                     );
                  testFinder.source().sendSuccess(() -> Component.translatable("commands.test.locate.found", component, l), false);
                  mutableInt.increment();
               }
            }
         );
      int i = mutableInt.intValue();
      if (i == 0) {
         throw NO_TEST_INSTANCES.create();
      } else {
         testFinder.source().sendSuccess(() -> Component.translatable("commands.test.locate.done", i), true);
         return i;
      }
   }

   private static ArgumentBuilder<CommandSourceStack, ?> runWithRetryOptions(
      ArgumentBuilder<CommandSourceStack, ?> argumentBuilder,
      InCommandFunction<CommandContext<CommandSourceStack>, TestFinder> finderGetter,
      Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> modifier
   ) {
      return argumentBuilder.executes(commandContext -> run(finderGetter.apply(commandContext), RetryOptions.noRetries(), 0, 8))
         .then(
            ((RequiredArgumentBuilder)Commands.argument("numberOfTimes", IntegerArgumentType.integer(0))
                  .executes(
                     commandContext -> run(
                        finderGetter.apply(commandContext), new RetryOptions(IntegerArgumentType.getInteger(commandContext, "numberOfTimes"), false), 0, 8
                     )
                  ))
               .then(
                  modifier.apply(
                     Commands.argument("untilFailed", BoolArgumentType.bool())
                        .executes(
                           commandContext -> run(
                              finderGetter.apply(commandContext),
                              new RetryOptions(
                                 IntegerArgumentType.getInteger(commandContext, "numberOfTimes"), BoolArgumentType.getBool(commandContext, "untilFailed")
                              ),
                              0,
                              8
                           )
                        )
                  )
               )
         );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> runWithRetryOptions(
      ArgumentBuilder<CommandSourceStack, ?> argumentBuilder, InCommandFunction<CommandContext<CommandSourceStack>, TestFinder> finderGetter
   ) {
      return runWithRetryOptions(argumentBuilder, finderGetter, argumentBuilderx -> argumentBuilderx);
   }

   private static ArgumentBuilder<CommandSourceStack, ?> runWithRetryOptionsAndBuildInfo(
      ArgumentBuilder<CommandSourceStack, ?> argumentBuilder, InCommandFunction<CommandContext<CommandSourceStack>, TestFinder> finderGetter
   ) {
      return runWithRetryOptions(
         argumentBuilder,
         finderGetter,
         argumentBuilderx -> argumentBuilderx.then(
            ((RequiredArgumentBuilder)Commands.argument("rotationSteps", IntegerArgumentType.integer())
                  .executes(
                     commandContext -> run(
                        finderGetter.apply(commandContext),
                        new RetryOptions(
                           IntegerArgumentType.getInteger(commandContext, "numberOfTimes"), BoolArgumentType.getBool(commandContext, "untilFailed")
                        ),
                        IntegerArgumentType.getInteger(commandContext, "rotationSteps"),
                        8
                     )
                  ))
               .then(
                  Commands.argument("testsPerRow", IntegerArgumentType.integer())
                     .executes(
                        commandContext -> run(
                           finderGetter.apply(commandContext),
                           new RetryOptions(
                              IntegerArgumentType.getInteger(commandContext, "numberOfTimes"), BoolArgumentType.getBool(commandContext, "untilFailed")
                           ),
                           IntegerArgumentType.getInteger(commandContext, "rotationSteps"),
                           IntegerArgumentType.getInteger(commandContext, "testsPerRow")
                        )
                     )
               )
         )
      );
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
      ArgumentBuilder<CommandSourceStack, ?> argumentBuilder = runWithRetryOptionsAndBuildInfo(
         Commands.argument("onlyRequiredTests", BoolArgumentType.bool()),
         commandContext -> TestFinder.builder().failedTests(commandContext, BoolArgumentType.getBool(commandContext, "onlyRequiredTests"))
      );
      LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder = (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                                               "test"
                                                            )
                                                            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                                                         .then(
                                                            Commands.literal("run")
                                                               .then(
                                                                  runWithRetryOptionsAndBuildInfo(
                                                                     Commands.argument(
                                                                        "tests",
                                                                        ResourceSelectorArgument.resourceSelector(buildContext, Registries.TEST_INSTANCE)
                                                                     ),
                                                                     commandContext -> TestFinder.builder()
                                                                        .byResourceSelection(
                                                                           commandContext,
                                                                           ResourceSelectorArgument.getSelectedResources(commandContext, "tests")
                                                                        )
                                                                  )
                                                               )
                                                         ))
                                                      .then(
                                                         Commands.literal("runmultiple")
                                                            .then(
                                                               ((RequiredArgumentBuilder)Commands.argument(
                                                                        "tests",
                                                                        ResourceSelectorArgument.resourceSelector(buildContext, Registries.TEST_INSTANCE)
                                                                     )
                                                                     .executes(
                                                                        commandContext -> run(
                                                                           TestFinder.builder()
                                                                              .byResourceSelection(
                                                                                 commandContext,
                                                                                 ResourceSelectorArgument.getSelectedResources(commandContext, "tests")
                                                                              ),
                                                                           RetryOptions.noRetries(),
                                                                           0,
                                                                           8
                                                                        )
                                                                     ))
                                                                  .then(
                                                                     Commands.argument("amount", IntegerArgumentType.integer())
                                                                        .executes(
                                                                           commandContext -> run(
                                                                              TestFinder.builder()
                                                                                 .createMultipleCopies(IntegerArgumentType.getInteger(commandContext, "amount"))
                                                                                 .byResourceSelection(
                                                                                    commandContext,
                                                                                    ResourceSelectorArgument.getSelectedResources(commandContext, "tests")
                                                                                 ),
                                                                              RetryOptions.noRetries(),
                                                                              0,
                                                                              8
                                                                           )
                                                                        )
                                                                  )
                                                            )
                                                      ))
                                                   .then(runWithRetryOptions(Commands.literal("runthese"), TestFinder.builder()::allNearby)))
                                                .then(runWithRetryOptions(Commands.literal("runclosest"), TestFinder.builder()::nearest)))
                                             .then(runWithRetryOptions(Commands.literal("runthat"), TestFinder.builder()::lookedAt)))
                                          .then(
                                             runWithRetryOptionsAndBuildInfo(
                                                Commands.literal("runfailed").then(argumentBuilder), TestFinder.builder()::failedTests
                                             )
                                          ))
                                       .then(
                                          Commands.literal("verify")
                                             .then(
                                                Commands.argument("tests", ResourceSelectorArgument.resourceSelector(buildContext, Registries.TEST_INSTANCE))
                                                   .executes(
                                                      commandContext -> verify(
                                                         TestFinder.builder()
                                                            .byResourceSelection(
                                                               commandContext, ResourceSelectorArgument.getSelectedResources(commandContext, "tests")
                                                            )
                                                      )
                                                   )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("locate")
                                          .then(
                                             Commands.argument("tests", ResourceSelectorArgument.resourceSelector(buildContext, Registries.TEST_INSTANCE))
                                                .executes(
                                                   commandContext -> locate(
                                                      TestFinder.builder()
                                                         .byResourceSelection(
                                                            commandContext, ResourceSelectorArgument.getSelectedResources(commandContext, "tests")
                                                         )
                                                   )
                                                )
                                          )
                                    ))
                                 .then(Commands.literal("resetclosest").executes(commandContext -> reset(TestFinder.builder().nearest(commandContext)))))
                              .then(Commands.literal("resetthese").executes(commandContext -> reset(TestFinder.builder().allNearby(commandContext)))))
                           .then(Commands.literal("resetthat").executes(commandContext -> reset(TestFinder.builder().lookedAt(commandContext)))))
                        .then(Commands.literal("clearthat").executes(commandContext -> clear(TestFinder.builder().lookedAt(commandContext)))))
                     .then(Commands.literal("clearthese").executes(commandContext -> clear(TestFinder.builder().allNearby(commandContext)))))
                  .then(
                     ((LiteralArgumentBuilder)Commands.literal("clearall").executes(commandContext -> clear(TestFinder.builder().radius(commandContext, 250))))
                        .then(
                           Commands.argument("radius", IntegerArgumentType.integer())
                              .executes(
                                 commandContext -> clear(
                                    TestFinder.builder().radius(commandContext, Mth.clamp(IntegerArgumentType.getInteger(commandContext, "radius"), 0, 1024))
                                 )
                              )
                        )
                  ))
               .then(Commands.literal("stop").executes(commandContext -> stopTests())))
            .then(
               ((LiteralArgumentBuilder)Commands.literal("pos").executes(commandContext -> showPos((CommandSourceStack)commandContext.getSource(), "pos")))
                  .then(
                     Commands.argument("var", StringArgumentType.word())
                        .executes(
                           commandContext -> showPos((CommandSourceStack)commandContext.getSource(), StringArgumentType.getString(commandContext, "var"))
                        )
                  )
            ))
         .then(
            Commands.literal("create")
               .then(
                  ((RequiredArgumentBuilder)Commands.argument("id", IdentifierArgument.id())
                        .suggests(TestCommand::suggestTestFunction)
                        .executes(
                           commandContext -> createNewStructure(
                              (CommandSourceStack)commandContext.getSource(), IdentifierArgument.getId(commandContext, "id"), 5, 5, 5
                           )
                        ))
                     .then(
                        ((RequiredArgumentBuilder)Commands.argument("width", IntegerArgumentType.integer())
                              .executes(
                                 commandContext -> createNewStructure(
                                    (CommandSourceStack)commandContext.getSource(),
                                    IdentifierArgument.getId(commandContext, "id"),
                                    IntegerArgumentType.getInteger(commandContext, "width"),
                                    IntegerArgumentType.getInteger(commandContext, "width"),
                                    IntegerArgumentType.getInteger(commandContext, "width")
                                 )
                              ))
                           .then(
                              Commands.argument("height", IntegerArgumentType.integer())
                                 .then(
                                    Commands.argument("depth", IntegerArgumentType.integer())
                                       .executes(
                                          commandContext -> createNewStructure(
                                             (CommandSourceStack)commandContext.getSource(),
                                             IdentifierArgument.getId(commandContext, "id"),
                                             IntegerArgumentType.getInteger(commandContext, "width"),
                                             IntegerArgumentType.getInteger(commandContext, "height"),
                                             IntegerArgumentType.getInteger(commandContext, "depth")
                                          )
                                       )
                                 )
                           )
                     )
               )
         );
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         literalArgumentBuilder = (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)literalArgumentBuilder.then(
                     Commands.literal("export")
                        .then(
                           Commands.argument("test", ResourceArgument.resource(buildContext, Registries.TEST_INSTANCE))
                              .executes(
                                 commandContext -> exportTestStructure(
                                    (CommandSourceStack)commandContext.getSource(),
                                    ResourceArgument.getResource(commandContext, "test", Registries.TEST_INSTANCE)
                                 )
                              )
                        )
                  ))
                  .then(Commands.literal("exportclosest").executes(commandContext -> export(TestFinder.builder().nearest(commandContext)))))
               .then(Commands.literal("exportthese").executes(commandContext -> export(TestFinder.builder().allNearby(commandContext)))))
            .then(Commands.literal("exportthat").executes(commandContext -> export(TestFinder.builder().lookedAt(commandContext))));
      }

      dispatcher.register(literalArgumentBuilder);
   }

   public static CompletableFuture<Suggestions> suggestTestFunction(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
      Stream<String> stream = ((CommandSourceStack)context.getSource())
         .registryAccess()
         .lookupOrThrow(Registries.TEST_FUNCTION)
         .listElements()
         .map(Holder::getRegisteredName);
      return SharedSuggestionProvider.suggest(stream, builder);
   }

   private static int resetGameTestInfo(CommandSourceStack source, GameTestInfo testInfo) {
      TestInstanceBlockEntity testInstanceBlockEntity = testInfo.getTestInstanceBlockEntity();
      testInstanceBlockEntity.resetTest(source::sendSystemMessage);
      return 1;
   }

   private static Stream<GameTestInfo> toGameTestInfos(CommandSourceStack source, RetryOptions retryOptions, TestPosFinder posFinder) {
      return posFinder.findTestPos().map(blockPos -> createGameTestInfo(blockPos, source, retryOptions)).flatMap(Optional::stream);
   }

   private static Stream<GameTestInfo> toGameTestInfo(CommandSourceStack source, RetryOptions retryOptions, TestInstanceFinder finder, int rotationSteps) {
      return finder.findTests()
         .filter(reference -> verifyStructureExists(source, reference.value().structure()))
         .map(
            reference -> new GameTestInfo(
               (Holder.Reference<GameTestInstance>)reference, StructureUtils.getRotationForRotationSteps(rotationSteps), source.getLevel(), retryOptions
            )
         );
   }

   private static Optional<GameTestInfo> createGameTestInfo(BlockPos pos, CommandSourceStack source, RetryOptions retryOptions) {
      ServerLevel serverLevel = source.getLevel();
      if (serverLevel.getBlockEntity(pos) instanceof TestInstanceBlockEntity testInstanceBlockEntity) {
         Optional<Holder.Reference<GameTestInstance>> optional = testInstanceBlockEntity.test()
            .flatMap(source.registryAccess().lookupOrThrow(Registries.TEST_INSTANCE)::get);
         if (optional.isEmpty()) {
            source.sendFailure(Component.translatable("commands.test.error.non_existant_test", testInstanceBlockEntity.getTestName()));
            return Optional.empty();
         } else {
            Holder.Reference<GameTestInstance> reference = optional.get();
            GameTestInfo gameTestInfo = new GameTestInfo(reference, testInstanceBlockEntity.getRotation(), serverLevel, retryOptions);
            gameTestInfo.setTestBlockPos(pos);
            return !verifyStructureExists(source, gameTestInfo.getStructure()) ? Optional.empty() : Optional.of(gameTestInfo);
         }
      } else {
         source.sendFailure(Component.translatable("commands.test.error.test_instance_not_found.position", pos.getX(), pos.getY(), pos.getZ()));
         return Optional.empty();
      }
   }

   private static int createNewStructure(CommandSourceStack source, Identifier id, int width, int height, int depth) throws CommandSyntaxException {
      if (width <= 48 && height <= 48 && depth <= 48) {
         ServerLevel serverLevel = source.getLevel();
         BlockPos blockPos = createTestPositionAround(source);
         TestInstanceBlockEntity testInstanceBlockEntity = StructureUtils.createNewEmptyTest(
            id, blockPos, new Vec3i(width, height, depth), Rotation.NONE, serverLevel
         );
         BlockPos blockPos2 = testInstanceBlockEntity.getStructurePos();
         BlockPos blockPos3 = blockPos2.offset(width - 1, 0, depth - 1);
         BlockPos.betweenClosedStream(blockPos2, blockPos3).forEach(blockPosx -> serverLevel.setBlockAndUpdate(blockPosx, Blocks.BEDROCK.defaultBlockState()));
         source.sendSuccess(() -> Component.translatable("commands.test.create.success", testInstanceBlockEntity.getTestName()), true);
         return 1;
      } else {
         throw TOO_LARGE.create(48);
      }
   }

   private static int showPos(CommandSourceStack source, String variableName) throws CommandSyntaxException {
      ServerPlayer serverPlayer = source.getPlayerOrException();
      BlockHitResult blockHitResult = (BlockHitResult)serverPlayer.pick(10.0, 1.0F, false);
      BlockPos blockPos = blockHitResult.getBlockPos();
      ServerLevel serverLevel = source.getLevel();
      Optional<BlockPos> optional = StructureUtils.findTestContainingPos(blockPos, 15, serverLevel);
      if (optional.isEmpty()) {
         optional = StructureUtils.findTestContainingPos(blockPos, 250, serverLevel);
      }

      if (optional.isEmpty()) {
         throw NO_TEST_CONTAINING.create(blockPos.getX(), blockPos.getY(), blockPos.getZ());
      } else if (serverLevel.getBlockEntity(optional.get()) instanceof TestInstanceBlockEntity testInstanceBlockEntity) {
         BlockPos var13 = testInstanceBlockEntity.getStructurePos();
         BlockPos blockPos3 = blockPos.subtract(var13);
         String string = blockPos3.getX() + ", " + blockPos3.getY() + ", " + blockPos3.getZ();
         String string2 = testInstanceBlockEntity.getTestName().getString();
         MutableComponent component = Component.translatable("commands.test.coordinates", blockPos3.getX(), blockPos3.getY(), blockPos3.getZ())
            .setStyle(
               Style.EMPTY
                  .withBold(true)
                  .withColor(ChatFormatting.GREEN)
                  .withHoverEvent(new HoverEvent.ShowText(Component.translatable("commands.test.coordinates.copy")))
                  .withClickEvent(new ClickEvent.CopyToClipboard("final BlockPos " + variableName + " = new BlockPos(" + string + ");"))
            );
         source.sendSuccess(() -> Component.translatable("commands.test.relative_position", string2, component), false);
         serverPlayer.connection.send(new ClientboundGameTestHighlightPosPacket(blockPos, blockPos3));
         return 1;
      } else {
         throw TEST_INSTANCE_COULD_NOT_BE_FOUND.create();
      }
   }

   private static int stopTests() {
      GameTestTicker.SINGLETON.clear();
      return 1;
   }

   public static int trackAndStartRunner(CommandSourceStack source, GameTestRunner testRunner) {
      testRunner.addListener(new TestCommand.TestBatchSummaryDisplayer(source));
      MultipleTestTracker multipleTestTracker = new MultipleTestTracker(testRunner.getTestInfos());
      multipleTestTracker.addListener(new TestCommand.TestSummaryDisplayer(source, multipleTestTracker));
      multipleTestTracker.addFailureListener(gameTestInfo -> FailedTestTracker.rememberFailedTest(gameTestInfo.getTestHolder()));
      testRunner.start();
      return 1;
   }

   private static int exportTestStructure(CommandSourceStack source, Holder<GameTestInstance> testInstance) {
      return !TestInstanceBlockEntity.export(source.getLevel(), testInstance.value().structure(), source::sendSystemMessage) ? 0 : 1;
   }

   private static boolean verifyStructureExists(CommandSourceStack source, Identifier structure) {
      if (source.getLevel().getStructureManager().get(structure).isEmpty()) {
         source.sendFailure(Component.translatable("commands.test.error.structure_not_found", Component.translationArg(structure)));
         return false;
      } else {
         return true;
      }
   }

   private static BlockPos createTestPositionAround(CommandSourceStack source) {
      BlockPos blockPos = BlockPos.containing(source.getPosition());
      int i = source.getLevel().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, blockPos).getY();
      return new BlockPos(blockPos.getX(), i, blockPos.getZ() + 3);
   }

   record TestBatchSummaryDisplayer(CommandSourceStack source) implements GameTestBatchListener {
      @Override
      public void testBatchStarting(GameTestBatch batch) {
         this.source.sendSuccess(() -> Component.translatable("commands.test.batch.starting", batch.environment().getRegisteredName(), batch.index()), true);
      }

      @Override
      public void testBatchFinished(GameTestBatch batch) {
      }
   }

   public record TestSummaryDisplayer(CommandSourceStack source, MultipleTestTracker tracker) implements GameTestListener {
      @Override
      public void testStructureLoaded(GameTestInfo testInfo) {
      }

      @Override
      public void testPassed(GameTestInfo test, GameTestRunner runner) {
         this.showTestSummaryIfAllDone();
      }

      @Override
      public void testFailed(GameTestInfo test, GameTestRunner runner) {
         this.showTestSummaryIfAllDone();
      }

      @Override
      public void testAddedForRerun(GameTestInfo oldTest, GameTestInfo newTest, GameTestRunner runner) {
         this.tracker.addTestToTrack(newTest);
      }

      private void showTestSummaryIfAllDone() {
         if (this.tracker.isDone()) {
            this.source.sendSuccess(() -> Component.translatable("commands.test.summary", this.tracker.getTotalCount()).withStyle(ChatFormatting.WHITE), true);
            if (this.tracker.hasFailedRequired()) {
               this.source.sendFailure(Component.translatable("commands.test.summary.failed", this.tracker.getFailedRequiredCount()));
            } else {
               this.source.sendSuccess(() -> Component.translatable("commands.test.summary.all_required_passed").withStyle(ChatFormatting.GREEN), true);
            }

            if (this.tracker.hasFailedOptional()) {
               this.source.sendSystemMessage(Component.translatable("commands.test.summary.optional_failed", this.tracker.getFailedOptionalCount()));
            }
         }
      }
   }
}
