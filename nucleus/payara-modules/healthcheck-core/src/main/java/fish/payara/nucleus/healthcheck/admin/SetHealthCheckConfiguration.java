/*
DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.


Copyright (c) 2016 Payara Foundation. All rights reserved.


The contents of this file are subject to the terms of the Common Development
and Distribution License("CDDL") (collectively, the "License").  You
may not use this file except in compliance with the License.  You can
obtain a copy of the License at
https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
or packager/legal/LICENSE.txt.  See the License for the specific
language governing permissions and limitations under the License.

When distributing the software, include this License Header Notice in each
file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.healthcheck.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.util.Properties;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Susan Rai
 */
@Service(name = "set-healthcheck-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("set.healthcheck.configuration")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-healthcheck-configuration",
            description = "Set Health Check Services Configuration")
})
public class SetHealthCheckConfiguration implements AdminCommand {

    @Param(name = "serviceDynamic", optional = true, defaultValue = "false")
    protected Boolean serviceDynamic;

     @Param(name = "target", optional = true, defaultValue = "server")
    protected String target;

    @Param(name = "serviceEnabled", optional = false)
    private Boolean serviceEnabled;

    @Param(name = "enabled", optional = false)
    private Boolean enabled;

    @Param(name = "time", optional = true, defaultValue = "5")
    private String time;

    @Param(name = "unit", optional = true, defaultValue = "MINUTES")
    private String unit;

    @Param(name = "serviceName", optional = false)
    private String serviceName;

    @Param(name = "name", optional = true)
    private String name;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;

    @Inject
    ServiceLocator serviceLocator;    
    
//    @Inject
//    Checker checkerProxy;

    @Inject
    protected Logger logger;

    @Override
    public void execute(AdminCommandContext context) {
        final AdminCommandContext theContext = context;
        final ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        if (serviceDynamic || serviceEnabled) {
            enableHealthCheckConfigurer(actionReport, context, enabled);
        }

        if (dynamic || enabled) {
//            if (name != null) {
//                try {
//                    checkerProxy.setName(name);
//                } catch (PropertyVetoException ex) {
//                    logger.log(Level.SEVERE, null, ex);
//                }
//            }
            enableHealthCheckServiceConfigurer(actionReport, context, serviceEnabled);
        }
    }

    private void enableHealthCheckConfigurer(ActionReport actionReport, AdminCommandContext context, Boolean enabled) {
        CommandRunner runner = serviceLocator.getService(CommandRunner.class);
        ActionReport subReport = context.getActionReport().addSubActionsReport();
        CommandRunner.CommandInvocation inv;
        inv = runner.getCommandInvocation("healthcheck-configure", subReport, context.getSubject());

        ParameterMap params = new ParameterMap();
        params.add("enabled", enabled.toString());
        params.add("target", target);
        params.add("dynamic", serviceDynamic.toString());
        inv.parameters(params);
        inv.execute();
        // swallow the offline warning as it is not a problem
        if (subReport.hasWarnings()) {
            subReport.setMessage("");
        }
    }

    private void enableHealthCheckServiceConfigurer(ActionReport actionReport, AdminCommandContext context, Boolean enabled) {
        CommandRunner runner = serviceLocator.getService(CommandRunner.class);
        ActionReport subReport = context.getActionReport().addSubActionsReport();
        CommandRunner.CommandInvocation inv;
        inv = runner.getCommandInvocation("healthcheck-configure-service", subReport, context.getSubject());

        ParameterMap params = new ParameterMap();
        params.add("enabled", enabled.toString());
        params.add("target", target);
        params.add("dynamic", dynamic.toString());
        params.add("time", time);
        params.add("unit", unit);
        params.add("name", name);
        params.add("serviceName", serviceName);
        inv.parameters(params);
        inv.execute();
        // swallow the offline warning as it is not a problem
        if (subReport.hasWarnings()) {
            subReport.setMessage("");
        }

    }

}
