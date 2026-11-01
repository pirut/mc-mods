package net.neoforged.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;

public interface CustomBlockOutlineRenderer {
   boolean render(BlockOutlineRenderState var1, MultiBufferSource.BufferSource var2, PoseStack var3, boolean var4, LevelRenderState var5);
}
