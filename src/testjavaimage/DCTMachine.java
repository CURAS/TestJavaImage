package testjavaimage;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
class DCTMachine {

    //Configuration
    final static private int MAX_THREAD_COUNT = 64;
    final static private int BLOCK_SIZE = 8;

    //type definitions
    private class SyncNum {

        int count;
    }

    private interface WorkerCore {

        double work(double[][] input, double[] buff1, double[] buff2, int x, int y);
    }

    private class IllegalInputException extends Exception {
    }

    //field definitions
    private final SyncNum sync;
    private int M, N;
    private WorkerCore workerCore;

    private int[] zigzag_x;
    private int[] zigzag_y;
    private int zigzag_M;
    private int zigzag_N;

    private double sqrt2;
    private double sqrtMN;

    //entrances and initialization
    public DCTMachine() {
        sync = new SyncNum();
        sqrt2 = Math.sqrt(2);
        zigzag_M = 0;
        zigzag_N = 0;
    }

    public double[][][][] DCT_p(double[][][][] input) {
        boolean success;
        double[][][][] output = null;

        try {
            workerCore = this::DCTCore;
            initialize(input);
            output = new double[input.length][input[0].length][N][M];
            dispatchWorks(input, output);
            success = true;
        } catch (IllegalInputException | InterruptedException e) {
            success = false;
        }
        if (success) {
            return output;
        } else {
            return null;
        }
    }

    public double[][][][] iDCT_p(double[][][][] input) {
        boolean success;
        double[][][][] output = null;

        try {
            workerCore = this::iDCTCore;
            initialize(input);
            output = new double[input.length][input[0].length][N][M];
            dispatchWorks(input, output);
            success = true;
        } catch (IllegalInputException | InterruptedException e) {
            success = false;
        }
        if (success) {
            return output;
        } else {
            return null;
        }
    }

    public double[][] DCT(double[][] input) {
        try {
            initialize(input);
        } catch (IllegalInputException ex) {
            ex.printStackTrace();
        }

        double[][] output = new double[N][M];
        double[] a_buff = new double[M];
        double[] b_buff = new double[N];

        for (int y = 0; y < input.length; y++) {
            for (int x = 0; x < input[0].length; x++) {
                output[x][y] = DCTCore(input, a_buff, b_buff, x, y);
            }
        }
        return output;
    }

    public double[][] iDCT(double[][] input) {
        try {
            initialize(input);
        } catch (IllegalInputException ex) {
            ex.printStackTrace();
        }

        double[][] output = new double[N][M];
        double[] a_buff = new double[M];
        double[] b_buff = new double[N];

        for (int y = 0; y < input.length; y++) {
            for (int x = 0; x < input[0].length; x++) {
                output[x][y] = iDCTCore(input, a_buff, b_buff, x, y);
            }
        }
        return output;
    }

    public double[][] DCT_t(double[][] input) {
        boolean success;
        double[][] output = null;
        try {
            workerCore = this::DCTCore;
            output = new double[N][M];
            initialize(input);
            dispatchWorks(input, output);
            success = true;
        } catch (IllegalInputException | InterruptedException e) {
            success = false;
        }
        if (success) {
            return output;
        } else {
            return null;
        }
    }

    public double[][] iDCT_t(double[][] input) {
        boolean success;
        double[][] output = null;
        try {
            workerCore = this::iDCTCore;
            initialize(input);
            output = new double[N][M];
            dispatchWorks(input, output);
            success = true;
        } catch (IllegalInputException | InterruptedException e) {
            success = false;
        }
        if (success) {
            return output;
        } else {
            return null;
        }
    }

    private void initialize(double[][] input) throws IllegalInputException {
        if (input == null) {
            throw new IllegalInputException();
        } else if (input.length <= 0) {
            throw new IllegalInputException();
        } else if (input[0].length <= 0) {
            throw new IllegalInputException();
        }

        N = input.length;
        M = input[0].length;
        sqrtMN = Math.sqrt(M * N);
        sync.count = 0;
    }

    private void initialize(double[][][][] input) throws IllegalInputException {
        if (input == null) {
            throw new IllegalInputException();
        } else if (input.length <= 0) {
            throw new IllegalInputException();
        } else if (input[0].length <= 0) {
            throw new IllegalInputException();
        }

        N = BLOCK_SIZE;
        M = BLOCK_SIZE;
        sqrtMN = Math.sqrt(M * N);
        sync.count = 0;
    }

    //thread part
    //block-divided dispatcher
    private void dispatchWorks(double[][][][] input, double[][][][] output) throws InterruptedException {
        //initialization
        int i;
        final int length = input.length * input[0].length;
        @SuppressWarnings("unchecked")
        List<Integer>[] tasks = new List[MAX_THREAD_COUNT];
        List<Thread> workers = new ArrayList<>();
        for (i = 0; i < MAX_THREAD_COUNT; i++) {
            tasks[i] = new ArrayList<>();
        }

        //dispatch tasks
        i = 0;
        for (int j = 0; j < length; j++) {
            tasks[i].add(j);
            i = (i + 1) % MAX_THREAD_COUNT;
        }
        for (i = 0; i < MAX_THREAD_COUNT; i++) {
            if (!tasks[i].isEmpty()) {
                final Integer I = i;
                workers.add(new Thread(() -> worker(tasks[I], input, output)));
                sync.count++;
            }
        }

        //start sub threads
        synchronized (sync) {
            for (Thread worker : workers) {
                worker.start();
            }
            sync.wait();
        }
    }

    //row-divided dispatcher
    private void dispatchWorks(double[][] input, double[][] output) throws InterruptedException {
        //initialization
        int i;
        @SuppressWarnings("unchecked")
        List<Integer>[] tasks = new List[MAX_THREAD_COUNT];
        List<Thread> workers = new ArrayList<>();
        for (i = 0; i < MAX_THREAD_COUNT; i++) {
            tasks[i] = new ArrayList<>();
        }

        //dispatch tasks
        i = 0;
        for (int j = 0; j < N; j++) {
            tasks[i].add(j);
            i = (i + 1) % MAX_THREAD_COUNT;
        }
        for (i = 0; i < MAX_THREAD_COUNT; i++) {
            if (!tasks[i].isEmpty()) {
                final Integer I = i;
                workers.add(new Thread(() -> worker(tasks[I], input, output)));
                sync.count++;
            }
        }

        //start sub threads
        synchronized (sync) {
            for (Thread worker : workers) {
                worker.start();
            }
            sync.wait();
        }
    }

    private void end() {
        synchronized (sync) {
            sync.count--;
            if (sync.count == 0) {
                sync.notify();
            }
        }
    }

    //row worker
    private void worker(List<Integer> task, double[][] input, double[][] output) {
        double[] a_buff = new double[M];
        double[] b_buff = new double[N];
        for (Integer row : task) {
            for (int u = 0; u < M; u++) {
                output[row][u] = workerCore.work(input, a_buff, b_buff, u, row);
            }
        }
        end();
    }

    //block worker
    private void worker(List<Integer> task, double[][][][] input, double[][][][] output) {
        double[] a_buff = new double[M];
        double[] b_buff = new double[N];
        for (Integer outterPos : task) {
            for (int y = 0; y < N; y++) {
                for (int x = 0; x < M; x++) {
                    output[seq2pointy(outterPos, input[0].length)][seq2pointx(outterPos, input[0].length)][y][x]
                            = workerCore.work(input[seq2pointy(outterPos, input[0].length)][seq2pointx(outterPos, input[0].length)],
                                    a_buff, b_buff, x, y);
                }
            }
        }
        end();
    }

    //math part
    private double DCTCore(double[][] input, double[] x_buff, double[] y_buff, int u, int v) {
        int x, y;
        double sum = 0d;

        for (x = 0; x < M; x++) {
            x_buff[x] = g(x, u, M);
        }
        for (y = 0; y < N; y++) {
            y_buff[y] = g(y, v, N);
        }

        for (x = 0; x < M; x++) {
            for (y = 0; y < N; y++) {
                sum += input[y][x] * x_buff[x] * y_buff[y];
            }
        }

        sum /= sqrtMN;
        if (u != 0 && v != 0) {
            sum *= 2;
        } else if (u != 0 || v != 0) {
            sum *= sqrt2;
        }
        return sum;
    }

    private double iDCTCore(double[][] input, double[] u_buff, double[] v_buff, int x, int y) {
        int u, v;
        double sum = 0;
        for (u = 0; u < M; u++) {
            u_buff[u] = g(x, u, M);
        }
        for (v = 0; v < N; v++) {
            v_buff[v] = g(y, v, N);
        }

        for (u = 0; u < M; u++) {
            for (v = 0; v < N; v++) {
                if (u == 0 && v == 0) {
                    sum += input[v][u] * u_buff[u] * v_buff[v] / 2d;
                } else if (u == 0 || v == 0) {
                    sum += input[v][u] * u_buff[u] * v_buff[v] / sqrt2;
                } else {
                    sum += input[v][u] * u_buff[u] * v_buff[v];
                }
            }
        }

        sum /= sqrtMN / 2d;
        if (sum > 255d) {
            sum = 255d;
        }
        if (sum < 0d) {
            sum = 0d;
        }

        return sum;
    }

    private double g(int a, int b, int dimension) {
        if (b == 0) {
            return 1.0d;
        } else {
            return Math.cos(Math.PI / 2 / dimension * (2 * a + 1) * b);
        }
    }

    public int[] getZigzag_x(int M, int N) {
        if (M != zigzag_M || N != zigzag_N) {
            generateZigzag(M, N);
        }
        return zigzag_x;
    }

    public int[] getZigzag_y(int M, int N) {
        if (M != zigzag_M || N != zigzag_N) {
            generateZigzag(M, N);
        }
        return zigzag_y;
    }

    private void generateZigzag(int M, int N) {
        this.zigzag_M = M;
        this.zigzag_N = N;
        zigzag_x = new int[M * N];
        zigzag_y = new int[M * N];

        int cursor_x = -1;
        int cursor_y = 1;
        int cursor_zigzag = 0;
        int direction = 1;
        boolean borderFlag = false;

        while (true) {
            //try move
            cursor_x += direction;
            cursor_y -= direction;

            //border check
            if (direction > 0) {
                //right
                if (cursor_x >= M) {
                    if (cursor_y == N - 2) {
                        break;
                    }
                    cursor_y++;
                    borderFlag = true;

                } //top
                else if (cursor_y < 0) {
                    cursor_x++;
                    borderFlag = true;
                }

            } else {
                //bottom
                if (cursor_y >= N) {
                    if (cursor_x == M - 2) {
                        break;
                    }
                    cursor_x++;
                    borderFlag = true;
                } //left
                else if (cursor_x < 0) {
                    cursor_y++;
                    borderFlag = true;
                }
            }
            if (borderFlag) {
                cursor_x -= direction;
                cursor_y += direction;
                direction = -direction;
                borderFlag = false;
            }
            //record
            zigzag_x[cursor_zigzag] = cursor_x;
            zigzag_y[cursor_zigzag++] = cursor_y;
        }
    }

    public static double[][][][] blockPartition(double[][] pixels, int len) {
        final int width = pixels[0].length;
        final int height = pixels.length;

        double[][][][] blocks = new double[height / len][width / len][len][len];

        for (int y = 0; y < height - height % len; y++) {
            for (int x = 0; x < width - width % len; x++) {
                blocks[y / len][x / len][y % len][x % len] = pixels[y][x];
            }
        }

        return blocks;
    }

    public static void blockMerge(double[][][][] blocks, double[][] pixels) {
        final int len = blocks[0][0][0].length;
        final int width = pixels[0].length;
        final int height = pixels.length;

        for (int y = 0; y < height - height % len; y++) {
            for (int x = 0; x < width - width % len; x++) {
                pixels[y][x] = blocks[y / len][x / len][y % len][x % len];
            }
        }
    }

    private static int seq2pointx(int seq, int width) {
        return seq % width;
    }

    private static int seq2pointy(int seq, int width) {
        return seq / width;
    }

}
