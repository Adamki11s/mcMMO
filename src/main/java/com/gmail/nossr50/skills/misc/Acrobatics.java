package com.gmail.nossr50.skills.misc;

import java.util.Random;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import com.gmail.nossr50.datatypes.PlayerProfile;
import com.gmail.nossr50.datatypes.SkillType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.party.Party;
import com.gmail.nossr50.util.Misc;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.Skills;
import com.gmail.nossr50.util.Users;

public class Acrobatics {
    private static Random random = new Random();

    /**
     * Check for fall damage reduction.
     *
     * @param player The player whose fall damage to modify
     * @param event The event to check
     */
    public static void acrobaticsCheck(Player player, EntityDamageEvent event) {
        final int ROLL_XP_MODIFIER = 80;
        final int FALL_XP_MODIFIER = 120;
        final int MAX_BONUS_LEVEL = 1000;

        PlayerProfile PP = Users.getProfile(player);
        int acrovar = PP.getSkillLevel(SkillType.ACROBATICS);
        boolean gracefulRoll = player.isSneaking();
        int damage = event.getDamage();
        int health = player.getHealth();

        if (!Permissions.getInstance().gracefulRoll(player)) {
            gracefulRoll = false;
        }

        if (gracefulRoll) {
            acrovar = acrovar * 2;
        }

        if ((acrovar > MAX_BONUS_LEVEL || random.nextInt(1000) <= acrovar) && Permissions.getInstance().roll(player)) {
            int threshold = 7;

            if (gracefulRoll) {
                threshold = threshold * 2;
            }

            int newDamage = damage - threshold;

            if (newDamage < 0) {
                newDamage = 0;
            }

            /* Check for death */
            if (health - damage >= 1) {
                Skills.xpProcessing(player, PP, SkillType.ACROBATICS, damage * ROLL_XP_MODIFIER);

                event.setDamage(newDamage);

                if (event.getDamage() <= 0) {
                    event.setCancelled(true);
                }

                if (gracefulRoll) {
                    player.sendMessage(LocaleLoader.getString("Acrobatics.Ability.Proc"));
                }
                else {
                    player.sendMessage(LocaleLoader.getString("Acrobatics.Roll.Text"));
                }
            }
        }
        else if (health - damage >= 1) {
            Skills.xpProcessing(player, PP, SkillType.ACROBATICS, event.getDamage() * FALL_XP_MODIFIER);
        }
    }

    /**
     * Check for dodge damage reduction.
     *
     * @param event The event to check
     */
    public static void dodgeChecks(EntityDamageByEntityEvent event) {
        final int DODGE_MODIFIER = 120;
        final int MAX_BONUS_LEVEL = 800;

        Player defender = (Player) event.getEntity();
        PlayerProfile PPd = Users.getProfile(defender);
        int damage = event.getDamage();

        /* PARTY CHECK */
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();

            if (Party.getInstance().inSameParty(defender, attacker)) {
                return;
            }
        }

        if (Permissions.getInstance().acrobatics(defender)) {
            int skillLevel = PPd.getSkillLevel(SkillType.ACROBATICS);
            int skillCheck = Misc.skillCheck(skillLevel, MAX_BONUS_LEVEL);

            if (random.nextInt(4000) <= skillCheck && Permissions.getInstance().dodge(defender)) {
                defender.sendMessage(LocaleLoader.getString("Acrobatics.Combat.Proc"));

                if (System.currentTimeMillis() >= (5000 + PPd.getRespawnATS()) && defender.getHealth() >= 1) {
                    Skills.xpProcessing(defender, PPd, SkillType.ACROBATICS, damage * DODGE_MODIFIER);
                }

                int newDamage = damage / 2;

                if (newDamage <= 0) {
                    event.setDamage(1);
                }
                else {
                    event.setDamage(newDamage);
                }
            }
        }
    }
}
