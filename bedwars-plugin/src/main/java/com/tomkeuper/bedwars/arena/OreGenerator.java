/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: andrew.dascalu@gmail.com
 */

package com.tomkeuper.bedwars.arena;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.generator.GeneratorType;
import com.tomkeuper.bedwars.api.arena.generator.IGenHolo;
import com.tomkeuper.bedwars.api.arena.generator.IGenerator;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.api.events.gameplay.GeneratorUpgradeEvent;
import com.tomkeuper.bedwars.api.events.gameplay.GeneratorDropEvent;
import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.api.language.Messages;
import com.tomkeuper.bedwars.api.region.Cuboid;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@SuppressWarnings("WeakerAccess")
public class OreGenerator implements IGenerator {

    private Location location;
    private double upgradeStage = 1.0;
    private int spawnLimit = 0, amount = 1;
    final int speedMultiplier = 4;
    private double delay = 1, lastSpawn;
    private IArena arena;
    private ItemStack ore;
    private GeneratorType type;
    private int rotate = 0, dropID = 0;
    private ITeam bwt;
    boolean up = true, disabled = false;

    /**
     * Generator holograms per language <iso, holo></iso,>
     */
    private HashMap<String, IGenHolo> armorStands = new HashMap<>();

    private ArmorStand item;
    public boolean stack = BedWars.getGeneratorsCfg().getBoolean(ConfigPath.GENERATOR_STACK_ITEMS);

    private static final ConcurrentLinkedDeque<OreGenerator> rotation = new ConcurrentLinkedDeque<>();

    public OreGenerator(Location location, IArena arena, GeneratorType type, ITeam bwt) {
        if (type == GeneratorType.EMERALD || type == GeneratorType.DIAMOND) {
            this.location = new Location(location.getWorld(), location.getBlockX() + 0.5, location.getBlockY() + 1.3, location.getBlockZ() + 0.5);
        } else {
            this.location = location.add(0, 1.3, 0);
        }
        this.arena = arena;
        this.bwt = bwt;
        this.type = type;
        loadDefaults();
        BedWars.debug("Initializing new generator at: " + location + " - " + type + " - " + (bwt == null ? "NOTEAM" : bwt.getName()));

        Cuboid c = new Cuboid(location, arena.getConfig().getInt(ConfigPath.ARENA_GENERATOR_PROTECTION), true);
        c.setMaxY(c.getMaxY() + 5);
        c.setMinY(c.getMinY() - 2);
        arena.getRegionsList().add(c);
    }

    @Override
    public void upgrade() {
        switch (type) {
            case DIAMOND:
                upgradeStage++;
                if (upgradeStage == 2) {
                    delay = BedWars.getGeneratorsCfg().getDouble(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_II_DELAY) == null ?
                            "Default." + ConfigPath.GENERATOR_DIAMOND_TIER_II_DELAY : arena.getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_II_DELAY) * speedMultiplier;
                    amount = BedWars.getGeneratorsCfg().getInt(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_II_AMOUNT) == null ?
                            "Default." + ConfigPath.GENERATOR_DIAMOND_TIER_II_AMOUNT : arena.getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_II_AMOUNT);
                    spawnLimit = BedWars.getGeneratorsCfg().getInt(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_II_SPAWN_LIMIT) == null ?
                            "Default." + ConfigPath.GENERATOR_DIAMOND_TIER_II_SPAWN_LIMIT : arena.getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_II_SPAWN_LIMIT);
                } else if (upgradeStage == 3) {
                    delay = BedWars.getGeneratorsCfg().getDouble(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_III_DELAY) == null ?
                            "Default." + ConfigPath.GENERATOR_DIAMOND_TIER_III_DELAY : arena.getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_III_DELAY) * speedMultiplier;
                    amount = BedWars.getGeneratorsCfg().getInt(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_III_AMOUNT) == null ?
                            "Default." + ConfigPath.GENERATOR_DIAMOND_TIER_III_AMOUNT : arena.getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_III_AMOUNT);
                    spawnLimit = BedWars.getGeneratorsCfg().getInt(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_II_SPAWN_LIMIT) == null ?
                            "Default." + ConfigPath.GENERATOR_DIAMOND_TIER_III_SPAWN_LIMIT : arena.getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_III_SPAWN_LIMIT);
                }
                ore = new ItemStack(Material.DIAMOND);
                for (IGenHolo e : armorStands.values()) {
                    e.setTierName(Language.getLang(e.getIso()).m(Messages.GENERATOR_HOLOGRAM_TIER).replace("%bw_tier%", Language.getLang(e.getIso())
                            .m(upgradeStage == 2 ? Messages.FORMATTING_GENERATOR_TIER2 : Messages.FORMATTING_GENERATOR_TIER3)));
                }
                break;
            case EMERALD:
                upgradeStage++;
                if (upgradeStage == 2) {
                    delay = BedWars.getGeneratorsCfg().getDouble(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_II_DELAY) == null ?
                            "Default." + ConfigPath.GENERATOR_EMERALD_TIER_II_DELAY : arena.getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_II_DELAY) * speedMultiplier;
                    amount = BedWars.getGeneratorsCfg().getInt(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_II_AMOUNT) == null ?
                            "Default." + ConfigPath.GENERATOR_EMERALD_TIER_II_AMOUNT : arena.getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_II_AMOUNT);
                    spawnLimit = BedWars.getGeneratorsCfg().getInt(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_II_SPAWN_LIMIT) == null ?
                            "Default." + ConfigPath.GENERATOR_EMERALD_TIER_II_SPAWN_LIMIT : arena.getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_II_SPAWN_LIMIT);
                } else if (upgradeStage == 3) {
                    delay = BedWars.getGeneratorsCfg().getDouble(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_III_DELAY) == null ?
                            "Default." + ConfigPath.GENERATOR_EMERALD_TIER_III_DELAY : arena.getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_III_DELAY) * speedMultiplier;
                    amount = BedWars.getGeneratorsCfg().getInt(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_III_AMOUNT) == null ?
                            "Default." + ConfigPath.GENERATOR_EMERALD_TIER_III_AMOUNT : arena.getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_III_AMOUNT);
                    spawnLimit = BedWars.getGeneratorsCfg().getInt(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_II_SPAWN_LIMIT) == null ?
                            "Default." + ConfigPath.GENERATOR_EMERALD_TIER_III_SPAWN_LIMIT : arena.getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_III_SPAWN_LIMIT);
                }
                ore = new ItemStack(Material.EMERALD);
                for (IGenHolo e : armorStands.values()) {
                    e.setTierName(Language.getLang(e.getIso()).m(Messages.GENERATOR_HOLOGRAM_TIER).replace("%bw_tier%",
                            Language.getLang(e.getIso()).m(upgradeStage == 2 ? Messages.FORMATTING_GENERATOR_TIER2 : Messages.FORMATTING_GENERATOR_TIER3)));
                }
                break;
        }
        Bukkit.getPluginManager().callEvent(new GeneratorUpgradeEvent(this));
    }

    @Override
    public void spawn() {
        if (arena.getStatus() != GameState.playing){
            return;
        }

        if (disabled) return;

        if (lastSpawn <= 0) {
            lastSpawn = delay - 1;

            if (spawnLimit != 0) {
                int oreCount = 0;

                for (Entity e : location.getWorld().getNearbyEntities(location, 3, 3, 3)) {
                    if (e.getType() == EntityType.DROPPED_ITEM) {
                        Item i = (Item) e;
                        if (i.getItemStack().getType() == ore.getType()) {
                            oreCount++;
                        }
                        if (oreCount >= spawnLimit) return;
                    }
                }
            }

            GeneratorDropEvent event;
            Bukkit.getPluginManager().callEvent(event = new GeneratorDropEvent(this));

            if (event.isCancelled()){
                return;
            }
            dropItem(location);
            return;
        }
        lastSpawn--;
        for (IGenHolo e : armorStands.values()) {
            e.setTimerName(Language.getLang(e.getIso()).m(Messages.GENERATOR_HOLOGRAM_TIMER).replace("%bw_seconds%", String.valueOf((int) Math.ceil(lastSpawn/speedMultiplier))));
        }
    }

    private void dropItem(Location location, double amount) {
        for (double temp = amount; temp > 0; temp--) {
            ItemStack itemStack = new ItemStack(ore);
            if (!stack) {
                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.setDisplayName("custom" + dropID++);
                itemStack.setItemMeta(itemMeta);
            }
            Item item = location.getWorld().dropItem(location, itemStack);
            item.setVelocity(new Vector(0, 0, 0));
        }
    }

    /**
     * Drop item stack with ID
     */
    @Override
    public void dropItem(Location location) {
        dropItem(location, amount);
    }

    @Override
    public void setOre(ItemStack ore) {
        BedWars.debug("Changing ore for generator at " + location.toString() + " from " + this.ore + " to " + ore);
        this.ore = ore;
    }

    /**
     * Get the arena assigned to this generator.
     */
    public IArena getArena() {
        return arena;
    }

    public static ConcurrentLinkedDeque<OreGenerator> getRotation() {
        return rotation;
    }

    @Override
    public HashMap<String, IGenHolo> getLanguageHolograms() {
        return armorStands;
    }

    @SuppressWarnings("WeakerAccess")
    public class HoloGram implements IGenHolo {
        String iso;
        ArmorStand tier, timer, name;

        public HoloGram(String iso) {
            this.iso = iso;
            this.tier = createArmorStand(Language.getLang(iso).m(Messages.GENERATOR_HOLOGRAM_TIER)
                    .replace("%bw_tier%", Language.getLang(iso).m(Messages.FORMATTING_GENERATOR_TIER1)), location.clone().add(0, 3, 0));
            this.timer = createArmorStand(Language.getLang(iso).m(Messages.GENERATOR_HOLOGRAM_TIMER)
                    .replace("%bw_seconds%", String.valueOf(lastSpawn)), location.clone().add(0, 2.4, 0));
            this.name = createArmorStand(Language.getLang(iso).m(getOre().getType() == Material.DIAMOND ? Messages.GENERATOR_HOLOGRAM_TYPE_DIAMOND
                    : Messages.GENERATOR_HOLOGRAM_TYPE_EMERALD), location.clone().add(0, 2.7, 0));

        }

        @Override
        public void updateForAll() {
            for (Player p2 : timer.getWorld().getPlayers()) {
                if (Language.getPlayerLanguage(p2).getIso().equalsIgnoreCase(iso)) continue;
                BedWars.nms.hideEntity(tier, p2);
                BedWars.nms.hideEntity(timer, p2);
                BedWars.nms.hideEntity(name, p2);
            }
        }

        @Override
        public void updateForPlayer(Player p, String lang) {
            if (lang.equalsIgnoreCase(iso)) return;
            BedWars.nms.hideEntity(tier, p);
            BedWars.nms.hideEntity(timer, p);
            BedWars.nms.hideEntity(name, p);
        }

        @Override
        public void setTierName(String name) {
            if (tier.isDead()) return;
            tier.setCustomName(name);
        }

        @Override
        public String getIso() {
            return iso;
        }

        @Override
        public void setTimerName(String name) {
            if (timer.isDead()) return;
            timer.setCustomName(name);
        }

        @Override
        public void destroy() {
            tier.remove();
            timer.remove();
            name.remove();
        }
    }

    private static ArmorStand createArmorStand(String name, Location l) {
        ArmorStand a = (ArmorStand) l.getWorld().spawnEntity(l, EntityType.ARMOR_STAND);
        a.setGravity(false);
        if (name != null) {
            a.setCustomName(name);
            a.setCustomNameVisible(true);
        }
        a.setRemoveWhenFarAway(false);
        a.setVisible(false);
        a.setCanPickupItems(false);
        a.setArms(false);
        a.setBasePlate(false);
        a.setMarker(true);
        return a;
    }

    @Override
    public void rotate() {
        if (up) {
            if (rotate >= 540) {
                up = false;
            }
            if (rotate > 500) {
                item.setHeadPose(new EulerAngle(0, Math.toRadians(rotate += 1), 0));
            } else if (rotate > 470) {
                item.setHeadPose(new EulerAngle(0, Math.toRadians(rotate += 2), 0));
                /*item.teleport(item.getLocation().add(0, 0.005D, 0));*/
            } else if (rotate > 450) {
                item.setHeadPose(new EulerAngle(0, Math.toRadians(rotate += 3), 0));
                /*item.teleport(item.getLocation().add(0, 0.001D, 0));*/
            } else {
                item.setHeadPose(new EulerAngle(0, Math.toRadians(rotate += 4), 0));
                /*item.teleport(item.getLocation().add(0, 0.002D, 0));*/
            }
        } else {
            if (rotate <= 0) {
                up = true;
            }
            if (rotate > 120) {
                item.setHeadPose(new EulerAngle(0, Math.toRadians(rotate -= 4), 0));
                /*item.teleport(item.getLocation().subtract(0, 0.002D, 0));*/
            } else if (rotate > 90) {
                item.setHeadPose(new EulerAngle(0, Math.toRadians(rotate -= 3), 0));
                /*item.teleport(item.getLocation().add(0, 0.001D, 0));*/
            } else if (rotate > 70) {
                item.setHeadPose(new EulerAngle(0, Math.toRadians(rotate -= 2), 0));
                /*item.teleport(item.getLocation().add(0, 0.005D, 0));*/
            } else {
                item.setHeadPose(new EulerAngle(0, Math.toRadians(rotate -= 1), 0));
            }
        }
    }

    @Override
    public void setDelay(double delay) {
        this.delay = delay * speedMultiplier;
    }

    @Override
    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public ItemStack getOre() {
        return ore;
    }

    @Override
    public void disable() {
        if (getOre().getType() == Material.EMERALD || getOre().getType() == Material.DIAMOND) {
            rotation.remove(this);
            for (IGenHolo a : armorStands.values()) {
                a.destroy();
            }
            if (item != null) {
                item.remove();
            }
            armorStands.clear();
        }
        disabled = true;
    }

    @Override
    public void enable() {
        if (getOre().getType() == Material.EMERALD || getOre().getType() == Material.DIAMOND) {
            enableRotation();
        }
        disabled = false;
    }

    @Override
    public void updateHolograms(Player p, String iso) {
        for (IGenHolo h : armorStands.values()) {
            h.updateForPlayer(p, iso);
        }
    }

    @Override
    public void enableRotation() {
        //loadDefaults(false);
        //if (getType() == GeneratorType.EMERALD || getType() == GeneratorType.DIAMOND) {
        rotation.add(this);
        for (Language lan : Language.getLanguages()) {
            IGenHolo h = armorStands.get(lan.getIso());
            if (h == null) {
                armorStands.put(lan.getIso(), new HoloGram(lan.getIso()));
            }
        }
        for (IGenHolo hg : armorStands.values()) {
            hg.updateForAll();
        }

        item = createArmorStand(null, location.clone().add(0, 0.5, 0));
        item.setHelmet(new ItemStack(type == GeneratorType.DIAMOND ? Material.DIAMOND_BLOCK : Material.EMERALD_BLOCK));
        //}
    }

    @Override
    public void setSpawnLimit(int value) {
        this.spawnLimit = value;
    }

    private void loadDefaults() {
        switch (type) {
            case GOLD:
                delay = BedWars.getGeneratorsCfg().getDouble(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_GOLD_DELAY) == null ?
                        "Default." + ConfigPath.GENERATOR_GOLD_DELAY : arena.getGroup() + "." + ConfigPath.GENERATOR_GOLD_DELAY) * speedMultiplier;
                ore = new ItemStack(Material.GOLD_INGOT);
                amount = BedWars.getGeneratorsCfg().getInt(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_GOLD_AMOUNT) == null ?
                        "Default." + ConfigPath.GENERATOR_GOLD_AMOUNT : arena.getGroup() + "." + ConfigPath.GENERATOR_GOLD_AMOUNT);
                spawnLimit = BedWars.getGeneratorsCfg().getInt(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_GOLD_SPAWN_LIMIT) == null ?
                        "Default." + ConfigPath.GENERATOR_GOLD_SPAWN_LIMIT : arena.getGroup() + "." + ConfigPath.GENERATOR_GOLD_SPAWN_LIMIT);
                break;
            case IRON:
                delay = BedWars.getGeneratorsCfg().getDouble(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_IRON_DELAY) == null ?
                        "Default." + ConfigPath.GENERATOR_IRON_DELAY : arena.getGroup() + "." + ConfigPath.GENERATOR_IRON_DELAY) * speedMultiplier;
                amount = BedWars.getGeneratorsCfg().getInt(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_IRON_AMOUNT) == null ?
                        "Default." + ConfigPath.GENERATOR_IRON_AMOUNT : arena.getGroup() + "." + ConfigPath.GENERATOR_IRON_AMOUNT);
                ore = new ItemStack(Material.IRON_INGOT);
                spawnLimit = BedWars.getGeneratorsCfg().getInt(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_IRON_SPAWN_LIMIT) == null ?
                        "Default." + ConfigPath.GENERATOR_IRON_SPAWN_LIMIT : arena.getGroup() + "." + ConfigPath.GENERATOR_IRON_SPAWN_LIMIT);
                break;
            case DIAMOND:
                delay = BedWars.getGeneratorsCfg().getDouble(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_I_DELAY) == null ?
                        "Default." + ConfigPath.GENERATOR_DIAMOND_TIER_I_DELAY : arena.getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_I_DELAY) * speedMultiplier;
                amount = BedWars.getGeneratorsCfg().getInt(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_I_AMOUNT) == null ?
                        "Default." + ConfigPath.GENERATOR_DIAMOND_TIER_I_AMOUNT : arena.getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_I_AMOUNT);
                spawnLimit = BedWars.getGeneratorsCfg().getInt(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_I_SPAWN_LIMIT) == null ?
                        "Default." + ConfigPath.GENERATOR_DIAMOND_TIER_I_SPAWN_LIMIT : arena.getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_I_SPAWN_LIMIT);
                ore = new ItemStack(Material.DIAMOND);
                break;
            case EMERALD:
                delay = BedWars.getGeneratorsCfg().getDouble(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_I_DELAY) == null ?
                        "Default." + ConfigPath.GENERATOR_EMERALD_TIER_I_DELAY : arena.getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_I_DELAY) * speedMultiplier;
                amount = BedWars.getGeneratorsCfg().getInt(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_I_AMOUNT) == null ?
                        "Default." + ConfigPath.GENERATOR_EMERALD_TIER_I_AMOUNT : arena.getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_I_AMOUNT);
                spawnLimit = BedWars.getGeneratorsCfg().getInt(BedWars.getGeneratorsCfg().getYml().get(arena.getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_I_SPAWN_LIMIT) == null ?
                        "Default." + ConfigPath.GENERATOR_EMERALD_TIER_I_SPAWN_LIMIT : arena.getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_I_SPAWN_LIMIT);
                ore = new ItemStack(Material.EMERALD);
                break;
        }
        lastSpawn = delay;
    }

    @Override
    @Deprecated(since = "1.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "2.0")
    public ITeam getBwt() {
        return bwt;
    }
    @Override
    public ITeam getBedWarsTeam() {
        return bwt;
    }

    @Override
    public ArmorStand getHologramHolder() {
        return item;
    }

    @Override
    public GeneratorType getType() {
        return type;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public double getDelay() {
        return delay;
    }

    @Override
    public double getNextSpawn() {
        return lastSpawn;
    }

    @Override
    public int getSpawnLimit() {
        return spawnLimit;
    }

    @Override
    public void setNextSpawn(double nextSpawn) {
        this.lastSpawn = nextSpawn;
    }

    @Override
    public void setStack(boolean stack) {
        this.stack = stack;
    }

    @Override
    public boolean isStack() {
        return stack;
    }

    @Override
    public void setType(GeneratorType type) {
        this.type = type;
    }

    public void destroyData() {
        rotation.remove(this);
        location = null;
        arena = null;
        ore = null;
        bwt = null;
        armorStands = null;
        item = null;
    }
}
