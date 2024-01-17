package duck7;

import battlecode.common.MapLocation;

public class Debug extends RobotPlayer {
  public static void log(String s) {
    System.out.println(myLocation.toString() + " " + s);
  }

  public static void logId(String s, int id) {
    System.out.println(id + ": " + s);
  }

  public static String printMapLocations(MapLocation[] locs) {
    String s = "";
    for (MapLocation loc : locs) {
      if (loc != null) {
        s += loc.toString() + " ";
      } else {
        s += "null ";
      }
    }
    return s;

  }

}
