package com.github.cloudecho.protobuf;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class TestBean {
  private int anInt;
  private Integer integer;
  private long aLong;
  private Long aLongObject;
  private String string;
  private Date date;

  private NestedBean nestedDto;
  private List<String> stringList;
  private NestedBean[] nestedDtos;

  public int getAnInt() {
    return anInt;
  }

  public void setAnInt(int anInt) {
    this.anInt = anInt;
  }

  public Integer getInteger() {
    return integer;
  }

  public void setInteger(Integer integer) {
    this.integer = integer;
  }

  public long getaLong() {
    return aLong;
  }

  public void setaLong(long aLong) {
    this.aLong = aLong;
  }

  public Long getaLongObject() {
    return aLongObject;
  }

  public void setaLongObject(Long aLongObject) {
    this.aLongObject = aLongObject;
  }

  public String getString() {
    return string;
  }

  public void setString(String string) {
    this.string = string;
  }

  public NestedBean getNestedDto() {
    return nestedDto;
  }

  public void setNestedDto(NestedBean nestedDto) {
    this.nestedDto = nestedDto;
  }

  public NestedBean[] getNestedDtos() {
    return nestedDtos;
  }

  public void setNestedDtos(NestedBean[] nestedDtos) {
    this.nestedDtos = nestedDtos;
  }

  public List<String> getStringList() {
    return stringList;
  }

  public void setStringList(List<String> stringList) {
    this.stringList = stringList;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  @Override
  public String toString() {
    return "TestBean{" +
        "anInt=" + anInt +
        ", integer=" + integer +
        ", aLong=" + aLong +
        ", aLongObject=" + aLongObject +
        ", string='" + string + '\'' +
        ", date=" + date +
        ", nestedDto=" + nestedDto +
        ", stringList=" + stringList +
        ", nestedDtos=" + Arrays.toString(nestedDtos) +
        '}';
  }

  public static class NestedBean {
    private int nestedInt;
    private Integer nestedInteger;
    private long nestedLong;
    private Long nestedLongObject;
    private String nestedString;

    private List<String> stringList;

    public int getNestedInt() {
      return nestedInt;
    }

    public void setNestedInt(int nestedInt) {
      this.nestedInt = nestedInt;
    }

    public Integer getNestedInteger() {
      return nestedInteger;
    }

    public void setNestedInteger(Integer nestedInteger) {
      this.nestedInteger = nestedInteger;
    }

    public long getNestedLong() {
      return nestedLong;
    }

    public void setNestedLong(long nestedLong) {
      this.nestedLong = nestedLong;
    }

    public Long getNestedLongObject() {
      return nestedLongObject;
    }

    public void setNestedLongObject(Long nestedLongObject) {
      this.nestedLongObject = nestedLongObject;
    }

    public String getNestedString() {
      return nestedString;
    }

    public void setNestedString(String nestedString) {
      this.nestedString = nestedString;
    }

    public List<String> getStringList() {
      return stringList;
    }

    public void setStringList(List<String> stringList) {
      this.stringList = stringList;
    }

    @Override
    public String toString() {
      return "NestedBean{" +
          "nestedInt=" + nestedInt +
          ", nestedInteger=" + nestedInteger +
          ", nestedLong=" + nestedLong +
          ", nestedLongObject=" + nestedLongObject +
          ", nestedString='" + nestedString + '\'' +
          ", stringList=" + stringList +
          '}';
    }
  }
}
