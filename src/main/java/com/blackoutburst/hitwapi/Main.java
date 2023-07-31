package com.blackoutburst.hitwapi;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import spark.Spark;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

public class Main extends JavaPlugin {

    public static String TOKEN = readEnv(0);
    public static String PORT = readEnv(1);

    private static String readEnv(int index) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(".env"));
            return lines.get(index).split("=")[1].replace("\"", "");
        } catch (Exception e) {
            System.err.println("Invalid .env file");
            System.exit(0);
        }
        return null;
    }

    private static String readCompressed() {
        try (FileInputStream fis = new FileInputStream("./plugins/HitW/analytics.gz");
             GZIPInputStream gis = new GZIPInputStream(fis);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gis))) {

            return reader.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onEnable() {
        Spark.port(Integer.parseInt(PORT));

        Spark.get("/credits", (req, res) -> {
            String uuid = req.queryParams("uuid");

            if (uuid == null) {
                res.status(400);
                return "Missing parameter 'uuid'";
            }

            if (!uuid.contains("-")) {
                char[] givenUUID = uuid.toCharArray();
                char[] dashedUUID = new char[36];
                dashedUUID[8] = '-';
                dashedUUID[13] = '-';
                dashedUUID[18] = '-';
                dashedUUID[23] = '-';
                System.arraycopy(givenUUID, 0, dashedUUID, 0, 8);
                System.arraycopy(givenUUID, 8, dashedUUID, 9, 4);
                System.arraycopy(givenUUID, 12, dashedUUID, 14, 4);
                System.arraycopy(givenUUID, 16, dashedUUID, 19, 4);
                System.arraycopy(givenUUID, 20, dashedUUID, 24, 12);
                uuid = new String(dashedUUID);
            }

            File file = new File("./plugins/HitW/playerdata/monthly/"+uuid+".json");
            if (!file.exists()) {
                return "0, 0";
            }
            int currentMonth = Calendar.getInstance(TimeZone.getTimeZone("EST")).get(Calendar.MONTH);

            //temporary workaround until the datafixer is ran on everybody
            if (currentMonth == 6) {
                file = new File("./plugins/HitW/playerdata/"+uuid+".json");
                if (!file.exists()) {
                    return "0, 0";
                }
            }

            List<String> lines = Files.readAllLines(file.toPath());
            StringBuilder s = new StringBuilder();
            lines.forEach(s::append);
            String fileContents = s.toString();

            JsonParser parser = new JsonParser();
            JsonObject data = parser.parse(fileContents).getAsJsonObject().get("data").getAsJsonObject();

            JsonElement month = data.get("month");
            if (month != null) {
                if (month.getAsInt() != currentMonth) return "0, 0";
            }
            int creditsEarned = data.get("creditsEarned").getAsInt();

            return creditsEarned + ", " + creditsEarned;
        });

        Spark.get("/analytics", (req, res) -> {
            final String token = req.queryParams("token");

            if (token == null || !token.equals(TOKEN)) {
                res.status(401);
                return "Inavlid token";
            }

            return readCompressed();
        });

        Spark.get("/user", (req, res) -> {
            final String uuid = req.queryParams("uuid");
            if (uuid == null) {
                res.status(400);
                return "Missing parameter 'uuid'";
            }

            final File file = new File("./plugins/HitW/player data/"+uuid+".yml");
            if (!file.exists()) {
                return "null";
            }

            return generateAssJson(file, uuid);
        });


        Spark.get("/whitelistadd", (req, res) -> {
            final String token = req.queryParams("token");
            final String name = req.queryParams("name");

            if (token == null || !token.equals(TOKEN)) {
                res.status(401);
                return "Inavlid token";
            }

            if (name == null) {
                res.status(400);
                return "Missing parameter 'name'";
            }

            OfflinePlayer player = Bukkit.getOfflinePlayer(name);

            if (player == null) {
                res.status(400);
                return "Unknown player";
            }

            player.setWhitelisted(true);
            Bukkit.getServer().reloadWhitelist();

            return "Player added to the whitelist";
        });

        Spark.get("/whitelistremove", (req, res) -> {
            final String token = req.queryParams("token");
            final String name = req.queryParams("name");

            if (token == null || !token.equals(TOKEN)) {
                res.status(401);
                return "Inavlid token";
            }

            if (name == null) {
                res.status(400);
                return "Missing parameter 'name'";
            }

            OfflinePlayer player = Bukkit.getOfflinePlayer(name);

            if (player == null) {
                res.status(400);
                return "Unknown player";
            }

            player.setWhitelisted(false);
            Bukkit.getServer().reloadWhitelist();

            return "Player removed from the whitelist";
        });

        Spark.get("/whitelistenable", (req, res) -> {
            final String token = req.queryParams("token");

            if (token == null || !token.equals(TOKEN)) {
                res.status(401);
                return "Inavlid token";
            }

            Bukkit.getServer().setWhitelist(true);
            Bukkit.getServer().reloadWhitelist();

            return "Server whitelist enabled";
        });

        Spark.get("/whitelistdisable", (req, res) -> {
            final String token = req.queryParams("token");

            if (token == null || !token.equals(TOKEN)) {
                res.status(401);
                return "Inavlid token";
            }

            Bukkit.getServer().setWhitelist(false);
            Bukkit.getServer().reloadWhitelist();

            return "Server whitelist disabled";
        });
    }

    private String credits(String uuid) {
        if (!uuid.contains("-")) {
            char[] givenUUID = uuid.toCharArray();
            char[] dashedUUID = new char[36];
            dashedUUID[8] = '-';
            dashedUUID[13] = '-';
            dashedUUID[18] = '-';
            dashedUUID[23] = '-';
            System.arraycopy(givenUUID, 0, dashedUUID, 0, 8);
            System.arraycopy(givenUUID, 8, dashedUUID, 9, 4);
            System.arraycopy(givenUUID, 12, dashedUUID, 14, 4);
            System.arraycopy(givenUUID, 16, dashedUUID, 19, 4);
            System.arraycopy(givenUUID, 20, dashedUUID, 24, 12);
            uuid = String.copyValueOf(dashedUUID);
        }

        final File file = new File("./plugins/HitW/playerdata/"+uuid+".json");
        if (!file.exists()) {
            return "null";
        }

        try {
            List<String> lines = Files.readAllLines(file.toPath());
            StringBuilder s = new StringBuilder();
            lines.forEach(s::append);
            String fileContents = s.toString();

            JsonParser parser = new JsonParser();
            JsonObject data = parser.parse(fileContents).getAsJsonObject().get("data").getAsJsonObject();

            int credits = data.get("credits").getAsInt();
            int creditsEarned = data.get("creditsEarned").getAsInt();

            return credits + ", " + creditsEarned;
        } catch(Exception e) {
            return "0, 0";
        }
    }

    private String generateAssJson(final File file, final String uuid) {
        final YamlConfiguration playerData = YamlConfiguration.loadConfiguration(file);
        final int Q = playerData.getInt("score.Q", 0);
        final int F = playerData.getInt("score.F", 0);
        final int WQ = playerData.getInt("score.WQ", 0);
        final int L = playerData.getInt("score.L", 0);
        final int WF = playerData.getInt("score.WF", 0);
        final String name = playerData.getString("name");
        final int credit = Integer.parseInt(credits(uuid).split(", ")[0]);
        final int creditEarned = Integer.parseInt(credits(uuid).split(", ")[1]);


        String qh = "";
        for (int i = 1; i <= 100; i++) qh += playerData.getInt("qualification_history."+i, 0)+",";
        qh = qh.substring(0, qh.length() - 1);

        String fh = "";
        for (int i = 1; i <= 100; i++) fh += playerData.getInt("finals_history."+i, 0)+",";
        fh = fh.substring(0, fh.length() - 1);

        String wqh = "";
        for (int i = 1; i <= 100; i++) wqh += playerData.getInt("wide_qualification_history."+i, 0)+",";
        wqh = wqh.substring(0, wqh.length() - 1);

        String lh = "";
        for (int i = 1; i <= 100; i++) lh += playerData.getInt("lobby_history."+i, 0)+",";
        lh = lh.substring(0, lh.length() - 1);

        String wfh = "";
        for (int i = 1; i <= 100; i++) wfh += playerData.getInt("wide_finals_history."+i, 0)+",";
        wfh = wfh.substring(0, wfh.length() - 1);

        return "{"
                + "\"qualification_history\":["+qh+"],"
                + "\"finals_history\":["+fh+"],"
                + "\"wide_qualification_history\":["+wqh+"],"
                + "\"lobby_history\":["+lh+"],"
                + "\"wide_finals_history\":["+wfh+"],"
                + "\"name\":\""+name+"\","
                + "\"uuid\":\""+uuid+"\","
                + "\"scores\":{"
                + "\"qualification\":"+Q+","
                + "\"finals\":"+F+","
                + "\"wide_qualification\":"+WQ+","
                + "\"lobby\":"+L+","
                + "\"wide_finals\":"+WF+","
                + "\"credit\":"+credit+","
                + "\"credit_earned\":"+creditEarned
                + "}"
                + "}";
    }
}
