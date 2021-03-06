/*
 * Copyright (c) 2011-2014 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.core.eventbus.impl.local;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.*;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class LocalMessage<U, V> implements Message<V> {

  private static final Logger log = LoggerFactory.getLogger(LocalMessage.class);

  protected MessageCodec<U, V> messageCodec;
  protected LocalEventBus bus;
  protected String address;
  protected String replyAddress;
  protected MultiMap headers;
  protected U sentBody;
  protected V receivedBody;
  protected boolean send;

  public LocalMessage() {
  }

  public LocalMessage(String address, String replyAddress, MultiMap headers, U sentBody,
                      MessageCodec<U, V> messageCodec,
                      boolean send) {
    this.messageCodec = messageCodec;
    this.address = address;
    this.replyAddress = replyAddress;
    this.headers = headers;
    this.sentBody = sentBody;
    this.send = send;
  }

  protected LocalMessage(LocalMessage<U, V> other) {
    this.bus = other.bus;
    this.address = other.address;
    this.replyAddress = other.replyAddress;
    this.messageCodec = other.messageCodec;
    if (other.headers != null) {
      List<Map.Entry<String, String>> entries = other.headers.entries();
      this.headers = new CaseInsensitiveHeaders();
      for (Map.Entry<String, String> entry: entries) {
        this.headers.add(entry.getKey(), entry.getValue());
      }
    }
    if (other.sentBody != null) {
      this.sentBody = other.sentBody;
      this.receivedBody = messageCodec.transform(other.sentBody);
    }
    this.send = other.send;
  }

  public LocalMessage<U, V> copyBeforeReceive() {
    return new LocalMessage<>(this);
  }

  @Override
  public String address() {
    return address;
  }

  @Override
  public MultiMap headers() {
    // Lazily decode headers
    if (headers == null) {
      headers = new CaseInsensitiveHeaders();
    }
    return headers;
  }

  @Override
  public V body() {
    return receivedBody;
  }

  @Override
  public String replyAddress() {
    return replyAddress;
  }

  @Override
  public void fail(int failureCode, String message) {
    if (replyAddress != null) {
      sendReply(bus.createMessage(true, replyAddress, null,
        new ReplyException(ReplyFailure.RECIPIENT_FAILURE, failureCode, message), null), null, null);
    }
  }

  @Override
  public void reply(Object message) {
    reply(message, new DeliveryOptions(), null);
  }

  @Override
  public <R> void reply(Object message, Handler<AsyncResult<Message<R>>> replyHandler) {
    reply(message, new DeliveryOptions(), replyHandler);
  }

  @Override
  public void reply(Object message, DeliveryOptions options) {
    reply(message, options, null);
  }

  @Override
  public <R> void reply(Object message, DeliveryOptions options, Handler<AsyncResult<Message<R>>> replyHandler) {
    if (replyAddress != null) {
      sendReply(bus.createMessage(true, replyAddress, options.getHeaders(), message, options.getCodecName()), options, replyHandler);
    }
  }

  public void setReplyAddress(String replyAddress) {
    this.replyAddress = replyAddress;
  }

  public boolean send() {
    return send;
  }

  public MessageCodec<U, V> codec() {
    return messageCodec;
  }

  public void setBus(LocalEventBus bus) {
    this.bus = bus;
  }

  protected <R> void sendReply(LocalMessage msg, DeliveryOptions options, Handler<AsyncResult<Message<R>>> replyHandler) {
    if (bus != null) {
      bus.sendReply(msg, options, replyHandler);
    }
  }


}
