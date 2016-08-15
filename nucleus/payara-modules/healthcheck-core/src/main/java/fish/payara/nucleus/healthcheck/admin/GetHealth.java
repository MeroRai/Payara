/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.nucleus.healthcheck.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.ColumnFormatter;
import fish.payara.nucleus.healthcheck.configuration.Checker;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.healthcheck.preliminary.BaseHealthCheck;
import java.util.HashMap;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author susan
 */
@Service(name = "get-health")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("get.health")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "get-health",
            description = "List HealthCheck Configuration")
})
public class GetHealth implements AdminCommand {

    @Inject
    ServiceLocator habitat;

    @Inject
    private Target targetUtil;

    @Param(name = "target", optional = true, defaultValue = "server")
    private String target;

    @Override
    public void execute(AdminCommandContext context) {
        Config config = targetUtil.getConfig(target);
        if (config == null) {
            context.getActionReport().setMessage("No such config named: " + target);
            context.getActionReport().setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        ActionReport mainActionReport = context.getActionReport();
        String headers[] = {"Name", "Enabled", "Time", "Unit", "Service Name"};
        ColumnFormatter columnFormatter = new ColumnFormatter(headers);

        HealthCheckServiceConfiguration configuration = config.getExtensionByType(HealthCheckServiceConfiguration.class);
        List<ServiceHandle<BaseHealthCheck>> allServiceHandles = habitat.getAllServiceHandles(BaseHealthCheck.class);
        for (ServiceHandle<BaseHealthCheck> serviceHandle : allServiceHandles) {
            Checker checker = configuration.getCheckerByType(serviceHandle.getService().getCheckerType());
            if (checker != null) {
                Object values[] = new Object[5];
                values[0] = checker.getName();
                values[1] = checker.getEnabled();
                values[2] = checker.getTime();
                values[3] = checker.getUnit();
                values[4] = serviceHandle.getActiveDescriptor().getName();
                columnFormatter.addRow(values);
                
                Map<String, Object> map = new HashMap<String, Object>(5);
                Properties extraProps = new Properties();
                map.put("name", values[0]);
                map.put("enabled", values[1]);
                map.put("time", values[2]);
                map.put("unit", values[3]);
                map.put("serviceName", values[4]);
                
                extraProps.put("getHealth", map);
                mainActionReport.setExtraProperties(extraProps);
                mainActionReport.setMessage(columnFormatter.toString());
                mainActionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            }
        }
    }
}
