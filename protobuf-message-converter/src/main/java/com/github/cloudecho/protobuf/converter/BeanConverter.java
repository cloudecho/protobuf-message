package com.github.cloudecho.protobuf.converter;

import java.util.Map;

import com.github.cloudecho.protobuf.exception.MessageConvertException;

public interface BeanConverter {
  String GETTER_PREFIX = "get";
  String SETTER_PREFIX = "set";

  /**
   * Populate a bean with a given properties map
   *
   * @param beanType   The class object of the result bean
   * @param properties Then given properties map
   * @param <T>        The type of the result bean
   * @return A populated bean
   * @throws MessageConvertException
   */
  <T> T toBean(Class<T> beanType, Map<String, ?> properties)
      throws MessageConvertException;

  /**
   * Describe a bean in the type of properties map
   * @param bean Then bean object to be described
   * @param <T> The type of the bean
   * @return A properties map
   * @throws MessageConvertException
   */
  <T> Map<String, ?> toProperties(T bean)
      throws MessageConvertException;
}
