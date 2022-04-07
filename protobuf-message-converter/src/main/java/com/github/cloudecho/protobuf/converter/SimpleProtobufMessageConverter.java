package com.github.cloudecho.protobuf.converter;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.github.cloudecho.protobuf.exception.MessageConvertException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolMessageEnum;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;


public class SimpleProtobufMessageConverter implements ProtobufMessageConverter {
  private static final Logger logger = LoggerFactory.getLogger(SimpleProtobufMessageConverter.class);

  @Override
  public Map<String, Object> toProperties(Message message) {
    Map<String, Object> result = new HashMap<>();
    for (Method getter : getMessageGetterList(message.getClass()).values()) {
      String getterName = getter.getName();
      if (!maybeMessageValuePresent(message, getterName)) {
        continue;
      }

      Object value;
      try {
        value = getter.invoke(message);
      } catch (Exception ex) {
        throw new MessageConvertException(
            String.format("invoke getter \"%s\" on an object of type of \"%s\" fail",
                getterName, message.getClass().getName()), ex);
      }

      value = toPropertyValue(value);
      if (value == null || (value instanceof Collection && CollectionUtils.isEmpty((Collection<?>) value))) {
        continue;
      }
      String propertyKey = toPropertyKey(getter);
      result.put(propertyKey, value);
    }
    return result;
  }


  @Override
  public <T extends Message> Message.Builder
  newMessageBuilder(Class<T> messageType, Map<String, ?> properties) {
    Message.Builder builder = newMessageBuilder(messageType);
    for (Map.Entry<String, ?> entry : properties.entrySet()) {
      Optional<Method> setter = getMessageSetterMethod(entry.getKey(), builder);
      if (!setter.isPresent()) {
        continue;
      }
      buildMessageAttribute(builder, setter.get(), entry.getValue());
    }
    return builder;
  }


  /**
   * Convert the getter name to property key .<br>
   * e.g. getName -&gt; name,  getMyName -&gt; myName
   */
  protected String toPropertyKey(String getterName) {
    int n = GETTER_PREFIX.length();
    return Character.toLowerCase(getterName.charAt(n))
        + getterName.substring(n + 1);
  }

  /**
   * Convert the protobuf message attribute value to property value
   */
  protected Object toPropertyValue(Object value) {
    if (value instanceof ProtocolMessageEnum) {
      return ((ProtocolMessageEnum) value).getNumber();
    } else if (value instanceof String) {
      String v = (String) value;
      return StringUtils.hasLength(v) ? v : null;
    } else if (value instanceof ByteString) {
      return unwrap((ByteString) value);
    } else if (value instanceof List) {
      return toPropertyValueOf((List) value);
    } else if (value instanceof Timestamp) {
      return toDate((Timestamp) value);
    } else if (value instanceof DoubleValue) {
      return ((DoubleValue) value).getValue();
    } else if (value instanceof FloatValue) {
      return ((FloatValue) value).getValue();
    } else if (value instanceof Int64Value) {
      return ((Int64Value) value).getValue();
    } else if (value instanceof UInt64Value) {
      return ((UInt64Value) value).getValue();
    } else if (value instanceof Int32Value) {
      return ((Int32Value) value).getValue();
    } else if (value instanceof UInt32Value) {
      return ((UInt32Value) value).getValue();
    } else if (value instanceof BoolValue) {
      return ((BoolValue) value).getValue();
    } else if (value instanceof StringValue) {
      return ((StringValue) value).getValue();
    } else if (value instanceof BytesValue) {
      return ((BytesValue) value).getValue();
    } else if (value instanceof Message) {
      return toProperties((Message) value);
    } else {
      return value;
    }
  }

  /**
   * Convert the property key to the setter name. <br>
   * e.g. name -&gt; setName
   */
  protected String toMessageSetterName(String propertyKey) {
    return SETTER_PREFIX
        + Character.toUpperCase(propertyKey.charAt(0))
        + propertyKey.substring(1);
  }

  /**
   * Convert the given {@code value} to a specified {@code targetType} object
   */
  @SuppressWarnings({"unchecked"})
  protected Object toMessageValue(Type targetType, Object value) {
    if (value == null) {
      return null;
    }

    if (String.class.equals(targetType)) {
      if (value instanceof byte[]) {
        return new String((byte[]) value, Charset.defaultCharset());
      }
      return String.valueOf(value);
    } else if (Timestamp.class.equals(targetType)) {
      return toTimestamp(value);
    } else if (DoubleValue.class.equals(targetType)) {
      return DoubleValue.of(castToNumber(value).doubleValue());
    } else if (FloatValue.class.equals(targetType)) {
      return FloatValue.of(castToNumber(value).floatValue());
    } else if (Int64Value.class.equals(targetType)) {
      return Int64Value.of(castToNumber(value).longValue());
    } else if (UInt64Value.class.equals(targetType)) {
      return UInt64Value.of(castToNumber(value).longValue());
    } else if (Int32Value.class.equals(targetType)) {
      return Int32Value.of(castToNumber(value).intValue());
    } else if (UInt32Value.class.equals(targetType)) {
      return UInt32Value.of(castToNumber(value).intValue());
    } else if (BoolValue.class.equals(targetType)) {
      return BoolValue.of(toBool(value));
    } else if (StringValue.class.equals(targetType)) {
      return StringValue.of(String.valueOf(value));
    } else if (BytesValue.class.equals(targetType)) {
      return BytesValue.of(toByteString(value));
    } else if (ByteString.class.equals(targetType)) {
      return toByteString(value);
    } else if (targetType instanceof ParameterizedType) {
      return toListMessageValue(LIST_COMPONENT_TYPE_CACHE.get(targetType.getTypeName()), value);
    } else if (long.class.equals(targetType) || Long.class.equals(targetType)) {
      return castToNumber(value).longValue();
    } else if (int.class.equals(targetType) || Integer.class.equals(targetType)) {
      return castToNumber(value).intValue();
    } else if (Message.class.isAssignableFrom((Class<?>) targetType)) {
      if (value instanceof Map) {
        return buildMessage((Class) targetType, (Map<String, ?>) value);
      } else {
        throw new MessageConvertException(
            String.format("Expect a Map<String,?> value but got %s, targetType: %s",
                value.getClass().getName(), targetType.getTypeName()));
      }
    }

    return value;
  }

  protected boolean maybeMessageGetter(Method method) {
    String name = method.getName();
    return name.startsWith(GETTER_PREFIX)
        && !name.endsWith(BUILDER_SUFFIX)
        && !name.endsWith(BUILDER_SUFFIX + LIST_SUFFIX)
        && method.getParameterCount() == 0
        && !PROTOBUF_INTERNAL_METHODS.contains(name);
  }

  protected boolean maybeMessageValuePresent(Message message, String getterName) {
    Optional<Method> hasserMethod = getHasserMethod(message.getClass(), getterName);
    if (!hasserMethod.isPresent()) {
      // if the has-method does not exist
      return true;
    }
    try {
      return (boolean) hasserMethod.get().invoke(message);
    } catch (Exception e) {
      throw new MessageConvertException(e);
    }
  }

  protected final Cache<String, Optional<Method>> MESSAGE_HASSER_CACHE = CacheBuilder.newBuilder().build();

  protected Optional<Method> getHasserMethod(Class<?> messageClass, String getterName) {
    final String key = getterName + "@" + messageClass.getName();
    try {
      return MESSAGE_HASSER_CACHE.get(key, () -> getHasserMethod0(messageClass, getterName));
    } catch (ExecutionException e) {
      throw new MessageConvertException(e);
    }
  }

  protected Optional<Method> getHasserMethod0(Class<?> messageClass, String getterName) {
    try {
      Method hasser = messageClass.getDeclaredMethod(
          HASSER_PREFIX + getterName.substring(GETTER_PREFIX.length()));
      //if has-method exists
      logger.debug("has-method exists: {}, message type: {}", getterName, messageClass.getName());
      return Optional.of(hasser);
    } catch (NoSuchMethodException e) {
      return Optional.empty();
    }
  }

  protected String toPropertyKey(Method getter) {
    String getterName = getter.getName();
    if (List.class.isAssignableFrom(getter.getReturnType())) {
      if (getterName.endsWith(LIST_SUFFIX)) {
        getterName = getterName.substring(0, getterName.length() - LIST_SUFFIX.length());
      }
    }

    return toPropertyKey(getterName);
  }

  protected List<Object> toPropertyValueOf(List value) {
    if (CollectionUtils.isEmpty(value)) {
      return null;
    }
    List<Object> v = new ArrayList<>();
    for (Object item : value) {
      v.add(toPropertyValue(item));
    }
    return v;
  }


  protected byte[] unwrap(ByteString bytes) {
    return bytes.size() == 0 ? null : bytes.toByteArray();
  }


  protected final Cache<String, Map<String, Method>> MESSAGE_GETTERS_CACHE = CacheBuilder.newBuilder().build();

  protected <T extends Message> Map<String, Method> getMessageGetterList(final Class<T> messageType) {
    try {
      return MESSAGE_GETTERS_CACHE.get(
          messageType.getName(),
          () -> getMessageGetterList0(messageType));
    } catch (ExecutionException e) {
      throw new MessageConvertException(e);
    }
  }

  protected <T extends Message> Map<String, Method> getMessageGetterList0(Class<T> messageType) {
    Map<String, Method> result = new LinkedHashMap<>();
    for (Method getter : messageType.getDeclaredMethods()) {
      String name = getter.getName();
      if (maybeMessageGetter(getter)) {
        result.put(name, getter);
      }
    }
    for (String k : result.keySet().toArray(new String[0])) {
      Method getter = result.get(k);
      if (getter == null) {
        continue;
      }
      result.remove(k + BYTES_SUFFIX);
      Class<?> returnType = getter.getReturnType();
      if (k.endsWith(LIST_SUFFIX) && List.class.isAssignableFrom(returnType)) {
        result.remove(k.substring(0, k.length() - LIST_SUFFIX.length()) + COUNT_SUFFIX);
      }
      // ProtocolMessageEnum
      if (ProtocolMessageEnum.class.isAssignableFrom(returnType)) {
        result.remove(k + VALUE_SUFFIX);
      }
    }
    return result;
  }

  protected final Cache<String, Message> DEFAULT_MESSAGE_INSTANCE_CACHE = CacheBuilder.newBuilder().build();

  protected Message getDefaultMessageInstance(Class<?> messageType) {
    try {
      return DEFAULT_MESSAGE_INSTANCE_CACHE.get(messageType.getName(),
          () -> (Message) messageType.getMethod(GET_DEFAULT_INSTANCE_METHOD).invoke(null));
    } catch (ExecutionException e) {
      throw new MessageConvertException(e);
    }
  }

  @SuppressWarnings("unchecked")
  protected <T extends Message> Message.Builder newMessageBuilder(Class<T> messageType) {
    return getDefaultMessageInstance(messageType).toBuilder();
  }

  protected final Cache<String, Optional<Method>> MESSAGE_SETTERS_CACHE = CacheBuilder.newBuilder().build();

  protected Optional<Method> getMessageSetterMethod(final String propertyKey, final Message.Builder builder) {
    final String key = propertyKey + "@" + builder.getClass().getName();
    try {
      return MESSAGE_SETTERS_CACHE.get(key, () -> Optional.ofNullable(getMessageSetterMethod0(propertyKey, builder)));
    } catch (ExecutionException e) {
      throw new MessageConvertException(e);
    }
  }

  /**
   * key: setter.getGenericParameterTypes()[0].getTypeName()
   */
  protected Map<String, Class<?>> LIST_COMPONENT_TYPE_CACHE = new ConcurrentHashMap<>();

  protected Method getMessageSetterMethod0(String propertyKey, Message.Builder builder) {
    String setterName = toMessageSetterName(propertyKey);
    logger.debug("propertyKey to message-setter: {} -> {}", propertyKey, setterName);

    Map<String, Method> getterList = getMessageGetterList(builder.getDefaultInstanceForType().getClass());
    Method getter = getterList.get("g" + setterName.substring(1));
    boolean isList = false;
    boolean isProtocolMessageEnum = false;

    if (getter == null) {
      getter = getterList.get("g" + setterName.substring(1) + LIST_SUFFIX);
      if (getter == null) {
        logger.debug("No getter method: {}, builder type: {}", setterName, builder.getClass().getName());
        return null;
      }
      setterName = LIST_SETTER_PREFIX + setterName.substring(SETTER_PREFIX.length());
      isList = true;
    } else if (ProtocolMessageEnum.class.isAssignableFrom(getter.getReturnType())) {
      setterName += VALUE_SUFFIX;
      isProtocolMessageEnum = true;
    }

    try {
      Method setter = builder.getClass().getDeclaredMethod(setterName,
          isList ? Iterable.class : isProtocolMessageEnum ? int.class : getter.getReturnType());
      if (isList) {
        final String setterGenericParamTypeName = setter.getGenericParameterTypes()[0].getTypeName();
        LIST_COMPONENT_TYPE_CACHE.put(setterGenericParamTypeName,
            toListComponentType(setterGenericParamTypeName, getter.getGenericReturnType()));
      }
      return setter;
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  protected Class<?> toListComponentType(String setterGenericParamTypeName, Type getterGenericReturnType) {
    if (getterGenericReturnType instanceof ParameterizedType) {
      return (Class<?>) ((ParameterizedType) getterGenericReturnType).getActualTypeArguments()[0];
    } else {
      int end = setterGenericParamTypeName.lastIndexOf('>');
      int start = setterGenericParamTypeName.lastIndexOf('<', end - 1);
      try {
        return Class.forName(setterGenericParamTypeName.substring(start + 1, end));
      } catch (ClassNotFoundException e) {
        throw new MessageConvertException(e);
      }
    }
  }

  protected void buildMessageAttribute(Message.Builder builder, Method setter, Object value) {
    Type targetType = setter.getGenericParameterTypes()[0];
    try {
      value = toMessageValue(targetType, value);
      if (value != null) {
        setter.invoke(builder, value);
      }
    } catch (MessageConvertException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new MessageConvertException(
          String.format("buildMessageAttribute failed. builder type:%s, setter:%s, value:%s, targetType:%s",
              builder.getClass().getName(), setter, value, targetType), ex);
    }
  }

  protected Number castToNumber(Object value) {
    if (value == null) {
      throw new MessageConvertException("Null value is not allowed for being casted to be number");
    } else if (!(value instanceof Number)) {
      throw new MessageConvertException(
          String.format("\"%s\" is not a number but a %s",
              value, value.getClass().getName()));
    } else {
      return (Number) value;
    }
  }

  protected boolean toBool(Object value) {
    if (value == null) {
      throw new MessageConvertException("Null value is not allowed for being converted to be bool");
    } else if (value instanceof Boolean) {
      return (Boolean) value;
    } else if (value instanceof Number) {
      return castToNumber(value).intValue() != 0;
    } else if (value instanceof String) {
      return StringUtils.hasLength((String) value);
    } else {
      throw new MessageConvertException(
          String.format("Unknown value type \"%s\", cannot to be converted to be bool",
              value.getClass().getName()));
    }
  }

  protected Date toDate(Timestamp timestamp) {
    long millis = timestamp.getSeconds() * 1000
        + timestamp.getNanos() / 1000000;
    return new Date(millis);
  }

  protected Timestamp toTimestamp(Object value) {
    if (value instanceof Date) {
      return toTimestamp((Date) value);
    } else if (value instanceof Long) { // millis
      return toTimestamp(((Long) value).longValue());
    } else {
      throw new MessageConvertException(
          String.format("Unknown type \"%s\", cannot cast the value \"%s\" to be timestamp"
              , value.getClass().getName(), value));
    }
  }

  protected Timestamp toTimestamp(Date date) {
    return toTimestamp(date.getTime());
  }

  protected Timestamp toTimestamp(final long millis) {
    return Timestamp.newBuilder()
        .setSeconds(millis / 1000)
        .setNanos((int) ((millis % 1000) * 1000000))
        .build();
  }

  protected ByteString toByteString(Object value) {
    if (value instanceof byte[]) {
      return ByteString.copyFrom((byte[]) value);
    } else if (value instanceof String) {
      return ByteString.copyFromUtf8((String) value);
    } else if (value instanceof ByteBuffer) {
      return ByteString.copyFrom((ByteBuffer) value);
    } else {
      throw new MessageConvertException(
          String.format("Cannot convert value \"%s\" from type of %s to ByteString",
              value, value.getClass().getName()));
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  protected List toListMessageValue(Class componentType, Object value) {
    List result = new ArrayList();

    // If value is not a List
    if (!(value instanceof List)) {
      value = toMessageValue(componentType, value);
      if (value == null) {
        return null;
      } else {
        result.add(value);
        return result;
      }
    }

    // If value is a List
    for (Object item : (List) value) {
      Object v = toMessageValue(componentType, item);
      if (v != null) {
        result.add(v);
      }
    }

    return result.isEmpty() ? null : result;
  }
}
