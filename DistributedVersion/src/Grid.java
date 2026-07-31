import java.util.ArrayList;
import java.util.Random;
import mpi.MPI;

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

    private int[] makeGridArray() {
        int[] gridArray = new int[rows * cols];
        int position = 0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {

                gridArray[position] = grid[i][j];
                position++;
            }
        }
        return gridArray;
    }

    private void copyArrayToGrid(int[] gridArray) {

        int position = 0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {

                grid[i][j] = gridArray[position];
                position++;
            }
        }
    }

    private void findIgnitionsInRows(int startRow, int endRow, ArrayList<Integer> igniteRows, ArrayList<Integer> igniteCols) {

        for (int i = startRow; i < endRow; i++) {
            for (int j = 0; j < cols; j++) {

                if (grid[i][j] == BURNING) {

                    for (int dr = -1; dr <= 1; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {  //The possible values are: -1, 0, 1 Together they check positions around the burning cell: top-left top top-right left itself right bottom-left bottom bottom-right

                            if (dr == 0 && dc == 0) {
                                continue;
                            }

                            int nr = i + dr;
                            int nc = j + dc;

                            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) {
                                continue;
                            }

                            if (grid[nr][nc] == FOREST) {

                                if (spreadRnd.nextDouble() < pSpread) {
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

    public void runDistributed(int worldRank, int worldSize, Gui gui) {
        boolean finished = false;
        int tickCount = 0;

        while (!finished) {
            tickCount++;

            int[] gridArray = new int[rows * cols];

            if (worldRank == 0) {

                gridArray = makeGridArray();

                for (int process = 1; process < worldSize; process++) {

                    MPI.COMM_WORLD.Send(
                            gridArray,
                            0,
                            gridArray.length,
                            MPI.INT,
                            process,
                            10
                    );
                }

            } else {

                MPI.COMM_WORLD.Recv(
                        gridArray,
                        0,
                        gridArray.length,
                        MPI.INT,
                        0,
                        10
                );

                copyArrayToGrid(gridArray);
            }

            int rowsPerProcess = rows / worldSize;

            int startRow = worldRank * rowsPerProcess;
            int endRow;

            if (worldRank == worldSize - 1) {
                endRow = rows;
            } else {
                endRow = startRow + rowsPerProcess;
            }

            ArrayList<Integer> localIgniteRows = new ArrayList<>();

            ArrayList<Integer> localIgniteCols = new ArrayList<>();

            findIgnitionsInRows(startRow, endRow, localIgniteRows, localIgniteCols);

            ArrayList<Integer> allIgniteRows = new ArrayList<>();
            ArrayList<Integer> allIgniteCols = new ArrayList<>();

            if (worldRank == 0) {

                for (int i = 0; i < localIgniteRows.size(); i++) {
                    allIgniteRows.add(localIgniteRows.get(i));
                    allIgniteCols.add(localIgniteCols.get(i));
                }

                for (int process = 1; process < worldSize; process++) {

                    int[] receivedCount = new int[1];

                    MPI.COMM_WORLD.Recv(
                            receivedCount,
                            0,
                            1,
                            MPI.INT,
                            process,
                            11
                    );

                    int count = receivedCount[0];

                    if (count > 0) {

                        int[] receivedRows = new int[count];
                        int[] receivedCols = new int[count];

                        MPI.COMM_WORLD.Recv(
                                receivedRows,
                                0,
                                count,
                                MPI.INT,
                                process,
                                12
                        );

                        MPI.COMM_WORLD.Recv(
                                receivedCols,
                                0,
                                count,
                                MPI.INT,
                                process,
                                13
                        );

                        for (int i = 0; i < count; i++) {
                            allIgniteRows.add(receivedRows[i]);
                            allIgniteCols.add(receivedCols[i]);
                        }
                    }
                }

            } else {

                int count = localIgniteRows.size();

                int[] countArray = new int[1];
                countArray[0] = count;

                MPI.COMM_WORLD.Send(
                        countArray,
                        0,
                        1,
                        MPI.INT,
                        0,
                        11
                );

                if (count > 0) {

                    int[] sendRows = new int[count];
                    int[] sendCols = new int[count];

                    for (int i = 0; i < count; i++) {
                        sendRows[i] = localIgniteRows.get(i);
                        sendCols[i] = localIgniteCols.get(i);
                    }

                    MPI.COMM_WORLD.Send(
                            sendRows,
                            0,
                            count,
                            MPI.INT,
                            0,
                            12
                    );

                    MPI.COMM_WORLD.Send(
                            sendCols,
                            0,
                            count,
                            MPI.INT,
                            0,
                            13
                    );
                }
            }

            if (worldRank == 0) {

                // Apply all new ignitions.
                for (int i = 0; i < allIgniteRows.size(); i++) {

                    int igniteRow = allIgniteRows.get(i);
                    int igniteCol = allIgniteCols.get(i);

                    if (grid[igniteRow][igniteCol] == FOREST) {
                        grid[igniteRow][igniteCol] = BURNING;
                        burnTimer[igniteRow][igniteCol] = burnTicks;
                    }
                }

                int burningCount = 0;

                // Update the burning time of every burning cell.
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
                    finished = true;
                }
                if (worldRank == 0 && gui != null) {
                    gui.drawGrid();

                    try {
                        Thread.sleep(70);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            int[] finishedArray = new int[1];

            if (worldRank == 0) {

                if (finished) {
                    finishedArray[0] = 1;
                } else {
                    finishedArray[0] = 0;
                }

            }

            MPI.COMM_WORLD.Bcast(
                    finishedArray,
                    0,
                    1,
                    MPI.INT,
                    0
            );

            if (finishedArray[0] == 1) {
                finished = true;
            }
        } if (worldRank == 0) {
                System.out.println(
                        "Tick count: " + tickCount
                );
            }
    }

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

    }

    public void setBurnTicks(int burnTicks) {
        this.burnTicks = burnTicks;
    }


    public void startForestWalk(long seed) {
        random = new Random(seed);

        int totalTiles = rows * cols;
        targetForest = totalTiles / 2;
        forestCount = 0;

        r = random.nextInt(rows); //random starting tile
        c = random.nextInt(cols);

        if (grid[r][c] == BARE) {
            grid[r][c] = FOREST;
            forestCount++;
        }
    }

    //one step of the random walk
    public boolean oneStep() {
        if (forestCount >= targetForest) return true;

        int dir = random.nextInt(4);
        if (dir == UP) r--;
        else if (dir == DOWN) r++;
        else if (dir == LEFT) c--;
        else c++;

        if (r < 0) r = 0;
        if (r >= rows) r = rows - 1;
        if (c < 0) c = 0;
        if (c >= cols) c = cols - 1;

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

    public boolean spreadOneTick() {
        ArrayList<Integer> igniteR = new ArrayList<>();
        ArrayList<Integer> igniteC = new ArrayList<>();

        //decide ignitions based on current burning tiles
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j] == BURNING) {

                    for (int dr = -1; dr <= 1; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {
                            if (dr == 0 && dc == 0) continue; // scan the neighbours, skip itself

                            int nr = i + dr;
                            int nc = j + dc;

                            if (nr < 0) nr = 0;
                            if (nr >= rows) nr = rows - 1;
                            if (nc < 0) nc = 0;
                            if (nc >= cols) nc = cols - 1;

                            if (grid[nr][nc] == FOREST) {
                                if (spreadRnd.nextDouble() < pSpread) { //condition is true 30% of the time
                                    igniteR.add(nr);
                                    igniteC.add(nc);

                                }
                            }
                        }
                    }
                }
            }
        }

        // apply ignitions
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


}