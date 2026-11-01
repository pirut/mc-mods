package net.minecraft.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.world.waypoints.WaypointTransmitter;

public class WaypointArgument {
   public static final SimpleCommandExceptionType ERROR_NOT_A_WAYPOINT = new SimpleCommandExceptionType(Component.translatable("argument.waypoint.invalid"));

   public static WaypointTransmitter getWaypoint(CommandContext<CommandSourceStack> p_415558_, String p_416309_) throws CommandSyntaxException {
      if (((EntitySelector)p_415558_.getArgument(p_416309_, EntitySelector.class)).findSingleEntity((CommandSourceStack)p_415558_.getSource()) instanceof WaypointTransmitter waypointtransmitter
         )
       {
         return waypointtransmitter;
      } else {
         throw ERROR_NOT_A_WAYPOINT.create();
      }
   }
}
