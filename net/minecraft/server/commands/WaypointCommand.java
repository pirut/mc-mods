package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.HexColorArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.WaypointArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointStyleAsset;
import net.minecraft.world.waypoints.WaypointStyleAssets;
import net.minecraft.world.waypoints.WaypointTransmitter;

public class WaypointCommand {
   public static void register(CommandDispatcher<CommandSourceStack> p_416242_, CommandBuildContext p_415741_) {
      p_416242_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("waypoint")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .then(Commands.literal("list").executes(p_415978_ -> listWaypoints((CommandSourceStack)p_415978_.getSource()))))
            .then(
               Commands.literal("modify")
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("waypoint", EntityArgument.entity())
                           .then(
                              ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("color")
                                       .then(
                                          Commands.argument("color", ColorArgument.color())
                                             .executes(
                                                p_416036_ -> setWaypointColor(
                                                   (CommandSourceStack)p_416036_.getSource(),
                                                   WaypointArgument.getWaypoint(p_416036_, "waypoint"),
                                                   ColorArgument.getColor(p_416036_, "color")
                                                )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("hex")
                                          .then(
                                             Commands.argument("color", HexColorArgument.hexColor())
                                                .executes(
                                                   p_415965_ -> setWaypointColor(
                                                      (CommandSourceStack)p_415965_.getSource(),
                                                      WaypointArgument.getWaypoint(p_415965_, "waypoint"),
                                                      HexColorArgument.getHexColor(p_415965_, "color")
                                                   )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("reset")
                                       .executes(
                                          p_416058_ -> resetWaypointColor(
                                             (CommandSourceStack)p_416058_.getSource(), WaypointArgument.getWaypoint(p_416058_, "waypoint")
                                          )
                                       )
                                 )
                           ))
                        .then(
                           ((LiteralArgumentBuilder)Commands.literal("style")
                                 .then(
                                    Commands.literal("reset")
                                       .executes(
                                          p_419433_ -> setWaypointStyle(
                                             (CommandSourceStack)p_419433_.getSource(),
                                             WaypointArgument.getWaypoint(p_419433_, "waypoint"),
                                             WaypointStyleAssets.DEFAULT
                                          )
                                       )
                                 ))
                              .then(
                                 Commands.literal("set")
                                    .then(
                                       Commands.argument("style", IdentifierArgument.id())
                                          .executes(
                                             p_466318_ -> setWaypointStyle(
                                                (CommandSourceStack)p_466318_.getSource(),
                                                WaypointArgument.getWaypoint(p_466318_, "waypoint"),
                                                ResourceKey.create(WaypointStyleAssets.ROOT_ID, IdentifierArgument.getId(p_466318_, "style"))
                                             )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int setWaypointStyle(CommandSourceStack p_416684_, WaypointTransmitter p_416166_, ResourceKey<WaypointStyleAsset> p_419544_) {
      mutateIcon(p_416684_, p_416166_, p_419435_ -> p_419435_.style = p_419544_);
      p_416684_.sendSuccess(() -> Component.translatable("commands.waypoint.modify.style"), false);
      return 0;
   }

   private static int setWaypointColor(CommandSourceStack p_416274_, WaypointTransmitter p_416001_, ChatFormatting p_415711_) {
      mutateIcon(p_416274_, p_416001_, p_416671_ -> p_416671_.color = Optional.of(p_415711_.getColor()));
      p_416274_.sendSuccess(() -> Component.translatable("commands.waypoint.modify.color", Component.literal(p_415711_.getName()).withStyle(p_415711_)), false);
      return 0;
   }

   private static int setWaypointColor(CommandSourceStack p_416623_, WaypointTransmitter p_416621_, Integer p_415844_) {
      mutateIcon(p_416623_, p_416621_, p_415935_ -> p_415935_.color = Optional.of(p_415844_));
      p_416623_.sendSuccess(
         () -> Component.translatable(
            "commands.waypoint.modify.color", Component.literal(HexFormat.of().withUpperCase().toHexDigits(ARGB.color(0, p_415844_), 6)).withColor(p_415844_)
         ),
         false
      );
      return 0;
   }

   private static int resetWaypointColor(CommandSourceStack p_415559_, WaypointTransmitter p_415615_) {
      mutateIcon(p_415559_, p_415615_, p_415983_ -> p_415983_.color = Optional.empty());
      p_415559_.sendSuccess(() -> Component.translatable("commands.waypoint.modify.color.reset"), false);
      return 0;
   }

   private static int listWaypoints(CommandSourceStack p_416666_) {
      ServerLevel serverlevel = p_416666_.getLevel();
      Set<WaypointTransmitter> set = serverlevel.getWaypointManager().transmitters();
      String s = serverlevel.dimension().identifier().toString();
      if (set.isEmpty()) {
         p_416666_.sendSuccess(() -> Component.translatable("commands.waypoint.list.empty", s), false);
         return 0;
      } else {
         Component component = ComponentUtils.formatList(
            set.stream()
               .map(
                  p_482390_ -> {
                     if (p_482390_ instanceof LivingEntity livingentity) {
                        BlockPos blockpos = livingentity.blockPosition();
                        return livingentity.getFeedbackDisplayName()
                           .copy()
                           .withStyle(
                              p_415705_ -> p_415705_.withClickEvent(
                                    new ClickEvent.SuggestCommand(
                                       "/execute in " + s + " run tp @s " + blockpos.getX() + " " + blockpos.getY() + " " + blockpos.getZ()
                                    )
                                 )
                                 .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip")))
                                 .withColor(p_482390_.waypointIcon().color.orElse(-1))
                           );
                     } else {
                        return Component.literal(p_482390_.toString());
                     }
                  }
               )
               .toList(),
            Function.identity()
         );
         p_416666_.sendSuccess(() -> Component.translatable("commands.waypoint.list.success", set.size(), s, component), false);
         return set.size();
      }
   }

   private static void mutateIcon(CommandSourceStack p_416169_, WaypointTransmitter p_416645_, Consumer<Waypoint.Icon> p_416385_) {
      ServerLevel serverlevel = p_416169_.getLevel();
      serverlevel.getWaypointManager().untrackWaypoint(p_416645_);
      p_416385_.accept(p_416645_.waypointIcon());
      serverlevel.getWaypointManager().trackWaypoint(p_416645_);
   }
}
