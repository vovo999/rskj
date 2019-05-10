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

package co.rsk.listeners;

import co.rsk.core.bc.MainchainView;
import org.ethereum.core.Block;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.listener.EthereumListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class MainchainViewListener extends EthereumListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger("mainchainviewlistener");

    private MainchainView mainchainView;

    public MainchainViewListener(MainchainView mainchainView) {
        this.mainchainView = mainchainView;
    }

    @Override
    public void onBestBlock(Block block, List<TransactionReceipt> receipts) {
        logger.trace("Start onBestBlock");

        mainchainView.addBestBlock(block);

        logger.trace("End onBestBlock");
    }

    @Override
    public void onStatusChange(Block block) {
        logger.trace("Start onStatusChange");

        mainchainView.addBestBlock(block);

        logger.trace("End onStatusChange");
    }
}
