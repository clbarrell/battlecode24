package duck7;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.FlagInfo;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapInfo;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

public class Navigation extends RobotPlayer {

    static MapLocation target;
    static MapLocation lastLoc = null;

    static boolean[] impassable = null;
    static int movementCooldown;
    static boolean turnRight;
    static int symmetryGuess = -1; // 0 rotational, 1 x reflection, 2 y reflection
    static String[] symmetryGuessWords = { "rotational", "x reflection", "y reflection" };

    static MapLocation[] spawnLocs = rc.getAllySpawnLocations();

    static int myRandomDir = 0;

    static public void move(MapLocation loc) throws GameActionException {
        rc.setIndicatorLine(myLocation, loc, 0, 0, 255);
        if (!rc.isMovementReady()) {
            // Debug.log("Movement not ready"); // because of taking an action
            return;
        }

        target = loc;
        BugNav.move();
        // if (!BugNav.move())
        // greedyPath();
        // Astar.runUntilFree(loc);
        lastLoc = loc;
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
        MapLocation[] spawnsToIgnore = Comm.getEnemyBadSpawnPoints();
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
        // Debug.log("Invalid symm: " + invalidSymmetryOptions[0] + ", " +
        // invalidSymmetryOptions[1] + ", "
        // + invalidSymmetryOptions[2]);

        MapLocation[] ourFlagLocations = Comm.getFlagLocations();
        // go to center of map if no flag locations saved
        if (ourFlagLocations.length == 0) {
            Debug.log("No flag locations saved, going to center of map");
            return new MapLocation(width / 2, height / 2);
        }

        for (MapLocation flag : ourFlagLocations) {
            if (flag == null)
                continue;
            int[][] enemySpawns = calculateSymmetryOptions(flag);
            // check whether any of the enemy spawns are valid
            // Debug.log(
            // flag + ":, Options: [" + enemySpawns[0][0] + ", " + enemySpawns[0][1] + "],
            // ["
            // + enemySpawns[1][0] + ", "
            // + enemySpawns[1][1] + "], [" + enemySpawns[2][0] + ", " + enemySpawns[2][1] +
            // "]");
            for (int i = 0; i < invalidSymmetryOptions.length; i++) {
                if (invalidSymmetryOptions[i] == 1) {
                    // Debug.log("Symm invalid: " + symmetryGuessWords[i]);
                    continue;
                }

                // Debug.log("Checking: " + symmetryGuessWords[i]);
                boolean isValid = true;
                MapLocation symmOption = new MapLocation(enemySpawns[i][0], enemySpawns[i][1]);
                // see if symmOption is in badSpawnLocations
                for (MapLocation badSpawn : spawnsToIgnore) {
                    if (badSpawn == null)
                        continue;
                    if (badSpawn.equals(symmOption)) {
                        isValid = false;
                        break;
                    }
                }
                for (MapLocation spawn : ourFlagLocations) {
                    if (spawn == null)
                        continue;
                    if (distanceBetween(symmOption, spawn) < 7.2) {
                        // Debug.log("Too close to spawn: " + spawn.toString() + ", option: " +
                        // symmOption.toString());
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
                        // Debug.log("Making guess: " + symmetryGuessWords[i] + ", " + symmOption);
                    }
                }
                // I could write to the shared array now, just writing off an option? But
                // all robots will write it off anyway
            }
        }

        if (closestEnemySpawn == null) {
            // no valid enemy spawns with flags, go to center of map
            // Debug.log("No valid enemy spawns, going to center of map");
            closestEnemySpawn = ourFlagLocations[rng.nextInt(ourFlagLocations.length)];
            if (closestEnemySpawn == null) {
                Debug.log("No flag locations saved, going to center of map. FlagS: "
                        + Debug.printMapLocations(ourFlagLocations));
                return new MapLocation(width / 2, height / 2);
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
        myRandomDir = rng.nextInt(directions.length);
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
        static boolean rotateRight = true; // if I should rotate right or left around obstacles
        static MapLocation lastObstacleFound = null; // latest obstacle I've found in my way
        static int minDistToEnemy = INF; // minimum distance I've been to the enemy while going around an obstacle
        static MapLocation prevTarget = null; // previous target
        static ArrayList<Integer> visited = new ArrayList<Integer>();
        static Direction dirTryingToGo = null;
        static ArrayList<MapLocation> lPath = new ArrayList<MapLocation>();
        static ArrayList<MapLocation> rPath = new ArrayList<MapLocation>();
        static MapLocation current = null;
        static MapLocation lastLObstacle = null;
        static MapLocation lastRObstacle = null;

        // my vars
        static ArrayList<Integer> wallOrWater = new ArrayList<Integer>();
        // static ArrayList<Integer> myPastLocs = new ArrayList<Integer>();

        static boolean move() {
            try {
                // different target? ==> previous data does not help!
                if (prevTarget == null || target.distanceSquaredTo(prevTarget) > 0) {
                    // Debug.println("New target: " + target, id);
                    resetPathfinding();
                }

                // If I'm at a minimum distance to the target, I'm free!
                MapLocation myLoc = rc.getLocation();
                current = myLoc;
                int d = Util.distance(myLoc, target);
                if (d < minDistToEnemy) {
                    // Debug.log("New min dist: " + d + " Old: " + minDistToEnemy);
                    if (lastObstacleFound != null) {
                        // Debug.log("broke free from!!: " + lastObstacleFound);
                        resetPathfinding();
                        minDistToEnemy = d;
                    }
                }

                int myLocEncoded = Comm.encodeCoordinates(myLoc.x, myLoc.y);
                // myPastLocs.add(myLocEncoded);

                // Update data
                prevTarget = target;

                Direction dir = myLoc.directionTo(target);
                if (dir == Direction.CENTER) {
                    // no need to move
                    return true;
                }

                // If there's an obstacle I try to go around it [until I'm free] instead of
                // going to the target directly
                // I'll keep turning r/l until I can go next to it.
                if (lastObstacleFound != null) {
                    // Debug.println("Last obstacle found: " + lastObstacleFound, id);
                    dir = myLoc.directionTo(lastObstacleFound);
                }

                // CAN MOVE
                if (canMove(dir)) {
                    // Success - go in straight line.
                    // Debug.log("can move: " + dir);
                    resetPathfinding();
                    rc.move(dir);
                    return true;
                } else {
                    // Obstacle. Follow obstacle boundary. STUCK!
                    minDistToEnemy = d;
                    MapLocation newLoc = myLoc.add(dir);
                    rc.setIndicatorDot(newLoc, 255, 0, 0);
                    if (rc.onTheMap(newLoc)) {
                        MapInfo mi = rc.senseMapInfo(newLoc);
                        boolean isRobot = rc.senseRobotAtLocation(newLoc) != null;
                        if (!mi.isWall() && !mi.isWater() && !isRobot && roundNum <= GameConstants.SETUP_ROUNDS) {
                            // Debug.log("Cant go " + newLoc.toString() + " probs barrier");
                            myState = States.WAITING_AT_BARRIER;
                            return true;
                        }

                        if (mi.isWater()) {
                            if (mi.getCrumbs() > 0) {
                                rc.fill(mi.getMapLocation());
                                return true;
                            }

                            // fill only if there are less than 5 water cells
                            int cellsWithWater = 1;
                            // already chcekd next
                            int maxWaterCells = (int) Math.ceil(width * 0.18);
                            for (int i = 0; i < maxWaterCells; i++) {
                                MapLocation nextLoc = newLoc.add(dir);
                                if (rc.onTheMap(nextLoc) && rc.senseMapInfo(nextLoc).isWater())
                                    cellsWithWater++;
                            }

                            if (rc.canFill(mi.getMapLocation()) && cellsWithWater < maxWaterCells) {
                                rc.fill(mi.getMapLocation());
                                return true;
                            }
                        }
                    }
                }

                // only do this if hitting obs for 1st time
                if (lastObstacleFound == null && (myState != States.SUPPORTING_FLAG_CARRIER
                        || myState != States.ATTACKING || myState != States.MOVING_TO_ATTACK)) {
                    int dR = INF;
                    int dL = INF;
                    lastRObstacle = myLoc.add(dir);
                    lastLObstacle = lastRObstacle;
                    for (int i = 0; i < 4; i++) {
                        // build lpath and right path
                        MapLocation l = getNextPassableDirection(false, i > 0 ? null : dir);
                        if (l != null) {
                            rc.setIndicatorDot(l, 0, 255, 0);
                            dL = Util.distance(l, target);
                            lPath.add(l);
                        }
                        MapLocation r = getNextPassableDirection(true, i > 0 ? null : dir);
                        if (r != null) {
                            rc.setIndicatorDot(r, 80, 80, 255);
                            rPath.add(r);
                            dR = Util.distance(r, target);
                        }
                        // keep iterating for them
                        // check the distance for the last location of L path and R path
                        // the closest to target wins

                        // break out of loop if I'm closer than mniD
                        if (dR < 3 || dL < 3) {
                            // Debug.log("Breaking out of loop early");
                            break;
                        }
                    }
                    // also set rotateRight & lastobs for future
                    dir = chooseBestDir();
                    if (dir == null) {
                        Debug.log("WARNING!! No best dir found");
                        return false;
                    }
                    // reset
                    rPath.clear();
                    lPath.clear();
                    lastRObstacle = null;
                    lastLObstacle = null;
                } else {
                    // turn based on turnRight
                    for (int j = 8; j-- > 0;) {
                        if (rotateRight) {
                            dir = dir.rotateRight();
                        } else {
                            dir = dir.rotateLeft();
                        }
                        if (canMove(dir)) {
                            if (lastLoc == null) {
                                lastLoc = myLocation;
                                break;
                            } else if (!myLocation.add(dir).equals(lastLoc)) {
                                // Can move and it isn't my location
                                break;
                            } else {
                                Debug.log("can move but same loc");
                                MapLocation newLoc = myLoc.add(dir);
                                rc.setIndicatorDot(newLoc, 100, 100, 100);
                            }
                        } else {
                            MapLocation newLoc = myLoc.add(dir);
                            lastObstacleFound = newLoc;
                            rc.setIndicatorDot(newLoc, 255, 180, 180);
                        }
                    }
                }

                if (dir == null) {
                    Debug.log("WARNING!! No dir found at all");
                    return false;
                }

                if (canMove(dir))
                    rc.move(dir);

            } catch (Exception e) {
                e.printStackTrace();
            }
            // Debug.println("Last exit", id);
            return true;
        }

        private static Direction chooseBestDir() {
            if (lPath.size() == 0 && rPath.size() == 0) {
                Debug.log("WARNING!! No L/R paths found");
                return null;
            }

            if (lPath.size() == 0) {
                rotateRight = true;
                return rPath.get(rPath.size() - 1).directionTo(target);

            }

            if (rPath.size() == 0) {
                rotateRight = false;
                return lPath.get(lPath.size() - 1).directionTo(target);
            }

            // Debug.log("Passed the checks for null paths");

            MapLocation lLast = lPath.get(lPath.size() - 1);
            MapLocation rLast = rPath.get(rPath.size() - 1);
            int lDist = Util.distance(lLast, target);
            int rDist = Util.distance(rLast, target);
            if (lDist < rDist) {
                rotateRight = false;
                lastObstacleFound = lastLObstacle;
                // Debug.log("[L] Setting lastObsFound to " + lastObstacleFound);
                return myLocation.directionTo(lPath.get(0));
            } else if (rDist < lDist) {
                rotateRight = true;
                lastObstacleFound = lastRObstacle;
                // Debug.log("[R] Setting lastObsFound to " + lastObstacleFound);
                return myLocation.directionTo(rPath.get(0));
            } else {
                // same distance
                if (lPath.size() < rPath.size()) {
                    rotateRight = false;
                    lastObstacleFound = lastLObstacle;
                    // Debug.log("[L] Setting lastObsFound to " + lastObstacleFound);
                    return myLocation.directionTo(lPath.get(0));
                } else {
                    rotateRight = true;
                    lastObstacleFound = lastRObstacle;
                    // Debug.log("[R] Setting lastObsFound to " + lastObstacleFound);
                    return myLocation.directionTo(rPath.get(0));
                }
            }
        }

        // clear some of the previous data
        static void resetPathfinding() {
            // Debug.println("Resetting pathfinding", id);
            lastObstacleFound = null;
            minDistToEnemy = INF;
            visited.clear();
            shouldGuessRotation = true;
            dirTryingToGo = null;
            rotateRight = turnRight;
        }

        /**
         * A loop I can recursively call with new turns/moves/
         * 
         * @param right
         * @param dir
         * @return
         * @throws GameActionException
         */
        static MapLocation getNextPassableDirection(boolean right, Direction dir) throws GameActionException {
            MapLocation obs = right ? lastRObstacle : lastLObstacle;
            MapLocation start = rPath.size() == 0 ? myLocation
                    : right ? rPath.get(rPath.size() - 1)
                            : lPath.get(lPath.size() - 1);
            dir = dir == null ? start.directionTo(obs) : dir;
            // Debug.log(
            // "Checking " + (right ? "right" : "left") + " dir: " + dir.toString() + " from
            // " + start.toString());
            for (int j = 8; j-- > 0;) {
                if (right) {
                    dir = dir.rotateRight();
                } else {
                    dir = dir.rotateLeft();
                }

                MapLocation newLoc = start.add(dir);
                boolean canMove = false;
                if (newLoc.equals(myLocation)) {
                    canMove = rc.canMove(dir);
                } else {
                    canMove = rc.canSenseLocation(newLoc) && rc.onTheMap(newLoc)
                            && !rc.senseMapInfo(newLoc).isWall() && !rc.senseMapInfo(newLoc).isWater(); // water?
                }
                if (canMove) {
                    // Debug.log(" Next dir for " + (right ? "right" : "left") + " is " + dir + " to
                    // " + newLoc);
                    return newLoc;
                } else {
                    rc.setIndicatorDot(newLoc, 255, 180, 180);
                    if (rotateRight) {
                        lastRObstacle = newLoc;
                    } else {
                        lastLObstacle = newLoc;
                    }
                }
            }

            return null;
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
            // Debug.log("going to priority flag: " + priorityFlag.toString());
            // Debug.log("Priority flag in shared Array " +
            // Comm.readMapLocationFromIndex(Comm.PRIORITY_FLAG_INDEX));
            return priorityFlag;
        }

        // start with symmetry then, can I see which rotation is right?
        if (nearestEnemySpawnPoint != null && symmetryGuess != -1) {
            // Debug.log("Dist to enemy spawn: " + nearestEnemySpawnPoint.toString() + ", "
            // + myLocation.distanceSquaredTo(nearestEnemySpawnPoint));
            if (rc.canSenseLocation(nearestEnemySpawnPoint)) {
                // if this actually a spawn point
                MapInfo potentialSpawn = rc.senseMapInfo(nearestEnemySpawnPoint);
                FlagInfo[] flags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
                if (!potentialSpawn.isSpawnZone()) {
                    // this symmetry guess was wrong.
                    Comm.shareFailedSymmetry(symmetryGuess);
                    // Debug.log("This symmetry guess was wrong: " +
                    // symmetryGuessWords[symmetryGuess]);
                    nearestEnemySpawnPoint = null;
                } else if (flags.length == 0) {
                    // no flags at this spawn point, it's crap, get a new one.
                    Comm.shareBadSpawnPoint(nearestEnemySpawnPoint);
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
            // Debug.log("going to flag: " + flags[0].toString());
            return flags[0];
        }

        nearestEnemySpawnPoint = nearestEnenySpawn();
        // Debug.log("going to new spawnPoint: " + nearestEnemySpawnPoint.toString());

        return nearestEnemySpawnPoint;

        // then look for broadcast flag location

    }

    public static int encodeLoc(MapLocation loc) {
        return Comm.encodeCoordinates(loc.x, loc.y);
    }

    static MapLocation decodeLoc(int encoded) {
        int[] coords = Comm.decodeCoordinates(encoded);
        return new MapLocation(coords[0], coords[1]);
    }

    static class Astar {

        static void runUntilFree(MapLocation target) throws GameActionException {
            PriorityQueue<Node> frontier = new PriorityQueue<Node>(new NodeComparator());
            ArrayList<MapLocation> visited = new ArrayList<MapLocation>();
            HashMap<Integer, Integer> cameFrom = new HashMap<Integer, Integer>();
            HashMap<Integer, Integer> costsSoFar = new HashMap<Integer, Integer>();
            // HashMap<Integer, Integer> distToTarget = new HashMap<Integer, Integer>();
            Node current = null;

            frontier.add(new Node(myLocation, 0, 0));
            cameFrom.put(Navigation.encodeLoc(myLocation), null);
            costsSoFar.put(Navigation.encodeLoc(myLocation), 0);

            frontierLoop: while (frontier.size() > 0) {
                // Debug.log("Frontier size" + frontier.size() + ", Bytecodes left: " +
                // Clock.getBytecodesLeft()
                // + ", Visited size: " + visited.size());
                if (Clock.getBytecodesLeft() < 4000) {
                    Debug.log("Less than 4k bytecodes left because of A*");
                    break;
                }

                current = frontier.poll();
                // Debug.log("A* Checking: " + current.loc.toString() + ", " + current.cost);
                visited.add(current.loc);
                int currentCode = Navigation.encodeLoc(current.loc);

                if (current.loc.equals(target)) {
                    break;
                }

                Direction[] validDirs = frontier.size() == 0 ? initialDirs(current.loc, target) : directions;

                for (Direction dir : validDirs) {
                    MapLocation next = current.loc.add(dir);
                    if (next.x < 0 || next.y < 0 || next.x >= width || next.y >= height)
                        continue;
                    int nextCode = Navigation.encodeLoc(next);

                    boolean canSense = rc.canSenseLocation(next);
                    if (rc.onTheMap(next) && !canSense) {
                        // break the while loop
                        break frontierLoop;
                    }

                    // Can I even go to it?
                    if (rc.onTheMap(next) && canSense) {
                        // sense?
                        MapInfo nextInfo = rc.senseMapInfo(next);
                        // RobotInfo nextRobot = rc.senseRobotAtLocation(next);
                        if (nextInfo.isWall()) {
                            continue;
                        }

                        int newCost = costsSoFar.get(currentCode) + (nextInfo.isWater() ? 2 : 1);

                        // is it worth it?
                        if (!costsSoFar.containsKey(nextCode)
                                || newCost < costsSoFar.get(nextCode)) { // and I can go there!
                            costsSoFar.put(nextCode, newCost);
                            frontier.add(new Node(next, newCost, Util.manhattan(next, target)));
                            cameFrom.put(nextCode, Navigation.encodeLoc(current.loc));
                        }
                    }

                }
            }

            // have to decide which last direction to keep!
            // String s = "";
            // for (int i = 0; i < 6; i++) {
            // if (visited.size() - 1 - i < 0)
            // break;
            // s += visited.get(visited.size() - 1 - i).toString() + ", ";
            // }
            // Debug.log("Last 6 visited: " + s);

            // reconstruct path
            // Debug.log("Current: " + current.loc.toString());
            // String path = "in reverse: ";
            int currentCode = Navigation.encodeLoc(current.loc);
            int cameFromCode = cameFrom.get(currentCode);

            while (cameFromCode != Navigation.encodeLoc(myLocation)) {
                // path += decodeLoc(cameFromCode).toString() + ", ";
                MapLocation loc = decodeLoc(cameFromCode);
                rc.setIndicatorDot(loc, 255, 90, 90);
                currentCode = cameFromCode;
                cameFromCode = cameFrom.get(currentCode);
            }
            // Debug.log("Path " + path);

        }

        private static Direction[] initialDirs(MapLocation loc, MapLocation target) {
            Direction d = loc.directionTo(target);
            int i = 0;
            for (int x = 0; x < directions.length; x++) {
                if (d == directions[x]) {
                    i = x;
                    break;
                }
            }
            return Util.dirsToCheckAS[i];
        }

    }

    public static void explore() throws GameActionException {
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        if (allies.length == 0) {
            // move randomly
            // round the roundNum to the nearest 10
            Direction dir = directions[myRandomDir % (directions.length)];
            MapLocation nextLoc = myLocation.add(dir);
            move(nextLoc);
        } else {
            RobotInfo closestAlly = getClosestRI(allies);
            Direction dirToAlly = myLocation.directionTo(closestAlly.location);
            MapLocation nextLoc = myLocation.add(dirToAlly.opposite());
            // but not off the map
            while (!rc.onTheMap(nextLoc)) {
                Direction randomDir = directions[rng.nextInt(directions.length)];
                nextLoc = myLocation.add(randomDir.opposite());
                // Debug.log("Had to change direction to stay on map");
            }
            move(nextLoc);
        }

        if (roundNum % 15 == 0) {
            myRandomDir += 4;
        }
    }

    static class Node {
        MapLocation loc;
        int cost;
        int distToTarget;
        int totalCost;

        public Node(MapLocation loc, int cost, int distToTarget) {
            this.loc = loc;
            this.cost = cost;
            this.distToTarget = distToTarget;
            this.totalCost = cost + distToTarget;
        }

        public Node(MapLocation loc, int cost) {
            this.loc = loc;
            this.cost = cost;
        }

        public Node(MapLocation loc) {
            this.loc = loc;
        }

        public int compareTo(Node other) {
            return this.totalCost - other.totalCost;
        }
    }

    static class NodeComparator implements Comparator<Node> {
        public int compare(Node n1, Node n2) {
            return n1.compareTo(n2);
        }
    }

}
