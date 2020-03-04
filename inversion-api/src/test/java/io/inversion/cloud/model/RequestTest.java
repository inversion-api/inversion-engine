package io.inversion.cloud.model;

public class RequestTest
{

//   JSNode singleNode = new JSNode("key1", "value1", "key2", "value2");
//
//   @Test
//   public void getData_returns_JSArray_when_body_is_customSingle()
//   {
//
//      Request request = new Request("GET", "/", singleNode.toString());
//      List<JSNode> data = request.getData().asNodeList();
//      assertThat(data.size(), is(1));
//      assertThat(data.get(0).toString(), is(singleNode.toString()));
//   }
//
//   @Test
//   public void getData_returns_JSArray_when_body_is_customMultiNode()
//   {
//      JSArray array = new JSArray(singleNode, new JSNode(singleNode));
//      Request request = new Request("GET", "/", array.toString());
//      List<JSNode> data = request.getData().asNodeList();
//      assertThat(data.size(), is(2));
//      data.forEach(jsNode -> assertThat(jsNode.toString(), is(singleNode.toString())));
//   }
//
//   @Test
//   public void getData_return_JSArray_when_body_is_inversionWrapper()
//   {
//      JSNode wrapper = new JSNode();
//      wrapper.put("meta", new JSArray(new JSNode("metaKey1", "metaValue1", "metaKey2", "metaValue2")));
//      wrapper.put("data", new JSArray(singleNode));
//      Request request = new Request("GET", "/", wrapper.toString());
//      List<JSNode> data = request.getData().asNodeList();
//      assertThat(data.size(), is(1));
//      assertThat(data.get(0).toString(), is(singleNode.toString()));
//   }
}
