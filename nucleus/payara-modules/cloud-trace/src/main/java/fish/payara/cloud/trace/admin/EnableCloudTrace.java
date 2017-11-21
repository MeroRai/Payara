/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.cloud.trace.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.cloud.trace.CloudTraceService;
import fish.payara.cloud.trace.config.CloudTraceConfiguration;
import java.beans.PropertyVetoException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author susan
 */
@Service(name = "enable-cloud-trace")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("enable-cloud-trace")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "enable-cloud-trace",
            description = "Enables cloud trace service")
})
public class EnableCloudTrace implements AdminCommand {

    @Inject
    CloudTraceConfiguration cloudTraceConfiguration;

    @Inject
    CloudTraceService cloudTraceService;

    @Override
    public void execute(AdminCommandContext acc) {
        final ActionReport report = acc.getActionReport();
        try {
            ConfigSupport.apply(new SingleConfigCode<CloudTraceConfiguration>() {
                public Object run(CloudTraceConfiguration cloudTraceConfigurationProxy)
                        throws PropertyVetoException, TransactionFailure {

                    if (Boolean.parseBoolean(cloudTraceConfiguration.getEnabled())) {
                        Logger.getLogger(EnableCloudTrace.class.getName())
                                .log(Level.INFO,
                                        "Cloud Trace Service is already enabled");
                    } else {
                        cloudTraceConfigurationProxy.setEnabled(true);
                        Logger.getLogger(EnableCloudTrace.class.getName())
                                .log(Level.INFO,
                                        "Cloud Trace Service is enabled");
                    }

                    return null;
                }
            }, cloudTraceConfiguration);
        } catch (TransactionFailure ex) {
            Logger.getLogger(EnableCloudTrace.class.getName()).
                    log(Level.SEVERE, null, ex);
        }
        cloudTraceService.start();
        report.setMessage("Cloud Trace Service is enabled");
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
