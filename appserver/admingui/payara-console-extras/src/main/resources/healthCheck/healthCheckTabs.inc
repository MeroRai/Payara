
<sun:tabSet id="healthCheckTabs" immediate="true" selected="#{sessionScope.healthCheckTabs}">
    <sun:tab id="healthCheckConfigure" immediate="true" text="$resource{i18n.edJmxConnector.jmxConnectorLabel}" toolTip="$resource{i18n.edJmxConnector.jmxConnectorLabelTab}" >
        <!command
            setSessionAttribute(key="healthCheckTabs" value="healthCheckConfigure");
            gf.redirect(page="#{request.contextPath}/payaraExtras/healthCheck/healthCheck.jsf?configName=#{pageSession.configName}");
            /> 
    </sun:tab>

    <sun:tab id="healthCheckThresholdConfigure" immediate="true" text="$resource{i18n.edJmxConnector.sslSettingsLabel}" toolTip="$resource{i18n.edJmxConnector.sslSettingsLabelTab}" >
        <!command
            setSessionAttribute(key="healthCheckTabs" value="healthCheckThresholdConfigure");
            gf.redirect(page="#{request.contextPath}/payaraExtras/healthCheck/healthCheckThreshold.jsf?configName=#{pageSession.configName}");
            /> 
    </sun:tab>
</sun:tabSet>