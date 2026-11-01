package net.neoforged.neoforge.client.model.generators.template;

import com.google.common.base.Preconditions;
import com.mojang.math.Quadrant;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

public final class FaceBuilder {
   @Nullable
   private Direction cullface;
   private int tintindex = -1;
   @Nullable
   private TextureSlot texture = null;
   @Nullable
   private BlockElementFace.UVs uvs;
   private Quadrant rotation = Quadrant.R0;
   private int color = -1;
   private int lightEmission = 0;
   private boolean hasAmbientOcclusion = true;

   public FaceBuilder cullface(@Nullable Direction dir) {
      this.cullface = dir;
      return this;
   }

   public FaceBuilder tintindex(int index) {
      this.tintindex = index;
      return this;
   }

   public FaceBuilder texture(TextureSlot texture) {
      Preconditions.checkNotNull(texture, "Texture must not be null");
      this.texture = texture;
      return this;
   }

   public FaceBuilder uvs(float u1, float v1, float u2, float v2) {
      this.uvs = new BlockElementFace.UVs(u1, v1, u2, v2);
      return this;
   }

   public FaceBuilder rotation(Quadrant rot) {
      Preconditions.checkNotNull(rot, "Rotation must not be null");
      this.rotation = rot;
      return this;
   }

   public FaceBuilder lightEmission(int lightEmission) {
      this.lightEmission = lightEmission;
      return this;
   }

   public FaceBuilder color(int color) {
      this.color = color;
      return this;
   }

   public FaceBuilder ambientOcclusion(boolean ambientOcclusion) {
      this.hasAmbientOcclusion = ambientOcclusion;
      return this;
   }

   BlockElementFace build() {
      if (this.texture == null) {
         throw new IllegalStateException("A model face must have a texture");
      } else {
         return new BlockElementFace(
            this.cullface,
            this.tintindex,
            this.texture.toString(),
            this.uvs,
            this.rotation,
            new ExtraFaceData(this.color, this.lightEmission, this.hasAmbientOcclusion),
            new MutableObject()
         );
      }
   }

   FaceBuilder copy() {
      FaceBuilder builder = new FaceBuilder();
      builder.texture = this.texture;
      builder.color = this.color;
      builder.cullface = this.cullface;
      builder.tintindex = this.tintindex;
      builder.uvs = this.uvs;
      builder.rotation = this.rotation;
      builder.lightEmission = this.lightEmission;
      builder.hasAmbientOcclusion = this.hasAmbientOcclusion;
      return builder;
   }
}
