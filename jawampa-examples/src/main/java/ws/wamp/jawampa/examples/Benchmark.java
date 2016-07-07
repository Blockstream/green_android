/*
 * Copyright 2014 Matthias Einwag
 *
 * The jawampa authors license this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package ws.wamp.jawampa.examples;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import rx.Subscription;
import rx.functions.Action1;
import ws.wamp.jawampa.ApplicationError;
import ws.wamp.jawampa.Request;
import ws.wamp.jawampa.WampRouter;
import ws.wamp.jawampa.WampRouterBuilder;
import ws.wamp.jawampa.WampClient;
import ws.wamp.jawampa.WampClientBuilder;
import ws.wamp.jawampa.connection.IWampConnectorProvider;
import ws.wamp.jawampa.transport.netty.NettyWampClientConnectorProvider;
import ws.wamp.jawampa.transport.netty.SimpleWampWebsocketListener;

public class Benchmark {
    
    public static void main(String[] args) {
        new Benchmark().start();
    }
    
    final static int TOTAL_CALLS = 10000;
    int nrCalls = TOTAL_CALLS;
    int nrErrors = 0;
    long startTime;
    long stopTime;
    
    CountDownLatch waitLatch = new CountDownLatch(1);
    
    Subscription procSubscription;
    WampClient client1;
    WampClient client2;
    
    public void start() {
        
        WampRouterBuilder routerBuilder = new WampRouterBuilder();
        WampRouter router;
        try {
            routerBuilder.addRealm("realm2");
            router = routerBuilder.build();
        } catch (ApplicationError e1) {
            e1.printStackTrace();
            return;
        }
        
        URI serverUri = URI.create("ws://0.0.0.0:8080/bench");
        SimpleWampWebsocketListener server;
        
        IWampConnectorProvider connectorProvider = new NettyWampClientConnectorProvider();

        try {
            server = new SimpleWampWebsocketListener(router, serverUri, null);
            server.start();

            WampClientBuilder builder = new WampClientBuilder();
            
            builder.withConnectorProvider(connectorProvider)
                   .withUri("ws://localhost:8080/bench")
                   .withRealm("realm2")
                   .withInfiniteReconnects()
                   .withReconnectInterval(1, TimeUnit.SECONDS);
            client1 = builder.build();
            client2 = builder.build();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        
        client1.statusChanged().subscribe(new Action1<WampClient.State>() {
            @Override
            public void call(WampClient.State t1) {
                System.out.println("Session status changed to " + t1);

                if (t1 instanceof WampClient.ConnectedState) {
                    
                    // Provide a procedure
                    procSubscription = client1
                        .registerProcedure("testfunc")
                        .subscribe(new Action1<Request>() {
                        @Override
                        public void call(Request request) {
                            if (request.arguments() == null || request.arguments().size() != 1
                             || !request.arguments().get(0).canConvertToLong())
                            {
                                try {
                                    request.replyError(new ApplicationError(ApplicationError.INVALID_PARAMETER));
                                } catch (ApplicationError e) {
                                    e.printStackTrace();
                                }
                            } else {
                                long a = request.arguments().get(0).asLong();
                                request.reply(a);
                            }
                        }
                    });
                }
            }
        });

        client2.statusChanged().subscribe(new Action1<WampClient.State>() {
            @Override
            public void call(WampClient.State t1) {
                System.out.println("Session status changed to " + t1);

                if (t1 instanceof WampClient.ConnectedState) {
                    try {
                        // Wait until the other client could register the procedure
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    startTime = System.currentTimeMillis();
                    makeNextCall();
                }
            }
        });

        client1.open();
        client2.open();
        
        try {
            waitLatch.await();
        } catch (InterruptedException e1) { }
            
        long duration = stopTime - startTime;
        System.out.println("Benchmark took " + duration + "ms");
        double durationS = (double) duration / (1000.0);
        System.out.println(TOTAL_CALLS + " took " + durationS + "s");
        double callsPerS = (double) TOTAL_CALLS / durationS;
        System.out.println("Equals " + callsPerS + " calls/s");
        
        System.out.println("Nr of errors: " + nrErrors);
        
        client1.close().toBlocking().last();
        client2.close().toBlocking().last();
        
        server.stop();
        
        router.close().toBlocking().last();
    }
    
    void makeNextCall() {
        client2.call("testfunc", Integer.class, 7).subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer t1) {
                nrCalls--;
                if (nrCalls != 0) {
                    makeNextCall();
                } else {
                    stopTime = System.currentTimeMillis();
                    waitLatch.countDown();
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable t1) {
                nrCalls--;
                nrErrors++;
                if (nrCalls != 0) {
                    makeNextCall();
                } else {
                    stopTime = System.currentTimeMillis();
                    waitLatch.countDown();
                }
            }
            
        });
    }
}
