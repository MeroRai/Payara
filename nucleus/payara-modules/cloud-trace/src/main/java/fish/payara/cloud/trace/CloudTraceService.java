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

import fish.payara.appserver.micro.services.PayaraInstanceImpl;
import fish.payara.cloud.trace.config.CloudTraceConfiguration;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import fish.payara.nucleus.events.HazelcastEvents;
import fish.payara.nucleus.requesttracing.RequestTraceStore;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.glassfish.api.event.EventTypes;

/**
 *
 * @author Susan Rai
 */
@Service(name = "cloud-trace")
@RunLevel(StartupRunLevel.VAL)
public class CloudTraceService implements EventListener {

    private static final Logger logger = Logger.getLogger(CloudTraceService.class.getCanonicalName());
    private static final String THREAD_NAME = "Cloud-trace-service";
    private boolean enabled;
    private ScheduledExecutorService executor;
    private String instanceID = "";
    private String newInstanceID = "";

    @Inject
    private CloudTraceConfiguration cloudTraceConfiguration;

    @Inject
    private Events events;

    @Inject
    private HazelcastCore hzCore;

    @Inject
    private ServiceLocator habitat;

    @Inject
    private RequestTraceStore requestTraceStore;

    @Inject
    private PayaraInstanceImpl payaraInstance;

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
        // If Hazelcast is enabled, wait for it, otherwise just bootstrap when the server is ready
        if (hzCore.isEnabled()) {
            if (event.is(HazelcastEvents.HAZELCAST_BOOTSTRAP_COMPLETE)) {
                bootStarpCloudTraceService();
            }
        } else if (event.is(EventTypes.SERVER_READY)) {
            bootStarpCloudTraceService();
        } else if (event.is(EventTypes.SERVER_SHUTDOWN)) {
            shutDownCloudTraceService();
        }

        if (event.is(requestTraceStore.TRACE_STORE_FULL)) {
            bootStarpCloudTraceService();
        }
    }

    private void bootStarpCloudTraceService() {
        if (enabled) {
            Long frequencyValue = Long.valueOf(cloudTraceConfiguration.getFrequencyValue()).longValue();
            TimeUnit frequencyUnit = TimeUnit.valueOf(cloudTraceConfiguration.getFrequencyUnit());
            String cloudEndpointURL = cloudTraceConfiguration.getCloudEndpointUrl();
            executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    return new Thread(r, THREAD_NAME);
                }
            });
            executor.scheduleAtFixedRate(new CloudTraceServiceTask(cloudEndpointURL, requestTraceStore, payaraInstance, hzCore), 0, frequencyValue, frequencyUnit);
        }
    }

    private void shutDownCloudTraceService() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
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

    public String getInstanceID() {
        return instanceID;
    }

    public void setInstanceID(String instanceID) {
        this.instanceID = instanceID;
    }

    public String getNewInstanceID() {
        return newInstanceID;
    }

    public void setNewInstanceID(String NewInstanceID) {
        this.newInstanceID = NewInstanceID;
    }
}
