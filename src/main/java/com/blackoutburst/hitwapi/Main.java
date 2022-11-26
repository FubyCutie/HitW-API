package com.blackoutburst.hitwapi;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import spark.Spark;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main extends JavaPlugin {

    public static String TOKEN = readEnv();

    private static String readEnv() {
        try {
            List<String> lines = Files.readAllLines(Paths.get(".env"));
            return lines.get(0).split("=")[1].replace("\"", "");
        } catch (Exception e) {
            System.err.println("Invalid .env file");
            System.exit(0);
        }
        return null;
    }

    @Override
    public void onEnable() {
        Spark.port(30100);
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

    private String generateAssJson(final File file, final String uuid) {
        final YamlConfiguration playerData = YamlConfiguration.loadConfiguration(file);
        final int Q = playerData.getInt("score.Q", 0);
        final int F = playerData.getInt("score.F", 0);
        final int WQ = playerData.getInt("score.WQ", 0);
        final int L = playerData.getInt("score.L", 0);
        final int WF = playerData.getInt("score.WF", 0);
        final String name = playerData.getString("name");

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
                + "\"wide_finals\":"+WF
                + "}"
                + "}";
    }
}
