package duck5;

import java.util.ArrayList;
import java.util.Map;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;

public class Comm extends RobotPlayer {

  private static final int MAX_COORDINATE = GameConstants.MAP_MAX_WIDTH;
  private static final int BITS_FOR_X = 6; // Adjust as needed based on the range

  // Cache local things needed in same turn
  private static int[] failedSymmetryCache = { 0, 0, 0 };

  private static MapLocation flag1 = null;
  private static MapLocation flag2 = null;
  private static MapLocation flag3 = null;
  private static MapLocation enemyFlag1 = null;
  private static MapLocation enemyFlag2 = null;
  private static MapLocation enemyFlag3 = null;
  private static MapLocation[] enemyBadSpawnPoints = { null, null, null };
  private static MapLocation missingFlagLocation = null;

  // Shared array has 64 slots
  // 0-2 are for confirming symmetry. 0 rotational, 1 x reflection, 2 y reflection
  // 0:
  /**
   * rotationalValid
   * rotationalInvalid
   * xReflectionValid
   * xReflectionInvalid
   * yReflectionValid
   * yReflectionInvalid
   */
  // 3-5 our flag locations
  private static int OURFLAG1INDEX = 3;
  private static int OURFLAG2INDEX = 4;
  private static int OURFLAG3INDEX = 5;
  // 6 = our robot count
  private static int robotCountIndex = 6;
  // 7 = priority flag destination
  // 8-10 enemy flag locations
  static int PRIORITY_FLAG_INDEX = 7;
  private static int ENEMY_FLAG_INDEX_1 = 8;
  private static int ENEMY_FLAG_INDEX_2 = 9;
  private static int ENEMY_FLAG_INDEX_3 = 10;
  // 11-13 enemy spawn locations to ignore
  private static int ENEMY_SPAWN_IGNORE_INDEX_1 = 11;
  private static int ENEMY_SPAWN_IGNORE_INDEX_2 = 12;
  private static int ENEMY_SPAWN_IGNORE_INDEX_3 = 13;
  // 14-16 spawn defenders
  private static int SPAWN_DEFENDER_INDEX_1 = 14;
  private static int SPAWN_DEFENDER_INDEX_2 = 15;
  private static int SPAWN_DEFENDER_INDEX_3 = 16;
  // 17-19 flag still there
  private static int FLAG_1_PRESENT_INDEX = 17;
  private static int FLAG_2_PRESENT_INDEX = 18;
  private static int FLAG_3_PRESENT_INDEX = 19;

  public static int anySymmetryConfirmed() throws GameActionException {
    int rotational = rc.readSharedArray(0);
    int xReflection = rc.readSharedArray(1);
    int yReflection = rc.readSharedArray(2);

    if (rotational == 1) {
      return 0;
    } else if (xReflection == 1) {
      return 1;
    } else if (yReflection == 1) {
      return 2;
    } else {
      return -1;
    }
  }

  /**
   * Depreciated! Not using
   * 
   * @param loc
   * @return
   * @throws GameActionException
   */
  public static MapLocation safeFlagLocation(MapLocation loc) throws GameActionException {
    // read maplocation via decoding from shared array index 4
    flag1 = readMapLocationFromIndex(OURFLAG1INDEX);
    if (flag1.equals(loc)) {
      // they're the same, so return a new maplocation adjacent to flag1 by 1 cell
      return flag1.add(flag1.directionTo(myLocation));
    }
    flag2 = readMapLocationFromIndex(OURFLAG2INDEX);
    if (flag2.equals(loc)) {
      // they're the same, so return a new maplocation adjacent to flag1 by 1 cell
      return flag1.add(flag1.directionTo(myLocation));
    }
    flag3 = readMapLocationFromIndex(OURFLAG3INDEX);
    if (flag3.equals(loc)) {
      // they're the same, so return a new maplocation adjacent to flag1 by 1 cell
      return flag1.add(flag1.directionTo(myLocation));
    }
    // no flags are targetted for this location, so return the location it is safe
    return loc;
  }

  /**
   * @return an array of our original flag/spawn locations [3]
   * @throws GameActionException
   */
  public static MapLocation[] getFlagLocations() throws GameActionException {
    MapLocation[] flags = new MapLocation[] { flag1, flag2, flag3 };
    if (flag1 == null) {
      flags[0] = readMapLocationFromIndex(OURFLAG1INDEX);
    }
    if (flag2 == null) {
      flags[1] = readMapLocationFromIndex(OURFLAG2INDEX);
    }
    if (flag3 == null) {
      flags[2] = readMapLocationFromIndex(OURFLAG3INDEX);
    }
    return flags;
  }

  public static void saveOurFlagLocation(MapLocation myFlagDestination) throws GameActionException {
    MapLocation[] flags = getFlagLocations();

    if (flags[0] == null) {
      writeMapLocationToIndex(myFlagDestination, OURFLAG1INDEX);
      flag1 = myFlagDestination;
    } else if (flags[1] == null) {
      writeMapLocationToIndex(myFlagDestination, OURFLAG2INDEX);
      flag2 = myFlagDestination;
    } else if (flags[2] == null) {
      writeMapLocationToIndex(myFlagDestination, OURFLAG3INDEX);
      flag3 = myFlagDestination;
    }
  }

  static void writeMapLocationToIndex(MapLocation loc, int i) throws GameActionException {
    if (loc == null) {
      rc.writeSharedArray(i, 0);
      return;
    }
    int encodedValue = encodeCoordinates(loc.x, loc.y);
    rc.writeSharedArray(i, encodedValue);
  }

  static MapLocation readMapLocationFromIndex(int i) throws GameActionException {
    int encodedValue = rc.readSharedArray(i);
    if (encodedValue == 0) {
      return null;
    }
    int[] decodedCoordinates = decodeCoordinates(encodedValue);
    return new MapLocation(decodedCoordinates[0], decodedCoordinates[1]);
  }

  public static int encodeCoordinates(int x, int y) {
    if (x < 0 || x > MAX_COORDINATE || y < 0 || y > MAX_COORDINATE) {
      throw new IllegalArgumentException("Coordinates out of range (0-60 inclusive)");
    }
    return (x << BITS_FOR_X) | y;
  }

  public static int[] decodeCoordinates(int encodedValue) {
    int x = encodedValue >> BITS_FOR_X;
    int y = encodedValue & ((1 << BITS_FOR_X) - 1);
    return new int[] { x, y };
  }

  public static void testEncoding(String[] args) {
    int x = 30; // Example X coordinate
    int y = 60; // Example Y coordinate

    int encodedValue = encodeCoordinates(x, y);
    System.out.println("Encoded value: " + encodedValue);

    int[] decodedCoordinates = decodeCoordinates(encodedValue);
    System.out.println("Decoded X: " + decodedCoordinates[0]);
    System.out.println("Decoded Y: " + decodedCoordinates[1]);
  }

  public static void shareFailedSymmetry(int symmetryGuess) throws GameActionException {
    failedSymmetryCache[symmetryGuess] = 1;

    int[] asymmetryInvalidIndex = { 1, 3, 5 };
    // rotationalValid
    // rotationalInvalid *
    // xReflectionValid
    // xReflectionInvalid *
    // yReflectionValid
    // yReflectionInvalid *

    // 0 rotational, 1 x reflection, 2 y reflection
    int asymmetryCoded = rc.readSharedArray(0);
    boolean[] asymmetry;
    if (asymmetryCoded == 0) {
      asymmetryCoded = 1;
      asymmetry = new boolean[] { false, false, false, false, false, false };
    } else {
      asymmetry = Util.decodeBits(asymmetryCoded, 6);
    }
    asymmetry[asymmetryInvalidIndex[symmetryGuess]] = true;
    asymmetryCoded = Util.encodeBits(asymmetry);
    rc.writeSharedArray(0, asymmetryCoded);
  }

  /**
   * @returns an array of ints, if any is 1 then that symmetry is invalid
   *          {rotational, xReflection, yReflection}
   * @throws GameActionException
   */
  public static int[] getfailedSymmetry() throws GameActionException {
    int[] failed = failedSymmetryCache;
    int asymmetryCoded = rc.readSharedArray(0);
    if (asymmetryCoded == 0) {
      return failed;
    }
    boolean[] asymmetry = Util.decodeBits(asymmetryCoded, 6);
    // rotationalValid
    // rotationalInvalid
    // xReflectionValid
    // xReflectionInvalid
    // yReflectionValid
    // yReflectionInvalid
    if (asymmetry[1]) {
      // x reflection is invalid
      failed[0] = 1;
    } else if (asymmetry[3]) {
      // x reflection is invalid
      failed[1] = 1;
    } else if (asymmetry[5]) {
      // x reflection is invalid
      failed[2] = 1;
    }
    return failed;
  }

  public static void iCameAlive() throws GameActionException {
    int current = Comm.readNumRobots();
    rc.writeSharedArray(robotCountIndex, current + 1);
  }

  public static int readNumRobots() throws GameActionException {
    return rc.readSharedArray(robotCountIndex);
  }

  public static MapLocation[] getEnemyFlagLocations() throws GameActionException {
    MapLocation[] flags = new MapLocation[] { enemyFlag1, enemyFlag2, enemyFlag3 };
    if (enemyFlag1 == null) {
      flags[0] = readMapLocationFromIndex(ENEMY_FLAG_INDEX_1);
    }
    if (enemyFlag2 == null) {
      flags[1] = readMapLocationFromIndex(ENEMY_FLAG_INDEX_2);
    }
    if (enemyFlag3 == null) {
      flags[2] = readMapLocationFromIndex(ENEMY_FLAG_INDEX_3);
    }
    return flags;
  }

  public static void reportEnemyFlagLocation(MapLocation location) throws GameActionException {
    MapLocation[] flags = getEnemyFlagLocations();

    // location already in spans?
    for (MapLocation flag : flags) {
      if (flag != null && flag.equals(location)) {
        return;
      }
    }

    if (readMapLocationFromIndex(PRIORITY_FLAG_INDEX) == null) {
      writeMapLocationToIndex(location, PRIORITY_FLAG_INDEX);
    }

    if (flags[0] == null) {
      writeMapLocationToIndex(location, ENEMY_FLAG_INDEX_1);
      enemyFlag1 = location;
    } else if (flags[1] == null) {
      writeMapLocationToIndex(location, ENEMY_FLAG_INDEX_2);
      enemyFlag2 = location;
    } else if (flags[2] == null) {
      writeMapLocationToIndex(location, ENEMY_FLAG_INDEX_3);
      enemyFlag3 = location;
    }
  }

  public static void clearEnemyPriorityFlag() throws GameActionException {
    rc.writeSharedArray(PRIORITY_FLAG_INDEX, 0);
  }

  public static MapLocation getEnemyPriorityFlag() throws GameActionException {
    return readMapLocationFromIndex(PRIORITY_FLAG_INDEX);
  }

  public static void clearEnemyFlagLocation(MapLocation location) throws GameActionException {
    MapLocation[] flags = getEnemyFlagLocations();
    MapLocation newPriority = null;
    int[] indexs = { ENEMY_FLAG_INDEX_1, ENEMY_FLAG_INDEX_2, ENEMY_FLAG_INDEX_3 };

    for (int i = 0; i < flags.length; i++) {
      MapLocation flag = flags[i];
      if (flag != null && flag.equals(location)) {
        writeMapLocationToIndex(null, indexs[i]);
        if (i == 0) {
          enemyFlag1 = null;
        } else if (i == 1) {
          enemyFlag2 = null;
        } else if (i == 2) {
          enemyFlag3 = null;
        }
      }

      if (flag != null && !flag.equals(location)) {
        newPriority = flag;
      }
    }

    if (newPriority != null) {
      writeMapLocationToIndex(newPriority, PRIORITY_FLAG_INDEX);
    } else {
      clearEnemyPriorityFlag();
    }

  }

  public static MapLocation[] getEnemyBadSpawnPoints() throws GameActionException {
    MapLocation[] spawns = enemyBadSpawnPoints;
    if (spawns[0] == null) {
      spawns[0] = readMapLocationFromIndex(ENEMY_SPAWN_IGNORE_INDEX_1);
    }
    if (spawns[1] == null) {
      spawns[1] = readMapLocationFromIndex(ENEMY_SPAWN_IGNORE_INDEX_2);
    }
    if (spawns[2] == null) {
      spawns[2] = readMapLocationFromIndex(ENEMY_SPAWN_IGNORE_INDEX_3);
    }

    return spawns;
  }

  public static void shareBadSpawnPoint(MapLocation location) throws GameActionException {
    MapLocation[] spawns = getEnemyBadSpawnPoints();

    // location already in spans?
    for (MapLocation spawn : spawns) {
      if (spawn != null && spawn.equals(location)) {
        return;
      }
    }

    if (spawns[0] == null) {
      writeMapLocationToIndex(location, ENEMY_SPAWN_IGNORE_INDEX_1);
      enemyBadSpawnPoints[0] = location;
    } else if (spawns[1] == null) {
      writeMapLocationToIndex(location, ENEMY_SPAWN_IGNORE_INDEX_2);
      enemyBadSpawnPoints[1] = location;
    } else if (spawns[2] == null) {
      writeMapLocationToIndex(location, ENEMY_SPAWN_IGNORE_INDEX_3);
      enemyBadSpawnPoints[2] = location;
    }
  }

  public static int[] getBestSupportingPosition(MapLocation target) throws GameActionException {
    //
    int i = rng.nextInt(Util.supportingPositionDxDy.length);
    int[] dx_dy = Util.supportingPositionDxDy[i];
    // MapLocation closestBase = getClosestML(getFlagLocations());
    // int[] boost = { 0, 0 };
    // if
    return dx_dy;
  }

  // only called when they're not null
  public static int newSpawnDefender(MapLocation loc) throws GameActionException {
    MapLocation[] spawns = getFlagLocations();
    // which spawn am I closest to?
    int closest = 0;
    int distance = 999999;
    for (int i = 0; i < spawns.length; i++) {
      MapLocation spawn = spawns[i];
      if (spawn != null) {
        int d = myLocation.distanceSquaredTo(spawn);
        if (d < distance) {
          distance = d;
          closest = i;
        }
      }
    }

    // how many already definding that?
    int[] indexs = { SPAWN_DEFENDER_INDEX_1, SPAWN_DEFENDER_INDEX_2, SPAWN_DEFENDER_INDEX_3 };
    int currentCount = rc.readSharedArray(indexs[closest]);

    if (currentCount < DEFENDERS_PER_SPAWN) {
      // increase count
      rc.writeSharedArray(indexs[closest], currentCount + 1);
      return closest;
    }
    return -1; // 0 = none
  }

  public static void reportStolenFlag(MapLocation f) throws GameActionException {
    MapLocation[] flags = getFlagLocations();
    int[] indexs = { FLAG_1_PRESENT_INDEX, FLAG_2_PRESENT_INDEX, FLAG_3_PRESENT_INDEX };

    for (int i = 0; i < flags.length; i++) {
      MapLocation flag = flags[i];
      if (flag != null && flag.equals(f)) {
        missingFlagLocation = f;
        rc.writeSharedArray(indexs[i], 1);
      }
    }

  }

  public static MapLocation getNextDefendLocation() throws GameActionException {
    MapLocation[] flags = getFlagLocations();
    int[] indexs = { FLAG_1_PRESENT_INDEX, FLAG_2_PRESENT_INDEX, FLAG_3_PRESENT_INDEX };

    for (int i = 0; i < flags.length; i++) {
      MapLocation flag = flags[i];
      if (flag != null && rc.readSharedArray(indexs[i]) == 1 && !flag.equals(missingFlagLocation)) {
        return flag;
      }
    }

    return null;
  }

}
