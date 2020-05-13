/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
package io.inversion.action.db;

/**
 * Delegates all operations to DbPostAction which currently implements all POST/PUT/PATCH methods.
 * <p>
 * Currently this class exists as a potential future compatibility shim and so that people looking at the
 * source code tree before really digging in will not be confused by the superficial lack of a PATCH action class. 
 */
public class DbPatchAction extends DbPostAction<DbPatchAction>
{

}
