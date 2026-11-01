package io.github.pirut.chunks3.mc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Set;

/**
 * Reads NBT the same way on every Minecraft version. From 1.21.5 a tag's getters return
 * {@code Optional}s; these return the old defaults (0, false, "", an empty tag or list) for a
 * missing or mistyped entry, as 1.21.1's did.
 */
public final class Nbt {
    private Nbt() {}

    //#if MC >= 1.21.5
    //$$ public static int getInt(CompoundTag tag, String key) { return tag.getIntOr(key, 0); }
    //$$ public static long getLong(CompoundTag tag, String key) { return tag.getLongOr(key, 0L); }
    //$$ public static byte getByte(CompoundTag tag, String key) { return tag.getByteOr(key, (byte) 0); }
    //$$ public static short getShort(CompoundTag tag, String key) { return tag.getShortOr(key, (short) 0); }
    //$$ public static float getFloat(CompoundTag tag, String key) { return tag.getFloatOr(key, 0F); }
    //$$ public static double getDouble(CompoundTag tag, String key) { return tag.getDoubleOr(key, 0D); }
    //$$ public static boolean getBoolean(CompoundTag tag, String key) { return tag.getBooleanOr(key, false); }
    //$$ public static String getString(CompoundTag tag, String key) { return tag.getStringOr(key, ""); }
    //$$ public static byte[] getByteArray(CompoundTag tag, String key) { return tag.getByteArray(key).orElse(new byte[0]); }
    //$$ public static int[] getIntArray(CompoundTag tag, String key) { return tag.getIntArray(key).orElse(new int[0]); }
    //$$ public static long[] getLongArray(CompoundTag tag, String key) { return tag.getLongArray(key).orElse(new long[0]); }
    //$$ public static CompoundTag getCompound(CompoundTag tag, String key) { return tag.getCompoundOrEmpty(key); }
    //$$ /** The list at {@code key}; {@code type} is the old element type check, which 1.21.5's lists no longer need. */
    //$$ public static ListTag getList(CompoundTag tag, String key, int type) { return tag.getListOrEmpty(key); }
    //$$ public static Set<String> keys(CompoundTag tag) { return tag.keySet(); }
    //$$ public static double getDouble(ListTag list, int i) { return list.getDoubleOr(i, 0D); }
    //$$ public static float getFloat(ListTag list, int i) { return list.getFloatOr(i, 0F); }
    //$$ public static int getInt(ListTag list, int i) { return list.getIntOr(i, 0); }
    //$$ public static CompoundTag getCompound(ListTag list, int i) { return list.getCompoundOrEmpty(i); }
    //#else
    public static int getInt(CompoundTag tag, String key) { return tag.getInt(key); }
    public static long getLong(CompoundTag tag, String key) { return tag.getLong(key); }
    public static byte getByte(CompoundTag tag, String key) { return tag.getByte(key); }
    public static short getShort(CompoundTag tag, String key) { return tag.getShort(key); }
    public static float getFloat(CompoundTag tag, String key) { return tag.getFloat(key); }
    public static double getDouble(CompoundTag tag, String key) { return tag.getDouble(key); }
    public static boolean getBoolean(CompoundTag tag, String key) { return tag.getBoolean(key); }
    public static String getString(CompoundTag tag, String key) { return tag.getString(key); }
    public static byte[] getByteArray(CompoundTag tag, String key) { return tag.getByteArray(key); }
    public static int[] getIntArray(CompoundTag tag, String key) { return tag.getIntArray(key); }
    public static long[] getLongArray(CompoundTag tag, String key) { return tag.getLongArray(key); }
    public static CompoundTag getCompound(CompoundTag tag, String key) { return tag.getCompound(key); }
    /** The list at {@code key} whose elements are of {@code type} (a {@code Tag.TAG_*}), or an empty one. */
    public static ListTag getList(CompoundTag tag, String key, int type) { return tag.getList(key, type); }
    public static Set<String> keys(CompoundTag tag) { return tag.getAllKeys(); }
    public static double getDouble(ListTag list, int i) { return list.getDouble(i); }
    public static float getFloat(ListTag list, int i) { return list.getFloat(i); }
    public static int getInt(ListTag list, int i) { return list.getInt(i); }
    public static CompoundTag getCompound(ListTag list, int i) { return list.getCompound(i); }
    //#endif

    /** Whether {@code tag} has an entry {@code key} of {@code type} (a {@code Tag.TAG_*}). */
    public static boolean has(CompoundTag tag, String key, int type) {
        Tag entry = tag.get(key);
        return entry != null && entry.getId() == type;
    }
}
