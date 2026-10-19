package org.sgx.classes.profession.impl;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.sgx.classes.Arz_classes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PlacedLogTracker extends SavedData {

    private static final Codec<Set<Long>> POSITIONS_CODEC =
            Codec.LONG.listOf().xmap(HashSet::new, List::copyOf);

    public static final SavedDataType<PlacedLogTracker> TYPE = new SavedDataType<>(
            Arz_classes.id("placed_log_tracker"),
            PlacedLogTracker::new,
            POSITIONS_CODEC.xmap(PlacedLogTracker::new, t -> t.positions),
            null
    );

    private final Set<Long> positions;

    private PlacedLogTracker() {
        this(new HashSet<>());
    }

    private PlacedLogTracker(Set<Long> positions) {
        this.positions = positions;
    }

    public static PlacedLogTracker get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void markPlaced(BlockPos pos) {
        positions.add(pos.asLong());
        setDirty();
    }

    public boolean isPlaced(BlockPos pos) {
        return positions.contains(pos.asLong());
    }

    public void unmark(BlockPos pos) {
        if (positions.remove(pos.asLong())) {
            setDirty();
        }
    }
}