package net.minecraft.client.data.models.model;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelLocationUtils {
   @Deprecated
   public static Identifier decorateBlockModelLocation(String p_388520_) {
      return Identifier.parse(p_388520_).withPrefix("block/");
   }

   public static Identifier decorateItemModelLocation(String p_387226_) {
      return Identifier.parse(p_387226_).withPrefix("item/");
   }

   public static Identifier getModelLocation(Block p_387758_, String p_388221_) {
      Identifier identifier = BuiltInRegistries.BLOCK.getKey(p_387758_);
      return identifier.withPath(p_388420_ -> "block/" + p_388420_ + p_388221_);
   }

   public static Identifier getModelLocation(Block p_387471_) {
      Identifier identifier = BuiltInRegistries.BLOCK.getKey(p_387471_);
      return identifier.withPrefix("block/");
   }

   public static Identifier getModelLocation(Item p_388071_) {
      Identifier identifier = BuiltInRegistries.ITEM.getKey(p_388071_);
      return identifier.withPrefix("item/");
   }

   public static Identifier getModelLocation(Item p_387888_, String p_388435_) {
      Identifier identifier = BuiltInRegistries.ITEM.getKey(p_387888_);
      return identifier.withPath(p_386751_ -> "item/" + p_386751_ + p_388435_);
   }
}
