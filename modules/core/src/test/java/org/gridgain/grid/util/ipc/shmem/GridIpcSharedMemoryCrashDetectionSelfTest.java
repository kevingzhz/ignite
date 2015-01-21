/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gridgain.grid.util.ipc.shmem;

import org.apache.commons.collections.*;
import org.apache.ignite.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.ipc.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Test shared memory endpoints crash detection.
 */
public class GridIpcSharedMemoryCrashDetectionSelfTest extends GridCommonAbstractTest {
    /** Timeout in ms between read/write attempts in busy-wait loops. */
    public static final int RW_SLEEP_TIMEOUT = 50;

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        GridIpcSharedMemoryNativeLoader.load();
    }

    /**
     * @throws Exception If failed.
     */
    public void testGgfsServerClientInteractionsUponClientKilling() throws Exception {
        U.setWorkDirectory(null, U.getGridGainHome());

        // Run server endpoint.
        GridIpcSharedMemoryServerEndpoint srv = new GridIpcSharedMemoryServerEndpoint();

        new GridTestResources().inject(srv);

        try {
            srv.start();

            info("Check that server gets correct exception upon client's killing.");

            info("Shared memory IDs before starting client endpoint: " + GridIpcSharedMemoryUtils.sharedMemoryIds());

            Collection<Integer> shmemIdsWithinInteractions = interactWithClient(srv, true);

            Collection<Integer> shmemIdsAfterInteractions = null;

            // Give server endpoint some time to make resource clean up. See GridIpcSharedMemoryServerEndpoint.GC_FREQ.
            for (int i = 0; i < 12; i++) {
                shmemIdsAfterInteractions = GridIpcSharedMemoryUtils.sharedMemoryIds();

                info("Shared memory IDs created within interaction: " + shmemIdsWithinInteractions);
                info("Shared memory IDs after killing client endpoint: " + shmemIdsAfterInteractions);

                if (CollectionUtils.containsAny(shmemIdsAfterInteractions, shmemIdsWithinInteractions))
                    U.sleep(1000);
                else
                    break;
            }

            assertFalse("List of shared memory IDs after killing client endpoint should not include IDs created " +
                "within server-client interactions.",
                CollectionUtils.containsAny(shmemIdsAfterInteractions, shmemIdsWithinInteractions));
        }
        finally {
            srv.close();
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testGgfsClientServerInteractionsUponServerKilling() throws Exception {
        Collection<Integer> shmemIdsBeforeInteractions = GridIpcSharedMemoryUtils.sharedMemoryIds();

        info("Shared memory IDs before starting server-client interactions: " + shmemIdsBeforeInteractions);

        Collection<Integer> shmemIdsWithinInteractions = interactWithServer();

        Collection<Integer> shmemIdsAfterInteractions = GridIpcSharedMemoryUtils.sharedMemoryIds();

        info("Shared memory IDs created within interaction: " + shmemIdsWithinInteractions);
        info("Shared memory IDs after server and client killing: " + shmemIdsAfterInteractions);

        if (!U.isLinux())
            assertTrue("List of shared memory IDs after server-client interactions should include IDs created within " +
                "client-server interactions.", shmemIdsAfterInteractions.containsAll(shmemIdsWithinInteractions));
        else
            assertFalse("List of shared memory IDs after server-client interactions should not include IDs created " +
                "(on Linux): within client-server interactions.",
                CollectionUtils.containsAny(shmemIdsAfterInteractions, shmemIdsWithinInteractions));

        ProcessStartResult srvStartRes = startSharedMemoryTestServer();

        try {
            // Give server endpoint some time to make resource clean up. See GridIpcSharedMemoryServerEndpoint.GC_FREQ.
            for (int i = 0; i < 12; i++) {
                shmemIdsAfterInteractions = GridIpcSharedMemoryUtils.sharedMemoryIds();

                info("Shared memory IDs after server restart: " + shmemIdsAfterInteractions);

                if (CollectionUtils.containsAny(shmemIdsAfterInteractions, shmemIdsWithinInteractions))
                    U.sleep(1000);
                else
                    break;
            }

            assertFalse("List of shared memory IDs after server endpoint restart should not include IDs created: " +
                "within client-server interactions.",
                CollectionUtils.containsAny(shmemIdsAfterInteractions, shmemIdsWithinInteractions));
        }
        finally {
            srvStartRes.proc().kill();

            srvStartRes.isKilledLatch().await();
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testClientThrowsCorrectExceptionUponServerKilling() throws Exception {
        info("Shared memory IDs before starting server-client interactions: " +
            GridIpcSharedMemoryUtils.sharedMemoryIds());

        Collection<Integer> shmemIdsWithinInteractions = checkClientThrowsCorrectExceptionUponServerKilling();

        Collection<Integer> shmemIdsAfterInteractions = GridIpcSharedMemoryUtils.sharedMemoryIds();

        info("Shared memory IDs created within interaction: " + shmemIdsWithinInteractions);
        info("Shared memory IDs after server killing and client graceful termination: " + shmemIdsAfterInteractions);

        assertFalse("List of shared memory IDs after killing server endpoint should not include IDs created " +
            "within server-client interactions.",
            CollectionUtils.containsAny(shmemIdsAfterInteractions, shmemIdsWithinInteractions));
    }

    /**
     * Launches GridGgfsSharedMemoryTestServer and GridGgfsSharedMemoryTestClient.
     * After successful connection kills firstly server and secondly client.
     *
     * @return Collection of shared memory IDs created while client-server interactions.
     * @throws Exception In case of any exception happen.
     */
    private Collection<Integer> interactWithServer() throws Exception {
        ProcessStartResult srvStartRes = startSharedMemoryTestServer();

        ProcessStartResult clientStartRes = startSharedMemoryTestClient();

        // Wait until client and server start to talk.
        clientStartRes.isReadyLatch().await();

        info("Going to kill server.");

        srvStartRes.proc().kill();

        srvStartRes.isKilledLatch().await();

        info("Going to kill client.");

        clientStartRes.proc().kill();

        clientStartRes.isKilledLatch().await();

        return clientStartRes.shmemIds();
    }

    /**
     * Launches GridGgfsSharedMemoryTestServer and connects to it with client endpoint.
     * After couple of reads-writes kills the server and checks client throws correct exception.
     *
     * @return List of shared memory IDs created while client-server interactions.
     * @throws Exception In case of any exception happen.
     */
    @SuppressWarnings("BusyWait")
    private Collection<Integer> checkClientThrowsCorrectExceptionUponServerKilling() throws Exception {
        ProcessStartResult srvStartRes = startSharedMemoryTestServer();

        Collection<Integer> shmemIds = new ArrayList<>();
        GridIpcSharedMemoryClientEndpoint client = null;

        int interactionsCntBeforeSrvKilling = 5;
        int i = 1;

        try {
            // Run client endpoint.
            client = (GridIpcSharedMemoryClientEndpoint)GridIpcEndpointFactory.connectEndpoint(
                "shmem:" + GridIpcSharedMemoryServerEndpoint.DFLT_IPC_PORT, log);

            OutputStream os = client.outputStream();

            shmemIds.add(client.inSpace().sharedMemoryId());
            shmemIds.add(client.outSpace().sharedMemoryId());

            for (; i < interactionsCntBeforeSrvKilling * 2; i++) {
                info("Write: 123");

                os.write(123);

                Thread.sleep(RW_SLEEP_TIMEOUT);

                if (i == interactionsCntBeforeSrvKilling) {
                    info("Going to kill server.");

                    srvStartRes.proc().kill();

                    info("Write 512k array to hang write procedure.");

                    os.write(new byte[512 * 1024]);
                }
            }

            fail("Client should throw IOException upon server killing.");
        }
        catch (IOException e) {
            assertTrue(i >= interactionsCntBeforeSrvKilling);

            assertTrue(X.hasCause(e, IgniteCheckedException.class));
            assertTrue(X.cause(e, IgniteCheckedException.class).getMessage().contains("Shared memory segment has been closed"));
        }
        finally {
            U.closeQuiet(client);
        }

        srvStartRes.isKilledLatch().await();

        return shmemIds;
    }

    /**
     * Creates client endpoint and launches interaction between the one and the given server endpoint.
     *
     *
     * @param srv Server endpoint to interact with.
     * @param killClient Whether or not kill client endpoint within interaction.
     * @return List of shared memory IDs created while client-server interactions.
     * @throws Exception In case of any exception happen.
     */
    @SuppressWarnings({"BusyWait", "TypeMayBeWeakened"})
    private Collection<Integer> interactWithClient(GridIpcSharedMemoryServerEndpoint srv, boolean killClient)
        throws Exception {
        ProcessStartResult clientStartRes = startSharedMemoryTestClient();

        GridIpcSharedMemoryClientEndpoint clientEndpoint = (GridIpcSharedMemoryClientEndpoint)srv.accept();

        Collection<Integer> shmemIds = new ArrayList<>();
        InputStream is = null;

        int interactionsCntBeforeClientKilling = 5;
        int i = 1;

        try {
            is = clientEndpoint.inputStream();

            shmemIds.add(clientEndpoint.inSpace().sharedMemoryId());
            shmemIds.add(clientEndpoint.outSpace().sharedMemoryId());

            for (; i < interactionsCntBeforeClientKilling * 2; i++) {
                info("Before read.");

                is.read();

                Thread.sleep(RW_SLEEP_TIMEOUT);

                if (killClient && i == interactionsCntBeforeClientKilling) {
                    info("Going to kill client.");

                    clientStartRes.proc().kill();
                }
            }
        }
        catch (IOException e) {
            assertTrue("No IOException should be thrown if we do not kill client.", killClient);
            assertTrue("No IOException should be thrown before client is killed.",
                i > interactionsCntBeforeClientKilling);

            assertTrue(X.hasCause(e, IgniteCheckedException.class));
            assertTrue(X.cause(e, IgniteCheckedException.class).getMessage().contains("Shared memory segment has been closed"));

            clientStartRes.isKilledLatch().await();

            return shmemIds;
        }
        finally {
            U.closeQuiet(is);
        }

        assertTrue(
            "Interactions count should be bigger than interactionsCntBeforeClientKilling if we do not kill client.",
            i > interactionsCntBeforeClientKilling);

        // Cleanup client.
        clientStartRes.proc().kill();

        clientStartRes.isKilledLatch().await();

        assertFalse("No IOException have been thrown while the client should be killed.", killClient);

        return shmemIds;
    }

    /**
     * Starts {@code GridGgfsSharedMemoryTestClient}. The method doesn't wait while client being started.
     *
     * @return Start result of the {@code GridGgfsSharedMemoryTestClient}.
     * @throws Exception In case of any exception happen.
     */
    private ProcessStartResult startSharedMemoryTestClient() throws Exception {
        /** */
        final CountDownLatch killedLatch = new CountDownLatch(1);

        /** */
        final CountDownLatch readyLatch = new CountDownLatch(1);

        /** */
        final ProcessStartResult res = new ProcessStartResult();

        /** Process. */
        GridJavaProcess proc = GridJavaProcess.exec(
            GridGgfsSharedMemoryTestClient.class, null,
            log,
            new CI1<String>() {
                @Override public void apply(String s) {
                    info("Client process prints: " + s);

                    if (s.startsWith(GridGgfsSharedMemoryTestClient.SHMEM_IDS_MSG_PREFIX)) {
                        res.shmemIds(s.substring(GridGgfsSharedMemoryTestClient.SHMEM_IDS_MSG_PREFIX.length()));

                        readyLatch.countDown();
                    }
                }
            },
            new CA() {
                @Override public void apply() {
                    info("Client is killed");

                    killedLatch.countDown();
                }
            },
            null,
            System.getProperty("surefire.test.class.path")
        );

        res.proc(proc);
        res.isKilledLatch(killedLatch);
        res.isReadyLatch(readyLatch);

        return res;
    }

    /**
     * Starts {@code GridGgfsSharedMemoryTestServer}. The method waits while server being started.
     *
     * @return Start result of the {@code GridGgfsSharedMemoryTestServer}.
     * @throws Exception In case of any exception happen.
     */
    private ProcessStartResult startSharedMemoryTestServer() throws Exception {
        final CountDownLatch srvReady = new CountDownLatch(1);
        final CountDownLatch isKilledLatch = new CountDownLatch(1);

        GridJavaProcess proc = GridJavaProcess.exec(
            GridGgfsSharedMemoryTestServer.class, null,
            log,
            new CI1<String>() {
                @Override public void apply(String str) {
                    info("Server process prints: " + str);

                    if (str.contains("IPC shared memory server endpoint started"))
                        srvReady.countDown();
                }
            },
            new CA() {
                @Override public void apply() {
                    info("Server is killed");

                    isKilledLatch.countDown();
                }
            },
            null,
            System.getProperty("surefire.test.class.path")
        );

        srvReady.await();

        ProcessStartResult res = new ProcessStartResult();

        res.proc(proc);
        res.isKilledLatch(isKilledLatch);

        return res;
    }

    /**
     * Internal utility class to store results of running client/server in separate process.
     */
    private static class ProcessStartResult {
        /** Java process within which some class has been run. */
        private GridJavaProcess proc;

        /** Count down latch to signal when process termination will be detected. */
        private CountDownLatch killedLatch;

        /** Count down latch to signal when process is readiness (in terms of business logic) will be detected. */
        private CountDownLatch readyLatch;

        /** Shared memory IDs string read from system.input. */
        private Collection<Integer> shmemIds;

        /**
         * @return Java process within which some class has been run.
         */
        GridJavaProcess proc() {
            return proc;
        }

        /**
         * Sets Java process within which some class has been run.
         *
         * @param proc Java process.
         */
        void proc(GridJavaProcess proc) {
            this.proc = proc;
        }

        /**
         * @return Latch to signal when process termination will be detected.
         */
        CountDownLatch isKilledLatch() {
            return killedLatch;
        }

        /**
         * Sets CountDownLatch to signal when process termination will be detected.
         *
         * @param killedLatch CountDownLatch
         */
        void isKilledLatch(CountDownLatch killedLatch) {
            this.killedLatch = killedLatch;
        }

        /**
         * @return Latch to signal when process is readiness (in terms of business logic) will be detected.
         */
        CountDownLatch isReadyLatch() {
            return readyLatch;
        }

        /**
         * Sets CountDownLatch to signal when process readiness (in terms of business logic) will be detected.
         *
         * @param readyLatch CountDownLatch
         */
        void isReadyLatch(CountDownLatch readyLatch) {
            this.readyLatch = readyLatch;
        }

        /**
         * @return Shared memory IDs string read from system.input. Nullable.
         */
        @Nullable Collection<Integer> shmemIds() {
            return shmemIds;
        }

        /**
         * Sets Shared memory IDs string read from system.input.
         *
         * @param shmemIds Shared memory IDs string.
         */
        public void shmemIds(String shmemIds) {
            this.shmemIds = (shmemIds == null) ? null :
                F.transform(shmemIds.split(","), new C1<String, Integer>() {
                    @Override public Integer apply(String s) {
                        return Long.valueOf(s).intValue();
                    }
                });
        }
    }
}
