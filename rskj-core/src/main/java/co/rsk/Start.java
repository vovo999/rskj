/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
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
package co.rsk;

import co.rsk.cli.migration.UnitrieMigrationTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * The entrypoint for the RSK full node
 */
public class Start {
    private static Logger logger = LoggerFactory.getLogger("start");

    public static void main(String[] args) {
        RskContext ctx = new RskContext(args);
        // we need to check before the data source is init'ed
        if (!Files.exists(Paths.get(ctx.getRskSystemProperties().databaseDir(), "unitrie"))) {
            UnitrieMigrationTool unitrieMigrationTool = new UnitrieMigrationTool(
                    ctx.getRskSystemProperties().databaseDir(),
                    ctx.getBlockStore(),
                    ctx.getRepository(),
                    ctx.getStateRootHandler(),
                    ctx.getTrieConverter()
            );
            if (unitrieMigrationTool.canMigrate()) {
                unitrieMigrationTool.migrate();
            } else {
                logger.error("Reset database or continue syncing with previous version");
                System.exit(1);
            }
        }

        NodeRunner runner = ctx.getNodeRunner();
        try {
            runner.run();
            Runtime.getRuntime().addShutdownHook(new Thread(runner::stop));
        } catch (Exception e) {
            logger.error("The RSK node main thread failed, closing program", e);
            runner.stop();
            System.exit(1);
        }
    }
}
