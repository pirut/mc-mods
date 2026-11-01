package net.minecraft.client.renderer.culling;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public class Frustum {
   public static final int OFFSET_STEP = 4;
   private final FrustumIntersection intersection = new FrustumIntersection();
   private final Matrix4f matrix = new Matrix4f();
   private Vector4f viewVector;
   private double camX;
   private double camY;
   private double camZ;

   public Frustum(Matrix4f frustum, Matrix4f projection) {
      this.calculateFrustum(frustum, projection);
   }

   public Frustum(Frustum other) {
      this.intersection.set(other.matrix);
      this.matrix.set(other.matrix);
      this.camX = other.camX;
      this.camY = other.camY;
      this.camZ = other.camZ;
      this.viewVector = other.viewVector;
   }

   public Frustum offset(float offset) {
      this.camX = this.camX + this.viewVector.x * offset;
      this.camY = this.camY + this.viewVector.y * offset;
      this.camZ = this.camZ + this.viewVector.z * offset;
      return this;
   }

   public Frustum offsetToFullyIncludeCameraCube(int offset) {
      double d = Math.floor(this.camX / offset) * offset;
      double e = Math.floor(this.camY / offset) * offset;
      double f = Math.floor(this.camZ / offset) * offset;
      double g = Math.ceil(this.camX / offset) * offset;
      double h = Math.ceil(this.camY / offset) * offset;

      for (double i = Math.ceil(this.camZ / offset) * offset;
         this.intersection
               .intersectAab(
                  (float)(d - this.camX),
                  (float)(e - this.camY),
                  (float)(f - this.camZ),
                  (float)(g - this.camX),
                  (float)(h - this.camY),
                  (float)(i - this.camZ)
               )
            != -2;
         this.camZ = this.camZ - this.viewVector.z() * 4.0F
      ) {
         this.camX = this.camX - this.viewVector.x() * 4.0F;
         this.camY = this.camY - this.viewVector.y() * 4.0F;
      }

      return this;
   }

   public void prepare(double camX, double camY, double camZ) {
      this.camX = camX;
      this.camY = camY;
      this.camZ = camZ;
   }

   private void calculateFrustum(Matrix4f frustum, Matrix4f projection) {
      projection.mul(frustum, this.matrix);
      this.intersection.set(this.matrix);
      this.viewVector = this.matrix.transformTranspose(new Vector4f(0.0F, 0.0F, 1.0F, 0.0F));
   }

   public boolean isVisible(AABB aabb) {
      int i = this.cubeInFrustum(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
      return i == -2 || i == -1;
   }

   public int cubeInFrustum(BoundingBox boundingBox) {
      return this.cubeInFrustum(
         boundingBox.minX(), boundingBox.minY(), boundingBox.minZ(), boundingBox.maxX() + 1, boundingBox.maxY() + 1, boundingBox.maxZ() + 1
      );
   }

   private int cubeInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
      float f = (float)(minX - this.camX);
      float g = (float)(minY - this.camY);
      float h = (float)(minZ - this.camZ);
      float i = (float)(maxX - this.camX);
      float j = (float)(maxY - this.camY);
      float k = (float)(maxZ - this.camZ);
      return this.intersection.intersectAab(f, g, h, i, j, k);
   }

   public boolean pointInFrustum(double x, double y, double z) {
      return this.intersection.testPoint((float)(x - this.camX), (float)(y - this.camY), (float)(z - this.camZ));
   }

   public Vector4f[] getFrustumPoints() {
      Vector4f[] vector4fs = new Vector4f[]{
         new Vector4f(-1.0F, -1.0F, -1.0F, 1.0F),
         new Vector4f(1.0F, -1.0F, -1.0F, 1.0F),
         new Vector4f(1.0F, 1.0F, -1.0F, 1.0F),
         new Vector4f(-1.0F, 1.0F, -1.0F, 1.0F),
         new Vector4f(-1.0F, -1.0F, 1.0F, 1.0F),
         new Vector4f(1.0F, -1.0F, 1.0F, 1.0F),
         new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
         new Vector4f(-1.0F, 1.0F, 1.0F, 1.0F)
      };
      Matrix4f matrix4f = this.matrix.invert(new Matrix4f());

      for (int i = 0; i < 8; i++) {
         matrix4f.transform(vector4fs[i]);
         vector4fs[i].div(vector4fs[i].w());
      }

      return vector4fs;
   }

   public double getCamX() {
      return this.camX;
   }

   public double getCamY() {
      return this.camY;
   }

   public double getCamZ() {
      return this.camZ;
   }
}
