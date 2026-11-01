package net.minecraft.server.dialog.action;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

public class ActionTypes {
   public static MapCodec<? extends Action> bootstrap(Registry<MapCodec<? extends Action>> p_428527_) {
      StaticAction.WRAPPED_CODECS
         .forEach((p_466323_, p_466324_) -> Registry.register(p_428527_, Identifier.withDefaultNamespace(p_466323_.getSerializedName()), p_466324_));
      Registry.register(p_428527_, Identifier.withDefaultNamespace("dynamic/run_command"), CommandTemplate.MAP_CODEC);
      return Registry.register(p_428527_, Identifier.withDefaultNamespace("dynamic/custom"), CustomAll.MAP_CODEC);
   }
}
