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

import org.jboss.wsf.spi.security.WildFlyClientConfigProvider;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.URISyntaxException;

import static org.jboss.wsf.stack.cxf.i18n.Messages.MESSAGES;

/**
 * Class for storing of authentication information obtained from Elytron client configuration
 *
 * @author dvilkola@redhat.com
 * @since 24-Jul-2019
 */
class ElytronClientConfig {

   private static String username;
   private static String password;
   private static SSLContext sslContext;
   private static HttpMechanismType httpMechanism;
   private static WsSecurityType wsSecurityType;

   enum HttpMechanismType {
      BASIC("BASIC");

      private String text;

      HttpMechanismType(String text) {
         this.text = text;
      }

      public String getText() {
         return this.text;
      }

      public static HttpMechanismType fromString(String text) {
         for (HttpMechanismType httpMechanismType : HttpMechanismType.values()) {
            if (httpMechanismType.text.equalsIgnoreCase(text)) {
               return httpMechanismType;
            }
         }
         return null;
      }
   }

   enum WsSecurityType {
      USERNAME_TOKEN("UsernameToken");

      private String text;

      WsSecurityType(String text) {
         this.text = text;
      }

      public String getText() {
         return this.text;
      }

      public static WsSecurityType fromString(String text) {
         for (WsSecurityType wsSecurityType : WsSecurityType.values()) {
            if (wsSecurityType.text.equalsIgnoreCase(text)) {
               return wsSecurityType;
            }
         }
         return null;
      }
   }

   static String getUsername() {
      return username;
   }

   static String getPassword() {
      return password;
   }

   static SSLContext getSslContext() {
      return sslContext;
   }

   static HttpMechanismType getHttpMechanism() {
      return httpMechanism;
   }

   static WsSecurityType getWsSecurityType() {
      return wsSecurityType;
   }

   static void setAuthConfiguration(WildFlyClientConfigProvider wildflyConfigProvider, String endpointAddress) {
      URI endpointURI = null;
      try {
         endpointURI = new URI(endpointAddress);
      } catch (URISyntaxException e) {
         throw MESSAGES.invalidEndpointURI(endpointAddress);
      }
      username = wildflyConfigProvider.getUsername(endpointURI);
      password = wildflyConfigProvider.getPassword(endpointURI);
      sslContext = wildflyConfigProvider.getSSLContext(endpointURI);
      httpMechanism = HttpMechanismType.fromString(wildflyConfigProvider.getHttpMechanism(endpointURI));
      wsSecurityType = WsSecurityType.fromString(wildflyConfigProvider.getWsSecurityType(endpointURI));
   }
}
