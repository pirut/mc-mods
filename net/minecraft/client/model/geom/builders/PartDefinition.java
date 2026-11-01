package net.minecraft.client.model.geom.builders;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;

@Environment(EnvType.CLIENT)
public class PartDefinition {
   private final List<CubeDefinition> cubes;
   private final PartPose partPose;
   private final Map<String, PartDefinition> children = Maps.newHashMap();

   PartDefinition(List<CubeDefinition> cubes, PartPose partPose) {
      this.cubes = cubes;
      this.partPose = partPose;
   }

   public PartDefinition addOrReplaceChild(String name, CubeListBuilder cubes, PartPose partPose) {
      PartDefinition partDefinition = new PartDefinition(cubes.getCubes(), partPose);
      return this.addOrReplaceChild(name, partDefinition);
   }

   public PartDefinition addOrReplaceChild(String name, PartDefinition child) {
      PartDefinition partDefinition = this.children.put(name, child);
      if (partDefinition != null) {
         child.children.putAll(partDefinition.children);
      }

      return child;
   }

   public PartDefinition clearRecursively() {
      for (String string : this.children.keySet()) {
         this.clearChild(string).clearRecursively();
      }

      return this;
   }

   public PartDefinition clearChild(String name) {
      PartDefinition partDefinition = this.children.get(name);
      if (partDefinition == null) {
         throw new IllegalArgumentException("No child with name: " + name);
      } else {
         return this.addOrReplaceChild(name, CubeListBuilder.create(), partDefinition.partPose);
      }
   }

   public void retainPartsAndChildren(Set<String> parts) {
      for (Entry<String, PartDefinition> entry : this.children.entrySet()) {
         PartDefinition partDefinition = entry.getValue();
         if (!parts.contains(entry.getKey())) {
            this.addOrReplaceChild(entry.getKey(), CubeListBuilder.create(), partDefinition.partPose).retainPartsAndChildren(parts);
         }
      }
   }

   public void retainExactParts(Set<String> parts) {
      for (Entry<String, PartDefinition> entry : this.children.entrySet()) {
         PartDefinition partDefinition = entry.getValue();
         if (parts.contains(entry.getKey())) {
            partDefinition.clearRecursively();
         } else {
            this.addOrReplaceChild(entry.getKey(), CubeListBuilder.create(), partDefinition.partPose).retainExactParts(parts);
         }
      }
   }

   public ModelPart bake(int texWidth, int texHeight) {
      Object2ObjectArrayMap<String, ModelPart> object2ObjectArrayMap = this.children
         .entrySet()
         .stream()
         .collect(
            Collectors.toMap(
               Entry::getKey,
               entry -> ((PartDefinition)entry.getValue()).bake(texWidth, texHeight),
               (modelPartx, modelPart2) -> modelPartx,
               Object2ObjectArrayMap::new
            )
         );
      List<ModelPart.Cube> list = this.cubes.stream().map(cubeDefinition -> cubeDefinition.bake(texWidth, texHeight)).toList();
      ModelPart modelPart = new ModelPart(list, object2ObjectArrayMap);
      modelPart.setInitialPose(this.partPose);
      modelPart.loadPose(this.partPose);
      return modelPart;
   }

   public PartDefinition getChild(String name) {
      return this.children.get(name);
   }

   public Set<Entry<String, PartDefinition>> getChildren() {
      return this.children.entrySet();
   }

   public PartDefinition transformed(UnaryOperator<PartPose> transformer) {
      PartDefinition partDefinition = new PartDefinition(this.cubes, transformer.apply(this.partPose));
      partDefinition.children.putAll(this.children);
      return partDefinition;
   }
}
