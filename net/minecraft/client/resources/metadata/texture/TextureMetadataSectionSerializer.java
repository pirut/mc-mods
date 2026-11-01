package net.minecraft.client.resources.metadata.texture;

import com.google.gson.JsonObject;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.util.GsonHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TextureMetadataSectionSerializer implements MetadataSectionSerializer<TextureMetadataSection> {
   public TextureMetadataSection fromJson(JsonObject p_119122_) {
      boolean $$1 = GsonHelper.getAsBoolean(p_119122_, "blur", false);
      boolean $$2 = GsonHelper.getAsBoolean(p_119122_, "clamp", false);
      return new TextureMetadataSection($$1, $$2);
   }

   @Override
   public String getMetadataSectionName() {
      return "texture";
   }
}
