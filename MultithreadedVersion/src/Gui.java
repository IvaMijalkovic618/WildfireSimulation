import javax.swing.*;
import java.awt.*;

public class Gui {

    private final JFrame frame;
    private final JPanel panel;
    private final Grid myGrid;

    private Timer fireTimer;

    private long startingTime;
    private int tickCount;

    private final int K;
    private final long igniteSeed;

    public Gui(Grid myGrid, int K, long igniteSeed) {
        this.myGrid = myGrid;
        this.K = K;
        this.igniteSeed = igniteSeed;

        frame = new JFrame("Wildfire Simulation");
        panel = new JPanel();

        frame.setDefaultCloseOperation(
                WindowConstants.EXIT_ON_CLOSE
        );

        frame.setSize(500, 500);
        frame.setLocationRelativeTo(null);
        frame.add(panel);
        frame.setVisible(true);

        fireTimer = new Timer(70, e -> {
            boolean finished = myGrid.spreadOneTick();
            tickCount++;
            drawGrid();

            if (finished) {
                fireTimer.stop();
                myGrid.shutdown();

                long endingTime =
                        System.currentTimeMillis();

                long totalTime =
                        endingTime - startingTime;

                System.out.println(
                        "Ticks until extinction: " +
                                tickCount
                );

                System.out.println(
                        "Simulation time: " +
                                totalTime +
                                "ms"
                );
            }
        });

        myGrid.igniteRandomForestTiles(K, igniteSeed);
        drawGrid();

        startingTime = System.currentTimeMillis();
        tickCount = 0;
        fireTimer.start();
    }

    private void drawGrid() {
        Graphics g = panel.getGraphics();

        if (g == null) {
            return;
        }

        int rows = myGrid.rows;
        int cols = myGrid.cols;

        double cellW =
                (double) panel.getWidth() / cols;

        double cellH =
                (double) panel.getHeight() / rows;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                int state = myGrid.grid[r][c];

                if (state == Grid.BARE) {
                    g.setColor(Color.WHITE);
                } else if (state == Grid.FOREST) {
                    g.setColor(Color.GREEN);
                } else if (state == Grid.BURNING) {
                    g.setColor(Color.RED);
                } else if (state == Grid.BURNED) {
                    g.setColor(Color.GRAY);
                }

                int x = (int) (c * cellW);
                int y = (int) (r * cellH);
                int w = (int) Math.ceil(cellW);
                int h = (int) Math.ceil(cellH);

                g.fillRect(x, y, w, h);
            }
        }
    }
}