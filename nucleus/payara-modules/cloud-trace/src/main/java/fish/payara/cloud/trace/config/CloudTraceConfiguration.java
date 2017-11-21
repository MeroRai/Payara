/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.cloud.trace.config;

import javax.validation.constraints.Pattern;
import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;

/**
 *
 * @author Susan Rai
 */
@Configured
public interface CloudTraceConfiguration extends ConfigBeanProxy, ConfigExtension {

    @Attribute(defaultValue = "false", dataType = Boolean.class)
    public String getEnabled();
    public void setEnabled(Boolean enabled);

    @Attribute(defaultValue = "HOURS")
    @Pattern(regexp = "SECONDS|MINUTES|HOURS|DAYS", message = "Invalid time unit. Value must be one of: SECONDS, MINUTES, HOURS, DAYS.")
    String getFrequencyUnit();
    public void setFrequencyUnit(String value);

    @Attribute(defaultValue = "")
    String getTraces();
    public void setTraces(String traces);
    
    @Attribute(defaultValue = "http://httpbin.org/post")
    String getURL();
    public void setURL(String traces);
}
