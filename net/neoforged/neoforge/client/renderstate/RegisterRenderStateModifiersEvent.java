package net.neoforged.neoforge.client.renderstate;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.function.BiConsumer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterRenderStateModifiersEvent extends Event implements IModBusEvent {
   public <E extends Entity, S extends EntityRenderState> void registerEntityModifier(
      TypeToken<? extends EntityRenderer<? extends E, ? extends S>> baseRenderer, BiConsumer<E, S> modifier
   ) {
      ensureParametersMatchBounds(baseRenderer);
      RenderStateExtensions.registerEntity(baseRenderer.getRawType(), modifier);
   }

   public <E extends Entity, S extends EntityRenderState> void registerEntityModifier(
      Class<? extends EntityRenderer<? extends E, ? extends S>> baseRenderer, BiConsumer<E, S> modifier
   ) {
      ensureParametersMatchBounds(TypeToken.of(baseRenderer));
      RenderStateExtensions.registerEntity(baseRenderer, modifier);
   }

   public void registerMapModifier(BiConsumer<MapItemSavedData, MapRenderState> modifier) {
      RenderStateExtensions.registerMap(modifier);
   }

   public void registerMapDecorationModifier(ResourceKey<MapDecorationType> mapDecorationTypeKey, MapDecorationRenderStateModifier modifier) {
      RenderStateExtensions.registerMapDecoration(mapDecorationTypeKey, modifier);
   }

   private static void ensureParametersMatchBounds(TypeToken<? extends EntityRenderer<? extends Entity, ? extends EntityRenderState>> baseRenderer) {
      if (baseRenderer.getType() instanceof ParameterizedType parameterizedType) {
         Class<?> bound = baseRenderer.getRawType();
         ParameterizedType parameterized = parameterizedType;

         do {
            Type[] userArgs = parameterized.getActualTypeArguments();
            TypeVariable<? extends Class<?>>[] typeArgs = bound.getTypeParameters();

            for (int i = 0; i < userArgs.length; i++) {
               Type userArg = userArgs[i];
               TypeToken<? extends RegisterRenderStateModifiersEvent.Container<?>> userToken = RegisterRenderStateModifiersEvent.Container.of(
                  TypeToken.of(userArg)
               );
               TypeVariable<? extends Class<?>> typeArg = typeArgs[i];

               for (Type singleBound : typeArg.getBounds()) {
                  TypeToken<? extends RegisterRenderStateModifiersEvent.Container<?>> token = RegisterRenderStateModifiersEvent.Container.of(
                     TypeToken.of(singleBound)
                  );
                  if (!token.isSubtypeOf(userToken)) {
                     throw new IllegalArgumentException("%s does not match expected type parameter %s".formatted(userArg, singleBound));
                  }
               }
            }

            if (!(parameterized.getOwnerType() instanceof ParameterizedType parameterizedOwner)) {
               break;
            }

            parameterized = parameterizedOwner;
            bound = bound.getEnclosingClass();
         } while (bound != null);
      }
   }

   private record Container<X>() {
      private static <Z> TypeToken<RegisterRenderStateModifiersEvent.Container<Z>> of(TypeToken<Z> parameter) {
         return (new TypeToken<RegisterRenderStateModifiersEvent.Container<Z>>() {}).where(new TypeParameter<Z>() {}, parameter);
      }
   }
}
