package net.neoforged.neoforge.client.entity.animation;

import org.joml.Vector3f;

@FunctionalInterface
public interface AnimationKeyframeTarget {
   Vector3f apply(float var1, float var2, float var3);
}
