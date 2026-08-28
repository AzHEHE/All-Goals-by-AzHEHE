package dev.allgoals.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.allgoals.AllGoals;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

final class RunModeSavedData extends SavedData {
    private static final Codec<RunModeSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RunMode.CODEC.optionalFieldOf("mode", RunMode.ALL_GOALS).forGetter(data -> data.mode)
    ).apply(instance, RunModeSavedData::new));
    private static final SavedDataType<RunModeSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(AllGoals.MOD_ID, "run_settings"),
            RunModeSavedData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private RunMode mode;

    private RunModeSavedData() {
        this(RunMode.ALL_GOALS);
    }

    private RunModeSavedData(RunMode mode) {
        this.mode = mode == null ? RunMode.ALL_GOALS : mode;
    }

    static RunModeSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    RunMode mode() {
        return mode;
    }

    void setMode(RunMode mode) {
        RunMode normalized = mode == null ? RunMode.ALL_GOALS : mode;
        if (this.mode == normalized) return;
        this.mode = normalized;
        setDirty();
    }
}
