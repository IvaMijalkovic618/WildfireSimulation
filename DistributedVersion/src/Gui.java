import javax.swing.*;
import java.awt.*;

public class Gui {

    private final JFrame frame;
    private final JPanel panel;
    private final Grid myGrid;

    public Gui(Grid myGrid) {
        this.myGrid = myGrid;

        frame = new JFrame("Distributed Wildfire Simulation");
        panel = new JPanel();

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(500, 500);
        frame.setLocationRelativeTo(null);
        frame.add(panel);
        frame.setVisible(true);
    }

    public void drawGrid() {
        Graphics g = panel.getGraphics();

        if (g == null) {
            return;
        }

        int rows = myGrid.rows;
        int cols = myGrid.cols;

        double cellW = (double) panel.getWidth() / cols;
        double cellH = (double) panel.getHeight() / rows;

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