package de.lostesburger.mySqlPlayerBridge.Config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class SimpleConfig {
    private final JavaPlugin plugin;
    private final String fileName;
    private YamlConfiguration config;
    private File configFile;

    public SimpleConfig(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.configFile = new File(plugin.getDataFolder(), fileName);
        loadConfig();
    }

    private void loadConfig() {
        // 确保数据文件夹存在
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // 如果配置文件不存在，则从资源中复制默认配置
        if (!configFile.exists()) {
            // 尝试从插件资源中复制默认配置文件
            InputStream defaultConfigStream = plugin.getResource(fileName);
            if (defaultConfigStream != null) {
                try {
                    Files.copy(defaultConfigStream, configFile.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // 如果没有默认配置文件，则创建一个空的配置文件
                try {
                    configFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // 加载配置
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }
}