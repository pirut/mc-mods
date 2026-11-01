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
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CompiledSectionMesh implements SectionMesh {
   public static final SectionMesh UNCOMPILED = new SectionMesh() {
      @Override
      public boolean facesCanSeeEachother(Direction p_427455_, Direction p_427422_) {
         return false;
      }
   };
   public static final SectionMesh EMPTY = new SectionMesh() {
      @Override
      public boolean facesCanSeeEachother(Direction p_427417_, Direction p_427302_) {
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

   public CompiledSectionMesh(TranslucencyPointOfView p_427516_, SectionCompiler.Results p_427448_) {
      this.translucencyPointOfView = p_427516_;
      this.visibilitySet = p_427448_.visibilitySet;
      this.renderableBlockEntities = p_427448_.blockEntities;
      this.transparencyState = p_427448_.transparencyState;
   }

   public void setTranslucencyPointOfView(TranslucencyPointOfView p_427440_) {
      this.translucencyPointOfView = p_427440_;
   }

   @Override
   public boolean isDifferentPointOfView(TranslucencyPointOfView p_427247_) {
      return !p_427247_.equals(this.translucencyPointOfView);
   }

   @Override
   public boolean hasRenderableLayers() {
      return !this.buffers.isEmpty();
   }

   @Override
   public boolean isEmpty(ChunkSectionLayer p_427390_) {
      return !this.buffers.containsKey(p_427390_);
   }

   @Override
   public List<BlockEntity> getRenderableBlockEntities() {
      return this.renderableBlockEntities;
   }

   @Override
   public boolean facesCanSeeEachother(Direction p_427348_, Direction p_427368_) {
      return this.visibilitySet.visibilityBetween(p_427348_, p_427368_);
   }

   @Nullable
   @Override
   public SectionBuffers getBuffers(ChunkSectionLayer p_427316_) {
      return this.buffers.get(p_427316_);
   }

   public void uploadMeshLayer(ChunkSectionLayer p_427297_, MeshData p_427250_, long p_427371_) {
      CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
      SectionBuffers sectionbuffers = this.getBuffers(p_427297_);
      if (sectionbuffers != null) {
         if (sectionbuffers.getVertexBuffer().size() < p_427250_.vertexBuffer().remaining()) {
            sectionbuffers.getVertexBuffer().close();
            sectionbuffers.setVertexBuffer(
               RenderSystem.getDevice()
                  .createBuffer(
                     () -> "Section vertex buffer - layer: "
                        + p_427297_.label()
                        + "; cords: "
                        + SectionPos.x(p_427371_)
                        + ", "
                        + SectionPos.y(p_427371_)
                        + ", "
                        + SectionPos.z(p_427371_),
                     40,
                     p_427250_.vertexBuffer()
                  )
            );
         } else if (!sectionbuffers.getVertexBuffer().isClosed()) {
            commandencoder.writeToBuffer(sectionbuffers.getVertexBuffer().slice(), p_427250_.vertexBuffer());
         }

         ByteBuffer bytebuffer = p_427250_.indexBuffer();
         if (bytebuffer != null) {
            if (sectionbuffers.getIndexBuffer() != null && sectionbuffers.getIndexBuffer().size() >= bytebuffer.remaining()) {
               if (!sectionbuffers.getIndexBuffer().isClosed()) {
                  commandencoder.writeToBuffer(sectionbuffers.getIndexBuffer().slice(), bytebuffer);
               }
            } else {
               if (sectionbuffers.getIndexBuffer() != null) {
                  sectionbuffers.getIndexBuffer().close();
               }

               sectionbuffers.setIndexBuffer(
                  RenderSystem.getDevice()
                     .createBuffer(
                        () -> "Section index buffer - layer: "
                           + p_427297_.label()
                           + "; cords: "
                           + SectionPos.x(p_427371_)
                           + ", "
                           + SectionPos.y(p_427371_)
                           + ", "
                           + SectionPos.z(p_427371_),
                        72,
                        bytebuffer
                     )
               );
            }
         } else if (sectionbuffers.getIndexBuffer() != null) {
            sectionbuffers.getIndexBuffer().close();
            sectionbuffers.setIndexBuffer(null);
         }

         sectionbuffers.setIndexCount(p_427250_.drawState().indexCount());
         sectionbuffers.setIndexType(p_427250_.drawState().indexType());
      } else {
         GpuBuffer gpubuffer1 = RenderSystem.getDevice()
            .createBuffer(
               () -> "Section vertex buffer - layer: "
                  + p_427297_.label()
                  + "; cords: "
                  + SectionPos.x(p_427371_)
                  + ", "
                  + SectionPos.y(p_427371_)
                  + ", "
                  + SectionPos.z(p_427371_),
               40,
               p_427250_.vertexBuffer()
            );
         ByteBuffer bytebuffer1 = p_427250_.indexBuffer();
         GpuBuffer gpubuffer = bytebuffer1 != null
            ? RenderSystem.getDevice()
               .createBuffer(
                  () -> "Section index buffer - layer: "
                     + p_427297_.label()
                     + "; cords: "
                     + SectionPos.x(p_427371_)
                     + ", "
                     + SectionPos.y(p_427371_)
                     + ", "
                     + SectionPos.z(p_427371_),
                  72,
                  bytebuffer1
               )
            : null;
         SectionBuffers sectionbuffers1 = new SectionBuffers(gpubuffer1, gpubuffer, p_427250_.drawState().indexCount(), p_427250_.drawState().indexType());
         this.buffers.put(p_427297_, sectionbuffers1);
      }
   }

   public void uploadLayerIndexBuffer(ChunkSectionLayer p_427505_, ByteBufferBuilder.Result p_427275_, long p_427419_) {
      SectionBuffers sectionbuffers = this.getBuffers(p_427505_);
      if (sectionbuffers != null) {
         if (sectionbuffers.getIndexBuffer() == null) {
            sectionbuffers.setIndexBuffer(
               RenderSystem.getDevice()
                  .createBuffer(
                     () -> "Section index buffer - layer: "
                        + p_427505_.label()
                        + "; cords: "
                        + SectionPos.x(p_427419_)
                        + ", "
                        + SectionPos.y(p_427419_)
                        + ", "
                        + SectionPos.z(p_427419_),
                     72,
                     p_427275_.byteBuffer()
                  )
            );
         } else {
            CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
            if (!sectionbuffers.getIndexBuffer().isClosed()) {
               commandencoder.writeToBuffer(sectionbuffers.getIndexBuffer().slice(), p_427275_.byteBuffer());
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
