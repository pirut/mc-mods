package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SectionCompiler {
   private final BlockRenderDispatcher blockRenderer;
   private final BlockEntityRenderDispatcher blockEntityRenderer;

   public SectionCompiler(BlockRenderDispatcher p_350768_, BlockEntityRenderDispatcher p_350715_) {
      this.blockRenderer = p_350768_;
      this.blockEntityRenderer = p_350715_;
   }

   public SectionCompiler.Results compile(SectionPos p_350790_, RenderSectionRegion p_427336_, VertexSorting p_350635_, SectionBufferBuilderPack p_350612_) {
      return this.compile(p_350790_, p_427336_, p_350635_, p_350612_, List.of());
   }

   public SectionCompiler.Results compile(
      SectionPos p_350790_,
      RenderSectionRegion p_427336_,
      VertexSorting p_350635_,
      SectionBufferBuilderPack p_350612_,
      List<AddSectionGeometryEvent.AdditionalSectionRenderer> additionalRenderers
   ) {
      SectionCompiler.Results sectioncompiler$results = new SectionCompiler.Results();
      BlockPos blockpos = p_350790_.origin();
      BlockPos blockpos1 = blockpos.offset(15, 15, 15);
      VisGraph visgraph = new VisGraph();
      PoseStack posestack = new PoseStack();
      ModelBlockRenderer.enableCaching();
      Map<ChunkSectionLayer, BufferBuilder> map = new EnumMap<>(ChunkSectionLayer.class);
      RandomSource randomsource = new SingleThreadedRandomSource(RandomSupport.generateUniqueSeed());
      List<BlockModelPart> list = new ObjectArrayList();
      Function<ChunkSectionLayer, VertexConsumer> bufferLookup = renderType -> this.getOrBeginLayer(map, p_350612_, renderType);

      for (BlockPos blockpos2 : BlockPos.betweenClosed(blockpos, blockpos1)) {
         BlockState blockstate = p_427336_.getBlockState(blockpos2);
         if (blockstate.isSolidRender()) {
            visgraph.setOpaque(blockpos2);
         }

         if (blockstate.hasBlockEntity()) {
            BlockEntity blockentity = p_427336_.getBlockEntity(blockpos2);
            if (blockentity != null) {
               this.handleBlockEntity(sectioncompiler$results, blockentity);
            }
         }

         FluidState fluidstate = blockstate.getFluidState();
         if (!fluidstate.isEmpty()) {
            ChunkSectionLayer chunksectionlayer = ItemBlockRenderTypes.getRenderLayer(fluidstate);
            BufferBuilder bufferbuilder = this.getOrBeginLayer(map, p_350612_, chunksectionlayer);
            this.blockRenderer.renderLiquid(blockpos2, p_427336_, bufferbuilder, blockstate, fluidstate);
         }

         if (blockstate.getRenderShape() == RenderShape.MODEL) {
            randomsource.setSeed(blockstate.getSeed(blockpos2));
            this.blockRenderer.getBlockModel(blockstate).collectParts(p_427336_, blockpos2, blockstate, randomsource, list);
            posestack.pushPose();
            posestack.translate(
               (float)SectionPos.sectionRelative(blockpos2.getX()),
               (float)SectionPos.sectionRelative(blockpos2.getY()),
               (float)SectionPos.sectionRelative(blockpos2.getZ())
            );
            this.blockRenderer.renderBatched(blockstate, blockpos2, p_427336_, posestack, bufferLookup, true, list);
            posestack.popPose();
            list.clear();
         }
      }

      ClientHooks.addAdditionalGeometry(additionalRenderers, bufferLookup, p_427336_, posestack);

      for (Entry<ChunkSectionLayer, BufferBuilder> entry : map.entrySet()) {
         ChunkSectionLayer chunksectionlayer1 = entry.getKey();
         MeshData meshdata = entry.getValue().build();
         if (meshdata != null) {
            if (chunksectionlayer1 == ChunkSectionLayer.TRANSLUCENT) {
               sectioncompiler$results.transparencyState = meshdata.sortQuads(p_350612_.buffer(chunksectionlayer1), p_350635_);
            }

            sectioncompiler$results.renderedLayers.put(chunksectionlayer1, meshdata);
         }
      }

      ModelBlockRenderer.clearCache();
      sectioncompiler$results.visibilitySet = visgraph.resolve();
      return sectioncompiler$results;
   }

   private BufferBuilder getOrBeginLayer(Map<ChunkSectionLayer, BufferBuilder> p_350651_, SectionBufferBuilderPack p_350965_, ChunkSectionLayer p_427511_) {
      BufferBuilder bufferbuilder = p_350651_.get(p_427511_);
      if (bufferbuilder == null) {
         ByteBufferBuilder bytebufferbuilder = p_350965_.buffer(p_427511_);
         bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
         p_350651_.put(p_427511_, bufferbuilder);
      }

      return bufferbuilder;
   }

   private <E extends BlockEntity> void handleBlockEntity(SectionCompiler.Results p_350754_, E p_350386_) {
      BlockEntityRenderer<E, ?> blockentityrenderer = this.blockEntityRenderer.getRenderer(p_350386_);
      if (blockentityrenderer != null && !blockentityrenderer.shouldRenderOffScreen()) {
         p_350754_.blockEntities.add(p_350386_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static final class Results {
      public final List<BlockEntity> blockEntities = new ArrayList<>();
      public final Map<ChunkSectionLayer, MeshData> renderedLayers = new EnumMap<>(ChunkSectionLayer.class);
      public VisibilitySet visibilitySet = new VisibilitySet();
      @Nullable
      public MeshData.SortState transparencyState;

      public void release() {
         this.renderedLayers.values().forEach(MeshData::close);
      }
   }
}
