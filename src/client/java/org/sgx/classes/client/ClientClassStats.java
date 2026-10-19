package org.sgx.classes.client;

import org.sgx.classes.profession.ProfessionType;

import java.util.List;

/** Последние числа по классам, присланные сервером (ClassStatsPayload). */
public final class ClientClassStats {

    private static int[] counts = new int[0];

    private ClientClassStats() {
    }

    public static void set(List<Integer> list) {
        int[] fresh = new int[list.size()];
        for (int i = 0; i < fresh.length; i++) {
            fresh[i] = list.get(i);
        }
        counts = fresh;
    }

    public static int count(ProfessionType type) {
        int index = type.ordinal();
        return index < counts.length ? counts[index] : 0;
    }

    /** Всего игроков, выбравших какой-либо класс. */
    public static int total() {
        int sum = 0;
        for (int i = 0; i < counts.length; i++) {
            if (i != ProfessionType.NONE.ordinal()) {
                sum += counts[i];
            }
        }
        return sum;
    }

    /** Доля класса среди всех выбравших, в процентах (0..100). */
    public static int percent(ProfessionType type) {
        int total = total();
        return total == 0 ? 0 : Math.round(100f * count(type) / total);
    }
}