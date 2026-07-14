package com.zov.zovcapture.game;

import net.minecraft.nbt.CompoundTag;

public final class PlayerMatchStats {
    private String playerName = "";
    private String teamName = "";
    private int kills;
    private int deaths;
    private int captures;
    private int airdrops;
    private int moneyEarned;

    public String playerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String teamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public int kills() {
        return kills;
    }

    public int deaths() {
        return deaths;
    }

    public int captures() {
        return captures;
    }

    public int airdrops() {
        return airdrops;
    }

    public int moneyEarned() {
        return moneyEarned;
    }

    public void addKill() {
        kills++;
    }

    public void addDeath() {
        deaths++;
    }

    public void addCapture() {
        captures++;
    }

    public void addAirdrop() {
        airdrops++;
    }

    public void addMoneyEarned(int amount) {
        if (amount > 0) {
            moneyEarned += amount;
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("PlayerName", playerName);
        tag.putString("TeamName", teamName);
        tag.putInt("Kills", kills);
        tag.putInt("Deaths", deaths);
        tag.putInt("Captures", captures);
        tag.putInt("Airdrops", airdrops);
        tag.putInt("MoneyEarned", moneyEarned);
        return tag;
    }

    public static PlayerMatchStats load(CompoundTag tag) {
        PlayerMatchStats stats = new PlayerMatchStats();
        stats.playerName = tag.getString("PlayerName");
        stats.teamName = tag.getString("TeamName");
        stats.kills = tag.getInt("Kills");
        stats.deaths = tag.getInt("Deaths");
        stats.captures = tag.getInt("Captures");
        stats.airdrops = tag.getInt("Airdrops");
        stats.moneyEarned = tag.getInt("MoneyEarned");
        return stats;
    }
}
