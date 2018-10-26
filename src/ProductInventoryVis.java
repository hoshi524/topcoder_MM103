import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.*;
import java.util.Arrays;
import java.util.Random;
import javax.imageio.ImageIO;

public class ProductInventoryVis {

    private static boolean cheat = false;
    private Process proc;
    private OutputStream os;
    private InputStream is;
    private BufferedReader br;

    private int profit = 0;
    private int day = 0;
    private int itemCount;
    private int custCount;
    private int dayWidth;
    private BufferedImage image;
    private Color[] colors;

    private Random r;
    private int scoreDays;
    private int[] buy;
    private int[] sell;
    private int[] expiration;
    private boolean[] dead;
    private double[][] purchase;
    private double[] annoyance;
    private int[] purchases;
    private int[] sellToCustomer;
    private int[][] inventory;
    private boolean[] unsatisfiedCustomer;
    private int[] unsatisfiedProduct;

    private void generateTestCase(long x) {
        try {
            r = SecureRandom.getInstance("SHA1PRNG");
            r.setSeed(x);
        } catch (Exception e) {
            r = new Random(x);
        }
        itemCount = r.nextInt(91) + 10;
        custCount = r.nextInt(51) + 50;
        if (x < 5) {
            itemCount = custCount = (int) x * 5;
        }
        scoreDays = r.nextInt(91) + 10;
        buy = new int[itemCount];
        sell = new int[itemCount];
        expiration = new int[itemCount];
        purchases = new int[itemCount];
        sellToCustomer = new int[itemCount];
        inventory = new int[16][itemCount];
        annoyance = new double[custCount];
        purchase = new double[custCount][itemCount];
        unsatisfiedCustomer = new boolean[custCount];
        dead = new boolean[custCount];
        colors = new Color[itemCount];
        unsatisfiedProduct = new int[itemCount];

        Random cr = new Random(0);
        for (int i = 0; i < itemCount; i++) {
            buy[i] = r.nextInt(10) + 1;
            sell[i] = buy[i] / 2 + r.nextInt(1 + 3 * buy[i] - buy[i] / 2);
            expiration[i] = r.nextInt(16);
            colors[i] = new Color(cr.nextInt(256), cr.nextInt(256), cr.nextInt(256));
        }
        for (int i = 0; i < custCount; i++) {
            annoyance[i] = r.nextDouble() * 0.1;
            for (int j = 0; j < itemCount; j++) {
                purchase[i][j] = r.nextDouble() * 0.1;
                for (int k = 0; k < 10; k++) {
                    if (r.nextDouble() < purchase[i][j]) {
                        purchases[j]++;
                    }
                }
            }
        }
    }

    private boolean tryBuy(int itemNumber) {
        if (cheat) return true;
        for (int i = 0; i < 16; i++) {
            if (inventory[i][itemNumber] > 0) {
                inventory[i][itemNumber]--;
                return true;
            }
        }
        return false;
    }

    private int processDay(int[] order) {
        int ret = 0;
        if (!cheat) {
            for (int i = 0; i < order.length; i++) {
                ret -= buy[i] * order[i];
                inventory[expiration[i]][i] = order[i];
            }
        }
        Arrays.fill(purchases, 0);
        Arrays.fill(unsatisfiedCustomer, false);
        Arrays.fill(sellToCustomer, 0);
        Arrays.fill(unsatisfiedProduct, 0);
        for (int i = 0; i < dead.length; i++) {
            for (int j = 0; j < purchase[i].length; j++) {
			    double r1 = r.nextDouble();
				double r2 = r.nextDouble();
                if (dead[i]) continue;
                if (r1 < purchase[i][j]) {
                    if (tryBuy(j)) {
                        sellToCustomer[j]++;
                        if(cheat) ret -= buy[j];
                        ret += sell[j];
                        purchases[j]++;
                    } else {
                        unsatisfiedProduct[j]++;
                        if (r2 < annoyance[i]) {
                            // if (!dead[i]) System.out.printf("Day: %d, Unsatisfied customer: %d\n", day, i);
                            dead[i] = true;
                            unsatisfiedCustomer[i] = true;
                        }
                    }
                }
            }
        }
        for (int i = 0; i < 15; i++) {
            inventory[i] = inventory[i + 1];
        }
        inventory[15] = new int[buy.length];
        day++;
        return ret;
    }

    public int runTest(long seed, String exec, String save) throws IOException {
        generateTestCase(seed);
        try {
            proc = Runtime.getRuntime().exec(exec);
            os = proc.getOutputStream();
            is = proc.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));
            new ErrorReader(proc.getErrorStream()).start();

            if (save != null) {
                dayWidth = dead.length;
                image = new BufferedImage(20 + scoreDays * dayWidth, 20 + (1 + purchases.length) * 10, BufferedImage.TYPE_INT_ARGB);
            }
            callInit(buy, sell, expiration);
            for (int k = 0; k < scoreDays; k++) {
                int[] ret = callOrder(purchases);
                if (ret.length != buy.length) {
                    System.out.println("return value from order() is the wrong length: " + ret.length + " (expected " + buy.length + ")");
                    return -1;
                }
                for (int i = 0; i < ret.length; i++) {
                    if (ret[i] < 0 || ret[i] > 100) {
                        System.out.println("Element " + i + " of return value from order() must be between 0-100: received " + ret[i]);
                        return -1;
                    }
                }
                profit += processDay(ret);
                if (save != null) {
                    visualize();
                }
            }
        } finally {
            if (proc != null) {
                proc.destroy();
                br.close();
            }
        }
        if (save != null) {
            ImageIO.write(image, "png", new File(save));
        }
        return Math.max(profit, 0);
    }

    private void callInit(int[] buy, int[] sell, int[] expiration) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(buy.length).append('\n');
        for (int i = 0; i < buy.length; i++) {
            sb.append(buy[i]).append('\n');
        }
        sb.append(sell.length).append('\n');
        for (int i = 0; i < sell.length; i++) {
            sb.append(sell[i]).append('\n');
        }
        sb.append(expiration.length).append('\n');
        for (int i = 0; i < expiration.length; i++) {
            sb.append(expiration[i]).append('\n');
        }

        os.write(sb.toString().getBytes());
        os.flush();

        Integer.parseInt(br.readLine());
    }

    private int[] callOrder(int[] purchases) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(purchases.length).append('\n');
        for (int i = 0; i < purchases.length; i++) {
            sb.append(purchases[i]).append('\n');
        }
        os.write(sb.toString().getBytes());
        os.flush();

        int n = Integer.parseInt(br.readLine());
        int[] res = new int[n];
        for (int i = 0; i < n; i++) {
            res[i] = Integer.parseInt(br.readLine());
        }
        return res;
    }

    private void visualize() {
        Graphics2D g = image.createGraphics();

        for (int i = 0; i < sellToCustomer.length; i++) {
            int w = 5 * dayWidth * sellToCustomer[i] / unsatisfiedCustomer.length;
            g.setColor(colors[i]);
            g.fillRect(10 + (day - 1) * dayWidth, 10 + i * 10, w, 10);

            if (unsatisfiedProduct[i] > 0) {
                g.drawRect(10 + (day - 1) * dayWidth + w, 10 + i * 10, 5 * dayWidth * unsatisfiedProduct[i] / unsatisfiedCustomer.length, 10);
            }
        }

        String unhappy = "\u2639";
        StringBuilder unhappies = new StringBuilder();
        for (int i = 0; i < unsatisfiedCustomer.length; i++) {
            if (unsatisfiedCustomer[i]) {
                unhappies.append(unhappy);
            }
        }

        g.setColor(Color.RED);
        g.drawString(unhappies.toString(), 10 + (day - 1) * dayWidth, 10 + (1 + sellToCustomer.length) * 10);

        g.setColor(Color.BLACK);
        g.drawString(String.valueOf(day - 1), 20 + (day - 1) * dayWidth, 12);
        g.drawString(String.valueOf(custCount), 20 + (day - 1) * dayWidth, 24);
        g.drawString(String.valueOf(profit), 20 + (day - 1) * dayWidth, 35);
        if (day < scoreDays) {
            g.drawLine(10 + day * dayWidth, 0, 10 + day * dayWidth, image.getHeight());
        }
    }

    public static void main(String[] args) throws IOException {
        long seed = 1;
        String exec = null;
        String save = null;
        for (int i = 0; i < args.length; ++i) {
            if ("-seed".equals(args[i])) {
                seed = Long.parseLong(args[++i]);
            } else if ("-exec".equals(args[i])) {
                exec = args[++i];
            } else if ("-save".equals(args[i])) {
                save = String.format("img/%d.png", seed);
            } else if ("-cheat".equals(args[i])) {
                cheat = true;
            }
        }
        System.out.println(new ProductInventoryVis().runTest(seed, exec, save));
    }

    void tr(Object... o) {
        System.out.println(Arrays.deepToString(o));
    }
}

class ErrorReader extends Thread {

    private final InputStream error;

    public ErrorReader(InputStream is) {
        error = is;
    }

    @Override
    public void run() {
        try {
            byte[] ch = new byte[50000];
            int read;
            while ((read = error.read(ch)) > 0) {
                String s = new String(ch, 0, read);
                System.out.print(s);
                System.out.flush();
            }
        } catch (IOException e) {
        }
    }
}
