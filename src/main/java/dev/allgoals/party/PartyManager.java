package dev.allgoals.party;

import dev.allgoals.AllGoals;
import dev.allgoals.progress.AllGoalsAttachments;
import dev.allgoals.progress.PlayerGoalProgress;
import dev.allgoals.world.RunModeManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PartyManager {
    private static final int TIMER_UPDATE_INTERVAL_TICKS = 20;
    private static final long TIMER_UPDATE_MILLIS = TIMER_UPDATE_INTERVAL_TICKS * 50L;
    private static int timerUpdateTicks;

    private PartyManager() {
    }

    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onDisconnect(handler.getPlayer()));
        ServerTickEvents.END_SERVER_TICK.register(PartyManager::tick);
    }

    public static PlayerGoalProgress activeProgress(ServerPlayer player) {
        return data(player.level().getServer()).partyForMember(player.getUUID())
                .map(PartySavedData.Party::progress)
                .orElseGet(() -> player.getAttachedOrCreate(AllGoalsAttachments.PLAYER_PROGRESS));
    }

    public static void saveActiveProgress(ServerPlayer actor, PlayerGoalProgress progress) {
        MinecraftServer server = actor.level().getServer();
        PartySavedData savedData = data(server);
        Optional<PartySavedData.Party> party = savedData.partyForMember(actor.getUUID());
        if (party.isEmpty()) {
            actor.setAttached(AllGoalsAttachments.PLAYER_PROGRESS, progress);
            return;
        }

        PartySavedData.Party updated = party.get().withProgress(progress);
        savedData.put(updated);
        syncParty(server, updated);
    }

    public static List<ServerPlayer> completionRecipients(ServerPlayer actor) {
        MinecraftServer server = actor.level().getServer();
        return data(server).partyForMember(actor.getUUID())
                .map(party -> onlineMembers(server, party))
                .orElseGet(() -> List.of(actor));
    }

    public static boolean isInParty(ServerPlayer player) {
        return data(player.level().getServer()).partyForMember(player.getUUID()).isPresent();
    }

    public static List<LeaderboardSnapshot> leaderboard(MinecraftServer server) {
        PartySavedData savedData = data(server);
        List<LeaderboardSnapshot> entries = new ArrayList<>();
        Set<UUID> partyMembers = new HashSet<>();

        for (PartySavedData.Party party : savedData.parties()) {
            List<ServerPlayer> online = onlineMembers(server, party);
            if (online.isEmpty()) continue;
            partyMembers.addAll(party.members());
            entries.add(new LeaderboardSnapshot(
                    party.creator(), party.name(), true,
                    AllGoals.goalCatalog().completedCount(party.progress()),
                    online.size(), party.members().size()
            ));
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (partyMembers.contains(player.getUUID())) continue;
            entries.add(new LeaderboardSnapshot(
                    player.getUUID(), player.getScoreboardName(), false,
                    AllGoals.goalCatalog().completedCount(activeProgress(player)), 1, 1
            ));
        }

        entries.sort(Comparator.comparingInt(LeaderboardSnapshot::completed).reversed()
                .thenComparing(LeaderboardSnapshot::name, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(entries);
    }

    public static Result create(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        PartySavedData savedData = data(server);
        if (savedData.partyForMember(player.getUUID()).isPresent()) {
            return Result.failure("You are already in an All Goals party.");
        }

        UUID partyId = UUID.randomUUID();
        String partyName = player.getScoreboardName() + "'s Party";
        PartySavedData.Party party = new PartySavedData.Party(
                partyId,
                partyName,
                player.getUUID(),
                player.getUUID(),
                Set.of(player.getUUID()),
                Set.of(),
                player.getAttachedOrCreate(AllGoalsAttachments.PLAYER_PROGRESS),
                0L
        );
        savedData.put(party);
        syncParty(server, party);
        return Result.success("Created " + partyName + ". Use /party to invite players.");
    }

    public static Result invite(ServerPlayer owner, ServerPlayer target) {
        if (owner.getUUID().equals(target.getUUID())) {
            return Result.failure("You cannot invite yourself.");
        }
        MinecraftServer server = owner.level().getServer();
        PartySavedData savedData = data(server);
        Optional<PartySavedData.Party> ownedParty = savedData.partyOwnedBy(owner.getUUID());
        if (ownedParty.isEmpty()) {
            return Result.failure("Only a party owner can invite players.");
        }
        if (savedData.partyForMember(target.getUUID()).isPresent()) {
            return Result.failure(target.getScoreboardName() + " is already in an All Goals party.");
        }

        PartySavedData.Party party = ownedParty.get();
        if (party.invites().contains(target.getUUID())) {
            return Result.failure(target.getScoreboardName() + " already has an invitation.");
        }
        Set<UUID> invites = new LinkedHashSet<>(party.invites());
        invites.add(target.getUUID());
        savedData.put(party.withMembers(party.members(), invites));

        target.sendSystemMessage(Component.literal(owner.getScoreboardName()
                + " invited you to " + party.name() + ".").withStyle(ChatFormatting.AQUA));
        target.sendSystemMessage(Component.empty()
                .append(chatButton("[Accept]", ChatFormatting.GREEN,
                        "/party accept " + owner.getScoreboardName(), "Join " + party.name()))
                .append(Component.literal("  "))
                .append(chatButton("[Decline]", ChatFormatting.RED,
                        "/party decline " + owner.getScoreboardName(), "Decline this invitation")));
        return Result.success("Invited " + target.getScoreboardName() + " to " + party.name() + ".");
    }

    public static Result accept(ServerPlayer player, ServerPlayer owner) {
        MinecraftServer server = player.level().getServer();
        PartySavedData savedData = data(server);
        if (savedData.partyForMember(player.getUUID()).isPresent()) {
            return Result.failure("Leave your current party before accepting another invitation.");
        }
        Optional<PartySavedData.Party> ownedParty = savedData.partyOwnedBy(owner.getUUID());
        if (ownedParty.isEmpty() || !ownedParty.get().invites().contains(player.getUUID())) {
            return Result.failure("You do not have an invitation from " + owner.getScoreboardName() + ".");
        }

        PartySavedData.Party party = ownedParty.get();
        PlayerGoalProgress merged = PlayerGoalProgress.merge(
                party.progress(),
                player.getAttachedOrCreate(AllGoalsAttachments.PLAYER_PROGRESS)
        );
        Set<UUID> members = new LinkedHashSet<>(party.members());
        members.add(player.getUUID());
        Set<UUID> invites = new LinkedHashSet<>(party.invites());
        invites.remove(player.getUUID());
        PartySavedData.Party updated = party.withMembers(members, invites).withProgress(merged);
        savedData.put(updated);
        syncParty(server, updated);
        notifyMembers(server, updated, player.getScoreboardName() + " joined " + updated.name() + ".");
        return Result.success("Joined " + updated.name() + ". Your existing progress was merged.");
    }

    public static Result decline(ServerPlayer player, ServerPlayer owner) {
        PartySavedData savedData = data(player.level().getServer());
        Optional<PartySavedData.Party> ownedParty = savedData.partyOwnedBy(owner.getUUID());
        if (ownedParty.isEmpty() || !ownedParty.get().invites().contains(player.getUUID())) {
            return Result.failure("You do not have an invitation from " + owner.getScoreboardName() + ".");
        }
        PartySavedData.Party party = ownedParty.get();
        Set<UUID> invites = new LinkedHashSet<>(party.invites());
        invites.remove(player.getUUID());
        savedData.put(party.withMembers(party.members(), invites));
        owner.sendSystemMessage(Component.literal(player.getScoreboardName() + " declined your party invitation."));
        return Result.success("Declined the invitation to " + party.name() + ".");
    }

    public static Result leave(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        PartySavedData savedData = data(server);
        Optional<PartySavedData.Party> found = savedData.partyForMember(player.getUUID());
        if (found.isEmpty()) return Result.failure("You are not in an All Goals party.");

        PartySavedData.Party party = found.get();
        if (party.owner().equals(player.getUUID()) && party.members().size() > 1) {
            return Result.failure("Transfer ownership or disband the party before leaving.");
        }
        if (party.members().size() == 1) {
            savedData.remove(party.id());
        } else {
            Set<UUID> members = new LinkedHashSet<>(party.members());
            members.remove(player.getUUID());
            PartySavedData.Party updated = party.withMembers(members, party.invites());
            savedData.put(updated);
            syncParty(server, updated);
            notifyMembers(server, updated, player.getScoreboardName() + " left " + updated.name() + ".");
        }
        player.setAttached(AllGoalsAttachments.PLAYER_PROGRESS, party.progress());
        player.setAttached(AllGoalsAttachments.PARTY_STATUS, PartyStatus.solo());
        return Result.success("Left " + party.name() + ". You kept a snapshot of its progress.");
    }

    public static Result kick(ServerPlayer owner, ServerPlayer target) {
        MinecraftServer server = owner.level().getServer();
        PartySavedData savedData = data(server);
        Optional<PartySavedData.Party> found = savedData.partyOwnedBy(owner.getUUID());
        if (found.isEmpty()) return Result.failure("Only a party owner can remove members.");
        if (owner.getUUID().equals(target.getUUID())) return Result.failure("Use /party leave instead.");

        PartySavedData.Party party = found.get();
        if (!party.members().contains(target.getUUID())) {
            return Result.failure(target.getScoreboardName() + " is not in your party.");
        }
        Set<UUID> members = new LinkedHashSet<>(party.members());
        members.remove(target.getUUID());
        PartySavedData.Party updated = party.withMembers(members, party.invites());
        savedData.put(updated);
        target.setAttached(AllGoalsAttachments.PLAYER_PROGRESS, party.progress());
        target.setAttached(AllGoalsAttachments.PARTY_STATUS, PartyStatus.solo());
        target.sendSystemMessage(Component.literal("You were removed from " + party.name()
                + ". You kept a snapshot of its progress.").withStyle(ChatFormatting.YELLOW));
        syncParty(server, updated);
        notifyMembers(server, updated, target.getScoreboardName() + " was removed from " + updated.name() + ".");
        return Result.success("Removed " + target.getScoreboardName() + " from " + party.name() + ".");
    }

    public static Result transfer(ServerPlayer owner, ServerPlayer target) {
        PartySavedData savedData = data(owner.level().getServer());
        Optional<PartySavedData.Party> found = savedData.partyOwnedBy(owner.getUUID());
        if (found.isEmpty()) return Result.failure("Only a party owner can transfer ownership.");
        PartySavedData.Party party = found.get();
        if (owner.getUUID().equals(target.getUUID())) return Result.failure("You already own this party.");
        if (!party.members().contains(target.getUUID())) {
            return Result.failure(target.getScoreboardName() + " is not in your party.");
        }
        PartySavedData.Party updated = party.withOwner(target.getUUID());
        savedData.put(updated);
        notifyMembers(owner.level().getServer(), updated,
                target.getScoreboardName() + " is now the owner of " + updated.name() + ".");
        return Result.success("Transferred party ownership to " + target.getScoreboardName() + ".");
    }

    public static Result disband(ServerPlayer owner) {
        MinecraftServer server = owner.level().getServer();
        PartySavedData savedData = data(server);
        Optional<PartySavedData.Party> found = savedData.partyOwnedBy(owner.getUUID());
        if (found.isEmpty()) return Result.failure("Only a party owner can disband the party.");

        PartySavedData.Party party = found.get();
        for (UUID memberId : party.members()) {
            savedData.putPendingSnapshot(memberId, party.progress());
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) applyPendingSnapshot(savedData, member);
        }
        savedData.remove(party.id());
        return Result.success("Disbanded " + party.name() + ". Every member kept its final progress snapshot.");
    }

    public static Result status(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        Optional<PartySavedData.Party> found = data(server).partyForMember(player.getUUID());
        if (found.isEmpty()) return Result.success("You are not currently in an All Goals party.");
        PartySavedData.Party party = found.get();
        int online = onlineMembers(server, party).size();
        int completed = AllGoals.goalCatalog().completedCount(party.progress());
        String role = party.owner().equals(player.getUUID()) ? "Owner" : "Member";
        return Result.success(party.name() + " — " + role + ", " + online + "/"
                + party.members().size() + " online, " + completed + "/"
                + AllGoals.goalCatalog().goalCount() + " goals complete, timer "
                + formatTimer(party.elapsedMillis()) + ".");
    }

    public static Result rename(ServerPlayer owner, String requestedName) {
        String name = requestedName == null ? "" : requestedName.strip();
        if (name.isEmpty() || name.length() > 32 || name.chars().anyMatch(character ->
                character < 32 || character == 127 || character == '\u00a7')) {
            return Result.failure("Party names must be 1-32 normal characters.");
        }
        PartySavedData savedData = data(owner.level().getServer());
        Optional<PartySavedData.Party> found = savedData.partyOwnedBy(owner.getUUID());
        if (found.isEmpty()) return Result.failure("Only a party owner can rename the party.");
        PartySavedData.Party updated = found.get().withName(name);
        savedData.put(updated);
        syncParty(owner.level().getServer(), updated);
        notifyMembers(owner.level().getServer(), updated, "Party renamed to " + name + ".");
        return Result.success("Renamed your party to " + name + ".");
    }

    public static void showMenu(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        PartySavedData savedData = data(server);
        Optional<PartySavedData.Party> membership = savedData.partyForMember(player.getUUID());

        player.sendSystemMessage(Component.literal("—— All Goals Party ——").withStyle(ChatFormatting.GOLD));
        if (membership.isEmpty()) {
            player.sendSystemMessage(Component.literal("You are not in a party. ")
                    .append(chatButton("[Create Party]", ChatFormatting.GREEN,
                            "/party create", "Start a shared All Goals run")));
            for (PartySavedData.Party party : savedData.parties()) {
                if (!party.invites().contains(player.getUUID())) continue;
                ServerPlayer owner = server.getPlayerList().getPlayer(party.owner());
                if (owner == null) continue;
                player.sendSystemMessage(Component.literal(party.name() + "  ")
                        .append(chatButton("[Accept]", ChatFormatting.GREEN,
                                "/party accept " + owner.getScoreboardName(), "Join this party"))
                        .append(Component.literal(" "))
                        .append(chatButton("[Decline]", ChatFormatting.RED,
                                "/party decline " + owner.getScoreboardName(), "Decline this invitation")));
            }
            return;
        }

        PartySavedData.Party party = membership.get();
        boolean owner = party.owner().equals(player.getUUID());
        player.sendSystemMessage(Component.literal(status(player).message()).withStyle(ChatFormatting.AQUA));
        if (owner) {
            player.sendSystemMessage(Component.literal("Members:").withStyle(ChatFormatting.GRAY));
            for (ServerPlayer member : onlineMembers(server, party)) {
                if (member.getUUID().equals(player.getUUID())) continue;
                player.sendSystemMessage(Component.literal(member.getScoreboardName() + " ")
                        .append(chatButton("[Remove]", ChatFormatting.RED,
                                "/party kick " + member.getScoreboardName(),
                                "Remove " + member.getScoreboardName()))
                        .append(Component.literal(" "))
                        .append(chatButton("[Make Owner]", ChatFormatting.YELLOW,
                                "/party transfer " + member.getScoreboardName(),
                                "Transfer ownership to " + member.getScoreboardName())));
            }
            player.sendSystemMessage(Component.literal("Invite: ").withStyle(ChatFormatting.GRAY));
            boolean candidateFound = false;
            for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
                if (candidate.getUUID().equals(player.getUUID())
                        || savedData.partyForMember(candidate.getUUID()).isPresent()
                        || party.invites().contains(candidate.getUUID())) continue;
                candidateFound = true;
                player.sendSystemMessage(Component.literal(candidate.getScoreboardName() + " ")
                        .append(chatButton("[Invite]", ChatFormatting.GREEN,
                                "/party invite " + candidate.getScoreboardName(),
                                "Invite " + candidate.getScoreboardName())));
            }
            if (!candidateFound) {
                player.sendSystemMessage(Component.literal("No available online players.")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            player.sendSystemMessage(Component.empty()
                    .append(suggestButton("[Rename]", ChatFormatting.YELLOW,
                            "/party name ", "Type a new party name"))
                    .append(Component.literal("  "))
                    .append(chatButton("[Reset]", ChatFormatting.RED,
                            "/party reset confirm", "Reset shared progress and timer"))
                    .append(Component.literal("  "))
                    .append(chatButton("[Disband]", ChatFormatting.DARK_RED,
                            "/party disband confirm", "Disband this party")));
        } else {
            player.sendSystemMessage(chatButton("[Leave Party]", ChatFormatting.RED,
                    "/party leave", "Leave and keep a progress snapshot"));
        }
    }

    public static Result resetParty(ServerPlayer owner) {
        MinecraftServer server = owner.level().getServer();
        PartySavedData savedData = data(server);
        Optional<PartySavedData.Party> found = savedData.partyOwnedBy(owner.getUUID());
        if (found.isEmpty()) return Result.failure("Only a party owner can reset shared progress.");
        PartySavedData.Party party = found.get();
        PartySavedData.Party reset = new PartySavedData.Party(
                party.id(), party.name(), party.owner(), party.creator(), party.members(), party.invites(),
                PlayerGoalProgress.empty(), 0L
        );
        savedData.put(reset);
        syncParty(server, reset);
        notifyMembers(server, reset, owner.getScoreboardName() + " reset the party's All Goals run.");
        return Result.success("Reset shared party progress and timer.");
    }

    private static void onJoin(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        PartySavedData savedData = data(server);
        Optional<PartySavedData.Party> party = savedData.partyForMember(player.getUUID());
        if (party.isPresent()) {
            syncParty(server, party.get());
            return;
        }
        if (!applyPendingSnapshot(savedData, player)) {
            player.setAttached(AllGoalsAttachments.PARTY_STATUS, PartyStatus.solo());
        }
    }

    private static void onDisconnect(ServerPlayer player) {
        // The player list is finalized immediately after this callback. The next
        // status sync corrects online counts without risking a stale login packet.
    }

    private static void tick(MinecraftServer server) {
        if (!RunModeManager.tracksGoals(server)) return;
        if (++timerUpdateTicks < TIMER_UPDATE_INTERVAL_TICKS) return;
        timerUpdateTicks = 0;

        PartySavedData savedData = data(server);
        for (PartySavedData.Party party : savedData.parties()) {
            List<ServerPlayer> online = onlineMembers(server, party);
            boolean running = timerRunning(party, online);
            PartySavedData.Party current = party;
            if (running) {
                long elapsed = party.elapsedMillis() > Long.MAX_VALUE - TIMER_UPDATE_MILLIS
                        ? Long.MAX_VALUE : party.elapsedMillis() + TIMER_UPDATE_MILLIS;
                current = party.withElapsedMillis(elapsed);
                savedData.put(current);
            }
            syncParty(current, online, running);
        }
    }

    private static boolean applyPendingSnapshot(PartySavedData savedData, ServerPlayer player) {
        Optional<PlayerGoalProgress> pending = savedData.takePendingSnapshot(player.getUUID());
        if (pending.isEmpty()) return false;
        player.setAttached(AllGoalsAttachments.PLAYER_PROGRESS, pending.get());
        player.setAttached(AllGoalsAttachments.PARTY_STATUS, PartyStatus.solo());
        player.sendSystemMessage(Component.literal(
                "Your final All Goals party progress was restored.").withStyle(ChatFormatting.AQUA));
        return true;
    }

    private static void syncParty(MinecraftServer server, PartySavedData.Party party) {
        List<ServerPlayer> online = onlineMembers(server, party);
        syncParty(party, online, timerRunning(party, online));
    }

    private static void syncParty(PartySavedData.Party party, List<ServerPlayer> online, boolean timerRunning) {
        PartyStatus status = new PartyStatus(
                true,
                party.name(),
                party.members().size(),
                online.size(),
                party.elapsedMillis(),
                timerRunning
        );
        for (ServerPlayer member : online) {
            PlayerGoalProgress currentProgress = member.getAttachedOrElse(
                    AllGoalsAttachments.PLAYER_PROGRESS, PlayerGoalProgress.empty());
            if (currentProgress != party.progress()) {
                member.setAttached(AllGoalsAttachments.PLAYER_PROGRESS, party.progress());
            }
            PartyStatus currentStatus = member.getAttachedOrElse(
                    AllGoalsAttachments.PARTY_STATUS, PartyStatus.solo());
            if (!currentStatus.equals(status)) {
                member.setAttached(AllGoalsAttachments.PARTY_STATUS, status);
            }
        }
    }

    private static List<ServerPlayer> onlineMembers(MinecraftServer server, PartySavedData.Party party) {
        List<ServerPlayer> online = new ArrayList<>();
        for (UUID memberId : party.members()) {
            ServerPlayer player = server.getPlayerList().getPlayer(memberId);
            if (player != null) online.add(player);
        }
        return List.copyOf(online);
    }

    private static boolean timerRunning(PartySavedData.Party party, List<ServerPlayer> online) {
        return !online.isEmpty() && !AllGoals.goalCatalog().allComplete(party.progress());
    }

    private static void notifyMembers(MinecraftServer server, PartySavedData.Party party, String message) {
        Component component = Component.literal(message).withStyle(ChatFormatting.AQUA);
        onlineMembers(server, party).forEach(member -> member.sendSystemMessage(component));
    }

    private static Component chatButton(String label, ChatFormatting color, String command, String hover) {
        return Component.literal(label).withStyle(style -> style
                .withColor(color)
                .withBold(true)
                .withClickEvent(new net.minecraft.network.chat.ClickEvent.RunCommand(command))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(Component.literal(hover))));
    }

    private static Component suggestButton(String label, ChatFormatting color, String command, String hover) {
        return Component.literal(label).withStyle(style -> style
                .withColor(color)
                .withBold(true)
                .withClickEvent(new net.minecraft.network.chat.ClickEvent.SuggestCommand(command))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(Component.literal(hover))));
    }

    private static PartySavedData data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PartySavedData.TYPE);
    }

    private static String formatTimer(long elapsedMillis) {
        long totalSeconds = Math.max(0L, elapsedMillis) / 1000L;
        long seconds = totalSeconds % 60L;
        long minutes = totalSeconds / 60L % 60L;
        long hours = totalSeconds / 3600L;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    public record Result(boolean success, String message) {
        static Result success(String message) {
            return new Result(true, message);
        }

        static Result failure(String message) {
            return new Result(false, message);
        }
    }

    public record LeaderboardSnapshot(
            UUID headOwner,
            String name,
            boolean party,
            int completed,
            int onlineMembers,
            int totalMembers
    ) {
    }
}
