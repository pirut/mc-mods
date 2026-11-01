package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class CompiledSectionMesh implements SectionMesh {
   public static final SectionMesh UNCOMPILED = new SectionMesh() {
      @Override
      public boolean facesCanSeeEachother(Direction face1, Direction face2) {
         return false;
      }
   };
   public static final SectionMesh EMPTY = new SectionMesh() {
      @Override
      public boolean facesCanSeeEachother(Direction face1, Direction face2) {
         return true;
      }
   };
   private final List<BlockEntity> renderableBlockEntities;
   private final VisibilitySet visibilitySet;
   @Nullable
   private final MeshData.SortState transparencyState;
   @Nullable
   private TranslucencyPointOfView translucencyPointOfView;
   private final Map<ChunkSectionLayer, SectionBuffers> buffers = new EnumMap<>(ChunkSectionLayer.class);

   public CompiledSectionMesh(TranslucencyPointOfView translucencyPointOfView, SectionCompiler.Results results) {
      this.translucencyPointOfView = translucencyPointOfView;
      this.visibilitySet = results.visibilitySet;
      this.renderableBlockEntities = results.blockEntities;
      this.transparencyState = results.transparencyState;
   }

   public void setTranslucencyPointOfView(TranslucencyPointOfView translucencyPointOfView) {
      this.translucencyPointOfView = translucencyPointOfView;
   }

   @Override
   public boolean isDifferentPointOfView(TranslucencyPointOfView pointOfView) {
      return !pointOfView.equals(this.translucencyPointOfView);
   }

   @Override
   public boolean hasRenderableLayers() {
      return !this.buffers.isEmpty();
   }

   @Override
   public boolean isEmpty(ChunkSectionLayer layer) {
      return !this.buffers.containsKey(layer);
   }

   @Override
   public List<BlockEntity> getRenderableBlockEntities() {
      return this.renderableBlockEntities;
   }

   @Override
   public boolean facesCanSeeEachother(Direction face1, Direction face2) {
      return this.visibilitySet.visibilityBetween(face1, face2);
   }

   @Nullable
   @Override
   public SectionBuffers getBuffers(ChunkSectionLayer layer) {
      return this.buffers.get(layer);
   }

   public void uploadMeshLayer(ChunkSectionLayer layer, MeshData meshData, long sectionNode) {
      CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
      SectionBuffers sectionBuffers = this.getBuffers(layer);
      if (sectionBuffers != null) {
         if (sectionBuffers.getVertexBuffer().size() < meshData.vertexBuffer().remaining()) {
            sectionBuffers.getVertexBuffer().close();
            sectionBuffers.setVertexBuffer(
               RenderSystem.getDevice()
                  .createBuffer(
                     () -> "Section vertex buffer - layer: "
                        + layer.label()
                        + "; cords: "
                        + SectionPos.x(sectionNode)
                        + ", "
                        + SectionPos.y(sectionNode)
                        + ", "
                        + SectionPos.z(sectionNode),
                     40,
                     meshData.vertexBuffer()
                  )
            );
         } else if (!sectionBuffers.getVertexBuffer().isClosed()) {
            commandEncoder.writeToBuffer(sectionBuffers.getVertexBuffer().slice(), meshData.vertexBuffer());
         }

         ByteBuffer byteBuffer = meshData.indexBuffer();
         if (byteBuffer != null) {
            if (sectionBuffers.getIndexBuffer() != null && sectionBuffers.getIndexBuffer().size() >= byteBuffer.remaining()) {
               if (!sectionBuffers.getIndexBuffer().isClosed()) {
                  commandEncoder.writeToBuffer(sectionBuffers.getIndexBuffer().slice(), byteBuffer);
               }
            } else {
               if (sectionBuffers.getIndexBuffer() != null) {
                  sectionBuffers.getIndexBuffer().close();
               }

               sectionBuffers.setIndexBuffer(
                  RenderSystem.getDevice()
                     .createBuffer(
                        () -> "Section index buffer - layer: "
                           + layer.label()
                           + "; cords: "
                           + SectionPos.x(sectionNode)
                           + ", "
                           + SectionPos.y(sectionNode)
                           + ", "
                           + SectionPos.z(sectionNode),
                        72,
                        byteBuffer
                     )
               );
            }
         } else if (sectionBuffers.getIndexBuffer() != null) {
            sectionBuffers.getIndexBuffer().close();
            sectionBuffers.setIndexBuffer(null);
         }

         sectionBuffers.setIndexCount(meshData.drawState().indexCount());
         sectionBuffers.setIndexType(meshData.drawState().indexType());
      } else {
         GpuBuffer gpuBuffer = RenderSystem.getDevice()
            .createBuffer(
               () -> "Section vertex buffer - layer: "
                  + layer.label()
                  + "; cords: "
                  + SectionPos.x(sectionNode)
                  + ", "
                  + SectionPos.y(sectionNode)
                  + ", "
                  + SectionPos.z(sectionNode),
               40,
               meshData.vertexBuffer()
            );
         ByteBuffer byteBuffer2 = meshData.indexBuffer();
         GpuBuffer gpuBuffer2 = byteBuffer2 != null
            ? RenderSystem.getDevice()
               .createBuffer(
                  () -> "Section index buffer - layer: "
                     + layer.label()
                     + "; cords: "
                     + SectionPos.x(sectionNode)
                     + ", "
                     + SectionPos.y(sectionNode)
                     + ", "
                     + SectionPos.z(sectionNode),
                  72,
                  byteBuffer2
               )
            : null;
         SectionBuffers sectionBuffers2 = new SectionBuffers(gpuBuffer, gpuBuffer2, meshData.drawState().indexCount(), meshData.drawState().indexType());
         this.buffers.put(layer, sectionBuffers2);
      }
   }

   public void uploadLayerIndexBuffer(ChunkSectionLayer layer, ByteBufferBuilder.Result result, long sectionNode) {
      SectionBuffers sectionBuffers = this.getBuffers(layer);
      if (sectionBuffers != null) {
         if (sectionBuffers.getIndexBuffer() == null) {
            sectionBuffers.setIndexBuffer(
               RenderSystem.getDevice()
                  .createBuffer(
                     () -> "Section index buffer - layer: "
                        + layer.label()
                        + "; cords: "
                        + SectionPos.x(sectionNode)
                        + ", "
                        + SectionPos.y(sectionNode)
                        + ", "
                        + SectionPos.z(sectionNode),
                     72,
                     result.byteBuffer()
                  )
            );
         } else {
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            if (!sectionBuffers.getIndexBuffer().isClosed()) {
               commandEncoder.writeToBuffer(sectionBuffers.getIndexBuffer().slice(), result.byteBuffer());
            }
         }
      }
   }

   @Override
   public boolean hasTranslucentGeometry() {
      return this.buffers.containsKey(ChunkSectionLayer.TRANSLUCENT);
   }

   @Nullable
   public MeshData.SortState getTransparencyState() {
      return this.transparencyState;
   }

   @Override
   public void close() {
      this.buffers.values().forEach(SectionBuffers::close);
      this.buffers.clear();
   }
}
