package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class OrderManager {
    GamePanel gp;
    public ArrayList<Order> activeOrders = new ArrayList<>();
    private int nextOrderPosition = 1;
    private Random rnd = new Random();
    int stageId;

    // recipes definitions (name -> required items)
    // Note: requiredItems MUST match item keys used everywhere (e.g. "bun","cooked_meat","chopped_cheese"...)
    private final List<Order> recipePrototypes;

    public OrderManager(GamePanel gp) {
        this.gp = gp;
        int stageId = gp.stageConfig.stageId;

        // Ambil multiplier difficulty dari StageConfig (kalau belum ada, pakai 1.0)
        double r = (gp.stageConfig != null) ? gp.stageConfig.rewardMultiplier  : 1.0;
        double p = (gp.stageConfig != null) ? gp.stageConfig.penaltyMultiplier : 1.0;

        recipePrototypes = new ArrayList<>();

        if (stageId == 1) {
            addRecipeWeighted("Classic Burger", 5, r, p);
            addRecipeWeighted("Cheeseburger",   4, r, p);
            addRecipeWeighted("BLT Burger",     1, r, p);
            addRecipeWeighted("Deluxe Burger",  1, r, p);
        }
        else if (stageId == 2) {
            addRecipeWeighted("Classic Burger", 3, r, p);
            addRecipeWeighted("Cheeseburger",   3, r, p);
            addRecipeWeighted("BLT Burger",     2, r, p);
            addRecipeWeighted("Deluxe Burger",  2, r, p);
        }
        else if (stageId == 3) {
            addRecipeWeighted("Classic Burger", 2, r, p);
            addRecipeWeighted("Cheeseburger",   2, r, p);
            addRecipeWeighted("BLT Burger",     4, r, p);
            addRecipeWeighted("Deluxe Burger",  4, r, p);
        }
    }

    private void addRecipeWeighted(String name, int weight, double r, double p) {

        List<String> req;
        int reward, penalty, time;

        // matching resep berdasarkan implementasi Anda
        switch (name) {
            case "Classic Burger" -> {
                req = Arrays.asList("bun","cooked_meat");
                reward = (int)Math.round(120 * r);
                penalty = (int)Math.round(-50 * p);
                time = 45;
            }
            case "Cheeseburger" -> {
                req = Arrays.asList("bun","cooked_meat","chopped_cheese");
                reward = (int)Math.round(150 * r);
                penalty = (int)Math.round(-60 * p);
                time = 50;
            }
            case "BLT Burger" -> {
                req = Arrays.asList("bun","chopped_lettuce","chopped_tomato","cooked_meat");
                reward = (int)Math.round(170 * r);
                penalty = (int)Math.round(-70 * p);
                time = 55;
            }
            case "Deluxe Burger" -> {
                req = Arrays.asList("bun","chopped_lettuce","cooked_meat","chopped_cheese");
                reward = (int)Math.round(200 * r);
                penalty = (int)Math.round(-80 * p);
                time = 60;
            }
            default -> {
                return;
            }
        }

        // duplikasi resep sesuai weight
        for (int i = 0; i < weight; i++) {
            recipePrototypes.add(new Order(
                    0,
                    name,
                    req,
                    reward,
                    penalty,
                    time
            ));
        }
    }

    public void resetSequence() {
        nextOrderPosition = 1;
    }

    // helper supaya semua spawn respect maxActiveOrders di StageConfig
    private int getMaxActiveOrders() {
        return (gp.stageConfig != null) ? gp.stageConfig.maxActiveOrders : 3;
    }

    // spawn sampai menyentuh limit stage
    public void trySpawnInitial() {
        int max = getMaxActiveOrders();
        while (activeOrders.size() < max) {
            spawnRandomOrder();
        }
    }

    public void spawnRandomOrder() {
        // game sudah habis waktu? jangan spawn lagi
        if (gp.remainingTimeMillis <= 0) return;

        int max = getMaxActiveOrders();
        if (activeOrders.size() >= max) return;

        int idx = rnd.nextInt(recipePrototypes.size());
        Order proto = recipePrototypes.get(idx);

        Order o = new Order(
                nextOrderPosition++,
                proto.recipeName,
                proto.requiredItems,
                proto.reward,
                proto.penalty,
                proto.timeLimitSeconds
        );
        activeOrders.add(o);
    }

    public void update(long deltaMillis) {
        if (activeOrders.isEmpty()) return;

        // HANYA update order pertama (index 0). Order berikutnya menunggu.
        Order front = activeOrders.get(0);
        front.update(deltaMillis);

        // Jika order pertama expired -> beri penalti dan remove, lalu spawn pengganti jika game masih berjalan
        if (front.remainingMillis <= 0) {
            // penalti expired berbasis multiplier stage (bisa bedakan stage 1,2,3)
            double pm = (gp.stageConfig != null) ? gp.stageConfig.penaltyMultiplier : 1.0;
            int basePenalty = 50; // base
            int penaltyAmount = (int)Math.round(basePenalty * pm);

            gp.score -= penaltyAmount;
            gp.ordersFailed++;            // catat statistik
            activeOrders.remove(0);       // ambil next jadi index 0

            if (gp.remainingTimeMillis > 0) {
                spawnRandomOrder();
            }
        }
    }

    /**
     * Try match a plate contents to an active order.
     * If matched, return the matched Order (the earliest one if several match).
     * Matching rules: exact multiset equality (counts must match). If multiple same recipe active, return earliest position.
     */
    public Order matchPlateContents(List<String> plateItems) {
        if (plateItems == null) return null;
        // create multiset from plate
        List<String> plateCopy = new ArrayList<>(plateItems);
        for (Order o : new ArrayList<>(activeOrders)) {
            List<String> need = o.requiredItems;
            if (multisetEquals(need, plateCopy)) {
                // return the earliest matching one: since activeOrders is insertion order, first matching is earliest
                return o;
            }
        }
        return null;
    }

    public boolean multisetEquals(List<String> a, List<String> b) {
        if (a.size() != b.size()) return false;
        List<String> aa = new ArrayList<>(a);
        List<String> bb = new ArrayList<>(b);
        Collections.sort(aa);
        Collections.sort(bb);
        return aa.equals(bb);
    }

    public void completeOrder(Order o) {
        if (o == null) return;
        gp.score += o.reward;
        gp.ordersCompleted++;
        activeOrders.remove(o);
        if (gp.remainingTimeMillis > 0) {
            spawnRandomOrder();
        }
    }

    public void failOrder(Order o) {
        if (o == null) return;
        gp.score += o.penalty; // penalty di prototype sudah diskalakan multiplier stage
        gp.ordersFailed++;
        activeOrders.remove(o);
        if (gp.remainingTimeMillis > 0) {
            spawnRandomOrder();
        }
    }

    public void completeOrderAtIndex(int idx) {
        if (idx < 0 || idx >= activeOrders.size()) return;
        Order o = activeOrders.get(idx);
        gp.score += o.reward;
        gp.ordersCompleted++;
        activeOrders.remove(idx);
        if (gp.remainingTimeMillis > 0) {
            spawnRandomOrder();
        }
    }

    public void failOrderAtIndex(int idx) {
        if (idx < 0 || idx >= activeOrders.size()) return;
        Order o = activeOrders.get(idx);

        // penalti untuk salah pesanan (bukan expired) juga ikut multiplier stage
        double pm = (gp.stageConfig != null) ? gp.stageConfig.penaltyMultiplier : 1.0;
        int basePenalty = 50;
        int penaltyAmount = (int)Math.round(basePenalty * pm);

        gp.score -= penaltyAmount;
        gp.ordersFailed++;
        activeOrders.remove(idx);
        if (gp.remainingTimeMillis > 0) {
            spawnRandomOrder();
        }
    }
}
