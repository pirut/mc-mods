package net.minecraft.server.commands;

import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.entity.Entity;

public class TagCommand {
   private static final SimpleCommandExceptionType ERROR_ADD_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.tag.add.failed"));
   private static final SimpleCommandExceptionType ERROR_REMOVE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.tag.remove.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> p_138837_) {
      p_138837_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("tag").requires(p_138844_ -> p_138844_.hasPermission(2)))
            .then(
               ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.entities())
                        .then(
                           Commands.literal("add")
                              .then(
                                 Commands.argument("name", StringArgumentType.word())
                                    .executes(
                                       p_138861_ -> addTag(
                                          (CommandSourceStack)p_138861_.getSource(),
                                          EntityArgument.getEntities(p_138861_, "targets"),
                                          StringArgumentType.getString(p_138861_, "name")
                                       )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("remove")
                           .then(
                              Commands.argument("name", StringArgumentType.word())
                                 .suggests(
                                    (p_138841_, p_138842_) -> SharedSuggestionProvider.suggest(
                                       getTags(EntityArgument.getEntities(p_138841_, "targets")), p_138842_
                                    )
                                 )
                                 .executes(
                                    p_138855_ -> removeTag(
                                       (CommandSourceStack)p_138855_.getSource(),
                                       EntityArgument.getEntities(p_138855_, "targets"),
                                       StringArgumentType.getString(p_138855_, "name")
                                    )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("list")
                        .executes(p_138839_ -> listTags((CommandSourceStack)p_138839_.getSource(), EntityArgument.getEntities(p_138839_, "targets")))
                  )
            )
      );
   }

   private static Collection<String> getTags(Collection<? extends Entity> p_138853_) {
      Set<String> $$1 = Sets.newHashSet();

      for (Entity $$2 : p_138853_) {
         $$1.addAll($$2.getTags());
      }

      return $$1;
   }

   private static int addTag(CommandSourceStack p_138849_, Collection<? extends Entity> p_138850_, String p_138851_) throws CommandSyntaxException {
      int $$3 = 0;

      for (Entity $$4 : p_138850_) {
         if ($$4.addTag(p_138851_)) {
            $$3++;
         }
      }

      if ($$3 == 0) {
         throw ERROR_ADD_FAILED.create();
      } else {
         if (p_138850_.size() == 1) {
            p_138849_.sendSuccess(
               () -> Component.translatable("commands.tag.add.success.single", p_138851_, p_138850_.iterator().next().getDisplayName()), true
            );
         } else {
            p_138849_.sendSuccess(() -> Component.translatable("commands.tag.add.success.multiple", p_138851_, p_138850_.size()), true);
         }

         return $$3;
      }
   }

   private static int removeTag(CommandSourceStack p_138857_, Collection<? extends Entity> p_138858_, String p_138859_) throws CommandSyntaxException {
      int $$3 = 0;

      for (Entity $$4 : p_138858_) {
         if ($$4.removeTag(p_138859_)) {
            $$3++;
         }
      }

      if ($$3 == 0) {
         throw ERROR_REMOVE_FAILED.create();
      } else {
         if (p_138858_.size() == 1) {
            p_138857_.sendSuccess(
               () -> Component.translatable("commands.tag.remove.success.single", p_138859_, p_138858_.iterator().next().getDisplayName()), true
            );
         } else {
            p_138857_.sendSuccess(() -> Component.translatable("commands.tag.remove.success.multiple", p_138859_, p_138858_.size()), true);
         }

         return $$3;
      }
   }

   private static int listTags(CommandSourceStack p_138846_, Collection<? extends Entity> p_138847_) {
      Set<String> $$2 = Sets.newHashSet();

      for (Entity $$3 : p_138847_) {
         $$2.addAll($$3.getTags());
      }

      if (p_138847_.size() == 1) {
         Entity $$4 = p_138847_.iterator().next();
         if ($$2.isEmpty()) {
            p_138846_.sendSuccess(() -> Component.translatable("commands.tag.list.single.empty", $$4.getDisplayName()), false);
         } else {
            p_138846_.sendSuccess(
               () -> Component.translatable("commands.tag.list.single.success", $$4.getDisplayName(), $$2.size(), ComponentUtils.formatList($$2)), false
            );
         }
      } else if ($$2.isEmpty()) {
         p_138846_.sendSuccess(() -> Component.translatable("commands.tag.list.multiple.empty", p_138847_.size()), false);
      } else {
         p_138846_.sendSuccess(
            () -> Component.translatable("commands.tag.list.multiple.success", p_138847_.size(), $$2.size(), ComponentUtils.formatList($$2)), false
         );
      }

      return $$2.size();
   }
}
