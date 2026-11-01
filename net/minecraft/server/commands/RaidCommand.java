package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.phys.Vec3;

public class RaidCommand {
   public static void register(CommandDispatcher<CommandSourceStack> p_180469_, CommandBuildContext p_323666_) {
      p_180469_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                    "raid"
                                 )
                                 .requires(p_180498_ -> p_180498_.hasPermission(3)))
                              .then(
                                 Commands.literal("start")
                                    .then(
                                       Commands.argument("omenlvl", IntegerArgumentType.integer(0))
                                          .executes(
                                             p_180502_ -> start((CommandSourceStack)p_180502_.getSource(), IntegerArgumentType.getInteger(p_180502_, "omenlvl"))
                                          )
                                    )
                              ))
                           .then(Commands.literal("stop").executes(p_180500_ -> stop((CommandSourceStack)p_180500_.getSource()))))
                        .then(Commands.literal("check").executes(p_180496_ -> check((CommandSourceStack)p_180496_.getSource()))))
                     .then(
                        Commands.literal("sound")
                           .then(
                              Commands.argument("type", ComponentArgument.textComponent(p_323666_))
                                 .executes(p_180492_ -> playSound((CommandSourceStack)p_180492_.getSource(), ComponentArgument.getComponent(p_180492_, "type")))
                           )
                     ))
                  .then(Commands.literal("spawnleader").executes(p_180488_ -> spawnLeader((CommandSourceStack)p_180488_.getSource()))))
               .then(
                  Commands.literal("setomen")
                     .then(
                        Commands.argument("level", IntegerArgumentType.integer(0))
                           .executes(
                              p_337538_ -> setRaidOmenLevel((CommandSourceStack)p_337538_.getSource(), IntegerArgumentType.getInteger(p_337538_, "level"))
                           )
                     )
               ))
            .then(Commands.literal("glow").executes(p_180471_ -> glow((CommandSourceStack)p_180471_.getSource())))
      );
   }

   private static int glow(CommandSourceStack p_180473_) throws CommandSyntaxException {
      Raid $$1 = getRaid(p_180473_.getPlayerOrException());
      if ($$1 != null) {
         for (Raider $$3 : $$1.getAllRaiders()) {
            $$3.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1000, 1));
         }
      }

      return 1;
   }

   private static int setRaidOmenLevel(CommandSourceStack p_180475_, int p_180476_) throws CommandSyntaxException {
      Raid $$2 = getRaid(p_180475_.getPlayerOrException());
      if ($$2 != null) {
         int $$3 = $$2.getMaxRaidOmenLevel();
         if (p_180476_ > $$3) {
            p_180475_.sendFailure(Component.literal("Sorry, the max raid omen level you can set is " + $$3));
         } else {
            int $$4 = $$2.getRaidOmenLevel();
            $$2.setRaidOmenLevel(p_180476_);
            p_180475_.sendSuccess(() -> Component.literal("Changed village's raid omen level from " + $$4 + " to " + p_180476_), false);
         }
      } else {
         p_180475_.sendFailure(Component.literal("No raid found here"));
      }

      return 1;
   }

   private static int spawnLeader(CommandSourceStack p_180483_) {
      p_180483_.sendSuccess(() -> Component.literal("Spawned a raid captain"), false);
      Raider $$1 = EntityType.PILLAGER.create(p_180483_.getLevel());
      if ($$1 == null) {
         p_180483_.sendFailure(Component.literal("Pillager failed to spawn"));
         return 0;
      } else {
         $$1.setPatrolLeader(true);
         $$1.setItemSlot(EquipmentSlot.HEAD, Raid.getLeaderBannerInstance(p_180483_.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)));
         $$1.setPos(p_180483_.getPosition().x, p_180483_.getPosition().y, p_180483_.getPosition().z);
         $$1.finalizeSpawn(
            p_180483_.getLevel(), p_180483_.getLevel().getCurrentDifficultyAt(BlockPos.containing(p_180483_.getPosition())), MobSpawnType.COMMAND, null
         );
         p_180483_.getLevel().addFreshEntityWithPassengers($$1);
         return 1;
      }
   }

   private static int playSound(CommandSourceStack p_180478_, @Nullable Component p_180479_) {
      if (p_180479_ != null && p_180479_.getString().equals("local")) {
         ServerLevel $$2 = p_180478_.getLevel();
         Vec3 $$3 = p_180478_.getPosition().add(5.0, 0.0, 0.0);
         $$2.playSeededSound(null, $$3.x, $$3.y, $$3.z, SoundEvents.RAID_HORN, SoundSource.NEUTRAL, 2.0F, 1.0F, $$2.random.nextLong());
      }

      return 1;
   }

   private static int start(CommandSourceStack p_180485_, int p_180486_) throws CommandSyntaxException {
      ServerPlayer $$2 = p_180485_.getPlayerOrException();
      BlockPos $$3 = $$2.blockPosition();
      if ($$2.serverLevel().isRaided($$3)) {
         p_180485_.sendFailure(Component.literal("Raid already started close by"));
         return -1;
      } else {
         Raids $$4 = $$2.serverLevel().getRaids();
         Raid $$5 = $$4.createOrExtendRaid($$2, $$2.blockPosition());
         if ($$5 != null) {
            $$5.setRaidOmenLevel(p_180486_);
            $$4.setDirty();
            p_180485_.sendSuccess(() -> Component.literal("Created a raid in your local village"), false);
         } else {
            p_180485_.sendFailure(Component.literal("Failed to create a raid in your local village"));
         }

         return 1;
      }
   }

   private static int stop(CommandSourceStack p_180490_) throws CommandSyntaxException {
      ServerPlayer $$1 = p_180490_.getPlayerOrException();
      BlockPos $$2 = $$1.blockPosition();
      Raid $$3 = $$1.serverLevel().getRaidAt($$2);
      if ($$3 != null) {
         $$3.stop();
         p_180490_.sendSuccess(() -> Component.literal("Stopped raid"), false);
         return 1;
      } else {
         p_180490_.sendFailure(Component.literal("No raid here"));
         return -1;
      }
   }

   private static int check(CommandSourceStack p_180494_) throws CommandSyntaxException {
      Raid $$1 = getRaid(p_180494_.getPlayerOrException());
      if ($$1 != null) {
         StringBuilder $$2 = new StringBuilder();
         $$2.append("Found a started raid! ");
         p_180494_.sendSuccess(() -> Component.literal($$2.toString()), false);
         StringBuilder $$3 = new StringBuilder();
         $$3.append("Num groups spawned: ");
         $$3.append($$1.getGroupsSpawned());
         $$3.append(" Raid omen level: ");
         $$3.append($$1.getRaidOmenLevel());
         $$3.append(" Num mobs: ");
         $$3.append($$1.getTotalRaidersAlive());
         $$3.append(" Raid health: ");
         $$3.append($$1.getHealthOfLivingRaiders());
         $$3.append(" / ");
         $$3.append($$1.getTotalHealth());
         p_180494_.sendSuccess(() -> Component.literal($$3.toString()), false);
         return 1;
      } else {
         p_180494_.sendFailure(Component.literal("Found no started raids"));
         return 0;
      }
   }

   @Nullable
   private static Raid getRaid(ServerPlayer p_180467_) {
      return p_180467_.serverLevel().getRaidAt(p_180467_.blockPosition());
   }
}
