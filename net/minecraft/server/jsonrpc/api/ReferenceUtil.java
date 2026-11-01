package net.minecraft.server.jsonrpc.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.net.URI;
import java.net.URISyntaxException;

public class ReferenceUtil {
   public static final Codec<URI> REFERENCE_CODEC = Codec.STRING.comapFlatMap(p_451105_ -> {
      try {
         return DataResult.success(new URI(p_451105_));
      } catch (URISyntaxException var2) {
         return DataResult.error(var2::getMessage);
      }
   }, URI::toString);

   public static URI createLocalReference(String p_451224_) {
      return URI.create("#/components/schemas/" + p_451224_);
   }
}
