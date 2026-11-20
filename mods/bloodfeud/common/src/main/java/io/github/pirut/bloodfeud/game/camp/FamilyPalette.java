package io.github.pirut.bloodfeud.game.camp;

import io.github.pirut.bloodfeud.feud.Family;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * Distinct vanilla 1.21.1 block palettes for all six Blood Feud families, tinted by each pack's DyeColor
 * for wool canopies, carpets, and standing/wall banners.
 */
public final class FamilyPalette {
    public final Block foundation;
    public final Block ground;
    public final Block path;
    public final Block floor;
    public final Block log;
    public final Block pillar;
    public final Block planks;
    public final Block wall;
    public final Block wallAccent;
    public final Block slab;
    public final Block stair;
    public final Block fence;
    public final Block campfire;
    public final Block lantern;
    public final Block torch;
    public final Block wallTorch;
    public final Block familyPropA;
    public final Block familyPropB;
    public final Block familyCanopy;
    public final Block familyLight;
    public final Block workPropA;
    public final Block workPropB;
    public final Block throne;
    public final Block ruinFloor;
    public final Block ruinWall;
    public final Block ruinWallAlt;
    public final Block ruinSlab;
    public final Block ruinOvergrowth;
    public final Block ruinWeb;

    private FamilyPalette(
            Block foundation, Block ground, Block path, Block floor,
            Block log, Block pillar, Block planks, Block wall, Block wallAccent,
            Block slab, Block stair, Block fence,
            Block campfire, Block lantern, Block torch, Block wallTorch,
            Block familyPropA, Block familyPropB, Block familyCanopy, Block familyLight,
            Block workPropA, Block workPropB, Block throne,
            Block ruinFloor, Block ruinWall, Block ruinWallAlt, Block ruinSlab,
            Block ruinOvergrowth, Block ruinWeb
    ) {
        this.foundation = foundation;
        this.ground = ground;
        this.path = path;
        this.floor = floor;
        this.log = log;
        this.pillar = pillar;
        this.planks = planks;
        this.wall = wall;
        this.wallAccent = wallAccent;
        this.slab = slab;
        this.stair = stair;
        this.fence = fence;
        this.campfire = campfire;
        this.lantern = lantern;
        this.torch = torch;
        this.wallTorch = wallTorch;
        this.familyPropA = familyPropA;
        this.familyPropB = familyPropB;
        this.familyCanopy = familyCanopy;
        this.familyLight = familyLight;
        this.workPropA = workPropA;
        this.workPropB = workPropB;
        this.throne = throne;
        this.ruinFloor = ruinFloor;
        this.ruinWall = ruinWall;
        this.ruinWallAlt = ruinWallAlt;
        this.ruinSlab = ruinSlab;
        this.ruinOvergrowth = ruinOvergrowth;
        this.ruinWeb = ruinWeb;
    }

    private static final FamilyPalette ROTTEN = new FamilyPalette(
            Blocks.MOSSY_COBBLESTONE, Blocks.COARSE_DIRT, Blocks.PACKED_MUD, Blocks.SPRUCE_PLANKS,
            Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.OAK_PLANKS, Blocks.MOSSY_COBBLESTONE, Blocks.COBBLESTONE,
            Blocks.SPRUCE_SLAB, Blocks.SPRUCE_STAIRS, Blocks.OAK_FENCE,
            Blocks.CAMPFIRE, Blocks.LANTERN, Blocks.TORCH, Blocks.WALL_TORCH,
            Blocks.BONE_BLOCK, Blocks.MOSSY_STONE_BRICKS, Blocks.MOSS_CARPET, Blocks.LANTERN,
            Blocks.GRINDSTONE, Blocks.SMITHING_TABLE, Blocks.SPRUCE_STAIRS,
            Blocks.COARSE_DIRT, Blocks.MOSSY_STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.MOSSY_COBBLESTONE_SLAB,
            Blocks.MOSS_CARPET, Blocks.COBWEB
    );

    private static final FamilyPalette BONES = new FamilyPalette(
            Blocks.COBBLED_DEEPSLATE, Blocks.GRAVEL, Blocks.TUFF, Blocks.POLISHED_DEEPSLATE,
            Blocks.BONE_BLOCK, Blocks.BONE_BLOCK, Blocks.DARK_OAK_PLANKS, Blocks.STONE_BRICKS, Blocks.COBBLED_DEEPSLATE,
            Blocks.COBBLED_DEEPSLATE_SLAB, Blocks.DEEPSLATE_BRICK_STAIRS, Blocks.DARK_OAK_FENCE,
            Blocks.SOUL_CAMPFIRE, Blocks.SOUL_LANTERN, Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH,
            Blocks.BONE_BLOCK, Blocks.FLETCHING_TABLE, Blocks.SOUL_LANTERN, Blocks.SOUL_LANTERN,
            Blocks.TARGET, Blocks.FLETCHING_TABLE, Blocks.DEEPSLATE_BRICK_STAIRS,
            Blocks.GRAVEL, Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.COBBLED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE_SLAB,
            Blocks.COBWEB, Blocks.COBWEB
    );

    private static final FamilyPalette ILLAGERS = new FamilyPalette(
            Blocks.COBBLESTONE, Blocks.GRAVEL, Blocks.DIRT_PATH, Blocks.DARK_OAK_PLANKS,
            Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_PLANKS, Blocks.COBBLESTONE, Blocks.STONE_BRICKS,
            Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_FENCE,
            Blocks.CAMPFIRE, Blocks.LANTERN, Blocks.TORCH, Blocks.WALL_TORCH,
            Blocks.IRON_BARS, Blocks.HAY_BLOCK, Blocks.LANTERN, Blocks.LANTERN,
            Blocks.GRINDSTONE, Blocks.SMITHING_TABLE, Blocks.DARK_OAK_STAIRS,
            Blocks.COARSE_DIRT, Blocks.CRACKED_STONE_BRICKS, Blocks.MOSSY_COBBLESTONE, Blocks.COBBLESTONE_SLAB,
            Blocks.MOSS_CARPET, Blocks.COBWEB
    );

    private static final FamilyPalette NETHER = new FamilyPalette(
            Blocks.BLACKSTONE, Blocks.BLACKSTONE, Blocks.BASALT, Blocks.POLISHED_BLACKSTONE_BRICKS,
            Blocks.BASALT, Blocks.POLISHED_BASALT, Blocks.CRIMSON_PLANKS, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.GILDED_BLACKSTONE,
            Blocks.POLISHED_BLACKSTONE_BRICK_SLAB, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Blocks.CRIMSON_FENCE,
            Blocks.SOUL_CAMPFIRE, Blocks.SOUL_LANTERN, Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH,
            Blocks.GILDED_BLACKSTONE, Blocks.MAGMA_BLOCK, Blocks.CHAIN, Blocks.SOUL_LANTERN,
            Blocks.SMITHING_TABLE, Blocks.GRINDSTONE, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS,
            Blocks.SOUL_SOIL, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS, Blocks.BLACKSTONE, Blocks.BLACKSTONE_SLAB,
            Blocks.BLACKSTONE_SLAB, Blocks.CHAIN
    );

    private static final FamilyPalette SPIDERS = new FamilyPalette(
            Blocks.MOSSY_COBBLESTONE, Blocks.PODZOL, Blocks.PACKED_MUD, Blocks.DARK_OAK_PLANKS,
            Blocks.DARK_OAK_LOG, Blocks.SPRUCE_LOG, Blocks.SPRUCE_PLANKS, Blocks.MOSSY_COBBLESTONE, Blocks.DARK_OAK_PLANKS,
            Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_FENCE,
            Blocks.CAMPFIRE, Blocks.LANTERN, Blocks.REDSTONE_TORCH, Blocks.REDSTONE_WALL_TORCH,
            Blocks.COBWEB, Blocks.PODZOL, Blocks.COBWEB, Blocks.REDSTONE_TORCH,
            Blocks.COBWEB, Blocks.BARREL, Blocks.DARK_OAK_STAIRS,
            Blocks.PODZOL, Blocks.MOSSY_COBBLESTONE, Blocks.CRACKED_STONE_BRICKS, Blocks.MOSSY_COBBLESTONE_SLAB,
            Blocks.COBWEB, Blocks.COBWEB
    );

    private static final FamilyPalette WITCHES = new FamilyPalette(
            Blocks.MOSSY_COBBLESTONE, Blocks.PODZOL, Blocks.MUD_BRICKS, Blocks.SPRUCE_PLANKS,
            Blocks.SPRUCE_LOG, Blocks.SPRUCE_LOG, Blocks.SPRUCE_PLANKS, Blocks.MUD_BRICKS, Blocks.MOSSY_COBBLESTONE,
            Blocks.SPRUCE_SLAB, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_FENCE,
            Blocks.CAMPFIRE, Blocks.LANTERN, Blocks.TORCH, Blocks.WALL_TORCH,
            Blocks.CAULDRON, Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM, Blocks.LANTERN,
            Blocks.CAULDRON, Blocks.BREWING_STAND, Blocks.SPRUCE_STAIRS,
            Blocks.MUD, Blocks.MOSSY_COBBLESTONE, Blocks.PACKED_MUD, Blocks.MUD_BRICK_SLAB,
            Blocks.MOSS_CARPET, Blocks.COBWEB
    );

    public static FamilyPalette of(Family family) {
        return switch (family) {
            case ROTTEN -> ROTTEN;
            case BONES -> BONES;
            case ILLAGERS -> ILLAGERS;
            case NETHER -> NETHER;
            case SPIDERS -> SPIDERS;
            case WITCHES -> WITCHES;
        };
    }

    public BlockState resolve(CampBlueprint.Placement op, int packColor) {
        DyeColor primary = DyeColor.byId(Math.floorMod(packColor, 16));
        DyeColor accent = accentColor(primary);
        Direction dir = toDirection(op.facing());
        return switch (op.material()) {
            case AIR -> Blocks.AIR.defaultBlockState();
            case FOUNDATION_COLUMN -> foundation.defaultBlockState();
            case GROUND -> ground.defaultBlockState();
            case PATH -> path.defaultBlockState();
            case FLOOR -> floor.defaultBlockState();
            case LOG -> logState(log, op.facing());
            case PILLAR -> pillar.defaultBlockState();
            case PLANKS -> planks.defaultBlockState();
            case WALL -> wall.defaultBlockState();
            case WALL_ACCENT -> wallAccent.defaultBlockState();
            case SLAB -> slab.defaultBlockState();
            case STAIR -> stair.defaultBlockState().setValue(StairBlock.FACING, dir).setValue(StairBlock.HALF, Half.BOTTOM);
            case STAIR_TOP -> stair.defaultBlockState().setValue(StairBlock.FACING, dir).setValue(StairBlock.HALF, Half.TOP);
            case FENCE -> fence.defaultBlockState();
            case IRON_BARS -> Blocks.IRON_BARS.defaultBlockState();
            case CHAIN -> Blocks.CHAIN.defaultBlockState();
            case WOOL_PRIMARY -> wool(DyeColor.byId(Math.floorMod(op.meta() > 0 ? op.meta() : packColor, 16))).defaultBlockState();
            case WOOL_ACCENT -> wool(accent).defaultBlockState();
            case CARPET -> carpet(primary).defaultBlockState();
            case BANNER_STANDING -> BannerBlock.byColor(primary).defaultBlockState().setValue(BannerBlock.ROTATION, bannerRotation(dir));
            case BANNER_WALL -> wallBanner(primary).defaultBlockState().setValue(WallBannerBlock.FACING, dir);
            case VASSAL_BANNER -> BannerBlock.byColor(DyeColor.byId(Math.floorMod(op.meta(), 16)))
                    .defaultBlockState().setValue(BannerBlock.ROTATION, bannerRotation(dir));
            case CAMPFIRE -> campfire.defaultBlockState().setValue(CampfireBlock.LIT, true);
            case TORCH -> torch.defaultBlockState();
            case WALL_TORCH -> wallTorch.defaultBlockState().setValue(WallTorchBlock.FACING, dir);
            case LANTERN -> lantern.defaultBlockState().setValue(LanternBlock.HANGING, false);
            case HANGING_LANTERN -> lantern.defaultBlockState().setValue(LanternBlock.HANGING, true);
            case FAMILY_LIGHT -> familyLight == Blocks.LANTERN || familyLight == Blocks.SOUL_LANTERN
                    ? familyLight.defaultBlockState().setValue(LanternBlock.HANGING, false)
                    : familyLight.defaultBlockState();
            case LADDER -> Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, dir);
            case BARREL -> Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP);
            case CHEST_CAMP, CHEST_OUTPOST, CHEST_STRONGHOLD, CHEST_TOWN ->
                    Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, dir);
            case THRONE -> throne.defaultBlockState().setValue(StairBlock.FACING, dir.getOpposite()).setValue(StairBlock.HALF, Half.BOTTOM);
            case FAMILY_PROP_A -> familyPropA.defaultBlockState();
            case FAMILY_PROP_B -> familyPropB.defaultBlockState();
            case FAMILY_CANOPY -> familyCanopy.defaultBlockState();
            case WORK_PROP_A -> workPropA.defaultBlockState();
            case WORK_PROP_B -> workPropB.defaultBlockState();
            case RUIN_FLOOR -> ruinFloor.defaultBlockState();
            case RUIN_WALL -> ruinWall.defaultBlockState();
            case RUIN_WALL_ALT -> ruinWallAlt.defaultBlockState();
            case RUIN_SLAB -> ruinSlab.defaultBlockState();
            case CAMPFIRE_UNLIT -> campfire.defaultBlockState().setValue(CampfireBlock.LIT, false);
            case RUIN_OVERGROWTH -> ruinOvergrowth.defaultBlockState();
            case RUIN_WEB -> ruinWeb.defaultBlockState();
        };
    }

    private static BlockState logState(Block block, CampBlueprint.Facing facing) {
        if (block.defaultBlockState().hasProperty(RotatedPillarBlock.AXIS)) {
            Direction.Axis axis = (facing == CampBlueprint.Facing.EAST || facing == CampBlueprint.Facing.WEST)
                    ? Direction.Axis.X
                    : (facing == CampBlueprint.Facing.SOUTH ? Direction.Axis.Z : Direction.Axis.Y);
            return block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
        }
        return block.defaultBlockState();
    }

    public static Direction toDirection(CampBlueprint.Facing f) {
        return switch (f) {
            case NORTH -> Direction.NORTH;
            case EAST -> Direction.EAST;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
        };
    }

    private static int bannerRotation(Direction dir) {
        return switch (dir) {
            case SOUTH -> 0;
            case WEST -> 4;
            case NORTH -> 8;
            case EAST -> 12;
            default -> 0;
        };
    }

    private static DyeColor accentColor(DyeColor primary) {
        return switch (primary) {
            case WHITE, LIGHT_GRAY, YELLOW, LIME, PINK, LIGHT_BLUE -> DyeColor.BLACK;
            case BLACK, GRAY, BROWN -> DyeColor.LIGHT_GRAY;
            default -> DyeColor.WHITE;
        };
    }

    public static Block wool(DyeColor color) {
        return switch (color) {
            case WHITE -> Blocks.WHITE_WOOL;
            case ORANGE -> Blocks.ORANGE_WOOL;
            case MAGENTA -> Blocks.MAGENTA_WOOL;
            case LIGHT_BLUE -> Blocks.LIGHT_BLUE_WOOL;
            case YELLOW -> Blocks.YELLOW_WOOL;
            case LIME -> Blocks.LIME_WOOL;
            case PINK -> Blocks.PINK_WOOL;
            case GRAY -> Blocks.GRAY_WOOL;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_WOOL;
            case CYAN -> Blocks.CYAN_WOOL;
            case PURPLE -> Blocks.PURPLE_WOOL;
            case BLUE -> Blocks.BLUE_WOOL;
            case BROWN -> Blocks.BROWN_WOOL;
            case GREEN -> Blocks.GREEN_WOOL;
            case RED -> Blocks.RED_WOOL;
            case BLACK -> Blocks.BLACK_WOOL;
        };
    }

    public static Block carpet(DyeColor color) {
        return switch (color) {
            case WHITE -> Blocks.WHITE_CARPET;
            case ORANGE -> Blocks.ORANGE_CARPET;
            case MAGENTA -> Blocks.MAGENTA_CARPET;
            case LIGHT_BLUE -> Blocks.LIGHT_BLUE_CARPET;
            case YELLOW -> Blocks.YELLOW_CARPET;
            case LIME -> Blocks.LIME_CARPET;
            case PINK -> Blocks.PINK_CARPET;
            case GRAY -> Blocks.GRAY_CARPET;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_CARPET;
            case CYAN -> Blocks.CYAN_CARPET;
            case PURPLE -> Blocks.PURPLE_CARPET;
            case BLUE -> Blocks.BLUE_CARPET;
            case BROWN -> Blocks.BROWN_CARPET;
            case GREEN -> Blocks.GREEN_CARPET;
            case RED -> Blocks.RED_CARPET;
            case BLACK -> Blocks.BLACK_CARPET;
        };
    }

    public static Block wallBanner(DyeColor color) {
        return switch (color) {
            case WHITE -> Blocks.WHITE_WALL_BANNER;
            case ORANGE -> Blocks.ORANGE_WALL_BANNER;
            case MAGENTA -> Blocks.MAGENTA_WALL_BANNER;
            case LIGHT_BLUE -> Blocks.LIGHT_BLUE_WALL_BANNER;
            case YELLOW -> Blocks.YELLOW_WALL_BANNER;
            case LIME -> Blocks.LIME_WALL_BANNER;
            case PINK -> Blocks.PINK_WALL_BANNER;
            case GRAY -> Blocks.GRAY_WALL_BANNER;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_WALL_BANNER;
            case CYAN -> Blocks.CYAN_WALL_BANNER;
            case PURPLE -> Blocks.PURPLE_WALL_BANNER;
            case BLUE -> Blocks.BLUE_WALL_BANNER;
            case BROWN -> Blocks.BROWN_WALL_BANNER;
            case GREEN -> Blocks.GREEN_WALL_BANNER;
            case RED -> Blocks.RED_WALL_BANNER;
            case BLACK -> Blocks.BLACK_WALL_BANNER;
        };
    }
}
