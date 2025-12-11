package main;

import java.util.List;

public class Order {
    public int position; // nomor urut (1..)
    public String recipeName; // "Classic Burger", ...
    public List<String> requiredItems; // contoh: ["bun","cooked_meat"]
    public int reward;
    public int penalty;
    public int timeLimitSeconds;
    public long remainingMillis; // runtime

    public Order(int position, String recipeName, List<String> requiredItems, int reward, int penalty, int timeLimitSeconds) {
        this.position = position;
        this.recipeName = recipeName;
        this.requiredItems = requiredItems;
        this.reward = reward;
        this.penalty = penalty;
        this.timeLimitSeconds = timeLimitSeconds;
        this.remainingMillis = timeLimitSeconds * 1000L;
    }

    public void update(long deltaMillis) {
        remainingMillis -= deltaMillis;
        if (remainingMillis < 0) remainingMillis = 0;
    }

    public int getRemainingSeconds() {
        return (int) Math.ceil(remainingMillis / 1000.0);
    }
}
