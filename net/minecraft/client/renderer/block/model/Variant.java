package net.minecraft.client.renderer.block.model;

import com.mojang.math.Quadrant;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record Variant(Identifier modelLocation, Variant.SimpleModelState modelState) implements BlockModelPart.Unbaked {
   public static final MapCodec<Variant> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_465619_ -> p_465619_.group(
            Identifier.CODEC.fieldOf("model").forGetter(Variant::modelLocation), Variant.SimpleModelState.MAP_CODEC.forGetter(Variant::modelState)
         )
         .apply(p_465619_, Variant::new)
   );
   public static final Codec<Variant> CODEC = MAP_CODEC.codec();

   public Variant(Identifier p_468720_) {
      this(p_468720_, Variant.SimpleModelState.DEFAULT);
   }

   public Variant withXRot(Quadrant p_404895_) {
      return this.withState(this.modelState.withX(p_404895_));
   }

   public Variant withYRot(Quadrant p_404966_) {
      return this.withState(this.modelState.withY(p_404966_));
   }

   public Variant withZRot(Quadrant p_470694_) {
      return this.withState(this.modelState.withZ(p_470694_));
   }

   public Variant withUvLock(boolean p_405100_) {
      return this.withState(this.modelState.withUvLock(p_405100_));
   }

   public Variant withModel(Identifier p_469406_) {
      return new Variant(p_469406_, this.modelState);
   }

   public Variant withState(Variant.SimpleModelState p_404847_) {
      return new Variant(this.modelLocation, p_404847_);
   }

   public Variant with(VariantMutator p_405775_) {
      return p_405775_.apply(this);
   }

   @Override
   public BlockModelPart bake(ModelBaker p_410762_) {
      return SimpleModelWrapper.bake(p_410762_, this.modelLocation, this.modelState.asModelState());
   }

   @Override
   public void resolveDependencies(ResolvableModel.Resolver p_410425_) {
      p_410425_.markDependency(this.modelLocation);
   }

   @OnlyIn(Dist.CLIENT)
   public record SimpleModelState(Quadrant x, Quadrant y, Quadrant z, boolean uvLock) {
      public static final MapCodec<Variant.SimpleModelState> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_470510_ -> p_470510_.group(
               Quadrant.CODEC.optionalFieldOf("x", Quadrant.R0).forGetter(Variant.SimpleModelState::x),
               Quadrant.CODEC.optionalFieldOf("y", Quadrant.R0).forGetter(Variant.SimpleModelState::y),
               Quadrant.CODEC.optionalFieldOf("z", Quadrant.R0).forGetter(Variant.SimpleModelState::z),
               Codec.BOOL.optionalFieldOf("uvlock", false).forGetter(Variant.SimpleModelState::uvLock)
            )
            .apply(p_470510_, Variant.SimpleModelState::new)
      );
      public static final Variant.SimpleModelState DEFAULT = new Variant.SimpleModelState(Quadrant.R0, Quadrant.R0, Quadrant.R0, false);

      public ModelState asModelState() {
         BlockModelRotation blockmodelrotation = BlockModelRotation.get(Quadrant.fromXYZAngles(this.x, this.y, this.z));
         return (ModelState)(this.uvLock ? blockmodelrotation.withUvLock() : blockmodelrotation);
      }

      public Variant.SimpleModelState withX(Quadrant p_405203_) {
         return new Variant.SimpleModelState(p_405203_, this.y, this.z, this.uvLock);
      }

      public Variant.SimpleModelState withY(Quadrant p_405288_) {
         return new Variant.SimpleModelState(this.x, p_405288_, this.z, this.uvLock);
      }

      public Variant.SimpleModelState withZ(Quadrant p_470806_) {
         return new Variant.SimpleModelState(this.x, this.y, p_470806_, this.uvLock);
      }

      public Variant.SimpleModelState withUvLock(boolean p_405150_) {
         return new Variant.SimpleModelState(this.x, this.y, this.z, p_405150_);
      }
   }
}
