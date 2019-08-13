/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat Middleware LLC, and individual contributors
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

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.UpfrontConduitSelector;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.http.HTTPConduit;

import javax.xml.ws.BindingProvider;

/**
 * Extension of UpfrontConduitSelector that uses SSLContext specified in Elytron client configuration
 *
 * @author dvilkola@redhat.com
 * @since 24-Jul-2019
 */
public class ElytronConduitSelector extends UpfrontConduitSelector {

   /**
    * Called when a Conduit is actually required.
    *
    * @param message
    * @return the Conduit to use for mediation of the message
    */
   @Override
   public Conduit selectConduit(Message message) {
      Conduit c = super.selectConduit(message);
      if (c instanceof HTTPConduit)
      {
         setAuthorizationPolicyCredentials(message, (HTTPConduit) c);
         if (((HTTPConduit) c).getTlsClientParameters() == null || ((HTTPConduit) c).getTlsClientParameters().getSslContext() == null)
         {
            TLSClientParameters params = ((HTTPConduit) c).getTlsClientParameters() == null ? new TLSClientParameters() : ((HTTPConduit) c).getTlsClientParameters();
            params.setSslContext(ElytronClientConfig.getSslContext());
            params.setUseHttpsURLConnectionDefaultSslSocketFactory(false);
            ((HTTPConduit) c).setTlsClientParameters(params);
         }
      }
      return c;
   }

   private void setAuthorizationPolicyCredentials(Message message, HTTPConduit c) {
      if (ElytronClientConfig.getUsername() != null && ((ElytronClientConfig.getHttpMechanism() == null && ElytronClientConfig.getWsSecurityType() == null) ||
              ElytronClientConfig.getHttpMechanism() == ElytronClientConfig.HttpMechanismType.BASIC))
      {
         String defaultUsername = (String) message.get(BindingProvider.USERNAME_PROPERTY);
         String defaultPassword = (String) message.get(BindingProvider.PASSWORD_PROPERTY);
         AuthorizationPolicy authorizationPolicy = new AuthorizationPolicy();
         authorizationPolicy.setUserName(defaultUsername == null ? ElytronClientConfig.getUsername() : defaultUsername);
         authorizationPolicy.setPassword(defaultPassword == null ? ElytronClientConfig.getPassword() : defaultPassword);
         c.setAuthorization(authorizationPolicy);
      }
   }
}
