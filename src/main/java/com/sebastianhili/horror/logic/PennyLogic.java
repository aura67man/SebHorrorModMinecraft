package com.sebastianhili.horror.logic;

import com.sebastianhili.horror.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Everything to do with finding and taking pennies. He only ever touches pennies:
 * none of these methods modify any other item.
 */
public final class PennyLogic {
    private PennyLogic() {
    }

    public static boolean isPenny(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.PENNY.get());
    }

    public static boolean isHoldingPenny(Player p) {
        return isPenny(p.getMainHandItem()) || isPenny(p.getOffhandItem());
    }

    /** Pennies anywhere on the player: main inventory, hotbar, offhand, and inside carried shulker boxes or bundles. */
    public static int countPennies(Player p) {
        Inventory inv = p.getInventory();
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (isPenny(s)) {
                total += s.getCount();
            } else {
                total += countNested(s);
            }
        }
        return total;
    }

    public static boolean hasPenny(Player p) {
        return countPennies(p) > 0;
    }

    /** Removes exactly one penny from the player, preferring loose pennies over ones packed in containers. */
    public static boolean takeOnePenny(Player p) {
        Inventory inv = p.getInventory();
        // hands first, then the rest
        if (isPenny(p.getMainHandItem())) {
            p.getMainHandItem().shrink(1);
            inv.setChanged();
            return true;
        }
        if (isPenny(p.getOffhandItem())) {
            p.getOffhandItem().shrink(1);
            inv.setChanged();
            return true;
        }
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (isPenny(s)) {
                s.shrink(1);
                inv.setChanged();
                return true;
            }
        }
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (takeNested(inv.getItem(i))) {
                inv.setChanged();
                return true;
            }
        }
        return false;
    }

    /** Removes every penny from a container. Returns how many were taken. Other items are never touched. */
    public static int removePennies(Container c) {
        int taken = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (isPenny(s)) {
                taken += s.getCount();
                c.setItem(i, ItemStack.EMPTY);
            }
        }
        if (taken > 0) {
            c.setChanged();
        }
        return taken;
    }

    /** Same as {@link #removePennies(Container)} but for modded inventories exposed as item handlers. */
    public static int removePennies(IItemHandler handler) {
        int taken = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (isPenny(s)) {
                taken += handler.extractItem(i, s.getCount(), false).getCount();
            }
        }
        return taken;
    }

    // ---- nested containers (shulker boxes and bundles carried in the inventory) -------------

    @Nullable
    private static ListTag nestedItems(ItemStack s) {
        CompoundTag tag = s.getTag();
        if (tag == null) {
            return null;
        }
        if (tag.contains("BlockEntityTag", Tag.TAG_COMPOUND)) {
            CompoundTag be = tag.getCompound("BlockEntityTag");
            if (be.contains("Items", Tag.TAG_LIST)) {
                return be.getList("Items", Tag.TAG_COMPOUND);
            }
        }
        if (tag.contains("Items", Tag.TAG_LIST)) { // bundles
            return tag.getList("Items", Tag.TAG_COMPOUND);
        }
        return null;
    }

    private static String pennyId() {
        return BuiltInRegistries.ITEM.getKey(ModItems.PENNY.get()).toString();
    }

    private static int countNested(ItemStack s) {
        ListTag items = nestedItems(s);
        if (items == null) {
            return 0;
        }
        int total = 0;
        String id = pennyId();
        for (int i = 0; i < items.size(); i++) {
            CompoundTag e = items.getCompound(i);
            if (id.equals(e.getString("id"))) {
                total += Math.max(1, e.getByte("Count"));
            }
        }
        return total * s.getCount();
    }

    private static boolean takeNested(ItemStack s) {
        ListTag items = nestedItems(s);
        if (items == null) {
            return false;
        }
        String id = pennyId();
        for (int i = 0; i < items.size(); i++) {
            CompoundTag e = items.getCompound(i);
            if (id.equals(e.getString("id"))) {
                int count = e.getByte("Count") - 1;
                if (count <= 0) {
                    items.remove(i);
                } else {
                    e.putByte("Count", (byte) count);
                }
                return true;
            }
        }
        return false;
    }
}
