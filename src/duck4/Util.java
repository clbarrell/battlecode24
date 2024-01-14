package duck4;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class Util {
    static int distance(MapLocation A, MapLocation B) {
        return Math.max(Math.abs(A.x - B.x), Math.abs(A.y - B.y));
    }

    static int manhattan(MapLocation A, MapLocation B) {
        return Math.abs(A.x - B.x) + Math.abs(A.y - B.y);
    }

    static boolean isDirAdj(Direction dir, Direction dir2) {
        switch (dir) {
            case NORTH:
                switch (dir2) {
                    case NORTH:
                    case NORTHEAST:
                    case NORTHWEST:
                        return true;
                    default:
                        return false;
                }
            case NORTHEAST:
                switch (dir2) {
                    case NORTH:
                    case NORTHEAST:
                    case EAST:
                        return true;
                    default:
                        return false;
                }
            case EAST:
                switch (dir2) {
                    case NORTHEAST:
                    case EAST:
                    case SOUTHEAST:
                        return true;
                    default:
                        return false;
                }
            case SOUTHEAST:
                switch (dir2) {
                    case EAST:
                    case SOUTHEAST:
                    case SOUTH:
                        return true;
                    default:
                        return false;
                }
            case SOUTH:
                switch (dir2) {
                    case SOUTHEAST:
                    case SOUTH:
                    case SOUTHWEST:
                        return true;
                    default:
                        return false;
                }
            case SOUTHWEST:
                switch (dir2) {
                    case SOUTH:
                    case SOUTHWEST:
                    case WEST:
                        return true;
                    default:
                        return false;
                }
            case WEST:
                switch (dir2) {
                    case SOUTHWEST:
                    case WEST:
                    case NORTHWEST:
                        return true;
                    default:
                        return false;
                }
            case NORTHWEST:
                switch (dir2) {
                    case WEST:
                    case NORTHWEST:
                    case NORTH:
                        return true;
                    default:
                        return false;
                }
            default:
                return false;
        }
    }

    public static int encodeBits(boolean... bits) {
        if (bits.length > 6) {
            throw new IllegalArgumentException("Too many bits to encode (maximum 6 bits)");
        }

        int encodedValue = 0;

        for (int i = 0; i < bits.length; i++) {
            if (bits[i]) {
                encodedValue |= (1 << i);
            }
        }

        return encodedValue;
    }

    public static boolean[] decodeBits(int encodedValue, int numBits) {
        boolean[] decodedBits = new boolean[numBits];

        for (int i = 0; i < numBits; i++) {
            decodedBits[i] = ((encodedValue >> i) & 1) == 1;
        }

        return decodedBits;
    }

    static int[][] supportingPositionDxDy = new int[][] {
            { -3, 1 }, { 2, -3 }, { -3, 3 }, { 1, -2 }, { 2, 1 }, { -1, 2 }, { 3, 0 }, { 3, -3 }, { 1, 3 }, { 1, 2 },
            { 0, -2 }, { -3, 0 }, { 1, -3 }, { 3, 3 }, { 3, 1 }, { 0, 2 }, { 2, 3 }, { 3, -2 }, { -2, -3 }, { 2, -1 },
            { 2, 0 }, { -2, 3 }, { -3, -1 }, { -2, 2 }, { 0, 3 }, { -3, -2 }, { -2, 1 }, { -2, 0 }, { -1, -3 },
            { -2, -2 }, { 2, 2 }, { -2, -1 }, { -1, 3 }, { -3, -3 }, { 3, -1 }, { 3, 2 }, { -3, 2 }, { 0, -3 },
            { 2, -2 }, { -1, -2 }
    };

    static int[][] defendingTraps = new int[][] {
            { -4, 4 }, { 0, 4 }, { 4, 4 },
            { -2, 2 }, { 2, 2 },
            { -4, 1 }, { 4, 1 },
            { -2, 0 }, { 2, 0 },
            { -4, -1 }, { 4, -1 },
            { -2, -2 }, { 2, -2 },
            { -4, -4 }, { 0, -4 }, { 4, -4 }
    };

    /**
     * REMEMBER EACH SHOCK TRAP triggers on 1 either side.
     * T - - - T - - - T
     * - - T - - - T - -
     * T - - * * * - - T
     * - - T * X * T - -
     * T - - * * * - - T
     * - - T - - - T - -
     * T - - - T - - - T
     */
    // ROBOT CAN copy the array, and as they check/churn off each one, they can
    // remove it from the array locally. incase it's a wall, etc

}
