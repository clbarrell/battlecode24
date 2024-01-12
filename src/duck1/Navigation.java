package duck1;

import java.util.ArrayList;
import java.util.Map;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapInfo;
import battlecode.common.MapLocation;

public class Navigation extends RobotPlayer {

    static MapLocation target;
    static boolean[] impassable = null;
    static int movementCooldown;
    static boolean turnRight;
    static int symmetryGuess = -1; // 0 rotational, 1 x reflection, 2 y reflection
    static String[] symmetryGuessWords = { "rotational", "x reflection", "y reflection" };

    static MapLocation[] spawnLocs = rc.getAllySpawnLocations();

    static public void move(MapLocation loc) {
        rc.setIndicatorLine(myLocation, loc, 0, 0, 255);
        if (!rc.isMovementReady()) {
            // Debug.log("Movement not ready"); // because of taking an action
            return;
        }

        target = loc;
        // if (!BugNav.move())
        greedyPath();
    }

    /**
     * @return the location of a safe location to place a flag, on 'our' side of the
     *         map
     * @throws GameActionException
     */
    static public MapLocation getSafeLocation() throws GameActionException {
        MapLocation safePlace;

        // find which corner I am in
        if (myLocation.x < width / 2) {
            // left side
            if (myLocation.y < height / 2) {
                // bottom left
                safePlace = new MapLocation(0, 0);
            } else {
                // top left
                safePlace = new MapLocation(0, height);
            }
        } else {
            // right side
            if (myLocation.y < height / 2) {
                // bottom right
                safePlace = new MapLocation(width, 0);
            } else {
                // top right
                safePlace = new MapLocation(width, height);
            }
        }
        Debug.log("Safe place: " + safePlace.toString());
        // check whether it is safe and alter
        safePlace = Comm.safeFlagLocation(safePlace);

        return safePlace;
    }

    private static MapLocation nearestEnemySpawnPoint = null;

    static MapLocation nearestEnenySpawn() throws GameActionException {
        if (nearestEnemySpawnPoint != null) {
            return nearestEnemySpawnPoint;
        }

        // TODO: This iterates through too many options. have 1 robot calculate the
        // options based on the flag location.T hen just pick one.

        // for each spawnLocs get each symmetry option
        double closestDistance = Double.MAX_VALUE;
        MapLocation closestEnemySpawn = null;
        // 0 unknown, 1 invalid
        int[] invalidSymmetryOptions = Comm.getfailedSymmetry(); // 0 rotational, 1 x reflection, 2 y reflection
        Debug.log("Invalid symm: " + invalidSymmetryOptions[0] + ", " + invalidSymmetryOptions[1] + ", "
                + invalidSymmetryOptions[2]);

        MapLocation[] ourFlagLocations = Comm.getFlagLocations();
        // go to center of map if no flag locations saved
        if (ourFlagLocations.length == 0) {
            return new MapLocation(width / 2, height / 2);
        }

        for (MapLocation flag : ourFlagLocations) {
            if (flag == null)
                continue;
            int[][] enemySpawns = calculateSymmetryOptions(flag);
            // check whether any of the enemy spawns are valid
            Debug.log(
                    flag + ":, Options: [" + enemySpawns[0][0] + ", " + enemySpawns[0][1] + "], ["
                            + enemySpawns[1][0] + ", "
                            + enemySpawns[1][1] + "], [" + enemySpawns[2][0] + ", " + enemySpawns[2][1] + "]");
            for (int i = 0; i < invalidSymmetryOptions.length; i++) {
                if (invalidSymmetryOptions[i] == 1) {
                    // Debug.log("Symm invalid: " + symmetryGuessWords[i]);
                    continue;
                }

                Debug.log("Checking: " + symmetryGuessWords[i]);
                boolean isValid = true;
                MapLocation symmOption = new MapLocation(enemySpawns[i][0], enemySpawns[i][1]);
                for (MapLocation spawn : ourFlagLocations) {
                    if (spawn == null)
                        continue;
                    if (distanceBetween(symmOption, spawn) < 7.2) {
                        Debug.log("Too close to spawn: " + spawn.toString() + ", option: " + symmOption.toString());
                        invalidSymmetryOptions[i] = 1;
                        isValid = false;
                        break;
                    }
                }

                if (isValid) {
                    double dist = distanceBetween(symmOption, myLocation);
                    if (dist < closestDistance) {
                        closestDistance = dist;
                        closestEnemySpawn = symmOption;
                        symmetryGuess = i;
                        Debug.log("Making guess: " + symmetryGuessWords[i] + ", " + symmOption);
                    }
                }
                // I could write to the shared array now, just writing off an option? But
                // all robots will write it off anyway
            }
        }

        nearestEnemySpawnPoint = closestEnemySpawn;
        return closestEnemySpawn;
    }

    static int[][] calculateSymmetryOptions(MapLocation loc) {
        // save a new variable with all the calculate symmetry options
        return new int[][] {
                calculateRotationalSymmetry(new int[] { loc.x, loc.y }),
                calculateXReflectionSymmetry(new int[] { loc.x, loc.y }),
                calculateYReflectionSymmetry(new int[] { loc.x, loc.y })
        };
    }

    private static int[] calculateRotationalSymmetry(int[] coordinates) {
        return new int[] { width - myLocation.x, height - myLocation.y };
    }

    private static int[] calculateXReflectionSymmetry(int[] coordinates) {
        int y = height - coordinates[1];
        return new int[] { myLocation.x, y };
    }

    private static int[] calculateYReflectionSymmetry(int[] coordinates) {
        int x = width - coordinates[0];
        return new int[] { x, myLocation.y };
    }

    // private static double distanceBetween(int[] coord1, int[] coord2) {
    // int deltaX = coord1[0] - coord2[0];
    // int deltaY = coord1[1] - coord2[1];
    // return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    // }

    private static double distanceBetween(MapLocation coord1, MapLocation coord2) {
        int deltaX = coord1.x - coord2.x;
        int deltaY = coord1.y - coord2.y;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    static final double eps = 1e-5;

    static void greedyPath() {
        try {
            MapLocation myLoc = rc.getLocation();
            Direction bestDir = null;
            double bestEstimation = 0;
            int bestEstimationDist = 0;
            for (Direction dir : directions) {
                MapLocation newLoc = myLoc.add(dir);
                if (!rc.onTheMap(newLoc))
                    continue;

                if (!canMove(dir))
                    continue;
                if (!strictlyCloser(newLoc, myLoc, target))
                    continue;

                int newDist = newLoc.distanceSquaredTo(target);

                // TODO: Better estimation?
                double estimation = 1 + Util.distance(target, newLoc);
                if (bestDir == null || estimation < bestEstimation - eps
                        || (Math.abs(estimation - bestEstimation) <= 2 * eps && newDist < bestEstimationDist)) {
                    bestEstimation = estimation;
                    bestDir = dir;
                    bestEstimationDist = newDist;
                }
            }
            if (bestDir != null)
                rc.move(bestDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void setImpassable(boolean[] imp) {
        impassable = imp;
    }

    static void init() {
        impassable = new boolean[directions.length];
        turnRight = rng.nextBoolean();
    }

    static boolean strictlyCloser(MapLocation newLoc, MapLocation oldLoc, MapLocation target) {
        int dOld = Util.distance(target, oldLoc), dNew = Util.distance(target, newLoc);
        if (dOld < dNew)
            return false;
        if (dNew < dOld)
            return true;
        return target.distanceSquaredTo(newLoc) < target.distanceSquaredTo(oldLoc);
    }

    static boolean canMove(Direction dir) {
        return rc.canMove(dir);
    }

    static class BugNav {

        static final int INF = 1000000;
        static final int MAX_MAP_SIZE = GameConstants.MAP_MAX_HEIGHT;

        static boolean shouldGuessRotation = true; // if I should guess which rotation is the best
        static boolean rotateRight = true; // if I should rotate right or left
        static MapLocation lastObstacleFound = null; // latest obstacle I've found in my way
        static int minDistToEnemy = INF; // minimum distance I've been to the enemy while going around an obstacle
        static MapLocation prevTarget = null; // previous target
        static ArrayList<Integer> visited = new ArrayList<Integer>();

        // my vars
        static ArrayList<Integer> wallOrWater = new ArrayList<Integer>();
        static ArrayList<Integer> myPastLocs = new ArrayList<Integer>();

        static boolean move() {
            try {
                // different target? ==> previous data does not help!
                if (prevTarget == null || target.distanceSquaredTo(prevTarget) > 0) {
                    // Debug.println("New target: " + target, id);
                    resetPathfinding();
                }

                // If I'm at a minimum distance to the target, I'm free!
                MapLocation myLoc = rc.getLocation();
                // int d = myLoc.distanceSquaredTo(target);
                int d = Util.distance(myLoc, target);
                if (d < minDistToEnemy) {
                    // Debug.println("New min dist: " + d + " Old: " + minDistToEnemy, id);
                    resetPathfinding();
                    minDistToEnemy = d;
                }

                int myLocEncoded = Comm.encodeCoordinates(myLoc.x, myLoc.y);
                myPastLocs.add(myLocEncoded);

                // Update data
                prevTarget = target;

                Direction dir = myLoc.directionTo(target);
                // // If there's an obstacle I try to go around it [until I'm free] instead of
                // // going to the target directly
                // if (lastObstacleFound != null) {
                // // Debug.println("Last obstacle found: " + lastObstacleFound, id);
                // dir = myLoc.directionTo(lastObstacleFound);
                // }
                // MapLocation nextLoc = myLoc.add(dir);

                // CAN MOVE
                if (canMove(dir)) {
                    // Debug.log("can move: " + dir);
                    resetPathfinding();
                    rc.move(dir);
                    return true;
                } else {
                    MapLocation newLoc = myLoc.add(dir);
                    if (rc.onTheMap(newLoc)) {
                        MapInfo mi = rc.senseMapInfo(myLoc.add(dir));
                        boolean isRobot = rc.senseRobotAtLocation(newLoc) != null;
                        if (!mi.isWall() && !mi.isWater() && !isRobot && roundNum <= GameConstants.SETUP_ROUNDS) {
                            Debug.log("Cant go " + newLoc.toString() + " probs barrier");
                            myState = States.WAITING_AT_BARRIER;
                            return true;
                        }

                        if (myState == States.GETTING_CRUMB && mi.isWater()) {
                            // fill
                            if (rc.canFill(mi.getMapLocation())) {
                                rc.fill(mi.getMapLocation());
                            }
                            return true;
                        }
                    }
                }

                // turn based on turnRight
                for (int j = 8; j-- > 0;) {
                    if (turnRight) {
                        dir = dir.rotateRight();
                    } else {
                        dir = dir.rotateLeft();
                    }
                    if (canMove(dir))
                        break;
                }

                /**
                 * // Get valid movement options
                 * // Rotate left and right and find the first dir that you can move in
                 * Direction dirL = dir;
                 * for (int j = 8; j-- > 0;) {
                 * dirL = dirL.rotateLeft();
                 * int encoded = Comm.encodeCoordinates(myLoc.x + dirL.dx, myLoc.y + dirL.dy);
                 * if (!myPastLocs.contains(encoded) && canMove(dirL))
                 * break;
                 * 
                 * MapLocation newLoc = myLoc.add(dirL);
                 * if (rc.onTheMap(newLoc)) {
                 * MapInfo mi = rc.senseMapInfo(myLoc.add(dirL));
                 * if (mi.isWall() || mi.isWater()) {
                 * wallOrWater.add(Comm.encodeCoordinates(newLoc.x, newLoc.y));
                 * }
                 * }
                 * }
                 * 
                 * Direction dirR = dir;
                 * for (int j = 8; j-- > 0;) {
                 * dirR = dirR.rotateRight();
                 * int encoded = Comm.encodeCoordinates(myLoc.x + dirR.dx, myLoc.y + dirR.dy);
                 * if (!myPastLocs.contains(encoded) && canMove(dirL))
                 * break;
                 * 
                 * MapLocation newLoc = myLoc.add(dirR);
                 * if (rc.onTheMap(newLoc)) {
                 * MapInfo mi = rc.senseMapInfo(myLoc.add(dirR));
                 * if (mi.isWall() || mi.isWater()) {
                 * wallOrWater.add(Comm.encodeCoordinates(newLoc.x, newLoc.y));
                 * }
                 * }
                 * }
                 * 
                 * // Check which results in a location closer to the target
                 * MapLocation locL = myLoc.add(dirL);
                 * MapLocation locR = myLoc.add(dirR);
                 * Debug.log("Looking at [L] " + locL.toString() + ", [R] " + locR.toString());
                 * 
                 * int lDist = Util.distance(target, locL);
                 * int rDist = Util.distance(target, locR);
                 * int lDistSq = target.distanceSquaredTo(locL);
                 * int rDistSq = target.distanceSquaredTo(locR);
                 * 
                 * if (lDist < rDist) {
                 * rotateRight = false;
                 * } else if (rDist < lDist) {
                 * rotateRight = true;
                 * } else {
                 * rotateRight = rDistSq < lDistSq;
                 * }
                 **/

                if (canMove(dir))
                    rc.move(dir);

            } catch (Exception e) {
                e.printStackTrace();
            }
            // Debug.println("Last exit", id);
            return true;
        }

        // clear some of the previous data
        static void resetPathfinding() {
            // Debug.println("Resetting pathfinding", id);
            // lastObstacleFound = null;
            minDistToEnemy = INF;
            visited.clear();
            shouldGuessRotation = true;
        }

        // static int getCode() {
        // int x = rc.getLocation().x;
        // int y = rc.getLocation().y;
        // Direction obstacleDir = rc.getLocation().directionTo(target);
        // if (lastObstacleFound != null)
        // obstacleDir = rc.getLocation().directionTo(lastObstacleFound);
        // int bit = rotateRight ? 1 : 0;
        // return (((((x << 6) | y) << 4) | obstacleDir.ordinal()) << 1) | bit;
        // }
    }

    public static MapLocation bestEnemyLocationGuess() throws GameActionException {
        // Check priority flags
        MapLocation priorityFlag = Comm.getEnemyPriorityFlag();
        if (roundNum > GameConstants.SETUP_ROUNDS && priorityFlag != null) {
            Debug.log("going to priority flag: " + priorityFlag.toString());
            Debug.log("Priority flag in shared Array " + Comm.readMapLocationFromIndex(Comm.PRIORITY_FLAG_INDEX));
            return priorityFlag;
        }

        // start with symmetry then, can I see which rotation is right?
        if (nearestEnemySpawnPoint != null) {
            // Debug.log("Dist to enemy spawn: " + nearestEnemySpawnPoint.toString() + ", "
            // + myLocation.distanceSquaredTo(nearestEnemySpawnPoint));
            if (rc.canSenseLocation(nearestEnemySpawnPoint)) {
                // if this actually a spawn point
                MapInfo potentialSpawn = rc.senseMapInfo(nearestEnemySpawnPoint);
                if (!potentialSpawn.isSpawnZone()) {
                    // this symmetry guess was wrong.
                    Comm.shareFailedSymmetry(symmetryGuess);
                    Debug.log("This symmetry guess was wrong: " + symmetryGuessWords[symmetryGuess]);
                    nearestEnemySpawnPoint = null;
                }
            } else {
                // Occaisonaly check that my symmetry guess is still valid
                if (rng.nextBoolean()) {
                    int[] invalidSymmetryOpt = Comm.getfailedSymmetry(); // 0 rotational, 1 x reflection, 2 y reflection
                    if (invalidSymmetryOpt[symmetryGuess] == 1) {
                        nearestEnemySpawnPoint = null;
                        return bestEnemyLocationGuess();
                    }
                } else {

                    return nearestEnemySpawnPoint;
                }
            }
        }

        MapLocation[] flags = rc.senseBroadcastFlagLocations();
        if (flags.length > 0) {
            Debug.log("going to flag: " + flags[0].toString());
            // TODO: Send everyone to the one flag via broadcasting
            return flags[0];
        }

        nearestEnemySpawnPoint = nearestEnenySpawn();
        Debug.log("going to new spawnPoint: " + nearestEnemySpawnPoint.toString());

        return nearestEnemySpawnPoint;

        // then look for broadcast flag location

    }

    static int encodeLoc(MapLocation loc) {
        return Comm.encodeCoordinates(loc.x, loc.y);
    }

    static MapLocation decodeLoc(int encoded) {
        int[] coords = Comm.decodeCoordinates(encoded);
        return new MapLocation(coords[0], coords[1]);
    }

}
