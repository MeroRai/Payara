/*
 *
 * Copyright (c) 2016-2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.nucleus.requesttracing;

import com.hazelcast.core.MultiMap;
import fish.payara.nucleus.notification.domain.BoundedTreeSet;
import fish.payara.nucleus.requesttracing.store.ReservoirBoundedTreeSet;
import fish.payara.nucleus.store.ClusteredStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.Random;
import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventListener.Event;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;

/**
 * Stores request traces with descending elapsed time. Comparator is implemented
 * on {@link RequestTrace}
 *
 * @author mertcaliskan
 */
@Service
@Singleton
public class RequestTraceStore {

    private static final String REQUEST_TRACE_STORE = "REQUEST_TRACE_STORE";
    public static final EventTypes TRACE_STORE_FULL = EventTypes.create("trace_store_full");

    @Inject
    private ClusteredStore clusteredStore;

    @Inject
    Events events;

    private String instanceId;
    private int storeSize;

    private boolean isClustered;
    private boolean reservoirStyle;
    private long reservoirCounter;
    private Random random;
    private MultiMap<String, RequestTrace> clusteredTraceStore;
    private BoundedTreeSet<RequestTrace> localTraceStore;

    protected void initialize(int storeSize, boolean reservoirSamplingEnabled) {
        this.storeSize = storeSize;
        this.reservoirStyle = reservoirSamplingEnabled;
        reservoirCounter = 0;
        random = new Random();

        if (clusteredStore.isEnabled()) {
            isClustered = true;

            instanceId = clusteredStore.getInstanceId();
            System.out.println("Instance Id = " + instanceId);
            MultiMap clusteredTracesMultiMap = clusteredStore.getMultiMap(REQUEST_TRACE_STORE);

            if (clusteredTracesMultiMap != null) {
                clusteredTraceStore = clusteredTracesMultiMap;
            } else {
                // If we've returned null, something has gone wrong with Hazelcast, so go back to offline behaviour
                isClustered = false;
                localTraceStore = new BoundedTreeSet<>(storeSize);
            }
        } else if (reservoirStyle) {
            isClustered = false;
            localTraceStore = new ReservoirBoundedTreeSet<>(storeSize);
        } else {
            isClustered = false;
            localTraceStore = new BoundedTreeSet<>(storeSize);
        }
    }

    protected void addTrace(long elapsedTime, RequestTrace requestTrace) {
        if (isClustered) {
            //if (clusteredTraceStore.get(instanceId).size() > storeSize) {
            if (reservoirStyle) {
                if (reservoirCounter < Long.MAX_VALUE) {
                    reservoirCounter++;
                }

                // If the store isn't full yet, just add the item
                if (clusteredTraceStore.valueCount(instanceId) < storeSize) {
                    clusteredTraceStore.put(instanceId, requestTrace);
                } else {
                    // Probability of keeping the new item
                    double probability = (double) storeSize / reservoirCounter;
                    boolean keepItem = random.nextDouble() < probability;

                    if (keepItem) {
                        // Replace a random item in the list
                        clusteredTraceStore.remove(instanceId,
                                clusteredTraceStore.get(instanceId).toArray()[random.nextInt(storeSize - 1)]);
                        clusteredTraceStore.put(instanceId, requestTrace);
                    }
                }
            } else {
                clusteredTraceStore.put(instanceId, requestTrace);
                //List<RequestTrace> traces = sortLocalClusteredRequestTraces();

                if (clusteredTraceStore.get(instanceId).size() > storeSize) {
                    List<RequestTrace> traces = sortLocalClusteredRequestTraces();
                    clusteredTraceStore.remove(instanceId, traces.get(traces.size() - 1));
                }

//                    if (traces.get(traces.size() -1).compareTo(requestTrace) < 0) {
//                        clusteredTraceStore.remove(instanceId, traces.get(traces.size() - 1));
//                        clusteredTraceStore.put(instanceId, requestTrace);
//                    }
            }
            //}
        } else {
            localTraceStore.add(requestTrace);
        }

        if (isClustered()) {
            if (clusteredTraceStore.size() >= storeSize) {
                System.out.println("clustered Trace store full send traces");
                events.send(new Event(TRACE_STORE_FULL));
            }
        } else if (localTraceStore.size() >= storeSize) {
            System.out.println("Trace store full send traces");
            events.send(new Event(TRACE_STORE_FULL));
        }
//        if (clusteredTraceStore != null || localTraceStore != null) {
//            //System.out.println("ClustertraceStoreSize = " + clusteredTraceStore.size());
//            System.out.println("LocaltraceStore = " + localTraceStore.size());
//            if (localTraceStore.size() >= storeSize || clusteredTraceStore.size() >= storeSize) {
//                System.out.println("Trace store full send traces");
//                events.send(new Event(TRACE_STORE_FULL));
//            }
//        }
    }

    public RequestTrace[] getTraces() {
        RequestTrace[] traces = new RequestTrace[0];

        if (isClustered) {
            traces = clusteredTraceStore.get(instanceId).toArray(traces);
        } else {
            traces = localTraceStore.toArray(traces);
        }

        return traces;
    }

    public RequestTrace[] getTraces(Integer limit) {
        RequestTrace[] traces;

        if (isClustered) {
            traces = copyToArray(sortLocalClusteredRequestTraces().toArray(
                    new RequestTrace[clusteredTraceStore.valueCount(instanceId)]), limit);
        } else {
            traces = copyToArray(localTraceStore.toArray(new RequestTrace[localTraceStore.size()]), limit);
        }

        return traces;
    }

    private RequestTrace[] copyToArray(RequestTrace[] traceStore, Integer limit) {
        RequestTrace[] traces;
        if (limit < traceStore.length) {
            traces = new RequestTrace[limit];
            System.arraycopy(traceStore, 0, traces, 0, limit);
        } else {
            traces = traceStore;
        }

        return traces;
    }

    public boolean isClustered() {
        return isClustered;
    }

    public int getStoreSize() {
        return storeSize;
    }

    public void setStoreSize(int storeSize) {
        this.storeSize = storeSize;
    }

    public NavigableSet<RequestTrace> getLocalRequestTraceStore() {
        return localTraceStore;
    }

    public Collection<RequestTrace> getLocalClusteredRequestTraces() {
        return clusteredTraceStore.get(instanceId);
    }

    public MultiMap<String, RequestTrace> getClusteredRequestTraceStore() {
        return clusteredTraceStore;
    }

    public void clearClusteredRequestTraceStore() {
        clusteredTraceStore.clear();
    }

    public void clearLocalRequestTraceStore() {
        localTraceStore.clear();
    }

    private List<RequestTrace> sortLocalClusteredRequestTraces() {
        List<RequestTrace> traces = new ArrayList(clusteredTraceStore.get(instanceId));
        Collections.sort(traces);
        return traces;
    }
}
