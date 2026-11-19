package io.github.pirut.chunks3;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

/** Dimensions of the Cubic world type are cubic while their world is played, and only then. */
class CubicAdoptTest {
    private static ResourceKey<Level> key(String name) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("chunks3test", name));
    }

    @Test void adoptedWhilePlayedAndDroppedAfter() {
        ResourceKey<Level> cubicWorld = key("adopted_overworld");
        Cubic.adopt(List.of(cubicWorld));
        assertTrue(Cubic.isCubic(cubicWorld));
        assertTrue(Cubic.dimension(cubicWorld).adopted());
        assertSame(Cubic.WORLD_TYPE, Cubic.dimension(cubicWorld).generators());
        // The next world opened in the same game has no Cubic-type dimension.
        Cubic.adopt(List.of());
        assertFalse(Cubic.isCubic(cubicWorld));
        assertFalse(Cubic.dimensions().contains(cubicWorld));
    }

    @Test void registeredDimensionsAreNeverDroppedAndStayHome() {
        ResourceKey<Level> registered = key("registered"), adopted = key("adopted_other");
        CubicDimension.Generators own = (level, folder) -> { throw new UnsupportedOperationException(); };
        Cubic.register(registered, own);
        Cubic.adopt(List.of(adopted, registered));
        assertFalse(Cubic.dimension(registered).adopted(), "a registered dimension keeps its own generator");
        assertSame(own, Cubic.dimension(registered).generators());
        assertTrue(Cubic.dimensions().indexOf(registered) < Cubic.dimensions().indexOf(adopted), "registered ones come first");
        Cubic.adopt(List.of());
        assertTrue(Cubic.isCubic(registered));
        assertFalse(Cubic.isCubic(adopted));
        Cubic.adoptLevel(registered, false);
        assertTrue(Cubic.isCubic(registered), "a client never drops a registered dimension");
    }

    @Test void aClientAdoptsByTheLevelsType() {
        ResourceKey<Level> remote = key("remote_overworld");
        Cubic.adoptLevel(remote, true);
        assertTrue(Cubic.isCubic(remote));
        Cubic.adoptLevel(remote, true);
        assertEquals(1, Cubic.dimensions().stream().filter(remote::equals).count());
        // Joining a vanilla server after a Cubic one: its overworld of a vanilla type is not cubic.
        Cubic.adoptLevel(remote, false);
        assertFalse(Cubic.isCubic(remote));
    }
}
