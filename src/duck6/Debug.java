package duck6;

import battlecode.common.MapLocation;

public class Debug {
  public static void log(String s) {
    System.out.println(s);
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
