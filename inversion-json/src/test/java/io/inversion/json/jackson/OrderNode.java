package io.inversion.json.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import io.inversion.json.JSNode;

public class OrderNode /* extends JSNode*/ {
    int  orderId    = 0;
    String   receiptNum = null;
    JsonNode lineItems  = null;

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getReceiptNum() {
        return receiptNum;
    }

    public void setReceiptNum(String receiptNum) {
        this.receiptNum = receiptNum;
    }

    public JsonNode getLineItems() {
        return lineItems;
    }

    public void setLineItems(JsonNode lineItems) {
        this.lineItems = lineItems;
    }

}
