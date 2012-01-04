/*
 * JBoss, Home of Professional Open Source
 * Copyright 2003-2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.slee.container.management.console.server.mbeans;

import java.util.ArrayList;

import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.slee.usage.SampleStatistics;

import org.mobicents.slee.container.management.console.client.ManagementConsoleException;
import org.mobicents.slee.container.management.console.client.usage.CounterTypeSBBUsageParameterInfo;
import org.mobicents.slee.container.management.console.client.usage.SBBUsageParameterInfo;
import org.mobicents.slee.container.management.console.client.usage.SampleTypeSBBUsageParameterInfo;

/**
 * @author Povilas Jurna
 * 
 */
public class RaEntityUsageMBeanUtils {
  private MBeanServerConnection mbeanServer;

  private ObjectName raEntityUsageMBean;

  public RaEntityUsageMBeanUtils(MBeanServerConnection mbeanServer, ObjectName raEntityUsageMBean) {
    super();
    this.mbeanServer = mbeanServer;
    this.raEntityUsageMBean = raEntityUsageMBean;
  }

  public void close() throws ManagementConsoleException {
    try {
      mbeanServer.invoke(raEntityUsageMBean, "close", new Object[] {}, new String[] {});
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new ManagementConsoleException(SleeManagementMBeanUtils.doMessage(e));
    }
  }

  public void resetAllUsageParameters() throws ManagementConsoleException {
    try {
      mbeanServer.invoke(raEntityUsageMBean, "resetAllUsageParameters", new Object[] {}, new String[] {});
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new ManagementConsoleException(SleeManagementMBeanUtils.doMessage(e));
    }
  }

  public Long getCounterTypeParameter(String name, boolean reset) throws ManagementConsoleException {
    try {
      return (Long) mbeanServer.invoke(raEntityUsageMBean, "get" + name, new Object[] { new Boolean(reset) }, new String[] { "boolean" });
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new ManagementConsoleException(SleeManagementMBeanUtils.doMessage(e));
    }
  }

  public SampleStatistics getSampleTypeParameter(String name, boolean reset) throws ManagementConsoleException {
    try {
      return (SampleStatistics) mbeanServer.invoke(raEntityUsageMBean, "get" + name, new Object[] { new Boolean(reset) }, new String[] { "boolean" });
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new ManagementConsoleException(SleeManagementMBeanUtils.doMessage(e));
    }
  }

  private SBBUsageParameterInfo toSBBUsageParameterInfo(MBeanOperationInfo beanOperationInfo) throws ManagementConsoleException {
    ManagementConsoleException exception = new ManagementConsoleException("SBB Usage MBean method " + beanOperationInfo.getName()
        + " does not correspond to a SBB Usage Parameter");

    // operation name must start with "get"
    String parameterName = beanOperationInfo.getName();
    if (!parameterName.startsWith("get")) {
      throw exception;
    }

    parameterName = parameterName.substring("get".length());

    // operation must have one single boolean type argument
    MBeanParameterInfo[] beanParameterInfos = beanOperationInfo.getSignature();
    if (beanParameterInfos.length != 1)
      throw exception;

    if (!beanParameterInfos[0].getType().equals(boolean.class.getName())) {
      throw exception;
    }

    // operation return type must be long or SampleStatistics
    String returnType = beanOperationInfo.getReturnType();
    if (returnType.equals(long.class.getName())) {
      // counter type usage parameter
      long parameterValue = getCounterTypeParameter(parameterName, false).longValue();

      CounterTypeSBBUsageParameterInfo parameterInfo = new CounterTypeSBBUsageParameterInfo(parameterName, parameterValue);
      return parameterInfo;

    }
    else if (returnType.equals(SampleStatistics.class.getName())) {
      // sample type usage parameter
      SampleStatistics parameterValue = getSampleTypeParameter(parameterName, false);

      SampleTypeSBBUsageParameterInfo parameterInfo = new SampleTypeSBBUsageParameterInfo(parameterName, parameterValue.getMaximum(),
          parameterValue.getMinimum(), parameterValue.getMean(), parameterValue.getSampleCount());

      return parameterInfo;
    }
    else {
      throw exception;
    }
  }

  public SBBUsageParameterInfo[] getSBBUsageParameterInfos() throws ManagementConsoleException {
    try {
      ArrayList<SBBUsageParameterInfo> SBBUsageParameterInfoArrayList = new ArrayList<SBBUsageParameterInfo>();
      MBeanOperationInfo[] beanOperationInfos = mbeanServer.getMBeanInfo(raEntityUsageMBean).getOperations();
      for (int i = 0; i < beanOperationInfos.length; i++) {
        MBeanOperationInfo beanOperationInfo = beanOperationInfos[i];
        try {
          SBBUsageParameterInfoArrayList.add(toSBBUsageParameterInfo(beanOperationInfo));
        }
        catch (Exception e) {
        }
      }
      SBBUsageParameterInfo[] SBBUsageParameterInfos = new SBBUsageParameterInfo[SBBUsageParameterInfoArrayList.size()];
      SBBUsageParameterInfos = SBBUsageParameterInfoArrayList.toArray(SBBUsageParameterInfos);
      return SBBUsageParameterInfos;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new ManagementConsoleException(SleeManagementMBeanUtils.doMessage(e));
    }
  }
}
