package io.inversion;

public class RequestTest {

//   JSNode singleNode = new JSMap("key1", "value1", "key2", "value2");
//
//   @Test
//   public void getData_returns_JSList_when_body_is_customSingle()
//   {
//
//      Request request = new Request("GET", "/", singleNode.toString());
//      List<JSNode> data = request.getData().asNodeList();
//      assertThat(data.size(), is(1));
//      assertThat(data.get(0).toString(), is(singleNode.toString()));
//   }
//
//   @Test
//   public void getData_returns_JSList_when_body_is_customMultiNode()
//   {
//      JSList array = new JSList(singleNode, new JSMap(singleNode));
//      Request request = new Request("GET", "/", array.toString());
//      List<JSNode> data = request.getData().asNodeList();
//      assertThat(data.size(), is(2));
//      data.forEach(JSNode -> assertThat(JSNode.toString(), is(singleNode.toString())));
//   }
//
//   @Test
//   public void getData_return_JSList_when_body_is_inversionWrapper()
//   {
//      JSNode wrapper = new JSMap();
//      wrapper.put("meta", new JSList(new JSMap("metaKey1", "metaValue1", "metaKey2", "metaValue2")));
//      wrapper.put("data", new JSList(singleNode));
//      Request request = new Request("GET", "/", wrapper.toString());
//      List<JSNode> data = request.getData().asNodeList();
//      assertThat(data.size(), is(1));
//      assertThat(data.get(0).toString(), is(singleNode.toString()));
//   }
}
