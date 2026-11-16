package com.carbuyer.bot.entity;

public enum BotStatus {
    ALREADY_STARTED("Bots is already started"),
    SUCCESSFULLY_STARTED("Bots successfully started"),
    STOPPED("Bots stopped");

    public final String text;

    BotStatus(final String text)
    {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
