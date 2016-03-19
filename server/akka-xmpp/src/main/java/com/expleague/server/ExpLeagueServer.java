package com.expleague.server;

import akka.actor.ActorSystem;
import akka.actor.Props;
import com.expleague.server.admin.ExpLeagueAdminService;
import com.expleague.server.agents.LaborExchange;
import com.expleague.server.agents.XMPP;
import com.expleague.server.dao.Archive;
import com.expleague.server.services.XMPPServices;
import com.expleague.util.ios.NotificationsManager;
import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.duration.FiniteDuration;

import java.io.FileInputStream;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

/**
 * User: solar
 * Date: 24.11.15
 * Time: 17:42
 */
@SuppressWarnings("unused")
public class ExpLeagueServer {
//  private static final Logger log = Logger.getLogger(ExpLeagueServer.class.getName());
  private static Cfg config;
  private static Roster users;
  private static LaborExchange.Board leBoard;
  private static Archive archive;

  public static void main(String[] args) throws Exception {
    final Config load = ConfigFactory.load();
    setConfig(new ServerCfg(load));

    final ActorSystem system = ActorSystem.create("ExpLeague", load);

    NotificationsManager.instance();
    // singletons
    system.actorOf(Props.create(XMPP.class), "xmpp");
    system.actorOf(Props.create(LaborExchange.class), "labor-exchange");

    // per node
    system.actorOf(Props.create(XMPPServices.class), "services");
    system.actorOf(Props.create(XMPPServer.class), "comm");
    system.actorOf(Props.create(BOSHServer.class), "bosh");
    system.actorOf(Props.create(ImageStorage.class), "image-storage");
    system.actorOf(Props.create(ExpLeagueAdminService.class, load.getConfig("tbts.admin.embedded")), "admin-service");
  }

  public static Roster roster() {
    return users;
  }
  public static LaborExchange.Board board() {
    return leBoard;
  }
  public static Archive archive() {
    return archive;
  }

  public static Cfg config() {
    return config;
  }

  @VisibleForTesting
  static void setConfig(final Cfg cfg) throws Exception {
    config = cfg;
    users = config.roster().newInstance();
    leBoard = config.board().newInstance();
    archive = config.archive().newInstance();
    if (System.getProperty("logger.config") == null)
      LogManager.getLogManager().readConfiguration(ExpLeagueServer.class.getResourceAsStream("/logging.properties"));
    else
      LogManager.getLogManager().readConfiguration(new FileInputStream(System.getProperty("logger.config")));
  }

  public interface Cfg {
    ServerCfg.Type type();

    String domain();

    String db();

    Class<? extends Archive> archive();

    Class<? extends Roster> roster();

    Class<? extends LaborExchange.Board> board();

    Config config();

    default FiniteDuration timeout(final String name) {
      final Config config = config().getConfig(name);
      if (config == null) {
        throw new IllegalArgumentException("No timeout configured for: " + name);
      }
      return FiniteDuration.create(
        config.getLong("length"),
        TimeUnit.valueOf(config.getString("unit"))
      );
    }

    default TimeUnit timeUnit(final String name) {
      return TimeUnit.valueOf(config().getString(name + ".unit"));
    }

    enum Type {
      PRODUCTION,
      TEST
    }
  }

  public static class ServerCfg implements Cfg {
    private final Config config;

    private final String db;
    private final String domain;
    private final Class<? extends Archive> archive;
    private final Class<? extends Roster> roster;
    private final Class<? extends LaborExchange.Board> board;
    private final Type type;

    public ServerCfg(Config load) throws ClassNotFoundException {
      config = load.getConfig("tbts");
      db = config.getString("db");
      domain = config.getString("domain");
      //noinspection unchecked
      archive = (Class<? extends Archive>) Class.forName(config.getString("archive"));
      //noinspection unchecked
      board = (Class<? extends LaborExchange.Board>) Class.forName(config.getString("board"));
      //noinspection unchecked
      roster = (Class<? extends Roster>) Class.forName(config.getString("roster"));
      type = Type.valueOf(config.getString("type").toUpperCase());
    }

    @Override
    public Config config() {
      return config;
    }

    @Override
    public String domain() {
      return domain;
    }

    @Override
    public String db() {
      return db;
    }

    @Override
    public Class<? extends Archive> archive() {
      return archive;
    }

    @Override
    public Class<? extends Roster> roster() {
      return roster;
    }

    @Override
    public Type type() {
      return type;
    }

    @Override
    public Class<? extends LaborExchange.Board> board() {
      return board;
    }
  }
}
