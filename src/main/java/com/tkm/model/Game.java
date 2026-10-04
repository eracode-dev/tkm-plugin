package com.tkm.model;

import java.util.UUID;

/**
 * Tek bir TKM oyunu.
 */
public class Game {

    private final UUID creator;
    private final String creatorName;
    private final double bet;

    private UUID joiner;
    private String joinerName;

    private GameState state = GameState.WAITING;

    private Choice creatorChoice;
    private Choice joinerChoice;

    private int taskId = -1;
    private long chooseDeadline; // millis
    private int round = 1;

    public Game(UUID creator, String creatorName, double bet) {
        this.creator = creator;
        this.creatorName = creatorName;
        this.bet = bet;
    }

    public UUID getCreator() { return creator; }
    public String getCreatorName() { return creatorName; }
    public double getBet() { return bet; }

    public UUID getJoiner() { return joiner; }
    public String getJoinerName() { return joinerName; }
    public void setJoiner(UUID joiner, String joinerName) {
        this.joiner = joiner;
        this.joinerName = joinerName;
    }

    public GameState getState() { return state; }
    public void setState(GameState state) { this.state = state; }

    public Choice getCreatorChoice() { return creatorChoice; }
    public Choice getJoinerChoice() { return joinerChoice; }
    public void setCreatorChoice(Choice c) { this.creatorChoice = c; }
    public void setJoinerChoice(Choice c) { this.joinerChoice = c; }

    public boolean isParticipant(UUID uuid) {
        return creator.equals(uuid) || (joiner != null && joiner.equals(uuid));
    }

    public UUID getOpponent(UUID uuid) {
        if (creator.equals(uuid)) return joiner;
        if (joiner != null && joiner.equals(uuid)) return creator;
        return null;
    }

    public Choice getChoice(UUID uuid) {
        if (creator.equals(uuid)) return creatorChoice;
        if (joiner != null && joiner.equals(uuid)) return joinerChoice;
        return null;
    }

    public void setChoice(UUID uuid, Choice c) {
        if (creator.equals(uuid)) creatorChoice = c;
        else if (joiner != null && joiner.equals(uuid)) joinerChoice = c;
    }

    public boolean bothChose() {
        return creatorChoice != null && joinerChoice != null;
    }

    public void clearChoices() {
        creatorChoice = null;
        joinerChoice = null;
    }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public long getChooseDeadline() { return chooseDeadline; }
    public void setChooseDeadline(long chooseDeadline) { this.chooseDeadline = chooseDeadline; }

    public int getRound() { return round; }
    public void nextRound() { round++; clearChoices(); }
}
