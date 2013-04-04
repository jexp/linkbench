/*
 * Copyright 2012, Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.LinkBench.neo4j;

import com.facebook.LinkBench.Config;
import com.facebook.LinkBench.DummyLinkStore;
import com.facebook.LinkBench.LinkStoreTestBase;
import com.facebook.LinkBench.Phase;

import java.io.IOException;
import java.util.Properties;

public class Neo4jLinkStoreTest extends LinkStoreTestBase {

    Neo4jGraphStore store;

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected Properties basicProps() {
        Properties props = super.basicProps();
        return props;
    }

    @Override
    protected void tearDown() throws Exception {
        store.close();
        Neo4jTestConfig.dropDirectory(basicProps().getProperty(Config.DBID));
        super.tearDown();
    }

    @Override
    protected void initStore(Properties props) throws IOException,
            Exception {
        Neo4jTestConfig.dropDirectory(basicProps().getProperty(Config.DBID));
        store = new Neo4jGraphStore();
        store.initialize(props, Phase.REQUEST,0);
    }

    @Override
    protected DummyLinkStore getStoreHandle(boolean initialized) {
        // Return a new memory link store handle. The underlying link store doesn't
        // need to be initialized, so just set wrapper to correct init status
        return new DummyLinkStore(store, initialized);
    }

}
