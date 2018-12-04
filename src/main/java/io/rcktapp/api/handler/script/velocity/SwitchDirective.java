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
import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.InputBase;
import org.apache.velocity.runtime.parser.node.ASTDirective;
import org.apache.velocity.runtime.parser.node.ASTStringLiteral;
import org.apache.velocity.runtime.parser.node.ASTText;
import org.apache.velocity.runtime.parser.node.Node;

/**
 * 
 * @author Wells Burke
 *
 */
public class SwitchDirective extends InputBase
{
   @Override
   public String getName()
   {
      return "switch";
   }

   @Override
   public int getType()
   {
      return BLOCK;
   }

   @Override
   public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException
   {
      /*
       *  did we get an argument?
       */
      if (node.jjtGetChild(0) == null)
      {
         //rsvc.error("#switch() error :  null argument");
         return false;
      }

      /*
       *  does it have a value?  If you have a null reference, then no.
       */
      Object value = node.jjtGetChild(0).value(context);

      if (value == null)
      {
         value = "[DEFAULT]";
      }

      /*
       *  get the arg
       */
      String arg = value.toString();

      Node n = node.jjtGetChild(1);
      Node child = null;
      Node renderNode = null;
      int numChildren = n.jjtGetNumChildren();
      for (int i = 0; i < numChildren; i++)
      {
         child = n.jjtGetChild(i);
         if (child instanceof ASTDirective)
         {
            ASTDirective directive = ((ASTDirective) child);
            String dirName = ((ASTDirective) child).getDirectiveName();

            if (dirName.equalsIgnoreCase("case"))
            {
               String casetoken = ((ASTStringLiteral) directive.jjtGetChild(0)).literal();

               if (casetoken.equalsIgnoreCase(arg))
               {
                  // render all the children until we hit either
                  // a case directive, default directive, or the end of this switch
                  for (int j = i + 1; j < numChildren; j++)
                  {
                     renderNode = n.jjtGetChild(j);
                     if (renderNode instanceof ASTDirective)
                     {
                        String directiveName = ((ASTDirective) renderNode).getDirectiveName();
                        if (directiveName.equalsIgnoreCase("case") || directiveName.equalsIgnoreCase("default"))
                        {
                           break;
                        }
                     }

                     renderNode.render(context, writer);
                  }

                  break;
               }
            }
            else if (dirName.equalsIgnoreCase("default"))
            {
               for (int j = i + 1; j < numChildren; j++)
               {
                  renderNode = n.jjtGetChild(j);
                  renderNode.render(context, writer);
               }

               break;
            }
         }
      }

      return true;
   }
}