package net.covers1624.devlogin.data;

import com.google.gson.JsonElement;

import java.util.UUID;

/**
 * Created by covers1624 on 14/9/22.
 */
public class MinecraftProfile {

    public String id;
    public String name;
    public JsonElement skins;
    public JsonElement capes;

    public UUID uuid() {
        if (id.length() != 32) throw new IllegalStateException("Invalid slim UUID: " + id);
        String timeLow = id.substring(0, 8);
        String timeMid = id.substring(8, 12);
        String timeHighAndVersion = id.substring(12, 16);
        String variantAndSeq = id.substring(16, 20);
        String node = id.substring(20, 32);
        return UUID.fromString(timeLow + "-" + timeMid + "-" + timeHighAndVersion + "-" + variantAndSeq + "-" + node);
    }
}
