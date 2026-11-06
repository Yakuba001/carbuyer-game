package com.carbuyer.bot.entity;

public enum BotAddedStatus {
    BOTS_ADDED("Bots added"),
    BOTS_ADDED_IN_PROGRESS("Bots added in progress");

    public final String text;

    BotAddedStatus(final String text)
    {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
