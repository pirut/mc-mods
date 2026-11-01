package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.ARBVertexAttribBinding;
import org.lwjgl.opengl.GLCapabilities;

@OnlyIn(Dist.CLIENT)
public abstract class VertexArrayCache {
   public static VertexArrayCache create(GLCapabilities p_410150_, GlDebugLabel p_410596_, Set<String> p_411102_) {
      if (p_410150_.GL_ARB_vertex_attrib_binding && GlDevice.USE_GL_ARB_vertex_attrib_binding) {
         p_411102_.add("GL_ARB_vertex_attrib_binding");
         return new VertexArrayCache.Separate(p_410596_);
      } else {
         return new VertexArrayCache.Emulated(p_410596_);
      }
   }

   public abstract void bindVertexArray(VertexFormat var1, @Nullable GlBuffer var2);

   @OnlyIn(Dist.CLIENT)
   static class Emulated extends VertexArrayCache {
      private final Map<VertexFormat, VertexArrayCache.VertexArray> cache = new HashMap<>();
      private final GlDebugLabel debugLabels;

      public Emulated(GlDebugLabel p_409735_) {
         this.debugLabels = p_409735_;
      }

      @Override
      public void bindVertexArray(VertexFormat p_410490_, @Nullable GlBuffer p_410612_) {
         VertexArrayCache.VertexArray vertexarraycache$vertexarray = this.cache.get(p_410490_);
         if (vertexarraycache$vertexarray == null) {
            int i = GlStateManager._glGenVertexArrays();
            GlStateManager._glBindVertexArray(i);
            if (p_410612_ != null) {
               GlStateManager._glBindBuffer(34962, p_410612_.handle);
               setupCombinedAttributes(p_410490_, true);
            }

            VertexArrayCache.VertexArray vertexarraycache$vertexarray1 = new VertexArrayCache.VertexArray(i, p_410490_, p_410612_);
            this.debugLabels.applyLabel(vertexarraycache$vertexarray1);
            this.cache.put(p_410490_, vertexarraycache$vertexarray1);
         } else {
            GlStateManager._glBindVertexArray(vertexarraycache$vertexarray.id);
            if (p_410612_ != null && vertexarraycache$vertexarray.lastVertexBuffer != p_410612_) {
               GlStateManager._glBindBuffer(34962, p_410612_.handle);
               vertexarraycache$vertexarray.lastVertexBuffer = p_410612_;
               setupCombinedAttributes(p_410490_, false);
            }
         }
      }

      private static void setupCombinedAttributes(VertexFormat p_409981_, boolean p_410588_) {
         int i = p_409981_.getVertexSize();
         List<VertexFormatElement> list = p_409981_.getElements();

         for (int j = 0; j < list.size(); j++) {
            VertexFormatElement vertexformatelement = list.get(j);
            if (p_410588_) {
               GlStateManager._enableVertexAttribArray(j);
            }

            switch (vertexformatelement.usage()) {
               case POSITION:
               case GENERIC:
               case UV:
                  if (vertexformatelement.type() == VertexFormatElement.Type.FLOAT) {
                     GlStateManager._vertexAttribPointer(
                        j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), false, i, p_409981_.getOffset(vertexformatelement)
                     );
                  } else {
                     GlStateManager._vertexAttribIPointer(
                        j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), i, p_409981_.getOffset(vertexformatelement)
                     );
                  }
                  break;
               case NORMAL:
               case COLOR:
                  GlStateManager._vertexAttribPointer(
                     j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), true, i, p_409981_.getOffset(vertexformatelement)
                  );
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class Separate extends VertexArrayCache {
      private final Map<VertexFormat, VertexArrayCache.VertexArray> cache = new HashMap<>();
      private final GlDebugLabel debugLabels;
      private final boolean needsMesaWorkaround;

      public Separate(GlDebugLabel p_410359_) {
         this.debugLabels = p_410359_;
         if ("Mesa".equals(GlStateManager._getString(7936))) {
            String s = GlStateManager._getString(7938);
            this.needsMesaWorkaround = s.contains("25.0.0") || s.contains("25.0.1") || s.contains("25.0.2");
         } else {
            this.needsMesaWorkaround = false;
         }
      }

      @Override
      public void bindVertexArray(VertexFormat p_410638_, @Nullable GlBuffer p_410508_) {
         VertexArrayCache.VertexArray vertexarraycache$vertexarray = this.cache.get(p_410638_);
         if (vertexarraycache$vertexarray != null) {
            GlStateManager._glBindVertexArray(vertexarraycache$vertexarray.id);
            if (p_410508_ != null && vertexarraycache$vertexarray.lastVertexBuffer != p_410508_) {
               if (this.needsMesaWorkaround
                  && vertexarraycache$vertexarray.lastVertexBuffer != null
                  && vertexarraycache$vertexarray.lastVertexBuffer.handle == p_410508_.handle) {
                  ARBVertexAttribBinding.glBindVertexBuffer(0, 0, 0L, 0);
               }

               ARBVertexAttribBinding.glBindVertexBuffer(0, p_410508_.handle, 0L, p_410638_.getVertexSize());
               vertexarraycache$vertexarray.lastVertexBuffer = p_410508_;
            }
         } else {
            int i = GlStateManager._glGenVertexArrays();
            GlStateManager._glBindVertexArray(i);
            if (p_410508_ != null) {
               List<VertexFormatElement> list = p_410638_.getElements();

               for (int j = 0; j < list.size(); j++) {
                  VertexFormatElement vertexformatelement = list.get(j);
                  GlStateManager._enableVertexAttribArray(j);
                  switch (vertexformatelement.usage()) {
                     case POSITION:
                     case GENERIC:
                     case UV:
                        if (vertexformatelement.type() == VertexFormatElement.Type.FLOAT) {
                           ARBVertexAttribBinding.glVertexAttribFormat(
                              j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), false, p_410638_.getOffset(vertexformatelement)
                           );
                        } else {
                           ARBVertexAttribBinding.glVertexAttribIFormat(
                              j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), p_410638_.getOffset(vertexformatelement)
                           );
                        }
                        break;
                     case NORMAL:
                     case COLOR:
                        ARBVertexAttribBinding.glVertexAttribFormat(
                           j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), true, p_410638_.getOffset(vertexformatelement)
                        );
                  }

                  ARBVertexAttribBinding.glVertexAttribBinding(j, 0);
               }
            }

            if (p_410508_ != null) {
               ARBVertexAttribBinding.glBindVertexBuffer(0, p_410508_.handle, 0L, p_410638_.getVertexSize());
            }

            VertexArrayCache.VertexArray vertexarraycache$vertexarray1 = new VertexArrayCache.VertexArray(i, p_410638_, p_410508_);
            this.debugLabels.applyLabel(vertexarraycache$vertexarray1);
            this.cache.put(p_410638_, vertexarraycache$vertexarray1);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class VertexArray {
      final int id;
      final VertexFormat format;
      @Nullable
      GlBuffer lastVertexBuffer;

      VertexArray(int p_409990_, VertexFormat p_410774_, @Nullable GlBuffer p_410599_) {
         this.id = p_409990_;
         this.format = p_410774_;
         this.lastVertexBuffer = p_410599_;
      }
   }
}
