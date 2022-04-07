package com.github.cloudecho.protobuf;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.cloudecho.protobuf.converter.SimpleBeanConverter;
import com.github.cloudecho.protobuf.converter.SimpleProtobufMessageConverter;
import com.github.cloudecho.protobuf.test.NestedMessage;
import com.github.cloudecho.protobuf.test.TestMessage;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleProtobufMessageConverterTest {
  SimpleProtobufMessageConverter messageConverter = new SimpleProtobufMessageConverter();
  SimpleBeanConverter beanConverter = new SimpleBeanConverter();

  @Test
  public void testToProperties() {
    TestMessage testMessage = createTestMessage();
    Map<String, ?> properties = messageConverter.toProperties(testMessage);
    //    System.out.println(properties);
    assertThat(properties.get("anInt")).isEqualTo(99);
    assertThat(properties.get("integer")).isEqualTo(100);
    assertThat(properties.get("aLong")).isEqualTo(101L);
    assertThat(properties.get("aLongObject")).isEqualTo(102L);
    assertThat(properties.get("string")).isEqualTo("a string value");
    assertThat(properties.get("stringList")).isEqualTo(Arrays.asList("str1", "str2"));
    assertThat(properties.get("date")).isEqualTo(new Date(100 * 1000));

    assertThat(properties.get("nestedDto")).isInstanceOf(Map.class);
    Map<String, ?> nestedDto = (Map<String, ?>) properties.get("nestedDto");
    assertThat(nestedDto.get("nestedInt")).isEqualTo(200);
    assertThat(nestedDto.get("nestedInteger")).isEqualTo(201);
    assertThat(nestedDto.get("nestedLong")).isEqualTo(202L);
    assertThat(nestedDto.get("nestedLongObject")).isEqualTo(203L);
    assertThat(nestedDto.get("nestedString")).isEqualTo("nested string value");
    assertThat(nestedDto.get("stringList")).isEqualTo(Arrays.asList("nested-str1", "nested-str2", "nested-str3"));

    assertThat(properties.get("nestedDtos")).isInstanceOf(List.class);
    List<Map<String, ?>> nestedDtos = (List<Map<String, ?>>) properties.get("nestedDtos");
    assertThat(nestedDtos.size()).isEqualTo(2);
    Map<String, ?> nestedDto1 = nestedDtos.get(0);
    Map<String, ?> nestedDto2 = nestedDtos.get(1);

    assertThat(nestedDto1.get("nestedInt")).isEqualTo(300);
    assertThat(nestedDto1.get("nestedInteger")).isEqualTo(301);
    assertThat(nestedDto1.get("nestedLong")).isEqualTo(302L);
    assertThat(nestedDto1.get("nestedLongObject")).isEqualTo(303L);
    assertThat(nestedDto1.get("nestedString")).isEqualTo("nested string value-1");
    assertThat(nestedDto1.get("stringList")).isEqualTo(
        Arrays.asList("nested-str-11", "nested-str-12", "nested-str-13"));

    assertThat(nestedDto2.get("nestedInt")).isEqualTo(400);
    assertThat(nestedDto2.get("nestedInteger")).isEqualTo(401);
    assertThat(nestedDto2.get("nestedLong")).isEqualTo(402L);
    assertThat(nestedDto2.get("nestedLongObject")).isEqualTo(403L);
    assertThat(nestedDto2.get("nestedString")).isEqualTo("nested string value-2");
    assertThat(nestedDto2.get("stringList")).isEqualTo(
        Arrays.asList("nested-str-21", "nested-str-22", "nested-str-23"));
  }

  @Test
  public void testToBean() {
    TestMessage testMessage = createTestMessage();
    TestBean bean = messageConverter.toBean(TestBean.class, testMessage, beanConverter);
    System.out.println(bean);
    // TODO assertions
  }

  @Test
  public void testBuildMessage() {
    TestBean bean = createTestBean();
    TestMessage testMessage = messageConverter.buildMessage(TestMessage.class, bean, beanConverter);
    System.out.println(testMessage);
    // TODO assertions
  }

  private TestMessage createTestMessage() {
    return TestMessage
        .newBuilder()
        .setAnInt(99)
        .setInteger(Int32Value.of(100))
        .setALong(101L)
        .setALongObject(Int64Value.of(102L))
        .setString("a string value")
        .addStringList("str1")
        .addStringList("str2")
        .setDate(Timestamp.newBuilder().setSeconds(100))
        .setNestedDto(NestedMessage.newBuilder()
            .setNestedInt(200)
            .setNestedInteger(Int32Value.of(201))
            .setNestedLong(202)
            .setNestedLongObject(Int64Value.of(203))
            .setNestedString("nested string value")
            .addStringList("nested-str1")
            .addStringList("nested-str2")
            .addStringList("nested-str3"))
        .addNestedDtos(NestedMessage.newBuilder()
            .setNestedInt(300)
            .setNestedInteger(Int32Value.of(301))
            .setNestedLong(302)
            .setNestedLongObject(Int64Value.of(303))
            .setNestedString("nested string value-1")
            .addStringList("nested-str-11")
            .addStringList("nested-str-12")
            .addStringList("nested-str-13"))
        .addNestedDtos(NestedMessage.newBuilder()
            .setNestedInt(400)
            .setNestedInteger(Int32Value.of(401))
            .setNestedLong(402)
            .setNestedLongObject(Int64Value.of(403))
            .setNestedString("nested string value-2")
            .addStringList("nested-str-21")
            .addStringList("nested-str-22")
            .addStringList("nested-str-23"))
        .build();
  }

  private TestBean createTestBean() {
    TestBean bean = new TestBean();
    bean.setAnInt(99);
    bean.setInteger(100);
    bean.setaLong(101L);
    bean.setaLongObject(102L);
    bean.setString("a string value");
    bean.setStringList(Arrays.asList("str1", "str2"));
    bean.setDate(new Date(100 * 1000));

    TestBean.NestedBean nestedDto = new TestBean.NestedBean();
    nestedDto.setNestedInt(200);
    nestedDto.setNestedInteger(201);
    nestedDto.setNestedLong(202);
    nestedDto.setNestedLongObject(203L);
    nestedDto.setNestedString("nested string value");
    nestedDto.setStringList(Arrays.asList("nested-str1", "nested-str2", "nested-str3"));
    bean.setNestedDto(nestedDto);

    TestBean.NestedBean nestedDto1 = new TestBean.NestedBean();
    nestedDto1.setNestedInt(300);
    nestedDto1.setNestedInteger(301);
    nestedDto1.setNestedLong(302L);
    nestedDto1.setNestedLongObject(303L);
    nestedDto1.setNestedString("nested string value-1");
    nestedDto1.setStringList(Arrays.asList("nested-str-11", "nested-str-12", "nested-str-13"));

    TestBean.NestedBean nestedDto2 = new TestBean.NestedBean();
    nestedDto2.setNestedInt(400);
    nestedDto2.setNestedInteger(401);
    nestedDto2.setNestedLong(402L);
    nestedDto2.setNestedLongObject(403L);
    nestedDto2.setNestedString("nested string value-2");
    nestedDto2.setStringList(Arrays.asList("nested-str-21", "nested-str-22", "nested-str-23"));

    bean.setNestedDtos(new TestBean.NestedBean[] {nestedDto1, nestedDto2});
    return bean;
  }
}
