package duck7;

import battlecode.common.Clock;
import battlecode.common.MapLocation;
import java.util.HashMap;

public class Debug extends RobotPlayer {
  static boolean showLogs = true;

  public static void log(String s) {
    if (showLogs)
      System.out.println(pl(rc.getLocation()) + " " + s);
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

  private static HashMap<String, Integer> bytecodeUsage = new HashMap<String, Integer>();

  public static void checkBytecodeUsageStart(String name) {
    bytecodeUsage.put(name, Clock.getBytecodeNum());
  }

  public static void checkBytecodeUsageStop(String name) {
    int bytecodeEnd = Clock.getBytecodeNum();
    int end = bytecodeUsage.get(name);
    Debug.log("Bytecode used [" + name + "]: " + (bytecodeEnd - end));
  }

}
