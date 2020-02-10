/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.model;

import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;

public interface EngineListener
{
   default void onStartup(Engine engine, Api api)
   {

   }

   default void afterRequest(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res)
   {

   }

   default void afterError(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res)
   {

   }

   default void beforeFinally(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res)
   {

   }

}