package io.github.pirut.chunks3;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;

/** Reads and binds a built-in registry's tags in tests, the same on every version. */
public final class TestTags {
    private TestTags() {}

    public static <T> Map<TagKey<T>, List<Holder<T>>> current(Registry<T> registry) {
        //#if MC >= 1.21.2
        //$$ return registry.getTags().collect(Collectors.toMap(named -> named.key(), named -> named.stream().toList()));
        //#else
        return registry.getTags().collect(Collectors.toMap(p -> p.getFirst(), p -> p.getSecond().stream().toList()));
        //#endif
    }

    public static <T> void bind(Registry<T> registry, Map<TagKey<T>, List<Holder<T>>> tags) {
        //#if MC >= 1.21.2
        //$$ registry.prepareTagReload(new net.minecraft.tags.TagLoader.LoadResult<>(registry.key(), tags)).apply();
        //#else
        registry.bindTags(tags);
        //#endif
    }
}
