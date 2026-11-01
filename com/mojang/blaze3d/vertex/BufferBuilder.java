package com.mojang.blaze3d.vertex;

import java.nio.ByteOrder;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
public class BufferBuilder implements VertexConsumer {
   private static final long NOT_BUILDING = -1L;
   private static final long UNKNOWN_ELEMENT = -1L;
   private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
   private final ByteBufferBuilder buffer;
   private long vertexPointer = -1L;
   private int vertices;
   private final VertexFormat format;
   private final VertexFormat.Mode mode;
   private final boolean fastFormat;
   private final boolean fullFormat;
   private final int vertexSize;
   private final int initialElementsToFill;
   private final int[] offsetsByElement;
   private int elementsToFill;
   private boolean building = true;

   public BufferBuilder(ByteBufferBuilder p_350781_, VertexFormat.Mode p_350789_, VertexFormat p_350682_) {
      if (!p_350682_.contains(VertexFormatElement.POSITION)) {
         throw new IllegalArgumentException("Cannot build mesh with no position element");
      } else {
         this.buffer = p_350781_;
         this.mode = p_350789_;
         this.format = p_350682_;
         this.vertexSize = p_350682_.getVertexSize();
         this.initialElementsToFill = p_350682_.getElementsMask() & ~VertexFormatElement.POSITION.mask();
         this.offsetsByElement = p_350682_.getOffsetsByElement();
         boolean $$3 = p_350682_ == DefaultVertexFormat.NEW_ENTITY;
         boolean $$4 = p_350682_ == DefaultVertexFormat.BLOCK;
         this.fastFormat = $$3 || $$4;
         this.fullFormat = $$3;
      }
   }

   @Nullable
   public MeshData build() {
      this.ensureBuilding();
      this.endLastVertex();
      MeshData $$0 = this.storeMesh();
      this.building = false;
      this.vertexPointer = -1L;
      return $$0;
   }

   public MeshData buildOrThrow() {
      MeshData $$0 = this.build();
      if ($$0 == null) {
         throw new IllegalStateException("BufferBuilder was empty");
      } else {
         return $$0;
      }
   }

   private void ensureBuilding() {
      if (!this.building) {
         throw new IllegalStateException("Not building!");
      }
   }

   @Nullable
   private MeshData storeMesh() {
      if (this.vertices == 0) {
         return null;
      } else {
         ByteBufferBuilder.Result $$0 = this.buffer.build();
         if ($$0 == null) {
            return null;
         } else {
            int $$1 = this.mode.indexCount(this.vertices);
            VertexFormat.IndexType $$2 = VertexFormat.IndexType.least(this.vertices);
            return new MeshData($$0, new MeshData.DrawState(this.format, this.vertices, $$1, this.mode, $$2));
         }
      }
   }

   private long beginVertex() {
      this.ensureBuilding();
      this.endLastVertex();
      this.vertices++;
      long $$0 = this.buffer.reserve(this.vertexSize);
      this.vertexPointer = $$0;
      return $$0;
   }

   private long beginElement(VertexFormatElement p_350425_) {
      int $$1 = this.elementsToFill;
      int $$2 = $$1 & ~p_350425_.mask();
      if ($$2 == $$1) {
         return -1L;
      } else {
         this.elementsToFill = $$2;
         long $$3 = this.vertexPointer;
         if ($$3 == -1L) {
            throw new IllegalArgumentException("Not currently building vertex");
         } else {
            return $$3 + this.offsetsByElement[p_350425_.id()];
         }
      }
   }

   private void endLastVertex() {
      if (this.vertices != 0) {
         if (this.elementsToFill != 0) {
            String $$0 = VertexFormatElement.elementsFromMask(this.elementsToFill).map(this.format::getElementName).collect(Collectors.joining(", "));
            throw new IllegalStateException("Missing elements in vertex: " + $$0);
         } else {
            if (this.mode == VertexFormat.Mode.LINES || this.mode == VertexFormat.Mode.LINE_STRIP) {
               long $$1 = this.buffer.reserve(this.vertexSize);
               MemoryUtil.memCopy($$1 - this.vertexSize, $$1, this.vertexSize);
               this.vertices++;
            }
         }
      }
   }

   private static void putRgba(long p_350739_, int p_350474_) {
      int $$2 = FastColor.ABGR32.fromArgb32(p_350474_);
      MemoryUtil.memPutInt(p_350739_, IS_LITTLE_ENDIAN ? $$2 : Integer.reverseBytes($$2));
   }

   private static void putPackedUv(long p_350878_, int p_350667_) {
      if (IS_LITTLE_ENDIAN) {
         MemoryUtil.memPutInt(p_350878_, p_350667_);
      } else {
         MemoryUtil.memPutShort(p_350878_, (short)(p_350667_ & 65535));
         MemoryUtil.memPutShort(p_350878_ + 2L, (short)(p_350667_ >> 16 & 65535));
      }
   }

   @Override
   public VertexConsumer addVertex(float p_350828_, float p_350614_, float p_350700_) {
      long $$3 = this.beginVertex() + this.offsetsByElement[VertexFormatElement.POSITION.id()];
      this.elementsToFill = this.initialElementsToFill;
      MemoryUtil.memPutFloat($$3, p_350828_);
      MemoryUtil.memPutFloat($$3 + 4L, p_350614_);
      MemoryUtil.memPutFloat($$3 + 8L, p_350700_);
      return this;
   }

   @Override
   public VertexConsumer setColor(int p_350581_, int p_350952_, int p_350275_, int p_350985_) {
      long $$4 = this.beginElement(VertexFormatElement.COLOR);
      if ($$4 != -1L) {
         MemoryUtil.memPutByte($$4, (byte)p_350581_);
         MemoryUtil.memPutByte($$4 + 1L, (byte)p_350952_);
         MemoryUtil.memPutByte($$4 + 2L, (byte)p_350275_);
         MemoryUtil.memPutByte($$4 + 3L, (byte)p_350985_);
      }

      return this;
   }

   @Override
   public VertexConsumer setColor(int p_350530_) {
      long $$1 = this.beginElement(VertexFormatElement.COLOR);
      if ($$1 != -1L) {
         putRgba($$1, p_350530_);
      }

      return this;
   }

   @Override
   public VertexConsumer setUv(float p_350574_, float p_350773_) {
      long $$2 = this.beginElement(VertexFormatElement.UV0);
      if ($$2 != -1L) {
         MemoryUtil.memPutFloat($$2, p_350574_);
         MemoryUtil.memPutFloat($$2 + 4L, p_350773_);
      }

      return this;
   }

   @Override
   public VertexConsumer setUv1(int p_350396_, int p_350722_) {
      return this.uvShort((short)p_350396_, (short)p_350722_, VertexFormatElement.UV1);
   }

   @Override
   public VertexConsumer setOverlay(int p_350297_) {
      long $$1 = this.beginElement(VertexFormatElement.UV1);
      if ($$1 != -1L) {
         putPackedUv($$1, p_350297_);
      }

      return this;
   }

   @Override
   public VertexConsumer setUv2(int p_351058_, int p_350320_) {
      return this.uvShort((short)p_351058_, (short)p_350320_, VertexFormatElement.UV2);
   }

   @Override
   public VertexConsumer setLight(int p_350848_) {
      long $$1 = this.beginElement(VertexFormatElement.UV2);
      if ($$1 != -1L) {
         putPackedUv($$1, p_350848_);
      }

      return this;
   }

   private VertexConsumer uvShort(short p_350449_, short p_350780_, VertexFormatElement p_350925_) {
      long $$3 = this.beginElement(p_350925_);
      if ($$3 != -1L) {
         MemoryUtil.memPutShort($$3, p_350449_);
         MemoryUtil.memPutShort($$3 + 2L, p_350780_);
      }

      return this;
   }

   @Override
   public VertexConsumer setNormal(float p_351000_, float p_350982_, float p_350974_) {
      long $$3 = this.beginElement(VertexFormatElement.NORMAL);
      if ($$3 != -1L) {
         MemoryUtil.memPutByte($$3, normalIntValue(p_351000_));
         MemoryUtil.memPutByte($$3 + 1L, normalIntValue(p_350982_));
         MemoryUtil.memPutByte($$3 + 2L, normalIntValue(p_350974_));
      }

      return this;
   }

   private static byte normalIntValue(float p_350741_) {
      return (byte)((int)(Mth.clamp(p_350741_, -1.0F, 1.0F) * 127.0F) & 0xFF);
   }

   @Override
   public void addVertex(
      float p_350423_,
      float p_350381_,
      float p_350383_,
      int p_350371_,
      float p_350977_,
      float p_350674_,
      int p_350816_,
      int p_350690_,
      float p_350640_,
      float p_350490_,
      float p_350810_
   ) {
      if (this.fastFormat) {
         long $$11 = this.beginVertex();
         MemoryUtil.memPutFloat($$11 + 0L, p_350423_);
         MemoryUtil.memPutFloat($$11 + 4L, p_350381_);
         MemoryUtil.memPutFloat($$11 + 8L, p_350383_);
         putRgba($$11 + 12L, p_350371_);
         MemoryUtil.memPutFloat($$11 + 16L, p_350977_);
         MemoryUtil.memPutFloat($$11 + 20L, p_350674_);
         long $$12;
         if (this.fullFormat) {
            putPackedUv($$11 + 24L, p_350816_);
            $$12 = $$11 + 28L;
         } else {
            $$12 = $$11 + 24L;
         }

         putPackedUv($$12 + 0L, p_350690_);
         MemoryUtil.memPutByte($$12 + 4L, normalIntValue(p_350640_));
         MemoryUtil.memPutByte($$12 + 5L, normalIntValue(p_350490_));
         MemoryUtil.memPutByte($$12 + 6L, normalIntValue(p_350810_));
      } else {
         VertexConsumer.super.addVertex(p_350423_, p_350381_, p_350383_, p_350371_, p_350977_, p_350674_, p_350816_, p_350690_, p_350640_, p_350490_, p_350810_);
      }
   }
}
