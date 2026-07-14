package com.zov.zovcapture.game;

import com.zov.zovcapture.airdrop.ActiveAirdrop;
import com.zov.zovcapture.airdrop.AirdropLootEntry;
import com.zov.zovcapture.airdrop.AirdropSpawnPoint;
import com.zov.zovcapture.economy.EconomyManager;
import com.zov.zovcapture.economy.EconomyRules;
import com.zov.zovcapture.shop.ShopOffer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import com.zov.zovcapture.presets.ScoreboardTeamPresets;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.List;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class CaptureGameData extends SavedData {
    private static final String DATA_NAME = "zovcapture_game";

    private final Map<String, CapturePoint> points = new LinkedHashMap<>();
    private final Map<String, Integer> teamScores = new HashMap<>();
    private final Map<String, TeamVisualSettings> teamVisuals = new HashMap<>();
    private final MatchStatRules globalStatRules = new MatchStatRules();
    private final Map<String, MatchStatRules> teamStatRules = new HashMap<>();
    private final Map<String, BaseZone> baseZones = new LinkedHashMap<>();
    private final Map<UUID, Integer> personalMoney = new HashMap<>();
    private final Map<String, Integer> teamMoney = new HashMap<>();
    private final Map<String, UUID> teamCaptains = new HashMap<>();
    private final Map<UUID, String> playerClasses = new HashMap<>();
    private final Map<UUID, Long> classChangeAvailableAtTick = new HashMap<>();
    private final Map<UUID, Long> respawnPenaltyUntilTick = new HashMap<>();
    private final Map<String, ShopOffer> shopOffers = new LinkedHashMap<>();
    private final Map<UUID, Map<String, Long>> shopCooldowns = new HashMap<>();
    private final Map<String, AirdropSpawnPoint> airdropSpawnPoints = new LinkedHashMap<>();
    private final Map<String, AirdropLootEntry> airdropLoot = new LinkedHashMap<>();
    private final Map<UUID, PlayerMatchStats> matchStats = new HashMap<>();
    private final EconomyRules economyRules = new EconomyRules();
    private transient final Map<UUID, Integer> teamMoneyPulseByPlayer = new HashMap<>();

    private ResourceLocation neutralParticle = TeamVisualSettings.DEFAULT_NEUTRAL_PARTICLE;
    private ResourceLocation baseZoneParticle = TeamVisualSettings.DEFAULT_BASE_ZONE_PARTICLE;
    private boolean baseZoneParticlesEnabled = false;
    private int pointsToWin = 100;
    private boolean gameActive = false;
    private boolean matchFinished = false;
    private long countdownEndTick = 0L;
    @Nullable
    private String winningTeam;

    private boolean airdropEnabled = true;
    private int airdropIntervalSeconds = 600;
    private int airdropCaptureSeconds = 45;
    private ResourceLocation airdropParticle = ResourceLocation.withDefaultNamespace("dust");
    private long nextAirdropTick = 0;
    @Nullable
    private ActiveAirdrop activeAirdrop;

    public static CaptureGameData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CaptureGameData::new, CaptureGameData::load, DataFixTypes.LEVEL),
                DATA_NAME
        );
    }

    public Map<String, CapturePoint> points() {
        return Collections.unmodifiableMap(points);
    }

    public Map<String, Integer> teamScores() {
        return Collections.unmodifiableMap(teamScores);
    }

    public Map<String, TeamVisualSettings> teamVisuals() {
        return Collections.unmodifiableMap(teamVisuals);
    }

    public Map<String, BaseZone> baseZones() {
        return Collections.unmodifiableMap(baseZones);
    }

    @javax.annotation.Nullable
    public BaseZone findBaseForTeam(String teamName) {
        for (BaseZone zone : baseZones.values()) {
            if (teamName.equals(zone.team())) {
                return zone;
            }
        }
        return null;
    }

    public long countdownEndTick() {
        return countdownEndTick;
    }

    public void setCountdownEndTick(long countdownEndTick) {
        this.countdownEndTick = Math.max(0L, countdownEndTick);
        setDirty();
    }

    public boolean isMatchPreparing(long gameTime) {
        return isCountdownPending() && gameTime < countdownEndTick;
    }

    public boolean isCountdownPending() {
        return countdownEndTick > 0L && !gameActive && !matchFinished;
    }

    public int countdownSecondsRemaining(long gameTime) {
        if (countdownEndTick <= 0L || gameTime >= countdownEndTick) {
            return 0;
        }
        return (int) Math.ceil((countdownEndTick - gameTime) / 20.0D);
    }

    public Map<UUID, Integer> personalMoneyView() {
        return Collections.unmodifiableMap(personalMoney);
    }

    public Map<String, Integer> teamMoneyView() {
        return Collections.unmodifiableMap(teamMoney);
    }

    public Map<String, UUID> teamCaptains() {
        return Collections.unmodifiableMap(teamCaptains);
    }

    public Map<String, ShopOffer> shopOffers() {
        return Collections.unmodifiableMap(shopOffers);
    }

    public Map<String, AirdropSpawnPoint> airdropSpawnPoints() {
        return Collections.unmodifiableMap(airdropSpawnPoints);
    }

    public Map<String, AirdropLootEntry> airdropLoot() {
        return Collections.unmodifiableMap(airdropLoot);
    }

    public boolean airdropEnabled() {
        return airdropEnabled;
    }

    public void setAirdropEnabled(boolean airdropEnabled) {
        this.airdropEnabled = airdropEnabled;
        setDirty();
    }

    public int airdropIntervalSeconds() {
        return airdropIntervalSeconds;
    }

    public void setAirdropIntervalSeconds(int airdropIntervalSeconds) {
        this.airdropIntervalSeconds = Math.max(30, airdropIntervalSeconds);
        setDirty();
    }

    public int airdropCaptureSeconds() {
        return airdropCaptureSeconds;
    }

    public void setAirdropCaptureSeconds(int airdropCaptureSeconds) {
        this.airdropCaptureSeconds = Math.clamp(airdropCaptureSeconds, 5, 300);
        setDirty();
    }

    public ResourceLocation airdropParticle() {
        return airdropParticle;
    }

    public void setAirdropParticle(ResourceLocation airdropParticle) {
        this.airdropParticle = airdropParticle;
        setDirty();
    }

    public long nextAirdropTick() {
        return nextAirdropTick;
    }

    public void setNextAirdropTick(long nextAirdropTick) {
        this.nextAirdropTick = nextAirdropTick;
        setDirty();
    }

    @Nullable
    public ActiveAirdrop activeAirdrop() {
        return activeAirdrop;
    }

    public void setActiveAirdrop(@Nullable ActiveAirdrop activeAirdrop) {
        this.activeAirdrop = activeAirdrop;
        setDirty();
    }

    public void addAirdropSpawnPoint(AirdropSpawnPoint point) {
        airdropSpawnPoints.put(point.id(), point);
        setDirty();
    }

    public boolean removeAirdropSpawnPoint(String id) {
        if (airdropSpawnPoints.remove(id) != null) {
            setDirty();
            return true;
        }
        return false;
    }

    @Nullable
    public AirdropSpawnPoint getAirdropSpawnPoint(String id) {
        return airdropSpawnPoints.get(id);
    }

    public void addAirdropLoot(AirdropLootEntry entry) {
        airdropLoot.put(entry.id(), entry);
        setDirty();
    }

    public boolean removeAirdropLoot(String id) {
        if (airdropLoot.remove(id) != null) {
            setDirty();
            return true;
        }
        return false;
    }

    public int getShopCooldownRemaining(UUID playerId, String offerId, long gameTime) {
        Map<String, Long> map = shopCooldowns.get(playerId);
        if (map == null) {
            return 0;
        }
        Long until = map.get(offerId);
        if (until == null || until <= gameTime) {
            return 0;
        }
        return (int) Math.ceil((until - gameTime) / 20.0);
    }

    public void setShopCooldown(UUID playerId, String offerId, int cooldownSeconds, long gameTime) {
        if (cooldownSeconds <= 0) {
            return;
        }
        shopCooldowns.computeIfAbsent(playerId, ignored -> new HashMap<>())
                .put(offerId, gameTime + cooldownSeconds * 20L);
        setDirty();
    }

    public EconomyRules economyRules() {
        return economyRules;
    }

    public int getPersonalMoney(UUID playerId) {
        return personalMoney.getOrDefault(playerId, 0);
    }

    public void setPersonalMoney(UUID playerId, int amount) {
        personalMoney.put(playerId, Math.max(0, amount));
        setDirty();
    }

    public void addPersonalMoney(UUID playerId, int amount) {
        if (amount == 0) {
            return;
        }
        personalMoney.merge(playerId, amount, Integer::sum);
        setDirty();
    }

    public int getTeamMoney(String team) {
        return teamMoney.getOrDefault(team, 0);
    }

    public void setTeamMoney(String team, int amount) {
        teamMoney.put(team, Math.max(0, amount));
        setDirty();
    }

    public void addTeamMoney(String team, int amount) {
        if (amount == 0) {
            return;
        }
        teamMoney.merge(team, amount, Integer::sum);
        setDirty();
    }

    public void pulseTeamMoneyForTeam(MinecraftServer server, String teamName, int amount) {
        if (amount <= 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.getTeam() != null && teamName.equals(player.getTeam().getName())) {
                teamMoneyPulseByPlayer.merge(player.getUUID(), amount, Integer::sum);
            }
        }
    }

    public int consumeTeamMoneyPulse(UUID playerId) {
        Integer amount = teamMoneyPulseByPlayer.remove(playerId);
        return amount != null ? amount : 0;
    }

    public void setTeamCaptain(String team, UUID captainId) {
        teamCaptains.put(team, captainId);
        setDirty();
    }

    public void clearTeamCaptain(String team) {
        teamCaptains.remove(team);
        setDirty();
    }

    @Nullable
    public UUID getTeamCaptain(String team) {
        return teamCaptains.get(team);
    }

    @Nullable
    public String getPlayerClass(UUID playerId) {
        return playerClasses.get(playerId);
    }

    public void setPlayerClass(UUID playerId, String classId) {
        playerClasses.put(playerId, classId);
        setDirty();
    }

    public void clearPlayerClass(UUID playerId) {
        if (playerClasses.remove(playerId) != null) {
            setDirty();
        }
    }

    public long getClassChangeAvailableAtTick(UUID playerId) {
        return classChangeAvailableAtTick.getOrDefault(playerId, 0L);
    }

    public void setClassChangeAvailableAtTick(UUID playerId, long gameTick) {
        if (gameTick <= 0L) {
            clearClassChangeCooldown(playerId);
            return;
        }
        classChangeAvailableAtTick.put(playerId, gameTick);
        setDirty();
    }

    public void clearClassChangeCooldown(UUID playerId) {
        if (classChangeAvailableAtTick.remove(playerId) != null) {
            setDirty();
        }
    }

    public long getRespawnPenaltyUntil(UUID playerId) {
        return respawnPenaltyUntilTick.getOrDefault(playerId, 0L);
    }

    public void setRespawnPenaltyUntil(UUID playerId, long gameTick) {
        if (gameTick <= 0L) {
            clearRespawnPenalty(playerId);
            return;
        }
        respawnPenaltyUntilTick.put(playerId, gameTick);
        setDirty();
    }

    public void clearRespawnPenalty(UUID playerId) {
        if (respawnPenaltyUntilTick.remove(playerId) != null) {
            setDirty();
        }
    }

    public void addBaseZone(BaseZone zone) {
        baseZones.put(zone.id(), zone);
        setDirty();
    }

    public boolean removeBaseZone(String id) {
        if (baseZones.remove(id) != null) {
            setDirty();
            return true;
        }
        return false;
    }

    @Nullable
    public BaseZone getBaseZone(String id) {
        return baseZones.get(id);
    }

    public void addShopOffer(ShopOffer offer) {
        shopOffers.put(offer.id(), offer);
        setDirty();
    }

    public boolean removeShopOffer(String id) {
        if (shopOffers.remove(id) != null) {
            setDirty();
            return true;
        }
        return false;
    }

    @Nullable
    public ShopOffer getShopOffer(String id) {
        return shopOffers.get(id);
    }

    public void clearShopOffers() {
        if (!shopOffers.isEmpty()) {
            shopOffers.clear();
            setDirty();
        }
    }

    public int replaceShopOffers(List<ShopOffer> offers) {
        shopOffers.clear();
        for (ShopOffer offer : offers) {
            shopOffers.put(offer.id(), offer);
        }
        setDirty();
        return shopOffers.size();
    }

    public int mergeShopOffers(List<ShopOffer> offers) {
        for (ShopOffer offer : offers) {
            shopOffers.put(offer.id(), offer);
        }
        setDirty();
        return offers.size();
    }

    public MatchStatRules globalStatRules() {
        return globalStatRules;
    }

    public Map<String, MatchStatRules> teamStatRules() {
        return Collections.unmodifiableMap(teamStatRules);
    }

    public MatchStatRules getOrCreateTeamStatRules(String team) {
        return teamStatRules.computeIfAbsent(team, ignored -> new MatchStatRules());
    }

    public void clearTeamStatRules(String team) {
        teamStatRules.remove(team);
        setDirty();
    }

    public ResourceLocation neutralParticle() {
        return neutralParticle;
    }

    public void setNeutralParticle(ResourceLocation neutralParticle) {
        this.neutralParticle = neutralParticle;
        setDirty();
    }

    public ResourceLocation baseZoneParticle() {
        return baseZoneParticle;
    }

    public void setBaseZoneParticle(ResourceLocation baseZoneParticle) {
        this.baseZoneParticle = baseZoneParticle;
        setDirty();
    }

    public boolean baseZoneParticlesEnabled() {
        return baseZoneParticlesEnabled;
    }

    public void setBaseZoneParticlesEnabled(boolean baseZoneParticlesEnabled) {
        this.baseZoneParticlesEnabled = baseZoneParticlesEnabled;
        setDirty();
    }

    public TeamVisualSettings getOrCreateTeamVisual(String team) {
        return teamVisuals.computeIfAbsent(team, ignored -> new TeamVisualSettings());
    }

    @Nullable
    public TeamVisualSettings getTeamVisual(String team) {
        return teamVisuals.get(team);
    }

    public void clearTeamVisual(String team) {
        teamVisuals.remove(team);
        setDirty();
    }

    public ResourceLocation resolveTeamParticle(String team) {
        TeamVisualSettings settings = teamVisuals.get(team);
        if (settings != null && settings.particle().isPresent()) {
            return settings.particle().get();
        }
        return TeamVisualSettings.DEFAULT_PARTICLE;
    }

    public BossEvent.BossBarColor resolveTeamBossColor(String team, @Nullable BossEvent.BossBarColor fallback) {
        TeamVisualSettings settings = teamVisuals.get(team);
        if (settings != null && settings.bossBarColor().isPresent()) {
            return settings.bossBarColor().get();
        }
        return fallback != null ? fallback : TeamVisualSettings.DEFAULT_BOSS_COLOR;
    }

    public int pointsToWin() {
        return pointsToWin;
    }

    public void setPointsToWin(int pointsToWin) {
        this.pointsToWin = Math.max(1, pointsToWin);
        setDirty();
    }

    public boolean gameActive() {
        return gameActive;
    }

    public void setGameActive(boolean gameActive) {
        this.gameActive = gameActive;
        setDirty();
    }

    public boolean matchFinished() {
        return matchFinished;
    }

    @Nullable
    public String winningTeam() {
        return winningTeam;
    }

    public void resetMatch() {
        teamScores.clear();
        personalMoney.clear();
        teamMoney.clear();
        shopCooldowns.clear();
        classChangeAvailableAtTick.clear();
        respawnPenaltyUntilTick.clear();
        matchStats.clear();
        matchFinished = false;
        winningTeam = null;
        gameActive = false;
        countdownEndTick = 0L;
        activeAirdrop = null;
        nextAirdropTick = 0;
        EconomyManager.resetHoldRewardCounter();
        for (CapturePoint point : points.values()) {
            point.setProgress(0.0F);
            point.setOwnerTeam(null);
            point.setCapturingTeam(null);
        }
        setDirty();
    }

    public Map<UUID, PlayerMatchStats> matchStatsView() {
        return Collections.unmodifiableMap(matchStats);
    }

    public PlayerMatchStats getOrCreateMatchStats(UUID playerId, String playerName, String teamName) {
        return matchStats.computeIfAbsent(playerId, ignored -> {
            PlayerMatchStats stats = new PlayerMatchStats();
            stats.setPlayerName(playerName);
            stats.setTeamName(teamName);
            return stats;
        });
    }

    public CompoundTag exportPreset(MinecraftServer server) {
        HolderLookup.Provider provider = server.registryAccess();
        CompoundTag tag = new CompoundTag();

        ListTag pointList = new ListTag();
        for (CapturePoint point : points.values()) {
            pointList.add(point.saveTemplate());
        }
        tag.put("Points", pointList);

        ListTag baseList = new ListTag();
        for (BaseZone zone : baseZones.values()) {
            baseList.add(zone.save());
        }
        tag.put("BaseZones", baseList);

        CompoundTag visuals = new CompoundTag();
        teamVisuals.forEach((team, settings) -> visuals.put(team, settings.save()));
        tag.put("TeamVisuals", visuals);
        tag.putString("NeutralParticle", neutralParticle.toString());
        tag.putString("BaseZoneParticle", baseZoneParticle.toString());
        tag.putBoolean("BaseZoneParticlesEnabled", baseZoneParticlesEnabled);
        tag.put("GlobalStatRules", globalStatRules.save());

        CompoundTag teamStatsTag = new CompoundTag();
        teamStatRules.forEach((team, rules) -> teamStatsTag.put(team, rules.save()));
        tag.put("TeamStatRules", teamStatsTag);

        tag.put("EconomyRules", economyRules.save());

        ListTag shopList = new ListTag();
        for (ShopOffer offer : shopOffers.values()) {
            shopList.add(offer.save(provider));
        }
        tag.put("ShopOffers", shopList);

        ListTag airdropPoints = new ListTag();
        for (AirdropSpawnPoint point : airdropSpawnPoints.values()) {
            airdropPoints.add(point.save());
        }
        tag.put("AirdropSpawnPoints", airdropPoints);

        ListTag airdropLootList = new ListTag();
        for (AirdropLootEntry entry : airdropLoot.values()) {
            airdropLootList.add(entry.save(provider));
        }
        tag.put("AirdropLoot", airdropLootList);

        tag.putBoolean("AirdropEnabled", airdropEnabled);
        tag.putInt("AirdropIntervalSeconds", airdropIntervalSeconds);
        tag.putInt("AirdropCaptureSeconds", airdropCaptureSeconds);
        tag.putString("AirdropParticle", airdropParticle.toString());
        tag.putInt("PointsToWin", pointsToWin);
        tag.put("ScoreboardTeams", ScoreboardTeamPresets.exportTeams(server.getScoreboard(), provider));
        return tag;
    }

    public void applyPresetConfiguration(CompoundTag preset, HolderLookup.Provider provider, boolean merge) {
        if (!merge) {
            points.clear();
            baseZones.clear();
            teamVisuals.clear();
            teamStatRules.clear();
            shopOffers.clear();
            airdropSpawnPoints.clear();
            airdropLoot.clear();
        }

        if (preset.contains("Points")) {
            ListTag pointList = preset.getList("Points", Tag.TAG_COMPOUND);
            for (Tag entry : pointList) {
                CapturePoint point = CapturePoint.loadTemplate((CompoundTag) entry);
                points.put(point.id(), point);
            }
        }

        if (preset.contains("BaseZones")) {
            ListTag baseList = preset.getList("BaseZones", Tag.TAG_COMPOUND);
            for (Tag entry : baseList) {
                BaseZone zone = BaseZone.load((CompoundTag) entry);
                baseZones.put(zone.id(), zone);
            }
        }

        if (preset.contains("TeamVisuals")) {
            CompoundTag visuals = preset.getCompound("TeamVisuals");
            for (String key : visuals.getAllKeys()) {
                teamVisuals.put(key, TeamVisualSettings.load(visuals.getCompound(key)));
            }
        }
        if (preset.contains("NeutralParticle")) {
            neutralParticle = ResourceLocation.parse(preset.getString("NeutralParticle"));
        }
        if (preset.contains("BaseZoneParticle")) {
            baseZoneParticle = ResourceLocation.parse(preset.getString("BaseZoneParticle"));
        }
        if (preset.contains("BaseZoneParticlesEnabled")) {
            baseZoneParticlesEnabled = preset.getBoolean("BaseZoneParticlesEnabled");
        }
        if (preset.contains("GlobalStatRules")) {
            if (!merge) {
                globalStatRules.clear();
            }
            globalStatRules.mergeOver(MatchStatRules.load(preset.getCompound("GlobalStatRules")));
        }
        if (preset.contains("TeamStatRules")) {
            CompoundTag loadedTeamStats = preset.getCompound("TeamStatRules");
            for (String key : loadedTeamStats.getAllKeys()) {
                teamStatRules.put(key, MatchStatRules.load(loadedTeamStats.getCompound(key)));
            }
        }
        if (preset.contains("EconomyRules")) {
            economyRules.mergeFrom(EconomyRules.load(preset.getCompound("EconomyRules")));
        }
        if (preset.contains("ShopOffers")) {
            ListTag shopList = preset.getList("ShopOffers", Tag.TAG_COMPOUND);
            for (Tag entry : shopList) {
                ShopOffer offer = ShopOffer.load((CompoundTag) entry, provider);
                shopOffers.put(offer.id(), offer);
            }
        }
        if (preset.contains("AirdropSpawnPoints")) {
            ListTag airdropPointList = preset.getList("AirdropSpawnPoints", Tag.TAG_COMPOUND);
            for (Tag entry : airdropPointList) {
                AirdropSpawnPoint point = AirdropSpawnPoint.load((CompoundTag) entry);
                airdropSpawnPoints.put(point.id(), point);
            }
        }
        if (preset.contains("AirdropLoot")) {
            ListTag airdropLootList = preset.getList("AirdropLoot", Tag.TAG_COMPOUND);
            for (Tag entry : airdropLootList) {
                AirdropLootEntry lootEntry = AirdropLootEntry.load((CompoundTag) entry, provider);
                airdropLoot.put(lootEntry.id(), lootEntry);
            }
        }
        if (preset.contains("AirdropEnabled")) {
            airdropEnabled = preset.getBoolean("AirdropEnabled");
        }
        if (preset.contains("AirdropIntervalSeconds")) {
            airdropIntervalSeconds = preset.getInt("AirdropIntervalSeconds");
        }
        if (preset.contains("AirdropCaptureSeconds")) {
            airdropCaptureSeconds = preset.getInt("AirdropCaptureSeconds");
        }
        if (preset.contains("AirdropParticle")) {
            airdropParticle = ResourceLocation.parse(preset.getString("AirdropParticle"));
        }
        if (preset.contains("PointsToWin")) {
            pointsToWin = preset.getInt("PointsToWin");
        }
        setDirty();
    }

    public void addPoint(CapturePoint point) {
        points.put(point.id(), point);
        setDirty();
    }

    public boolean removePoint(String id) {
        if (points.remove(id) != null) {
            setDirty();
            return true;
        }
        return false;
    }

    @Nullable
    public CapturePoint getPoint(String id) {
        return points.get(id);
    }

    public void addScore(String teamName, int amount) {
        teamScores.merge(teamName, amount, Integer::sum);
        setDirty();
    }

    public int getScore(String teamName) {
        return teamScores.getOrDefault(teamName, 0);
    }

    public void finishMatch(String teamName) {
        matchFinished = true;
        winningTeam = teamName;
        gameActive = false;
        countdownEndTick = 0L;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag pointList = new ListTag();
        for (CapturePoint point : points.values()) {
            pointList.add(point.save());
        }
        tag.put("Points", pointList);

        CompoundTag scores = new CompoundTag();
        teamScores.forEach(scores::putInt);
        tag.put("TeamScores", scores);

        CompoundTag visuals = new CompoundTag();
        teamVisuals.forEach((team, settings) -> visuals.put(team, settings.save()));
        tag.put("TeamVisuals", visuals);
        tag.putString("NeutralParticle", neutralParticle.toString());
        tag.putString("BaseZoneParticle", baseZoneParticle.toString());
        tag.putBoolean("BaseZoneParticlesEnabled", baseZoneParticlesEnabled);
        tag.put("GlobalStatRules", globalStatRules.save());

        CompoundTag teamStats = new CompoundTag();
        teamStatRules.forEach((team, rules) -> teamStats.put(team, rules.save()));
        tag.put("TeamStatRules", teamStats);

        ListTag baseList = new ListTag();
        for (BaseZone zone : baseZones.values()) {
            baseList.add(zone.save());
        }
        tag.put("BaseZones", baseList);

        CompoundTag personal = new CompoundTag();
        personalMoney.forEach((id, amount) -> personal.putInt(id.toString(), amount));
        tag.put("PersonalMoney", personal);

        CompoundTag teamCash = new CompoundTag();
        teamMoney.forEach(teamCash::putInt);
        tag.put("TeamMoney", teamCash);

        CompoundTag captains = new CompoundTag();
        teamCaptains.forEach((team, uuid) -> captains.putUUID(team, uuid));
        tag.put("TeamCaptains", captains);

        CompoundTag classes = new CompoundTag();
        playerClasses.forEach((uuid, classId) -> classes.putString(uuid.toString(), classId));
        tag.put("PlayerClasses", classes);

        CompoundTag classCooldowns = new CompoundTag();
        classChangeAvailableAtTick.forEach((uuid, tick) -> classCooldowns.putLong(uuid.toString(), tick));
        tag.put("ClassChangeCooldowns", classCooldowns);

        CompoundTag respawnPenalties = new CompoundTag();
        respawnPenaltyUntilTick.forEach((uuid, tick) -> respawnPenalties.putLong(uuid.toString(), tick));
        tag.put("RespawnPenaltyUntil", respawnPenalties);

        tag.put("EconomyRules", economyRules.save());

        ListTag shopList = new ListTag();
        for (ShopOffer offer : shopOffers.values()) {
            shopList.add(offer.save(provider));
        }
        tag.put("ShopOffers", shopList);

        ListTag airdropPoints = new ListTag();
        for (AirdropSpawnPoint point : airdropSpawnPoints.values()) {
            airdropPoints.add(point.save());
        }
        tag.put("AirdropSpawnPoints", airdropPoints);

        ListTag airdropLootList = new ListTag();
        for (AirdropLootEntry entry : airdropLoot.values()) {
            airdropLootList.add(entry.save(provider));
        }
        tag.put("AirdropLoot", airdropLootList);

        tag.putBoolean("AirdropEnabled", airdropEnabled);
        tag.putInt("AirdropIntervalSeconds", airdropIntervalSeconds);
        tag.putInt("AirdropCaptureSeconds", airdropCaptureSeconds);
        tag.putString("AirdropParticle", airdropParticle.toString());

        tag.putInt("PointsToWin", pointsToWin);
        tag.putBoolean("GameActive", gameActive);
        tag.putLong("CountdownEndTick", countdownEndTick);
        tag.putBoolean("MatchFinished", matchFinished);
        if (winningTeam != null) {
            tag.putString("WinningTeam", winningTeam);
        }
        return tag;
    }

    public static CaptureGameData load(CompoundTag tag, HolderLookup.Provider provider) {
        CaptureGameData data = new CaptureGameData();

        ListTag pointList = tag.getList("Points", Tag.TAG_COMPOUND);
        for (Tag entry : pointList) {
            CapturePoint point = CapturePoint.load((CompoundTag) entry);
            data.points.put(point.id(), point);
        }

        CompoundTag scores = tag.getCompound("TeamScores");
        for (String key : scores.getAllKeys()) {
            data.teamScores.put(key, scores.getInt(key));
        }

        if (tag.contains("TeamVisuals")) {
            CompoundTag visuals = tag.getCompound("TeamVisuals");
            for (String key : visuals.getAllKeys()) {
                data.teamVisuals.put(key, TeamVisualSettings.load(visuals.getCompound(key)));
            }
        }
        if (tag.contains("NeutralParticle")) {
            data.neutralParticle = ResourceLocation.parse(tag.getString("NeutralParticle"));
        }
        if (tag.contains("BaseZoneParticle")) {
            data.baseZoneParticle = ResourceLocation.parse(tag.getString("BaseZoneParticle"));
        } else if (tag.contains("RestoreZoneParticle")) {
            data.baseZoneParticle = ResourceLocation.parse(tag.getString("RestoreZoneParticle"));
        }
        if (tag.contains("BaseZoneParticlesEnabled")) {
            data.baseZoneParticlesEnabled = tag.getBoolean("BaseZoneParticlesEnabled");
        } else if (tag.contains("RestoreZoneParticlesEnabled")) {
            data.baseZoneParticlesEnabled = tag.getBoolean("RestoreZoneParticlesEnabled");
        }
        if (tag.contains("GlobalStatRules")) {
            data.globalStatRules.clear();
            data.globalStatRules.mergeOver(MatchStatRules.load(tag.getCompound("GlobalStatRules")));
        }
        if (tag.contains("TeamStatRules")) {
            CompoundTag teamStats = tag.getCompound("TeamStatRules");
            for (String key : teamStats.getAllKeys()) {
                data.teamStatRules.put(key, MatchStatRules.load(teamStats.getCompound(key)));
            }
        }

        if (tag.contains("BaseZones")) {
            ListTag baseList = tag.getList("BaseZones", Tag.TAG_COMPOUND);
            for (Tag entry : baseList) {
                BaseZone zone = BaseZone.load((CompoundTag) entry);
                data.baseZones.put(zone.id(), zone);
            }
        } else if (tag.contains("RestoreZones")) {
            ListTag zoneList = tag.getList("RestoreZones", Tag.TAG_COMPOUND);
            for (Tag entry : zoneList) {
                CompoundTag zoneTag = (CompoundTag) entry;
                BaseZone zone = BaseZone.load(zoneTag);
                data.baseZones.put(zone.id(), zone);
            }
        }

        if (tag.contains("PersonalMoney")) {
            CompoundTag personal = tag.getCompound("PersonalMoney");
            for (String key : personal.getAllKeys()) {
                data.personalMoney.put(UUID.fromString(key), personal.getInt(key));
            }
        }
        if (tag.contains("TeamMoney")) {
            CompoundTag teamCash = tag.getCompound("TeamMoney");
            for (String key : teamCash.getAllKeys()) {
                data.teamMoney.put(key, teamCash.getInt(key));
            }
        }
        if (tag.contains("TeamCaptains")) {
            CompoundTag captains = tag.getCompound("TeamCaptains");
            for (String key : captains.getAllKeys()) {
                data.teamCaptains.put(key, captains.getUUID(key));
            }
        }
        if (tag.contains("PlayerClasses")) {
            CompoundTag classes = tag.getCompound("PlayerClasses");
            for (String key : classes.getAllKeys()) {
                data.playerClasses.put(UUID.fromString(key), classes.getString(key));
            }
        }
        if (tag.contains("ClassChangeCooldowns")) {
            CompoundTag classCooldowns = tag.getCompound("ClassChangeCooldowns");
            for (String key : classCooldowns.getAllKeys()) {
                data.classChangeAvailableAtTick.put(UUID.fromString(key), classCooldowns.getLong(key));
            }
        }
        if (tag.contains("RespawnPenaltyUntil")) {
            CompoundTag respawnPenalties = tag.getCompound("RespawnPenaltyUntil");
            for (String key : respawnPenalties.getAllKeys()) {
                data.respawnPenaltyUntilTick.put(UUID.fromString(key), respawnPenalties.getLong(key));
            }
        }
        if (tag.contains("EconomyRules")) {
            data.economyRules().mergeFrom(EconomyRules.load(tag.getCompound("EconomyRules")));
        }
        if (tag.contains("ShopOffers")) {
            ListTag shopList = tag.getList("ShopOffers", Tag.TAG_COMPOUND);
            for (Tag entry : shopList) {
                ShopOffer offer = ShopOffer.load((CompoundTag) entry, provider);
                data.shopOffers.put(offer.id(), offer);
            }
        }

        if (tag.contains("AirdropSpawnPoints")) {
            ListTag airdropPoints = tag.getList("AirdropSpawnPoints", Tag.TAG_COMPOUND);
            for (Tag entry : airdropPoints) {
                AirdropSpawnPoint point = AirdropSpawnPoint.load((CompoundTag) entry);
                data.airdropSpawnPoints.put(point.id(), point);
            }
        }
        if (tag.contains("AirdropLoot")) {
            ListTag airdropLootList = tag.getList("AirdropLoot", Tag.TAG_COMPOUND);
            for (Tag entry : airdropLootList) {
                AirdropLootEntry lootEntry = AirdropLootEntry.load((CompoundTag) entry, provider);
                data.airdropLoot.put(lootEntry.id(), lootEntry);
            }
        }
        if (tag.contains("AirdropEnabled")) {
            data.airdropEnabled = tag.getBoolean("AirdropEnabled");
        }
        if (tag.contains("AirdropIntervalSeconds")) {
            data.airdropIntervalSeconds = tag.getInt("AirdropIntervalSeconds");
        }
        if (tag.contains("AirdropCaptureSeconds")) {
            data.airdropCaptureSeconds = tag.getInt("AirdropCaptureSeconds");
        }
        if (tag.contains("AirdropParticle")) {
            data.airdropParticle = ResourceLocation.parse(tag.getString("AirdropParticle"));
        }

        if (tag.contains("PointsToWin")) {
            data.pointsToWin = tag.getInt("PointsToWin");
        }
        if (tag.contains("GameActive")) {
            data.gameActive = tag.getBoolean("GameActive");
        }
        if (tag.contains("CountdownEndTick")) {
            data.countdownEndTick = tag.getLong("CountdownEndTick");
        }
        if (tag.contains("MatchFinished")) {
            data.matchFinished = tag.getBoolean("MatchFinished");
        }
        if (tag.contains("WinningTeam")) {
            data.winningTeam = tag.getString("WinningTeam");
        }
        return data;
    }
}
