package salty5844.collisioneffects.client.config;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Config {

	private static final String FILE_NAME = "collision-effects.json";
	private static final Config INSTANCE = new Config();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Path configDir;
	private static final int MIN_GROUND_PROXIMITY = 1;
	private static final int MAX_GROUND_PROXIMITY = 100;
	private static final int MIN_CENTER_DEADZONE = 0;
	private static final int MAX_CENTER_DEADZONE = 100;
	private static final int MIN_PARTICLE_CAPACITY = 0;
	private static final int MAX_PARTICLE_CAPACITY = 200;
	private static final int MIN_PARTICLE_OPACITY = 0;
	private static final int MAX_PARTICLE_OPACITY = 100;
	private static final int MIN_MOB_HIT_TRACKING_RADIUS = 0;
	private static final int MAX_MOB_HIT_TRACKING_RADIUS = 5;
	private static final int MIN_EXPLOSION_REGISTER_PROXIMITY = 0;
	private static final int MAX_EXPLOSION_REGISTER_PROXIMITY = 10;
	private static final int DEFAULT_CONFIGURATION_HOTKEY = com.mojang.blaze3d.platform.InputConstants.KEY_BACKSLASH;
	private static final double MIN_BIOME_MULTIPLIER = 0.0D;
	private static final double MAX_BIOME_MULTIPLIER = 10.0D;
	private static final int DEFAULT_REGULAR_GROUND_PROXIMITY = 10;
	private static final int DEFAULT_DOUBLE_GROUND_PROXIMITY = 5;
	private static final int DEFAULT_CENTER_DEADZONE = 50;
	private static final int DEFAULT_PARTICLE_CAPACITY = 100;
	private static final int DEFAULT_PARTICLE_OPACITY = 25;
	private static final int DEFAULT_MOB_HIT_TRACKING_RADIUS = 3;
	private static final int DEFAULT_EXPLOSION_REGISTER_PROXIMITY = 5;
	private static final boolean DEFAULT_THIRD_PERSON_EFFECTS = false;
	private static final double DEFAULT_SWAMP_MULTIPLIER = 1.5D;
	private static final double DEFAULT_JUNGLE_MULTIPLIER = 1.5D;

	private final Map<String, Boolean> effectEnabled = new LinkedHashMap<>();
	private int regularGroundProximity = DEFAULT_REGULAR_GROUND_PROXIMITY;
	private int doubleGroundProximity = DEFAULT_DOUBLE_GROUND_PROXIMITY;
	private int centerDeadzone = DEFAULT_CENTER_DEADZONE;
	private int particleCapacity = DEFAULT_PARTICLE_CAPACITY;
	private int particleOpacity = DEFAULT_PARTICLE_OPACITY;
	private int mobHitTrackingRadius = DEFAULT_MOB_HIT_TRACKING_RADIUS;
	private int explosionRegisterProximity = DEFAULT_EXPLOSION_REGISTER_PROXIMITY;
	private int configurationHotkey = DEFAULT_CONFIGURATION_HOTKEY;
	private boolean thirdPersonEffects = DEFAULT_THIRD_PERSON_EFFECTS;
	private double swampMultiplier = DEFAULT_SWAMP_MULTIPLIER;
	private double jungleMultiplier = DEFAULT_JUNGLE_MULTIPLIER;

	private Config() {
		effectEnabled.put("bug_splatter", true);
		effectEnabled.put("lava_splatters", true);
		effectEnabled.put("water_droplets", true);
		effectEnabled.put("rain_droplets", true);
		effectEnabled.put("magma_touch_splatters", true);
		effectEnabled.put("magma_damaged_splatters", true);
		effectEnabled.put("magma_hit_splatters", true);
		effectEnabled.put("slime_touch_splatters", true);
		effectEnabled.put("slime_damaged_splatters", true);
		effectEnabled.put("slime_hit_splatters", true);
		effectEnabled.put("spider_splatters", true);
		effectEnabled.put("llama_spit", true);
		effectEnabled.put("squid_ink", true);
		effectEnabled.put("snowflakes", true);
		effectEnabled.put("snowball_clumps", true);
		effectEnabled.put("bee_pollen", true);
		effectEnabled.put("bee_pollen_hit", true);
		effectEnabled.put("bee_pollen_touch", true);
		effectEnabled.put("chicken_hit_feathers", true);
		effectEnabled.put("chicken_touch_feathers", true);
		effectEnabled.put("damage_tint", true);
		effectEnabled.put("damage_vingette", true);
		effectEnabled.put("health_tint", true);
		effectEnabled.put("explosion_flash", true);
		effectEnabled.put("explosion_debris", true);
		effectEnabled.put("wither_vingette", true);
	}

	public static Config getInstance() {
		return INSTANCE;
	}

	public static void setConfigDir(Path dir) {
		configDir = dir;
	}

	public static Path getConfigDir() {
		return configDir;
	}

	public boolean isEnabled(String key) {
		return effectEnabled.getOrDefault(key, true);
	}

	public void setEnabled(String key, boolean enabled) {
		effectEnabled.put(key, enabled);
	}

	public Map<String, Boolean> getEffects() {
		return effectEnabled;
	}

	public int getRegularGroundProximity() {
		return regularGroundProximity;
	}

	public void setRegularGroundProximity(int value) {
		regularGroundProximity = clampInt(value, MIN_GROUND_PROXIMITY, MAX_GROUND_PROXIMITY);
	}

	public int getDoubleGroundProximity() {
		return doubleGroundProximity;
	}

	public void setDoubleGroundProximity(int value) {
		doubleGroundProximity = clampInt(value, MIN_GROUND_PROXIMITY, MAX_GROUND_PROXIMITY);
	}

	public int getCenterDeadzone() {
		return centerDeadzone;
	}

	public void setCenterDeadzone(int value) {
		centerDeadzone = clampInt(value, MIN_CENTER_DEADZONE, MAX_CENTER_DEADZONE);
	}

	public int getParticleCapacity() {
		return particleCapacity;
	}

	public void setParticleCapacity(int value) {
		particleCapacity = clampInt(value, MIN_PARTICLE_CAPACITY, MAX_PARTICLE_CAPACITY);
	}

	public int getParticleOpacity() {
		return particleOpacity;
	}

	public void setParticleOpacity(int value) {
		particleOpacity = clampInt(value, MIN_PARTICLE_OPACITY, MAX_PARTICLE_OPACITY);
	}

	public boolean isThirdPersonEffectsEnabled() {
		return thirdPersonEffects;
	}

	public int getMobHitTrackingRadius() {
		return mobHitTrackingRadius;
	}

	public void setMobHitTrackingRadius(int value) {
		mobHitTrackingRadius = clampInt(value, MIN_MOB_HIT_TRACKING_RADIUS, MAX_MOB_HIT_TRACKING_RADIUS);
	}

	public int getExplosionRegisterProximity() {
		return explosionRegisterProximity;
	}

	public void setExplosionRegisterProximity(int value) {
		explosionRegisterProximity = clampInt(value, MIN_EXPLOSION_REGISTER_PROXIMITY, MAX_EXPLOSION_REGISTER_PROXIMITY);
	}

	public int getConfigurationHotkey() {
		return configurationHotkey;
	}

	public void setConfigurationHotkey(int keyCode) {
		if (keyCode >= 0) {
			configurationHotkey = keyCode;
		}
	}

	public void setThirdPersonEffectsEnabled(boolean enabled) {
		thirdPersonEffects = enabled;
	}

	public double getSwampMultiplier() {
		return swampMultiplier;
	}

	public void setSwampMultiplier(double value) {
		swampMultiplier = clampDouble(value, MIN_BIOME_MULTIPLIER, MAX_BIOME_MULTIPLIER);
	}

	public double getJungleMultiplier() {
		return jungleMultiplier;
	}

	public void setJungleMultiplier(double value) {
		jungleMultiplier = clampDouble(value, MIN_BIOME_MULTIPLIER, MAX_BIOME_MULTIPLIER);
	}

	public void resetBugSplatterDefaults() {
		effectEnabled.put("bug_splatter", true);
		regularGroundProximity = DEFAULT_REGULAR_GROUND_PROXIMITY;
		doubleGroundProximity = DEFAULT_DOUBLE_GROUND_PROXIMITY;
		swampMultiplier = DEFAULT_SWAMP_MULTIPLIER;
		jungleMultiplier = DEFAULT_JUNGLE_MULTIPLIER;
	}

	public void resetWaterRainDefaults() {
		effectEnabled.put("lava_splatters", true);
		effectEnabled.put("water_droplets", true);
		effectEnabled.put("rain_droplets", true);
	}

	public void resetLavaDefaults() {
		effectEnabled.put("lava_splatters", true);
	}

	public void resetMagmaDefaults() {
		effectEnabled.put("magma_touch_splatters", true);
		effectEnabled.put("magma_damaged_splatters", true);
		effectEnabled.put("magma_hit_splatters", true);
	}

	public void resetSlimeDefaults() {
		effectEnabled.put("slime_touch_splatters", true);
		effectEnabled.put("slime_damaged_splatters", true);
		effectEnabled.put("slime_hit_splatters", true);
	}

	public void resetSpiderDefaults() {
		effectEnabled.put("spider_splatters", true);
	}

	public void resetLlamaSpitDefaults() {
		effectEnabled.put("llama_spit", true);
	}

	public void resetSquidInkDefaults() {
		effectEnabled.put("squid_ink", true);
	}

	public void resetSnowDefaults() {
		effectEnabled.put("snowflakes", true);
		effectEnabled.put("snowball_clumps", true);
	}

	public void resetBeePollenDefaults() {
		effectEnabled.put("bee_pollen", true);
		effectEnabled.put("bee_pollen_hit", true);
		effectEnabled.put("bee_pollen_touch", true);
	}

	public void resetChickenFeathersDefaults() {
		effectEnabled.put("chicken_hit_feathers", true);
		effectEnabled.put("chicken_touch_feathers", true);
	}

	public void resetDamageTintDefaults() {
		effectEnabled.put("damage_tint", true);
	}

	public void resetDamageVingetteDefaults() {
		effectEnabled.put("damage_vingette", true);
	}

	public void resetHealthTintDefaults() {
		effectEnabled.put("health_tint", true);
	}

	public void resetExplosionFlashDefaults() {
		effectEnabled.put("explosion_flash", true);
		effectEnabled.put("explosion_debris", true);
		explosionRegisterProximity = DEFAULT_EXPLOSION_REGISTER_PROXIMITY;
	}

	public void resetWitherVingetteDefaults() {
		effectEnabled.put("wither_vingette", true);
	}

	public void resetThirdPersonDefaults() {
		thirdPersonEffects = DEFAULT_THIRD_PERSON_EFFECTS;
	}

	public void resetCenterDeadzoneDefaults() {
		centerDeadzone = DEFAULT_CENTER_DEADZONE;
	}

	public void resetParticleCapacityDefaults() {
		particleCapacity = DEFAULT_PARTICLE_CAPACITY;
	}

	public void resetParticleOpacityDefaults() {
		particleOpacity = DEFAULT_PARTICLE_OPACITY;
	}

	public void resetMobHitTrackingRadiusDefaults() {
		mobHitTrackingRadius = DEFAULT_MOB_HIT_TRACKING_RADIUS;
	}

	public void resetConfigurationHotkeyDefaults() {
		configurationHotkey = DEFAULT_CONFIGURATION_HOTKEY;
	}

	public int getDefaultConfigurationHotkey() {
		return DEFAULT_CONFIGURATION_HOTKEY;
	}

	public void resetToDefaults() {
		effectEnabled.clear();
		resetBugSplatterDefaults();
		resetWaterRainDefaults();
		resetMagmaDefaults();
		resetSlimeDefaults();
		resetSpiderDefaults();
		resetLlamaSpitDefaults();
		resetSquidInkDefaults();
		resetSnowDefaults();
		resetBeePollenDefaults();
		resetChickenFeathersDefaults();
		resetDamageTintDefaults();
		resetDamageVingetteDefaults();
		resetHealthTintDefaults();
		resetExplosionFlashDefaults();
		resetWitherVingetteDefaults();
		resetThirdPersonDefaults();
		resetCenterDeadzoneDefaults();
		resetParticleCapacityDefaults();
		resetParticleOpacityDefaults();
		resetMobHitTrackingRadiusDefaults();
		resetConfigurationHotkeyDefaults();
	}

	public void load(Path configDirectory) {
		Path file = configDirectory.resolve(FILE_NAME);
		if (!Files.exists(file)) {
			return;
		}

		try {
			String content = Files.readString(file, StandardCharsets.UTF_8).trim();
			if (content.isEmpty()) {
				return;
			}
			if (content.startsWith("{")) {
				loadFromJson(content);
			} else {
				loadLegacy(content);
			}
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to load Collision Effects config", exception);
		}
	}

	public void save(Path configDirectory) {
		Path file = configDirectory.resolve(FILE_NAME);
		try {
			Files.createDirectories(configDirectory);
			JsonObject root = new JsonObject();
			root.addProperty("bug_splatter", isEnabled("bug_splatter"));
			root.addProperty("regular_ground_proximity", regularGroundProximity);
			root.addProperty("double_ground_proximity", doubleGroundProximity);
			root.addProperty("center_deadzone", centerDeadzone);
			root.addProperty("particle_capacity", particleCapacity);
			root.addProperty("particle_opacity", particleOpacity);
			root.addProperty("mob_hit_tracking_radius", mobHitTrackingRadius);
			root.addProperty("explosion_register_proximity", explosionRegisterProximity);
			root.addProperty("configuration_hotkey", configurationHotkey);
			root.addProperty("third_person_effects", thirdPersonEffects);
			root.addProperty("swamp_multiplier", swampMultiplier);
			root.addProperty("jungle_multiplier", jungleMultiplier);
			root.addProperty("lava_splatters", isEnabled("lava_splatters"));
			root.addProperty("water_droplets", isEnabled("water_droplets"));
			root.addProperty("rain_droplets", isEnabled("rain_droplets"));
			root.addProperty("magma_touch_splatters", isEnabled("magma_touch_splatters"));
			root.addProperty("magma_damaged_splatters", isEnabled("magma_damaged_splatters"));
			root.addProperty("magma_hit_splatters", isEnabled("magma_hit_splatters"));
			root.addProperty("slime_touch_splatters", isEnabled("slime_touch_splatters"));
			root.addProperty("slime_damaged_splatters", isEnabled("slime_damaged_splatters"));
			root.addProperty("slime_hit_splatters", isEnabled("slime_hit_splatters"));
			root.addProperty("spider_splatters", isEnabled("spider_splatters"));
			root.addProperty("llama_spit", isEnabled("llama_spit"));
			root.addProperty("squid_ink", isEnabled("squid_ink"));
			root.addProperty("snowflakes", isEnabled("snowflakes"));
			root.addProperty("snowball_clumps", isEnabled("snowball_clumps"));
			root.addProperty("bee_pollen", isEnabled("bee_pollen"));
			root.addProperty("bee_pollen_hit", isEnabled("bee_pollen_hit"));
			root.addProperty("bee_pollen_touch", isEnabled("bee_pollen_touch"));
			root.addProperty("chicken_hit_feathers", isEnabled("chicken_hit_feathers"));
			root.addProperty("chicken_touch_feathers", isEnabled("chicken_touch_feathers"));
			root.addProperty("damage_tint", isEnabled("damage_tint"));
			root.addProperty("damage_vingette", isEnabled("damage_vingette"));
			root.addProperty("health_tint", isEnabled("health_tint"));
			root.addProperty("explosion_flash", isEnabled("explosion_flash"));
			root.addProperty("explosion_debris", isEnabled("explosion_debris"));
			root.addProperty("wither_vingette", isEnabled("wither_vingette"));
			Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to save Collision Effects config", exception);
		}
	}

	private void loadFromJson(String content) {
		try {
			JsonElement element = JsonParser.parseString(content);
			if (!element.isJsonObject()) {
				return;
			}
			JsonObject root = element.getAsJsonObject();
			if (root.has("bug_splatter")) {
				setEnabled("bug_splatter", root.get("bug_splatter").getAsBoolean());
			}
			if (root.has("lava_splatters")) {
				setEnabled("lava_splatters", root.get("lava_splatters").getAsBoolean());
			}
			if (root.has("water_droplets")) {
				setEnabled("water_droplets", root.get("water_droplets").getAsBoolean());
			}
			if (root.has("rain_droplets")) {
				setEnabled("rain_droplets", root.get("rain_droplets").getAsBoolean());
			}
			if (root.has("magma_touch_splatters")) {
				setEnabled("magma_touch_splatters", root.get("magma_touch_splatters").getAsBoolean());
			}
			if (root.has("magma_damaged_splatters")) {
				setEnabled("magma_damaged_splatters", root.get("magma_damaged_splatters").getAsBoolean());
			} else if (root.has("magma_hit_splatters")) {
				// Migrate legacy single magma hit toggle into the new damaged toggle.
				setEnabled("magma_damaged_splatters", root.get("magma_hit_splatters").getAsBoolean());
			}
			if (root.has("magma_hit_splatters")) {
				setEnabled("magma_hit_splatters", root.get("magma_hit_splatters").getAsBoolean());
			}
			if (root.has("slime_touch_splatters")) {
				setEnabled("slime_touch_splatters", root.get("slime_touch_splatters").getAsBoolean());
			}
			if (root.has("slime_damaged_splatters")) {
				setEnabled("slime_damaged_splatters", root.get("slime_damaged_splatters").getAsBoolean());
			} else if (root.has("slime_hit_splatters")) {
				// Migrate legacy single slime hit toggle into the new damaged toggle.
				setEnabled("slime_damaged_splatters", root.get("slime_hit_splatters").getAsBoolean());
			}
			if (root.has("slime_hit_splatters")) {
				setEnabled("slime_hit_splatters", root.get("slime_hit_splatters").getAsBoolean());
			}
			if (root.has("spider_splatters")) {
				setEnabled("spider_splatters", root.get("spider_splatters").getAsBoolean());
			}
			if (root.has("llama_spit")) {
				setEnabled("llama_spit", root.get("llama_spit").getAsBoolean());
			}
			if (root.has("squid_ink")) {
				setEnabled("squid_ink", root.get("squid_ink").getAsBoolean());
			}
			if (root.has("snowflakes")) {
				setEnabled("snowflakes", root.get("snowflakes").getAsBoolean());
			}
			if (root.has("snowball_clumps")) {
				setEnabled("snowball_clumps", root.get("snowball_clumps").getAsBoolean());
			}
			if (root.has("bee_pollen")) {
				setEnabled("bee_pollen", root.get("bee_pollen").getAsBoolean());
			}
			if (root.has("bee_pollen_hit")) {
				setEnabled("bee_pollen_hit", root.get("bee_pollen_hit").getAsBoolean());
			} else if (root.has("bee_pollen")) {
				// Migrate legacy single bee pollen hit setting into the explicit hit toggle.
				setEnabled("bee_pollen_hit", root.get("bee_pollen").getAsBoolean());
			}
			if (root.has("bee_pollen_touch")) {
				setEnabled("bee_pollen_touch", root.get("bee_pollen_touch").getAsBoolean());
			}
			if (root.has("chicken_hit_feathers")) {
				setEnabled("chicken_hit_feathers", root.get("chicken_hit_feathers").getAsBoolean());
			}
			if (root.has("chicken_touch_feathers")) {
				setEnabled("chicken_touch_feathers", root.get("chicken_touch_feathers").getAsBoolean());
			}

			// Migrate legacy single chicken toggle into both split toggles.
			if (root.has("chicken_feathers")) {
				boolean legacyChickenFeathers = root.get("chicken_feathers").getAsBoolean();
				if (!root.has("chicken_hit_feathers")) {
					setEnabled("chicken_hit_feathers", legacyChickenFeathers);
				}
				if (!root.has("chicken_touch_feathers")) {
					setEnabled("chicken_touch_feathers", legacyChickenFeathers);
				}
			}
			if (root.has("damage_tint")) {
				setEnabled("damage_tint", root.get("damage_tint").getAsBoolean());
			}
			if (root.has("damage_vingette")) {
				setEnabled("damage_vingette", root.get("damage_vingette").getAsBoolean());
			}
			if (root.has("health_tint")) {
				setEnabled("health_tint", root.get("health_tint").getAsBoolean());
			}
			if (root.has("explosion_flash")) {
				setEnabled("explosion_flash", root.get("explosion_flash").getAsBoolean());
			}
			if (root.has("explosion_debris")) {
				setEnabled("explosion_debris", root.get("explosion_debris").getAsBoolean());
			}
			if (root.has("wither_vingette")) {
				setEnabled("wither_vingette", root.get("wither_vingette").getAsBoolean());
			}
			if (root.has("regular_ground_proximity")) {
				setRegularGroundProximity(root.get("regular_ground_proximity").getAsInt());
			}
			if (root.has("double_ground_proximity")) {
				setDoubleGroundProximity(root.get("double_ground_proximity").getAsInt());
			}
			if (root.has("center_deadzone")) {
				setCenterDeadzone(root.get("center_deadzone").getAsInt());
			}
			if (root.has("particle_capacity")) {
				setParticleCapacity(root.get("particle_capacity").getAsInt());
			}
			if (root.has("particle_opacity")) {
				setParticleOpacity(root.get("particle_opacity").getAsInt());
			}
			if (root.has("mob_hit_tracking_radius")) {
				setMobHitTrackingRadius(root.get("mob_hit_tracking_radius").getAsInt());
			}
			if (root.has("explosion_register_proximity")) {
				setExplosionRegisterProximity(root.get("explosion_register_proximity").getAsInt());
			}
			if (root.has("configuration_hotkey")) {
				setConfigurationHotkey(root.get("configuration_hotkey").getAsInt());
			}
			if (root.has("third_person_effects")) {
				setThirdPersonEffectsEnabled(root.get("third_person_effects").getAsBoolean());
			}
			if (root.has("swamp_multiplier")) {
				setSwampMultiplier(root.get("swamp_multiplier").getAsDouble());
			}
			if (root.has("jungle_multiplier")) {
				setJungleMultiplier(root.get("jungle_multiplier").getAsDouble());
			}

			// Migrate legacy per-effect third-person flags into the single global toggle.
			if (legacyThirdPersonFlagEnabled(root)) {
				setThirdPersonEffectsEnabled(true);
			}
		} catch (ClassCastException | IllegalStateException | UnsupportedOperationException | JsonParseException ignored) {
		}
	}

	private void loadLegacy(String content) {
		for (String line : content.split("\\R")) {
			String trimmed = line.trim();
			if (trimmed.isEmpty() || trimmed.startsWith("#")) {
				continue;
			}
			int separator = trimmed.indexOf('=');
			if (separator <= 0) {
				continue;
			}
			String key = trimmed.substring(0, separator).trim();
			String value = trimmed.substring(separator + 1).trim();
			switch (key) {
				case "bug_splatter", "lava_splatters", "water_droplets", "rain_droplets", "magma_touch_splatters", "magma_damaged_splatters", "magma_hit_splatters", "slime_touch_splatters", "slime_damaged_splatters", "slime_hit_splatters", "spider_splatters", "llama_spit", "squid_ink", "snowflakes", "snowball_clumps", "bee_pollen", "bee_pollen_hit", "bee_pollen_touch", "chicken_hit_feathers", "chicken_touch_feathers", "damage_tint", "damage_vingette", "health_tint", "explosion_flash", "explosion_debris", "wither_vingette" -> effectEnabled.put(key, Boolean.parseBoolean(value));
				case "chicken_feathers" -> {
					boolean legacyChickenFeathers = Boolean.parseBoolean(value);
					effectEnabled.put("chicken_hit_feathers", legacyChickenFeathers);
					effectEnabled.put("chicken_touch_feathers", legacyChickenFeathers);
				}
				case "regular_ground_proximity" -> setRegularGroundProximity(parseIntOrDefault(value, regularGroundProximity));
				case "double_ground_proximity" -> setDoubleGroundProximity(parseIntOrDefault(value, doubleGroundProximity));
				case "center_deadzone" -> setCenterDeadzone(parseIntOrDefault(value, centerDeadzone));
				case "particle_capacity" -> setParticleCapacity(parseIntOrDefault(value, particleCapacity));
				case "particle_opacity" -> setParticleOpacity(parseIntOrDefault(value, particleOpacity));
				case "mob_hit_tracking_radius" -> setMobHitTrackingRadius(parseIntOrDefault(value, mobHitTrackingRadius));
				case "explosion_register_proximity" -> setExplosionRegisterProximity(parseIntOrDefault(value, explosionRegisterProximity));
				case "configuration_hotkey" -> setConfigurationHotkey(parseIntOrDefault(value, configurationHotkey));
				case "third_person_effects" -> setThirdPersonEffectsEnabled(Boolean.parseBoolean(value));
				case "swamp_multiplier" -> setSwampMultiplier(parseDoubleOrDefault(value, swampMultiplier));
				case "jungle_multiplier" -> setJungleMultiplier(parseDoubleOrDefault(value, jungleMultiplier));
				case "third_person_splats", "droplets_third_person", "slime_third_person", "spider_third_person", "llama_spit_third_person", "squid_ink_third_person", "snow_clumps_third_person", "water_third_person_splats", "rain_third_person_splats" -> {
					if (Boolean.parseBoolean(value)) {
						setThirdPersonEffectsEnabled(true);
					}
				}
				default -> {
				}
			}
		}
	}

	private boolean legacyThirdPersonFlagEnabled(JsonObject root) {
		return (root.has("third_person_splats") && root.get("third_person_splats").getAsBoolean())
			|| (root.has("droplets_third_person") && root.get("droplets_third_person").getAsBoolean())
			|| (root.has("slime_third_person") && root.get("slime_third_person").getAsBoolean())
			|| (root.has("spider_third_person") && root.get("spider_third_person").getAsBoolean())
			|| (root.has("llama_spit_third_person") && root.get("llama_spit_third_person").getAsBoolean())
			|| (root.has("squid_ink_third_person") && root.get("squid_ink_third_person").getAsBoolean())
			|| (root.has("snow_clumps_third_person") && root.get("snow_clumps_third_person").getAsBoolean())
			|| (root.has("water_third_person_splats") && root.get("water_third_person_splats").getAsBoolean())
			|| (root.has("rain_third_person_splats") && root.get("rain_third_person_splats").getAsBoolean());
	}

	private static int parseIntOrDefault(String raw, int fallback) {
		try {
			return Integer.parseInt(raw);
		} catch (NumberFormatException ignored) {
			return fallback;
		}
	}

	private static double parseDoubleOrDefault(String raw, double fallback) {
		try {
			return Double.parseDouble(raw);
		} catch (NumberFormatException ignored) {
			return fallback;
		}
	}

	private static int clampInt(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static double clampDouble(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
