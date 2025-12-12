package main;

public class StageMeta {

    public String name;
    public String mapPath;
    public int targetScore;
    public boolean isUnlocked;
    public boolean isCleared;

    public StageMeta(String name, String mapPath, int targetScore) {
        this.name = name;
        this.mapPath = mapPath;
        this.targetScore = targetScore;
        this.isUnlocked = false;
        this.isCleared = false;
    }
}

