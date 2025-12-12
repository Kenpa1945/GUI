package main;

import java.util.ArrayList;

public class StageManager {

    public ArrayList<StageMeta> stages = new ArrayList<>();

    public StageManager() {
        // Stage 1 – Burger basic
        StageMeta s1 = new StageMeta("Burger Stage 1", "/res/maps/map01.txt", 300);
        s1.isUnlocked = true;

        // Stage 2 – Burger medium
        StageMeta s2 = new StageMeta("Burger Stage 2", "/res/maps/map02.txt", 450);

        // Stage 3 – Sushi (contoh)
        StageMeta s3 = new StageMeta("Sushi Stage 1", "/res/maps/map03.txt", 600);

        stages.add(s1);
        stages.add(s2);
        stages.add(s3);
    }

    public void unlockNext(int index) {
        if (index + 1 < stages.size()) {
            stages.get(index + 1).isUnlocked = true;
        }
    }
}
