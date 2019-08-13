/*
 *  Copyright 2017 SmartBear Software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.swagger.oas.inflector.controllers;

import io.swagger.oas.inflector.config.FilterFactory;
import io.swagger.oas.inflector.config.OpenAPIProcessor;
import io.swagger.v3.core.filter.OpenAPISpecFilter;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import org.glassfish.jersey.process.Inflector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class OpenAPIResourceController implements Inflector<ContainerRequestContext, Response> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAPIResourceController.class);

    private OpenAPI openAPI;
    private List<OpenAPIProcessor> openAPIProcessors;

    public OpenAPIResourceController(OpenAPI openAPI, List<String> swaggerProcessors) {

        this.openAPI = openAPI;

        this.openAPIProcessors = new ArrayList<>(swaggerProcessors.size());
        for (String swaggerProcessorClass : swaggerProcessors) {
            try {
                this.openAPIProcessors.add(((OpenAPIProcessor) OpenAPIResourceController.class.getClassLoader()
                        .loadClass(swaggerProcessorClass).newInstance()));
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
                LOGGER.error("Unable to load class: " + swaggerProcessorClass, e);
            }
        }
    }

    @Override
    public Response apply(ContainerRequestContext arg0) {
        OpenAPISpecFilter filter = FilterFactory.getFilter();
        if(filter != null) {
            Map<String, Cookie> cookiesvalue = arg0.getCookies();
            Map<String, String> cookies = new HashMap<>();
            if(cookiesvalue != null) {
                for(String key: cookiesvalue.keySet()) {
                    cookies.put(key, cookiesvalue.get(key).getValue());
                }
            }

            MultivaluedMap<String, String> headers = arg0.getHeaders();
            // since https://github.com/swagger-api/swagger-inflector/issues/305 filtering of inflector extensions is handled at init time by ExtensionsUtils, and VendorSpecFilter is not needed anymore
            return Response.ok().entity(getOpenAPI()).build();
        }
        return Response.ok().entity(getOpenAPI()).build();
    }

    private OpenAPI getOpenAPI() {
        if (!openAPIProcessors.isEmpty()) {
            try {
                final OpenAPI openAPI = Json.mapper().readValue(Json.mapper().writeValueAsString(this.openAPI),
                        OpenAPI.class);
                for (OpenAPIProcessor openAPIProcessor : openAPIProcessors) {
                    openAPIProcessor.process(openAPI);
                }
                return openAPI;
            } catch (IOException e) {
                LOGGER.error("Unable to serialize/deserialize swagger: " + openAPI, e);
            }
        }
        return openAPI;
    }
}
