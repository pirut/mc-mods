package net.neoforged.neoforge.client.event;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.client.model.block.CustomBlockModelDefinition;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterBlockStateModels extends Event implements IModBusEvent {
   private final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends CustomUnbakedBlockStateModel>> modelIdMapper;
   private final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends CustomBlockModelDefinition>> defintionIdMapper;

   @Internal
   public RegisterBlockStateModels(
      ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends CustomUnbakedBlockStateModel>> modelIdMapper,
      ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends CustomBlockModelDefinition>> defintionIdMapper
   ) {
      this.modelIdMapper = modelIdMapper;
      this.defintionIdMapper = defintionIdMapper;
   }

   public void registerModel(Identifier location, MapCodec<? extends CustomUnbakedBlockStateModel> codec) {
      this.modelIdMapper.put(location, codec);
   }

   public void registerDefinition(Identifier location, MapCodec<? extends CustomBlockModelDefinition> codec) {
      this.defintionIdMapper.put(location, codec);
   }
}
