package com.expleague.server.dao.sql;

import com.expleague.model.Offer;
import com.expleague.server.ExpLeagueServer;
import com.expleague.server.Roster;
import com.expleague.server.agents.ExpLeagueOrder;
import com.expleague.server.agents.ExpLeagueRoomAgent;
import com.expleague.server.agents.LaborExchange;
import com.expleague.server.agents.XMPP;
import com.expleague.server.dao.fake.InMemBoard;
import com.expleague.xmpp.JID;
import com.spbsu.commons.io.StreamTools;
import com.spbsu.commons.util.cache.CacheStrategy;
import com.spbsu.commons.util.cache.impl.FixedSizeCache;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.*;

/**
 * Experts League
 * Created by solar on 05/03/16.
 */
@SuppressWarnings("unused")
public class MySQLBoard extends MySQLOps implements LaborExchange.Board {
  public MySQLBoard() {
    super(ExpLeagueServer.config().db());
  }

  private final FixedSizeCache<String, ExpLeagueOrder> ordersCache = new FixedSizeCache<>(1000, CacheStrategy.Type.LRU);
  @Override
  public MySQLOrder register(Offer offer) {
    try {
      final PreparedStatement registerOrder = createStatement("register-order", "INSERT INTO expleague.Orders SET room = ?, offer = ?, eta = ?, status = " + ExpLeagueOrder.Status.OPEN.index(), true);
      registerOrder.setString(1, offer.room().local());
      registerOrder.setCharacterStream(2, new StringReader(offer.xmlString()));
      registerOrder.setTimestamp(3, Timestamp.from(offer.expires().toInstant()));
      registerOrder.execute();
      final MySQLOrder result;
      try (final ResultSet generatedKeys = registerOrder.getGeneratedKeys()) {
        generatedKeys.next();
        final int id = generatedKeys.getInt(1);
        result = new MySQLOrder(id, offer);
      }
      result.role(offer.client(), ExpLeagueOrder.Role.OWNER);
      ordersCache.put(offer.room().local(), result);
      return result;
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  @Override
  public ExpLeagueOrder active(String roomId) {
    return ordersCache.get(roomId, id -> {
      try {
        final PreparedStatement lookForActive = createStatement("active-order", "SELECT * FROM expleague.Orders WHERE room = ? AND status < " + ExpLeagueOrder.Status.DONE.index());
        lookForActive.setString(1, id);
        try (final ResultSet resultSet = lookForActive.executeQuery()) {
          if (resultSet.next())
            return new MySQLOrder(resultSet);
        }
        final PreparedStatement countOrders = createStatement("count-orders", "SELECT COUNT(*) FROM expleague.Orders WHERE room = ?");
        countOrders.setString(1, id);
        final long ordersCount;
        try (final ResultSet countRS = countOrders.executeQuery()){
          countRS.next();
          ordersCount = countRS.getLong(1);
        }
        if (ordersCount == 0) {
          final ExpLeagueOrder[] replay = ExpLeagueRoomAgent.replay(this, roomId);
          if (replay.length > 0 && replay[replay.length - 1].status() != ExpLeagueOrder.Status.DONE)
            return replay[replay.length - 1];
        }
        return null;
      }
      catch (SQLException | IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  public ExpLeagueOrder[] history(String roomId) {
    try {
      final PreparedStatement lookForActive = createStatement("orders-room", "SELECT * FROM expleague.Orders WHERE room = ?");
      lookForActive.setString(1, roomId);
      final List<ExpLeagueOrder> result = new ArrayList<>();
      int count = 0;
      try (final ResultSet resultSet = lookForActive.executeQuery()) {
        while (resultSet.next()) {
          result.add(new MySQLOrder(resultSet));
          count++;
        }
      }
      return result.toArray(new ExpLeagueOrder[result.size()]);
    }
    catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Stream<ExpLeagueOrder> related(JID jid) {
    return stream("related-orders", "SELECT Orders.* " +
        "FROM expleague.Orders INNER JOIN expleague.Participants ON expleague.Orders.id = expleague.Participants.`order` " +
        "WHERE Participants.partisipant = ?", stmt -> stmt.setString(1, jid.local()))
        .map(rs -> {
          try {
            return new MySQLOrder(rs) {
              @Override
              public State state() {
                throw new IllegalStateException("Related orders are read-only!");
              }
            };
          }
          catch (SQLException | IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Override
  public Stream<ExpLeagueOrder> open() {
    return stream("active-orders", "SELECT Orders.* FROM expleague.Orders WHERE status < " + ExpLeagueOrder.Status.DONE.index(), stmt -> {})
        .map(rs -> {
          try {
            final MySQLOrder order = new MySQLOrder(rs);
            ordersCache.put(order.room().local(), order);
            return (ExpLeagueOrder)order;
          }
          catch (SQLException | IOException e) {
            throw new RuntimeException(e);
          }
        }).filter(o -> o != null);
  }

  @Override
  public Stream<JID> topExperts() {
    return stream("top-experts", "SELECT U.id, count(*) FROM expleague.Users AS U " +
        "JOIN expleague.Participants AS P ON U.id = P.partisipant " +
        "JOIN expleague.Orders AS O ON P.`order` = O.id " +
        "WHERE P.role = " + ExpLeagueOrder.Role.ACTIVE.index() + " GROUP BY U.id", stmt -> {}).map(rs -> {
      try {
        return XMPP.jid(rs.getString(1));
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  public Stream<String> tags() {
    tags = new TObjectIntHashMap<>();
    stream("all-tags", "SELECT * FROM expleague.Tags", q -> {}).forEach(rs -> {
      try {
        tags.put(rs.getString(2), rs.getInt(1));
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });

    return tags.keySet().stream();
  }

  private TObjectIntHashMap<String> tags;
  private int tag(String tag) {
    if (tags == null || !tags.containsKey(tag))
      tags();
    if (tags.containsKey(tag))
      return tags.get(tag);
    final PreparedStatement statement = createStatement("add-tag", "INSERT expleague.Tags SET tag = ?", true);
    try {
      statement.setString(1, tag);
      statement.executeUpdate();
      try (final ResultSet gk = statement.getGeneratedKeys()) {
        gk.next();
        final int tagId = gk.getInt(1);
        tags.put(tag, tagId);
        return tagId;
      }
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


  private class MySQLOrder extends InMemBoard.MyOrder {
    private final int id;

    public MySQLOrder(int id, Offer offer) {
      super(offer);
      this.id = id;
    }

    public MySQLOrder(ResultSet resultSet) throws SQLException, IOException {
      super((Offer)Offer.create(StreamTools.readReader(resultSet.getCharacterStream(3))));
      id = resultSet.getInt(1);
      super.status(Status.valueOf((int)resultSet.getByte(5)));
      super.feedback(resultSet.getDouble(6));
      final PreparedStatement restoreRoles = createStatement("roles-restore", "SELECT * FROM expleague.Participants WHERE `order` = ? ORDER BY id");
      restoreRoles.setInt(1, id);
      try (final ResultSet rolesRS = restoreRoles.executeQuery()) {
        while (rolesRS.next()) {
          super.role(XMPP.jid(rolesRS.getString(3)), Role.valueOf(rolesRS.getByte(4)));
        }
      }
    }

    @Override
    protected void status(Status status) {
      try {
        super.status(status);
        final PreparedStatement feedback = createStatement("status-update", "UPDATE expleague.Orders SET status = ? WHERE id = ?");
        feedback.setInt(2, id);
        feedback.setInt(1, status.index());
        feedback.execute();
      }
      catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void feedback(double stars) {
      try {
        super.feedback(stars);
        final PreparedStatement feedback = createStatement("feedback-role", "UPDATE expleague.Orders SET score = ? WHERE id = ?");
        feedback.setInt(2, id);
        feedback.setDouble(1, stars);
        feedback.execute();
      }
      catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    boolean tagsAcquired = false;
    @Override
    public String[] tags() {
      if (!tagsAcquired) {
        tagsAcquired = true;
        super.tags.addAll(stream("order-topics", "SELECT Tags.tag FROM expleague.Topics JOIN expleague.Tags ON Topics.tag = Tags.id WHERE `order` = ?",
            stmt -> stmt.setInt(1, id)
        ).map(rs -> {
          try {
            return rs.getString(1);
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
        }).collect(Collectors.toSet()));
      }
      return super.tags();
    }

    @Override
    protected void role(JID jid, Role role) {
      super.role(jid, role);
      if (role.permanent()) {
        try {

          final PreparedStatement changeRole = createStatement("change-role", "INSERT INTO expleague.Participants SET `order` = ?, partisipant = ?, role = ?");
          changeRole.setInt(1, id);
          changeRole.setString(2, jid.local());
          changeRole.setByte(3, (byte) role.index());
          changeRole.execute();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }
    }

    @Override
    protected void tag(String tag) {
      try {
        super.tag(tag);
        final int tagId = MySQLBoard.this.tag(tag);
        final PreparedStatement changeRole = createStatement("append-topic", "INSERT INTO expleague.Topics SET `order` = ?, tag = ?");
        changeRole.setInt(1, id);
        changeRole.setInt(2, tagId);
        changeRole.execute();
      }
      catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    protected void untag(String tag) {
      try {
        super.tag(tag);
        final int tagId = MySQLBoard.this.tag(tag);
        final PreparedStatement changeRole = createStatement("remove-topic", "DELETE FROM expleague.Topics WHERE `order` = ? AND tag = ?");
        changeRole.setInt(1, id);
        changeRole.setInt(2, tagId);
        changeRole.execute();
      }
      catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
