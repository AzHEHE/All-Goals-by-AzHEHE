package dev.allgoals.party;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.allgoals.AllGoals;
import dev.allgoals.progress.PlayerGoalProgress;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class PartySavedData extends SavedData {
    private static final Codec<Set<UUID>> UUID_SET_CODEC = UUIDUtil.STRING_CODEC.listOf().xmap(
            LinkedHashSet::new,
            ArrayList::new
    );

    static final Codec<Party> PARTY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(Party::id),
            Codec.STRING.fieldOf("name").forGetter(Party::name),
            UUIDUtil.STRING_CODEC.fieldOf("owner").forGetter(Party::owner),
            UUIDUtil.STRING_CODEC.optionalFieldOf("creator")
                    .forGetter(party -> Optional.of(party.creator())),
            UUID_SET_CODEC.fieldOf("members").forGetter(Party::members),
            UUID_SET_CODEC.optionalFieldOf("invites", Set.of()).forGetter(Party::invites),
            PlayerGoalProgress.CODEC.optionalFieldOf("progress", PlayerGoalProgress.empty())
                    .forGetter(Party::progress),
            Codec.LONG.optionalFieldOf("elapsed_ms", 0L).forGetter(Party::elapsedMillis)
    ).apply(instance, (id, name, owner, creator, members, invites, progress, elapsedMillis) ->
            new Party(id, name, owner, creator.orElse(owner), members, invites, progress, elapsedMillis)));

    static final Codec<PartySavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PARTY_CODEC.listOf().optionalFieldOf("parties", List.of())
                    .forGetter(data -> List.copyOf(data.parties.values())),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, PlayerGoalProgress.CODEC)
                    .optionalFieldOf("pending_snapshots", Map.of())
                    .forGetter(data -> Map.copyOf(data.pendingSnapshots))
    ).apply(instance, PartySavedData::new));

    static final SavedDataType<PartySavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(AllGoals.MOD_ID, "parties"),
            PartySavedData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final Map<UUID, Party> parties = new LinkedHashMap<>();
    private final Map<UUID, UUID> partyByMember = new LinkedHashMap<>();
    private final Map<UUID, UUID> partyByOwner = new LinkedHashMap<>();
    private final Map<UUID, PlayerGoalProgress> pendingSnapshots = new LinkedHashMap<>();

    PartySavedData() {
    }

    private PartySavedData(List<Party> decodedParties,
                           Map<UUID, PlayerGoalProgress> decodedSnapshots) {
        for (Party party : decodedParties) {
            if (parties.containsKey(party.id()) || hasMembershipConflict(party)) {
                AllGoals.LOGGER.warn("Ignored invalid duplicate All Goals party {} while loading saved data",
                        party.id());
                continue;
            }
            parties.put(party.id(), party);
            index(party);
        }
        pendingSnapshots.putAll(decodedSnapshots);
    }

    Collection<Party> parties() {
        return List.copyOf(parties.values());
    }

    Optional<Party> party(UUID id) {
        return Optional.ofNullable(parties.get(id));
    }

    Optional<Party> partyForMember(UUID playerId) {
        UUID partyId = partyByMember.get(playerId);
        return partyId == null ? Optional.empty() : Optional.ofNullable(parties.get(partyId));
    }

    Optional<Party> partyOwnedBy(UUID playerId) {
        UUID partyId = partyByOwner.get(playerId);
        return partyId == null ? Optional.empty() : Optional.ofNullable(parties.get(partyId));
    }

    void put(Party party) {
        Party previous = parties.get(party.id());
        if (previous != null) unindex(previous);
        if (hasMembershipConflict(party)) {
            if (previous != null) index(previous);
            throw new IllegalStateException("Player cannot belong to multiple All Goals parties");
        }
        parties.put(party.id(), party);
        index(party);
        setDirty();
    }

    void remove(UUID partyId) {
        Party removed = parties.remove(partyId);
        if (removed != null) {
            unindex(removed);
            setDirty();
        }
    }

    void putPendingSnapshot(UUID playerId, PlayerGoalProgress progress) {
        pendingSnapshots.put(playerId, progress);
        setDirty();
    }

    Optional<PlayerGoalProgress> takePendingSnapshot(UUID playerId) {
        PlayerGoalProgress progress = pendingSnapshots.remove(playerId);
        if (progress != null) setDirty();
        return Optional.ofNullable(progress);
    }

    private boolean hasMembershipConflict(Party party) {
        for (UUID memberId : party.members()) {
            UUID existingParty = partyByMember.get(memberId);
            if (existingParty != null && !existingParty.equals(party.id())) return true;
        }
        UUID existingOwnedParty = partyByOwner.get(party.owner());
        return existingOwnedParty != null && !existingOwnedParty.equals(party.id());
    }

    private void index(Party party) {
        party.members().forEach(memberId -> partyByMember.put(memberId, party.id()));
        partyByOwner.put(party.owner(), party.id());
    }

    private void unindex(Party party) {
        party.members().forEach(memberId -> partyByMember.remove(memberId, party.id()));
        partyByOwner.remove(party.owner(), party.id());
    }

    record Party(
            UUID id,
            String name,
            UUID owner,
            UUID creator,
            Set<UUID> members,
            Set<UUID> invites,
            PlayerGoalProgress progress,
            long elapsedMillis
    ) {
        Party {
            name = name == null || name.isBlank() ? "All Goals Party" : name;
            if (name.length() > 64) name = name.substring(0, 64);
            LinkedHashSet<UUID> normalizedMembers = new LinkedHashSet<>(members);
            normalizedMembers.add(owner);
            creator = creator == null ? owner : creator;
            members = Set.copyOf(normalizedMembers);
            LinkedHashSet<UUID> normalizedInvites = new LinkedHashSet<>(invites);
            normalizedInvites.removeAll(normalizedMembers);
            invites = Set.copyOf(normalizedInvites);
            progress = progress == null ? PlayerGoalProgress.empty() : progress;
            elapsedMillis = Math.max(0L, elapsedMillis);
        }

        Party withProgress(PlayerGoalProgress nextProgress) {
            return new Party(id, name, owner, creator, members, invites, nextProgress, elapsedMillis);
        }

        Party withElapsedMillis(long nextElapsedMillis) {
            return new Party(id, name, owner, creator, members, invites, progress, nextElapsedMillis);
        }

        Party withMembers(Set<UUID> nextMembers, Set<UUID> nextInvites) {
            return new Party(id, name, owner, creator, nextMembers, nextInvites, progress, elapsedMillis);
        }

        Party withOwner(UUID nextOwner) {
            return new Party(id, name, nextOwner, creator, members, invites, progress, elapsedMillis);
        }

        Party withName(String nextName) {
            return new Party(id, nextName, owner, creator, members, invites, progress, elapsedMillis);
        }
    }
}
