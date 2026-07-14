package com.zov.zovcapture.game;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

public final class MatchStatRules {
    private OptionalDouble maxHealth = OptionalDouble.empty();
    private OptionalDouble movementSpeed = OptionalDouble.empty();
    private OptionalDouble attackDamage = OptionalDouble.empty();
    private OptionalDouble armor = OptionalDouble.empty();
    private OptionalDouble attackSpeed = OptionalDouble.empty();
    private OptionalDouble knockbackResistance = OptionalDouble.empty();

    public OptionalDouble maxHealth() {
        return maxHealth;
    }

    public OptionalDouble movementSpeed() {
        return movementSpeed;
    }

    public OptionalDouble attackDamage() {
        return attackDamage;
    }

    public OptionalDouble armor() {
        return armor;
    }

    public OptionalDouble attackSpeed() {
        return attackSpeed;
    }

    public OptionalDouble knockbackResistance() {
        return knockbackResistance;
    }

    public void setMaxHealth(@Nullable Double value) {
        maxHealth = value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    public void setMovementSpeed(@Nullable Double value) {
        movementSpeed = value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    public void setAttackDamage(@Nullable Double value) {
        attackDamage = value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    public void setArmor(@Nullable Double value) {
        armor = value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    public void setAttackSpeed(@Nullable Double value) {
        attackSpeed = value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    public void setKnockbackResistance(@Nullable Double value) {
        knockbackResistance = value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    public boolean isEmpty() {
        return maxHealth.isEmpty()
                && movementSpeed.isEmpty()
                && attackDamage.isEmpty()
                && armor.isEmpty()
                && attackSpeed.isEmpty()
                && knockbackResistance.isEmpty();
    }

    public void clear() {
        maxHealth = OptionalDouble.empty();
        movementSpeed = OptionalDouble.empty();
        attackDamage = OptionalDouble.empty();
        armor = OptionalDouble.empty();
        attackSpeed = OptionalDouble.empty();
        knockbackResistance = OptionalDouble.empty();
    }

    public MatchStatRules copy() {
        MatchStatRules copy = new MatchStatRules();
        maxHealth.ifPresent(copy::setMaxHealth);
        movementSpeed.ifPresent(copy::setMovementSpeed);
        attackDamage.ifPresent(copy::setAttackDamage);
        armor.ifPresent(copy::setArmor);
        attackSpeed.ifPresent(copy::setAttackSpeed);
        knockbackResistance.ifPresent(copy::setKnockbackResistance);
        return copy;
    }

    public void mergeOver(MatchStatRules overlay) {
        overlay.maxHealth.ifPresent(this::setMaxHealth);
        overlay.movementSpeed.ifPresent(this::setMovementSpeed);
        overlay.attackDamage.ifPresent(this::setAttackDamage);
        overlay.armor.ifPresent(this::setArmor);
        overlay.attackSpeed.ifPresent(this::setAttackSpeed);
        overlay.knockbackResistance.ifPresent(this::setKnockbackResistance);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        maxHealth.ifPresent(value -> tag.putDouble("MaxHealth", value));
        movementSpeed.ifPresent(value -> tag.putDouble("MovementSpeed", value));
        attackDamage.ifPresent(value -> tag.putDouble("AttackDamage", value));
        armor.ifPresent(value -> tag.putDouble("Armor", value));
        attackSpeed.ifPresent(value -> tag.putDouble("AttackSpeed", value));
        knockbackResistance.ifPresent(value -> tag.putDouble("KnockbackResistance", value));
        return tag;
    }

    public static MatchStatRules load(CompoundTag tag) {
        MatchStatRules rules = new MatchStatRules();
        if (tag.contains("MaxHealth")) {
            rules.setMaxHealth(tag.getDouble("MaxHealth"));
        }
        if (tag.contains("MovementSpeed")) {
            rules.setMovementSpeed(tag.getDouble("MovementSpeed"));
        }
        if (tag.contains("AttackDamage")) {
            rules.setAttackDamage(tag.getDouble("AttackDamage"));
        }
        if (tag.contains("Armor")) {
            rules.setArmor(tag.getDouble("Armor"));
        }
        if (tag.contains("AttackSpeed")) {
            rules.setAttackSpeed(tag.getDouble("AttackSpeed"));
        }
        if (tag.contains("KnockbackResistance")) {
            rules.setKnockbackResistance(tag.getDouble("KnockbackResistance"));
        }
        return rules;
    }

    public static Optional<MatchStatRules.StatField> parseField(String raw) {
        return Optional.ofNullable(switch (raw.toLowerCase()) {
            case "maxhealth", "health", "hp" -> StatField.MAX_HEALTH;
            case "speed", "movementspeed", "move_speed" -> StatField.MOVEMENT_SPEED;
            case "damage", "attackdamage", "attack_damage" -> StatField.ATTACK_DAMAGE;
            case "armor", "armour" -> StatField.ARMOR;
            case "attackspeed", "attack_speed" -> StatField.ATTACK_SPEED;
            case "knockback", "knockbackresistance", "knockback_resistance" -> StatField.KNOCKBACK_RESISTANCE;
            default -> null;
        });
    }

    public enum StatField {
        MAX_HEALTH,
        MOVEMENT_SPEED,
        ATTACK_DAMAGE,
        ARMOR,
        ATTACK_SPEED,
        KNOCKBACK_RESISTANCE
    }
}
