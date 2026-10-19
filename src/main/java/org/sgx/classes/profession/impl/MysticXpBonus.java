package org.sgx.classes.profession.impl;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionType;

public final class MysticXpBonus {

    private MysticXpBonus() {}

    public static int apply(ServerPlayer player, int base, RandomSource random) {
        ProfessionData data = ModAttachments.get(player);
        if (data.currentProfession() != ProfessionType.MYSTIC) {
            return base;
        }
        double percent = data.customScalarTotal("mystic.xp_bonus_percent");
        if (percent <= 0.0) {
            return base;
        }
        double exact = base * (1.0 + percent / 100.0);
        int whole = (int) exact;
        return whole + (random.nextDouble() < exact - whole ? 1 : 0);
    }
}