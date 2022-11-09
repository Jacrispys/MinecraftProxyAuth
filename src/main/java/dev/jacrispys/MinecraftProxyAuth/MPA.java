package dev.jacrispys.MinecraftProxyAuth;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Random;

public class MPA extends Plugin implements Listener {

    private Connection connection;

    @Override
    public void onEnable() {
        try {
            SecretData.initLoginInfo();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            resetConnection("mc_discord");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        StartConnectionManager();

        getProxy().getPluginManager().registerListener(this, this);
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent e) throws SQLException {
        String key = generateKey();
        connection.createStatement().executeUpdate("REPLACE INTO mc_auth (`key`, ign, uuid) values ('" + key + "', '" + e.getPlayer().getName() + "', '" + e.getPlayer().getUniqueId() + "');");
        String kickMsg = color("&c&lAuthorizing User: " + e.getPlayer().getName() + "(" + e.getPlayer().getUniqueId() + ")...\n" +
                "&b&lPlease enter Code below into discord! This code will be used to link your minecraft account to the server.\n\n" +
                "&9&lCode: &e&l" + key);
        e.getPlayer().disconnect(kickMsg);
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    protected String generateKey() {
        String generatedStringOne = generateBuffer().toString();
        String generatedStringTwo = generateBuffer().toString();
        return generatedStringOne + "-" + generatedStringTwo;
    }

    private StringBuilder generateBuffer() {
        int leftLimit = 48; // number '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 5;
        Random random = new Random();
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int)
                    (random.nextFloat() * (rightLimit - leftLimit + 1));
            while ((randomLimitedInt >= 58 && randomLimitedInt <= 64) || (randomLimitedInt >= 91 && randomLimitedInt <= 96)) {
                randomLimitedInt = leftLimit + (int) (random.nextFloat() * (rightLimit - leftLimit + 1));
            }
            buffer.append((char) randomLimitedInt);
        }
        return buffer;
    }

    private Connection resetConnection(String dataBase) throws SQLException {
        try {
            String userName = "Jacrispys";
            String db_password = SecretData.getDataBasePass();

            String url = "jdbc:mysql://" + SecretData.getDBHost() + ":3306/" + dataBase + "?autoReconnect=true";
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            connection = DriverManager.getConnection(url, userName, db_password);
            return connection;


        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Could not connect to the given database!");
        }
    }

    private Thread thread;

    private void StartConnectionManager() {
        this.thread = new Thread(() -> {
            try {
                while (true) {
                    if (!connection.isClosed())  {
                        Thread.sleep(3600000);
                        return;
                    }
                    resetConnection("mc_discord");
                }
            } catch (SQLException | InterruptedException ex) {
                ex.printStackTrace();
            }
        });
    }

}
