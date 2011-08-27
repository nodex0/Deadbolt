package com.daemitus.lockette;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.ContainerBlock;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.material.Door;
import org.bukkit.material.MaterialData;
import org.bukkit.material.TrapDoor;

public class Util {

    public static final Set<BlockFace> horizontalBlockFaces = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);
    public static final Set<BlockFace> verticalBlockFaces = EnumSet.of(BlockFace.UP, BlockFace.DOWN);
    private static final HashMap<Player, Block> selectedSign = new HashMap<Player, Block>();
    private static final String patternStripColor = "(?i)§[0-9a-zA-Z]";
    private static final String patternNormalTooLong = ".{16,}";
    private static final String patternBracketTooLong = "\\[.{14,}\\]";

    public static boolean isProtected(Block block) {
        return getOwnerSign(block) != null;
    }

    public static boolean isOwner(String name, Block block) {
        return truncate(name).equalsIgnoreCase(getOwnerName(block));
    }

    public static boolean isAuthorized(String name, Block block) {
        List<String> names = getAllNames(block);
        return !names.isEmpty() && (names.contains(truncate(name).toLowerCase())
                                    || names.contains(Config.signtext_everyone)
                                    || names.contains(Config.signtext_everyone_locale));
    }

    public static String getOwnerName(Block block) {
        Block owner = getOwnerSign(block);
        if (owner == null)
            return "";
        Sign sign = (Sign) owner.getState();
        String text = stripColor(sign.getLine(1));
        return text;
    }

    public static Block getOwnerSign(Block block) {
        Material type = block.getType();
        BlockState state = block.getState();
        MaterialData data = state.getData();
        if (type.equals(Material.CHEST))
            return getOwnerSign(block, true, false, false);
        else if (state instanceof ContainerBlock)
            return getOwnerSign(block, false, false, false);
        else if (data instanceof Door)
            return getOwnerSign(block, true, true, false);
        else if (data instanceof TrapDoor)
            return getOwnerSign(block, false, false, true);
        else if (type.equals(Material.WALL_SIGN)) {
            Sign sign = (Sign) block.getState();
            String text = stripColor(sign.getLine(0));
            if (text.equalsIgnoreCase(Config.signtext_private) || text.equalsIgnoreCase(Config.signtext_private_locale))
                return block;
            if (text.equalsIgnoreCase(Config.signtext_moreusers) || text.equalsIgnoreCase(Config.signtext_moreusers_locale)) {
                Block attached = getBlockSignAttachedTo(block);
                return getOwnerSign(attached);
            }
            return null;
        } else {
            Block check = getOwnerSign(block, false, false, false);
            if (check == null) {
                Block upBlock = block.getRelative(BlockFace.UP);
                if (upBlock.getState().getData() instanceof Door)
                    return getOwnerSign(upBlock);
                Block downBlock = block.getRelative(BlockFace.DOWN);
                if (downBlock.getState().getData() instanceof Door)
                    return getOwnerSign(downBlock);
            }
            return check;
        }
    }

    private static Block getOwnerSign(Block block, boolean iterateHorizontal, boolean iterateVertical, boolean trapDoor) {
        if (trapDoor && block.getState().getData() instanceof TrapDoor) {
            TrapDoor door = (TrapDoor) block.getState().getData();
            block = block.getRelative(door.getAttachedFace());
        }

        for (BlockFace bf : horizontalBlockFaces) {
            Block adjacent = block.getRelative(bf);

            if (iterateHorizontal && adjacent.getType().equals(block.getType())) {
                adjacent = getOwnerSign(adjacent, false, iterateVertical, false);
                if (adjacent != null)
                    return adjacent;
                else
                    continue;
            }

            if (!adjacent.getType().equals(Material.WALL_SIGN))
                continue;
            if ((bf.equals(BlockFace.NORTH) && adjacent.getData() != 4)
                || (bf.equals(BlockFace.SOUTH) && adjacent.getData() != 5)
                || (bf.equals(BlockFace.EAST) && adjacent.getData() != 2)
                || (bf.equals(BlockFace.WEST) && adjacent.getData() != 3))
                continue;
            Sign sign = (Sign) adjacent.getState();
            String text = stripColor(sign.getLine(0));
            if (!text.equalsIgnoreCase(Config.signtext_private) && !text.equalsIgnoreCase(Config.signtext_private_locale))
                continue;
            return adjacent;
        }

        if (iterateVertical) {
            Set<Block> set = new HashSet<Block>();
            for (BlockFace bf : verticalBlockFaces) {
                Block iterating = block;
                while (iterating.getType().equals(block.getType())) {
                    iterating = iterating.getRelative(bf);
                    set.add(iterating);
                }
            }
            for (Block setBlock : set) {
                Block checkBlock = getOwnerSign(setBlock, false, false, false);
                if (checkBlock != null)
                    return checkBlock;
            }
        }
        return null;
    }

    public static List<String> getAllNames(Block block) {
        Set<Block> signs = getAllSigns(block);
        List<String> names = new ArrayList<String>();
        for (Block signBlock : signs) {
            Sign sign = (Sign) signBlock.getState();
            for (int i = 1; i < 4; i++) {
                String line = sign.getLine(i);
                if (line.length() > 0)
                    names.add(stripColor(line).toLowerCase());
            }
        }
        return names;
    }

    private static Set<Block> getAllSigns(Block block) {
        Material type = block.getType();
        BlockState state = block.getState();
        MaterialData data = state.getData();
        if (type.equals(Material.CHEST))
            return getAllSigns(block, true, false, false);
        else if (state instanceof ContainerBlock)
            return getAllSigns(block, false, false, false);
        else if (data instanceof Door)
            return getAllSigns(block, true, true, false);
        else if (data instanceof TrapDoor)
            return getAllSigns(block, false, false, true);
        else if (type.equals(Material.WALL_SIGN)) {
            Sign sign = (Sign) block.getState();
            String text = stripColor(sign.getLine(0));
            if (text.equalsIgnoreCase(Config.signtext_private) || text.equalsIgnoreCase(Config.signtext_private_locale))
                return getAllSigns(getBlockSignAttachedTo(block));
            if (text.equalsIgnoreCase(Config.signtext_moreusers) || text.equalsIgnoreCase(Config.signtext_moreusers_locale))
                return getAllSigns(getBlockSignAttachedTo(block));
        } else {
            Block upBlock = block.getRelative(BlockFace.UP);
            if (upBlock.getState().getData() instanceof Door)
                return getAllSigns(upBlock);
            Block downBlock = block.getRelative(BlockFace.DOWN);
            if (downBlock.getState().getData() instanceof Door)
                return getAllSigns(downBlock);
        }
        return new HashSet<Block>();
    }

    private static Set<Block> getAllSigns(Block block, boolean iterateHorizontal, boolean iterateVertical, boolean trapDoor) {
        Set<Block> signSet = new HashSet<Block>();

        if (trapDoor && block.getState().getData() instanceof TrapDoor) {
            TrapDoor door = (TrapDoor) block.getState().getData();
            block = block.getRelative(door.getAttachedFace());
        }

        for (BlockFace bf : horizontalBlockFaces) {
            Block adjacent = block.getRelative(bf);

            if (iterateHorizontal && adjacent.getType().equals(block.getType()))
                signSet.addAll(getAllSigns(adjacent, false, iterateVertical, false));

            if (!adjacent.getType().equals(Material.WALL_SIGN))
                continue;
            if ((bf.equals(BlockFace.NORTH) && adjacent.getData() != 4)
                || (bf.equals(BlockFace.SOUTH) && adjacent.getData() != 5)
                || (bf.equals(BlockFace.EAST) && adjacent.getData() != 2)
                || (bf.equals(BlockFace.WEST) && adjacent.getData() != 3))
                continue;
            Sign sign = (Sign) adjacent.getState();
            String text = stripColor(sign.getLine(0));
            if (text.equalsIgnoreCase(Config.signtext_private) || text.equalsIgnoreCase(Config.signtext_private_locale))
                signSet.add(adjacent);
            if (text.equalsIgnoreCase(Config.signtext_moreusers) || text.equalsIgnoreCase(Config.signtext_moreusers_locale))
                signSet.add(adjacent);
        }

        if (iterateVertical) {
            Set<Block> set = new HashSet<Block>();
            for (BlockFace bf : verticalBlockFaces) {
                Block iterating = block;
                while (iterating.getType().equals(block.getType())) {
                    iterating = iterating.getRelative(bf);
                    set.add(iterating);
                }
            }
            for (Block setBlock : set)
                signSet.addAll(getAllSigns(setBlock, false, false, false));
        }
        return signSet;
    }

    public static Block getBlockSignAttachedTo(Block block) {
        if (block.getType().equals(Material.WALL_SIGN))
            switch (block.getData()) {
                case 2:
                    return block.getRelative(BlockFace.WEST);
                case 3:
                    return block.getRelative(BlockFace.EAST);
                case 4:
                    return block.getRelative(BlockFace.SOUTH);
                case 5:
                    return block.getRelative(BlockFace.NORTH);
            }
        return null;
    }

    public static String stripColor(String line) {
        return line.replaceAll(patternStripColor, "");
    }

    public static String truncate(String text) {
        if (text.matches(patternBracketTooLong))
            return "[" + text.substring(1, 14) + "]";
        if (text.matches(patternNormalTooLong))
            return text.substring(0, 15);
        return text;
    }

    public static Block getSelectedSign(Player player) {
        return selectedSign.get(player);
    }

    public static void setSelectedSign(Player player, Block block) {
        selectedSign.put(player, block);
    }

    public static void clearSelectedSign(Player player) {
        selectedSign.remove(player);
    }

    public static void clearSelectedSigns() {
        selectedSign.clear();
    }
}