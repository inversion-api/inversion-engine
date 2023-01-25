package io.inversion.json;


public interface JSVisitor {
    boolean visit(JSPointer path);
}
