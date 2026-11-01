package net.minecraft.client.resources.model;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class Material {
   public static final Comparator<Material> COMPARATOR = Comparator.comparing(Material::atlasLocation).thenComparing(Material::texture);
   private final Identifier atlasLocation;
   private final Identifier texture;
   @Nullable
   private RenderType renderType;

   public Material(Identifier p_468996_, Identifier p_469649_) {
      this.atlasLocation = p_468996_;
      this.texture = p_469649_;
   }

   public Identifier atlasLocation() {
      return this.atlasLocation;
   }

   public Identifier texture() {
      return this.texture;
   }

   public RenderType renderType(Function<Identifier, RenderType> p_119202_) {
      if (this.renderType == null) {
         this.renderType = p_119202_.apply(this.atlasLocation);
      }

      return this.renderType;
   }

   public VertexConsumer buffer(MaterialSet p_433736_, MultiBufferSource p_119195_, Function<Identifier, RenderType> p_119196_) {
      return p_433736_.get(this).wrap(p_119195_.getBuffer(this.renderType(p_119196_)));
   }

   public VertexConsumer buffer(
      MaterialSet p_433991_, MultiBufferSource p_119198_, Function<Identifier, RenderType> p_119199_, boolean p_119200_, boolean p_361164_
   ) {
      return p_433991_.get(this).wrap(ItemRenderer.getFoilBuffer(p_119198_, this.renderType(p_119199_), p_119200_, p_361164_));
   }

   @Override
   public boolean equals(Object p_119206_) {
      if (this == p_119206_) {
         return true;
      } else if (p_119206_ != null && this.getClass() == p_119206_.getClass()) {
         Material material = (Material)p_119206_;
         return this.atlasLocation.equals(material.atlasLocation) && this.texture.equals(material.texture);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.atlasLocation, this.texture);
   }

   @Override
   public String toString() {
      return "Material{atlasLocation=" + this.atlasLocation + ", texture=" + this.texture + "}";
   }
}
