package integration_tests;

import com.expleague.bots.*;
import com.expleague.bots.utils.ExpectedMessage;
import com.expleague.bots.utils.ExpectedMessageBuilder;
import com.expleague.model.*;
import com.expleague.server.ExpLeagueServer;
import com.expleague.server.agents.GlobalChatAgent;
import com.expleague.xmpp.JID;
import com.expleague.xmpp.stanza.Message;
import com.spbsu.commons.util.sync.StateLatch;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * User: Artem
 * Date: 28.02.2017
 * Time: 15:05
 */
public class BaseSingleBotsTest {
  protected BotsManager botsManager;

  @Before
  public void setUp() throws JaxmppException {
    botsManager = new BotsManager();
  }

  @After
  public void tearDown() throws JaxmppException {
    botsManager.stopAll();
  }

  protected String generateRandomString() {
    return UUID.randomUUID().toString();
  }

  protected int generateRandomInt(int min, int max) {
    return ThreadLocalRandom.current().nextInt(min, max + 1);
  }

  protected void assertAllExpectedMessagesAreReceived(ExpectedMessage[] notReceivedMessages) {
    if (notReceivedMessages.length > 0) {
      final StringBuilder stringBuilder = new StringBuilder();
      for (ExpectedMessage expectedMessage : notReceivedMessages) {
        stringBuilder.append(String.format("\n%s was/were not received from %s (or had incorrect attributes)\n", expectedMessage, expectedMessage.from()));
      }
      Assert.fail(stringBuilder.toString());
    }
  }

  protected String domain() {
    return ExpLeagueServer.config().domain();
  }

  protected JID groupChatJID(BareJID roomJID) {
    return new JID(GlobalChatAgent.ID, domain(), roomJID.getLocalpart());
  }

  protected JID domainJID() {
    return JID.parse(domain());
  }

  protected JID botRoomJID(BareJID roomJID, Bot bot) {
    return new JID(roomJID.getLocalpart(), roomJID.getDomain(), bot.jid().getLocalpart());
  }

  protected void roomCloseStateByClientCancel(BareJID roomJID, ClientBot clientBot, AdminBot adminBot) throws JaxmppException {
    //Arrange
    final ExpectedMessage cancel = new ExpectedMessageBuilder().from(botRoomJID(roomJID, clientBot)).has(Operations.Cancel.class).build();
    final ExpectedMessage roomStateChanged = new ExpectedMessageBuilder()
        .from(groupChatJID(roomJID))
        .has(Operations.RoomStateChanged.class, rsc -> RoomState.CLOSED.equals(rsc.state()))
        .build();

    //Act
    clientBot.sendGroupchat(roomJID, new Operations.Cancel());
    final ExpectedMessage[] notReceivedMessages = adminBot.tryReceiveMessages(new StateLatch(), cancel, roomStateChanged);

    //Assert
    assertAllExpectedMessagesAreReceived(notReceivedMessages);
  }

  protected void roomCloseStateByClientFeedback(BareJID roomJID, ClientBot clientBot, AdminBot adminBot) throws JaxmppException {
    //Arrange
    final Operations.Feedback feedback = new Operations.Feedback(generateRandomInt(1, 5));
    final ExpectedMessage roomStateChanged = new ExpectedMessageBuilder()
        .from(groupChatJID(roomJID))
        .has(Operations.RoomStateChanged.class, rsc -> RoomState.CLOSED.equals(rsc.state()))
        .build();
    final ExpectedMessage expectedFeedback = new ExpectedMessageBuilder().from(botRoomJID(roomJID, clientBot)).has(Operations.Feedback.class, f -> feedback.stars() == f.stars()).build();

    //Act
    clientBot.sendGroupchat(roomJID, feedback);
    final ExpectedMessage[] notReceivedMessages = adminBot.tryReceiveMessages(new StateLatch(), roomStateChanged, expectedFeedback);

    //Assert
    assertAllExpectedMessagesAreReceived(notReceivedMessages);
  }

  protected BareJID obtainRoomOpenState(ClientBot clientBot) throws JaxmppException {
    //Arrange
    final BareJID roomJID = generateRoomJID(clientBot);
    final Offer offer = new Offer(
        JID.parse(clientBot.jid().toString()),
        generateRandomString(),
        Offer.Urgency.ASAP, new Offer.Location(59.98062295379115, 30.32538469883643),
        System.currentTimeMillis() / 1000.);
    final Message.Body message = new Message.Body(generateRandomString());
    final ExpectedMessage expectedMessage = new ExpectedMessageBuilder().has(Message.Body.class, body -> message.value().equals(body.value())).build();

    //Act
    clientBot.send(roomJID, offer);
    clientBot.sendGroupchat(clientBot.jid(), message);
    clientBot.tryReceiveMessages(new StateLatch(), expectedMessage);

    return roomJID;
  }

  protected BareJID obtainRoomOpenState(ClientBot clientBot, AdminBot adminBot) throws JaxmppException {
    //Arrange
    final BareJID roomJID = generateRoomJID(clientBot);
    final Offer offer = new Offer(
        JID.parse(clientBot.jid().toString()),
        generateRandomString(),
        Offer.Urgency.ASAP, new Offer.Location(59.98062295379115, 30.32538469883643),
        System.currentTimeMillis() / 1000.);
    final Image image = new Image(generateRandomString());
    offer.attach(image);

    final ExpectedMessageBuilder roomRoleUpdateNone = new ExpectedMessageBuilder()
        .has(Operations.RoomRoleUpdate.class, rru -> Role.NONE.equals(rru.role()) && Affiliation.OWNER.equals(rru.affiliation()));
    final ExpectedMessageBuilder roomRoleUpdateModer = new ExpectedMessageBuilder()
        .has(Operations.RoomRoleUpdate.class, rru -> Role.MODERATOR.equals(rru.role()) && Affiliation.OWNER.equals(rru.affiliation()));
    final ExpectedMessageBuilder roomStateChanged = new ExpectedMessageBuilder()
        .has(Operations.RoomStateChanged.class, rsc -> RoomState.OPEN.equals(rsc.state()));
    final ExpectedMessageBuilder expectedOffer = new ExpectedMessageBuilder()
        .has(Offer.class, o -> o.location() != null
            && offer.location().longitude() == o.location().longitude()
            && offer.location().latitude() == o.location().latitude()
            && Arrays.stream(o.attachments()).anyMatch(a -> a instanceof Image && image.url().equals(((Image) a).url()))
            && offer.topic().equals(o.topic())
            && Offer.Urgency.ASAP.equals(o.urgency())
            && Double.compare(offer.started(), o.started()) == 0
        )
        .has(Operations.OfferChange.class);

    //Act
    clientBot.send(roomJID, offer);
    final ExpectedMessage[] notReceivedMessages = adminBot.tryReceiveMessages(new StateLatch(),
        roomRoleUpdateNone.from(groupChatJID(roomJID)).build(),
        roomRoleUpdateModer.from(groupChatJID(roomJID)).build(),
        roomStateChanged.from(groupChatJID(roomJID)).build(),
        expectedOffer.from(groupChatJID(roomJID)).build());

    //Assert
    assertAllExpectedMessagesAreReceived(notReceivedMessages);

    return roomJID;
  }

  @NotNull
  private BareJID generateRoomJID(ClientBot clientBot) {
    return BareJID.bareJIDInstance(clientBot.jid().getLocalpart() + "-room-" + (int) (System.nanoTime() / 1000), "muc." + clientBot.jid().getDomain());
  }

  protected BareJID obtainRoomWorkState(ClientBot clientBot, AdminBot adminBot, ExpertBot expertBot) throws JaxmppException {
    final BareJID roomJID = obtainRoomOpenState(clientBot, adminBot);
    { //obtain work state
      //Arrange
      final Offer offer = new Offer(JID.parse(roomJID.toString()));
      final ExpectedMessage offerCheck = new ExpectedMessageBuilder().from(domainJID()).has(Offer.class).has(Operations.Check.class).build();

      //Act
      adminBot.send(roomJID, offer);
      final ExpectedMessage[] notReceivedMessages = expertBot.tryReceiveMessages(new StateLatch(), offerCheck);

      //Assert
      assertAllExpectedMessagesAreReceived(notReceivedMessages);
    }
    return roomJID;
  }

  protected BareJID obtainRoomDeliverState(ClientBot clientBot, AdminBot adminBot, ExpertBot expertBot) throws JaxmppException {
    final BareJID roomJID = obtainRoomWorkState(clientBot, adminBot, expertBot);
    { //obtain deliver state
      //Arrange
      final ExpectedMessage invite = new ExpectedMessageBuilder().from(roomJID).has(Offer.class).has(Operations.Invite.class).build();
      final ExpectedMessage startAndExpert = new ExpectedMessageBuilder().from(roomJID).has(Operations.Start.class).has(ExpertsProfile.class).build();

      //Act
      expertBot.sendGroupchat(roomJID, new Operations.Ok());
      final ExpectedMessage[] notReceivedByExpert = expertBot.tryReceiveMessages(new StateLatch(), invite);
      //Assert
      assertAllExpectedMessagesAreReceived(notReceivedByExpert);

      //Act
      expertBot.sendGroupchat(roomJID, new Operations.Start());
      final ExpectedMessage[] notReceivedByClient = clientBot.tryReceiveMessages(new StateLatch(), startAndExpert);
      //Assert
      assertAllExpectedMessagesAreReceived(notReceivedByClient);
    }
    return roomJID;
  }

  protected BareJID obtainRoomFeedbackState(ClientBot clientBot, AdminBot adminBot, ExpertBot expertBot) throws JaxmppException {
    final BareJID roomJID = obtainRoomDeliverState(clientBot, adminBot, expertBot);
    { //obtain feedback state
      //Arrange
      final Answer answer = new Answer(generateRandomString());
      final ExpectedMessage expectedAnswer = new ExpectedMessageBuilder().from(botRoomJID(roomJID, expertBot)).has(Answer.class, a -> answer.value().equals(a.value())).build();

      //Act
      expertBot.sendGroupchat(roomJID, answer);
      final ExpectedMessage[] notReceivedMessages = clientBot.tryReceiveMessages(new StateLatch(), expectedAnswer);

      //Assert
      assertAllExpectedMessagesAreReceived(notReceivedMessages);
    }
    return roomJID;
  }
}
