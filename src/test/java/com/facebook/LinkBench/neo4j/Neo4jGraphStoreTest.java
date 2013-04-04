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

import com.facebook.LinkBench.*;

import java.io.IOException;
import java.sql.Connection;
import java.util.Properties;

public class Neo4jGraphStoreTest extends GraphStoreTestBase {

  private Properties props;
  private Connection conn;
    private Neo4jGraphStore neo4jGraphStore;

    @Override
  protected void initStore(Properties props) throws IOException, Exception {
    this.props = props;
    Neo4jTestConfig.dropDirectory(testDB);
  }

  @Override
  protected long getIDCount() {
    // Make quicker
    return 500;
  }

  @Override
  protected int getRequestCount() {
    // Make quicker, enough requests that we can reasonably check
    // that operation percentages are about about right
    return 10000;
  }

  @Override
  protected void tearDown() throws Exception {
    if (neo4jGraphStore!=null) neo4jGraphStore.close();
    neo4jGraphStore=null;
    Neo4jTestConfig.dropDirectory(testDB);
    super.tearDown();
  }

  @Override
  protected Properties basicProps() {
    Properties props = super.basicProps();
    Neo4jTestConfig.createConfig(testDB,props);
    return props;
  }


  @Override
  protected DummyLinkStore getStoreHandle(boolean initialize) throws IOException, Exception {

    DummyLinkStore result = new DummyLinkStore(getNeo4jStore());
    if (initialize) {
      result.initialize(props, Phase.REQUEST, 0);
    }
    return result;
  }

    private Neo4jGraphStore getNeo4jStore() {
        if (neo4jGraphStore==null) neo4jGraphStore = new Neo4jGraphStore();
        return neo4jGraphStore;
    }

}
