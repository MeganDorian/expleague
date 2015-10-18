package com.tbts.model.impl;

import com.spbsu.commons.func.Action;
import com.spbsu.commons.func.impl.WeakListenerHolderImpl;
import com.tbts.model.*;
import com.tbts.model.experts.ExpertManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * User: solar
 * Date: 04.10.15
 * Time: 19:25
 */
public class RoomImpl extends WeakListenerHolderImpl<Room> implements Room {
  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final Action<Client> clientLst;
  private final String id;

  private State state;

  public RoomImpl(String id, Client client) {
    this.id = id;
    state = State.INIT;
    client.addListener(clientLst = cl -> {
      if (cl.active() != this)
        return;
      switch (cl.state()) {
        case CHAT:
        case COMMITED:
          commit();
          break;
        case FEEDBACK:
          fix();
          break;
      }
    });
  }

  protected void fix() {
    if (state != State.COMPLETE)
      throw new IllegalStateException();
  }

  protected void commit() {
    if (state != State.CLEAN && state != State.COMPLETE)
      throw new IllegalStateException();
    answersCountOnDeploy = answers.size();
    state(State.DEPLOYED);
    ExpertManager.instance().challenge(this);
  }

  @Override
  public String id() {
    return id;
  }

  private Query.Builder qBuilder = new Query.Builder();

  @Override
  public void text(String text) {
    if (state != State.CLEAN)
      throw new IllegalStateException();
    qBuilder.addText(text);
  }

  public Query query() {
    return qBuilder.build();
  }

  private void state(State state) {
    this.state = state;
    invoke(this);
  }

  private final List<Answer> answers = new ArrayList<>();
  private int answersCountOnDeploy;
  @Override
  public void answer(Answer answer) {
    answers.add(answer);
  }

  @Override
  public void enterExpert(Expert winner) {
    state(State.LOCKED);
    if (answers.size() > answersCountOnDeploy)
      state(State.COMPLETE);
  }

  @Override
  public void open() {
    state(State.CLEAN);
  }

  @Override
  public boolean quorum(Set<Expert> reserved) {
    return reserved.size() > 0;
  }

  @Override
  public State state() {
    return state;
  }
}
