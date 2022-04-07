package com.github.cloudecho.protobuf.exception;

public class MessageConvertException extends RuntimeException {
  public MessageConvertException(String message) {
    super(message);
  }

  public MessageConvertException(Throwable cause) {
    super(cause);
  }

  public MessageConvertException(String message, Throwable cause) {
    super(message, cause);
  }
}
