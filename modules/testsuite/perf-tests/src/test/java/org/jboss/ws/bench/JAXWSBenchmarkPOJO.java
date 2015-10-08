package org.jboss.ws.bench;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.jboss.test.ws.jaxws.benchmark.test.datatypes.EndpointDoc;
import org.jboss.wsf.test.JBossWSTestHelper;

public class JAXWSBenchmarkPOJO extends AbstractJavaSamplerClient
{
   private final String endpointURL = "http://" + JBossWSTestHelper.getServerHost() + ":" + JBossWSTestHelper.getServerPort() + "/jaxws-benchmark-datatypes-pojo/EndpointPOJODocImpl";
   private final String targetNS = "http://datatypes.test.benchmark.jaxws.ws.test.jboss.org/";
   private EndpointDoc ep;
   
   @Override
   public void setupTest(JavaSamplerContext context) {
      super.setupTest(context);
      try {
         this.ep = prepare();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }
   
   @Override
   public void teardownTest(JavaSamplerContext context) {
      super.teardownTest(context);
      this.ep = null;
   }
   
   @Override
   public SampleResult runTest(JavaSamplerContext ctx)
   {
      final SampleResult sampleResult = new SampleResult();
      sampleResult.sampleStart();

      try {
         performIteration(ep);
         sampleResult.setSuccessful(true);
      } catch (Exception e) {
         sampleResult.setSuccessful(false);
         sampleResult.setResponseMessage("Exception: " + e);
         StringWriter stringWriter = new StringWriter();
         e.printStackTrace(new PrintWriter(stringWriter));
         sampleResult.setResponseData(stringWriter.toString(), "UTF-8");
         sampleResult.setDataType(SampleResult.TEXT);
      } finally {
         sampleResult.sampleEnd();
      }

      return sampleResult;
   }

   public EndpointDoc prepare() throws Exception
   {
      URL wsdlURL = new URL(endpointURL + "?wsdl");
      QName serviceName = new QName(targetNS, "EndpointDocService");

      Service service = Service.create(wsdlURL, serviceName);
      return service.getPort(EndpointDoc.class);
   }

   public void performIteration(Object port) throws Exception
   {
      for (int i = 0; i < 100; i++) {
         double par = Math.random();
         double ret = ((EndpointDoc)port).increaseDouble(par);
         if (!((par + 1) == (ret)))
         {
            throw new Exception("Unexpected result: " + ret);
         }
      }
   }
}
