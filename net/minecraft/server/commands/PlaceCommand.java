package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Optional;
import net.minecraft.IdentifierException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.TemplateMirrorArgument;
import net.minecraft.commands.arguments.TemplateRotationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class PlaceCommand {
   private static final SimpleCommandExceptionType ERROR_FEATURE_FAILED = new SimpleCommandExceptionType(
      Component.translatable("commands.place.feature.failed")
   );
   private static final SimpleCommandExceptionType ERROR_JIGSAW_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.place.jigsaw.failed"));
   private static final SimpleCommandExceptionType ERROR_STRUCTURE_FAILED = new SimpleCommandExceptionType(
      Component.translatable("commands.place.structure.failed")
   );
   private static final DynamicCommandExceptionType ERROR_TEMPLATE_INVALID = new DynamicCommandExceptionType(
      object -> Component.translatableEscape("commands.place.template.invalid", object)
   );
   private static final SimpleCommandExceptionType ERROR_TEMPLATE_FAILED = new SimpleCommandExceptionType(
      Component.translatable("commands.place.template.failed")
   );
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_TEMPLATES = (commandContext, suggestionsBuilder) -> {
      StructureTemplateManager structureTemplateManager = ((CommandSourceStack)commandContext.getSource()).getLevel().getStructureManager();
      return SharedSuggestionProvider.suggestResource(structureTemplateManager.listTemplates(), suggestionsBuilder);
   };

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("place")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                     .then(
                        Commands.literal("feature")
                           .then(
                              ((RequiredArgumentBuilder)Commands.argument("feature", ResourceKeyArgument.key(Registries.CONFIGURED_FEATURE))
                                    .executes(
                                       commandContext -> placeFeature(
                                          (CommandSourceStack)commandContext.getSource(),
                                          ResourceKeyArgument.getConfiguredFeature(commandContext, "feature"),
                                          BlockPos.containing(((CommandSourceStack)commandContext.getSource()).getPosition())
                                       )
                                    ))
                                 .then(
                                    Commands.argument("pos", BlockPosArgument.blockPos())
                                       .executes(
                                          commandContext -> placeFeature(
                                             (CommandSourceStack)commandContext.getSource(),
                                             ResourceKeyArgument.getConfiguredFeature(commandContext, "feature"),
                                             BlockPosArgument.getLoadedBlockPos(commandContext, "pos")
                                          )
                                       )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("jigsaw")
                        .then(
                           Commands.argument("pool", ResourceKeyArgument.key(Registries.TEMPLATE_POOL))
                              .then(
                                 Commands.argument("target", IdentifierArgument.id())
                                    .then(
                                       ((RequiredArgumentBuilder)Commands.argument("max_depth", IntegerArgumentType.integer(1, 20))
                                             .executes(
                                                commandContext -> placeJigsaw(
                                                   (CommandSourceStack)commandContext.getSource(),
                                                   ResourceKeyArgument.getStructureTemplatePool(commandContext, "pool"),
                                                   IdentifierArgument.getId(commandContext, "target"),
                                                   IntegerArgumentType.getInteger(commandContext, "max_depth"),
                                                   BlockPos.containing(((CommandSourceStack)commandContext.getSource()).getPosition())
                                                )
                                             ))
                                          .then(
                                             Commands.argument("position", BlockPosArgument.blockPos())
                                                .executes(
                                                   commandContext -> placeJigsaw(
                                                      (CommandSourceStack)commandContext.getSource(),
                                                      ResourceKeyArgument.getStructureTemplatePool(commandContext, "pool"),
                                                      IdentifierArgument.getId(commandContext, "target"),
                                                      IntegerArgumentType.getInteger(commandContext, "max_depth"),
                                                      BlockPosArgument.getLoadedBlockPos(commandContext, "position")
                                                   )
                                                )
                                          )
                                    )
                              )
                        )
                  ))
               .then(
                  Commands.literal("structure")
                     .then(
                        ((RequiredArgumentBuilder)Commands.argument("structure", ResourceKeyArgument.key(Registries.STRUCTURE))
                              .executes(
                                 commandContext -> placeStructure(
                                    (CommandSourceStack)commandContext.getSource(),
                                    ResourceKeyArgument.getStructure(commandContext, "structure"),
                                    BlockPos.containing(((CommandSourceStack)commandContext.getSource()).getPosition())
                                 )
                              ))
                           .then(
                              Commands.argument("pos", BlockPosArgument.blockPos())
                                 .executes(
                                    commandContext -> placeStructure(
                                       (CommandSourceStack)commandContext.getSource(),
                                       ResourceKeyArgument.getStructure(commandContext, "structure"),
                                       BlockPosArgument.getLoadedBlockPos(commandContext, "pos")
                                    )
                                 )
                           )
                     )
               ))
            .then(
               Commands.literal("template")
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("template", IdentifierArgument.id())
                           .suggests(SUGGEST_TEMPLATES)
                           .executes(
                              commandContext -> placeTemplate(
                                 (CommandSourceStack)commandContext.getSource(),
                                 IdentifierArgument.getId(commandContext, "template"),
                                 BlockPos.containing(((CommandSourceStack)commandContext.getSource()).getPosition()),
                                 Rotation.NONE,
                                 Mirror.NONE,
                                 1.0F,
                                 0,
                                 false
                              )
                           ))
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("pos", BlockPosArgument.blockPos())
                                 .executes(
                                    commandContext -> placeTemplate(
                                       (CommandSourceStack)commandContext.getSource(),
                                       IdentifierArgument.getId(commandContext, "template"),
                                       BlockPosArgument.getLoadedBlockPos(commandContext, "pos"),
                                       Rotation.NONE,
                                       Mirror.NONE,
                                       1.0F,
                                       0,
                                       false
                                    )
                                 ))
                              .then(
                                 ((RequiredArgumentBuilder)Commands.argument("rotation", TemplateRotationArgument.templateRotation())
                                       .executes(
                                          commandContext -> placeTemplate(
                                             (CommandSourceStack)commandContext.getSource(),
                                             IdentifierArgument.getId(commandContext, "template"),
                                             BlockPosArgument.getLoadedBlockPos(commandContext, "pos"),
                                             TemplateRotationArgument.getRotation(commandContext, "rotation"),
                                             Mirror.NONE,
                                             1.0F,
                                             0,
                                             false
                                          )
                                       ))
                                    .then(
                                       ((RequiredArgumentBuilder)Commands.argument("mirror", TemplateMirrorArgument.templateMirror())
                                             .executes(
                                                commandContext -> placeTemplate(
                                                   (CommandSourceStack)commandContext.getSource(),
                                                   IdentifierArgument.getId(commandContext, "template"),
                                                   BlockPosArgument.getLoadedBlockPos(commandContext, "pos"),
                                                   TemplateRotationArgument.getRotation(commandContext, "rotation"),
                                                   TemplateMirrorArgument.getMirror(commandContext, "mirror"),
                                                   1.0F,
                                                   0,
                                                   false
                                                )
                                             ))
                                          .then(
                                             ((RequiredArgumentBuilder)Commands.argument("integrity", FloatArgumentType.floatArg(0.0F, 1.0F))
                                                   .executes(
                                                      commandContext -> placeTemplate(
                                                         (CommandSourceStack)commandContext.getSource(),
                                                         IdentifierArgument.getId(commandContext, "template"),
                                                         BlockPosArgument.getLoadedBlockPos(commandContext, "pos"),
                                                         TemplateRotationArgument.getRotation(commandContext, "rotation"),
                                                         TemplateMirrorArgument.getMirror(commandContext, "mirror"),
                                                         FloatArgumentType.getFloat(commandContext, "integrity"),
                                                         0,
                                                         false
                                                      )
                                                   ))
                                                .then(
                                                   ((RequiredArgumentBuilder)Commands.argument("seed", IntegerArgumentType.integer())
                                                         .executes(
                                                            commandContext -> placeTemplate(
                                                               (CommandSourceStack)commandContext.getSource(),
                                                               IdentifierArgument.getId(commandContext, "template"),
                                                               BlockPosArgument.getLoadedBlockPos(commandContext, "pos"),
                                                               TemplateRotationArgument.getRotation(commandContext, "rotation"),
                                                               TemplateMirrorArgument.getMirror(commandContext, "mirror"),
                                                               FloatArgumentType.getFloat(commandContext, "integrity"),
                                                               IntegerArgumentType.getInteger(commandContext, "seed"),
                                                               false
                                                            )
                                                         ))
                                                      .then(
                                                         Commands.literal("strict")
                                                            .executes(
                                                               commandContext -> placeTemplate(
                                                                  (CommandSourceStack)commandContext.getSource(),
                                                                  IdentifierArgument.getId(commandContext, "template"),
                                                                  BlockPosArgument.getLoadedBlockPos(commandContext, "pos"),
                                                                  TemplateRotationArgument.getRotation(commandContext, "rotation"),
                                                                  TemplateMirrorArgument.getMirror(commandContext, "mirror"),
                                                                  FloatArgumentType.getFloat(commandContext, "integrity"),
                                                                  IntegerArgumentType.getInteger(commandContext, "seed"),
                                                                  true
                                                               )
                                                            )
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

   public static int placeFeature(CommandSourceStack source, Holder.Reference<ConfiguredFeature<?, ?>> feature, BlockPos pos) throws CommandSyntaxException {
      ServerLevel serverLevel = source.getLevel();
      ConfiguredFeature<?, ?> configuredFeature = feature.value();
      ChunkPos chunkPos = new ChunkPos(pos);
      checkLoaded(serverLevel, new ChunkPos(chunkPos.x - 1, chunkPos.z - 1), new ChunkPos(chunkPos.x + 1, chunkPos.z + 1));
      if (!configuredFeature.place(serverLevel, serverLevel.getChunkSource().getGenerator(), serverLevel.getRandom(), pos)) {
         throw ERROR_FEATURE_FAILED.create();
      } else {
         String string = feature.key().identifier().toString();
         source.sendSuccess(() -> Component.translatable("commands.place.feature.success", string, pos.getX(), pos.getY(), pos.getZ()), true);
         return 1;
      }
   }

   public static int placeJigsaw(CommandSourceStack source, Holder<StructureTemplatePool> templatePool, Identifier target, int maxDepth, BlockPos pos) throws CommandSyntaxException {
      ServerLevel serverLevel = source.getLevel();
      ChunkPos chunkPos = new ChunkPos(pos);
      checkLoaded(serverLevel, chunkPos, chunkPos);
      if (!JigsawPlacement.generateJigsaw(serverLevel, templatePool, target, maxDepth, pos, false)) {
         throw ERROR_JIGSAW_FAILED.create();
      } else {
         source.sendSuccess(() -> Component.translatable("commands.place.jigsaw.success", pos.getX(), pos.getY(), pos.getZ()), true);
         return 1;
      }
   }

   public static int placeStructure(CommandSourceStack source, Holder.Reference<Structure> structure, BlockPos pos) throws CommandSyntaxException {
      ServerLevel serverLevel = source.getLevel();
      Structure structure2 = structure.value();
      ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
      StructureStart structureStart = structure2.generate(
         structure,
         serverLevel.dimension(),
         source.registryAccess(),
         chunkGenerator,
         chunkGenerator.getBiomeSource(),
         serverLevel.getChunkSource().randomState(),
         serverLevel.getStructureManager(),
         serverLevel.getSeed(),
         new ChunkPos(pos),
         0,
         serverLevel,
         holder -> true
      );
      if (!structureStart.isValid()) {
         throw ERROR_STRUCTURE_FAILED.create();
      } else {
         BoundingBox boundingBox = structureStart.getBoundingBox();
         ChunkPos chunkPos = new ChunkPos(SectionPos.blockToSectionCoord(boundingBox.minX()), SectionPos.blockToSectionCoord(boundingBox.minZ()));
         ChunkPos chunkPos2 = new ChunkPos(SectionPos.blockToSectionCoord(boundingBox.maxX()), SectionPos.blockToSectionCoord(boundingBox.maxZ()));
         checkLoaded(serverLevel, chunkPos, chunkPos2);
         ChunkPos.rangeClosed(chunkPos, chunkPos2)
            .forEach(
               chunkPosx -> structureStart.placeInChunk(
                  serverLevel,
                  serverLevel.structureManager(),
                  chunkGenerator,
                  serverLevel.getRandom(),
                  new BoundingBox(
                     chunkPosx.getMinBlockX(),
                     serverLevel.getMinY(),
                     chunkPosx.getMinBlockZ(),
                     chunkPosx.getMaxBlockX(),
                     serverLevel.getMaxY() + 1,
                     chunkPosx.getMaxBlockZ()
                  ),
                  chunkPosx
               )
            );
         String string = structure.key().identifier().toString();
         source.sendSuccess(() -> Component.translatable("commands.place.structure.success", string, pos.getX(), pos.getY(), pos.getZ()), true);
         return 1;
      }
   }

   public static int placeTemplate(
      CommandSourceStack source, Identifier template, BlockPos pos, Rotation rotation, Mirror mirror, float integrity, int seed, boolean strict
   ) throws CommandSyntaxException {
      ServerLevel serverLevel = source.getLevel();
      StructureTemplateManager structureTemplateManager = serverLevel.getStructureManager();

      Optional<StructureTemplate> optional;
      try {
         optional = structureTemplateManager.get(template);
      } catch (IdentifierException var14) {
         throw ERROR_TEMPLATE_INVALID.create(template);
      }

      if (optional.isEmpty()) {
         throw ERROR_TEMPLATE_INVALID.create(template);
      } else {
         StructureTemplate structureTemplate = optional.get();
         checkLoaded(serverLevel, new ChunkPos(pos), new ChunkPos(pos.offset(structureTemplate.getSize())));
         StructurePlaceSettings structurePlaceSettings = new StructurePlaceSettings().setMirror(mirror).setRotation(rotation).setKnownShape(strict);
         if (integrity < 1.0F) {
            structurePlaceSettings.clearProcessors().addProcessor(new BlockRotProcessor(integrity)).setRandom(StructureBlockEntity.createRandom(seed));
         }

         boolean bl = structureTemplate.placeInWorld(
            serverLevel, pos, pos, structurePlaceSettings, StructureBlockEntity.createRandom(seed), 2 | (strict ? 816 : 0)
         );
         if (!bl) {
            throw ERROR_TEMPLATE_FAILED.create();
         } else {
            source.sendSuccess(
               () -> Component.translatable("commands.place.template.success", Component.translationArg(template), pos.getX(), pos.getY(), pos.getZ()), true
            );
            return 1;
         }
      }
   }

   private static void checkLoaded(ServerLevel level, ChunkPos start, ChunkPos end) throws CommandSyntaxException {
      if (ChunkPos.rangeClosed(start, end).filter(chunkPos -> !level.isLoaded(chunkPos.getWorldPosition())).findAny().isPresent()) {
         throw BlockPosArgument.ERROR_NOT_LOADED.create();
      }
   }
}
