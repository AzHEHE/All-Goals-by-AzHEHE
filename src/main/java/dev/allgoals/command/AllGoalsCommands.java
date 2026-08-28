package dev.allgoals.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.allgoals.AllGoals;
import dev.allgoals.goal.GoalDefinition;
import dev.allgoals.network.LeaderboardNetworking;
import dev.allgoals.party.PartyManager;
import dev.allgoals.progress.AllGoalsAttachments;
import dev.allgoals.progress.PlayerGoalProgress;
import dev.allgoals.tracking.GoalProgressService;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Optional;

public final class AllGoalsCommands {
    private AllGoalsCommands() {
    }

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> {
            dispatcher.register(
                Commands.literal("allgoals")
                        .executes(command -> status(command.getSource().getPlayerOrException()))
                        .then(Commands.literal("status")
                                .executes(command -> status(command.getSource().getPlayerOrException())))
                        .then(Commands.literal("party")
                                .executes(command -> partyMenu(command.getSource().getPlayerOrException()))
                                .then(Commands.literal("status")
                                        .executes(command -> partyResult(
                                                command.getSource().getPlayerOrException(),
                                                PartyManager.status(command.getSource().getPlayerOrException()))))
                                .then(Commands.literal("create")
                                        .executes(command -> partyResult(
                                                command.getSource().getPlayerOrException(),
                                                PartyManager.create(command.getSource().getPlayerOrException()))))
                                .then(Commands.literal("invite")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(command -> partyResult(
                                                        command.getSource().getPlayerOrException(),
                                                        PartyManager.invite(
                                                                command.getSource().getPlayerOrException(),
                                                                EntityArgument.getPlayer(command, "player"))))))
                                .then(Commands.literal("accept")
                                        .then(Commands.argument("owner", EntityArgument.player())
                                                .executes(command -> partyResult(
                                                        command.getSource().getPlayerOrException(),
                                                        PartyManager.accept(
                                                                command.getSource().getPlayerOrException(),
                                                                EntityArgument.getPlayer(command, "owner"))))))
                                .then(Commands.literal("decline")
                                        .then(Commands.argument("owner", EntityArgument.player())
                                                .executes(command -> partyResult(
                                                        command.getSource().getPlayerOrException(),
                                                        PartyManager.decline(
                                                                command.getSource().getPlayerOrException(),
                                                                EntityArgument.getPlayer(command, "owner"))))))
                                .then(Commands.literal("leave")
                                        .executes(command -> partyResult(
                                                command.getSource().getPlayerOrException(),
                                                PartyManager.leave(command.getSource().getPlayerOrException()))))
                                .then(Commands.literal("kick")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(command -> partyResult(
                                                        command.getSource().getPlayerOrException(),
                                                        PartyManager.kick(
                                                                command.getSource().getPlayerOrException(),
                                                                EntityArgument.getPlayer(command, "player"))))))
                                .then(Commands.literal("transfer")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(command -> partyResult(
                                                        command.getSource().getPlayerOrException(),
                                                        PartyManager.transfer(
                                                                command.getSource().getPlayerOrException(),
                                                                EntityArgument.getPlayer(command, "player"))))))
                                .then(Commands.literal("name")
                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                .executes(command -> partyResult(
                                                        command.getSource().getPlayerOrException(),
                                                        PartyManager.rename(
                                                                command.getSource().getPlayerOrException(),
                                                                StringArgumentType.getString(command, "name"))))))
                                .then(Commands.literal("disband")
                                        .then(Commands.literal("confirm")
                                                .executes(command -> partyResult(
                                                        command.getSource().getPlayerOrException(),
                                                        PartyManager.disband(
                                                                command.getSource().getPlayerOrException())))))
                                .then(Commands.literal("reset")
                                        .then(Commands.literal("confirm")
                                                .executes(command -> partyResult(
                                                        command.getSource().getPlayerOrException(),
                                                PartyManager.resetParty(
                                                                command.getSource().getPlayerOrException()))))))
                        .then(Commands.literal("leaderboard")
                                .executes(command -> leaderboard(command.getSource().getPlayerOrException())))
                        .then(Commands.literal("grant-all")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(command -> grantAll(command.getSource().getPlayerOrException())))
                        .then(Commands.literal("grant")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("goal_id", StringArgumentType.greedyString())
                                        .suggests((command, builder) -> SharedSuggestionProvider.suggest(
                                                AllGoals.goalCatalog().goals().stream()
                                                        .map(GoalDefinition::sourceId), builder))
                                        .executes(command -> grant(
                                                command.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(command, "goal_id")))))
                        .then(Commands.literal("revoke")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("goal_id", StringArgumentType.greedyString())
                                        .suggests((command, builder) -> SharedSuggestionProvider.suggest(
                                                AllGoals.goalCatalog().goals().stream()
                                                        .map(GoalDefinition::sourceId), builder))
                                        .executes(command -> revoke(
                                                command.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(command, "goal_id")))))
                        .then(Commands.literal("reset")
                                .then(Commands.literal("confirm")
                                        .executes(command -> reset(command.getSource().getPlayerOrException()))))
            );
            dispatcher.register(partyCommand());
            dispatcher.register(Commands.literal("leaderboard")
                    .executes(command -> leaderboard(command.getSource().getPlayerOrException())));
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> partyCommand() {
        return Commands.literal("party")
                .executes(command -> partyMenu(command.getSource().getPlayerOrException()))
                .then(Commands.literal("status")
                        .executes(command -> partyMenu(command.getSource().getPlayerOrException())))
                .then(Commands.literal("create")
                        .executes(command -> partyResultAndMenu(
                                command.getSource().getPlayerOrException(),
                                PartyManager.create(command.getSource().getPlayerOrException()))))
                .then(Commands.literal("invite")
                        .executes(command -> partyMenu(command.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(command -> partyResultAndMenu(
                                        command.getSource().getPlayerOrException(),
                                        PartyManager.invite(
                                                command.getSource().getPlayerOrException(),
                                                EntityArgument.getPlayer(command, "player"))))))
                .then(Commands.literal("accept")
                        .executes(command -> partyMenu(command.getSource().getPlayerOrException()))
                        .then(Commands.argument("owner", EntityArgument.player())
                                .executes(command -> partyResultAndMenu(
                                        command.getSource().getPlayerOrException(),
                                        PartyManager.accept(
                                                command.getSource().getPlayerOrException(),
                                                EntityArgument.getPlayer(command, "owner"))))))
                .then(Commands.literal("decline")
                        .executes(command -> partyMenu(command.getSource().getPlayerOrException()))
                        .then(Commands.argument("owner", EntityArgument.player())
                                .executes(command -> partyResultAndMenu(
                                        command.getSource().getPlayerOrException(),
                                        PartyManager.decline(
                                                command.getSource().getPlayerOrException(),
                                                EntityArgument.getPlayer(command, "owner"))))))
                .then(Commands.literal("leave")
                        .executes(command -> partyResultAndMenu(
                                command.getSource().getPlayerOrException(),
                                PartyManager.leave(command.getSource().getPlayerOrException()))))
                .then(Commands.literal("kick")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(command -> partyResultAndMenu(
                                        command.getSource().getPlayerOrException(),
                                        PartyManager.kick(
                                                command.getSource().getPlayerOrException(),
                                                EntityArgument.getPlayer(command, "player"))))))
                .then(Commands.literal("transfer")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(command -> partyResultAndMenu(
                                        command.getSource().getPlayerOrException(),
                                        PartyManager.transfer(
                                                command.getSource().getPlayerOrException(),
                                                EntityArgument.getPlayer(command, "player"))))))
                .then(Commands.literal("name")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(command -> partyResultAndMenu(
                                        command.getSource().getPlayerOrException(),
                                        PartyManager.rename(
                                                command.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(command, "name"))))))
                .then(Commands.literal("disband")
                        .then(Commands.literal("confirm")
                                .executes(command -> partyResultAndMenu(
                                        command.getSource().getPlayerOrException(),
                                        PartyManager.disband(command.getSource().getPlayerOrException())))))
                .then(Commands.literal("reset")
                        .then(Commands.literal("confirm")
                                .executes(command -> partyResultAndMenu(
                                        command.getSource().getPlayerOrException(),
                                        PartyManager.resetParty(command.getSource().getPlayerOrException())))));
    }

    private static int status(ServerPlayer player) {
        PlayerGoalProgress progress = PartyManager.activeProgress(player);
        int completed = AllGoals.goalCatalog().completedCount(progress);
        player.sendSystemMessage(Component.literal("All Goals: " + completed + " / "
                + AllGoals.goalCatalog().goalCount() + " complete. Press B to open the goal browser."));
        return completed;
    }

    private static int reset(ServerPlayer player) {
        if (PartyManager.isInParty(player)) {
            player.sendSystemMessage(Component.literal(
                    "Party progress can only be reset by its owner with /party reset confirm.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        player.setAttached(AllGoalsAttachments.PLAYER_PROGRESS, PlayerGoalProgress.empty());
        player.sendSystemMessage(Component.literal("All Goals progress reset for this player."));
        return 1;
    }

    private static int grantAll(ServerPlayer player) {
        PlayerGoalProgress oldProgress = PartyManager.activeProgress(player);
        PlayerGoalProgress.Editor editor = oldProgress.edit();
        int granted = 0;
        for (GoalDefinition goal : AllGoals.goalCatalog().goals()) {
            if (!oldProgress.isComplete(goal.sourceId())) granted++;
            editor.grantCompletion(goal.sourceId());
        }
        GoalProgressService.save(player, oldProgress, editor, false);
        player.sendSystemMessage(Component.literal("Granted " + granted + " All Goals completion(s)."));
        return granted;
    }

    private static int grant(ServerPlayer player, String requestedId) {
        Optional<GoalDefinition> requested = findGoal(requestedId);
        if (requested.isEmpty()) {
            player.sendSystemMessage(Component.literal("Unknown All Goals goal: " + requestedId));
            return 0;
        }
        GoalDefinition goal = requested.get();
        PlayerGoalProgress oldProgress = PartyManager.activeProgress(player);
        if (oldProgress.isComplete(goal.sourceId())) {
            player.sendSystemMessage(Component.literal(goal.displayName() + " is already completed."));
            return 0;
        }
        PlayerGoalProgress.Editor editor = oldProgress.edit();
        editor.grantCompletion(goal.sourceId());
        GoalProgressService.save(player, oldProgress, editor, false);
        player.sendSystemMessage(Component.literal("Granted goal: " + goal.displayName()));
        return 1;
    }

    private static int revoke(ServerPlayer player, String requestedId) {
        Optional<GoalDefinition> requested = findGoal(requestedId);
        if (requested.isEmpty()) {
            player.sendSystemMessage(Component.literal("Unknown All Goals goal: " + requestedId));
            return 0;
        }
        GoalDefinition goal = requested.get();
        PlayerGoalProgress oldProgress = PartyManager.activeProgress(player);
        if (!oldProgress.isComplete(goal.sourceId())) {
            player.sendSystemMessage(Component.literal(goal.displayName() + " is not completed."));
            return 0;
        }
        PlayerGoalProgress.Editor editor = oldProgress.edit();
        editor.revokeCompletion(goal.sourceId());
        GoalProgressService.save(player, oldProgress, editor, false);
        player.sendSystemMessage(Component.literal("Revoked goal: " + goal.displayName()));
        return 1;
    }

    private static Optional<GoalDefinition> findGoal(String requestedId) {
		String normalized = requestedId.strip().toUpperCase(Locale.ROOT);
		return AllGoals.goalCatalog().findSource(normalized);
    }

    private static int partyResult(ServerPlayer player, PartyManager.Result result) {
        player.sendSystemMessage(Component.literal(result.message()).withStyle(
                result.success() ? ChatFormatting.AQUA : ChatFormatting.RED
        ));
        return result.success() ? 1 : 0;
    }

    private static int partyMenu(ServerPlayer player) {
        PartyManager.showMenu(player);
        return 1;
    }

    private static int partyResultAndMenu(ServerPlayer player, PartyManager.Result result) {
        int value = partyResult(player, result);
        PartyManager.showMenu(player);
        return value;
    }

    private static int leaderboard(ServerPlayer player) {
        if (LeaderboardNetworking.sendSnapshot(player)) return 1;
        player.sendSystemMessage(Component.literal(
                "Update All Goals on your client to open the world leaderboard.")
                .withStyle(ChatFormatting.YELLOW));
        return 0;
    }

}
