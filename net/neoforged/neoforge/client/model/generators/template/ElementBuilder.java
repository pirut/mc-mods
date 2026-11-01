package net.neoforged.neoforge.client.model.generators.template;

import com.google.common.base.Preconditions;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class ElementBuilder {
   private Vector3f from = new Vector3f();
   private Vector3f to = new Vector3f(16.0F, 16.0F, 16.0F);
   private final Map<Direction, FaceBuilder> faces = new LinkedHashMap<>();
   @Nullable
   private RotationBuilder rotation;
   private boolean shade = true;
   private int lightEmission = 0;
   private int color = -1;
   private boolean hasAmbientOcclusion = true;

   public ElementBuilder from(float x, float y, float z) {
      this.from = new Vector3f(x, y, z);
      validatePosition(this.from);
      return this;
   }

   public ElementBuilder to(float x, float y, float z) {
      this.to = new Vector3f(x, y, z);
      validatePosition(this.to);
      return this;
   }

   public ElementBuilder face(Direction dir, Consumer<FaceBuilder> action) {
      Preconditions.checkNotNull(dir, "Direction must not be null");
      FaceBuilder builder = this.faces.computeIfAbsent(dir, $ -> new FaceBuilder());
      action.accept(builder);
      return this;
   }

   public ElementBuilder rotation(Consumer<RotationBuilder> action) {
      if (this.rotation == null) {
         this.rotation = new RotationBuilder();
      }

      action.accept(this.rotation);
      return this;
   }

   public ElementBuilder shade(boolean shade) {
      this.shade = shade;
      return this;
   }

   public ElementBuilder allFaces(BiConsumer<Direction, FaceBuilder> action) {
      Stream.of(Direction.values()).forEach(d -> this.face(d, b -> action.accept(d, b)));
      return this;
   }

   public ElementBuilder allFacesExcept(BiConsumer<Direction, FaceBuilder> action, Set<Direction> exc) {
      Stream.of(Direction.values()).filter(d -> !exc.contains(d)).forEach(d -> this.face(d, b -> action.accept(d, b)));
      return this;
   }

   public ElementBuilder faces(BiConsumer<Direction, FaceBuilder> action) {
      this.faces.forEach(action);
      return this;
   }

   public ElementBuilder textureAll(TextureSlot texture) {
      return this.allFaces(addTexture(texture));
   }

   public ElementBuilder texture(TextureSlot texture) {
      return this.faces(addTexture(texture));
   }

   public ElementBuilder cube(TextureSlot texture) {
      return this.allFaces(addTexture(texture).andThen((dir, f) -> f.cullface(dir)));
   }

   public ElementBuilder lightEmission(int lightEmission) {
      Preconditions.checkArgument(lightEmission < 16 && lightEmission >= 0, "lightEmission %s not in valid range - [0,15)", lightEmission);
      this.lightEmission = lightEmission;
      return this;
   }

   public ElementBuilder color(int color) {
      this.color = color;
      return this;
   }

   public ElementBuilder ambientOcclusion(boolean ambientOcclusion) {
      this.hasAmbientOcclusion = ambientOcclusion;
      return this;
   }

   private static BiConsumer<Direction, FaceBuilder> addTexture(TextureSlot texture) {
      return ($, f) -> f.texture(texture);
   }

   private static void validateCoordinate(float coord, char name) {
      Preconditions.checkArgument(!(coord < -16.0F) && !(coord > 32.0F), "Position " + name + " out of range, must be within [-16, 32]. Found: %d", coord);
   }

   private static void validatePosition(Vector3f pos) {
      validateCoordinate(pos.x(), 'x');
      validateCoordinate(pos.y(), 'y');
      validateCoordinate(pos.z(), 'z');
   }

   BlockElement build() {
      Map<Direction, BlockElementFace> faces = this.faces
         .entrySet()
         .stream()
         .collect(Collectors.toMap(Entry::getKey, e -> ((FaceBuilder)e.getValue()).build(), (k1, k2) -> {
            throw new IllegalArgumentException();
         }, LinkedHashMap::new));
      return new BlockElement(
         this.from,
         this.to,
         faces,
         this.rotation == null ? null : this.rotation.build(),
         this.shade,
         this.lightEmission,
         new ExtraFaceData(this.color, 0, this.hasAmbientOcclusion)
      );
   }

   ElementBuilder copy() {
      ElementBuilder builder = new ElementBuilder();
      builder.from.set(this.from);
      builder.to.set(this.to);
      this.faces.forEach((side, faceBuilder) -> builder.faces.put(side, faceBuilder.copy()));
      builder.rotation = this.rotation != null ? this.rotation.copy() : null;
      builder.shade = this.shade;
      builder.lightEmission = this.lightEmission;
      builder.color = this.color;
      builder.hasAmbientOcclusion = this.hasAmbientOcclusion;
      return builder;
   }
}
