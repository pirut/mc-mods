package net.neoforged.neoforge.client.model.quad;

import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public sealed interface BakedNormals permits BakedNormals.PerQuad, BakedNormals.PerVertex {
   BakedNormals UNSPECIFIED = new BakedNormals.PerQuad(0);

   int normal(int var1);

   static BakedNormals of(int normal0, int normal1, int normal2, int normal3) {
      return (BakedNormals)(normal0 == normal1 && normal0 == normal2 && normal0 == normal3
         ? of(normal0)
         : new BakedNormals.PerVertex(normal0, normal1, normal2, normal3));
   }

   static BakedNormals of(int normal) {
      return (BakedNormals)(isUnspecified(normal) ? UNSPECIFIED : new BakedNormals.PerQuad(normal));
   }

   static int pack(float x, float y, float z) {
      int packedx = (byte)Math.round(x * 127.0F) & 255;
      int packedy = (byte)Math.round(y * 127.0F) & 255;
      int packedz = (byte)Math.round(z * 127.0F) & 255;
      return packedx | packedy << 8 | packedz << 16;
   }

   static int pack(Vector3fc normal) {
      return pack(normal.x(), normal.y(), normal.z());
   }

   static float unpackComponent(int packedNormal, int axis) {
      int encodedNormalComponent = packedNormal >> axis * 8 & 0xFF;
      return (byte)encodedNormalComponent / 127.0F;
   }

   static float unpackX(int packedNormal) {
      return (byte)(packedNormal & 0xFF) / 127.0F;
   }

   static float unpackY(int packedNormal) {
      return (byte)(packedNormal >> 8 & 0xFF) / 127.0F;
   }

   static float unpackZ(int packedNormal) {
      return (byte)(packedNormal >> 16 & 0xFF) / 127.0F;
   }

   static Vector3fc unpack(int packedNormal, @Nullable Vector3f destination) {
      if (destination == null) {
         destination = new Vector3f();
      }

      return destination.set(unpackX(packedNormal), unpackY(packedNormal), unpackZ(packedNormal));
   }

   static boolean isUnspecified(int packedNormal) {
      return (packedNormal & 16777215) == 0;
   }

   static int computeQuadNormal(Vector3fc position0, Vector3fc position1, Vector3fc position2, Vector3fc position3) {
      float dx0 = position3.x() - position1.x();
      float dy0 = position3.y() - position1.y();
      float dz0 = position3.z() - position1.z();
      float dx1 = position2.x() - position0.x();
      float dy1 = position2.y() - position0.y();
      float dz1 = position2.z() - position0.z();
      float nx = dy1 * dz0 - dz1 * dy0;
      float ny = dz1 * dx0 - dx1 * dz0;
      float nz = dx1 * dy0 - dy1 * dx0;
      float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
      if (length > 0.0F) {
         nx /= length;
         ny /= length;
         nz /= length;
      }

      return pack(nx, ny, nz);
   }

   public record PerQuad(int normal) implements BakedNormals {
   }

   public record PerVertex(int normal0, int normal1, int normal2, int normal3) implements BakedNormals {
      @Override
      public int normal(int vertex) {
         return switch (vertex) {
            case 0 -> this.normal0;
            case 1 -> this.normal1;
            case 2 -> this.normal2;
            case 3 -> this.normal3;
            default -> throw new IndexOutOfBoundsException(vertex);
         };
      }
   }
}
