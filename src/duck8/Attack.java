package duck8;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import battlecode.common.*;

public class Attack extends RobotPlayer {

  private enum FIGHTS {
    ONE_V_ONE, ONE_V_MANY, MANY_V_MANY
  }

  private AttackState attackState;
  static RobotInfo[] allEnemies;
  static RobotInfo[] closeEnemies;
  static RobotInfo[] closeAllies;
  static RobotInfo[] allAllies;
  static RobotInfo[] enemiesForNextAttack;

  private static FIGHTS fightSituation;
  static boolean willAttack = false;
  static String s = "";

  static int turnsAttacking = 0;
  static final int ATTACK_RADIUS_SQUARED_PLUS_ONE = 10;
  static boolean adjacentToFlag = false;

  /**
   * Attack, moving and healing logic when >1 enemy visble
   * 
   * @return boolean array of [actionTaken, move]
   */
  static public void attack() throws GameActionException {
    // make decisions about what I should do, when I have sensed enemies.
    s = "A-RDY? " + rc.isActionReady() + ", MRDY? " + rc.isMovementReady() + ", ";
    // Sense nearby enemies
    sense();

    // -- ATTACK
    // if I can attack, and there are enemies in range, then attack

    if (closeEnemies.length > 0) {
      // I'm ready to attack
      if (rc.isActionReady()) {
        s += "attacking weakest, ";
        attackWeakestEnemy();
      }
      // Now step backwards to wait for cooldown
      if (rc.isMovementReady() && !adjacentToFlag) {
        if (rc.getHealth() < lowHealthRetreatNum || isHealing) {
          s += "Retreating low health, ";
          isHealing = true;
          myState = States.HEALING;
          setState(States.HEALING);
          moveToAlliesOrSpawn();
        } else if (allAllies.length > Math.max(allEnemies.length - 2, 0) && rc.getHealth() > 800) {
          s += "Doubling down we outnumber them. ";
          setState(States.ATTACKING);
          moveToWeakestEnemy();
        } else if (closeEnemies.length == 1 && rc.getActionCooldownTurns() < (2 * GameConstants.COOLDOWN_LIMIT)) {
          // if can't attack, wait till I can
          s += "waiting to attack, ";
          setState(States.ATTACKING);
        } else {
          // step back
          setState(States.STEPPING_BACK);
          moveBackwards();
        }
      }
    } else if ((rc.getHealth() < lowHealthRetreatNum || isHealing) && !adjacentToFlag) {
      // retreat until my health is higher
      s += "Retreating low health, healing if I can, ";
      isHealing = true;
      setState(States.HEALING);
      moveToAlliesOrSpawn();
      // heal friends
      healMyMate();

      if (rc.getHealth() > 800) {
        isHealing = false;
        setState(States.ATTACKING);
      }
    } else {
      // Close enemies but can't attack yet. Stay still if I can attack next turn.
      // but if there's too many enemies close I should stay in range, but only for 1
      // enemy
      if (isDefender && enemiesForNextAttack.length <= 2) {
        // don't attack, do my job.
        checkAndBuildDefensiveTraps();
      } else if (enemiesForNextAttack.length == 1 || enemiesForNextAttack.length == 2) {
        // ready to attack?
        if (rc.isActionReady()
            && rc.isMovementReady()) {
          s += "moving to fewestInRange then attacking, ";
          Debug.log("trying to move to fewest enemies in range then attack.");
          moveToFewestEnemiesInRange();
          Debug.log("should have moved now. Trying to attack.");
          sense();
          attackWeakestEnemy();
          setState(States.ATTACKING);
        } else if (rc.getActionCooldownTurns() <= (2 * GameConstants.COOLDOWN_LIMIT)) {
          s += "Waiting to attack next turn. ";
          setState(States.WAITING_TO_ATTACK);
        } else if (rc.isMovementReady()) {
          // can't attack for multiple turns but can move
          s += "action cooldown not ready for next attack, go backwards. ";
          moveBackwards();
          setState(States.STEPPING_BACK);
        }
      } else if (enemiesForNextAttack.length > 2 && allAllies.length < 3) {
        // too many robots to be waiting around for.
        s += "Too many enemies to wait for next attack. ";
        setState(States.RETREATING);
        moveToAllies();
      } else if (allEnemies.length > 0) {
        // it's a good idea to move closer to attack
        // find a position that complements my allies
        RobotInfo[] alliesAttacking = rc.senseNearbyRobots(allEnemies[0].getLocation(),
            GameConstants.ATTACK_RADIUS_SQUARED,
            rc.getTeam());
        // no close enemies and no near-next enemies
        // if out-numbered, move back or to allies
        if (allEnemies.length > Math.max(allAllies.length - 2, 1)) {
          s += "Outnumbered joining allies, ";
          moveToAllies();
          setState(States.RETREATING);
        } else if (alliesAttacking.length > 0) {
          s += "Supporting attacking allies " + pl(alliesAttacking[0].getLocation()) + ", ";
          Navigation.move(alliesAttacking[0].getLocation());
          setState(States.MOVING_TO_ATTACK);
        } else {
          // we have superiority
          // or no allies, so move towards the closest enemy
          s += "moving closer to weakest enemy, ";
          setState(States.MOVING_TO_ATTACK);
          moveToWeakestEnemy();
          // TODO: STILL VALIDATING THIS!!!
        }

      }
    }
    // Debug.log("Attack: " + s);
  }

  private static void moveToWeakestEnemy() throws GameActionException {
    RobotInfo weakestEnemy = getLowHealthRI(allEnemies);
    if (weakestEnemy != null) {
      s += ", moved to " + pl(weakestEnemy.getLocation());
      Navigation.move(weakestEnemy.getLocation());
    }
  }

  /** For robots who need to find reinforcement */
  private static void moveToAllies() throws GameActionException {
    MapLocation move = lookForMove(MoveTypes.TO_ALLIES);
    if (move != null) {
      Navigation.move(move);
      s += "moving to allies " + pl(move) + ", ";
    } else {
      // can't move to allies
      goToClosestAllySpawn();
    }
  }

  private static void attackWeakestEnemy() throws GameActionException {
    RobotInfo target = getLowHealthOrFlagEnemy(
        rc.senseNearbyRobots(GameConstants.ATTACK_RADIUS_SQUARED, rc.getTeam().opponent()));
    if (target == null) {
      s += "tried to attack weakest enemy but target was null. ";
    } else if (rc.canAttack(target.getLocation())) {
      rc.attack(target.getLocation());
      s += "Attacking " + pl(target.getLocation()) + ", ";
    } else {
      s += "Can't attack. ";
    }

  }

  private static RobotInfo getLowHealthOrFlagEnemy(RobotInfo[] senseNearbyRobots) {
    RobotInfo[] flagStealers = Arrays.stream(senseNearbyRobots).filter(x -> x.hasFlag())
        .toArray(RobotInfo[]::new);
    if (flagStealers.length > 0) {
      return flagStealers[0];
    }
    return getLowHealthRI(senseNearbyRobots);
  }

  private static void moveToFewestEnemiesInRange() throws GameActionException {
    MapLocation moveInRange = lookForMove(MoveTypes.FEWEST_ENEMY_TARGETS);
    if (moveInRange != null) {
      Navigation.move(moveInRange);
      s += "moving in range " + pl(moveInRange) + ", ";
    } else {
      s += "no move into attack range, just going closer. ";
      moveToWeakestEnemy();
    }
  }

  /**
   * Move backwards out of enemy range
   * 
   * @return
   * @throws GameActionException
   */
  private static void moveBackwards() throws GameActionException {
    MapLocation stepBack = lookForMove(MoveTypes.OUT_OF_RANGE);
    if (stepBack != null) {
      Navigation.move(stepBack);
      s += "Stepping back " + pl(stepBack);
    } else {
      s += "moving to closest spawn. ";
      goToClosestAllySpawn();
    }
  }

  /** Prioritises allies away from enemies for low health bots */
  private static void moveToAlliesOrSpawn() throws GameActionException {
    if (enemiesForNextAttack.length > 0) {
      MapLocation move = lookForMove(MoveTypes.FURTHEST_FROM_ENEMIES);
      if (move != null) {
        Navigation.move(move);
        s += "moving away from enemy " + pl(move);
      }
    } else if (allEnemies.length > 0) {
      goToClosestAllySpawn();
    } else {
      goToClosestAllyOrSpawn();
    }
  }

  enum MoveTypes {
    OUT_OF_RANGE, TO_ALLIES, FEWEST_ENEMY_TARGETS, NEXT_ATTACK_TURN, FURTHEST_FROM_ENEMIES
  }

  private static HashMap<MoveTypes, MapLocation> moveOptions = new HashMap<MoveTypes, MapLocation>();
  private static int bestOutOfRange;

  private static MapLocation lookForMove(MoveTypes mt) throws GameActionException {
    MapLocation moveOption = null;
    int maxEIAR = 0;
    int minEIAR = Integer.MAX_VALUE;
    int leastEnemies = Integer.MAX_VALUE;
    int eINAR = Integer.MAX_VALUE;
    int aIR = 0;
    int distToEnemies = 0;
    int distToAllies = Integer.MAX_VALUE;

    for (Direction d : directions) {
      MapLocation loc = rc.getLocation().add(d);
      if (rc.canMove(d)) {
        int enemiesInAttackRange = rc.senseNearbyRobots(loc, GameConstants.ATTACK_RADIUS_SQUARED,
            rc.getTeam().opponent()).length;

        switch (mt) {
          case FEWEST_ENEMY_TARGETS:
            if (enemiesInAttackRange > 0 && enemiesInAttackRange < minEIAR) {
              minEIAR = enemiesInAttackRange;
              moveOption = loc;
            }
            break;
          case OUT_OF_RANGE:
            if (enemiesInAttackRange < leastEnemies) {
              leastEnemies = enemiesInAttackRange;
              moveOption = loc;
            }
            break;
          case NEXT_ATTACK_TURN:
            if (enemiesInAttackRange == 0) {
              int enemiesInNextAttackRange = rc.senseNearbyRobots(loc, ATTACK_RADIUS_SQUARED_PLUS_ONE,
                  rc.getTeam().opponent()).length;
              if (enemiesInNextAttackRange > 1 && enemiesInNextAttackRange < eINAR) {
                eINAR = enemiesInNextAttackRange;
                moveOption = loc;
              }
            }
            break;
          case TO_ALLIES:
            int alliesInRange = rc.senseNearbyRobots(loc, ATTACK_RADIUS_SQUARED_PLUS_ONE, rc.getTeam()).length;
            if (alliesInRange > aIR) {
              aIR = alliesInRange;
              moveOption = loc;
            }
            break;
          case FURTHEST_FROM_ENEMIES:
            int dist = 0;
            for (RobotInfo enemy : (enemiesForNextAttack.length > 5 ? closeEnemies : enemiesForNextAttack)) {
              dist += loc.distanceSquaredTo(enemy.location);
            }
            if (dist > distToEnemies) {
              distToEnemies = dist;
              moveOption = loc;
            }
            // s += "loc: " + pl(loc) + " dist: " + dist + ", ";
            break;
        }
      }
    }
    return moveOption;
  }

  /**
   * Attack, moving and healing logic when >1 enemy visble
   * 
   * @return boolean array of [actionTaken, move]
   */
  static public boolean[] executeAttackLogic() throws GameActionException {
    boolean[] bools = new boolean[2];
    boolean attackFirst = false;
    // reset
    willAttack = false;
    // Sense nearby enemies
    sense();

    // what about switching the order?
    if (rc.isActionReady() && closeEnemies.length > 0) {
      // TODO: Also check that I can actually attack a person by moving into their
      // range
      attackFirst = true;
    }
    // use the order to determine whether I should only look in ragne too
    MapLocation attackTarget = selectAttackTarget();
    MapLocation moveTarget = selectMoveTarget();
    MapLocation shouldIHealSomeone = shouldIHealSomeone();

    // this has out of range error sometimes, if I move first. Need to figure that
    // out first! but
    // it's also so tied together.
    s = "";
    if (attackTarget == null && shouldIHealSomeone != null) {
      bools[0] = true;
      myState = States.HEALING;
      rc.heal(shouldIHealSomeone);
      s += "Healing " + pl(shouldIHealSomeone) + ". ";
    }

    if (attackFirst) {
      s += "Attacking first. ";
      if (attackTarget != null && rc.canAttack(attackTarget)) {
        bools[0] = true;
        myState = States.ATTACKING;
        rc.attack(attackTarget);
        s += "Attacking " + pl(attackTarget);
      }

      if (moveTarget != null) {
        bools[1] = true;
        Navigation.move(moveTarget);
        s += ", Moving  " + pl(moveTarget);
      }
    } else {
      s += "Moving first. ";
      if (moveTarget != null) {
        bools[1] = true;
        Navigation.move(moveTarget);
        s += "Moving " + pl(moveTarget);
      }

      if (attackTarget != null && rc.canAttack(attackTarget)) {
        bools[0] = true;
        myState = States.ATTACKING;
        rc.attack(attackTarget);
        s += ", Attacking " + pl(attackTarget);
      }
    }
    Debug.log(s);

    sense();
    return bools;
  }

  static private MapLocation shouldIHealSomeone() {
    if (!rc.isActionReady()) {
      return null;
    }

    RobotInfo closestEnemy = getClosestRI(allEnemies);
    boolean okToHeal = false;
    if (closestEnemy != null) {
      // is it far away enough to let me heal and then attack after?
      if ((closestEnemy.location.distanceSquaredTo(rc.getLocation()) - GameConstants.ATTACK_RADIUS_SQUARED) > 5) {
        okToHeal = true;
        s += ("Ok to heal. Enemy Dist: " + closestEnemy.location.distanceSquaredTo(rc.getLocation()) + ", "
            + pl(closestEnemy.location) + ", ");
      }
    } else if (allEnemies.length == 0) {
      okToHeal = true;
    }

    if (okToHeal) {
      RobotInfo[] injuredAllies = Arrays.stream(closeAllies).filter(x -> x.health < GameConstants.DEFAULT_HEALTH)
          .toArray(RobotInfo[]::new);
      if (injuredAllies.length > 0) {
        RobotInfo closestInjured = getClosestRI(injuredAllies);
        if (rc.canHeal(closestInjured.location)) {
          s += ("Trying to heal " + pl(closestInjured.location) + ", ");
          return closestInjured.location;
        }
      }
    }

    return null;
  }

  static private MapLocation selectMoveTarget() throws GameActionException {
    if (rc.getMovementCooldownTurns() > 10) {
      return null;
    }
    Debug.checkBytecodeUsageStart("moveTargetinit");
    HashMap<String, MapLocation> movementLocations = new HashMap<String, MapLocation>();
    int[] locScores = new int[] { 99999, 0, 99999, 0, 99999, 0, 0 };
    Debug.checkBytecodeUsageStop("moveTargetinit");
    // 0 - dirClosestToEnemies
    // 1 - dirFurthestFromEnemies
    // 2 - dirClosestToAllies
    // 3 - dirFurthestFromAllies
    // 4 - fewestEnemiesInAttackRange (at least 1)
    // 5 - mostAlliesInRange
    // 6 - targetIsolation

    Debug.checkBytecodeUsageStart("moveTargetLoop");
    // USE THESE!!!
    for (Direction d : directions) {
      MapLocation loc = rc.getLocation().add(d);
      if (rc.canMove(d)) {
        // calculate the things
        int distToEnemies = 0;
        int distToAllies = 0;
        int alliesInRange = 0;
        int[] distancesToEnemies = new int[allEnemies.length];

        for (int i = 0; i < allEnemies.length; i++) {
          int dis = loc.distanceSquaredTo(allEnemies[i].location);
          distToEnemies += dis;
          distancesToEnemies[i] = dis;
        }

        int eInRange = rc.senseNearbyRobots(loc, GameConstants.ATTACK_RADIUS_SQUARED,
            rc.getTeam().opponent()).length;
        if (eInRange > 0 && eInRange < locScores[4]) {
          movementLocations.put("fewestEnemiesInAttackRange", loc);
          locScores[4] = eInRange;
        }
        Debug.log("eInRange: " + eInRange + ", locScores[4]: " + locScores[4]);

        for (RobotInfo ally : allAllies) {
          distToAllies += loc.distanceSquaredTo(ally.location);
          alliesInRange += rc.senseNearbyRobots(loc, GameConstants.HEAL_RADIUS_SQUARED, rc.getTeam()).length;
        }

        if (alliesInRange > locScores[5]) {
          movementLocations.put("mostAlliesInRange", loc);
          locScores[5] = alliesInRange;
        }

        if (distToEnemies < locScores[0]) {
          locScores[0] = distToEnemies;
          movementLocations.put("dirClosestToEnemies", loc);
        }
        if (distToEnemies > locScores[1]) {
          locScores[1] = distToEnemies;
          movementLocations.put("dirFurthestFromEnemies", loc);
        }
        if (distToAllies < locScores[2]) {
          locScores[2] = distToAllies;
          movementLocations.put("dirClosestToAllies", loc);
        }
        if (distToAllies > locScores[3]) {
          locScores[3] = distToAllies;
          movementLocations.put("dirFurthestFromAllies", loc);
        }

        // targetIsolation
        // Calculate the distribution
        int distribution = calculateDistribution(distancesToEnemies);

        // Update the best direction based on distribution
        if (distribution > locScores[6]) {
          locScores[6] = distribution;
          movementLocations.put("targetIsolation", loc);
        }
        Debug.log("For Loc" + loc + ", distribution: " + distribution);
      }
    }
    Debug.checkBytecodeUsageStop("moveTargetLoop");

    // print all the movementLocations keys and value pairs to Debug.log()
    String s = "";
    for (String key : movementLocations.keySet()) {
      s += key + ": " + pl(movementLocations.get(key)) + ", ";
    }
    Debug.log("Movement Locations: " + s);

    // do I just go out of attack range, or do I fully retreat?
    RobotInfo[] flagStealersFar = Arrays.stream(allEnemies).filter(x -> x.hasFlag() &&
        (x.location.distanceSquaredTo(rc.getLocation()) > GameConstants.ATTACK_RADIUS_SQUARED))
        .toArray(RobotInfo[]::new);

    // check health first
    if (rc.getHealth() < lowHealthRetreatNum) {
      // retreat until my health is higher
      Debug.log("Retreating because my health is low");
      isHealing = true;
      myState = States.HEALING;
      return movementLocations.get("dirfurthestFromEnemies");
    }

    // outnumbered?
    if (closeEnemies.length > closeAllies.length || (closeEnemies.length > 1 && allAllies.length == 0)) {
      // retreat until I'm not outnumbered
      Debug.log("Retreating because I'm outnumbered");
      resignWhen = roundNum + 25;
      myState = States.RETREATING;
      return movementLocations.get("dirClosestToAllies");
    }

    // close enemies and I will attack
    if (closeEnemies.length > 0 && willAttack) {
      RobotInfo closest = getClosestRI(closeEnemies);
      // friends?
      if (closeAllies.length > 4 && closeEnemies.length < 3) {
        // double down - move in.
        Debug.log("Doubling down, moving in.");
        myState = States.MOVING_TO_ATTACK;
        return closest.getLocation();
      }

      // assume I attacked, so step back.
      // preferably go near my allies
      myState = States.STEPPING_BACK;
      if (allAllies.length > 0) {
        Debug.log("Stepping back to allies");
        return movementLocations.get("dirClosestToAllies");
      }

      // just a normal withdrawl
      // Direction dir = closest.getLocation().directionTo(rc.getLocation());
      Debug.log("Stepping back away from enemy");
      return movementLocations.get("dirfurthestFromEnemies");
    }

    // if I haven't attacked and no close enemies, then move towards an enemy
    if (closeEnemies.length == 0 && allEnemies.length > 0 && !willAttack) {
      if (rc.getActionCooldownTurns() <= 20) {
        myState = States.MOVING_TO_ATTACK;
        // Go to the stealer first
        if (flagStealersFar.length > 0) {
          return flagStealersFar[0].location;
        }

        Debug.log("Moving to enemy to acck. my cooldown: " + rc.getActionCooldownTurns());
        // calculate distance and compare to cooldown time

        // move towards closest enemy
        // ? Could more carefully go next to ally

        // RobotInfo closest = getClosestRI(allEnemies);
        // Direction dir = rc.getLocation().directionTo(closest.getLocation());
        MapLocation fewestInRange = movementLocations.get("fewestEnemiesInAttackRange");
        MapLocation targetIsolation = movementLocations.get("targetIsolation");
        if (fewestInRange != null) {
          Debug.log("Moving to fewestEnemiesInAttackRange");
          return fewestInRange;
        } else if (targetIsolation != null) {
          Debug.log("Moving to targetIsolation");
          return targetIsolation;
        } else {
          Debug.log("Moving to nearest enemy");
          RobotInfo closest = getClosestRI(allEnemies);
          Direction dir = rc.getLocation().directionTo(closest.getLocation());
          return rc.getLocation().add(dir);
        }
      } else {
        // won't be ready to attack yet
        s += "Moving away from enemy, waiting for cooldown. ";
        return movementLocations.get("dirfurthestFromEnemies");
      }

    }
    return null;
  }

  /*
   * Calculate the distribution of distances to enemies
   */
  private static int calculateDistribution(int[] distances) {
    // Sort the array to easily check for a consistent increase or decrease
    Arrays.sort(distances);

    // Calculate the difference between consecutive distances
    int diff = 0;
    // 12 if statements is faster than a for loop

    if (distances.length > 1) {
      diff += distances[1] - distances[0];
    }
    if (distances.length > 2) {
      diff += distances[2] - distances[1];
    }
    if (distances.length > 3) {
      diff += distances[3] - distances[2];
    }
    if (distances.length > 4) {
      diff += distances[4] - distances[3];
    }
    if (distances.length > 5) {
      diff += distances[5] - distances[4];
    }
    if (distances.length > 6) {
      diff += distances[6] - distances[5];
    }
    if (distances.length > 7) {
      diff += distances[7] - distances[6];
    }
    if (distances.length > 8) {
      diff += distances[8] - distances[7];
    }
    if (distances.length > 9) {
      diff += distances[9] - distances[8];
    }
    if (distances.length > 10) {
      diff += distances[10] - distances[9];
    }
    if (distances.length > 11) {
      diff += distances[11] - distances[10];
    }

    // A more distributed set will result in a higher total difference
    return diff;
  }

  // Get the robot that is furthest away from location
  private static RobotInfo getFurthestRI(RobotInfo[] bots, MapLocation location) {
    RobotInfo furthest = null;
    int furthestDistance = 0;
    for (RobotInfo bot : bots) {
      int distance = bot.location.distanceSquaredTo(location);
      if (distance > furthestDistance) {
        furthestDistance = distance;
        furthest = bot;
      }
    }
    return furthest;
  }

  static private MapLocation selectAttackTarget() {
    if (rc.getActionCooldownTurns() > GameConstants.ATTACK_COOLDOWN) {
      return null;
    }

    if (closeEnemies.length > 0) {
      willAttack = true;
      // attack flag stealers
      RobotInfo[] flagStealers = Arrays.stream(closeEnemies).filter(x -> x.hasFlag())
          .toArray(RobotInfo[]::new);
      if (flagStealers.length > 0) {
        return flagStealers[0].location;
      }

      // attack weakest close enemy
      RobotInfo weakestEnemy = getLowHealthRI(closeEnemies);
      if (weakestEnemy == null) {
        String s = "";
        for (RobotInfo enemy : closeEnemies) {
          s += enemy.location + ", ";
        }
        Debug.log(
            "Weakest enemy null even though there are closeEnemes. my loc: " + rc.getLocation() + ", close enemies: "
                + s);
      }
      return weakestEnemy.getLocation();
    }
    return null;
  }

  static int lastRun = 0;
  static ArrayList<Integer> enemiesClose = new ArrayList<Integer>();

  private static void sense() throws GameActionException {
    // Debug.checkBytecodeUsageStart("sense");
    if (lastRun == roundNum - 1) {
      enemiesClose.clear();
    }

    allEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
    closeEnemies = rc.senseNearbyRobots(GameConstants.ATTACK_RADIUS_SQUARED, rc.getTeam().opponent());
    closeAllies = rc.senseNearbyRobots(GameConstants.ATTACK_RADIUS_SQUARED, rc.getTeam());
    allAllies = rc.senseNearbyRobots(-1, rc.getTeam());
    enemiesForNextAttack = rc.senseNearbyRobots(ATTACK_RADIUS_SQUARED_PLUS_ONE, rc.getTeam().opponent());
    adjacentToFlag = rc.senseNearbyFlags(GameConstants.INTERACT_RADIUS_SQUARED, rc.getTeam()).length > 0;

    if (closeEnemies.length == 1) {
      fightSituation = FIGHTS.ONE_V_ONE;
    } else if (closeEnemies.length > 1 && closeAllies.length == 0) {
      fightSituation = FIGHTS.ONE_V_MANY;
    } else if (closeEnemies.length > 1 && closeAllies.length > 1) {
      fightSituation = FIGHTS.MANY_V_MANY;
    }

    String e = "";
    for (RobotInfo enemy : allEnemies) {
      int dist = enemy.getLocation().distanceSquaredTo(rc.getLocation());
      boolean inRange = dist <= GameConstants.ATTACK_RADIUS_SQUARED;
      boolean newClose = true;
      if (inRange) {
        if (enemiesClose.contains(enemy.getID())) {
          // already in the list
          newClose = false;
        }
        enemiesClose.add(enemy.getID());
      }
      if (dist <= GameConstants.ATTACK_RADIUS_SQUARED) {
        // attacking distance
        if (newClose) {
          rc.setIndicatorDot(enemy.getLocation(), 71, 247, 36); // green
        } else {
          rc.setIndicatorDot(enemy.getLocation(), 253, 255, 70); // yellow
        }
      } else {
        rc.setIndicatorDot(enemy.getLocation(), 199, 0, 57); // red
      }
      e += pl(enemy.getLocation()) + (inRange ? "*" : "") + (newClose ? "N" : "") + "-dsq:" + dist + "-d-"
          + Util.distance(rc.getLocation(), enemy.getLocation()) + ", ";
    }
    Debug.log("Enemies (" + closeEnemies.length + "/" + allEnemies.length + "): " + e);

    lastRun = roundNum;
    // Debug.checkBytecodeUsageStop("sense");
  }

  // Enum to represent different attack states
  private enum AttackState {
    ATTACK,
    RETREAT,
    MOVETOHEAL,
    // Add more states as needed
    IDLE // Default state
  }
}
