package me.jellysquid.mods.sodium.common.util;

import net.minecraft.util.math.Direction;

public class DirectionUtil {
    public static final Direction[] ALL_DIRECTIONS = Direction.values();
    public static final Direction[] HORIZONTAL_DIRECTIONS = new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
    public static final Direction[] VERTICAL_DIRECTIONS = new Direction[] { Direction.UP, Direction.DOWN };
}
