package net.pistonmaster.pistonfilter.utils;

public class BackendResponse {
    public boolean allowed;
    public String replacement;

    public BackendResponse(boolean allowed, String replacement) {
        this.allowed = allowed;
        this.replacement = replacement;
    }
}
