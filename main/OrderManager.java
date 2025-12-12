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

    // recipes definitions (name -> required items)
    // Note: requiredItems MUST match item keys used everywhere (e.g. "bun","cooked_meat","chopped_cheese"...)
    private final List<Order> recipePrototypes;

    public OrderManager(GamePanel gp) {
        this.gp = gp;

        // Ambil multiplier difficulty dari StageConfig (kalau belum ada, pakai 1.0)
        double r = (gp.stageConfig != null) ? gp.stageConfig.rewardMultiplier  : 1.0;
        double p = (gp.stageConfig != null) ? gp.stageConfig.penaltyMultiplier : 1.0;

        recipePrototypes = new ArrayList<>();

        // Base value
        int baseRewardClassic   = 120;
        int basePenaltyClassic  = -50;
        int baseRewardCheese    = 150;
        int basePenaltyCheese   = -60;
        int baseRewardBLT       = 170;
        int basePenaltyBLT      = -70;
        int baseRewardDeluxe    = 200;
        int basePenaltyDeluxe   = -80;

        // Pakai multiplier stage untuk reward & penalty
        recipePrototypes.add(new Order(
                0,
                "Classic Burger",
                Arrays.asList("bun","cooked_meat"),
                (int)Math.round(baseRewardClassic * r),
                (int)Math.round(basePenaltyClassic * p),
                45
        ));
        recipePrototypes.add(new Order(
                0,
                "Cheeseburger",
                Arrays.asList("bun","cooked_meat","chopped_cheese"),
                (int)Math.round(baseRewardCheese * r),
                (int)Math.round(basePenaltyCheese * p),
                50
        ));
        recipePrototypes.add(new Order(
                0,
                "BLT Burger",
                Arrays.asList("bun","chopped_lettuce","chopped_tomato","cooked_meat"),
                (int)Math.round(baseRewardBLT * r),
                (int)Math.round(basePenaltyBLT * p),
                55
        ));
        recipePrototypes.add(new Order(
                0,
                "Deluxe Burger",
                Arrays.asList("bun","chopped_lettuce","cooked_meat","chopped_cheese"),
                (int)Math.round(baseRewardDeluxe * r),
                (int)Math.round(basePenaltyDeluxe * p),
                60
        ));
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
