package net.neoforged.neoforge.client.model.obj;

import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.FileNotFoundException;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;

public class ObjLoader implements IGeometryLoader<ObjModel>, ResourceManagerReloadListener {
   public static ObjLoader INSTANCE = new ObjLoader();
   private final Map<ObjModel.ModelSettings, ObjModel> modelCache = Maps.newConcurrentMap();
   private final Map<ResourceLocation, ObjMaterialLibrary> materialCache = Maps.newConcurrentMap();
   private final ResourceManager manager = Minecraft.getInstance().getResourceManager();

   @Override
   public void onResourceManagerReload(ResourceManager resourceManager) {
      this.modelCache.clear();
      this.materialCache.clear();
   }

   public ObjModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
      if (!jsonObject.has("model")) {
         throw new JsonParseException("OBJ Loader requires a 'model' key that points to a valid .OBJ model.");
      } else {
         String modelLocation = jsonObject.get("model").getAsString();
         boolean automaticCulling = GsonHelper.getAsBoolean(jsonObject, "automatic_culling", true);
         boolean shadeQuads = GsonHelper.getAsBoolean(jsonObject, "shade_quads", true);
         boolean flipV = GsonHelper.getAsBoolean(jsonObject, "flip_v", false);
         boolean emissiveAmbient = GsonHelper.getAsBoolean(jsonObject, "emissive_ambient", true);
         String mtlOverride = GsonHelper.getAsString(jsonObject, "mtl_override", null);
         return this.loadModel(
            new ObjModel.ModelSettings(ResourceLocation.parse(modelLocation), automaticCulling, shadeQuads, flipV, emissiveAmbient, mtlOverride)
         );
      }
   }

   public ObjModel loadModel(ObjModel.ModelSettings settings) {
      return this.modelCache.computeIfAbsent(settings, data -> {
         Resource resource = this.manager.getResource(settings.modelLocation()).orElseThrow();

         try {
            ObjModel var5;
            try (ObjTokenizer tokenizer = new ObjTokenizer(resource.open())) {
               var5 = ObjModel.parse(tokenizer, settings);
            }

            return var5;
         } catch (FileNotFoundException var9) {
            throw new RuntimeException("Could not find OBJ model", var9);
         } catch (Exception var10) {
            throw new RuntimeException("Could not read OBJ model", var10);
         }
      });
   }

   public ObjMaterialLibrary loadMaterialLibrary(ResourceLocation materialLocation) {
      return this.materialCache.computeIfAbsent(materialLocation, location -> {
         Resource resource = this.manager.getResource(location).orElseThrow();

         try {
            ObjMaterialLibrary var4;
            try (ObjTokenizer rdr = new ObjTokenizer(resource.open())) {
               var4 = new ObjMaterialLibrary(rdr);
            }

            return var4;
         } catch (FileNotFoundException var8) {
            throw new RuntimeException("Could not find OBJ material library", var8);
         } catch (Exception var9) {
            throw new RuntimeException("Could not read OBJ material library", var9);
         }
      });
   }
}
