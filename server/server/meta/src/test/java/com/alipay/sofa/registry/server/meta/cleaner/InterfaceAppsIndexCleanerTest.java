/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.registry.server.meta.cleaner;

import static org.mockito.Mockito.*;

import com.alipay.sofa.registry.jdbc.config.DefaultCommonConfig;
import com.alipay.sofa.registry.jdbc.config.MetadataConfig;
import com.alipay.sofa.registry.jdbc.domain.AppRevisionDomain;
import com.alipay.sofa.registry.jdbc.domain.InterfaceAppsIndexDomain;
import com.alipay.sofa.registry.jdbc.mapper.AppRevisionMapper;
import com.alipay.sofa.registry.jdbc.mapper.InterfaceAppsIndexMapper;
import com.alipay.sofa.registry.server.meta.AbstractMetaServerTestBase;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;

public class InterfaceAppsIndexCleanerTest extends AbstractMetaServerTestBase {
  private InterfaceAppsIndexCleaner interfaceAppsIndexCleaner;

  @Before
  public void beforeTest() throws Exception {
    makeMetaLeader();
    interfaceAppsIndexCleaner = new InterfaceAppsIndexCleaner(metaLeaderService);
    interfaceAppsIndexCleaner.appRevisionMapper = mock(AppRevisionMapper.class);
    interfaceAppsIndexCleaner.interfaceAppsIndexMapper = mock(InterfaceAppsIndexMapper.class);
    interfaceAppsIndexCleaner.defaultCommonConfig = mock(DefaultCommonConfig.class);
    interfaceAppsIndexCleaner.metadataConfig = mock(MetadataConfig.class);
    when(interfaceAppsIndexCleaner.metadataConfig.getInterfaceAppsIndexRenewIntervalMinutes())
        .thenReturn(10000);
    when(interfaceAppsIndexCleaner.defaultCommonConfig.getClusterId())
        .thenReturn("DEFAULT_LOCALDATACENTER");
  }

  @Test
  public void testRenew() {
    InterfaceAppsIndexCleaner mocked = spy(interfaceAppsIndexCleaner);
    AppRevisionDomain domain1 = new AppRevisionDomain();
    domain1.setAppName("app1");
    domain1.setBaseParams("{}");
    domain1.setServiceParams("{\"service1\":{}}");
    AppRevisionDomain domain2 = new AppRevisionDomain();
    domain2.setBaseParams("{}");
    domain2.setAppName("app2");
    domain2.setServiceParams("{\"service1\":{}, \"service2\": {}}");
    doReturn(Lists.newArrayList(domain1, domain2))
        .when(mocked.appRevisionMapper)
        .listRevisions(anyString(), anyInt(), anyInt());
    mocked.renew();
    verify(mocked.interfaceAppsIndexMapper, times(3)).update(any());
    verify(mocked.interfaceAppsIndexMapper, times(3)).replace(any());
    mocked.renewer.getWaitingMillis();
    mocked.renewer.runUnthrowable();
    mocked.init();
    mocked.renewer.close();
    mocked.cleaner.close();
  }

  @Test
  public void testClean() {
    InterfaceAppsIndexCleaner mocked = spy(interfaceAppsIndexCleaner);
    InterfaceAppsIndexDomain domain1 = mock(InterfaceAppsIndexDomain.class);
    InterfaceAppsIndexDomain domain2 = mock(InterfaceAppsIndexDomain.class);
    doReturn(Lists.newArrayList(domain1, domain2))
        .when(mocked.interfaceAppsIndexMapper)
        .getExpired(anyString(), any(), anyInt());
    mocked.markDeleted();
    verify(domain1, times(1)).setReference(false);
    verify(domain2, times(1)).setReference(false);
    verify(mocked.interfaceAppsIndexMapper, times(2)).replace(any());
    mocked.cleaner.getWaitingMillis();
    mocked.cleaner.runUnthrowable();
  }
}
