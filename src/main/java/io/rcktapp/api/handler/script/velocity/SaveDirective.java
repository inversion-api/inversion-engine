/*
 * Copyright (c) 2006 Wells Burke
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package io.rcktapp.api.handler.script.velocity;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.InputBase;
import org.apache.velocity.runtime.parser.node.Node;

import io.forty11.j.J;

/**
 * 
 * @author Wells Burke
 *
 */
public class SaveDirective extends InputBase
{
   @Override
   public String getName()
   {
      return "save";
   }

   @Override
   public int getType()
   {
      return BLOCK;
   }

   @Override
   public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException
   {
      String var = "content";

      int length = node.jjtGetNumChildren();
      Node n = node.jjtGetChild(0);

      if (length > 1)
      {
         n = node.jjtGetChild(1);
         Node argNode = node.jjtGetChild(0);
         Object argVar = argNode.value(context);
         if (!J.empty(argVar))
         {
            var = argVar.toString();
         }
      }

      Node renderNode = null;
      int numChildren = n.jjtGetNumChildren();

      Writer savedWriter = new StringWriter();

      for (int i = 0; i < numChildren; i++)
      {
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