package net.minecraft.server.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.ResolvableProfile;

public class FetchProfileCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("fetchprofile")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .then(
                  Commands.literal("name")
                     .then(
                        Commands.argument("name", StringArgumentType.greedyString())
                           .executes(
                              commandContext -> resolveName(
                                 (CommandSourceStack)commandContext.getSource(), StringArgumentType.getString(commandContext, "name")
                              )
                           )
                     )
               ))
            .then(
               Commands.literal("id")
                  .then(
                     Commands.argument("id", UuidArgument.uuid())
                        .executes(commandContext -> resolveId((CommandSourceStack)commandContext.getSource(), UuidArgument.getUuid(commandContext, "id")))
                  )
            )
      );
   }

   private static void reportResolvedProfile(CommandSourceStack source, GameProfile profile, String translationKey, Component id) {
      ResolvableProfile resolvableProfile = ResolvableProfile.createResolved(profile);
      ResolvableProfile.CODEC
         .encodeStart(NbtOps.INSTANCE, resolvableProfile)
         .ifSuccess(
            tag -> {
               String string2 = tag.toString();
               MutableComponent mutableComponent = Component.object(new PlayerSprite(resolvableProfile, true));
               ComponentSerialization.CODEC
                  .encodeStart(NbtOps.INSTANCE, mutableComponent)
                  .ifSuccess(
                     tagx -> {
                        String string3 = tagx.toString();
                        source.sendSuccess(
                           () -> {
                              Component component2 = ComponentUtils.formatList(
                                 List.of(
                                    Component.translatable("commands.fetchprofile.copy_component")
                                       .withStyle(style -> style.withClickEvent(new ClickEvent.CopyToClipboard(string2))),
                                    Component.translatable("commands.fetchprofile.give_item")
                                       .withStyle(
                                          style -> style.withClickEvent(new ClickEvent.RunCommand("give @s minecraft:player_head[profile=" + string2 + "]"))
                                       ),
                                    Component.translatable("commands.fetchprofile.summon_mannequin")
                                       .withStyle(
                                          style -> style.withClickEvent(new ClickEvent.RunCommand("summon minecraft:mannequin ~ ~ ~ {profile:" + string2 + "}"))
                                       ),
                                    Component.translatable("commands.fetchprofile.copy_text", mutableComponent.withStyle(ChatFormatting.WHITE))
                                       .withStyle(style -> style.withClickEvent(new ClickEvent.CopyToClipboard(string3)))
                                 ),
                                 CommonComponents.SPACE,
                                 mutableComponentxxx -> ComponentUtils.wrapInSquareBrackets(mutableComponentxxx.withStyle(ChatFormatting.GREEN))
                              );
                              return Component.translatable(translationKey, id, component2);
                           },
                           false
                        );
                     }
                  )
                  .ifError(error -> source.sendFailure(Component.translatable("commands.fetchprofile.failed_to_serialize", error.message())));
            }
         )
         .ifError(error -> source.sendFailure(Component.translatable("commands.fetchprofile.failed_to_serialize", error.message())));
   }

   private static int resolveName(CommandSourceStack source, String name) {
      MinecraftServer minecraftServer = source.getServer();
      ProfileResolver profileResolver = minecraftServer.services().profileResolver();
      Util.nonCriticalIoPool()
         .execute(
            () -> {
               Component component = Component.literal(name);
               Optional<GameProfile> optional = profileResolver.fetchByName(name);
               minecraftServer.execute(
                  () -> optional.ifPresentOrElse(
                     gameProfile -> reportResolvedProfile(source, gameProfile, "commands.fetchprofile.name.success", component),
                     () -> source.sendFailure(Component.translatable("commands.fetchprofile.name.failure", component))
                  )
               );
            }
         );
      return 1;
   }

   private static int resolveId(CommandSourceStack source, UUID id) {
      MinecraftServer minecraftServer = source.getServer();
      ProfileResolver profileResolver = minecraftServer.services().profileResolver();
      Util.nonCriticalIoPool()
         .execute(
            () -> {
               Component component = Component.translationArg(id);
               Optional<GameProfile> optional = profileResolver.fetchById(id);
               minecraftServer.execute(
                  () -> optional.ifPresentOrElse(
                     gameProfile -> reportResolvedProfile(source, gameProfile, "commands.fetchprofile.id.success", component),
                     () -> source.sendFailure(Component.translatable("commands.fetchprofile.id.failure", component))
                  )
               );
            }
         );
      return 1;
   }
}
