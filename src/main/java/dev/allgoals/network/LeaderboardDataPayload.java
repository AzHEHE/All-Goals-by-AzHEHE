package dev.allgoals.network;

import dev.allgoals.AllGoals;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record LeaderboardDataPayload(
        List<Entry> entries,
        int totalGoals
) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 1024;
    private static final int MAX_NAME_LENGTH = 64;

    public static final Type<LeaderboardDataPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AllGoals.MOD_ID, "leaderboard_data")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, LeaderboardDataPayload> STREAM_CODEC =
            StreamCodec.of(LeaderboardDataPayload::write, LeaderboardDataPayload::read);

    public LeaderboardDataPayload {
        entries = List.copyOf(entries);
        totalGoals = Math.max(0, totalGoals);
    }

    @Override
    public Type<LeaderboardDataPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, LeaderboardDataPayload payload) {
        buffer.writeVarInt(payload.entries.size());
        for (Entry entry : payload.entries) {
            buffer.writeUUID(entry.headOwner());
            buffer.writeUtf(entry.name(), MAX_NAME_LENGTH);
            buffer.writeBoolean(entry.party());
            buffer.writeVarInt(entry.completed());
            buffer.writeVarInt(entry.onlineMembers());
            buffer.writeVarInt(entry.totalMembers());
        }
        buffer.writeVarInt(payload.totalGoals);
    }

    private static LeaderboardDataPayload read(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) {
            throw new DecoderException("Invalid All Goals leaderboard size: " + count);
        }
        List<Entry> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            entries.add(new Entry(
                    buffer.readUUID(),
                    buffer.readUtf(MAX_NAME_LENGTH),
                    buffer.readBoolean(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt()
            ));
        }
        return new LeaderboardDataPayload(entries, buffer.readVarInt());
    }

    public record Entry(
            UUID headOwner,
            String name,
            boolean party,
            int completed,
            int onlineMembers,
            int totalMembers
    ) {
        public Entry {
            name = name == null || name.isBlank() ? "Unknown" : name;
            completed = Math.max(0, completed);
            onlineMembers = Math.max(1, onlineMembers);
            totalMembers = Math.max(onlineMembers, totalMembers);
        }
    }
}
