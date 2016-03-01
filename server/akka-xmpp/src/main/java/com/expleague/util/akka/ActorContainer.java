package com.expleague.util.akka;

import akka.actor.Props;
import akka.actor.UntypedActor;

/**
 * @author vpdelta
 */
public class ActorContainer extends UntypedActor {
  private final ActorInvokeDispatcher dispatcher;

  public static Props props(final Class<? extends ActorAdapter> adapter, final Object... args) {
    return Props.create(ActorContainer.class, new Object[]{new AdapterProps[]{AdapterProps.create(adapter, args)}});
  }

  public static Props props(
    final AdapterProps adapterProps,
    final AdapterProps overrideProps
  ) {
    return Props.create(ActorContainer.class, new Object[]{new AdapterProps[]{adapterProps, overrideProps}});
  }

  public ActorContainer(AdapterProps[] props) {
    dispatcher = new ActorInvokeDispatcher(this, props, this::unhandled);
  }

  @Override
  public void onReceive(final Object message) throws Exception {
    dispatcher.invoke(message);
  }
}
