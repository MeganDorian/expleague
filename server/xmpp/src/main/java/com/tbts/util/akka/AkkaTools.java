package com.tbts.util.akka;

import akka.actor.*;
import akka.pattern.AskableActorRef;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: solar
 * Date: 21.12.15
 * Time: 14:48
 */
public class AkkaTools {
  private static final Logger log = Logger.getLogger(AkkaTools.class.getName());
  public static final FiniteDuration AKKA_OPERATION_TIMEOUT = FiniteDuration.create(1, TimeUnit.MINUTES);

  public static ActorRef getOrCreate(String path, ActorRefFactory context, BiFunction<String, ActorRefFactory, ActorRef> factory) {
    final ActorSelection selection = context.actorSelection(path);
    try {
      return Await.result(selection.resolveOne(AkkaTools.AKKA_OPERATION_TIMEOUT), Duration.Inf());
    }
    catch (ActorNotFound anf) {
      return factory.apply(path, context);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static ActorRef getOrCreate(String path, ActorRefFactory context, Supplier<Props> factory) {
    final ActorSelection selection = context.actorSelection(path);
    try {
      return Await.result(selection.resolveOne(AkkaTools.AKKA_OPERATION_TIMEOUT), Duration.Inf());
    }
    catch (ActorNotFound anf) {
      return context.actorOf(factory.get(), path);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static <T, A> T ask(ActorRef ref, A arg) {
    final AskableActorRef ask = new AskableActorRef(ref);
    //noinspection unchecked
    final Future<T> future = (Future<T>)ask.ask(arg, Timeout.apply(AkkaTools.AKKA_OPERATION_TIMEOUT));
    try {
      return Await.result(future, Duration.Inf());
    }
    catch (Exception e) {
      log.log(Level.WARNING, "Exception during synchronous ask", e);
      return null;
    }
  }
}
