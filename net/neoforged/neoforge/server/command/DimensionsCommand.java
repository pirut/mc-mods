package net.neoforged.neoforge.server.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;

class DimensionsCommand {
   static ArgumentBuilder<CommandSourceStack, ?> register() {
      return ((LiteralArgumentBuilder)Commands.literal("dimensions").requires(cs -> cs.hasPermission(0))).executes(ctx -> {
         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> CommandUtils.makeTranslatableWithFallback("commands.neoforge.dimensions.list"), true);
         Registry<DimensionType> reg = ((CommandSourceStack)ctx.getSource()).registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
         Map<ResourceLocation, List<Component>> types = new HashMap<>();

         for (ServerLevel dim : ((CommandSourceStack)ctx.getSource()).getServer().getAllLevels()) {
            types.computeIfAbsent(reg.getKey(dim.dimensionType()), k -> new ArrayList<>()).add(dim.getDescription());
         }

         types.keySet().stream().sorted().forEach(key -> ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> {
            MutableComponent component = Component.literal(key + ": ");
            List<Component> components = types.get(key);

            for (int i = 0; i < components.size(); i++) {
               component.append(components.get(i));
               if (i + 1 < components.size()) {
                  component.append(", ");
               }
            }

            return component;
         }, false));
         return 0;
      });
   }
}
