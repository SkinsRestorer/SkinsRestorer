package net.skinsrestorer.shared.utils.connections.responses.mineskin;

import lombok.Getter;

@Getter
public class MineSkinErrorDelayResponse {
    private String error;
    private int nextRequest;
    private int delay;
}
