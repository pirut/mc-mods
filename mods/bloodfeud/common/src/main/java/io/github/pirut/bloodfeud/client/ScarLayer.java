package io.github.pirut.bloodfeud.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.pirut.bloodfeud.BloodFeud;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.PiglinModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.WitchModel;
import net.minecraft.client.model.ZombieVillagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Paints a Brute's scars on its body: a charred face, stitches, arrow holes, claw marks, soot, algae, bandages. The
 * server says which Brutes nearby bear which scars; players without the mod see the scars as particles instead.
 */
public final class ScarLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private record Marks(List<String> scars, boolean flip, long until) {
    }

    private static final Map<Integer, Marks> MARKS = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> TEXTURES = new ConcurrentHashMap<>();

    private final String layout;

    private ScarLayer(RenderLayerParent<T, M> parent, String layout) {
        super(parent);
        this.layout = layout;
    }

    /** For Pirut Lib: a scar layer for every mob model we have overlays for. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static RenderLayer<?, ?> forRenderer(LivingEntityRenderer<?, ?> renderer) {
        String layout = layout(renderer.getModel());
        return layout == null ? null : new ScarLayer(renderer, layout);
    }

    private static String layout(EntityModel<?> model) {
        // Order matters: the more specific models extend the general ones.
        if (model instanceof ZombieVillagerModel<?>) return "villager";
        if (model instanceof PiglinModel<?>) return "piglin";
        if (model instanceof SkeletonModel<?>) return "skeleton";
        if (model instanceof WitchModel<?>) return "witch";
        if (model instanceof IllagerModel<?>) return "villager";
        if (model instanceof SpiderModel<?>) return "spider";
        if (model instanceof HumanoidModel<?>) return "humanoid";
        return null;
    }

    /** The server's list of scarred Brutes near this player; it comes every two seconds. */
    public static void receive(CompoundTag tag) {
        long until = System.currentTimeMillis() + 5000;
        MARKS.clear();
        for (Tag t : tag.getList("brutes", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            List<String> scars = new ArrayList<>();
            for (Tag s : c.getList("scars", Tag.TAG_STRING)) scars.add(s.getAsString());
            MARKS.put(c.getInt("id"), new Marks(scars, c.getBoolean("flip"), until));
        }
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) return;
        Marks marks = MARKS.get(entity.getId());
        if (marks == null || marks.until() < System.currentTimeMillis()) return;
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0);
        for (String scar : marks.scars()) {
            String name = layout + "_" + scar + "_" + (marks.flip() ? "b" : "a");
            ResourceLocation tex = TEXTURES.computeIfAbsent(name, n -> BloodFeud.id("textures/entity/scar/" + n + ".png"));
            getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(tex)), light, overlay);
        }
    }
}
