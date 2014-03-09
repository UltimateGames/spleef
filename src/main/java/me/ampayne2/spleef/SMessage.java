package me.ampayne2.spleef;

import me.ampayne2.ultimategames.api.message.Message;

public enum SMessage implements Message {
    GAME_END("GameEnd", "%s won %s on arena %s!"),
    DEATH("Death", "%s died!");

    private String message;
    private final String path;
    private final String defaultMessage;

    private SMessage(String path, String defaultMessage) {
        this.path = path;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getDefault() {
        return defaultMessage;
    }

    @Override
    public String toString() {
        return message;
    }
}
