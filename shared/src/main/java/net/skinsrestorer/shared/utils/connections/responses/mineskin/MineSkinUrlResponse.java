package net.skinsrestorer.shared.utils.connections.responses.mineskin;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class MineSkinUrlResponse {
    private String id;
    private String idStr;
    private String uuid;
    private String name;
    private String variant;
    private MineSkinData data;
    private long timestamp;
    private int duration;
    private int account;
    private String server;
    @SerializedName("private")
    private boolean private_;
    private int views;
    private int nextRequest;
    private boolean duplicate;
}
