/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.cloud.trace;

import com.hazelcast.config.Config;
import com.hazelcast.config.SemaphoreConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ISemaphore;
import fish.payara.appserver.micro.services.PayaraInstanceImpl;
import fish.payara.micro.data.InstanceDescriptor;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import fish.payara.nucleus.requesttracing.RequestTrace;
import fish.payara.nucleus.requesttracing.RequestTraceStore;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.internal.api.Globals;

/**
 *
 * @author Susan Rai
 */
public class CloudTraceServiceTask implements Runnable {

    private static final Logger logger = Logger.getLogger(CloudTraceService.class.getCanonicalName());
    private boolean dasInCluster = false;
    private StringBuilder traces;
    private String instanceID = "";
    private String newInstanceID = "";
    private ISemaphore semaphore;
    private RequestTraceStore requestTraceStore;
    private PayaraInstanceImpl payaraInstance;
    private HazelcastCore hzCore;
    private String cloudEndpointURL;
    private CloudTraceService cloudTraceService;

    public CloudTraceServiceTask(String cloudEndpointURL, RequestTraceStore requestTraceStore, PayaraInstanceImpl payaraInstance, HazelcastCore hzCore) {
        this.cloudEndpointURL = cloudEndpointURL;
        this.requestTraceStore = requestTraceStore;
        this.payaraInstance = payaraInstance;
        this.hzCore = hzCore;
        this.cloudTraceService = Globals.getDefaultHabitat().getService(CloudTraceService.class);
    }

    @Override
    public void run() {
        sendTraces();
    }

    public void sendTraces() {
        instanceID = cloudTraceService.getInstanceID();
        newInstanceID = cloudTraceService.getNewInstanceID();

        if (hzCore.isEnabled()) {
            HashMap<String, String> payaraClusterInfo = new HashMap<>();
            payaraClusterInfo = getPayaraCluster();
            String dasInstanceId = payaraClusterInfo.get("DAS").toString();

            // Check if DAS is in the cluster
            if (dasInCluster && instanceID.equals(dasInstanceId)) {
                sendToCloudEndpoint(getTraces());
                // Check if selected instance is already in the cluster, if so that instance should send the traces. Otherwise select a new Instance. 
            } else if (!instanceID.isEmpty() && checkIfInstanceIsInCluster(payaraClusterInfo, instanceID) && instanceID.equals(newInstanceID)) {
                sendToCloudEndpoint(getTraces());
            } else {
                selectNewInstanceID();
                if (newInstanceID != null) {
                    cloudTraceService.setInstanceID(newInstanceID);
                    sendToCloudEndpoint(getTraces());
                }
            }

        } else {
            // Send Local Tarces if hazelcast isn't enabled
            sendToCloudEndpoint(getTraces());
        }
    }

    private void selectNewInstanceID() {
        if (hzCore.isEnabled()) {
            HazelcastInstance hazelcastInstance = hzCore.getInstance();

            Config config = hazelcastInstance.getConfig();
            SemaphoreConfig semaphoreConfig = config.getSemaphoreConfig("semaphore");
            semaphoreConfig.setName("semaphore").setBackupCount(1)
                    .setInitialPermits(1);
            hazelcastInstance.getConfig().addSemaphoreConfig(semaphoreConfig);

            semaphore = hazelcastInstance.getSemaphore("semaphore");

            if (semaphore.tryAcquire()) {
                cloudTraceService.setNewInstanceID(hazelcastInstance.getCluster().getLocalMember().getUuid());
            }
        } else {
            logger.log(Level.SEVERE, "Hazlecst isn't enabled");
        }

    }

    public String getTraces() {
        traces = new StringBuilder();
        if (requestTraceStore.getStoreSize() > 0) {
            if (hzCore.isEnabled()) {
                Set<String> store = requestTraceStore.getClusteredRequestTraceStore().keySet();
                traces.append("[");
                for (String key : store) {
                    Collection<RequestTrace> trace = requestTraceStore.getClusteredRequestTraceStore().get(key);
                    for (RequestTrace requestTrace : trace) {
                        traces.append(requestTrace.toString().replace("=\"\"", "=\\\"\\\""));
                        traces.append(",");
                    }
                }
                traces.setLength(traces.length() - 1);
                traces.append("]");
            } else {
                traces.append("[");
                for (RequestTrace requestTrace : requestTraceStore.getLocalRequestTraceStore()) {
                    traces.append(requestTrace.toString().replace("=\"\"", "=\\\"\\\""));
                    traces.append(",");
                }
                traces.setLength(traces.length() - 1);
                traces.append("]");
            }
        } else {
            logger.log(Level.INFO, "TraceStore is empty");
        }
        return traces.toString();
    }

    private Boolean checkIfInstanceIsInCluster(HashMap<String, String> payaraClusterInfo, String instanceID) {
        Boolean result = false;
        for (Map.Entry entry : payaraClusterInfo.entrySet()) {
            if (entry.getValue().toString().equals(instanceID)) {
                result = true;
                break;
            }
        }
        return result;
    }

    private HashMap<String, String> getPayaraCluster() {
        HashMap<String, String> payaraClusterInfo = new HashMap<>();
        if (payaraInstance.isClustered()) {
            //Get the hazelcastInstance descriptors of the cluster members
            Set<InstanceDescriptor> instances = payaraInstance.getClusteredPayaras();
            for (InstanceDescriptor instance : instances) {
                String instanceType = instance.getInstanceType();
                if (instanceType != null) {
                    if (instanceType.equals("DAS")) {
                        dasInCluster = true;
                        // Set DAS UUID as Instance ID
                        cloudTraceService.setInstanceID(hzCore.getInstance().getCluster().getLocalMember().getUuid());
                    }
                    payaraClusterInfo.put(instanceType, instance.getMemberUUID());
                }
            }
        }
        return payaraClusterInfo;
    }

    private void sendToCloudEndpoint(String traces) {
        HttpURLConnection connection = null;
        if (traces.length() > 0) {
            try {
                URL url = new URL(cloudEndpointURL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                OutputStreamWriter streamWriter = new OutputStreamWriter(connection.getOutputStream());
                streamWriter.write(traces);
                streamWriter.flush();

            } catch (IOException exception) {
                logger.log(Level.SEVERE, "Connection not found", exception);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            this.traces.setLength(0);
            if (hzCore.isEnabled()) {
                requestTraceStore.clearClusteredRequestTraceStore();
            } else {
                requestTraceStore.clearLocalRequestTraceStore();
            }
        } else {
            logger.log(Level.INFO, "Nothing to send, it is likely the TraceStore is empty");
            this.traces.setLength(0);
        }
    }
}
