package io.github.pirut.bloodfeud.feud;

import java.util.Random;

/** Names a pack's home after the kind of place it formed in: "Mossy Ravine", "Sunken Hut", "Bone Dunes". */
public final class PlaceNames {
    private PlaceNames() {
    }

    private static final String[] ADJ = {"Mossy", "Sunken", "Hollow", "Broken", "Crooked", "Rotten", "Silent", "Grim",
            "Bitter", "Old", "Cracked", "Black", "Red", "Lost", "Muddy", "Windy", "Cold", "Sour", "Burnt", "Dead"};

    /**
     * @param biome a biome id ("minecraft:dark_forest"), or "" if not known
     * @param underground whether it formed under the surface
     */
    public static String name(Random random, String biome, boolean underground) {
        String b = biome.contains(":") ? biome.substring(biome.indexOf(':') + 1) : biome;
        String[] nouns = underground ? new String[]{"Cave", "Pit", "Hollow", "Tunnels", "Den", "Crack", "Mine"} : nouns(b);
        String[] adj = adjectives(b);
        return adj[random.nextInt(adj.length)] + " " + nouns[random.nextInt(nouns.length)];
    }

    private static String[] adjectives(String b) {
        if (b.contains("swamp") || b.contains("mangrove")) return new String[]{"Mossy", "Sunken", "Rotten", "Muddy", "Foul", "Drowned"};
        if (b.contains("desert") || b.contains("badlands")) return new String[]{"Dry", "Bone", "Red", "Burnt", "Bleached", "Thirsty"};
        if (b.contains("snow") || b.contains("frozen") || b.contains("ice") || b.contains("grove")) return new String[]{"Frozen", "White", "Cold", "Bitter", "Pale", "Icy"};
        if (b.contains("nether") || b.contains("crimson") || b.contains("warped") || b.contains("basalt") || b.contains("soul")) return new String[]{"Burning", "Ashen", "Crimson", "Black", "Molten", "Cinder"};
        if (b.contains("dark_forest")) return new String[]{"Gloomy", "Black", "Crooked", "Silent", "Tangled"};
        return ADJ;
    }

    private static String[] nouns(String b) {
        if (b.contains("swamp") || b.contains("mangrove")) return new String[]{"Bog", "Mire", "Hut", "Marsh", "Roots"};
        if (b.contains("desert")) return new String[]{"Dunes", "Well", "Sands", "Ruins", "Bones"};
        if (b.contains("badlands")) return new String[]{"Mesa", "Gulch", "Canyon", "Spires"};
        if (b.contains("ocean") || b.contains("river") || b.contains("beach")) return new String[]{"Shore", "Wreck", "Shallows", "Cove", "Ford"};
        if (b.contains("mountain") || b.contains("peaks") || b.contains("slopes") || b.contains("hills")) return new String[]{"Crag", "Ridge", "Peak", "Ledge", "Pass"};
        if (b.contains("forest") || b.contains("taiga") || b.contains("jungle") || b.contains("grove")) return new String[]{"Thicket", "Wood", "Stump", "Glade", "Hollow"};
        if (b.contains("nether") || b.contains("crimson") || b.contains("warped") || b.contains("basalt") || b.contains("soul")) return new String[]{"Pits", "Bastion", "Wastes", "Forge", "Spire"};
        if (b.contains("plains") || b.contains("meadow") || b.contains("savanna")) return new String[]{"Field", "Farm", "Barrow", "Hill", "Fence"};
        return new String[]{"Ravine", "Hollow", "Ruins", "Barrow", "Crossing", "Hill"};
    }
}
