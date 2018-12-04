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
import org.apache.velocity.runtime.directive.Parse;
import org.apache.velocity.runtime.parser.node.Node;

/**
 * 
 * @author Wells Burke
 *
 */
public class LayoutDirective extends Parse
{
   @Override
   public String getName()
   {
      return "layout";
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

      for (int i = 1; i < length - 2; i += 2)
      {
         node.jjtGetChild(0).value(context);

         String arg = node.jjtGetChild(i).value(context) + "";
         Object val = node.jjtGetChild(i + 1).value(context);
         context.put(arg, val);
      }

      Node n = node.jjtGetChild(length - 1);
      Node renderNode = null;
      int numChildren = n.jjtGetNumChildren();

      Writer savedWriter = new StringWriter();

      for (int i = 0; i < numChildren; i++)
      {
         renderNode = n.jjtGetChild(i);
         renderNode.render(context, savedWriter);
      }
      context.put(var, savedWriter.toString());

      return super.render(context, writer, node);
   }
}