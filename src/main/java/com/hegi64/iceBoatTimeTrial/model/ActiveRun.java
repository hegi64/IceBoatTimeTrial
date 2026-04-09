package com.hegi64.iceBoatTimeTrial.model;

import java.util.UUID;

public class ActiveRun {
    private final UUID playerId;
    private final Track track;
    private final long startedAtMillis;
    private boolean checkpoint1Passed;
    private boolean checkpoint2Passed;
    private Long sector1Millis;
    private Long sector2Millis;
    private Long personalBestTotalMillis;
    private Long personalBestSector1Millis;
    private Long personalBestSector2Millis;
    private Long personalBestSector3Millis;
    private Long globalBestTotalMillis;
    private Long globalBestSector1Millis;
    private Long globalBestSector2Millis;
    private Long globalBestSector3Millis;

    public ActiveRun(UUID playerId, Track track, long startedAtMillis) {
        this.playerId = playerId;
        this.track = track;
        this.startedAtMillis = startedAtMillis;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Track getTrack() {
        return track;
    }

    public long getStartedAtMillis() {
        return startedAtMillis;
    }

    public boolean isCheckpoint1Passed() {
        return checkpoint1Passed;
    }

    public void setCheckpoint1Passed(boolean checkpoint1Passed) {
        this.checkpoint1Passed = checkpoint1Passed;
    }

    public boolean isCheckpoint2Passed() {
        return checkpoint2Passed;
    }

    public void setCheckpoint2Passed(boolean checkpoint2Passed) {
        this.checkpoint2Passed = checkpoint2Passed;
    }

    public Long getSector1Millis() {
        return sector1Millis;
    }

    public void setSector1Millis(Long sector1Millis) {
        this.sector1Millis = sector1Millis;
    }

    public Long getSector2Millis() {
        return sector2Millis;
    }

    public void setSector2Millis(Long sector2Millis) {
        this.sector2Millis = sector2Millis;
    }

    public Long getPersonalBestTotalMillis() {
        return personalBestTotalMillis;
    }

    public void setPersonalBestTotalMillis(Long personalBestTotalMillis) {
        this.personalBestTotalMillis = personalBestTotalMillis;
    }

    public Long getPersonalBestSector1Millis() {
        return personalBestSector1Millis;
    }

    public void setPersonalBestSector1Millis(Long personalBestSector1Millis) {
        this.personalBestSector1Millis = personalBestSector1Millis;
    }

    public Long getPersonalBestSector2Millis() {
        return personalBestSector2Millis;
    }

    public void setPersonalBestSector2Millis(Long personalBestSector2Millis) {
        this.personalBestSector2Millis = personalBestSector2Millis;
    }

    public Long getPersonalBestSector3Millis() {
        return personalBestSector3Millis;
    }

    public void setPersonalBestSector3Millis(Long personalBestSector3Millis) {
        this.personalBestSector3Millis = personalBestSector3Millis;
    }

    public Long getGlobalBestTotalMillis() {
        return globalBestTotalMillis;
    }

    public void setGlobalBestTotalMillis(Long globalBestTotalMillis) {
        this.globalBestTotalMillis = globalBestTotalMillis;
    }

    public Long getGlobalBestSector1Millis() {
        return globalBestSector1Millis;
    }

    public void setGlobalBestSector1Millis(Long globalBestSector1Millis) {
        this.globalBestSector1Millis = globalBestSector1Millis;
    }

    public Long getGlobalBestSector2Millis() {
        return globalBestSector2Millis;
    }

    public void setGlobalBestSector2Millis(Long globalBestSector2Millis) {
        this.globalBestSector2Millis = globalBestSector2Millis;
    }

    public Long getGlobalBestSector3Millis() {
        return globalBestSector3Millis;
    }

    public void setGlobalBestSector3Millis(Long globalBestSector3Millis) {
        this.globalBestSector3Millis = globalBestSector3Millis;
    }
}
