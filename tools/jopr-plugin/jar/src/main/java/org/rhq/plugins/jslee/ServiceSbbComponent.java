package org.rhq.plugins.jslee;

import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.security.auth.login.LoginException;
import javax.slee.SbbID;
import javax.slee.ServiceID;
import javax.slee.management.DeploymentMBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jslee.utils.MBeanServerUtils;

public class ServiceSbbComponent implements ResourceComponent<ServiceComponent>, MeasurementFacet, OperationFacet {
  private final Log log = LogFactory.getLog(ServiceSbbComponent.class);

  private ResourceContext<ServiceComponent> resourceContext;
  private SbbID sbbId = null;
  private ServiceID serviceId = null;
  private MBeanServerUtils mbeanUtils = null;

  private boolean isUp = false;
  private ObjectName deploymentMBeanObj;

  public void start(ResourceContext<ServiceComponent> context) throws InvalidPluginConfigurationException, Exception {
    if(log.isTraceEnabled()) {
      log.trace("start(" + context + ") called.");
    }

    this.resourceContext = context;
    this.deploymentMBeanObj = new ObjectName(DeploymentMBean.OBJECT_NAME);

    this.mbeanUtils = context.getParentResourceComponent().getMBeanServerUtils();
    this.serviceId = context.getParentResourceComponent().getServiceID();

    String name = this.resourceContext.getPluginConfiguration().getSimple("name").getStringValue();
    String version = this.resourceContext.getPluginConfiguration().getSimple("version").getStringValue();
    String vendor = this.resourceContext.getPluginConfiguration().getSimple("vendor").getStringValue();

    sbbId = new SbbID(name, vendor, version);
  }

  public void stop() {
    if(log.isTraceEnabled()) {
      log.trace("stop() called.");
    }
  }

  public AvailabilityType getAvailability() {
    if(log.isTraceEnabled()) {
      log.trace("getAvailability() called.");
    }

    this.isUp = false;

    try {
      MBeanServerConnection connection = this.mbeanUtils.getConnection();
      this.mbeanUtils.login();

      DeploymentMBean depMBean = (DeploymentMBean) MBeanServerInvocationHandler.newProxyInstance(connection, deploymentMBeanObj, DeploymentMBean.class, false);

      for(SbbID activeSbbId : depMBean.getSbbs()) {
        if(activeSbbId.equals(sbbId)) {
          this.isUp = true;
        }
      }
    }
    catch (Exception e) {
      log.error("getAvailability failed for SbbID = " + this.sbbId);
    }
    finally {
      try {
        this.mbeanUtils.logout();
      }
      catch (LoginException e) {
        if(log.isDebugEnabled()) {
          log.debug("Failed to logout from secured JMX", e);
        }
      }
    }

    return this.isUp ? AvailabilityType.UP : AvailabilityType.DOWN;
  }

  public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
    if(log.isTraceEnabled()) {
      log.trace("getValues(" + report + "," + metrics + ") called.");
    }

    for (MeasurementScheduleRequest request : metrics) {
      if (request.getName().equals("state")) {
        report.addData(new MeasurementDataTrait(request, this.isUp ? "UP" : "DOWN"));
      }
    }
  }

  public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException, Exception {
    if(log.isDebugEnabled()) {
      log.debug("invokeOperation(" + name + ", " + parameters + ") called.");
    }

    throw new UnsupportedOperationException("Operation [" + name + "] is not supported.");
  }

  public SbbID getSbbID() {
    return this.sbbId;
  }

  public ServiceID getServiceID() {
    return this.serviceId;
  }

  public MBeanServerUtils getMBeanServerUtils() {
    return this.mbeanUtils;
  }

}
