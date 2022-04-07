package com.github.cloudecho.protobuf.converter;


import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.github.cloudecho.protobuf.exception.MessageConvertException;
import com.google.protobuf.Message;

public interface ProtobufMessageConverter {
  String GETTER_PREFIX = "get";
  String SETTER_PREFIX = "set";
  String HASSER_PREFIX = "has";
  String LIST_SETTER_PREFIX = "addAll";

  String BUILDER_SUFFIX = "OrBuilder";
  String BYTES_SUFFIX = "Bytes";
  String COUNT_SUFFIX = "Count";
  String LIST_SUFFIX = "List";
  String VALUE_SUFFIX = "Value";

  String GET_DEFAULT_INSTANCE_METHOD = "getDefaultInstance";

  List<String> PROTOBUF_INTERNAL_METHODS = Arrays.asList(
      "getDefaultInstance",
      "getDefaultInstanceForType",
      "getDescriptor",
      "getParserForType",
      "getSerializedSize",
      "getUnknownFields"
  );

  /**
   * Convert a protobuf message to a case-sensitive properties map
   *
   * @param message A protobuf message
   * @return The properties map of the given message
   */
  Map<String, ?> toProperties(Message message)
      throws MessageConvertException;

  /**
   * Convert a protobuf message to a java bean
   *
   * @param beanType The target bean type
   * @param message  A protobuf message
   * @return The target bean
   */
  default <T> T toBean(Class<T> beanType, Message message, BeanConverter beanConverter)
      throws MessageConvertException {
    return beanConverter.toBean(beanType, toProperties(message));
  }

  /**
   * Create a {@code Message.Builder} with the given properties
   *
   * @param messageType The type of the target object to be populated
   * @param properties  The given properties map
   * @param <T>         The type of the protobuf message
   * @return The {@code Message.Builder} populated with then given properties
   */
  <T extends Message> Message.Builder
  newMessageBuilder(Class<T> messageType, Map<String, ?> properties)
      throws MessageConvertException;

  /**
   * Create a {@code Message.Builder} with the given bean
   *
   * @param messageType The type of the target object to be populated
   * @param bean        The given properties map
   * @param <T>         The type of the protobuf message
   * @return The {@code Message.Builder} populated with then given properties
   */
  default <T extends Message> Message.Builder
  newMessageBuilder(Class<T> messageType, Object bean, BeanConverter beanConverter)
      throws MessageConvertException {
    return newMessageBuilder(messageType, beanConverter.toProperties(bean));
  }

  /**
   * Build a protobuf message with the given properties map
   *
   * @param messageType The type of the target object to be populated
   * @param properties  A properties map
   * @param <T>         The type of protobuf message
   * @return The populated protobuf message object
   */
  @SuppressWarnings("unchecked")
  default <T extends Message> T
  buildMessage(Class<T> messageType, Map<String, ?> properties)
      throws MessageConvertException {
    return (T) newMessageBuilder(messageType, properties).build();
  }

  /**
   * Build a protobuf message with the given bean
   *
   * @param messageType The type of the target object to be populated
   * @param bean        The given bean
   * @param <T>         The type of protobuf message
   * @return The populated protobuf message object
   */
  @SuppressWarnings("unchecked")
  default <T extends Message> T
  buildMessage(Class<T> messageType, Object bean, BeanConverter beanConverter)
      throws MessageConvertException {
    return (T) newMessageBuilder(messageType, bean, beanConverter).build();
  }
}
