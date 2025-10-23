package de.lostesburger.mySqlPlayerBridge.Utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

public class Utils {
    public static boolean isPluginEnabled(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin != null && plugin.isEnabled();
    }
    
    public static String serializeObject(List<Map<String, Object>> object) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            oos.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }
    
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> deserializeObject(String serializedObject) {
        try {
            byte[] data = Base64.getDecoder().decode(serializedObject);
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);
            List<Map<String, Object>> object = (List<Map<String, Object>>) ois.readObject();
            ois.close();
            return object;
        } catch (Exception e) {
            return null;
        }
    }
}
