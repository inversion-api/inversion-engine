package io.inversion.json;

import org.apache.commons.lang3.tuple.Triple;

import java.util.Stack;

public interface JSVisitor {
    boolean visit(JSNode node, Stack<Triple<JSNode, JSProperty, Object>> path);
}
