package co.rsk;


import co.rsk.net.BlockProcessResult;
import org.ethereum.core.Block;
import org.ethereum.core.Blockchain;
import org.ethereum.core.ImportResult;
import org.ethereum.db.BlockInformation;
import org.ethereum.db.BlockStore;


import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class SnappyMetrics {

    public static final int MIN = 1;
    public static final int MAX = 300000;

    private final String[] normalDummyPath;
    private final String[] snappyDummyPath;

    public int VALUES_TO_GENERATE;
    private BlockStore blockchain;
    private BlockStore blockchainWithSnappy;
    private Blockchain snappyDummyBlockchain;
    private Blockchain normalDummyBlockchain;
    private int seed;
    private boolean useSnappy;
    private boolean rW;

    public SnappyMetrics(RskContext objects, String bcWithSnappy, String bcNormal, boolean useSnappy, boolean readWrite, int aSeed) {

        this.rW = readWrite;
        this.useSnappy = useSnappy;
        this.normalDummyPath = new String[] {"-base-path", "/home/julian/.rsk/normal-dummy-test"};
        this.snappyDummyPath = new String[] {"-base-path", "/home/julian/.rsk/snappy-dummy-test"};
        this.seed = aSeed;

        if (useSnappy){
            this.blockchainWithSnappy = RskContext.buildBlockStore(objects.getBlockFactory(), bcWithSnappy).setUseSnappy();
        } else {
            this.blockchain = RskContext.buildBlockStore(objects.getBlockFactory(), bcNormal);
        }

        if (!rW) {
            if (useSnappy){
                RskContext rskContextSnappyDummy = new RskContext(snappyDummyPath);
                rskContextSnappyDummy.getBlockStore().setUseSnappy();
                this.snappyDummyBlockchain = rskContextSnappyDummy.getBlockchain();
            } else {
                RskContext rskContextNormalDummy = new RskContext(normalDummyPath);
                rskContextNormalDummy.getBlockStore();
                this.normalDummyBlockchain = rskContextNormalDummy.getBlockchain();
            }
        }
    }

    public void runExperiment(int times, int valuesToGenerate) {
        long totalTime = 0;
        VALUES_TO_GENERATE = valuesToGenerate;

        if (useSnappy) {
            System.out.println("-- Run experiments with Snappy: " + times + " times --");
            if (rW) {
                System.out.println("You are measuring read");
                for (int i = 0; i < times; i++){
                    totalTime += measureSnappyRead();
                }
            } else {
                System.out.println("You are measuring write");
                for (int i = 0; i < times; i++){
                    totalTime += measureSnappyWrites();
                }
            }
        } else {
            System.out.println("-- Run experiments with Normal: " + times + " times --");
            if (rW) {
                System.out.println("You are measuring read");
                for (int i = 0; i < times; i++){
                    totalTime += measureNormalRead();
                }
            } else {
                System.out.println("You are measuring write");
                for (int i = 0; i < times; i++){
                    totalTime += measureNormalWrites();
                }
            }
        }

        System.out.println("Total time: " + totalTime);
        System.out.println("Average time: " + totalTime/times);
    }


    private long measureNormalWrites() {
        long normalBlockNumber = normalDummyBlockchain.getBestBlock().getNumber() + 1;
        long normalTime = 0;

        for (int i = 0; i < VALUES_TO_GENERATE; i++) {
            Block normalBlock = blockchain.getChainBlockByNumber(normalBlockNumber++);

            long saveTime = System.nanoTime();
            ImportResult result = normalDummyBlockchain.tryToConnect(normalBlock);
            normalTime += System.nanoTime() - saveTime;

            if (!BlockProcessResult.importOk(result)) {
                System.err.printf("Import failed at block %7d%n", normalBlockNumber);
                System.exit(1);
            }
        }
        return normalTime;
    }

    private long measureSnappyWrites() {
        long snappyBlockNumber = snappyDummyBlockchain.getBestBlock().getNumber() + 1;
        long snappyTime = 0;

        for (int i = 0; i < VALUES_TO_GENERATE; i++) {
            Block snappyBlock = blockchainWithSnappy.getChainBlockByNumber(snappyBlockNumber++);

            long saveTimeSnappy = System.nanoTime();
            ImportResult result = snappyDummyBlockchain.tryToConnect(snappyBlock);
            snappyTime += System.nanoTime() - saveTimeSnappy;

            if (!BlockProcessResult.importOk(result)) {
                System.err.printf("Import failed at block %7d%n", snappyBlockNumber);
                System.exit(1);
            }
        }

        return snappyTime;
    }



    private long measureNormalRead() {
        Random valueSource = new Random(seed);
        IntStream blockNumbers = valueSource.ints(VALUES_TO_GENERATE, MIN, MAX);
        long normalTime = 0;

        for (int anInt : blockNumbers.toArray()) {
            List<BlockInformation> bInfo = blockchain.getBlocksInformationByNumber(anInt);

            for (BlockInformation bi : bInfo) {
                long saveTime = System.nanoTime();
                blockchain.getBlockByHash(bi.getHash());
                normalTime += System.nanoTime() - saveTime;
            }
        }

        return normalTime;

    }

    private long measureSnappyRead() {
        Random valueSource = new Random(seed);
        IntStream blockNumbers = valueSource.ints(VALUES_TO_GENERATE, MIN, MAX);
        long snappyTime = 0;

        for (int anInt : blockNumbers.toArray()) {

            List<BlockInformation> bInfoWithSnappy = blockchainWithSnappy.getBlocksInformationByNumber(anInt);

            for (BlockInformation bi : bInfoWithSnappy) {
                long saveTime = System.nanoTime();
                blockchainWithSnappy.getBlockByHash(bi.getHash());
                snappyTime += System.nanoTime() - saveTime;

            }
        }

        return snappyTime;
    }

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("usage: SnappyMetrics <Normal Blockchain> <Snappy Blockchain> <Use Snappy (true/false)> <Use read/write (true/false)>");
            System.exit(0);
            return;
        }

        System.gc();
        //args = new String[] {"/home/julian/.rsk/mainnet-snappy", "/home/julian/.rsk/mainnet-test"};
        String[] nodeCliArgs = new String[0];
        RskContext objects = new RskContext(nodeCliArgs);
        boolean useSnappy = false;
        boolean rW = true;
        int seed = 100;
        SnappyMetrics sMetrics = new SnappyMetrics(objects, args[0], args[1], useSnappy, rW, seed);
        int times = 100;
        sMetrics.runExperiment(times, 3000);
        System.exit(0);
    }
}
