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

package ws.wamp.jawampa.internal;

import java.util.concurrent.TimeUnit;

import rx.Subscription;
import rx.functions.Action0;
import rx.internal.schedulers.ScheduledAction;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/**
 * A single threaded scheduler based on the RX computation scheduler.
 */
public class SingleThreadedComputationScheduler extends rx.Scheduler {
    /** 
     * A single threaded worker from the computation threadpool on which
     * we we will base our workers
     */
	final rx.Scheduler.Worker innerWorker = Schedulers.computation().createWorker();
    
    @Override
    public Worker createWorker() {
        return new SchedulerWorker(innerWorker);
    }
    
    private static class SchedulerWorker extends rx.Scheduler.Worker {
        final CompositeSubscription innerSubscription = new CompositeSubscription();
        final rx.Scheduler.Worker innerWorker;

        public SchedulerWorker(rx.Scheduler.Worker innerWorker) {
            this.innerWorker = innerWorker;
        }

        @Override
        public void unsubscribe() {
            innerSubscription.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return innerSubscription.isUnsubscribed();
        }

        @Override
        public Subscription schedule(Action0 action) {
            return schedule(action, 0, null);
        }

        @Override
        public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
            if (innerSubscription.isUnsubscribed()) {
                // don't schedule, we are unsubscribed
                return Subscriptions.empty();
            }
            
            ScheduledAction s = (ScheduledAction)innerWorker.schedule(action, delayTime, unit);
            innerSubscription.add(s);
            s.addParent(innerSubscription);
            return s;
        }
    }
}