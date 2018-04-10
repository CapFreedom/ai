import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.lang.Math;

import org.json.*;

class Main {
    public static void main(String[] args) {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        try {
            line = in.readLine();
            JSONObject configJson = new JSONObject(line);
            Config config = new Config(configJson.toMap());
            while ((line = in.readLine()) != null && line.length() != 0) {
                JSONObject parsed = new JSONObject(line);
                JSONObject command = onTick(parsed, config);
                System.out.println(command.toString());
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public static JSONObject onTick(JSONObject parsed, final Config config) {
        JSONArray mineJson = parsed.getJSONArray("Mine");
        JSONObject command = new JSONObject();
        if (mineJson.length() > 0) {
            Mine mine = new Mine(mineJson.getJSONObject(0).toMap(), config);
            JSONArray objectsJson = parsed.getJSONArray("Objects");
            List<GameObject> objects = objectsJson.toList().stream().map((j) -> new GameObject(((JSONObject) j).toMap(), config)).collect(Collectors.toList());

            List<GameObject>[] allObjects = getAll(mine, objects);
            List<GameObject> allPossibleFood = allObjects[0];
            List<GameObject> allPossibleThreats = allObjects[1];
            List<GameObject> allPossibleViruses = allObjects[2];

            List<GameObject> clusterAllFood = clusterFood(mine.r, allPossibleFood);

            if (food != null) {
                command.put("X", food.getInt("X"));
                command.put("Y", food.getInt("Y"));
            } else {
                command.put("X", 0);
                command.put("Y", 0);
                command.put("Debug", "No food");
            }
        } else {
            command.put("X", 0);
            command.put("Y", 0);
            command.put("Debug", "Died");
        }
        return command;
    }

    static List<GameObject>[] getAll(GameObject mine, List<GameObject> objects) {
        List<GameObject>[] result = (List<GameObject>[]) new ArrayList[3];
        for (GameObject o : objects) {
            if (isFood(mine, o)) {
                result[0].add(o);
            } else if (isThreat(mine, o)) {
                result[1].add(o);
            } else if (o.type.equals(Type.VIRUS)) {
                result[2].add(o);
            }
        }
        return result;
    }

    public static List<GameObject> getAllPossibleFood(List<GameObject> objects) {
        return objects.stream().filter((o) -> o.type.equals(Type.FOOD)).collect(Collectors.toList());
    }

    static boolean isFood(GameObject mine, GameObject o) {
        if (o.type.equals(Type.FOOD) || !o.type.equals(Type.VIRUS) && compareMass(mine, o))
            return true;
        else
            return false;
    }

    static boolean isThreat(GameObject mine, GameObject o) {
        return !o.type.equals(Type.VIRUS) && compareMass(mine, o);
    }

    static boolean compareMass(GameObject o1, GameObject o2, Double ratio) {
        if (o1.m * ratio < o2.m)
            return true;
        else
            return false;
    }

    static boolean compareMass(GameObject o1, GameObject o2) {
        return compareMass(o1, o2, 1.2d);
    }

    static boolean canSplit(GameObject o1, GameObject o2) {
        return compareMass(o1, o2, 2.8d) && !compareMass(o1, o2, 20d);
    }

    static List<GameObject> clusterFood(double blobSize, List<GameObject> foodList) {

    }

    static Double computeInexpensiveDistance(double x1, double y1, double x2, double y2) {
        double xdis = x1 - x2;
        double ydis = y1 - y2;
        // Get abs quickly
        xdis = Math.abs(xdis);
        ydis = Math.abs(ydis);
        return xdis + ydis;
    }

    static Double computeDistance(double x1, double y1, double x2, double y2, double s1, double s2) {
        double xdis = x1 - x2;
        double ydis = y1 - y2;
        return Math.sqrt(xdis * xdis + ydis * ydis) - (s1 + s2);
    }

    static Double computeDistance(double x1, double y1, double x2, double y2) {
        return computeDistance(x1, y1, x2, y2, 0, 0);
    }
}

class Config {
    final Integer GAME_HEIGHT;
    final Double INERTION_FACTOR;
    final Double FOOD_MASS;
    final Double VIRUS_RADIUS;
    final Double SPEED_FACTOR;
    final Double VIRUS_SPLIT_MASS;
    final Double VISCOSITY;
    final Integer MAX_FRAGS_CNT;
    final Integer GAME_WIDTH;
    final Integer GAME_TICKS;

    Config(Map<String, Object> map) {
        GAME_HEIGHT = (Integer) map.get("GAME_HEIGHT");
        GAME_WIDTH = (Integer) map.get("GAME_WIDTH");
        MAX_FRAGS_CNT = (Integer) map.get("MAX_FRAGS_CNT");
        GAME_TICKS = (Integer) map.get("GAME_TICKS");
        INERTION_FACTOR = (Double) map.get("INERTION_FACTOR");
        FOOD_MASS = (Double) map.get("FOOD_MASS");
        VIRUS_RADIUS = (Double) map.get("VIRUS_RADIUS");
        SPEED_FACTOR = (Double) map.get("SPEED_FACTOR");
        VIRUS_SPLIT_MASS = (Double) map.get("VIRUS_SPLIT_MASS");
        VISCOSITY = (Double) map.get("VISCOSITY");
    }
}

class GameObject {
    static HashMap<String, Type> typeMap = new HashMap<>();

    static {
        typeMap.put("V", Type.VIRUS);
        typeMap.put("P", Type.PLAYER);
        typeMap.put("E", Type.EJECTION);
        typeMap.put("M", Type.MINE);
        typeMap.put("F", Type.FOOD);
    }

    public final Integer id;
    public final Double m;
    public final Double x;
    public final Double y;
    public final Double r;
    public final Type type;

    public GameObject(Map<String, Object> map, Config config) {
        id = (Integer) map.get("id");
        x = (Double) map.get("X");
        y = (Double) map.get("Y");
        m = (Double) map.get("M");
        type = typeMap.get(map.getOrDefault("T", "M").toString());
        r = type.equals(Type.FOOD) ? 2.5d : (Double) map.getOrDefault("R", config.VIRUS_RADIUS);
    }
}

class Mine extends GameObject {
    public final Integer ttf;
    public final Double sx;
    public final Double sy;

    Mine(Map<String, Object> map, Config config) {
        super(map, config);
        sx = (Double) map.get("SX");
        sy = (Double) map.get("SY");
        ttf = (Integer) map.get("TTF");
    }
}

enum Type {
    MINE, VIRUS, FOOD, PLAYER, EJECTION
}