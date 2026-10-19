package org.sgx.classes.network;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.sgx.classes.profession.ProfessionColors;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionType;
import org.sgx.classes.profession.SubclassType;

public final class ProfessionTabList {

    private static final String TEAM_PREFIX = "arzcls_";

    private ProfessionTabList() {
    }

    public static void update(ServerPlayer player, ProfessionData data) {
        Scoreboard scoreboard = player.level().getServer().getScoreboard();
        String playerName = player.getScoreboardName();
        String teamName = TEAM_PREFIX + playerName;

        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
        }
        if (!team.getPlayers().contains(playerName)) {
            scoreboard.addPlayerToTeam(playerName, team);
        }

        team.setPlayerSuffix(buildSuffix(data));
    }

    private static Component buildSuffix(ProfessionData data) {
        SubclassType subclass = data.currentSubclass();
        int argb;
        Component labelText;
        int level = data.level(); // общий счётчик теперь один на класс+подкласс

        if (subclass != null) {
            labelText = subclass.displayName();
            argb = ProfessionColors.of(subclass.parent());
        } else {
            ProfessionType profession = data.currentProfession();
            if (profession == ProfessionType.NONE) {
                return Component.empty();
            }
            labelText = profession.displayName();
            argb = ProfessionColors.of(profession);
        }

        Style colored = Style.EMPTY.withColor(TextColor.fromRgb(argb & 0xFFFFFF));
        return Component.literal(" [")
                .append(labelText.copy().withStyle(colored))
                .append(Component.literal(" " + level))
                .append(Component.literal("]"))
                .withStyle(colored);
    }
}