import java.io.BufferedReader;
import java.io.FileReader;

public class Main {
    public static void main(String[] args) throws Exception {

        //defaults
        int rows = 100;
        int cols = 100;
        int K = 10;
        double pSpread = 0.30;
        int burnTicks = 5;
        long seed = 42;

        BufferedReader br = new BufferedReader(
                new FileReader("src/instructions.txt")
        );

        String line;

        line = br.readLine();
        if (line != null && !line.trim().isEmpty()) {
            String[] parts = line.split(" ");
            if (parts.length >= 2) {
                rows = Integer.parseInt(parts[0]);
                cols = Integer.parseInt(parts[1]);
            }
        }

        line = br.readLine();
        if (line != null && !line.trim().isEmpty()) {
            K = Integer.parseInt(line);
        }

        line = br.readLine();
        if (line != null && !line.trim().isEmpty()) {
            pSpread = Double.parseDouble(line);
        }

        line = br.readLine();
        if (line != null && !line.trim().isEmpty()) {
            burnTicks = Integer.parseInt(line);
        }

        line = br.readLine();
        if (line != null && !line.trim().isEmpty()) {
            seed = Long.parseLong(line);
        }

        br.close();

        Grid grid = new Grid(rows, cols);
        grid.startForestWalk(seed);

        while (!grid.oneStep()) {
        }

        grid.setBurnTicks(burnTicks);
        grid.startSpread(seed + 1, pSpread);

        new Gui(grid, K, seed + 2);
    }
}