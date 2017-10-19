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
import java.util.Collection;
import java.util.NavigableSet;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Stores historic request traces with descending elapsed time. Comparator is implemented on {@link HistoricRequestTracingEvent}
 *
 * @author mertcaliskan
 */
@Service
@Singleton
public class RequestTraceStore {

    private static final String REQUEST_TRACE_STORE = "REQUEST_TRACE_STORE";

    @Inject
    private ClusteredStore clusteredStore;

    private String instanceId;
    private int storeSize;
    
    private boolean isClustered;
    private MultiMap<String, RequestTrace> clusteredTraceStore;
    private BoundedTreeSet<RequestTrace> localTraceStore;
    
    protected void initialize(int storeSize, boolean reservoirSamplingEnabled) {
        this.storeSize = storeSize;
        
        if (clusteredStore.isEnabled()) {
            isClustered = true;
            
            instanceId = clusteredStore.getInstanceId();
            MultiMap clusteredTracesMultiMap = clusteredStore.getMultiMap(REQUEST_TRACE_STORE);

            if (clusteredTracesMultiMap != null) {
                clusteredTraceStore = clusteredTracesMultiMap;
            }
        } else if (reservoirSamplingEnabled) {
            localTraceStore = new ReservoirBoundedTreeSet<>(storeSize);
        } else {
            isClustered = false;
            localTraceStore = new BoundedTreeSet<>(storeSize);
        }
    }

    protected void addTrace(long elapsedTime, RequestTrace requestTrace) {
        if (isClustered) {
            clusteredTraceStore.put(instanceId, requestTrace);
            
            if (clusteredTraceStore.get(instanceId).size() > storeSize) {
                clusteredTraceStore.remove(instanceId, )
            }
        } else {
            localTraceStore.add(requestTrace);
        }
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
            traces = copyToArray(clusteredTraceStore.get(instanceId).toArray(new RequestTrace[clusteredTraceStore.valueCount(instanceId)]), 
                    limit);
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
    
    public NavigableSet<RequestTrace> getLocalRequestTraceStore() {
        return localTraceStore;
    }
    
    public Collection<RequestTrace> getLocalClusteredRequestTraces() {
        return clusteredTraceStore.get(instanceId);
    }
    
    public MultiMap<String, RequestTrace> getClusteredRequestTraceStore() {
        return clusteredTraceStore;
    }
    
    
}
