import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Grid {
    public final int rows;
    public final int cols;
    public final int[][] grid;

    //tile states
    public static final int BARE = 1;
    public static final int FOREST = 2;
    public static final int BURNING = 3;
    public static final int BURNED = 4;

    //directions of a random walk
    private static final int UP = 0;
    private static final int DOWN = 1;
    private static final int LEFT = 2;
    private static final int RIGHT = 3;

    //random walk variables
    private Random random;
    private int r, c; //walker's current position
    private int forestCount;
    private int targetForest;

    private int burnTicks;
    private int[][] burnTimer;
    private Random spreadRnd;
    public double pSpread;

    private final int numberOfThreads;
    private final ExecutorService executor;

    public Grid(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.grid = new int[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = BARE;
            }
        }

        this.burnTimer = new int[rows][cols];

        numberOfThreads =
                Runtime.getRuntime().availableProcessors();

        executor =
                Executors.newFixedThreadPool(numberOfThreads);
    }

    public void setBurnTicks(int burnTicks) {
        this.burnTicks = burnTicks;
    }

    public void startForestWalk(long seed) {
        random = new Random(seed);

        int totalTiles = rows * cols;
        targetForest = totalTiles / 2;
        forestCount = 0;

        //random starting tile
        r = random.nextInt(rows);
        c = random.nextInt(cols);

        if (grid[r][c] == BARE) {
            grid[r][c] = FOREST;
            forestCount++;
        }
    }

    //one step of the random walk
    public boolean oneStep() {
        if (forestCount >= targetForest) {
            return true;
        }

        int dir = random.nextInt(4);

        if (dir == UP) {
            r--;
        } else if (dir == DOWN) {
            r++;
        } else if (dir == LEFT) {
            c--;
        } else {
            c++;
        }

        if (r < 0) {
            r = 0;
        }

        if (r >= rows) {
            r = rows - 1;
        }

        if (c < 0) {
            c = 0;
        }

        if (c >= cols) {
            c = cols - 1;
        }

        if (grid[r][c] == BARE) {
            grid[r][c] = FOREST;
            forestCount++;
        }

        return false;
    }

    public void igniteRandomForestTiles(int K, long seed) {
        Random rnd = new Random(seed);
        int forestTiles = 0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j] == FOREST) {
                    forestTiles++;
                }
            }
        }

        if (K > forestTiles) {
            K = forestTiles;
        }

        while (K > 0) {
            int r = rnd.nextInt(rows);
            int c = rnd.nextInt(cols);

            if (grid[r][c] == FOREST) {
                grid[r][c] = BURNING;
                burnTimer[r][c] = burnTicks;
                K--;
            }
        }
    }

    public void startSpread(long seed, double pSpread) {
        this.spreadRnd = new Random(seed);
        this.pSpread = pSpread;
    }

    public void findIgnitions(
            int startRow,
            int endRow,
            Random random,
            ArrayList<Integer> igniteRows,
            ArrayList<Integer> igniteCols
    ) {
        for (int i = startRow; i < endRow; i++) {
            for (int j = 0; j < cols; j++) {

                if (grid[i][j] == BURNING) {

                    for (int dr = -1; dr <= 1; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {

                            if (dr == 0 && dc == 0) {
                                continue;
                            }

                            int nr = i + dr;
                            int nc = j + dc;

                            if (nr < 0 || nr >= rows ||
                                    nc < 0 || nc >= cols) {
                                continue;
                            }

                            if (grid[nr][nc] == FOREST) {
                                if (random.nextDouble() < pSpread) {
                                    igniteRows.add(nr);
                                    igniteCols.add(nc);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean spreadOneTick() {
        ArrayList<Integer> igniteR = new ArrayList<>();
        ArrayList<Integer> igniteC = new ArrayList<>();

        SpreadTask[] tasks =                        //one task for every thread
                new SpreadTask[numberOfThreads];

        Future<?>[] futures =                       // wait until every task is finished
                new Future[numberOfThreads];

        int rowsPerThread = rows / numberOfThreads;

        for (int i = 0; i < numberOfThreads; i++) {

            int startRow = i * rowsPerThread;
            int endRow;

            if (i == numberOfThreads - 1) {
                endRow = rows;
            } else {
                endRow = startRow + rowsPerThread;
            }

            tasks[i] = new SpreadTask(
                    this,
                    startRow,
                    endRow,
                    spreadRnd.nextLong()
            );

            futures[i] = executor.submit(tasks[i]);
        }

        for (int i = 0; i < numberOfThreads; i++) {
            try {
                futures[i].get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        for (int i = 0; i < numberOfThreads; i++) {
            for (int j = 0;
                 j < tasks[i].getIgniteRows().size();
                 j++) {

                igniteR.add(
                        tasks[i].getIgniteRows().get(j)
                );

                igniteC.add(
                        tasks[i].getIgniteCols().get(j)
                );
            }
        }

        //apply ignitions
        for (int i = 0; i < igniteR.size(); i++) {
            int rr = igniteR.get(i);
            int cc = igniteC.get(i);

            if (grid[rr][cc] == FOREST) {
                grid[rr][cc] = BURNING;
                burnTimer[rr][cc] = burnTicks;
            }
        }

        int burningCount = 0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {

                if (grid[i][j] == BURNING) {
                    burnTimer[i][j]--;

                    if (burnTimer[i][j] <= 0) {
                        grid[i][j] = BURNED;
                    } else {
                        burningCount++;
                    }
                }
            }
        }

        if (burningCount == 0) {
            return true;
        } else {
            return false;
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}