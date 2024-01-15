package duck6;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what
 * we'll call once your robot
 * is created!
 */
@SuppressWarnings("unused")
public strictfp class RobotPlayer {

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided
     * by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant
     * number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very
     * useful for debugging!
     */
    static Random rng;
    /**
     * Array containing all the possible movement directions.
     */
    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };
    /**
     * We will use this variable to count the number of turns this robot has been
     * alive.
     * You can use static variables like this to save any information you want. Keep
     * in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between
     * your robots.
     */
    static int turnCount = 0;
    static int width;
    static int height;
    static int id;
    static MapLocation myLocation;
    static RobotController rc;

    static MapLocation myFlagDestination = null;
    static MapLocation targetEnemyLocation = null;
    static MapInfo lastCrumb = null;
    static int turnsBeenDead;
    static int[] goToDxDy = null;

    static int roundNum = 0;

    static enum States {
        STARTING,
        MOVING_OWN_FLAG,
        GETTING_CRUMB,
        MOVING_TO_ENEMY_SPAWN,
        BUILDING_TRAP,
        WAITING_AT_BARRIER,
        ATTACKING,
        MOVING_TO_ATTACK, STEALING_FLAG, CHASING_FLAG,
        SUPPORTING_FLAG_CARRIER, DEFENDER, EXPLORING, HEALING, RETREATING, STEPPING_BACK
    }

    static States myState = States.STARTING;

    // defending
    static int DEFENDERS_PER_SPAWN = 3;
    static boolean isDefender = false;
    static MapLocation defendLocation = null;
    static ArrayList<MapLocation> myTrapLocations = null;

    // setup
    static int EXPLORE_UNTIL_ROUND = 130;

    // broadcasted flags
    static ArrayList<MapLocation[]> enemyFlagBroadcasts = new ArrayList<MapLocation[]>();

    /**
     * run() is the method that is called when a robot is instantiated in the
     * Battlecode world.
     * It is like the main function for your robot. If this method returns, the
     * robot dies!
     *
     * @param r The RobotController object. You use it to perform actions from this
     *          robot, and to get
     *          information on its current status. Essentially your portal to
     *          interacting with the world.
     **/

    public static void run(RobotController r) throws GameActionException {

        // Hello world! Standard output is very useful for debugging.
        // Everything you say here will be directly viewable in your terminal when you
        // run a match!

        // You can also use indicators to save debug notes in replays.
        id = r.getID();
        rc = r;
        width = rc.getMapWidth();
        height = rc.getMapHeight();
        rng = new Random(id);
        Navigation.init();

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in
            // an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At
            // the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to
            // do.

            turnCount += 1; // We have now been alive for one more turn!
            roundNum = rc.getRoundNum();

            // Try/catch blocks stop unhandled exceptions, which cause your robot to
            // explode.
            try {
                // Make sure you spawn your robot in before you attempt to take any actions!
                // Robots not spawned in do not have vision of any tiles and cannot perform any
                // actions.
                // Limit the number of robots we spawn
                if (!rc.isSpawned()) {
                    spawnMe();
                } else {
                    // ***** Start turn things *****
                    myLocation = rc.getLocation();

                    MapInfo[] locsNearMe = rc.senseNearbyMapInfos();
                    // filter locsnearme where crumbs > 0
                    MapInfo[] crumbs = Arrays.stream(locsNearMe).filter(x -> x.getCrumbs() > 0)
                            .toArray(MapInfo[]::new);

                    // ****** SETUP ******
                    if (roundNum <= GameConstants.SETUP_ROUNDS) {

                        switch (myState) {
                            case STARTING:
                                // flag: if we have one, move it to a good spot. One robot will always be on it.
                                if (rc.canPickupFlag(myLocation)) {
                                    Comm.saveOurFlagLocation(myLocation);
                                }

                                // DEFENDING - after all the flags have been identified?
                                checkAndSaveDefenderStatus();

                                // sense, crumb, then explore?
                                if (crumbs.length > 0 && myState != States.DEFENDER) {
                                    // move to crumbs
                                    myState = States.GETTING_CRUMB;
                                    lastCrumb = getClosestMI(crumbs);
                                    Navigation.move(lastCrumb.getMapLocation());
                                } else if (myState != States.DEFENDER) {
                                    // move to nearest enemy spawn
                                    // or search for crumbs?
                                    myState = States.MOVING_TO_ENEMY_SPAWN;
                                    targetEnemyLocation = Navigation.bestEnemyLocationGuess();
                                    Navigation.move(targetEnemyLocation);
                                }
                                break;
                            case GETTING_CRUMB:
                                // sense, crumb, then explore?
                                // crumbs
                                if (!lastCrumb.getMapLocation().equals(myLocation)
                                        && ((rc.canSenseLocation(lastCrumb.getMapLocation()) && crumbs.length > 0)
                                                || !rc.canSenseLocation(lastCrumb.getMapLocation()))) {
                                    Navigation.move(lastCrumb.getMapLocation());
                                } else if (crumbs.length > 0) {
                                    lastCrumb = getClosestMI(crumbs);
                                    Navigation.move(lastCrumb.getMapLocation());
                                } else {
                                    // move to nearest enemy spawn
                                    // or search for crumbs?
                                    myState = States.MOVING_TO_ENEMY_SPAWN;
                                    targetEnemyLocation = Navigation.bestEnemyLocationGuess();
                                    Navigation.move(targetEnemyLocation);
                                }
                                break;
                            case MOVING_TO_ENEMY_SPAWN:
                                if (crumbs.length > 0) {
                                    // move to crumbs
                                    myState = States.GETTING_CRUMB;
                                    lastCrumb = getClosestMI(crumbs);
                                    Navigation.move(lastCrumb.getMapLocation());
                                }

                                RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                                if (enemyRobots.length > 0) {
                                    // can I attack or just see?
                                    RobotInfo closestTarget = getClosestRI(enemyRobots);

                                    if (rc.canAttack(closestTarget.getLocation())) {
                                        rc.attack(closestTarget.getLocation());
                                    } else {
                                        Navigation.move(closestTarget.getLocation());
                                    }
                                } else {
                                    // dont see enemy?
                                    if (roundNum < EXPLORE_UNTIL_ROUND) {
                                        // explore
                                        myState = States.EXPLORING;
                                        Navigation.explore();
                                    } else {
                                        Navigation.move(Navigation.bestEnemyLocationGuess());
                                    }
                                }

                                break;
                            case EXPLORING:
                                if (crumbs.length > 0) {
                                    // move to crumbs
                                    myState = States.GETTING_CRUMB;
                                    lastCrumb = getClosestMI(crumbs);
                                    Navigation.move(lastCrumb.getMapLocation());
                                }
                                if (roundNum < EXPLORE_UNTIL_ROUND) {
                                    // explore
                                    myState = States.EXPLORING;
                                    Navigation.explore();
                                } else {
                                    myState = States.MOVING_TO_ENEMY_SPAWN;
                                    Navigation.move(Navigation.bestEnemyLocationGuess());
                                }
                                break;
                            case WAITING_AT_BARRIER:
                                // wait at barrier
                                // build trap, then go to enemy spawn when -1 from setup turn
                                MapInfo[] nearbyTraps = rc.senseNearbyMapInfos(1);
                                int shockTraps = 0;
                                int explosiveTraps = 0;
                                for (MapInfo cell : nearbyTraps) {
                                    if (cell.getTrapType() == TrapType.STUN) {
                                        shockTraps++;
                                    } else if (cell.getTrapType() == TrapType.EXPLOSIVE) {
                                        explosiveTraps++;
                                    }
                                }

                                TrapType trapToBuild = null;
                                if (shockTraps >= explosiveTraps) {
                                    trapToBuild = TrapType.EXPLOSIVE;
                                } else {
                                    trapToBuild = TrapType.STUN;
                                }

                                if (rc.canBuild(trapToBuild, myLocation)) {
                                    rc.build(trapToBuild, myLocation);
                                }
                                break;
                            case DEFENDER:
                                // Build traps
                                checkAndBuildDefensiveTraps();
                            default:
                                break;
                        }

                    }

                    // if (roundNum > 10) {
                    // rc.resign();
                    // }

                    // ****** POST-SETUP ******
                    if (roundNum > GameConstants.SETUP_ROUNDS) {
                        boolean haveMoved = false;
                        boolean takenAction = false;
                        // If we are holding an enemy flag, singularly focus on moving towards
                        // an ally spawn zone to capture it! We use the check roundNum >= SETUP_ROUNDS
                        // to make sure setup phase has ended.
                        if (rc.hasFlag()) {
                            MapLocation[] flagLocs = Comm.getFlagLocations();
                            MapLocation closestSpawn = getClosestML(flagLocs);
                            myState = States.STEALING_FLAG;
                            Navigation.move(closestSpawn);
                        } else {
                            if (crumbs.length > 0) {
                                // move to crumbs
                                myState = States.GETTING_CRUMB;
                                lastCrumb = crumbs[0];
                                for (MapInfo crumb : crumbs) {
                                    if (crumb.getMapLocation().distanceSquaredTo(myLocation) < lastCrumb
                                            .getMapLocation().distanceSquaredTo(myLocation)) {
                                        lastCrumb = crumb;
                                    }
                                }
                                Navigation.move(lastCrumb.getMapLocation());
                                haveMoved = true;
                            }
                            // LOOK FOR FLAG
                            FlagInfo[] flagInfos = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
                            if (flagInfos.length > 0 && !flagInfos[0].isPickedUp() && !haveMoved) {
                                // move to flag
                                if (rc.canPickupFlag(flagInfos[0].getLocation())) {
                                    rc.pickupFlag(flagInfos[0].getLocation());
                                    myState = States.STEALING_FLAG;
                                    takenAction = true;
                                    Comm.clearEnemyFlagLocation(flagInfos[0].getLocation());
                                } else {
                                    Comm.reportEnemyFlagLocation(flagInfos[0].getLocation());
                                    Navigation.move(flagInfos[0].getLocation());
                                    myState = States.CHASING_FLAG;
                                    haveMoved = true;
                                }
                            } else if (flagInfos.length > 0 && flagInfos[0].isPickedUp() && !haveMoved
                                    && flagInfos[0].getTeam() == rc.getTeam().opponent()) {
                                // priority support
                                // move to flag location
                                // random location around
                                if (goToDxDy == null) {
                                    goToDxDy = Comm.getBestSupportingPosition(flagInfos[0].getLocation());
                                }
                                myState = States.SUPPORTING_FLAG_CARRIER;
                                MapLocation loc = new MapLocation(flagInfos[0].getLocation().x + goToDxDy[0],
                                        flagInfos[0].getLocation().y + goToDxDy[1]);
                                Navigation.move(loc);
                                haveMoved = true;
                            }

                            // Priority flag - GO GET IT
                            MapLocation priorityFlag = Comm.getEnemyPriorityFlag();
                            if (priorityFlag != null && !isDefender) {

                                // if I can sense it and it doesn't exist, remove it
                                if (rc.canSenseLocation(priorityFlag) && flagInfos.length == 0) {
                                    Comm.clearEnemyFlagLocation(priorityFlag);
                                } else if (!haveMoved) {
                                    Navigation.move(priorityFlag);
                                    myState = States.CHASING_FLAG;
                                    haveMoved = true;
                                }
                            }

                            // LOOK FOR ENEMY
                            RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                            if (enemyRobots.length > 0) {
                                boolean[] bools = Attack.executeAttackLogic();
                                if (bools[0])
                                    takenAction = true;
                                if (bools[1])
                                    haveMoved = true;
                            } else if (!haveMoved) {
                                // dont see enemy?
                                if (isDefender) {
                                    checkAndBuildDefensiveTraps();
                                    haveMoved = true;
                                    myState = States.DEFENDER;
                                } else {
                                    targetEnemyLocation = Navigation.bestEnemyLocationGuess();
                                    Navigation.move(targetEnemyLocation);
                                    myState = States.MOVING_TO_ENEMY_SPAWN;
                                    haveMoved = true;
                                }
                            }

                            // Heal
                            // Maybe only heal here if I there's no enemies in view
                            if (!takenAction) {
                                RobotInfo[] allies = rc.senseNearbyRobots(GameConstants.HEAL_RADIUS_SQUARED,
                                        rc.getTeam());
                                if (allies.length > 0) {
                                    RobotInfo weakestAlly = getLowHealthRI(allies);

                                    if (rc.canHeal(weakestAlly.getLocation())) {
                                        rc.heal(weakestAlly.getLocation());
                                        takenAction = true;
                                    }
                                }
                            }

                            // Build a trap if I still haven't taken an action and
                            // there aren't any enemies visible
                            if (!takenAction && rc.getActionCooldownTurns() <= GameConstants.COOLDOWN_LIMIT
                                    && !isDefender && roundNum >= GameConstants.SETUP_ROUNDS) {
                                boolean buildTrap = rng.nextDouble() > 0.6;
                                if (rc.senseNearbyRobots(-1, rc.getTeam()).length > 1) {
                                    if (rc.canBuild(TrapType.STUN, myLocation)) {
                                        rc.build(TrapType.STUN, myLocation);
                                    } else if (rc.canBuild(TrapType.STUN, myLocation.add(Direction.NORTH))) {
                                        rc.build(TrapType.STUN, myLocation.add(Direction.NORTH));
                                    } else if (rc.canBuild(TrapType.STUN, myLocation.add(Direction.EAST))) {
                                        rc.build(TrapType.STUN, myLocation.add(Direction.EAST));
                                    } else if (rc.canBuild(TrapType.STUN, myLocation.add(Direction.WEST))) {
                                        rc.build(TrapType.STUN, myLocation.add(Direction.WEST));
                                    } else if (rc.canBuild(TrapType.STUN, myLocation.add(Direction.SOUTH))) {
                                        rc.build(TrapType.STUN, myLocation.add(Direction.SOUTH));
                                    }
                                }
                            }

                            // Defend
                            if (isDefender && myTrapLocations != null && myTrapLocations.size() == 0) {
                                generateDefensiveTrapLocations();
                                // Debug.log("Calling generate trap locs. Length now: " +
                                // myTrapLocations.size());
                            }

                        }
                    }

                    rc.setIndicatorString(myState.toString());

                    // if (id % 10 == 0) {
                    // Debug.log("Priority flag: " + Comm.getEnemyPriorityFlag() + " raw: "
                    // + rc.readSharedArray(Comm.PRIORITY_FLAG_INDEX));
                    // Debug.log("All enemy flags: " +
                    // Debug.printMapLocations(Comm.getEnemyFlagLocations()));
                    // }

                    if (roundNum == 750 || roundNum == 1500) {
                        if (rc.canBuyGlobal(GlobalUpgrade.ACTION)) {
                            rc.buyGlobal(GlobalUpgrade.ACTION);
                        }
                        if (rc.canBuyGlobal(GlobalUpgrade.HEALING)) {
                            rc.buyGlobal(GlobalUpgrade.HEALING);
                        }
                    }

                    // test - report broadcast loactions - they change every like 30 rounds. so you
                    // could estimate the actual location.
                    // MapLocation[] broadcastedFlags = rc.senseBroadcastFlagLocations();
                    // if (broadcastedFlags.length > 0) {
                    // for (MapLocation flag : broadcastedFlags) {
                    // rc.setIndicatorDot(flag, 255, 255, 0);
                    // }
                    // if (enemyFlagBroadcasts.size() == 0 ||
                    // / !enemyFlagBroadcasts.contains(broadcastedFlags)) {
                    // enemyFlagBroadcasts.add(broadcastedFlags);
                    // }
                    // }
                    // if (roundNum == 400) {
                    // for (int f = 0; f < enemyFlagBroadcasts.size(); f++) {
                    // MapLocation[] flags = enemyFlagBroadcasts.get(f);
                    // for (int i = 0; i < flags.length; i++) {
                    // rc.setIndicatorDot(flags[i], 255, 255, 0);
                    // }
                    // }
                    // }

                }

            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You
                // should
                // handle GameActionExceptions judiciously, in case unexpected events occur in
                // the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println("GameActionException");
                e.printStackTrace();
                Clock.yield();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println("Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop
                // again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for
            // another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction
        // imminent...
    }

    private static boolean checkedFlag = true;

    private static void checkAndBuildDefensiveTraps() throws GameActionException {
        // every 10 rounds check if the flag is still there
        if (myTrapLocations.size() % 4 == 0)
            checkedFlag = false;

        if (!checkedFlag && roundNum > GameConstants.SETUP_ROUNDS) {
            // Debug.log("Checking my flag is there");
            if (rc.canSenseLocation(defendLocation)) {
                FlagInfo[] flags = rc.senseNearbyFlags(-1, rc.getTeam());
                if (flags.length == 0) {
                    // no flags
                    Comm.reportStolenFlag(defendLocation);
                    defendLocation = Comm.getNextDefendLocation();

                    if (defendLocation == null) {
                        isDefender = false;
                        myState = States.MOVING_TO_ENEMY_SPAWN;
                    } else {
                        myTrapLocations = null;
                        generateDefensiveTrapLocations();
                    }

                }
                checkedFlag = true;
            } else {
                Navigation.move(defendLocation);
            }
        } else if (myTrapLocations.size() > 0 && rc.canSenseLocation(myTrapLocations.get(0))) {
            MapInfo info = rc.senseMapInfo(myTrapLocations.get(0));
            if (info.isWall() || info.isWater() || info.getTrapType() != TrapType.NONE) {
                myTrapLocations.remove(0);
                checkAndBuildDefensiveTraps();
            } else if (info.getTrapType() == TrapType.NONE) {
                // can I build or just move
                if (rc.canBuild(TrapType.STUN, myTrapLocations.get(0))) {
                    rc.build(TrapType.STUN, myTrapLocations.get(0));
                } else if (Util.distance(myLocation, myTrapLocations.get(0)) > 1) {
                    Navigation.move(myTrapLocations.get(0));
                } else {
                    // wait
                }
            }
        } else if (myTrapLocations.size() > 0 && !rc.canSenseLocation(myTrapLocations.get(0))) {
            Navigation.move(myTrapLocations.get(0));
        } else {
            // built all the traps!
            // now just wait
        }
    }

    private static void checkAndSaveDefenderStatus() throws GameActionException {
        MapLocation[] flagLocs = Comm.getFlagLocations();
        // null?
        boolean anyNull = false;
        for (MapLocation loc : flagLocs) {
            if (loc == null) {
                anyNull = true;
                break;
            }
        }
        if (!anyNull) {
            // should I be a defendder? Report my location and return the int for which
            // spawn I'm defending
            int spawnDefender = Comm.newSpawnDefender(myLocation);
            if (spawnDefender != -1) {
                isDefender = true;
                myState = States.DEFENDER;
                defendLocation = flagLocs[spawnDefender];
                // Debug.log("I'm a defender! for spawn " + spawnDefender + " at " +
                // defendLocation.toString());
                generateDefensiveTrapLocations();
            }
        }
    }

    private static void generateDefensiveTrapLocations() {
        boolean reverse = rng.nextBoolean();
        boolean alter = roundNum > 400;
        myTrapLocations = new ArrayList<MapLocation>(Util.defendingTraps.length);
        for (int i = 0; i < Util.defendingTraps.length; i++) {
            int index = reverse ? Util.defendingTraps.length - i - 1 : i;
            int[] trap = Util.defendingTraps[index];
            int x = defendLocation.x + trap[0];
            int y = defendLocation.y + trap[1];
            if (alter) {
                x = x + (rng.nextInt(5) - 2);
                y = y + (rng.nextInt(5) - 2);
            }
            if (x >= 0 && x < width && y >= 0 && y < height) {
                myTrapLocations.add(new MapLocation(x, y));
            }
        }

    }

    private static void spawnMe() throws GameActionException {
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();

        if (isDefender && rc.canSpawn(defendLocation)) {
            Comm.iCameAlive();
            rc.spawn(defendLocation);
            return;
        }

        // make an array of numbers the length of spawnLocs and then jumble it up
        int[] spawnLocsOrder = new int[spawnLocs.length];
        for (int i = 0; i < spawnLocs.length; i++) {
            spawnLocsOrder[i] = i;
        }
        for (int i = 0; i < spawnLocs.length; i++) {
            int j = rng.nextInt(spawnLocs.length);
            int temp = spawnLocsOrder[i];
            spawnLocsOrder[i] = spawnLocsOrder[j];
            spawnLocsOrder[j] = temp;
        }

        for (int i : spawnLocsOrder) {
            if (rc.canSpawn(spawnLocs[i])) {
                Comm.iCameAlive();
                rc.spawn(spawnLocs[i]);
                break;
            }
        }
    }

    public static MapLocation getClosestML(MapLocation[] closeEnemies) {
        MapLocation draft = closeEnemies[0];
        for (MapLocation l : closeEnemies) {
            if (l.distanceSquaredTo(myLocation) < draft.distanceSquaredTo(myLocation)) {
                draft = l;
            }
        }
        return draft;
    }

    public static MapInfo getClosestMI(MapInfo[] infos) {
        MapInfo draft = infos[0];
        for (MapInfo info : infos) {
            if (info.getMapLocation().distanceSquaredTo(myLocation) < draft
                    .getMapLocation().distanceSquaredTo(myLocation)) {
                draft = info;
            }
        }
        return draft;
    }

    public static RobotInfo getClosestRI(RobotInfo[] robs) {
        RobotInfo draft = robs[0];
        for (RobotInfo r : robs) {
            if (r.getLocation().distanceSquaredTo(myLocation) < draft.getLocation().distanceSquaredTo(myLocation)) {
                draft = r;
            }
        }
        return draft;
    }

    public static RobotInfo getLowHealthRI(RobotInfo[] enemies) {
        RobotInfo lowestHealthEnemy = null;
        int lowestHealth = 99999;

        for (RobotInfo enemy : enemies) {
            // Prioritize lowest health enemies
            if (enemy.health < lowestHealth) {
                lowestHealth = enemy.health;
                lowestHealthEnemy = enemy;
            }

        }

        return lowestHealthEnemy;
    }

}
