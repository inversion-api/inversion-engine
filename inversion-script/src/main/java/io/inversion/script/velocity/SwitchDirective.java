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
package io.inversion.cloud.action.script.velocity;

import java.io.IOException;
import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.InputBase;
import org.apache.velocity.runtime.parser.node.ASTDirective;
import org.apache.velocity.runtime.parser.node.ASTStringLiteral;
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