package com.mojang.blaze3d.vertex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.GraphicsWorkarounds;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class VertexFormat {
   public static final int UNKNOWN_ELEMENT = -1;
   private final List<VertexFormatElement> elements;
   private final List<String> names;
   private final int vertexSize;
   private final int elementsMask;
   private final int[] offsetsByElement = new int[32];
   @Nullable
   private GpuBuffer immediateDrawVertexBuffer;
   @Nullable
   private GpuBuffer immediateDrawIndexBuffer;

   VertexFormat(List<VertexFormatElement> p_350393_, List<String> p_350887_, IntList p_350817_, int p_350310_) {
      this.elements = p_350393_;
      this.names = p_350887_;
      this.vertexSize = p_350310_;
      this.elementsMask = p_350393_.stream().mapToInt(VertexFormatElement::mask).reduce(0, (p_350941_, p_350570_) -> p_350941_ | p_350570_);

      for (int i = 0; i < this.offsetsByElement.length; i++) {
         VertexFormatElement vertexformatelement = VertexFormatElement.byId(i);
         int j = vertexformatelement != null ? p_350393_.indexOf(vertexformatelement) : -1;
         this.offsetsByElement[i] = j != -1 ? p_350817_.getInt(j) : -1;
      }
   }

   public static VertexFormat.Builder builder() {
      return new VertexFormat.Builder();
   }

   @Override
   public String toString() {
      return "VertexFormat" + this.names;
   }

   public int getVertexSize() {
      return this.vertexSize;
   }

   public List<VertexFormatElement> getElements() {
      return this.elements;
   }

   public List<String> getElementAttributeNames() {
      return this.names;
   }

   public int[] getOffsetsByElement() {
      return this.offsetsByElement;
   }

   public int getOffset(VertexFormatElement p_350713_) {
      return this.offsetsByElement[p_350713_.id()];
   }

   public boolean contains(VertexFormatElement p_351022_) {
      return (this.elementsMask & p_351022_.mask()) != 0;
   }

   public int getElementsMask() {
      return this.elementsMask;
   }

   public String getElementName(VertexFormatElement p_350904_) {
      int i = this.elements.indexOf(p_350904_);
      if (i == -1) {
         throw new IllegalArgumentException(p_350904_ + " is not contained in format");
      } else {
         return this.names.get(i);
      }
   }

   @Override
   public boolean equals(Object p_86026_) {
      return this == p_86026_
         ? true
         : p_86026_ instanceof VertexFormat vertexformat
            && this.elementsMask == vertexformat.elementsMask
            && this.vertexSize == vertexformat.vertexSize
            && this.names.equals(vertexformat.names)
            && Arrays.equals(this.offsetsByElement, vertexformat.offsetsByElement);
   }

   @Override
   public int hashCode() {
      return this.elementsMask * 31 + Arrays.hashCode(this.offsetsByElement);
   }

   private static GpuBuffer uploadToBuffer(@Nullable GpuBuffer p_428839_, ByteBuffer p_428844_, @GpuBuffer.Usage int p_428826_, Supplier<String> p_428833_) {
      GpuDevice gpudevice = RenderSystem.getDevice();
      if (GraphicsWorkarounds.get(gpudevice).alwaysCreateFreshImmediateBuffer()) {
         if (p_428839_ != null) {
            p_428839_.close();
         }

         return gpudevice.createBuffer(p_428833_, p_428826_, p_428844_);
      } else {
         if (p_428839_ == null) {
            p_428839_ = gpudevice.createBuffer(p_428833_, p_428826_, p_428844_);
         } else {
            CommandEncoder commandencoder = gpudevice.createCommandEncoder();
            if (p_428839_.size() < p_428844_.remaining()) {
               p_428839_.close();
               p_428839_ = gpudevice.createBuffer(p_428833_, p_428826_, p_428844_);
            } else {
               commandencoder.writeToBuffer(p_428839_.slice(), p_428844_);
            }
         }

         return p_428839_;
      }
   }

   public GpuBuffer uploadImmediateVertexBuffer(ByteBuffer p_410477_) {
      this.immediateDrawVertexBuffer = uploadToBuffer(this.immediateDrawVertexBuffer, p_410477_, 40, () -> "Immediate vertex buffer for " + this);
      return this.immediateDrawVertexBuffer;
   }

   public GpuBuffer uploadImmediateIndexBuffer(ByteBuffer p_410562_) {
      this.immediateDrawIndexBuffer = uploadToBuffer(this.immediateDrawIndexBuffer, p_410562_, 72, () -> "Immediate index buffer for " + this);
      return this.immediateDrawIndexBuffer;
   }

   public ImmutableMap<String, VertexFormatElement> getElementMapping() {
      com.google.common.collect.ImmutableMap.Builder<String, VertexFormatElement> builder = ImmutableMap.builder();

      for (int i = 0; i < this.elements.size(); i++) {
         builder.put(this.names.get(i), this.elements.get(i));
      }

      return builder.build();
   }

   public boolean hasPosition() {
      return this.elements.stream().anyMatch(e -> e.usage() == VertexFormatElement.Usage.POSITION);
   }

   public boolean hasNormal() {
      return this.elements.stream().anyMatch(e -> e.usage() == VertexFormatElement.Usage.NORMAL);
   }

   public boolean hasColor() {
      return this.elements.stream().anyMatch(e -> e.usage() == VertexFormatElement.Usage.COLOR);
   }

   public boolean hasUV(int which) {
      return this.elements.stream().anyMatch(e -> e.usage() == VertexFormatElement.Usage.UV && e.index() == which);
   }

   @OnlyIn(Dist.CLIENT)
   @DontObfuscate
   public static class Builder {
      private final com.google.common.collect.ImmutableMap.Builder<String, VertexFormatElement> elements = ImmutableMap.builder();
      private final IntList offsets = new IntArrayList();
      private int offset;

      Builder() {
      }

      public VertexFormat.Builder add(String p_350281_, VertexFormatElement p_350956_) {
         this.elements.put(p_350281_, p_350956_);
         this.offsets.add(this.offset);
         this.offset = this.offset + p_350956_.byteSize();
         return this;
      }

      public VertexFormat.Builder padding(int p_351055_) {
         this.offset += p_351055_;
         return this;
      }

      public VertexFormat build() {
         ImmutableMap<String, VertexFormatElement> immutablemap = this.elements.buildOrThrow();
         ImmutableList<VertexFormatElement> immutablelist = immutablemap.values().asList();
         ImmutableList<String> immutablelist1 = immutablemap.keySet().asList();
         return new VertexFormat(immutablelist, immutablelist1, this.offsets, this.offset);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum IndexType {
      SHORT(2),
      INT(4);

      public final int bytes;

      private IndexType(int p_166930_) {
         this.bytes = p_166930_;
      }

      public static VertexFormat.IndexType least(int p_166934_) {
         return (p_166934_ & -65536) != 0 ? INT : SHORT;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Mode {
      LINES(2, 2, false),
      DEBUG_LINES(2, 2, false),
      DEBUG_LINE_STRIP(2, 1, true),
      POINTS(1, 1, false),
      TRIANGLES(3, 3, false),
      TRIANGLE_STRIP(3, 1, true),
      TRIANGLE_FAN(3, 1, true),
      QUADS(4, 4, false);

      public final int primitiveLength;
      public final int primitiveStride;
      public final boolean connectedPrimitives;

      private Mode(int p_231238_, int p_231239_, boolean p_231241_) {
         this.primitiveLength = p_231238_;
         this.primitiveStride = p_231239_;
         this.connectedPrimitives = p_231241_;
      }

      public int indexCount(int p_166959_) {
         return switch (this) {
            case LINES, QUADS -> p_166959_ / 4 * 6;
            case DEBUG_LINES, DEBUG_LINE_STRIP, POINTS, TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN -> p_166959_;
            default -> 0;
         };
      }
   }
}
