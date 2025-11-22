package org.newdawn.spaceinvaders.database;

public class ScoreEntry {
    private String mode;
    private String email;
    private Integer score;
    private Integer wave;
    private Long durationMs;
    private String timestamp;
    private Integer level;

    public String getMode()        { return mode; }
    public String getEmail()       { return email; }
    public Integer getScore()      { return score; }
    public Integer getWave()       { return wave; }
    public Long getDurationMs()    { return durationMs; }
    public String getTimestamp()   { return timestamp; }
    public Integer getLevel()      { return level; }

    public void setMode(String mode)             { this.mode = mode; }
    public void setEmail(String email)           { this.email = email; }
    public void setScore(Integer score)          { this.score = score; }
    public void setWave(Integer wave)            { this.wave = wave; }
    public void setDurationMs(Long durationMs)   { this.durationMs = durationMs; }
    public void setTimestamp(String timestamp)   { this.timestamp = timestamp; }
    public void setLevel(Integer level)          { this.level = level; }
}
