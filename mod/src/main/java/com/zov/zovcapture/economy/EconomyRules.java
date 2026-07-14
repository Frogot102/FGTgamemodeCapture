package com.zov.zovcapture.economy;

import net.minecraft.nbt.CompoundTag;

public final class EconomyRules {
    public enum HoldPersonalMode {
        ALL_MEMBERS,
        IN_ZONE,
        CAPTAIN
    }

    private int killRewardPersonal = 25;
    private int killRewardTeam = 10;
    private int holdPointPersonalPerSecond = 1;
    private int holdPointTeamPerSecond = 3;
    private int holdRewardIntervalSeconds = 1;
    private HoldPersonalMode holdPersonalMode = HoldPersonalMode.ALL_MEMBERS;

    public int killRewardPersonal() {
        return killRewardPersonal;
    }

    public int killRewardTeam() {
        return killRewardTeam;
    }

    public int holdPointPersonalPerSecond() {
        return holdPointPersonalPerSecond;
    }

    public int holdPointTeamPerSecond() {
        return holdPointTeamPerSecond;
    }

    public int holdRewardIntervalSeconds() {
        return holdRewardIntervalSeconds;
    }

    public HoldPersonalMode holdPersonalMode() {
        return holdPersonalMode;
    }

    public void setKillRewardPersonal(int value) {
        killRewardPersonal = Math.max(0, value);
    }

    public void setKillRewardTeam(int value) {
        killRewardTeam = Math.max(0, value);
    }

    public void setHoldPointPersonalPerSecond(int value) {
        holdPointPersonalPerSecond = Math.max(0, value);
    }

    public void setHoldPointTeamPerSecond(int value) {
        holdPointTeamPerSecond = Math.max(0, value);
    }

    public void setHoldRewardIntervalSeconds(int value) {
        holdRewardIntervalSeconds = Math.max(1, value);
    }

    public void setHoldPersonalMode(HoldPersonalMode mode) {
        holdPersonalMode = mode;
    }

    public void mergeFrom(EconomyRules other) {
        killRewardPersonal = other.killRewardPersonal;
        killRewardTeam = other.killRewardTeam;
        holdPointPersonalPerSecond = other.holdPointPersonalPerSecond;
        holdPointTeamPerSecond = other.holdPointTeamPerSecond;
        holdRewardIntervalSeconds = other.holdRewardIntervalSeconds;
        holdPersonalMode = other.holdPersonalMode;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("KillRewardPersonal", killRewardPersonal);
        tag.putInt("KillRewardTeam", killRewardTeam);
        tag.putInt("HoldPointPersonalPerSecond", holdPointPersonalPerSecond);
        tag.putInt("HoldPointTeamPerSecond", holdPointTeamPerSecond);
        tag.putInt("HoldRewardIntervalSeconds", holdRewardIntervalSeconds);
        tag.putString("HoldPersonalMode", holdPersonalMode.name());
        return tag;
    }

    public static EconomyRules load(CompoundTag tag) {
        EconomyRules rules = new EconomyRules();
        if (tag.contains("KillRewardPersonal")) {
            rules.killRewardPersonal = tag.getInt("KillRewardPersonal");
        }
        if (tag.contains("KillRewardTeam")) {
            rules.killRewardTeam = tag.getInt("KillRewardTeam");
        }
        if (tag.contains("HoldPointPersonalPerSecond")) {
            rules.holdPointPersonalPerSecond = tag.getInt("HoldPointPersonalPerSecond");
        }
        if (tag.contains("HoldPointTeamPerSecond")) {
            rules.holdPointTeamPerSecond = tag.getInt("HoldPointTeamPerSecond");
        }
        if (tag.contains("HoldRewardIntervalSeconds")) {
            rules.holdRewardIntervalSeconds = tag.getInt("HoldRewardIntervalSeconds");
        }
        if (tag.contains("HoldPersonalMode")) {
            rules.holdPersonalMode = HoldPersonalMode.valueOf(tag.getString("HoldPersonalMode"));
        }
        return rules;
    }

    public static HoldPersonalMode parseHoldPersonalMode(String raw) {
        return switch (raw.toLowerCase()) {
            case "zone", "in_zone" -> HoldPersonalMode.IN_ZONE;
            case "captain" -> HoldPersonalMode.CAPTAIN;
            default -> HoldPersonalMode.ALL_MEMBERS;
        };
    }
}
