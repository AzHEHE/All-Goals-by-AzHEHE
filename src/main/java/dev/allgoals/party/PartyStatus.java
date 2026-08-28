package dev.allgoals.party;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record PartyStatus(
        boolean inParty,
        String name,
        int memberCount,
        int onlineCount,
        long elapsedMillis,
        boolean timerRunning
) {
    public static final Codec<PartyStatus> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("in_party").forGetter(PartyStatus::inParty),
            Codec.STRING.fieldOf("name").forGetter(PartyStatus::name),
            Codec.INT.fieldOf("members").forGetter(PartyStatus::memberCount),
            Codec.INT.fieldOf("online").forGetter(PartyStatus::onlineCount),
            Codec.LONG.fieldOf("elapsed_ms").forGetter(PartyStatus::elapsedMillis),
            Codec.BOOL.fieldOf("timer_running").forGetter(PartyStatus::timerRunning)
    ).apply(instance, PartyStatus::new));

    public PartyStatus {
        name = name == null ? "" : name;
        memberCount = Math.max(0, memberCount);
        onlineCount = Math.max(0, onlineCount);
        elapsedMillis = Math.max(0L, elapsedMillis);
    }

    public static PartyStatus solo() {
        return new PartyStatus(false, "", 0, 0, 0L, false);
    }
}
