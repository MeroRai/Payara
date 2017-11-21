/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.cloud.trace;

import fish.payara.appserver.micro.services.PayaraInstanceImpl;
import fish.payara.cloud.trace.config.CloudTraceConfiguration;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import fish.payara.nucleus.requesttracing.RequestTraceStore;
import fish.payara.nucleus.requesttracing.RequestTrace;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import fish.payara.micro.data.InstanceDescriptor;
import java.util.Collection;

/**
 *
 * @author Susan Rai
 */
@Service(name = "cloud-trace")
@RunLevel(StartupRunLevel.VAL)
public class CloudTraceService implements EventListener {

    private static final Logger logger = Logger.getLogger(CloudTraceService.class.getCanonicalName());
    //private static final String CLOUD_ENDPOINT_URL = "http://httpbin.org/post";
    private StringBuilder sb;
    private boolean enabled;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    CloudTraceConfiguration cloudTraceConfiguration;

    @Inject
    Events events;

    @Inject
    RequestTraceStore requestTraceStore;

    @Inject
    private PayaraInstanceImpl payaraInstance;

    @Inject
    HazelcastCore hzCore;

    @Inject
    ServiceLocator habitat;

    private HashMap<String, String> payaraClusterInfo;
    private String instanceID;

    @PostConstruct
    public void bootService() {
        events.register(this);
        cloudTraceConfiguration = habitat.getService(CloudTraceConfiguration.class);
        enabled = Boolean.valueOf(cloudTraceConfiguration.getEnabled());
    }

    public boolean isEnabled() {
        boolean enabled = false;
        if (cloudTraceConfiguration == null) {
            Logger.getLogger(CloudTraceService.class.getName()).
                    log(Level.FINE, "No Cloud Trace Service configuration found, it is likely missing from the"
                            + " domain.xml. Setting enabled to default of false");
        } else {
            enabled = Boolean.parseBoolean(cloudTraceConfiguration.getEnabled());
        }
        return enabled;
    }

    @Override
    public void event(Event event) {
    }

    public void sendTraces() {
        if (hzCore.isEnabled()) {
            //Check if DAS is in the cluster
            if (getPayaraCluster().contains("DAS") && !payaraClusterInfo.isEmpty()) {
                sendToCloudEndpoint(cloudTraceConfiguration.getURL(), getTraces());
                //System.out.println("Instance id of DAS = " + payaraClusterInfo.get("DAS").toString());
                //instanceID = payaraClusterInfo.get("DAS").toString();
            } else if (!instanceID.isEmpty() && checkIfInstanceIsInCluster(instanceID)) {
                getTraces();
                sendToCloudEndpoint(cloudTraceConfiguration.getURL(), getTraces());
            } else {
                //Randomly select a member from cluster
                Random random = new Random();
                List<String> keys = new ArrayList<String>(payaraClusterInfo.keySet());
                String randomKey = keys.get(random.nextInt(keys.size()));
                instanceID = payaraClusterInfo.get(randomKey);
                sendToCloudEndpoint(cloudTraceConfiguration.getURL(), getTraces());
            }
        } else {
            System.out.println("Send Local Store ");
            sendToCloudEndpoint(cloudTraceConfiguration.getURL(), getTraces());
        }
    }

    public String getTraces() {
        sb = new StringBuilder();
        if (hzCore.isEnabled()) {
            Set<String> store = requestTraceStore.getClusteredRequestTraceStore().keySet();
            sb.append("[");
            int traceNumber = 0;
            for (String key : store) {
                traceNumber++;
                System.out.println("key" + traceNumber + " = " + key);
                Collection<RequestTrace> trace = requestTraceStore.getClusteredRequestTraceStore().get(key);
                for (RequestTrace requestTrace : trace) {
                    sb.append(requestTrace.toString().replace("=\"\"", "=\\\"\\\""));
                    sb.append(",");
                }
            }
            sb.setLength(sb.length() - 1);
            sb.append("]");
        } else {
            sb.append("[");
            for (RequestTrace requestTrace : requestTraceStore.getLocalRequestTraceStore()) {
                sb.append(requestTrace.toString().replace("=\"\"", "=\\\"\\\""));
                sb.append(",");
            }
            sb.setLength(sb.length() - 1);
            sb.append("]");
        }
        return sb.toString();
    }

    private Boolean checkIfInstanceIsInCluster(String instanceID) {
        Boolean result = false;
        for (Map.Entry entry : payaraClusterInfo.entrySet()) {
            if (entry.getValue().toString().equals(instanceID)) {
                result = true;
                break;
            }
        }
        return result;
    }

    private String getPayaraCluster() {
        StringBuilder stringBuilder = new StringBuilder();
        payaraClusterInfo = new HashMap<>();
        if (payaraInstance.isClustered()) {
            //Get the instance descriptors of the cluster members
            Set<InstanceDescriptor> instances = payaraInstance.getClusteredPayaras();
            for (InstanceDescriptor instance : instances) {
                String instanceType = instance.getInstanceType();
                if (instanceType != null) {
                    payaraClusterInfo.put(instanceType, instance.getMemberUUID());
                    System.out.println("Member type = " + instanceType + " Instance ID = " + instance.getMemberUUID());
                    stringBuilder.append(instanceType).append(",");
                }
            }
        }
        return stringBuilder.toString();
    }

    private void sendToCloudEndpoint(String cloudEndpointUrl, String traces) {
        HttpURLConnection connection = null;
        if (traces != null) {
            try {
                URL url = new URL(cloudEndpointUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                OutputStreamWriter streamWriter = new OutputStreamWriter(connection.getOutputStream());
                streamWriter.write(traces);
                streamWriter.flush();

                /* testing */
                StringBuilder stringBuilder = new StringBuilder();
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStreamReader streamReader = new InputStreamReader(connection.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(streamReader);
                    String response = null;
                    while ((response = bufferedReader.readLine()) != null) {
                        stringBuilder.append(response + "\n");
                    }
                    bufferedReader.close();
                    System.out.println("Cloud Enpoint reponseeeeee =" + stringBuilder.toString());
                    //return stringBuilder.toString();
                } else {
                    System.out.println("testsdsadas =" + connection.getResponseMessage());
                    //Log.e("test", connection.getResponseMessage());
                    //return null;
                }

                /* End Of test */
            } catch (IOException exception) {
                logger.log(Level.SEVERE, "Connection not found", exception);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            // listOfRequestTraceJsonObject.clear();
            sb.setLength(0);
        } else {
            logger.log(Level.INFO, "Nothing to send, it is likely the TraceStore is empty");
            //listOfRequestTraceJsonObject.clear();
            sb.setLength(0);
        }
    }

    private void bootStarpCloudTraceService() {

        if (enabled) {
            //sendToCloudEndpoint(cloudTraceConfiguration.getURL());
            System.out.println("Sb length = " + sb.length());
            sendTraces();
            System.out.println("sTARTED");
        }

    }

    private void shutDownCloudTraceService() {
        System.out.println("Shutdown");
    }

    public void start() {
        if (this.enabled) {
            shutDownCloudTraceService();
            bootStarpCloudTraceService();
        } else {
            this.enabled = true;
            bootStarpCloudTraceService();
        }
    }

    public void stop() {
        if (this.enabled) {
            this.enabled = false;
            shutDownCloudTraceService();
        }
    }
}
