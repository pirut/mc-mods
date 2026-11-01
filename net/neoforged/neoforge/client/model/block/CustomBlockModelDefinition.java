package net.neoforged.neoforge.client.model.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public interface CustomBlockModelDefinition {
   Map<BlockState, BlockStateModel.UnbakedRoot> instantiate(StateDefinition<Block, BlockState> var1, Supplier<String> var2);

   MapCodec<? extends CustomBlockModelDefinition> codec();
}
