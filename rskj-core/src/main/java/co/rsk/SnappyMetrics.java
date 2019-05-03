package co.rsk;


import org.ethereum.core.Block;
import org.ethereum.core.Blockchain;
import org.ethereum.db.BlockInformation;
import org.ethereum.db.BlockStore;
import org.ethereum.util.ByteUtil;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class SnappyMetrics {

    public static final int VALUES_TO_GENERATE = 3000;
    public static final int MIN = 1;
    public static final int MAX = 300000;
    private BlockStore blockchain;
    private BlockStore blockchainWithSnappy;
    private Blockchain snappyDummyBlockchain;
    private Blockchain normalDummyBlockchain;
    private boolean useSnappy;
    private boolean rW;

    public SnappyMetrics(RskContext objects, String bcWithSnappy, String bcNormal, boolean useSnappy, boolean readWrite) {

        this.rW = readWrite;
        this.useSnappy = useSnappy;

        if (useSnappy){
            this.blockchainWithSnappy = RskContext.buildBlockStore(objects.getBlockFactory(), bcWithSnappy).setUseSnappy();
        } else {
            this.blockchain = RskContext.buildBlockStore(objects.getBlockFactory(), bcNormal);
        }

        if (!rW) {
            if (useSnappy){
                RskContext rskContextSnappyDummy = new RskContext(new String[]{"-base-path" , "/home/julian/.rsk/snappy-dummy-test"});
                rskContextSnappyDummy.getBlockStore().setUseSnappy();
                this.snappyDummyBlockchain = rskContextSnappyDummy.getBlockchain();
            } else {
                RskContext rskContextNormalDummy = new RskContext(new String[]{"-base-path" , "/home/julian/.rsk/normal-dummy-test"});
                rskContextNormalDummy.getBlockStore();
                this.normalDummyBlockchain = rskContextNormalDummy.getBlockchain();
            }
        }
    }

    public void runExperiment(int times) {
        long totalTime = 0;

        if (useSnappy) {
            System.out.println("-- Run experiments with Snappy: " + times + " times --");
            if (rW) {
                System.out.println("You are measuring read");
            } else {
                System.out.println("You are measuring write");
            }
        } else {
            System.out.println("-- Run experiments with Normal: " + times + " times --");
            if (rW) {
                System.out.println("You are measuring read");
            } else {
                System.out.println("You are measuring write");
            }
        }

        for (int i = 0; i < times; i++){
            if (useSnappy) {
                if (rW) {
                    totalTime += measureSnappyRead();
                } else {
                    totalTime += measureSnappyWrites();
                }
            } else {
                if (rW) {
                    totalTime += measureNormalRead();
                } else {
                    totalTime += measureNormalWrites();
                }
            }
        }

        System.out.println("Total time: " + totalTime);
        System.out.println("Average time: " + totalTime/times);
    }
/*
    public void compareWrites() {

        long snappyBlockNumber = snappyDummyBlockchain.getBestBlock().getNumber() + 1;
        long normalBlockNumber = normalDummyBlockchain.getBestBlock().getNumber() + 1;
        long snappyTime = 0;
        long normalTime = 0;

        for (int i = 0; i < VALUES_TO_GENERATE; i++) {
            Block snappyBlock = blockchainWithSnappy.getChainBlockByNumber(snappyBlockNumber++);
            Block normalBlock = blockchain.getChainBlockByNumber(normalBlockNumber++);

            long saveTimeSnappy = System.nanoTime();
            snappyDummyBlockchain.tryToConnect(snappyBlock);
            snappyTime += System.nanoTime() - saveTimeSnappy;

            long saveTime = System.nanoTime();
            normalDummyBlockchain.tryToConnect(normalBlock);
            normalTime += System.nanoTime() - saveTime;
        }

        System.out.println("Writing Snappy time: " + snappyTime);
        System.out.println("Writing Normal time: " + normalTime);

    }
   */

    private long measureNormalWrites() {
        long normalBlockNumber = normalDummyBlockchain.getBestBlock().getNumber() + 1;
        long normalTime = 0;

        for (int i = 0; i < VALUES_TO_GENERATE; i++) {
            Block normalBlock = blockchain.getChainBlockByNumber(normalBlockNumber++);

            long saveTime = System.nanoTime();
            normalDummyBlockchain.tryToConnect(normalBlock);
            normalTime += System.nanoTime() - saveTime;
        }
        return normalTime;
    }

    private long measureSnappyWrites() {
        long snappyBlockNumber = snappyDummyBlockchain.getBestBlock().getNumber() + 1;
        long snappyTime = 0;

        for (int i = 0; i < VALUES_TO_GENERATE; i++) {
            Block snappyBlock = blockchainWithSnappy.getChainBlockByNumber(snappyBlockNumber++);

            long saveTimeSnappy = System.nanoTime();
            snappyDummyBlockchain.tryToConnect(snappyBlock);
            snappyTime += System.nanoTime() - saveTimeSnappy;
        }

        return snappyTime;
    }



    private long measureNormalRead() {
        Random valueSource = new Random();
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
        Random valueSource = new Random();
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

/*
    public long[] compareReads () {
        Random valueSource = new Random();
        IntStream blockNumbers = valueSource.ints(VALUES_TO_GENERATE, MIN, MAX);

        long snappyTime = 0;
        long normalTime = 0;
        int quantity = 1;


        for (int anInt : blockNumbers.toArray()) {

            if (quantity % 100 == 0) {
                System.out.println("Read block number " + quantity);
            }

            List<BlockInformation> bInfo = blockchain.getBlocksInformationByNumber(anInt);
            List<BlockInformation> bInfoWithSnappy = blockchainWithSnappy.getBlocksInformationByNumber(anInt);

            for (BlockInformation bi : bInfo) {
                long saveTime = System.nanoTime();
                blockchain.getBlockByHash(bi.getHash());
                normalTime += System.nanoTime() - saveTime;

            }

            for (BlockInformation bis : bInfoWithSnappy) {
                long saveTimeSnappy = System.nanoTime();
                blockchainWithSnappy.getBlockByHash(bis.getHash());
                snappyTime += System.nanoTime() - saveTimeSnappy;

            }
            quantity++;
        }

        blockNumbers.close();
        long[] result = new long[2];
        result[0] = normalTime;
        result[1] = snappyTime;
        return result;

        /*System.out.println("Read with normal time: " + normalTime);
        System.out.println("Read with snappy time: " + snappyTime);
        System.out.println("Percentage: " + (100 - (snappyTime*100/normalTime)));

    }


    public void runReadExperiment(int times){
        long snappyTotalTime = 0;
        long normalTotalTime = 0;


        for (int i = 0; i < times; i++){
           System.out.println("Time number " + i);
           long[] result =  compareReads();
           normalTotalTime += result[0];
           snappyTotalTime += result[1];
        }

        System.out.println(times + " times.");
        System.out.println("Average of time with normal db " + normalTotalTime/times);
        System.out.println("Average of time with snappy db " + snappyTotalTime/times);
    }
*/

    public void compareBlockchain() {

        Random valueSource = new Random();
        IntStream blockNumbers = valueSource.ints(VALUES_TO_GENERATE, MIN, MAX);
        boolean equals = true;

        for (int anInt : blockNumbers.toArray()) {

            List<BlockInformation> bInfo = blockchain.getBlocksInformationByNumber(anInt);
            List<BlockInformation> bInfoWithSnappy = blockchainWithSnappy.getBlocksInformationByNumber(anInt);
            byte[] normalBlock = new byte[0];
            byte[] snappyBlock = new byte[0];

            for (BlockInformation bi : bInfo) {
                normalBlock = blockchain.getBlockByHash(bi.getHash()).getEncoded();
            }

            for (BlockInformation bis : bInfoWithSnappy) {
                snappyBlock = blockchainWithSnappy.getBlockByHash(bis.getHash()).getEncoded();

            }

            equals &= ByteUtil.fastEquals(normalBlock,snappyBlock);
        }
        if (equals) {
            System.out.println("Son iguales");
        } else {
            System.out.println("No son iguales");
        }
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
        SnappyMetrics sMetrics = new SnappyMetrics(objects, args[0], args[1], useSnappy, rW);
        int times = 100;
        sMetrics.runExperiment(times);
        System.exit(0);
    }
}
