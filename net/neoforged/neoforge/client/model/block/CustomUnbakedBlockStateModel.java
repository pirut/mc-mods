package net.neoforged.neoforge.client.model.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.block.model.BlockStateModel;

public interface CustomUnbakedBlockStateModel extends BlockStateModel.Unbaked {
   MapCodec<? extends CustomUnbakedBlockStateModel> codec();
}
