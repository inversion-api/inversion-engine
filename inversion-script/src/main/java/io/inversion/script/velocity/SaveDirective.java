/*
 * Copyright (c) 2006 Wells Burke
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
package io.inversion.script.velocity;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.InputBase;
import org.apache.velocity.runtime.parser.node.Node;

import io.inversion.utils.Utils;

/**
 * 
 *
 */
public class SaveDirective extends InputBase {

   @Override
   public String getName() {
      return "save";
   }

   @Override
   public int getType() {
      return BLOCK;
   }

   @Override
   public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {
      String var = "content";

      int length = node.jjtGetNumChildren();
      Node n = node.jjtGetChild(0);

      if (length > 1) {
         n = node.jjtGetChild(1);
         Node argNode = node.jjtGetChild(0);
         Object argVar = argNode.value(context);
         if (!Utils.empty(argVar)) {
            var = argVar.toString();
         }
      }

      Node renderNode = null;
      int numChildren = n.jjtGetNumChildren();

      Writer savedWriter = new StringWriter();

      for (int i = 0; i < numChildren; i++) {
         renderNode = n.jjtGetChild(i);
         renderNode.render(context, savedWriter);
      }

      String content = savedWriter.toString();

      //don't include the final trailing return
      if (content.endsWith("\r\n"))
         content = content.substring(0, content.length() - 2);
      else if (content.endsWith("\n"))
         content = content.substring(0, content.length() - 1);

      context.put(var, content);

      return true;
   }
}