//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.12.11 at 11:34:34 PM MSK 
//


package com.expleague.xmpp.stanza;

import com.expleague.xmpp.AnyHolder;
import com.expleague.xmpp.Item;
import com.expleague.xmpp.JID;
import com.expleague.xmpp.stanza.data.Err;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * <p>Java class for anonymous complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;choice maxOccurs="unbounded" minOccurs="0">
 *           &lt;element ref="{jabber:client}subject"/>
 *           &lt;element ref="{jabber:client}body"/>
 *           &lt;element ref="{jabber:client}thread"/>
 *         &lt;/choice>
 *         &lt;any processContents='lax' namespace='##other' maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{jabber:client}error" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="from" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}NMTOKEN" />
 *       &lt;attribute name="to" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="type" default="normal">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}NMTOKEN">
 *             &lt;enumeration value="chat"/>
 *             &lt;enumeration value="error"/>
 *             &lt;enumeration value="groupchat"/>
 *             &lt;enumeration value="headline"/>
 *             &lt;enumeration value="normal"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "message")
public class Message extends Stanza implements AnyHolder {
  @XmlAnyElement(lax = true)
  protected List<Object> any = new ArrayList<>();

  @XmlElement
  protected Err error;

  @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
  @XmlSchemaType(name = "language")
  protected String lang;

  @XmlAttribute
  private MessageType type;

  public Message() {}

  public Message(JID from, JID to, String message) {
    this.from = from;
    this.to = to;
    any.add(new Body(message));
  }

  public Message(JID from, JID to, Item... any) {
    this.from = from;
    this.to = to;
    this.any.addAll(Arrays.asList(any));
  }

  public Message(JID from, JID to, MessageType type, Item... any) {
    this.from = from;
    this.to = to;
    this.type = type;
    this.any.addAll(Arrays.asList(any));
  }

  public Message(JID from, JID to, MessageType type, String str) {
    this.from = from;
    this.to = to;
    this.type = type;
    any.add(new Body(str));
  }

  public Message(JID to, MessageType type, Item... any) {
    this.to = to;
    this.type = type;
    this.any.addAll(Arrays.asList(any));
  }

  public Message(JID to, MessageType type, String body) {
    this.to = to;
    this.type = type;
    any.add(new Body(body));
  }

  public Message(JID to, String body) {
    this.to = to;
    any.add(new Body(body));
  }

  public Message(JID to, Item... items) {
    this.to = to;
    this.any.addAll(Arrays.asList(items));
  }

  public Message(String id) {
    super(id);
  }

  public MessageType type() {
    return type;
  }

  public void type(MessageType type) {
    this.type = type;
  }

  public String body() {
    final Body t = get(Body.class);
    return t != null ? t.value : "";
  }


  public <T extends Item> T copy(String idSuffix){
    final Message clone = super.copy(idSuffix);
    if (any != null)
      clone.any = new ArrayList<>(any);
    //noinspection unchecked
    return (T)clone;
  }

  @Override
  public List<? super Item> any() {
    return any;
  }

  /**
   * <p>Java class for anonymous complex type.
   *
   * <p>The following schema fragment specifies the expected content contained within this class.
   *
   * <pre>
   * &lt;complexType>
   *   &lt;simpleContent>
   *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
   *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/>
   *     &lt;/extension>
   *   &lt;/simpleContent>
   * &lt;/complexType>
   * </pre>
   *
   *
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlRootElement(name = "body")
  public static class Body extends Item {

    @XmlValue
    protected String value;
    @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "language")
    protected String lang;

    @SuppressWarnings("unused")
    public Body() {}

    public Body(String message) {
      value = message;
    }

    public String value() {
      return value;
    }
  }

  /**
   * <p>Java class for anonymous complex type.
   *
   * <p>The following schema fragment specifies the expected content contained within this class.
   *
   * <pre>
   * &lt;complexType>
   *   &lt;simpleContent>
   *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
   *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/>
   *     &lt;/extension>
   *   &lt;/simpleContent>
   * &lt;/complexType>
   * </pre>
   *
   *
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlRootElement(name = "subject")
  public static class Subject extends Item {
    @XmlValue
    protected String value;

    @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
    protected String lang;

    public Subject() {
    }

    public Subject(final String value) {
      this.value = value;
    }

    public String value() {
      return value;
    }
  }

  /**
   * <p>Java class for anonymous complex type.
   *
   * <p>The following schema fragment specifies the expected content contained within this class.
   *
   * <pre>
   * &lt;complexType>
   *   &lt;simpleContent>
   *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>NMTOKEN">
   *       &lt;attribute name="parent" type="{http://www.w3.org/2001/XMLSchema}NMTOKEN" />
   *     &lt;/extension>
   *   &lt;/simpleContent>
   * &lt;/complexType>
   * </pre>
   *
   *
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "", propOrder = {
      "value"
  })
  @XmlRootElement(name = "thread")
  public static class Thread extends Item {
    @XmlValue
    protected String value;
    @XmlAttribute(name = "parent")
    protected String parent;
  }

  @SuppressWarnings("unused")
  @XmlEnum
  public enum MessageType {
    @XmlEnumValue(value = "groupchat") GROUP_CHAT,
    @XmlEnumValue(value = "chat") CHAT,
    @XmlEnumValue(value = "normal") NORMAL,
    @XmlEnumValue(value = "error") ERROR,
    @XmlEnumValue(value = "headline") HEADLINE,
    @XmlEnumValue(value = "sync") SYNC,
  }
}
