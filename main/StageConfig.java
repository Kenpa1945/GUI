package main;

public class StageConfig {

    public final int stageId;

    public final int gameDurationSeconds; 
    public final int maxActiveOrders;
    public final double orderSpawnIntervalMultiplier;
    public final double rewardMultiplier;
    public final double penaltyMultiplier;

    public StageConfig(int id, int duration, int maxOrders,
                       double spawnMul, double rewardMul, double penaltyMul) {
        this.stageId = id;
        this.gameDurationSeconds = duration;
        this.maxActiveOrders = maxOrders;
        this.orderSpawnIntervalMultiplier = spawnMul;
        this.rewardMultiplier = rewardMul;
        this.penaltyMultiplier = penaltyMul;
    }

    // STATIC PRESET
    public static StageConfig forStage(int stageId) {
        switch(stageId) {
            case 2:
                return new StageConfig(
                        2,
                        120,          // 2 menit
                        4,            // maksimal 4 order aktif (lebih banyak)
                        0.8,          // spawn lebih cepat
                        1.1,          // reward naik sedikit
                        1.3           // penalty lebih besar
                );
            case 3:
                return new StageConfig(
                        3,
                        90,           // 1.5 menit
                        5,            // maksimal 5 order aktif
                        0.6,          // spawn jauh lebih cepat
                        1.2,          // reward lebih tinggi
                        1.5           // penalty jauh lebih besar
                );
            default:
            case 1:
                return new StageConfig(
                        1,
                        180,          // 3 menit
                        3,            // maksimal 3 order aktif
                        1.0,          // normal spawn
                        1.0,          // normal reward
                        1.0           // normal penalty
                );
        }
    }
}
