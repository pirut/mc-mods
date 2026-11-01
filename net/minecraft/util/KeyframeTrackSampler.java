package net.minecraft.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.attribute.LerpFunction;

public class KeyframeTrackSampler<T> {
   private final Optional<Integer> periodTicks;
   private final LerpFunction<T> lerp;
   private final List<KeyframeTrackSampler.Segment<T>> segments;

   KeyframeTrackSampler(KeyframeTrack<T> p_469398_, Optional<Integer> p_469234_, LerpFunction<T> p_468410_) {
      this.periodTicks = p_469234_;
      this.lerp = p_468410_;
      this.segments = bakeSegments(p_469398_, p_469234_);
   }

   private static <T> List<KeyframeTrackSampler.Segment<T>> bakeSegments(KeyframeTrack<T> p_468567_, Optional<Integer> p_467977_) {
      List<Keyframe<T>> list = p_468567_.keyframes();
      if (list.size() == 1) {
         T t = list.getFirst().value();
         return List.of(new KeyframeTrackSampler.Segment<>(EasingType.CONSTANT, t, 0, t, 0));
      } else {
         List<KeyframeTrackSampler.Segment<T>> list1 = new ArrayList<>();
         if (p_467977_.isPresent()) {
            Keyframe<T> keyframe = list.getFirst();
            Keyframe<T> keyframe1 = list.getLast();
            list1.add(new KeyframeTrackSampler.Segment<>(p_468567_, keyframe1, keyframe1.ticks() - p_467977_.get(), keyframe, keyframe.ticks()));
            addSegmentsFromKeyframes(p_468567_, list, list1);
            list1.add(new KeyframeTrackSampler.Segment<>(p_468567_, keyframe1, keyframe1.ticks(), keyframe, keyframe.ticks() + p_467977_.get()));
         } else {
            addSegmentsFromKeyframes(p_468567_, list, list1);
         }

         return List.copyOf(list1);
      }
   }

   private static <T> void addSegmentsFromKeyframes(KeyframeTrack<T> p_468796_, List<Keyframe<T>> p_467003_, List<KeyframeTrackSampler.Segment<T>> p_466885_) {
      for (int i = 0; i < p_467003_.size() - 1; i++) {
         Keyframe<T> keyframe = p_467003_.get(i);
         Keyframe<T> keyframe1 = p_467003_.get(i + 1);
         p_466885_.add(new KeyframeTrackSampler.Segment<>(p_468796_, keyframe, keyframe.ticks(), keyframe1, keyframe1.ticks()));
      }
   }

   public T sample(long p_468644_) {
      long i = this.loopTicks(p_468644_);
      KeyframeTrackSampler.Segment<T> segment = this.getSegmentAt(i);
      if (i <= segment.fromTicks) {
         return segment.fromValue;
      } else if (i >= segment.toTicks) {
         return segment.toValue;
      } else {
         float f = (float)(i - segment.fromTicks) / (segment.toTicks - segment.fromTicks);
         float f1 = segment.easing.apply(f);
         return this.lerp.apply(f1, segment.fromValue, segment.toValue);
      }
   }

   private KeyframeTrackSampler.Segment<T> getSegmentAt(long p_468111_) {
      for (KeyframeTrackSampler.Segment<T> segment : this.segments) {
         if (p_468111_ < segment.toTicks) {
            return segment;
         }
      }

      return this.segments.getLast();
   }

   private long loopTicks(long p_467963_) {
      return this.periodTicks.isPresent() ? Math.floorMod(p_467963_, this.periodTicks.get()) : p_467963_;
   }

   record Segment<T>(EasingType easing, T fromValue, int fromTicks, T toValue, int toTicks) {
      public Segment(KeyframeTrack<T> p_467990_, Keyframe<T> p_468045_, int p_467984_, Keyframe<T> p_467554_, int p_469320_) {
         this(p_467990_.easingType(), p_468045_.value(), p_467984_, p_467554_.value(), p_469320_);
      }
   }
}
