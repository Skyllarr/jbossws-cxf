/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.wsf.stack.cxf.client.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.jboss.ws.api.util.ServiceLoader;
import org.jboss.ws.common.configuration.ConfigHelper;
import org.jboss.wsf.spi.classloading.ClassLoaderProvider;
import org.jboss.wsf.spi.metadata.config.ClientConfig;
import org.jboss.wsf.spi.security.JASPIAuthenticationProvider;
import org.jboss.wsf.spi.security.WildFlyClientConfigProvider;
import org.jboss.wsf.stack.cxf.client.Constants;
import org.jboss.wsf.stack.cxf.i18n.Loggers;

/**
 * CXF extension of common ClientConfigurer
 * 
 * @author alessio.soldano@jboss.com
 * @since 25-Jul-2012
 *
 */
public class CXFClientConfigurer extends ConfigHelper
{
   private static final String JBOSSWS_CXF_CLIENT_CONF_PROPS = "jbossws.cxf.client.conf.props";
   
   @Override
   public void setConfigProperties(Object client, String configFile, String configName) {
      Class<?> clazz = !(client instanceof Dispatch) ? client.getClass() : null;
      ClientConfig config = readConfig(configFile, configName, clazz);
      setConfigProperties(client, config);
   }
   
   protected void setConfigProperties(Object client, ClientConfig config) {
      Client cxfClient;
      if (client instanceof DispatchImpl<?>) {
         cxfClient = ((DispatchImpl<?>)client).getClient();
      } else {
         cxfClient = ClientProxy.getClient(client);
      }
      cleanupPreviousProps(cxfClient);
      Map<String, String> props = new HashMap<>();
      if (config != null && config.getProperties() != null) {
         props.putAll(config.getProperties());
      }
      if (!props.isEmpty()) {
         savePropList(cxfClient, props);
      }
      setConfigProperties(cxfClient, props);
      
      //config jaspi
      JASPIAuthenticationProvider japsiProvider = (JASPIAuthenticationProvider) ServiceLoader.loadService(
            JASPIAuthenticationProvider.class.getName(), null, ClassLoaderProvider.getDefaultProvider().getServerIntegrationClassLoader());
      if (japsiProvider != null)
      {
         japsiProvider.enableClientAuthentication(cxfClient, props);
      }
      else
      {
         Loggers.SECURITY_LOGGER.cannotFindJaspiClasses();
      }
      
      //config elytron
      WildFlyClientConfigProvider elytronProvider = (WildFlyClientConfigProvider) ServiceLoader.loadService(
              WildFlyClientConfigProvider.class.getName(), null, ClassLoaderProvider.getDefaultProvider().getServerIntegrationClassLoader());
      if (elytronProvider != null)
      {
         String endpointAddress = ((BindingProvider) client).getRequestContext().get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY).toString();
         enableElytronClientAuthentication(elytronProvider, cxfClient, endpointAddress);
      }
   }
   
   public void setConfigProperties(Client client, Map<String, String> properties) {
      client.getEndpoint().putAll(properties);
      InterceptorUtils.addInterceptors(client, properties);
      FeatureUtils.addFeatures(client, client.getBus(), properties);
      PropertyReferenceUtils.createPropertyReference(properties, client.getBus().getProperties());
   }
   
   private void enableElytronClientAuthentication(WildFlyClientConfigProvider elytronProvider, Client cxfClient, String endpointAddress) {
      ElytronClientConfig.setAuthConfiguration(elytronProvider, endpointAddress);
      Map<String, Object> requestContext = cxfClient.getRequestContext();
      if (requestContext.get("com.sun.xml.ws.transport.https.client.SSLSocketFactory") == null)
      {
         setElytronConduitSelector(cxfClient); // sets BASIC in conduit
      }
      else
      {
         setHttpBasicProperties(cxfClient);
      }

      if (ElytronClientConfig.getUsername() != null && ElytronClientConfig.getWsSecurityType() == ElytronClientConfig.WsSecurityType.USERNAME_TOKEN)
      {
         setUsernameTokenProperties(cxfClient);
      }
   }
   
   private void setHttpBasicProperties(Client cxfClient) {
      if (ElytronClientConfig.getUsername() != null && ((ElytronClientConfig.getHttpMechanism() == null && ElytronClientConfig.getWsSecurityType() == null) ||
              ElytronClientConfig.getHttpMechanism() == ElytronClientConfig.HttpMechanismType.BASIC))
      {
         Map<String, Object> requestContext = cxfClient.getRequestContext();
         if (requestContext.get(BindingProvider.USERNAME_PROPERTY) == null && requestContext.get(BindingProvider.PASSWORD_PROPERTY) == null)
         {
            requestContext.put(BindingProvider.USERNAME_PROPERTY, ElytronClientConfig.getUsername());
            requestContext.put(BindingProvider.PASSWORD_PROPERTY, ElytronClientConfig.getPassword());
         }
      }
   }
   
   private void setUsernameTokenProperties(Client cxfClient) {
      Map<String, Object> requestContext = cxfClient.getRequestContext();
      if (requestContext.get(SecurityConstants.USERNAME) == null && requestContext.get(SecurityConstants.PASSWORD) == null &&
              requestContext.get(SecurityConstants.CALLBACK_HANDLER) == null) {
         requestContext.put(SecurityConstants.USERNAME, ElytronClientConfig.getUsername());
         requestContext.put(SecurityConstants.CALLBACK_HANDLER, (CallbackHandler) callbacks -> {
                    WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];
                    pc.setPassword(ElytronClientConfig.getPassword());
                 }
         );
      }
   }
   
   private void setElytronConduitSelector(Client cxfClient) {
      ConduitSelector elytronHttpConduitSelector = new ElytronConduitSelector();
      elytronHttpConduitSelector.setEndpoint(cxfClient.getEndpoint());
      cxfClient.setConduitSelector(elytronHttpConduitSelector);
   }
   
   private void savePropList(Client client, Map<String, String> props) {
      final Set<String> keys = props.keySet();
      client.getEndpoint().put(JBOSSWS_CXF_CLIENT_CONF_PROPS, (String[])keys.toArray(new String[keys.size()]));
   }
   
   private void cleanupPreviousProps(Client client) {
      Endpoint ep = client.getEndpoint();
      String[] previousProps = (String[])ep.get(JBOSSWS_CXF_CLIENT_CONF_PROPS);
      if (previousProps != null) {
         for (String p : previousProps) {
            if (Constants.CXF_IN_INTERCEPTORS_PROP.equals(p)) {
               InterceptorUtils.removeInterceptors(client.getInInterceptors(), (String)ep.get(p));
            } else if (Constants.CXF_OUT_INTERCEPTORS_PROP.equals(p)) {
               InterceptorUtils.removeInterceptors(client.getOutInterceptors(), (String)ep.get(p));
            } else if (Constants.CXF_IN_FAULT_INTERCEPTORS_PROP.equals(p)) {
               InterceptorUtils.removeInterceptors(client.getInFaultInterceptors(), (String)ep.get(p));
            } else if (Constants.CXF_OUT_FAULT_INTERCEPTORS_PROP.equals(p)) {
               InterceptorUtils.removeInterceptors(client.getOutFaultInterceptors(), (String)ep.get(p));
            } else if (Constants.CXF_FEATURES_PROP.equals(p)) {
               Loggers.ROOT_LOGGER.couldNoRemoveFeaturesOnClient((String)ep.get(p));
            }
            ep.remove(p);
         }
         ep.remove(JBOSSWS_CXF_CLIENT_CONF_PROPS);
      }
   }
}
