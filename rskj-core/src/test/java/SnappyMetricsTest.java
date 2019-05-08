import co.rsk.RskContext;
import co.rsk.SnappyMetrics;
import org.junit.Test;

import java.util.Arrays;

public class SnappyMetricsTest {

    @Test
    public void test1() {
        System.gc();
        String [] args = new String[] {"/home/julian/.rsk/mainnet-snappy", "/home/julian/.rsk/mainnet-test"};
        String[] nodeCliArgs = Arrays.copyOf(args, args.length - 1);
        RskContext objects = new RskContext(nodeCliArgs);
        SnappyMetrics sMetrics = new SnappyMetrics(objects, args[0], args[1], true, true, 100);
        sMetrics.runExperiment(50, 3000);
    }

}
