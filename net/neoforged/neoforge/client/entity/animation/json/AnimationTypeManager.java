package net.neoforged.neoforge.client.entity.animation.json;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableBiMap.Builder;
import com.mojang.serialization.Codec;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.entity.animation.AnimationTarget;
import net.neoforged.neoforge.client.event.RegisterJsonAnimationTypesEvent;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class AnimationTypeManager {
   private static final ImmutableBiMap<ResourceLocation, AnimationTarget> DEFAULT_TARGETS = ImmutableBiMap.of(
      ResourceLocation.withDefaultNamespace("position"),
      AnimationTarget.POSITION,
      ResourceLocation.withDefaultNamespace("rotation"),
      AnimationTarget.ROTATION,
      ResourceLocation.withDefaultNamespace("scale"),
      AnimationTarget.SCALE
   );
   private static final ImmutableBiMap<ResourceLocation, AnimationChannel.Interpolation> DEFAULT_INTERPOLATIONS = ImmutableBiMap.of(
      ResourceLocation.withDefaultNamespace("linear"),
      AnimationChannel.Interpolations.LINEAR,
      ResourceLocation.withDefaultNamespace("catmullrom"),
      AnimationChannel.Interpolations.CATMULLROM
   );
   private static ImmutableBiMap<ResourceLocation, AnimationTarget> TARGETS = DEFAULT_TARGETS;
   private static ImmutableMap<AnimationChannel.Target, AnimationTarget> TARGETS_BY_CHANNEL_TARGET = ImmutableMap.of();
   private static ImmutableMap<AnimationTarget, Codec<Keyframe>> KEYFRAME_CODECS = ImmutableMap.of();
   private static ImmutableBiMap<ResourceLocation, AnimationChannel.Interpolation> INTERPOLATIONS = DEFAULT_INTERPOLATIONS;
   private static String TARGET_LIST = "";
   private static String INTERPOLATION_LIST = "";

   private AnimationTypeManager() {
   }

   @Nullable
   public static AnimationTarget getTarget(ResourceLocation name) {
      return (AnimationTarget)TARGETS.get(name);
   }

   @Nullable
   public static ResourceLocation getTargetName(AnimationTarget target) {
      return (ResourceLocation)TARGETS.inverse().get(target);
   }

   @Nullable
   public static AnimationTarget getTargetFromChannelTarget(AnimationChannel.Target target) {
      return (AnimationTarget)TARGETS_BY_CHANNEL_TARGET.get(target);
   }

   @Nullable
   public static Codec<Keyframe> getKeyframeCodec(AnimationTarget target) {
      return (Codec<Keyframe>)KEYFRAME_CODECS.get(target);
   }

   @Nullable
   public static AnimationChannel.Interpolation getInterpolation(ResourceLocation name) {
      return (AnimationChannel.Interpolation)INTERPOLATIONS.get(name);
   }

   @Nullable
   public static ResourceLocation getInterpolationName(AnimationChannel.Interpolation interpolation) {
      return (ResourceLocation)INTERPOLATIONS.inverse().get(interpolation);
   }

   public static String getTargetList() {
      return TARGET_LIST;
   }

   public static String getInterpolationList() {
      return INTERPOLATION_LIST;
   }

   @Internal
   public static void init() {
      Builder<ResourceLocation, AnimationTarget> targets = ImmutableBiMap.builder().putAll(DEFAULT_TARGETS);
      Builder<ResourceLocation, AnimationChannel.Interpolation> interpolations = ImmutableBiMap.builder().putAll(DEFAULT_INTERPOLATIONS);
      RegisterJsonAnimationTypesEvent event = new RegisterJsonAnimationTypesEvent(targets, interpolations);
      ModLoader.postEventWrapContainerInModOrder(event);
      TARGETS = targets.buildOrThrow();
      INTERPOLATIONS = interpolations.buildOrThrow();
      recomputeDerivedFields();
   }

   private static void recomputeDerivedFields() {
      TARGETS_BY_CHANNEL_TARGET = TARGETS.values().stream().collect(ImmutableMap.toImmutableMap(AnimationTarget::channelTarget, Function.identity()));
      KEYFRAME_CODECS = TARGETS.values().stream().collect(ImmutableMap.toImmutableMap(Function.identity(), AnimationParser::keyframeCodec));
      TARGET_LIST = TARGETS.keySet().stream().map(ResourceLocation::toString).collect(Collectors.joining(", "));
      INTERPOLATION_LIST = INTERPOLATIONS.keySet().stream().map(ResourceLocation::toString).collect(Collectors.joining(", "));
   }

   static {
      recomputeDerivedFields();
   }
}
