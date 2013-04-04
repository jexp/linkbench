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

import org.neo4j.kernel.impl.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Class containing hardcoded parameters and helper functions used to create
 * and connect to the unit test database for Neo4j
 *
 * @author mhunger
 */
public class Neo4jTestConfig {

    public static void createConfig(String path, Properties props) {
        props.setProperty("store_dir", path);
        props.setProperty("cache_type", "gcr");
        // todo mmio, cache
    }

    public static void dropDirectory(String testDB) throws IOException {
        FileUtils.deleteRecursively(new File(testDB));
    }
}
