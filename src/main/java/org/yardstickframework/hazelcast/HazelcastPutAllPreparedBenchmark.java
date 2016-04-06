/*
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package org.yardstickframework.hazelcast;

import org.yardstickframework.BenchmarkConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Hazelcast benchmark that performs putAll operations.
 */
public class HazelcastPutAllPreparedBenchmark extends HazelcastAbstractMapBenchmark {

    private static final int PREPARED_MAPS_COUNT = 100;

    private final List<Map<Integer, Integer>> maps = new ArrayList<>();
    private int mapsSize;

    /** */
    public HazelcastPutAllPreparedBenchmark() {
        super("map");
    }

    /** {@inheritDoc} */
    @Override public void setUp(BenchmarkConfiguration cfg) throws Exception {
        super.setUp(cfg);

        for (int i = 0; i < PREPARED_MAPS_COUNT; i++) {
            SortedMap<Integer, Integer> valueMap = new TreeMap<>();
            for (int j = 0; j < args.batch(); j++) {
                int key = nextRandom(args.range());

                valueMap.put(key, key);
            }
            maps.add(valueMap);
        }
        mapsSize = maps.size();
    }

    /** {@inheritDoc} */
    @Override public boolean test(Map<Object, Object> ctx) throws Exception {
        Map<Integer, Integer> valueMap = maps.get(nextRandom(mapsSize));
        map.putAll(valueMap);

        return true;
    }
}
