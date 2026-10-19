package org.sgx.classes.network;

/** Результат мини-игры заточки: в какую зону полосы попал игрок. */
public enum SharpeningZone {
    MISS,     // промах: прочность падает до 1
    BAD,      // бывшая YELLOW: −75% прочности, заточки нет
    MEDIUM,   // бывшая GREEN: −25% прочности, заточки нет
    PERFECT   // бывшая BRIGHT_GREEN: предмет затачивается
}