package duck7;

import static duck7.RobotPlayer.getLowHealthRI;

import java.util.Arrays;

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

  private static FIGHTS fightSituation;
  static boolean willAttack = false;

  /**
   * Attack, moving and healing logic when >1 enemy visble
   * 
   * @return boolean array of [actionTaken, move]
   */
  static public boolean[] executeAttackLogic() throws GameActionException {
    boolean[] bools = new boolean[3];
    boolean attackFirst = false;
    // reset
    willAttack = false;
    // Sense nearby enemies
    sense();

    // what about switching the order?
    if (rc.getActionCooldownTurns() <= GameConstants.COOLDOWN_LIMIT && closeEnemies.length > 0) {
      // TODO: Also check that I can actually attack a person by moving into their
      // range
      attackFirst = true;
    }
    // use the order to determine whether I should only look in ragne too
    MapLocation attackTarget = selectAttackTarget();
    MapLocation moveTarget = selectMoveTarget();
    MapLocation shouldIHealSomeone = shouldIHealSomeone();
    
    // this has out of range error sometimes, if I move first. Need to figure that out first! but
    // it's also so tied together.
    if (attackTarget == null && shouldIHealSomeone != null) {
      bools[0] = true;
      myState = States.HEALING;
      rc.heal(shouldIHealSomeone);
    }

    String s = "";
    if (attackFirst) {
      s += "Attacking first. ";
      if (attackTarget != null && rc.canAttack(attackTarget)) {
        bools[0] = true;
        myState = States.ATTACKING;
        rc.attack(attackTarget);
        s += "Attacking " + attackTarget.toString();
      }

      if (moveTarget != null) {
        bools[1] = true;
        Navigation.move(moveTarget);
        s += ", Moving to " + moveTarget.toString();
      }
    } else {
      s += "Moving first. ";
      if (moveTarget != null) {
        bools[1] = true;
        Navigation.move(moveTarget);
        s += "Attacking " + moveTarget.toString();
      }

      if (attackTarget != null && rc.canAttack(attackTarget)) {
        bools[0] = true;
        myState = States.ATTACKING;
        rc.attack(attackTarget);
        s += ", Moving to " + attackTarget.toString();
      }
    }
    Debug.log(s);


    return bools;
  }

  static private MapLocation shouldIHealSomeone() {
    if (rc.getActionCooldownTurns() >= GameConstants.COOLDOWN_LIMIT) {
      // Prefer to attack
      return null;
    }

    RobotInfo closestEnemy = getClosestRI(allEnemies);
    boolean okToHeal = false;
    if (closestEnemy != null) {
      // is it far away enough to let me heal and then attack after?
      if ((closestEnemy.location.distanceSquaredTo(myLocation) - GameConstants.ATTACK_RADIUS_SQUARED) > Math.pow(3,
          2)) {
        okToHeal = true;
        Debug.log(
            "Ok to heal. Enemy Dist: " + closestEnemy.location.distanceSquaredTo(myLocation) + ", "
                + closestEnemy.location);
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
          Debug.log("Trying to heal " + closestInjured.location);
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

    MapLocation[] locs = new MapLocation[] { myLocation, myLocation, myLocation, myLocation };
    int[] locScores = new int[] { 99999, 0, 99999, 0 };
    // 0 - dirClosestToEnemies
    // 1 - dirFurthestFromEnemies
    // 2 - dirClosestToAllies
    // 3 - dirFurthestFromAllies

    for (Direction d : directions) {
      MapLocation loc = myLocation.add(d);
      if (rc.canMove(d)) {
        // calculate the things
        int distToEnemies = 0;
        int distToAllies = 0;
        for (RobotInfo enemy : allEnemies) {
          distToEnemies += Util.distance(loc, enemy.location);
        }
        for (RobotInfo ally : allAllies) {
          distToAllies += Util.distance(loc, ally.location);
        }

        if (distToEnemies < locScores[0]) {
          locScores[0] = distToEnemies;
          locs[0] = loc;
        }
        if (distToEnemies > locScores[1]) {
          locScores[1] = distToEnemies;
          locs[1] = loc;
        }
        if (distToAllies < locScores[2]) {
          locScores[2] = distToAllies;
          locs[2] = loc;
        }
        if (distToAllies > locScores[3]) {
          locScores[3] = distToAllies;
          locs[3] = loc;
        }
      }
    }

    // do I just go out of attack range, or do I fully retreat?
    RobotInfo[] flagStealersFar = Arrays.stream(allEnemies).filter(x -> x.hasFlag() &&
        (x.location.distanceSquaredTo(myLocation) > GameConstants.ATTACK_RADIUS_SQUARED))
        .toArray(RobotInfo[]::new);

    // check health first
    if (rc.getHealth() < lowHealthRetreatNum) {
      // retreat until my health is higher
      Debug.log("Retreating because my health is low");
      isHealing = true;
      return locs[1];
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
        // RobotInfo furthestAlly = getFurthestRI(allAllies, myLocation);
        Debug.log("Stepping back to allies");
        return locs[2];
      }

      // just a normal withdrawl
      // Direction dir = closest.getLocation().directionTo(rc.getLocation());
      Debug.log("Stepping back away from enemy");
      return locs[1];
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
        RobotInfo closest = getClosestRI(allEnemies);
        Direction dir = myLocation.directionTo(closest.getLocation());
        return myLocation.add(dir);
      } else {
        // won't be ready to attack yet
        return locs[1];
      }

    }
    return null;
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
        Debug.log("Weakest enemy null even though there are closeEnemes. my loc: " + myLocation + ", close enemies: "
            + s);
      }
      return weakestEnemy.getLocation();
    }
    return null;
  }

  private static void sense() throws GameActionException {
    allEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
    closeEnemies = rc.senseNearbyRobots(GameConstants.ATTACK_RADIUS_SQUARED, rc.getTeam().opponent());
    closeAllies = rc.senseNearbyRobots(GameConstants.ATTACK_RADIUS_SQUARED, rc.getTeam());
    allAllies = rc.senseNearbyRobots(-1, rc.getTeam());

    if (closeEnemies.length == 1) {
      fightSituation = FIGHTS.ONE_V_ONE;
    } else if (closeEnemies.length > 1 && closeAllies.length == 0) {
      fightSituation = FIGHTS.ONE_V_MANY;
    } else if (closeEnemies.length > 1 && closeAllies.length > 1) {
      fightSituation = FIGHTS.MANY_V_MANY;
    }
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
