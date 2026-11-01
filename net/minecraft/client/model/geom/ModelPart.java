package net.minecraft.client.model.geom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
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

   public ModelPart(List<ModelPart.Cube> p_171306_, Map<String, ModelPart> p_171307_) {
      this.cubes = p_171306_;
      this.children = p_171307_;
   }

   public PartPose storePose() {
      return PartPose.offsetAndRotation(this.x, this.y, this.z, this.xRot, this.yRot, this.zRot);
   }

   public PartPose getInitialPose() {
      return this.initialPose;
   }

   public void setInitialPose(PartPose p_233561_) {
      this.initialPose = p_233561_;
   }

   public void resetPose() {
      this.loadPose(this.initialPose);
   }

   public void loadPose(PartPose p_171323_) {
      this.x = p_171323_.x;
      this.y = p_171323_.y;
      this.z = p_171323_.z;
      this.xRot = p_171323_.xRot;
      this.yRot = p_171323_.yRot;
      this.zRot = p_171323_.zRot;
      this.xScale = 1.0F;
      this.yScale = 1.0F;
      this.zScale = 1.0F;
   }

   public void copyFrom(ModelPart p_104316_) {
      this.xScale = p_104316_.xScale;
      this.yScale = p_104316_.yScale;
      this.zScale = p_104316_.zScale;
      this.xRot = p_104316_.xRot;
      this.yRot = p_104316_.yRot;
      this.zRot = p_104316_.zRot;
      this.x = p_104316_.x;
      this.y = p_104316_.y;
      this.z = p_104316_.z;
   }

   public boolean hasChild(String p_233563_) {
      return this.children.containsKey(p_233563_);
   }

   public ModelPart getChild(String p_171325_) {
      ModelPart $$1 = this.children.get(p_171325_);
      if ($$1 == null) {
         throw new NoSuchElementException("Can't find part " + p_171325_);
      } else {
         return $$1;
      }
   }

   public void setPos(float p_104228_, float p_104229_, float p_104230_) {
      this.x = p_104228_;
      this.y = p_104229_;
      this.z = p_104230_;
   }

   public void setRotation(float p_171328_, float p_171329_, float p_171330_) {
      this.xRot = p_171328_;
      this.yRot = p_171329_;
      this.zRot = p_171330_;
   }

   public void render(PoseStack p_104302_, VertexConsumer p_104303_, int p_104304_, int p_104305_) {
      this.render(p_104302_, p_104303_, p_104304_, p_104305_, -1);
   }

   public void render(PoseStack p_104307_, VertexConsumer p_104308_, int p_104309_, int p_104310_, int p_350978_) {
      if (this.visible) {
         if (!this.cubes.isEmpty() || !this.children.isEmpty()) {
            p_104307_.pushPose();
            this.translateAndRotate(p_104307_);
            if (!this.skipDraw) {
               this.compile(p_104307_.last(), p_104308_, p_104309_, p_104310_, p_350978_);
            }

            for (ModelPart $$5 : this.children.values()) {
               $$5.render(p_104307_, p_104308_, p_104309_, p_104310_, p_350978_);
            }

            p_104307_.popPose();
         }
      }
   }

   public void visit(PoseStack p_171310_, ModelPart.Visitor p_171311_) {
      this.visit(p_171310_, p_171311_, "");
   }

   private void visit(PoseStack p_171313_, ModelPart.Visitor p_171314_, String p_171315_) {
      if (!this.cubes.isEmpty() || !this.children.isEmpty()) {
         p_171313_.pushPose();
         this.translateAndRotate(p_171313_);
         PoseStack.Pose $$3 = p_171313_.last();

         for (int $$4 = 0; $$4 < this.cubes.size(); $$4++) {
            p_171314_.visit($$3, p_171315_, $$4, this.cubes.get($$4));
         }

         String $$5 = p_171315_ + "/";
         this.children.forEach((p_171320_, p_171321_) -> p_171321_.visit(p_171313_, p_171314_, $$5 + p_171320_));
         p_171313_.popPose();
      }
   }

   public void translateAndRotate(PoseStack p_104300_) {
      p_104300_.translate(this.x / 16.0F, this.y / 16.0F, this.z / 16.0F);
      if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F) {
         p_104300_.mulPose(new Quaternionf().rotationZYX(this.zRot, this.yRot, this.xRot));
      }

      if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
         p_104300_.scale(this.xScale, this.yScale, this.zScale);
      }
   }

   private void compile(PoseStack.Pose p_104291_, VertexConsumer p_104292_, int p_104293_, int p_104294_, int p_350388_) {
      for (ModelPart.Cube $$5 : this.cubes) {
         $$5.compile(p_104291_, p_104292_, p_104293_, p_104294_, p_350388_);
      }
   }

   public ModelPart.Cube getRandomCube(RandomSource p_233559_) {
      return this.cubes.get(p_233559_.nextInt(this.cubes.size()));
   }

   public boolean isEmpty() {
      return this.cubes.isEmpty();
   }

   public void offsetPos(Vector3f p_253873_) {
      this.x = this.x + p_253873_.x();
      this.y = this.y + p_253873_.y();
      this.z = this.z + p_253873_.z();
   }

   public void offsetRotation(Vector3f p_253983_) {
      this.xRot = this.xRot + p_253983_.x();
      this.yRot = this.yRot + p_253983_.y();
      this.zRot = this.zRot + p_253983_.z();
   }

   public void offsetScale(Vector3f p_253957_) {
      this.xScale = this.xScale + p_253957_.x();
      this.yScale = this.yScale + p_253957_.y();
      this.zScale = this.zScale + p_253957_.z();
   }

   public Stream<ModelPart> getAllParts() {
      return Stream.concat(Stream.of(this), this.children.values().stream().flatMap(ModelPart::getAllParts));
   }

   @OnlyIn(Dist.CLIENT)
   public static class Cube {
      private final ModelPart.Polygon[] polygons;
      public final float minX;
      public final float minY;
      public final float minZ;
      public final float maxX;
      public final float maxY;
      public final float maxZ;

      public Cube(
         int p_273701_,
         int p_273034_,
         float p_272824_,
         float p_273777_,
         float p_273748_,
         float p_273722_,
         float p_273763_,
         float p_272823_,
         float p_272945_,
         float p_272790_,
         float p_272870_,
         boolean p_273589_,
         float p_273591_,
         float p_273313_,
         Set<Direction> p_273291_
      ) {
         this.minX = p_272824_;
         this.minY = p_273777_;
         this.minZ = p_273748_;
         this.maxX = p_272824_ + p_273722_;
         this.maxY = p_273777_ + p_273763_;
         this.maxZ = p_273748_ + p_272823_;
         this.polygons = new ModelPart.Polygon[p_273291_.size()];
         float $$15 = p_272824_ + p_273722_;
         float $$16 = p_273777_ + p_273763_;
         float $$17 = p_273748_ + p_272823_;
         p_272824_ -= p_272945_;
         p_273777_ -= p_272790_;
         p_273748_ -= p_272870_;
         $$15 += p_272945_;
         $$16 += p_272790_;
         $$17 += p_272870_;
         if (p_273589_) {
            float $$18 = $$15;
            $$15 = p_272824_;
            p_272824_ = $$18;
         }

         ModelPart.Vertex $$19 = new ModelPart.Vertex(p_272824_, p_273777_, p_273748_, 0.0F, 0.0F);
         ModelPart.Vertex $$20 = new ModelPart.Vertex($$15, p_273777_, p_273748_, 0.0F, 8.0F);
         ModelPart.Vertex $$21 = new ModelPart.Vertex($$15, $$16, p_273748_, 8.0F, 8.0F);
         ModelPart.Vertex $$22 = new ModelPart.Vertex(p_272824_, $$16, p_273748_, 8.0F, 0.0F);
         ModelPart.Vertex $$23 = new ModelPart.Vertex(p_272824_, p_273777_, $$17, 0.0F, 0.0F);
         ModelPart.Vertex $$24 = new ModelPart.Vertex($$15, p_273777_, $$17, 0.0F, 8.0F);
         ModelPart.Vertex $$25 = new ModelPart.Vertex($$15, $$16, $$17, 8.0F, 8.0F);
         ModelPart.Vertex $$26 = new ModelPart.Vertex(p_272824_, $$16, $$17, 8.0F, 0.0F);
         float $$27 = p_273701_;
         float $$28 = p_273701_ + p_272823_;
         float $$29 = p_273701_ + p_272823_ + p_273722_;
         float $$30 = p_273701_ + p_272823_ + p_273722_ + p_273722_;
         float $$31 = p_273701_ + p_272823_ + p_273722_ + p_272823_;
         float $$32 = p_273701_ + p_272823_ + p_273722_ + p_272823_ + p_273722_;
         float $$33 = p_273034_;
         float $$34 = p_273034_ + p_272823_;
         float $$35 = p_273034_ + p_272823_ + p_273763_;
         int $$36 = 0;
         if (p_273291_.contains(Direction.DOWN)) {
            this.polygons[$$36++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{$$24, $$23, $$19, $$20}, $$28, $$33, $$29, $$34, p_273591_, p_273313_, p_273589_, Direction.DOWN
            );
         }

         if (p_273291_.contains(Direction.UP)) {
            this.polygons[$$36++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{$$21, $$22, $$26, $$25}, $$29, $$34, $$30, $$33, p_273591_, p_273313_, p_273589_, Direction.UP
            );
         }

         if (p_273291_.contains(Direction.WEST)) {
            this.polygons[$$36++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{$$19, $$23, $$26, $$22}, $$27, $$34, $$28, $$35, p_273591_, p_273313_, p_273589_, Direction.WEST
            );
         }

         if (p_273291_.contains(Direction.NORTH)) {
            this.polygons[$$36++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{$$20, $$19, $$22, $$21}, $$28, $$34, $$29, $$35, p_273591_, p_273313_, p_273589_, Direction.NORTH
            );
         }

         if (p_273291_.contains(Direction.EAST)) {
            this.polygons[$$36++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{$$24, $$20, $$21, $$25}, $$29, $$34, $$31, $$35, p_273591_, p_273313_, p_273589_, Direction.EAST
            );
         }

         if (p_273291_.contains(Direction.SOUTH)) {
            this.polygons[$$36] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{$$23, $$24, $$25, $$26}, $$31, $$34, $$32, $$35, p_273591_, p_273313_, p_273589_, Direction.SOUTH
            );
         }
      }

      public void compile(PoseStack.Pose p_171333_, VertexConsumer p_171334_, int p_171335_, int p_171336_, int p_350744_) {
         Matrix4f $$5 = p_171333_.pose();
         Vector3f $$6 = new Vector3f();

         for (ModelPart.Polygon $$7 : this.polygons) {
            Vector3f $$8 = p_171333_.transformNormal($$7.normal, $$6);
            float $$9 = $$8.x();
            float $$10 = $$8.y();
            float $$11 = $$8.z();

            for (ModelPart.Vertex $$12 : $$7.vertices) {
               float $$13 = $$12.pos.x() / 16.0F;
               float $$14 = $$12.pos.y() / 16.0F;
               float $$15 = $$12.pos.z() / 16.0F;
               Vector3f $$16 = $$5.transformPosition($$13, $$14, $$15, $$6);
               p_171334_.addVertex($$16.x(), $$16.y(), $$16.z(), p_350744_, $$12.u, $$12.v, p_171336_, p_171335_, $$9, $$10, $$11);
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class Polygon {
      public final ModelPart.Vertex[] vertices;
      public final Vector3f normal;

      public Polygon(
         ModelPart.Vertex[] p_104362_,
         float p_104363_,
         float p_104364_,
         float p_104365_,
         float p_104366_,
         float p_104367_,
         float p_104368_,
         boolean p_104369_,
         Direction p_104370_
      ) {
         this.vertices = p_104362_;
         float $$9 = 0.0F / p_104367_;
         float $$10 = 0.0F / p_104368_;
         p_104362_[0] = p_104362_[0].remap(p_104365_ / p_104367_ - $$9, p_104364_ / p_104368_ + $$10);
         p_104362_[1] = p_104362_[1].remap(p_104363_ / p_104367_ + $$9, p_104364_ / p_104368_ + $$10);
         p_104362_[2] = p_104362_[2].remap(p_104363_ / p_104367_ + $$9, p_104366_ / p_104368_ - $$10);
         p_104362_[3] = p_104362_[3].remap(p_104365_ / p_104367_ - $$9, p_104366_ / p_104368_ - $$10);
         if (p_104369_) {
            int $$11 = p_104362_.length;

            for (int $$12 = 0; $$12 < $$11 / 2; $$12++) {
               ModelPart.Vertex $$13 = p_104362_[$$12];
               p_104362_[$$12] = p_104362_[$$11 - 1 - $$12];
               p_104362_[$$11 - 1 - $$12] = $$13;
            }
         }

         this.normal = p_104370_.step();
         if (p_104369_) {
            this.normal.mul(-1.0F, 1.0F, 1.0F);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class Vertex {
      public final Vector3f pos;
      public final float u;
      public final float v;

      public Vertex(float p_104375_, float p_104376_, float p_104377_, float p_104378_, float p_104379_) {
         this(new Vector3f(p_104375_, p_104376_, p_104377_), p_104378_, p_104379_);
      }

      public ModelPart.Vertex remap(float p_104385_, float p_104386_) {
         return new ModelPart.Vertex(this.pos, p_104385_, p_104386_);
      }

      public Vertex(Vector3f p_253667_, float p_253662_, float p_254308_) {
         this.pos = p_253667_;
         this.u = p_253662_;
         this.v = p_254308_;
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface Visitor {
      void visit(PoseStack.Pose var1, String var2, int var3, ModelPart.Cube var4);
   }
}
