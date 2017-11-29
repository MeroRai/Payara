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
 * @author Susan Rai
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

    private final String[] headers = {"Enabled", "FrequencyValue", "FrequencyUnit", "CloudEndpointUrl"};

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
            cloudTraceConfiguration.getFrequencyValue(),
            cloudTraceConfiguration.getFrequencyUnit(),
            cloudTraceConfiguration.getCloudEndpointUrl()};

        columnFormatter.addRow(values);

        Map<String, Object> map = new HashMap<String, Object>();
        Properties extraProps = new Properties();
        map.put("enabled", values[0]);
        map.put("frequencyValue", values[1]);
        map.put("frequencyUnit", values[2]);
        map.put("cloudEndpointUrl", values[3]);

        extraProps.put("getCloudTraceServiceConfiguration", map);

        actionReport.setExtraProperties(extraProps);

        actionReport.setMessage(columnFormatter.toString());
        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
