/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.jms.admin.cli;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.connectors.config.AdminObjectResource;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.resourcebase.resources.ResourceTypeOrder;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.types.Property;

/**
 *
 * @author Susan Rai
 */
@Service(name = "edit-jms-resource")
@PerLookup
@I18n("edit.jms.resource")
@ExecuteOn({RuntimeType.DAS})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.DOMAIN})
@RestEndpoints({
    @RestEndpoint(configBean = Resources.class,
            opType = RestEndpoint.OpType.POST,
            path = "edit-jms-resource",
            description = "edit-jms-resource")
})
public class EditJMSResource implements AdminCommand {

    @Inject
    CommandRunner commandRunner;

    @Inject
    Domain domain;
    @Inject
    AdminObjectResource adminObjectResource;

    private static final String QUEUE = "javax.jms.Queue";
    private static final String TOPIC = "javax.jms.Topic";

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        List<Property> p = adminObjectResource.getProperty();
        int count = p.size();
        System.out.println("Count: " + count);

        // Loop through elements.
        for (int i = 0; i < p.size(); i++) {
            Property value = p.get(i);
            System.out.println("Element: " + value);
        }
        for (Property model : p) {
            System.out.println("yaaaaaaaaaaaaaaaaaaa = " + model.getName());
        }

        List<Property> property = adminObjectResource.getProperty();
        String resourceType = adminObjectResource.getResType();
        String description = adminObjectResource.getDescription();
        String enabled = adminObjectResource.getEnabled();
        String indentiy = adminObjectResource.getJndiName();

        if (resourceType.equals(TOPIC)
                || resourceType.equals(QUEUE)) {
            System.out.println(p.toString());
            System.out.println("usadjklasjdks = " + Arrays.toString(p.toArray()));
            System.out.println("CLASSNAME  = " + adminObjectResource.getClassName());
            System.out.println("Get PRoperty = " + adminObjectResource.getProperty());
            System.out.println("Get Property to string = " + adminObjectResource.getProperty().toString());
            System.out.println("get Description = " + adminObjectResource.getDescription());
            System.out.println("get indentiy = " + adminObjectResource.getIdentity());
            System.out.println("get Res adatat = " + adminObjectResource.getResAdapter());
            System.out.println("get res type = " + adminObjectResource.getResType());
            System.out.println("get ebaled = " + adminObjectResource.getEnabled());
            System.out.println("Jindi name = " + adminObjectResource.getJndiName());
            System.out.println("PROPER anme = " + adminObjectResource.getPropertyValue("Susan"));
            System.out.println("object = " + adminObjectResource.getObjectType());
            System.out.println("Deployment order = " + adminObjectResource.getDeploymentOrder());
            System.out.println("PROPER anme 2 = " + adminObjectResource.getPropertyValue("KajiDestination"));
            System.out.println("toString = " + adminObjectResource.toString());
            System.out.println("");
        }
    }
}
