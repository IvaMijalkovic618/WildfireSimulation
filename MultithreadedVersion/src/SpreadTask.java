import java.util.ArrayList;
import java.util.Random;

public class SpreadTask implements Runnable {

    private final Grid grid;
    private final int startRow;
    private final int endRow;
    private final Random random;

    private final ArrayList<Integer> igniteRows =
            new ArrayList<>();

    private final ArrayList<Integer> igniteCols =
            new ArrayList<>();

    public SpreadTask(
            Grid grid,
            int startRow,
            int endRow,
            long seed
    ) {
        this.grid = grid;
        this.startRow = startRow;
        this.endRow = endRow;
        this.random = new Random(seed);
    }

    //checks only one part of the grid
    @Override
    public void run() {
        grid.findIgnitions(
                startRow,
                endRow,
                random,
                igniteRows,
                igniteCols
        );
    }

    public ArrayList<Integer> getIgniteRows() {
        return igniteRows;
    }

    public ArrayList<Integer> getIgniteCols() {
        return igniteCols;
    }
}