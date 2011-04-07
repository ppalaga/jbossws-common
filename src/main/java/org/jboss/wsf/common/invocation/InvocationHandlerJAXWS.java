/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.wsf.common.invocation;

import javax.xml.ws.WebServiceContext;

import org.jboss.wsf.common.injection.InjectionHelper;
import org.jboss.wsf.common.injection.PreDestroyHolder;
import org.jboss.wsf.common.injection.ThreadLocalAwareWebServiceContext;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.invocation.Invocation;
import org.jboss.wsf.spi.invocation.InvocationContext;
import org.jboss.wsf.spi.metadata.injection.InjectionsMetaData;

/**
 * Handles invocations on JAXWS endpoints.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
public final class InvocationHandlerJAXWS extends AbstractInvocationHandlerJSE
{

   /**
    * Constructor.
    */
   public InvocationHandlerJAXWS()
   {
      super();
   }

   /**
    * Injects resources on target bean and calls post construct method.
    * Finally it registers target bean for predestroy phase.
    *
    * @param endpoint used for predestroy phase registration process
    * @param invocation current invocation
    */
   @Override
   public void onEndpointInstantiated(final Endpoint endpoint, final Invocation invocation)
   {
      final InjectionsMetaData injectionsMD = endpoint.getAttachment(InjectionsMetaData.class);
      final Object targetBean = this.getTargetBean(invocation);

      this.log.debug("Injecting resources on JAXWS JSE endpoint: " + targetBean);
      if (injectionsMD != null)
         InjectionHelper.injectResources(targetBean, injectionsMD, endpoint.getJNDIContext());

      InjectionHelper.injectWebServiceContext(targetBean, ThreadLocalAwareWebServiceContext.getInstance());

      this.log.debug("Calling postConstruct method on JAXWS JSE endpoint: " + targetBean);
      InjectionHelper.callPostConstructMethod(targetBean);

      endpoint.addAttachment(PreDestroyHolder.class, new PreDestroyHolder(targetBean));
   }

   /**
    * Injects webservice context on target bean.
    *
    *  @param invocation current invocation
    */
   @Override
   public void onBeforeInvocation(final Invocation invocation)
   {
      final WebServiceContext wsContext = this.getWebServiceContext(invocation);
      ThreadLocalAwareWebServiceContext.getInstance().setMessageContext(wsContext);
   }

   /**
    * Cleanups injected webservice context on target bean.
    *
    * @param invocation current invocation
    */
   @Override
   public void onAfterInvocation(final Invocation invocation)
   {
      ThreadLocalAwareWebServiceContext.getInstance().setMessageContext(null);
   }

   /**
    * Returns WebServiceContext associated with this invocation.
    *
    * @param invocation current invocation
    * @return web service context or null if not available
    */
   private WebServiceContext getWebServiceContext(final Invocation invocation)
   {
      final InvocationContext invocationContext = invocation.getInvocationContext();

      return invocationContext.getAttachment(WebServiceContext.class);
   }

   /**
    * Returns endpoint instance associated with current invocation.
    *
    * @param invocation current invocation
    * @return target bean in invocation
    */
   private Object getTargetBean(final Invocation invocation)
   {
      final InvocationContext invocationContext = invocation.getInvocationContext();

      return invocationContext.getTargetBean();
   }

}
