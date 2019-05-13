/*
 * This file is part of RskJ
 * Copyright (C) 2019 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package co.rsk.cli.migration;

import co.rsk.RskContext;
import co.rsk.core.RskAddress;
import co.rsk.crypto.Keccak256;
import co.rsk.remasc.RemascTransaction;
import co.rsk.trie.*;
import org.bouncycastle.util.encoders.Hex;
import org.ethereum.config.SystemProperties;
import org.ethereum.config.net.MainNetConfig;
import org.ethereum.config.net.TestNetConfig;
import org.ethereum.core.AccountState;
import org.ethereum.core.Block;
import org.ethereum.core.BlockFactory;
import org.ethereum.core.Repository;
import org.ethereum.crypto.Keccak256Helper;
import org.ethereum.datasource.HashMapDB;
import org.ethereum.datasource.KeyValueDataSource;
import org.ethereum.db.BlockStore;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.db.MutableRepository;
import org.ethereum.util.*;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.PrecompiledContracts;

import java.util.*;
import java.util.stream.Collectors;

import static org.ethereum.crypto.HashUtil.EMPTY_TRIE_HASH;

public class UnitrieMigrationTool {

    private final KeyValueDataSource orchidContractDetailsDataStore;
    private final KeyValueDataSource orchidContractsStorage;
    private final TrieStore orchidContractsTrieStore;
    private final BlockStore blockStore;
    private final TrieStore orchidAccountsTrieStore;
    private final String orchidDatabase;
    private final Map<RskAddress, TrieStore> contractStoreCache = new HashMap<>();
    private final Map<ByteArrayWrapper, RskAddress> addressHashes;
    private final TrieConverter trieConverter;
    private final Map<ByteArrayWrapper, byte[]> keccak256Cache;

    public static void main(String[] args) {
//        UnitrieMigrationTool migrationTool = new UnitrieMigrationTool("/Users/diegoll/Documents/databases/mainnet-800k");
//        byte[] lastStateRoot = migrationTool.migrateRepository(800613, new TrieStoreImpl(RskContext.makeDataSource("state", "/Users/diegoll/Documents/databases/unitrie")));
//        System.out.println(Hex.toHexString(lastStateRoot));
        KeyValueDataSource stateRootTranslations = RskContext.makeDataSource("stateRoots", "/Users/diegoll/Documents/databases/unitrie");
        stateRootTranslations.put(Hex.decode("fc681cae6b850937cecdadb9678074775862137d0a931cbc9b01a1229d7123c0"), Hex.decode("9cd7b0c4e610d20c9c9c0e54db4279118443b54b1de741deb2e9c46b84a08045"));
        stateRootTranslations.flush();
    }

    public UnitrieMigrationTool(String orchidDatabase) {
        this.orchidDatabase = orchidDatabase;
        this.orchidContractDetailsDataStore = RskContext.makeDataSource("details", orchidDatabase);
        this.orchidContractsStorage = RskContext.makeDataSource("contracts-storage", orchidDatabase);
        this.orchidContractsTrieStore = new CachedTrieStore(new TrieStoreImpl(orchidContractsStorage));
        this.blockStore = RskContext.buildBlockStore(new BlockFactory(new MainNetConfig()), orchidDatabase);
        this.orchidAccountsTrieStore = new CachedTrieStore(new TrieStoreImpl(RskContext.makeDataSource("state", orchidDatabase)));
        this.trieConverter = new TrieConverter();
        this.keccak256Cache = new HashMap<>();
        this.addressHashes = orchidContractDetailsDataStore.keys().stream()
                .filter(accountAddress -> accountAddress.length == 20)
                .collect(
                    Collectors.toMap(accountAddress -> ByteUtil.wrap(Keccak256Helper.keccak256(accountAddress)),
                    RskAddress::new
                )
            );
        this.addressHashes.put(ByteUtil.wrap(Keccak256Helper.keccak256(PrecompiledContracts.REMASC_ADDR.getBytes())), PrecompiledContracts.REMASC_ADDR);
        this.addressHashes.put(ByteUtil.wrap(Keccak256Helper.keccak256(RemascTransaction.REMASC_ADDRESS.getBytes())), RemascTransaction.REMASC_ADDRESS);
    }

    public byte[] migrateRepository(long blockToMigrate, TrieStore unitrieStoreDestination) {
        MutableRepository unitrieRepository = new MutableRepository(new Trie(unitrieStoreDestination));
        if (blockToMigrate > blockStore.getMaxNumber()) {
            throw new IllegalArgumentException(String.format("Unable to migrate blocks greater than %d", blockStore.getMaxNumber()));
        }
        Block currentBlock = blockStore.getChainBlockByNumber(blockToMigrate);
        byte[] orchidStateRoot = currentBlock.getStateRoot();
        System.out.printf("====== %07d (%s) =======\n", blockToMigrate, Hex.toHexString(orchidStateRoot));
        Trie orchidAccountsTrie = orchidAccountsTrieStore.retrieve(orchidStateRoot);
        if (!Arrays.equals(orchidStateRoot, orchidAccountsTrie.getHashOrchid(true).getBytes())) {
            throw new IllegalStateException(String.format("Stored account state is not consistent with the expected root (%s) for block %d", Hex.toHexString(orchidStateRoot), blockToMigrate));
        }
        buildPartialUnitrie(orchidAccountsTrie, orchidContractDetailsDataStore, unitrieRepository);

        byte[] lastStateRoot = unitrieRepository.getRoot();
        byte[] orchidMigratedStateRoot = trieConverter.getOrchidAccountTrieRoot(unitrieRepository.getMutableTrie().getTrie());
        if (!Arrays.equals(orchidStateRoot, orchidMigratedStateRoot)) {
            System.out.printf("\nOrchid state root:\t\t%s\nConverted Unitrie root:\t%s\nUnitrie state root:\t %s\n",
                    Hex.toHexString(orchidStateRoot),
                    Hex.toHexString(orchidMigratedStateRoot),
                    Hex.toHexString(lastStateRoot)
            );
            throw new IllegalStateException("Not matching state root");
        } else {
            System.out.println("Matched state root");
        }
        return lastStateRoot;
    }

    private void buildPartialUnitrie(Trie orchidAccountsTrie, KeyValueDataSource detailsDataStore, Repository repository) {
        int accountsToLog = 500;
        int accountsCounter = 0;
        System.out.printf("(x = %d accounts): ", accountsToLog);
        Iterator<Trie.IterationElement> orchidAccountsTrieIterator = orchidAccountsTrie.getPreOrderIterator();
        while (orchidAccountsTrieIterator.hasNext()) {
            Trie.IterationElement orchidAccountsTrieElement = orchidAccountsTrieIterator.next();
            TrieKeySlice currentElementExpandedPath = orchidAccountsTrieElement.getNodeKey();
            if (currentElementExpandedPath.length() == Keccak256Helper.DEFAULT_SIZE) {
                accountsCounter++;
                byte[] hashedAddress = currentElementExpandedPath.encode();
                OldAccountState oldAccountState = new OldAccountState(orchidAccountsTrieElement.getNode().getValue());
                AccountState accountState = new AccountState(oldAccountState.getNonce(), oldAccountState.getBalance());
                RskAddress accountAddress = addressHashes.get(ByteUtil.wrap(hashedAddress));
                repository.createAccount(accountAddress);
                repository.updateAccountState(accountAddress, accountState);
                byte[] contractData = detailsDataStore.get(accountAddress.getBytes());
                byte[] codeHash = oldAccountState.getCodeHash();
                byte[] accountStateRoot = oldAccountState.getStateRoot();
                if (contractData != null) {
                    try {
                        migrateContract(accountAddress, repository, contractData, codeHash, accountStateRoot);
                    } catch (IllegalStateException e) {
                        throw new IllegalStateException(String.format("Unable to migrate contract %s", accountAddress), e);
                    }
                }
                if (accountsCounter % accountsToLog == 0) {
                    System.out.print("x");
                }
            }
        }
        System.out.println();
        allValuesProcessed(orchidAccountsTrie, accountsCounter);
    }

    private void migrateContract(RskAddress accountAddress, Repository currentRepository, byte[] contractData, byte[] accountCodeHash, byte[] stateRoot) {
        ArrayList<RLPElement> rlpData = RLP.decode2(contractData);
        RLPList rlpList = (RLPList) rlpData.get(0);
        RLPElement rlpCode = rlpList.get(3);
        byte[] code = rlpCode.getRLPData();

        RLPItem rlpAddress = (RLPItem) rlpList.get(0);
        RLPItem rlpIsExternalStorage = (RLPItem) rlpList.get(1);
        RLPItem rlpStorage = (RLPItem) rlpList.get(2);
        byte[] rawAddress = rlpAddress.getRLPData();
        RskAddress contractAddress;
        if (Arrays.equals(rawAddress, new byte[] { 0x00 })) {
            contractAddress = PrecompiledContracts.REMASC_ADDR;
        } else {
            contractAddress = new RskAddress(rawAddress);
        }
        byte[] external = rlpIsExternalStorage.getRLPData();
        byte[] root = rlpStorage.getRLPData();

        boolean initialized = false;
        if (!Arrays.equals(stateRoot, EMPTY_TRIE_HASH)) {
            Trie contractStorageTrie;
            if (external != null && external.length > 0 && external[0] == 1) {
                // picco-fix (ref: co.rsk.db.ContractStorageStoreFactory#getTrieStore)
                contractStorageTrie = orchidContractsTrieStore.retrieve(root);
                if (contractStorageTrie == null) {
                    TrieStore contractTrieStore = contractStoreCache.computeIfAbsent(
                            contractAddress,
                            address -> new CachedTrieStore(new TrieStoreImpl(RskContext.makeDataSource("details-storage/" + address, orchidDatabase)))
                    );
                    contractStorageTrie = contractTrieStore.retrieve(root);
                    if (contractStorageTrie == null) {
                        throw new IllegalStateException(String.format("Unable to find root %s for the contract %s", Hex.toHexString(root), contractAddress));
                    }
                    if (!Arrays.equals(root, contractStorageTrie.getHashOrchid(true).getBytes())) {
                        throw new IllegalStateException(String.format("Stored contract state is not consistent with the expected root (%s)", Hex.toHexString(root)));
                    }
                }
            } else {
                contractStorageTrie = orchidTrieDeserialize(root);
            }
            try {
                contractStorageTrie = contractStorageTrie.getSnapshotTo(new Keccak256(stateRoot));
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Cannot find state root trie", e);
            }

            RLPList rlpKeys = (RLPList) rlpList.get(4);
            int keysCount = rlpKeys.size();
            int keysToLog = 2000;
            boolean logKeysMigrationProgress = keysCount > keysToLog * 2;
            if (logKeysMigrationProgress) {
                System.out.printf("\nMigrating %s with %d keys\n(. = %d keys): ", contractAddress, rlpKeys.size(), keysToLog);
            }
            int migratedKeysCounter = 0;
            for (RLPElement rlpKey : rlpKeys) {
                byte[] rawKey = rlpKey.getRLPData();
                byte[] storageKey = keccak256Cache.computeIfAbsent(ByteUtil.wrap(rawKey), key -> Keccak256Helper.keccak256(key.getData()));
                byte[] value = contractStorageTrie.get(storageKey);
                if (value != null) {
                    migratedKeysCounter++;
                    if (!initialized) {
                        currentRepository.setupContract(accountAddress);
                        initialized = true;
                    }
                    if (logKeysMigrationProgress && migratedKeysCounter % keysToLog == 0) {
                        System.out.print(".");
                    }
                    currentRepository.addStorageBytes(contractAddress, DataWord.valueOf(rawKey), value);
                }
            }
            try {
                allValuesProcessed(contractStorageTrie, migratedKeysCounter);
            } catch (IllegalStateException ise) {
                throw new IllegalStateException(String.format("Error processing storage for contract %s", contractAddress), ise);
            }
            if (logKeysMigrationProgress) {
                System.out.println();
            }
        }

        if (code != null) {
            if (!initialized) {
                currentRepository.setupContract(accountAddress);
            }
            if (!Arrays.equals(accountCodeHash, Keccak256Helper.keccak256(code))) {
                // mati-fix (ref: org.ethereum.db.DetailsDataStore#get)
                code = orchidContractsStorage.get(accountCodeHash);
            }
            currentRepository.saveCode(accountAddress, code);
        }
    }

    /**
     * Counts all nodes with value and checks it's equals to accountsCounter
     * @param currentTrie
     * @param accountsCounter
     */
    private void allValuesProcessed(Trie currentTrie, int expectedCount) {
        int valueCounter = 0;
        Iterator<Trie.IterationElement> inOrderIterator = currentTrie.getInOrderIterator();
        while (inOrderIterator.hasNext()) {
            Trie.IterationElement iterationElement = inOrderIterator.next();
            if (iterationElement.getNode().getValue() != null) {
                valueCounter++;
            }
        }
        if (valueCounter != expectedCount) {
            throw new IllegalStateException(String.format("Trie %s has %d values and we expected %d", currentTrie.getHash(), valueCounter, expectedCount));
        }
    }

    public static Trie orchidTrieDeserialize(byte[] bytes) {
        final int keccakSize = Keccak256Helper.DEFAULT_SIZE_BYTES;
        int expectedSize = Short.BYTES + keccakSize;
        if (expectedSize > bytes.length) {
            throw new IllegalArgumentException(
                    String.format("Expected size is: %d actual size is %d", expectedSize, bytes.length));
        }

        byte[] root = Arrays.copyOfRange(bytes, Short.BYTES, expectedSize);
        TrieStore store = orchidTrieStoreDeserialize(bytes, expectedSize, new HashMapDB());

        Trie newTrie = store.retrieve(root);

        if (newTrie == null) {
            throw new IllegalArgumentException(String.format("Deserialized storage doesn't contain expected trie: %s", Hex.toHexString(root)));
        }

        return newTrie;
    }

    private static TrieStore orchidTrieStoreDeserialize(byte[] bytes, int offset, KeyValueDataSource ds) {
        int current = offset;
        current += Short.BYTES; // version

        int nkeys = readInt(bytes, current);
        current += Integer.BYTES;

        for (int k = 0; k < nkeys; k++) {
            int lkey = readInt(bytes, current);
            current += Integer.BYTES;
            if (lkey > bytes.length - current) {
                throw new IllegalArgumentException(String.format(
                        "Left bytes are too short for key expected:%d actual:%d total:%d",
                        lkey, bytes.length - current, bytes.length));
            }
            byte[] key = Arrays.copyOfRange(bytes, current, current + lkey);
            current += lkey;

            int lvalue = readInt(bytes, current);
            current += Integer.BYTES;
            if (lvalue > bytes.length - current) {
                throw new IllegalArgumentException(String.format(
                        "Left bytes are too short for value expected:%d actual:%d total:%d",
                        lvalue, bytes.length - current, bytes.length));
            }
            byte[] value = Arrays.copyOfRange(bytes, current, current + lvalue);
            current += lvalue;
            ds.put(key, value);
        }

        return new TrieStoreImpl(ds);
    }

    // this methods reads a int as dataInputStream + byteArrayInputStream
    private static int readInt(byte[] bytes, int position) {
        final int LAST_BYTE_ONLY_MASK = 0x000000ff;
        int ch1 = bytes[position] & LAST_BYTE_ONLY_MASK;
        int ch2 = bytes[position+1] & LAST_BYTE_ONLY_MASK;
        int ch3 = bytes[position+2] & LAST_BYTE_ONLY_MASK;
        int ch4 = bytes[position+3] & LAST_BYTE_ONLY_MASK;
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new IllegalArgumentException(
                    String.format("On position %d there are invalid bytes for a short value %s %s %s %s",
                            position, ch1, ch2, ch3, ch4));
        } else {
            return (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4);
        }
    }

    private class CachedTrieStore implements TrieStore {

        private final TrieStore parent;
        private final Map<Keccak256, Trie> triesCache;
        private final Map<ByteArrayWrapper, byte[]> valueCache;

        private CachedTrieStore(TrieStore parent) {
            this.parent = parent;
            this.triesCache = new HashMap<>();
            this.valueCache = new HashMap<>();
        }

        @Override
        public void save(Trie trie) {
            triesCache.put(trie.getHash(), trie);
            parent.save(trie);
        }

        @Override
        public Trie retrieve(byte[] hash) {
            return triesCache.computeIfAbsent(new Keccak256(hash), key -> parent.retrieve(hash));
        }

        @Override
        public byte[] retrieveValue(byte[] hash) {
            return valueCache.computeIfAbsent(ByteUtil.wrap(hash), key -> parent.retrieveValue(hash));
        }

        @Override
        public void flush() {
        }
    }
}
