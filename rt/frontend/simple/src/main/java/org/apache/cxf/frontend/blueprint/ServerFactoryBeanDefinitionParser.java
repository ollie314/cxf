/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.frontend.blueprint;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.blueprint.SimpleBPBeanDefinitionParser;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean;
import org.osgi.service.blueprint.reflect.Metadata;



public class ServerFactoryBeanDefinitionParser extends SimpleBPBeanDefinitionParser {
    

    public ServerFactoryBeanDefinitionParser() {
        this(BPServerFactoryBean.class);
    }
    public ServerFactoryBeanDefinitionParser(Class<?> cls) {
        super(cls);
    }
    
    @Override
    protected void mapAttribute(MutableBeanMetadata bean, 
                                Element e, String name, 
                                String val, ParserContext context) {
        if ("endpointName".equals(name) || "serviceName".equals(name)) {
            QName q = parseQName(e, val);
            bean.addProperty(name, createValue(context, q));
        } else {
            mapToProperty(bean, name, val, context);
        }
    }

    @Override
    protected void mapElement(ParserContext ctx, MutableBeanMetadata bean, Element el, String name) {
        if ("properties".equals(name)) {
            bean.addProperty("properties", this.parseMapData(ctx, bean, el));
        } else if ("executor".equals(name)) {
            setFirstChildAsProperty(el, ctx, bean, "serviceFactory.executor");
        } else if ("invoker".equals(name)) {
            setFirstChildAsProperty(el, ctx, bean, "serviceFactory.invoker");
        } else if ("binding".equals(name)) {
            setFirstChildAsProperty(el, ctx, bean, "bindingConfig");
        } else if ("inInterceptors".equals(name) || "inFaultInterceptors".equals(name)
            || "outInterceptors".equals(name) || "outFaultInterceptors".equals(name)
            || "features".equals(name) || "schemaLocations".equals(name)) {
            bean.addProperty(name, this.parseListData(ctx, bean, el));
        } else {
            setFirstChildAsProperty(el, ctx, bean, name);            
        }        
    }
    

    @Override
    public Metadata parse(Element element, ParserContext context) {
        MutableBeanMetadata bean = (MutableBeanMetadata)super.parse(element, context);
        bean.setInitMethod("init");
        bean.setDestroyMethod("destroy");

        // We don't really want to delay the registration of our Server
        bean.setActivation(MutableBeanMetadata.ACTIVATION_EAGER);
        return bean;
    }

    @Override
    public String getId(Element elem, ParserContext context) {
        String id = super.getId(elem, context);
        if (StringUtils.isEmpty(id)) {
            id = cls.getName() + "--" + context.getDefaultActivation();
        }
        return id;
    }

    @Override
    protected boolean hasBusProperty() {
        return true;
    }
    
    
    @NoJSR250Annotations
    public static class BPServerFactoryBean extends ServerFactoryBean {

        private Server server;

        public BPServerFactoryBean() {
            super();
        }
        public BPServerFactoryBean(ReflectionServiceFactoryBean fact) {
            super(fact);
        }
        public Server getServer() {
            return server;
        }
        
        public void init() {
            create();
        }
        @Override
        public Server create() {
            if (server == null) {
                server = super.create();
            }
            return server;
        }
        public void destroy() {
            if (server != null) {
                server.destroy();
                server = null;
            }
        }
    }
    
}
