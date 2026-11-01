package net.minecraft.client.model.geom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public final class ModelPart {
   public static final float DEFAULT_SCALE = 1.0F;
   public float x;
   public float y;
   public float z;
   public float xRot;
   public float yRot;
   public float zRot;
   public float xScale = 1.0F;
   public float yScale = 1.0F;
   public float zScale = 1.0F;
   public boolean visible = true;
   public boolean skipDraw;
   private final List<ModelPart.Cube> cubes;
   private final Map<String, ModelPart> children;
   private PartPose initialPose = PartPose.ZERO;

   public ModelPart(List<ModelPart.Cube> cubes, Map<String, ModelPart> children) {
      this.cubes = cubes;
      this.children = children;
   }

   public PartPose storePose() {
      return PartPose.offsetAndRotation(this.x, this.y, this.z, this.xRot, this.yRot, this.zRot);
   }

   public PartPose getInitialPose() {
      return this.initialPose;
   }

   public void setInitialPose(PartPose initialPose) {
      this.initialPose = initialPose;
   }

   public void resetPose() {
      this.loadPose(this.initialPose);
   }

   public void loadPose(PartPose partPose) {
      this.x = partPose.x();
      this.y = partPose.y();
      this.z = partPose.z();
      this.xRot = partPose.xRot();
      this.yRot = partPose.yRot();
      this.zRot = partPose.zRot();
      this.xScale = partPose.xScale();
      this.yScale = partPose.yScale();
      this.zScale = partPose.zScale();
   }

   public boolean hasChild(String name) {
      return this.children.containsKey(name);
   }

   public ModelPart getChild(String name) {
      ModelPart modelPart = this.children.get(name);
      if (modelPart == null) {
         throw new NoSuchElementException("Can't find part " + name);
      } else {
         return modelPart;
      }
   }

   public void setPos(float x, float y, float z) {
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public void setRotation(float xRot, float yRot, float zRot) {
      this.xRot = xRot;
      this.yRot = yRot;
      this.zRot = zRot;
   }

   public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
      this.render(poseStack, buffer, packedLight, packedOverlay, -1);
   }

   public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
      if (this.visible) {
         if (!this.cubes.isEmpty() || !this.children.isEmpty()) {
            poseStack.pushPose();
            this.translateAndRotate(poseStack);
            if (!this.skipDraw) {
               this.compile(poseStack.last(), buffer, packedLight, packedOverlay, color);
            }

            for (ModelPart modelPart : this.children.values()) {
               modelPart.render(poseStack, buffer, packedLight, packedOverlay, color);
            }

            poseStack.popPose();
         }
      }
   }

   public void rotateBy(Quaternionf quaternion) {
      Matrix3f matrix3f = new Matrix3f().rotationZYX(this.zRot, this.yRot, this.xRot);
      Matrix3f matrix3f2 = matrix3f.rotate(quaternion);
      Vector3f vector3f = matrix3f2.getEulerAnglesZYX(new Vector3f());
      this.setRotation(vector3f.x, vector3f.y, vector3f.z);
   }

   public void getExtentsForGui(PoseStack poseStack, Consumer<Vector3fc> output) {
      this.visit(poseStack, (pose, string, i, cube) -> {
         for (ModelPart.Polygon polygon : cube.polygons) {
            for (ModelPart.Vertex vertex : polygon.vertices()) {
               float f = vertex.worldX();
               float g = vertex.worldY();
               float h = vertex.worldZ();
               Vector3f vector3f = pose.pose().transformPosition(f, g, h, new Vector3f());
               output.accept(vector3f);
            }
         }
      });
   }

   public void visit(PoseStack poseStack, ModelPart.Visitor visitor) {
      this.visit(poseStack, visitor, "");
   }

   private void visit(PoseStack poseStack, ModelPart.Visitor visitor, String path) {
      if (!this.cubes.isEmpty() || !this.children.isEmpty()) {
         poseStack.pushPose();
         this.translateAndRotate(poseStack);
         PoseStack.Pose pose = poseStack.last();

         for (int i = 0; i < this.cubes.size(); i++) {
            visitor.visit(pose, path, i, this.cubes.get(i));
         }

         String string = path + "/";
         this.children.forEach((string2, modelPart) -> modelPart.visit(poseStack, visitor, string + string2));
         poseStack.popPose();
      }
   }

   public void translateAndRotate(PoseStack poseStack) {
      poseStack.translate(this.x / 16.0F, this.y / 16.0F, this.z / 16.0F);
      if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F) {
         poseStack.mulPose(new Quaternionf().rotationZYX(this.zRot, this.yRot, this.xRot));
      }

      if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
         poseStack.scale(this.xScale, this.yScale, this.zScale);
      }
   }

   private void compile(PoseStack.Pose pose, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
      for (ModelPart.Cube cube : this.cubes) {
         cube.compile(pose, buffer, packedLight, packedOverlay, color);
      }
   }

   public ModelPart.Cube getRandomCube(RandomSource random) {
      return this.cubes.get(random.nextInt(this.cubes.size()));
   }

   public boolean isEmpty() {
      return this.cubes.isEmpty();
   }

   public void offsetPos(Vector3f offset) {
      this.x = this.x + offset.x();
      this.y = this.y + offset.y();
      this.z = this.z + offset.z();
   }

   public void offsetRotation(Vector3f offset) {
      this.xRot = this.xRot + offset.x();
      this.yRot = this.yRot + offset.y();
      this.zRot = this.zRot + offset.z();
   }

   public void offsetScale(Vector3f offset) {
      this.xScale = this.xScale + offset.x();
      this.yScale = this.yScale + offset.y();
      this.zScale = this.zScale + offset.z();
   }

   public List<ModelPart> getAllParts() {
      List<ModelPart> list = new ArrayList<>();
      list.add(this);
      this.addAllChildren((string, modelPart) -> list.add(modelPart));
      return List.copyOf(list);
   }

   public Function<String, ModelPart> createPartLookup() {
      Map<String, ModelPart> map = new HashMap<>();
      map.put("root", this);
      this.addAllChildren(map::putIfAbsent);
      return map::get;
   }

   private void addAllChildren(BiConsumer<String, ModelPart> adder) {
      for (Entry<String, ModelPart> entry : this.children.entrySet()) {
         adder.accept(entry.getKey(), entry.getValue());
      }

      for (ModelPart modelPart : this.children.values()) {
         modelPart.addAllChildren(adder);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class Cube {
      public final ModelPart.Polygon[] polygons;
      public final float minX;
      public final float minY;
      public final float minZ;
      public final float maxX;
      public final float maxY;
      public final float maxZ;

      public Cube(
         int texCoordU,
         int texCoordV,
         float originX,
         float originY,
         float originZ,
         float dimensionX,
         float dimensionY,
         float dimensionZ,
         float growX,
         float growY,
         float growZ,
         boolean mirror,
         float texScaleU,
         float texScaleV,
         Set<Direction> visibleFaces
      ) {
         this.minX = originX;
         this.minY = originY;
         this.minZ = originZ;
         this.maxX = originX + dimensionX;
         this.maxY = originY + dimensionY;
         this.maxZ = originZ + dimensionZ;
         this.polygons = new ModelPart.Polygon[visibleFaces.size()];
         float f = originX + dimensionX;
         float g = originY + dimensionY;
         float h = originZ + dimensionZ;
         originX -= growX;
         originY -= growY;
         originZ -= growZ;
         f += growX;
         g += growY;
         h += growZ;
         if (mirror) {
            float i = f;
            f = originX;
            originX = i;
         }

         ModelPart.Vertex vertex = new ModelPart.Vertex(originX, originY, originZ, 0.0F, 0.0F);
         ModelPart.Vertex vertex2 = new ModelPart.Vertex(f, originY, originZ, 0.0F, 8.0F);
         ModelPart.Vertex vertex3 = new ModelPart.Vertex(f, g, originZ, 8.0F, 8.0F);
         ModelPart.Vertex vertex4 = new ModelPart.Vertex(originX, g, originZ, 8.0F, 0.0F);
         ModelPart.Vertex vertex5 = new ModelPart.Vertex(originX, originY, h, 0.0F, 0.0F);
         ModelPart.Vertex vertex6 = new ModelPart.Vertex(f, originY, h, 0.0F, 8.0F);
         ModelPart.Vertex vertex7 = new ModelPart.Vertex(f, g, h, 8.0F, 8.0F);
         ModelPart.Vertex vertex8 = new ModelPart.Vertex(originX, g, h, 8.0F, 0.0F);
         float j = texCoordU;
         float k = texCoordU + dimensionZ;
         float l = texCoordU + dimensionZ + dimensionX;
         float m = texCoordU + dimensionZ + dimensionX + dimensionX;
         float n = texCoordU + dimensionZ + dimensionX + dimensionZ;
         float o = texCoordU + dimensionZ + dimensionX + dimensionZ + dimensionX;
         float p = texCoordV;
         float q = texCoordV + dimensionZ;
         float r = texCoordV + dimensionZ + dimensionY;
         int s = 0;
         if (visibleFaces.contains(Direction.DOWN)) {
            this.polygons[s++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{vertex6, vertex5, vertex, vertex2}, k, p, l, q, texScaleU, texScaleV, mirror, Direction.DOWN
            );
         }

         if (visibleFaces.contains(Direction.UP)) {
            this.polygons[s++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{vertex3, vertex4, vertex8, vertex7}, l, q, m, p, texScaleU, texScaleV, mirror, Direction.UP
            );
         }

         if (visibleFaces.contains(Direction.WEST)) {
            this.polygons[s++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{vertex, vertex5, vertex8, vertex4}, j, q, k, r, texScaleU, texScaleV, mirror, Direction.WEST
            );
         }

         if (visibleFaces.contains(Direction.NORTH)) {
            this.polygons[s++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{vertex2, vertex, vertex4, vertex3}, k, q, l, r, texScaleU, texScaleV, mirror, Direction.NORTH
            );
         }

         if (visibleFaces.contains(Direction.EAST)) {
            this.polygons[s++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{vertex6, vertex2, vertex3, vertex7}, l, q, n, r, texScaleU, texScaleV, mirror, Direction.EAST
            );
         }

         if (visibleFaces.contains(Direction.SOUTH)) {
            this.polygons[s] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{vertex5, vertex6, vertex7, vertex8}, n, q, o, r, texScaleU, texScaleV, mirror, Direction.SOUTH
            );
         }
      }

      public void compile(PoseStack.Pose pose, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
         Matrix4f matrix4f = pose.pose();
         Vector3f vector3f = new Vector3f();

         for (ModelPart.Polygon polygon : this.polygons) {
            Vector3f vector3f2 = pose.transformNormal(polygon.normal, vector3f);
            float f = vector3f2.x();
            float g = vector3f2.y();
            float h = vector3f2.z();

            for (ModelPart.Vertex vertex : polygon.vertices) {
               float i = vertex.worldX();
               float j = vertex.worldY();
               float k = vertex.worldZ();
               Vector3f vector3f3 = matrix4f.transformPosition(i, j, k, vector3f);
               buffer.addVertex(vector3f3.x(), vector3f3.y(), vector3f3.z(), color, vertex.u, vertex.v, packedOverlay, packedLight, f, g, h);
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public record Polygon(ModelPart.Vertex[] vertices, Vector3fc normal) {

      public Polygon(
         ModelPart.Vertex[] vertices, float u1, float v1, float u2, float v2, float textureWidth, float textureHeight, boolean mirror, Direction direction
      ) {
         this(vertices, (mirror ? mirrorFacing(direction) : direction).getUnitVec3f());
         float f = 0.0F / textureWidth;
         float g = 0.0F / textureHeight;
         vertices[0] = vertices[0].remap(u2 / textureWidth - f, v1 / textureHeight + g);
         vertices[1] = vertices[1].remap(u1 / textureWidth + f, v1 / textureHeight + g);
         vertices[2] = vertices[2].remap(u1 / textureWidth + f, v2 / textureHeight - g);
         vertices[3] = vertices[3].remap(u2 / textureWidth - f, v2 / textureHeight - g);
         if (mirror) {
            int i = vertices.length;

            for (int j = 0; j < i / 2; j++) {
               ModelPart.Vertex vertex = vertices[j];
               vertices[j] = vertices[i - 1 - j];
               vertices[i - 1 - j] = vertex;
            }
         }
      }

      private static Direction mirrorFacing(Direction direction) {
         return direction.getAxis() == Direction.Axis.X ? direction.getOpposite() : direction;
      }
   }

   @Environment(EnvType.CLIENT)
   public record Vertex(float x, float y, float z, float u, float v) {
      public static final float SCALE_FACTOR = 16.0F;

      public ModelPart.Vertex remap(float u, float v) {
         return new ModelPart.Vertex(this.x, this.y, this.z, u, v);
      }

      public float worldX() {
         return this.x / 16.0F;
      }

      public float worldY() {
         return this.y / 16.0F;
      }

      public float worldZ() {
         return this.z / 16.0F;
      }
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface Visitor {
      void visit(PoseStack.Pose pose, String path, int index, ModelPart.Cube cube);
   }
}
