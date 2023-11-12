package com.tomkeuper.bedwars.connectionmanager.redis;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.api.events.communication.RedisMessageEvent;
import com.tomkeuper.bedwars.arena.Arena;
import com.tomkeuper.bedwars.connectionmanager.LoadedUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.JedisPubSub;

public class RedisPubSubListener extends JedisPubSub {
    private final String BW_CHANNEL;
    public RedisPubSubListener(String channel) {
        this.BW_CHANNEL = channel;
    }

    @Override
    public void onMessage(String channel, String message) {
        if(channel.equals(BW_CHANNEL)) {
            final JsonObject json;
            try {
                json = new JsonParser().parse(message).getAsJsonObject();
                BedWars.debug("incoming json message: " + json.toString());
            } catch (JsonSyntaxException e) {
                BedWars.plugin.getLogger().warning("Received bad data from redis message channel " + BW_CHANNEL);
                return;
            }
            if (!json.has("type")) return;
            switch (json.get("type").getAsString().toUpperCase()) {
                case "PLD":
                    new LoadedUser(json.get("uuid").getAsString(), json.get("arena_identifier").getAsString(), json.get("lang_iso").getAsString(), json.get("target").getAsString());
                    break;
                case "Q":
                    Player p = Bukkit.getPlayer(json.get("name").getAsString());
                    if (p != null && p.isOnline()){
                        IArena a = Arena.getArenaByPlayer(p);
                        if (a != null) {
                            JsonObject jo = new JsonObject();
                            jo.addProperty("type", "Q");
                            jo.addProperty("name", p.getName());
                            jo.addProperty("requester", json.get("requester").getAsString());
                            jo.addProperty("server_name", BedWars.config.getString(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID));
                            jo.addProperty("arena_id", a.getWorldName());
                        }
                    }
                    break;
                case "AM":
                    // Addon Message
                    if (!json.has("addon_name") || !json.has("addon_data")) {
                        break;
                    }

                    BedWars.debug("Calling RedisMessageEvent");
                    JsonElement addonDataElement = json.get("addon_data");

                    if (addonDataElement.isJsonPrimitive()) {
                        // Assuming addon_data is a string representation of a JSON object
                        String addonDataString = addonDataElement.getAsString();
                        JsonObject addonDataObject = new JsonParser().parse(addonDataString).getAsJsonObject();
                        Bukkit.getPluginManager().callEvent(new RedisMessageEvent(addonDataObject, json.get("addon_name").getAsString()));
                    } else {
                        // Handle other types if necessary
                        BedWars.debug("Unexpected type for 'addon_data': " + addonDataElement.getClass().getSimpleName());
                    }
                    break;
                default:
                    BedWars.debug("Found unexpected data from redis in `" + BW_CHANNEL + "` with message: " + json);
                    break;
            }
        }
    }
}
