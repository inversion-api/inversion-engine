/*
 * Copyright (c) 2006 Wells Burke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.script.velocity;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Parse;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 *
 */
public class LayoutDirective extends Parse {
    @Override
    public String getName() {
        return "layout";
    }

    @Override
    public int getType() {
        return BLOCK;
    }

    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {
        String var = "content";

        int length = node.jjtGetNumChildren();

        for (int i = 1; i < length - 2; i += 2) {
            node.jjtGetChild(0).value(context);

            String arg = node.jjtGetChild(i).value(context) + "";
            Object val = node.jjtGetChild(i + 1).value(context);
            context.put(arg, val);
        }

        Node n           = node.jjtGetChild(length - 1);
        Node renderNode;
        int  numChildren = n.jjtGetNumChildren();

        Writer savedWriter = new StringWriter();

        for (int i = 0; i < numChildren; i++) {
            renderNode = n.jjtGetChild(i);
            renderNode.render(context, savedWriter);
        }
        context.put(var, savedWriter.toString());

        return super.render(context, writer, node);
    }
}