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

import com.hazelcast.cache.impl.HazelcastServerCachingProvider;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.cache.impl.HazelcastClientCachingProvider;
import com.hazelcast.core.*;
import com.hazelcast.instance.HazelcastInstanceProxy;
import org.yardstickframework.BenchmarkConfiguration;
import org.yardstickframework.BenchmarkDriverAdapter;
import org.yardstickframework.BenchmarkUtils;

import javax.cache.CacheManager;
import javax.cache.spi.CachingProvider;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

import static org.yardstickframework.BenchmarkUtils.jcommander;
import static org.yardstickframework.BenchmarkUtils.println;

/**
 * Abstract class for Hazelcast benchmarks.
 */
public abstract class HazelcastAbstractBenchmark extends BenchmarkDriverAdapter {
    /** Cache name. */
    protected final String cacheName;

    /** Arguments. */
    protected final HazelcastBenchmarkArguments args = new HazelcastBenchmarkArguments();

    /** Node. */
    private HazelcastNode node;

    protected HazelcastAbstractBenchmark(String cacheName) {
        this.cacheName = cacheName;
    }

    /** {@inheritDoc} */
    @Override public void setUp(BenchmarkConfiguration cfg) throws Exception {
        super.setUp(cfg);

        jcommander(cfg.commandLineArguments(), args, "<hazelcast-driver>");

        SampleValue.sampleValueSize = args.valueSize();

        HazelcastInstance instance = startedInstance(args.clientMode());

        if (instance == null) {
            node = new HazelcastNode(args.clientMode());

            node.start(cfg);
        } else {
            node = new HazelcastNode(args.clientMode(), instance);
        }

        waitForNodes();
    }

    /**
     * @param clientMode Client mode.
     * @return Started instance.
     */
    private static HazelcastInstance startedInstance(boolean clientMode) {
        Collection<HazelcastInstance> col = clientMode ? HazelcastClient.getAllHazelcastClients() :
            Hazelcast.getAllHazelcastInstances();

        return col == null || col.isEmpty() ? null : col.iterator().next();
    }

    /** {@inheritDoc} */
    @Override public void tearDown() throws Exception {
        if (node != null)
            node.stop();
    }

    /** {@inheritDoc} */
    @Override public String description() {
        String desc = BenchmarkUtils.description(cfg, this);

        return desc.isEmpty() ?
            getClass().getSimpleName() + args.description() + cfg.defaultDescription() : desc;
    }

    /** {@inheritDoc} */
    @Override public String usage() {
        return BenchmarkUtils.usage(args);
    }

    /**
     * @return Grid.
     */
    protected HazelcastInstance hazelcast() {
        return node.hazelcast();
    }

    /**
     * @throws Exception If failed.
     */
    private void waitForNodes() throws Exception {
        final CountDownLatch nodesStartedLatch = new CountDownLatch(1);

        hazelcast().getCluster().addMembershipListener(new MembershipListener() {
            @Override public void memberAdded(MembershipEvent evt) {
                if (nodesStarted())
                    nodesStartedLatch.countDown();
            }

            @Override public void memberRemoved(MembershipEvent evt) {
                // No-op.
            }

            @Override public void memberAttributeChanged(MemberAttributeEvent memberAttributeEvent) {
                // No-op.
            }
        });

        if (!nodesStarted()) {
            println(cfg, "Waiting for " + (args.nodes() - 1) + " nodes to start...");

            nodesStartedLatch.await();
        }
    }

    /**
     * @return {@code True} if all nodes are started, {@code false} otherwise.
     */
    private boolean nodesStarted() {
        int rmtNodeCnt = args.clientMode() ? args.nodes() - 1 : args.nodes();

        return hazelcast().getCluster().getMembers().size() >= rmtNodeCnt;
    }

    /**
     * @param max Key range.
     * @return Next key.
     */
    protected int nextRandom(int max) {
        return ThreadLocalRandom.current().nextInt(max);
    }

    /**
     * @param min Minimum key in range.
     * @param max Maximum key in range.
     * @return Next key.
     */
    protected int nextRandom(int min, int max) {
        return ThreadLocalRandom.current().nextInt(max - min) + min;
    }


    public static boolean isMember(HazelcastInstance instance) {
        return instance instanceof HazelcastInstanceProxy;
    }
    public static boolean isClient(HazelcastInstance instance) {
        return ! isMember(instance);
    }

    public static CacheManager getCacheManager(HazelcastInstance instance){
        CachingProvider provider;
        if (isMember(instance)) {
            provider = HazelcastServerCachingProvider.createCachingProvider(instance);
        } else {
            provider = HazelcastClientCachingProvider.createCachingProvider(instance);
        }
        return provider.getCacheManager(provider.getDefaultURI(),provider.getDefaultClassLoader(), null);
    }

}
