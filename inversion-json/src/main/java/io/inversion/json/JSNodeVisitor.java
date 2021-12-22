package io.inversion.json;

import org.apache.commons.lang3.tuple.Triple;

import java.util.Stack;

public interface JSNodeVisitor {
    boolean visit(JSNode node, Object key, Object value, Stack<Triple<JSNode, String, Object>> path);
}
