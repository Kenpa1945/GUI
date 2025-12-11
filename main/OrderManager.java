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
        recipePrototypes = new ArrayList<>();
        // time limit example: 45 seconds; reward/penalty example values
        recipePrototypes.add(new Order(0, "Classic Burger", Arrays.asList("bun","cooked_meat"), 120, -50, 45));
        recipePrototypes.add(new Order(0, "Cheeseburger", Arrays.asList("bun","cooked_meat","chopped_cheese"), 150, -60, 50));
        recipePrototypes.add(new Order(0, "BLT Burger", Arrays.asList("bun","chopped_lettuce","chopped_tomato","cooked_meat"), 170, -70, 55));
        recipePrototypes.add(new Order(0, "Deluxe Burger", Arrays.asList("bun","chopped_lettuce","cooked_meat","chopped_cheese"), 200, -80, 60));
    }

    // spawn until max 3
    public void trySpawnInitial() {
        while (activeOrders.size() < 3) spawnRandomOrder();
    }

    public void spawnRandomOrder() {
        if (gp.remainingTimeMillis <= 0) return; // no more spawning if game time ended
        int idx = rnd.nextInt(recipePrototypes.size());
        Order proto = recipePrototypes.get(idx);
        Order o = new Order(nextOrderPosition++, proto.recipeName, proto.requiredItems, proto.reward, proto.penalty, proto.timeLimitSeconds);
        activeOrders.add(o);
    }

    public void update(long deltaMillis) {
        if (activeOrders.isEmpty()) return;
        // update all orders
        List<Order> expired = new ArrayList<>();
        for (Order o : activeOrders) {
            o.update(deltaMillis);
            if (o.remainingMillis <= 0) expired.add(o);
        }
        // handle expired (penalty, remove, spawn new)
        for (Order e : expired) {
            gp.score += e.penalty; // apply penalty
            activeOrders.remove(e);
            // spawn new only if game still running
            if (gp.remainingTimeMillis > 0) spawnRandomOrder();
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

    private boolean multisetEquals(List<String> a, List<String> b) {
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
        // increment completed counter on GamePanel
        gp.ordersCompleted++;
        activeOrders.remove(o);
        if (gp.remainingTimeMillis > 0) spawnRandomOrder();
    }
    
    public void failOrder(Order o) {
        if (o == null) return;
        gp.score += o.penalty;
        // increment failed counter on GamePanel
        gp.ordersFailed++;
        activeOrders.remove(o);
        if (gp.remainingTimeMillis > 0) spawnRandomOrder();
    }
    
}
