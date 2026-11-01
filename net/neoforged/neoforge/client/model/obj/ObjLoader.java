package net.neoforged.neoforge.client.model.obj;

import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.FileNotFoundException;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;

public class ObjLoader implements UnbakedModelLoader<ObjModel>, ResourceManagerReloadListener {
   public static ObjLoader INSTANCE = new ObjLoader();
   private final Map<ObjGeometry.Settings, ObjGeometry> geometryCache = Maps.newConcurrentMap();
   private final Map<Identifier, ObjMaterialLibrary> materialCache = Maps.newConcurrentMap();
   private final ResourceManager manager = Minecraft.getInstance().getResourceManager();

   @Override
   public void onResourceManagerReload(ResourceManager resourceManager) {
      this.geometryCache.clear();
      this.materialCache.clear();
   }

   public ObjModel read(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
      if (!jsonObject.has("model")) {
         throw new JsonParseException("OBJ Loader requires a 'model' key that points to a valid .OBJ model.");
      } else {
         String modelLocation = jsonObject.get("model").getAsString();
         boolean automaticCulling = GsonHelper.getAsBoolean(jsonObject, "automatic_culling", true);
         boolean shadeQuads = GsonHelper.getAsBoolean(jsonObject, "shade_quads", true);
         boolean flipV = GsonHelper.getAsBoolean(jsonObject, "flip_v", false);
         boolean emissiveAmbient = GsonHelper.getAsBoolean(jsonObject, "emissive_ambient", true);
         String mtlOverride = GsonHelper.getAsString(jsonObject, "mtl_override", null);
         StandardModelParameters parameters = StandardModelParameters.parse(jsonObject, jsonDeserializationContext);
         ObjGeometry geometry = this.loadGeometry(
            new ObjGeometry.Settings(Identifier.parse(modelLocation), automaticCulling, shadeQuads, flipV, emissiveAmbient, mtlOverride)
         );
         return new ObjModel(parameters, geometry);
      }
   }

   public ObjGeometry loadGeometry(ObjGeometry.Settings settings) {
      return this.geometryCache.computeIfAbsent(settings, data -> {
         Resource resource = this.manager.getResource(settings.modelLocation()).orElseThrow();

         try {
            ObjGeometry var5;
            try (ObjTokenizer tokenizer = new ObjTokenizer(resource.open())) {
               var5 = ObjGeometry.parse(tokenizer, data);
            }

            return var5;
         } catch (FileNotFoundException var9) {
            throw new RuntimeException("Could not find OBJ model", var9);
         } catch (Exception var10) {
            throw new RuntimeException("Could not read OBJ model", var10);
         }
      });
   }

   public ObjMaterialLibrary loadMaterialLibrary(Identifier materialLocation) {
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
