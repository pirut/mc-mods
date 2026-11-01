package net.minecraft.client.renderer.texture.atlas;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class SpriteSourceList {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final FileToIdConverter ATLAS_INFO_CONVERTER = new FileToIdConverter("atlases", ".json");
   private final List<SpriteSource> sources;

   private SpriteSourceList(List<SpriteSource> sources) {
      this.sources = sources;
   }

   public List<SpriteSource.Loader> list(ResourceManager resourceManager) {
      final Map<Identifier, SpriteSource.DiscardableLoader> map = new HashMap<>();
      SpriteSource.Output output = new SpriteSource.Output() {
         @Override
         public void add(Identifier location, SpriteSource.DiscardableLoader loader) {
            SpriteSource.DiscardableLoader discardableLoader = map.put(location, loader);
            if (discardableLoader != null) {
               discardableLoader.discard();
            }
         }

         @Override
         public void removeAll(Predicate<Identifier> predicate) {
            Iterator<Entry<Identifier, SpriteSource.DiscardableLoader>> iterator = map.entrySet().iterator();

            while (iterator.hasNext()) {
               Entry<Identifier, SpriteSource.DiscardableLoader> entry = iterator.next();
               if (predicate.test(entry.getKey())) {
                  entry.getValue().discard();
                  iterator.remove();
               }
            }
         }
      };
      this.sources.forEach(spriteSource -> spriteSource.run(resourceManager, output));
      Builder<SpriteSource.Loader> builder = ImmutableList.builder();
      builder.add((SpriteSource.Loader)spriteResourceLoader -> MissingTextureAtlasSprite.create());
      builder.addAll(map.values());
      return builder.build();
   }

   public static SpriteSourceList load(ResourceManager resourceManager, Identifier sprite) {
      Identifier identifier = ATLAS_INFO_CONVERTER.idToFile(sprite);
      List<SpriteSource> list = new ArrayList<>();

      for (Resource resource : resourceManager.getResourceStack(identifier)) {
         try (BufferedReader bufferedReader = resource.openAsReader()) {
            Dynamic<JsonElement> dynamic = new Dynamic(JsonOps.INSTANCE, StrictJsonParser.parse(bufferedReader));
            list.addAll((Collection<? extends SpriteSource>)SpriteSources.FILE_CODEC.parse(dynamic).getOrThrow());
         } catch (Exception var11) {
            LOGGER.error("Failed to parse atlas definition {} in pack {}", new Object[]{identifier, resource.sourcePackId(), var11});
         }
      }

      return new SpriteSourceList(list);
   }
}
