package org.jboss.test.ws.jaxws.samples.wsse.policy.basic;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
//import org.jboss.shrinkwrap.resolver.api.maven.Maven;
//import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.jboss.wsf.test.JBossWSTest;
import org.jboss.wsf.test.JBossWSTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.io.File;
import java.net.URL;


@RunWith(Arquillian.class)
public class UsernameElytronClientConfigTestCase extends JBossWSTest {

   private static final String DEPLOYMENT = "jaxws-client-deployment";
   private static final String JAXWS_CLIENT_SERVER = "jaxws-client";

   @ArquillianResource
   private URL baseURL;

   @ArquillianResource
   private Deployer deployer;

   @ArquillianResource
   private ContainerController containerController;

   @Before
   public void startContainerAndDeploy() throws Exception {
      if (!containerController.isStarted(JAXWS_CLIENT_SERVER)) {
         containerController.start(JAXWS_CLIENT_SERVER);
         deployer.deploy(DEPLOYMENT);
      }
   }

   @Deployment(name=DEPLOYMENT, testable = false)
   @TargetsContainer(JAXWS_CLIENT_SERVER)
   public static WebArchive createClientDeployment() {
//      PomEquippedResolveStage pomFile = Maven.resolver().loadPomFromFile("pom.xml");

      WebArchive archive = ShrinkWrap.create(WebArchive.class, "somethingsometnig" + ".war");
      archive.setManifest(new StringAsset("Manifest-Version: 1.0\n"
              + "Dependencies: org.apache.cxf.impl,org.jboss.as.server \n"))
              .addClass(org.jboss.test.ws.jaxws.cxf.clientConfig.Endpoint.class)
              .addClass(org.jboss.test.ws.jaxws.cxf.clientConfig.Helper.class)
              .addClass(org.jboss.test.ws.jaxws.cxf.clientConfig.TestUtils.class)
              .addClass(org.jboss.wsf.test.ClientHelper.class)
              .addClass(org.jboss.wsf.test.TestServlet.class)
//              .addAsLibraries(pomFile.resolve("org.wildfly.security:wildfly-elytron-client").withTransitivity().asFile())
              .addAsManifestResource(new File(JBossWSTestHelper.getTestResourcesDir() + "/jaxws/cxf/clientConfig/META-INF/default-client-permissions.xml"), "permissions.xml");
      return archive;
   }

   @Deployment(name="jaxws-server", testable = true)
   public static WebArchive createDeployment() {
      WebArchive archive = ShrinkWrap.create(WebArchive.class, "jaxws-samples-wsse-policy-username-unsecure-transport.war");
      archive.setManifest(new StringAsset("Manifest-Version: 1.0\n"
              + "Dependencies: org.jboss.ws.cxf.jbossws-cxf-client\n"))
              .addClass(org.jboss.test.ws.jaxws.samples.wsse.policy.basic.JavaFirstServiceIface.class)
              .addClass(org.jboss.test.ws.jaxws.samples.wsse.policy.basic.JavaFirstServiceImpl.class)
              .addClass(org.jboss.test.ws.jaxws.samples.wsse.policy.basic.ServerUsernamePasswordCallback.class)
              .addClass(org.jboss.test.ws.jaxws.samples.wsse.policy.basic.ServiceIface.class)
              .addClass(org.jboss.test.ws.jaxws.samples.wsse.policy.basic.ServiceImpl.class)
              .addClass(org.jboss.test.ws.jaxws.samples.wsse.policy.jaxws.SayHello.class)
              .addClass(org.jboss.test.ws.jaxws.samples.wsse.policy.jaxws.SayHelloResponse.class)
              .addAsWebInfResource(new File(JBossWSTestHelper.getTestResourcesDir() + "/jaxws/samples/wsse/policy/basic/username-unsecure-transport/JavaFirstPolicy.xml"), "classes/JavaFirstPolicy.xml")
              .addAsWebInfResource(new File(JBossWSTestHelper.getTestResourcesDir() + "/jaxws/samples/wsse/policy/basic/username-unsecure-transport/WEB-INF/jaxws-endpoint-config.xml"), "jaxws-endpoint-config.xml")
              .addAsWebInfResource(new File(JBossWSTestHelper.getTestResourcesDir() + "/jaxws/samples/wsse/policy/basic/username-unsecure-transport/WEB-INF/wsdl/SecurityService.wsdl"), "wsdl/SecurityService.wsdl")
              .addAsWebInfResource(new File(JBossWSTestHelper.getTestResourcesDir() + "/jaxws/samples/wsse/policy/basic/username-unsecure-transport/WEB-INF/wsdl/SecurityService_schema1.xsd"), "wsdl/SecurityService_schema1.xsd")
              .setWebXML(new File(JBossWSTestHelper.getTestResourcesDir() + "/jaxws/samples/wsse/policy/basic/username-unsecure-transport/WEB-INF/web.xml"));
      return archive;
   }




   @Test
   @RunAsClient
   @OperateOnDeployment(DEPLOYMENT)
   public void test() throws Exception
   {
      System.out.println(System.getProperty("wildfly.config.url"));
      QName serviceName = new QName("http://www.jboss.org/jbossws/ws-extensions/wssecuritypolicy", "SecurityService");
      URL wsdlURL = new URL("http://127.0.0.1:8080/jaxws-samples-wsse-policy-username-unsecure-transport" + "/service?wsdl");
      Service service = Service.create(wsdlURL, serviceName);
      ServiceIface proxy = (ServiceIface)service.getPort(ServiceIface.class);
      assertEquals("Secure Hello World!", proxy.sayHello());
   }
}
