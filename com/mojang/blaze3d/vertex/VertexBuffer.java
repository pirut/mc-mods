package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class VertexBuffer implements AutoCloseable {
   private final VertexBuffer.Usage usage;
   private int vertexBufferId;
   private int indexBufferId;
   private int arrayObjectId;
   @Nullable
   private VertexFormat format;
   @Nullable
   private RenderSystem.AutoStorageIndexBuffer sequentialIndices;
   private VertexFormat.IndexType indexType;
   private int indexCount;
   private VertexFormat.Mode mode;

   public VertexBuffer(VertexBuffer.Usage p_286252_) {
      this.usage = p_286252_;
      RenderSystem.assertOnRenderThread();
      this.vertexBufferId = GlStateManager._glGenBuffers();
      this.indexBufferId = GlStateManager._glGenBuffers();
      this.arrayObjectId = GlStateManager._glGenVertexArrays();
   }

   public void upload(MeshData p_350419_) {
      MeshData var2 = p_350419_;

      label40: {
         try {
            if (this.isInvalid()) {
               break label40;
            }

            RenderSystem.assertOnRenderThread();
            MeshData.DrawState $$1 = p_350419_.drawState();
            this.format = this.uploadVertexBuffer($$1, p_350419_.vertexBuffer());
            this.sequentialIndices = this.uploadIndexBuffer($$1, p_350419_.indexBuffer());
            this.indexCount = $$1.indexCount();
            this.indexType = $$1.indexType();
            this.mode = $$1.mode();
         } catch (Throwable var6) {
            if (p_350419_ != null) {
               try {
                  var2.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (p_350419_ != null) {
            p_350419_.close();
         }

         return;
      }

      if (p_350419_ != null) {
         p_350419_.close();
      }
   }

   public void uploadIndexBuffer(ByteBufferBuilder.Result p_350937_) {
      ByteBufferBuilder.Result var2 = p_350937_;

      label40: {
         try {
            if (this.isInvalid()) {
               break label40;
            }

            RenderSystem.assertOnRenderThread();
            GlStateManager._glBindBuffer(34963, this.indexBufferId);
            RenderSystem.glBufferData(34963, p_350937_.byteBuffer(), this.usage.id);
            this.sequentialIndices = null;
         } catch (Throwable var6) {
            if (p_350937_ != null) {
               try {
                  var2.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (p_350937_ != null) {
            p_350937_.close();
         }

         return;
      }

      if (p_350937_ != null) {
         p_350937_.close();
      }
   }

   private VertexFormat uploadVertexBuffer(MeshData.DrawState p_350866_, @Nullable ByteBuffer p_231220_) {
      boolean $$2 = false;
      if (!p_350866_.format().equals(this.format)) {
         if (this.format != null) {
            this.format.clearBufferState();
         }

         GlStateManager._glBindBuffer(34962, this.vertexBufferId);
         p_350866_.format().setupBufferState();
         $$2 = true;
      }

      if (p_231220_ != null) {
         if (!$$2) {
            GlStateManager._glBindBuffer(34962, this.vertexBufferId);
         }

         RenderSystem.glBufferData(34962, p_231220_, this.usage.id);
      }

      return p_350866_.format();
   }

   @Nullable
   private RenderSystem.AutoStorageIndexBuffer uploadIndexBuffer(MeshData.DrawState p_350538_, @Nullable ByteBuffer p_231225_) {
      if (p_231225_ != null) {
         GlStateManager._glBindBuffer(34963, this.indexBufferId);
         RenderSystem.glBufferData(34963, p_231225_, this.usage.id);
         return null;
      } else {
         RenderSystem.AutoStorageIndexBuffer $$2 = RenderSystem.getSequentialBuffer(p_350538_.mode());
         if ($$2 != this.sequentialIndices || !$$2.hasStorage(p_350538_.indexCount())) {
            $$2.bind(p_350538_.indexCount());
         }

         return $$2;
      }
   }

   public void bind() {
      BufferUploader.invalidate();
      GlStateManager._glBindVertexArray(this.arrayObjectId);
   }

   public static void unbind() {
      BufferUploader.invalidate();
      GlStateManager._glBindVertexArray(0);
   }

   public void draw() {
      RenderSystem.drawElements(this.mode.asGLMode, this.indexCount, this.getIndexType().asGLType);
   }

   private VertexFormat.IndexType getIndexType() {
      RenderSystem.AutoStorageIndexBuffer $$0 = this.sequentialIndices;
      return $$0 != null ? $$0.type() : this.indexType;
   }

   public void drawWithShader(Matrix4f p_254480_, Matrix4f p_254555_, ShaderInstance p_253993_) {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> this._drawWithShader(new Matrix4f(p_254480_), new Matrix4f(p_254555_), p_253993_));
      } else {
         this._drawWithShader(p_254480_, p_254555_, p_253993_);
      }
   }

   private void _drawWithShader(Matrix4f p_253705_, Matrix4f p_253737_, ShaderInstance p_166879_) {
      p_166879_.setDefaultUniforms(this.mode, p_253705_, p_253737_, Minecraft.getInstance().getWindow());
      p_166879_.apply();
      this.draw();
      p_166879_.clear();
   }

   @Override
   public void close() {
      if (this.vertexBufferId >= 0) {
         RenderSystem.glDeleteBuffers(this.vertexBufferId);
         this.vertexBufferId = -1;
      }

      if (this.indexBufferId >= 0) {
         RenderSystem.glDeleteBuffers(this.indexBufferId);
         this.indexBufferId = -1;
      }

      if (this.arrayObjectId >= 0) {
         RenderSystem.glDeleteVertexArrays(this.arrayObjectId);
         this.arrayObjectId = -1;
      }
   }

   public VertexFormat getFormat() {
      return this.format;
   }

   public boolean isInvalid() {
      return this.arrayObjectId == -1;
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Usage {
      STATIC(35044),
      DYNAMIC(35048);

      final int id;

      private Usage(int p_286680_) {
         this.id = p_286680_;
      }
   }
}
