package com.github.cloudecho.protobuf.converter;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.github.cloudecho.protobuf.exception.MessageConvertException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class SimpleBeanConverter implements BeanConverter {
  @Override
  public <T> T toBean(Class<T> beanType, Map<String, ?> properties) {
    T bean = newInstance(beanType);
    properties.forEach((k, v) -> {
      if (v == null) {
        return;
      }
      getterMethod(beanType, k).ifPresent(g ->
          setterMethod(beanType, g).ifPresent(setter -> {
            Object propertyValue = toPropertyValue(g.getReturnType(), g.getGenericReturnType(), v);
            try {
              setter.invoke(bean, propertyValue);
            } catch (Exception ex) {
              throw new MessageConvertException(ex);
            }
          })
      );
    });
    return bean;
  }

  @Override
  public <T> Map<String, ?> toProperties(T bean) throws MessageConvertException {
    Map<String, Object> result = new HashMap<>();
    for (Method m : bean.getClass().getMethods()) {
      if (!isGetter(m)) {
        continue;
      }
      try {
        Object value = m.invoke(bean);
        if (value != null) {
          result.put(
              toPropertiesMapKey(m.getName()),
              toPropertiesMapValue(value));
        }
      } catch (Exception e) {
        throw new MessageConvertException(e);
      }
    }
    return result;
  }

  protected String toPropertiesMapKey(String getterName) {
    int n = GETTER_PREFIX.length();
    return Character.toLowerCase(getterName.charAt(n))
        + getterName.substring(n + 1);
  }

  protected Object toPropertiesMapValue(Object value) {
    if (value instanceof Number ||
        value instanceof Boolean ||
        value instanceof Character ||
        value instanceof CharSequence ||
        value instanceof byte[] ||
        value instanceof Date) {
      return value;
    } else if (value instanceof List) {
      return ((List<?>) value).stream()
          .map(this::toPropertiesMapValue)
          .collect(Collectors.toList());
    } else if (value.getClass().isArray()) {
      List<Object> result = new ArrayList<>();
      arrayForEach(value, e ->
          result.add(toPropertiesMapValue(e)));
      return result;
    } else {
      return toProperties(value);
    }
  }

  protected void arrayForEach(Object array, Consumer<Object> consumer) {
    Objects.requireNonNull(array);
    int n = Array.getLength(array);
    for (int i = 0; i < n; i++) {
      Object e = Array.get(array, i);
      consumer.accept(e);
    }
  }

  protected Object toPropertyValue(Class<?> propertyType, Type propertyGenericType, Object value) {
    Class<?> valueType = value.getClass();
    Class<?> componentType = componentType(propertyType, propertyGenericType);
    if (componentType != null) {
      return propertyType.isArray()
          ? toArrayPropertyValue(componentType, value)
          : toListPropertyValue(componentType, value);
    } else if (propertyType.isAssignableFrom(valueType)) {
      return value;
    } else if (value instanceof Map) {
      return toBean(propertyType, (Map<String, ?>) value);
    } else if (int.class == propertyType) {
      return castToNumber(value).intValue();
    } else if (long.class == propertyType) {
      return castToNumber(value).longValue();
    } else if (float.class == propertyType) {
      return castToNumber(value).floatValue();
    } else if (double.class == propertyType) {
      return castToNumber(value).doubleValue();
    } else if (short.class == propertyType) {
      return castToNumber(value).shortValue();
    } else if (byte.class == propertyType) {
      return castToNumber(value).byteValue();
    } else {
      throw new MessageConvertException(
          String.format("Unknown propertyType \"%s\" for value \"%s\"",
              propertyType, value));
    }
  }

  protected Class<?> componentType(Class<?> propertyType, Type genericType) {
    if (List.class.isAssignableFrom(propertyType)) { // List
      if (genericType instanceof ParameterizedType) {
        return (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];
      } else {
        throw new MessageConvertException(
            String.format("Unknown genericType \"%s\"", genericType.getTypeName()));
      }
    } else if (propertyType.isArray()) { // Array
      return propertyType.getComponentType();
    }
    return null;
  }

  protected Object toArrayPropertyValue(Class<?> componentType, Object value) {
    if (value instanceof List) {
      List<?> value1 = (List<?>) value;
      Object result = Array.newInstance(componentType, value1.size());
      for (int i = 0; i < value1.size(); i++) {
        Object e = value1.get(i);
        Array.set(result, i, toPropertyValue(componentType, null, e));
      }
      return result;
    } else if (value.getClass().isArray()) {
      final int n = Array.getLength(value);
      Object result = Array.newInstance(componentType, n);
      for (int i = 0; i < n; i++) {
        Object e = Array.get(value, i);
        Array.set(result, i, toPropertyValue(componentType, null, e));
      }
      return result;
    } else {
      Object result = Array.newInstance(componentType, 1);
      Array.set(result, 0, value);
      return result;
    }
  }

  protected List<?> toListPropertyValue(Class<?> componentType, Object value) {
    if (value instanceof List) {
      return ((List<?>) value).stream()
          .map(e -> toPropertyValue(componentType, null, e))
          .collect(Collectors.toList());
    } else if (value.getClass().isArray()) {
      List<Object> result = new ArrayList<>();
      arrayForEach(value, e ->
          result.add(toPropertyValue(componentType, null, e)));
      return result;
    } else {
      return Collections.singletonList(value);
    }
  }

  protected Number castToNumber(Object value) {
    if (!(value instanceof Number)) {
      throw new MessageConvertException(
          String.format("\"%s\" is not a number but a %s",
              value, value.getClass().getName()));
    } else {
      return (Number) value;
    }
  }

  protected <T> T newInstance(Class<T> beanType) {
    try {
      return beanType.getDeclaredConstructor().newInstance();
    } catch (Exception ex) {
      throw new MessageConvertException(
          String.format("Creating instance of \"%s\" fail", beanType.getName()),
          ex);
    }
  }

  protected String toGetterName(String propertyKey) {
    int n = propertyKey.length();
    if (n == 1) {
      return GETTER_PREFIX + Character.toUpperCase(propertyKey.charAt(0));
    } else if (Character.isUpperCase(propertyKey.charAt(1))) {
      return GETTER_PREFIX + Character.toLowerCase(propertyKey.charAt(0))
          + propertyKey.substring(1);
    } else {
      return GETTER_PREFIX + Character.toUpperCase(propertyKey.charAt(0))
          + propertyKey.substring(1);
    }
  }

  protected final Cache<String, Optional<Method>> BEAN_METHODS_CACHE = CacheBuilder.newBuilder().build();

  protected <T> Optional<Method> getterMethod(Class<T> beanType, String propertyKey) {
    String getterName = toGetterName(propertyKey);
    try {
      return BEAN_METHODS_CACHE.get(getterName + '@' + beanType.getName(), () -> {
        try {
          Method m = beanType.getMethod(getterName);
          return isGetter(m) ? Optional.of(m) : Optional.empty();
        } catch (NoSuchMethodException e) {
          return Optional.empty();
        }
      });
    } catch (ExecutionException e) {
      throw new MessageConvertException(e);
    }
  }

  protected <T> Optional<Method> setterMethod(Class<T> beanType, Method getter) {
    String setterName = "s" + getter.getName().substring(1);
    Class<?> propertyType = getter.getReturnType();
    try {
      return BEAN_METHODS_CACHE.get(setterName + '@' + beanType.getName(), () -> {
        try {
          Method m = beanType.getMethod(setterName, propertyType);
          return Optional.of(m);
        } catch (NoSuchMethodException e) {
          return Optional.empty();
        }
      });
    } catch (ExecutionException e) {
      throw new MessageConvertException(e);
    }
  }

  protected boolean isGetter(Method m) {
    return m.getParameterCount() == 0
        && m.getName().startsWith(GETTER_PREFIX)
        && m.getName().length() > GETTER_PREFIX.length()
        && m.getReturnType() != void.class
        && !"getClass".equals(m.getName());
  }
}
