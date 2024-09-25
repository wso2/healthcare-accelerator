package org.wso2.healthcare.consentmgt.endpoint.factories;

import org.wso2.healthcare.consentmgt.endpoint.ConsentsApiService;
import org.wso2.healthcare.consentmgt.endpoint.impl.ConsentsApiServiceImpl;

public class ConsentsApiServiceFactory {

   private final static ConsentsApiService service = new ConsentsApiServiceImpl();

   public static ConsentsApiService getConsentsApi()
   {
      return service;
   }
}
