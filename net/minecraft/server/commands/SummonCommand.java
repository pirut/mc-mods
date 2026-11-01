package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SummonCommand {
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.summon.failed"));
   private static final SimpleCommandExceptionType ERROR_DUPLICATE_UUID = new SimpleCommandExceptionType(Component.translatable("commands.summon.failed.uuid"));
   private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(Component.translatable("commands.summon.invalidPosition"));

   public static void register(CommandDispatcher<CommandSourceStack> p_250343_, CommandBuildContext p_250122_) {
      p_250343_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("summon").requires(p_138819_ -> p_138819_.hasPermission(2)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("entity", ResourceArgument.resource(p_250122_, Registries.ENTITY_TYPE))
                     .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                     .executes(
                        p_248175_ -> spawnEntity(
                           (CommandSourceStack)p_248175_.getSource(),
                           ResourceArgument.getSummonableEntityType(p_248175_, "entity"),
                           ((CommandSourceStack)p_248175_.getSource()).getPosition(),
                           new CompoundTag(),
                           true
                        )
                     ))
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("pos", Vec3Argument.vec3())
                           .executes(
                              p_248173_ -> spawnEntity(
                                 (CommandSourceStack)p_248173_.getSource(),
                                 ResourceArgument.getSummonableEntityType(p_248173_, "entity"),
                                 Vec3Argument.getVec3(p_248173_, "pos"),
                                 new CompoundTag(),
                                 true
                              )
                           ))
                        .then(
                           Commands.argument("nbt", CompoundTagArgument.compoundTag())
                              .executes(
                                 p_248174_ -> spawnEntity(
                                    (CommandSourceStack)p_248174_.getSource(),
                                    ResourceArgument.getSummonableEntityType(p_248174_, "entity"),
                                    Vec3Argument.getVec3(p_248174_, "pos"),
                                    CompoundTagArgument.getCompoundTag(p_248174_, "nbt"),
                                    false
                                 )
                              )
                        )
                  )
            )
      );
   }

   public static Entity createEntity(
      CommandSourceStack p_270582_, Holder.Reference<EntityType<?>> p_270277_, Vec3 p_270366_, CompoundTag p_270197_, boolean p_270947_
   ) throws CommandSyntaxException {
      BlockPos $$5 = BlockPos.containing(p_270366_);
      if (!Level.isInSpawnableBounds($$5)) {
         throw INVALID_POSITION.create();
      } else {
         CompoundTag $$6 = p_270197_.copy();
         $$6.putString("id", p_270277_.key().location().toString());
         ServerLevel $$7 = p_270582_.getLevel();
         Entity $$8 = EntityType.loadEntityRecursive($$6, $$7, p_138828_ -> {
            p_138828_.moveTo(p_270366_.x, p_270366_.y, p_270366_.z, p_138828_.getYRot(), p_138828_.getXRot());
            return p_138828_;
         });
         if ($$8 == null) {
            throw ERROR_FAILED.create();
         } else {
            if (p_270947_ && $$8 instanceof Mob) {
               ((Mob)$$8).finalizeSpawn(p_270582_.getLevel(), p_270582_.getLevel().getCurrentDifficultyAt($$8.blockPosition()), MobSpawnType.COMMAND, null);
            }

            if (!$$7.tryAddFreshEntityWithPassengers($$8)) {
               throw ERROR_DUPLICATE_UUID.create();
            } else {
               return $$8;
            }
         }
      }
   }

   private static int spawnEntity(
      CommandSourceStack p_249752_, Holder.Reference<EntityType<?>> p_251948_, Vec3 p_251429_, CompoundTag p_250568_, boolean p_250229_
   ) throws CommandSyntaxException {
      Entity $$5 = createEntity(p_249752_, p_251948_, p_251429_, p_250568_, p_250229_);
      p_249752_.sendSuccess(() -> Component.translatable("commands.summon.success", $$5.getDisplayName()), true);
      return 1;
   }
}
