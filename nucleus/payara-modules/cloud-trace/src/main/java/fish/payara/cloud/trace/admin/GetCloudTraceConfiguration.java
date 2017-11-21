/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.cloud.trace.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.ColumnFormatter;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.cloud.trace.config.CloudTraceConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
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
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author susan
 */
@Service(name = "get-cloud-trace-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("get.cloud.trace.configuration")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "get-cloud-trace-configuration",
            description = "Gets the current configuration settings of the Cloud Trace Service")
})
public class GetCloudTraceConfiguration implements AdminCommand {

    @Inject
    protected Target targetUtil;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    private final String[] headers = {"Enabled", "Frequency", "Traces", "URL"};

    @Override
    public void execute(AdminCommandContext context) {
        Config config = targetUtil.getConfig(target);
        if (config == null) {
            context.getActionReport().setMessage("No such config named: " + target);
            context.getActionReport().setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        CloudTraceConfiguration cloudTraceConfiguration
                = config.getExtensionByType(CloudTraceConfiguration.class);
        final ActionReport actionReport = context.getActionReport();

        ColumnFormatter columnFormatter = new ColumnFormatter(headers);
        Object values[] = {cloudTraceConfiguration.getEnabled(),
            cloudTraceConfiguration.getFrequencyUnit(),
            cloudTraceConfiguration.getTraces(),
            cloudTraceConfiguration.getURL()};

        columnFormatter.addRow(values);

        Map<String, Object> map = new HashMap<String, Object>();
        Properties extraProps = new Properties();
        map.put("enabled", values[0]);
        map.put("frequency", values[1]);
        map.put("traces", values[2]);
        map.put("url", values[3]);

        extraProps.put("getCloudTraceServiceConfiguration", map);

        actionReport.setExtraProperties(extraProps);

        actionReport.setMessage(columnFormatter.toString());
        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
