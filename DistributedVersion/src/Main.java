import mpi.MPI;

import java.io.BufferedReader;
import java.io.FileReader;

public class Main {

    public static void main(String[] args) throws Exception {

        MPI.Init(args);

        int worldRank = MPI.COMM_WORLD.Rank();
        int worldSize = MPI.COMM_WORLD.Size();

        int[] integerValues = new int[4];
        double[] doubleValues = new double[1];
        long[] seedValue = new long[1];

        if (worldRank == 0) {

            //defaults
            integerValues[0] = 100;
            integerValues[1] = 100;
            integerValues[2] = 10;
            integerValues[3] = 5;

            doubleValues[0] = 0.30;
            seedValue[0] = 42;

            BufferedReader br = new BufferedReader(
                    new FileReader("src/instructions.txt")
            );

            String line;

            line = br.readLine();
            if (line != null && !line.trim().isEmpty()) {

                String[] parts = line.split(" ");

                if (parts.length >= 2) {
                    integerValues[0] = Integer.parseInt(parts[0]);

                    integerValues[1] = Integer.parseInt(parts[1]);
                }
            }

            line = br.readLine();
            if (line != null && !line.trim().isEmpty()) {
                integerValues[2] = Integer.parseInt(line);
            }

            line = br.readLine();
            if (line != null && !line.trim().isEmpty()) {
                doubleValues[0] = Double.parseDouble(line);
            }

            line = br.readLine();
            if (line != null && !line.trim().isEmpty()) {
                integerValues[3] = Integer.parseInt(line);
            }

            line = br.readLine();
            if (line != null && !line.trim().isEmpty()) {
                seedValue[0] = Long.parseLong(line);
            }

            br.close();

            for (int process = 1; process < worldSize; process++) {

                MPI.COMM_WORLD.Send(
                        integerValues,
                        0,
                        4,
                        MPI.INT,
                        process, //send them to the process whose number is currently stored in process
                        0
                );

                MPI.COMM_WORLD.Send(
                        doubleValues,
                        0,
                        1,
                        MPI.DOUBLE,
                        process,
                        1
                );

                MPI.COMM_WORLD.Send(
                        seedValue,
                        0,
                        1,
                        MPI.LONG,
                        process,
                        2
                );
            }

        } else {

            MPI.COMM_WORLD.Recv(
                    integerValues,
                    0,
                    4,
                    MPI.INT,
                    0,
                    0
            );

            MPI.COMM_WORLD.Recv(
                    doubleValues,
                    0,
                    1,
                    MPI.DOUBLE,
                    0,
                    1
            );

            MPI.COMM_WORLD.Recv(
                    seedValue,
                    0,
                    1,
                    MPI.LONG,
                    0,
                    2
            );
        }

        int rows = integerValues[0];
        int cols = integerValues[1];
        int K = integerValues[2];
        int burnTicks = integerValues[3];

        double pSpread = doubleValues[0];
        long seed = seedValue[0];

        Grid grid = new Grid(rows, cols);

        grid.setBurnTicks(burnTicks);
        grid.startSpread(seed + 1, pSpread);

        if (worldRank == 0) {

            grid.startForestWalk(seed);

            while (!grid.oneStep()) {
            }

            grid.igniteRandomForestTiles(K, seed + 2);
        }

        Gui gui = null;

        if (worldRank == 0) {
            gui = new Gui(grid);
        }

        long startTime = System.currentTimeMillis();

        grid.runDistributed(worldRank, worldSize, gui);

        long endTime = System.currentTimeMillis();

        if (worldRank == 0) {
            System.out.println("Simulation time: " + (endTime - startTime) + " ms");
        }

        MPI.Finalize();
    }
}