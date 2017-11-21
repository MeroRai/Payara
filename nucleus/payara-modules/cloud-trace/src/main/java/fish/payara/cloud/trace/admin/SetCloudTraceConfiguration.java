/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.cloud.trace.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.cloud.trace.CloudTraceService;
import fish.payara.cloud.trace.config.CloudTraceConfiguration;
import java.beans.PropertyVetoException;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.Properties;
import java.util.logging.Logger;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author susan
 */
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@Service(name = "set-cloud-trace-configuration")
@CommandLock(CommandLock.LockType.NONE)
@PerLookup
@I18n("set.cloud.trace.configuration")
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-cloud-trace-configuration",
            description = "Configures the Cloud Trace Service")
})
public class SetCloudTraceConfiguration implements AdminCommand {
    
    @Inject
    protected Target targetUtil;
    
    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;
    
    @Param(name = "enabled", optional = false)
    private Boolean enabled;
    
    @Param(name = "frequency", optional = true)
    private String frequency;
    
    @Param(name = "trace", optional = true)
    private String trace;
    
    @Param(name = "url", optional = true)
    private String URL;
    @Inject
    CloudTraceService cloudTraceService;
    
    @Inject
    CloudTraceConfiguration cloudTraceConfiguration;
    
    @Override
    public void execute(AdminCommandContext acc) {
        final ActionReport actionReport = acc.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

//        Config config = targetUtil.getConfig(target);
//        final CloudTraceConfiguration cloudTraceConfiguration = config.getExtensionByType(CloudTraceConfiguration.class);
//        if (cloudTraceConfiguration != null) {
        try {
            // to perform a transaction on the domain.xml you need to use this construct
            // see https://github.com/hk2-project/hk2/blob/master/hk2-configuration/persistence/hk2-xml-dom/hk2-config/src/main/java/org/jvnet/hk2/config/ConfigSupport.java
            ConfigSupport.apply(new SingleConfigCode<CloudTraceConfiguration>() {
                @Override
                public Object run(CloudTraceConfiguration config) throws PropertyVetoException {
                    if (enabled != null) {
                        config.setEnabled(enabled);
                        cloudTraceService.start();
                    }
                    
                    if (frequency != null) {
                        config.setFrequencyUnit(frequency);
                    }
                    
                    String traces = cloudTraceService.getTraces().toString();
                    if (traces != null) {
                        config.setTraces(traces);
                    }
                    
                    if (URL != null) {
                        config.setURL(URL);
                    }
                    return null;
                }
                
            }, cloudTraceConfiguration);
        } catch (TransactionFailure ex) {
            // set failure
            actionReport.failure(Logger.getLogger(SetCloudTraceConfiguration.class.getName()),
                    "Failed to update configuration due to: " + ex.getLocalizedMessage(), ex);
        }
        
    }
    
}
