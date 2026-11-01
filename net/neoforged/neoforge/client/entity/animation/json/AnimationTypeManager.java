package net.neoforged.neoforge.client.entity.animation.json;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableBiMap.Builder;
import com.mojang.serialization.Codec;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.entity.animation.AnimationTarget;
import net.neoforged.neoforge.client.event.RegisterJsonAnimationTypesEvent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public final class AnimationTypeManager {
   private static final ImmutableBiMap<Identifier, AnimationTarget> DEFAULT_TARGETS = ImmutableBiMap.of(
      Identifier.withDefaultNamespace("position"),
      AnimationTarget.POSITION,
      Identifier.withDefaultNamespace("rotation"),
      AnimationTarget.ROTATION,
      Identifier.withDefaultNamespace("scale"),
      AnimationTarget.SCALE
   );
   private static final ImmutableBiMap<Identifier, AnimationChannel.Interpolation> DEFAULT_INTERPOLATIONS = ImmutableBiMap.of(
      Identifier.withDefaultNamespace("linear"),
      AnimationChannel.Interpolations.LINEAR,
      Identifier.withDefaultNamespace("catmullrom"),
      AnimationChannel.Interpolations.CATMULLROM
   );
   private static ImmutableBiMap<Identifier, AnimationTarget> TARGETS = DEFAULT_TARGETS;
   private static ImmutableMap<AnimationChannel.Target, AnimationTarget> TARGETS_BY_CHANNEL_TARGET = ImmutableMap.of();
   private static ImmutableMap<AnimationTarget, Codec<Keyframe>> KEYFRAME_CODECS = ImmutableMap.of();
   private static ImmutableBiMap<Identifier, AnimationChannel.Interpolation> INTERPOLATIONS = DEFAULT_INTERPOLATIONS;
   private static String TARGET_LIST = "";
   private static String INTERPOLATION_LIST = "";

   private AnimationTypeManager() {
   }

   @Nullable
   public static AnimationTarget getTarget(Identifier name) {
      return (AnimationTarget)TARGETS.get(name);
   }

   @Nullable
   public static Identifier getTargetName(AnimationTarget target) {
      return (Identifier)TARGETS.inverse().get(target);
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
   public static AnimationChannel.Interpolation getInterpolation(Identifier name) {
      return (AnimationChannel.Interpolation)INTERPOLATIONS.get(name);
   }

   @Nullable
   public static Identifier getInterpolationName(AnimationChannel.Interpolation interpolation) {
      return (Identifier)INTERPOLATIONS.inverse().get(interpolation);
   }

   public static String getTargetList() {
      return TARGET_LIST;
   }

   public static String getInterpolationList() {
      return INTERPOLATION_LIST;
   }

   @Internal
   public static void init() {
      Builder<Identifier, AnimationTarget> targets = ImmutableBiMap.builder().putAll(DEFAULT_TARGETS);
      Builder<Identifier, AnimationChannel.Interpolation> interpolations = ImmutableBiMap.builder().putAll(DEFAULT_INTERPOLATIONS);
      RegisterJsonAnimationTypesEvent event = new RegisterJsonAnimationTypesEvent(targets, interpolations);
      ModLoader.postEventWrapContainerInModOrder(event);
      TARGETS = targets.buildOrThrow();
      INTERPOLATIONS = interpolations.buildOrThrow();
      recomputeDerivedFields();
   }

   private static void recomputeDerivedFields() {
      TARGETS_BY_CHANNEL_TARGET = TARGETS.values().stream().collect(ImmutableMap.toImmutableMap(AnimationTarget::channelTarget, Function.identity()));
      KEYFRAME_CODECS = TARGETS.values().stream().collect(ImmutableMap.toImmutableMap(Function.identity(), AnimationParser::keyframeCodec));
      TARGET_LIST = TARGETS.keySet().stream().map(Identifier::toString).collect(Collectors.joining(", "));
      INTERPOLATION_LIST = INTERPOLATIONS.keySet().stream().map(Identifier::toString).collect(Collectors.joining(", "));
   }

   static {
      recomputeDerivedFields();
   }
}
