package co.rsk;


import org.ethereum.core.ImportResult;
import org.ethereum.db.BlockInformation;
import org.ethereum.db.BlockStore;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class SnappyMetrics {

    public static final int VALUES_TO_GENERATE = 3000;
    public static final int MIN = 1;
    public static final int MAX = 300000;
    private final BlockStore blockchain;
    private final BlockStore blockchainWithSnappy;

    private SnappyMetrics(RskContext objects, String bc, String bcWithSnappy) {
        this.blockchain = RskContext.buildBlockStore(objects.getBlockFactory(), bc);
        this.blockchainWithSnappy = RskContext.buildBlockStore(objects.getBlockFactory(), bcWithSnappy).setUseSnappy();
    }

    public void compareReads () {
        Random valueSource = new Random();
        IntStream blockNumbers = valueSource.ints(VALUES_TO_GENERATE, MIN, MAX);
        long snappyTime = 0;
        long normalTime = 0;

        for (int anInt : blockNumbers.toArray()) {
            List<BlockInformation> bInfo = blockchain.getBlocksInformationByNumber(anInt);
            List<BlockInformation> bInfoWithSnappy = blockchainWithSnappy.getBlocksInformationByNumber(anInt);

            for (BlockInformation bi : bInfo) {
                long saveTime = System.nanoTime();
                blockchain.getBlockByHash(bi.getHash());
                normalTime += System.nanoTime() - saveTime;

            }

            for (BlockInformation bis : bInfoWithSnappy) {
                long saveTime = System.nanoTime();
                blockchainWithSnappy.getBlockByHash(bis.getHash());
                snappyTime += System.nanoTime() - saveTime;

            }
        }

        System.out.println("Read with normal time: " + normalTime);
        System.out.println("Read with snappy time: " + snappyTime);
    }

    public static void main(String[] args) {
        /*if (args.length == 0) {
            System.out.println("usage: BlockstoreBlockPlayer [<node cli args>] <block store source dir>");
            System.exit(0);
            return;
        }*/

        args = new String[] {"/home/julian/.rsk/mainnet-snappy-test", "/home/julian/workspace/DB-Mainnet/database/mainnet"};

        String[] nodeCliArgs = Arrays.copyOf(args, args.length - 1);
        RskContext objects = new RskContext(nodeCliArgs);
        SnappyMetrics sMetrics = new SnappyMetrics(objects, args[0], args[1]);
        sMetrics.compareReads();
        System.exit(0);
    }
}
