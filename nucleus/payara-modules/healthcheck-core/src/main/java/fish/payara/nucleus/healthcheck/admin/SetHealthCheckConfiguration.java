/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2016 C2B2 Consulting Limited. All rights reserved.
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
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.util.Properties;
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
@ExecuteOn({RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@Service(name = "set-healthcheck-configuration")
@CommandLock(CommandLock.LockType.NONE)
@PerLookup
@I18n("set.healthcheck.configuration")
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-healthcheck-configuration",
            description = "Set HealthCheck Services Configuration")
})
public class SetHealthCheckConfiguration implements AdminCommand {

    final private static LocalStringManagerImpl strings = new LocalStringManagerImpl(HealthCheckConfigurer.class);

    @Inject
    ServiceLocator serviceLocator;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    protected String target;

    @Param(name = "enabled", optional = false)
    private Boolean enabled;

//    @Param(name = "serviceEnabled", optional = false)
//    private Boolean serviceEnabled;
    @Param(name = "time", optional = true)
    private String time;

    @Param(name = "unit", optional = true)
    private String unit;

    @Param(name = "serviceName", optional = false)
    private String serviceName;

    @Param(name = "name", optional = true)
    private String name;

//    @Param(name = "thresholdCritical", optional = true, defaultValue = "90")
//    private String thresholdCritical;
//
//    @Param(name = "thresholdWarning", optional = true, defaultValue = "50")
//    private String thresholdWarning;
//
//    @Param(name = "thresholdGood", optional = true, defaultValue = "0")
//    private String thresholdGood;
    CommandRunner.CommandInvocation inv;

    @Override
    public void execute(AdminCommandContext context) {
        final AdminCommandContext theContext = context;
        final ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        enableHealthCheckConfigureOnTarget(actionReport, theContext, enabled);
        enableHealthCheckServiceConfigureOnTarget(actionReport, theContext, enabled);
//        enableHealthCheckServiceThresholdConfigureOnTarget(actionReport, theContext);
    }

    private void enableHealthCheckConfigureOnTarget(ActionReport actionReport, AdminCommandContext context, Boolean enabled) {
        CommandRunner runner = serviceLocator.getService(CommandRunner.class);
        ActionReport subReport = context.getActionReport().addSubActionsReport();

        inv = runner.getCommandInvocation("healthcheck-configure", subReport, context.getSubject());

        ParameterMap params = new ParameterMap();
        params.add("enabled", enabled.toString());
        params.add("target", target);
        params.add("dynamic", dynamic.toString());
        inv.parameters(params);
        inv.execute();
        // swallow the offline warning as it is not a problem
        if (subReport.hasWarnings()) {
            subReport.setMessage("");
        }
    }

    private void enableHealthCheckServiceConfigureOnTarget(ActionReport actionReport, AdminCommandContext context, Boolean enabled) {
        CommandRunner runner = serviceLocator.getService(CommandRunner.class);
        ActionReport subReport = context.getActionReport().addSubActionsReport();

        inv = runner.getCommandInvocation("healthcheck-configure-service", subReport, context.getSubject());

        ParameterMap params = new ParameterMap();
        params.add("enabled", enabled.toString());
        params.add("target", target);
        params.add("dynamic", dynamic.toString());
        params.add("time", time);
        params.add("unit", unit);
        params.add("serviceName", serviceName);
        params.add("name", name);

        inv.parameters(params);
        inv.execute();
        // swallow the offline warning as it is not a problem
        if (subReport.hasWarnings()) {
            subReport.setMessage("");
        }
    }

//    private void enableHealthCheckServiceThresholdConfigureOnTarget(ActionReport actionReport, AdminCommandContext context) {
//        CommandRunner runner = serviceLocator.getService(CommandRunner.class);
//        ActionReport subReport = context.getActionReport().addSubActionsReport();
//
//        inv = runner.getCommandInvocation("healthcheck-configure-service-threshold", subReport, context.getSubject());
//
//        ParameterMap params = new ParameterMap();
//        params.add("target", target);
//        params.add("dynamic", dynamic.toString());
//        params.add("serviceName", serviceName);
//        params.add("thresholdCritical", thresholdCritical);
//        params.add("thresholdWarning", thresholdWarning);
//        params.add("thresholdGood", thresholdGood);
//
//        inv.parameters(params);
//        inv.execute();
//        // swallow the offline warning as it is not a problem
//        if (subReport.hasWarnings()) {
//            subReport.setMessage("");
//        }
//    }
}
