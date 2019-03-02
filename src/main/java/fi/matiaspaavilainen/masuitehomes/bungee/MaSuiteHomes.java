package fi.matiaspaavilainen.masuitehomes.bungee;

import fi.matiaspaavilainen.masuitecore.bungee.Utils;
import fi.matiaspaavilainen.masuitecore.core.Updator;
import fi.matiaspaavilainen.masuitecore.core.channels.BungeePluginChannel;
import fi.matiaspaavilainen.masuitecore.core.configuration.BungeeConfiguration;
import fi.matiaspaavilainen.masuitecore.core.database.ConnectionManager;
import fi.matiaspaavilainen.masuitecore.core.objects.Location;
import fi.matiaspaavilainen.masuitehomes.bungee.commands.DeleteCommand;
import fi.matiaspaavilainen.masuitehomes.bungee.commands.ListCommand;
import fi.matiaspaavilainen.masuitehomes.bungee.commands.SetCommand;
import fi.matiaspaavilainen.masuitehomes.bungee.commands.TeleportCommand;
import fi.matiaspaavilainen.masuitehomes.core.Home;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

public class MaSuiteHomes extends Plugin implements Listener {

    private Utils utils = new Utils();

    @Override
    public void onEnable() {
        //Configs
        BungeeConfiguration config = new BungeeConfiguration();
        config.create(this, "homes", "messages.yml");
        getProxy().getPluginManager().registerListener(this, this);

        //Commands
        ConnectionManager.db.createTable("homes", "(id INT(10) unsigned NOT NULL PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100) NOT NULL, owner VARCHAR(36) NOT NULL, server VARCHAR(100) NOT NULL, world VARCHAR(100) NOT NULL, x DOUBLE, y DOUBLE, z DOUBLE, yaw FLOAT, pitch FLOAT) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

        // Check updates
        new Updator(new String[]{getDescription().getVersion(), getDescription().getName(), "60632"}).checkUpdates();

        config.addDefault("homes/messages.yml", "homes.title-others", "&9%player%''s &7homes: ");
        config.addDefault("homes/messages.yml", "homes.server-name", "&9%server%&7: ");
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) throws IOException {
        if (!e.getTag().equals("BungeeCord")) {
            return;
        }
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(e.getData()));
        String subchannel = in.readUTF();
        if (subchannel.equals("HomeCommand")) {
            TeleportCommand teleport = new TeleportCommand(this);
            ProxiedPlayer p = getProxy().getPlayer(in.readUTF());
            if (utils.isOnline(p)) {
                teleport.teleport(p, in.readUTF());
            }

        }
        if (subchannel.equals("HomeOtherCommand")) {
            TeleportCommand teleport = new TeleportCommand(this);
            ProxiedPlayer p = getProxy().getPlayer(in.readUTF());
            if (utils.isOnline(p)) {
                teleport.teleport(p, in.readUTF(), in.readUTF());
            }
        }
        if (subchannel.equals("SetHomeCommand")) {
            ProxiedPlayer p = getProxy().getPlayer(in.readUTF());
            if (utils.isOnline(p)) {
                SetCommand set = new SetCommand(this);
                String[] location = in.readUTF().split(":");
                set.set(p, in.readUTF(), in.readInt(), new Location(location[0], Double.parseDouble(location[1]), Double.parseDouble(location[2]), Double.parseDouble(location[3]), Float.parseFloat(location[4]), Float.parseFloat(location[5])));
            }
        }

        if (subchannel.equals("SetHomeOtherCommand")) {
            ProxiedPlayer p = getProxy().getPlayer(in.readUTF());
            String player = in.readUTF();
            if (utils.isOnline(p)) {
                SetCommand set = new SetCommand(this);
                String[] location = in.readUTF().split(":");
                set.set(p, player, in.readUTF(), in.readInt(), new Location(location[0], Double.parseDouble(location[1]), Double.parseDouble(location[2]), Double.parseDouble(location[3]), Float.parseFloat(location[4]), Float.parseFloat(location[5])));
            }
        }

        if (subchannel.equals("DelHomeCommand")) {
            ProxiedPlayer p = getProxy().getPlayer(in.readUTF());
            if (utils.isOnline(p)) {
                DeleteCommand delete = new DeleteCommand(this);
                delete.delete(p, in.readUTF());
            }
        }

        if (subchannel.equals("DelHomeOtherCommand")) {
            ProxiedPlayer p = getProxy().getPlayer(in.readUTF());
            if (utils.isOnline(p)) {
                DeleteCommand delete = new DeleteCommand(this);
                delete.delete(p, in.readUTF(), in.readUTF());
            }
        }

        if (subchannel.equals("ListHomeCommand")) {
            ProxiedPlayer p = getProxy().getPlayer(in.readUTF());
            if (utils.isOnline(p)) {
                ListCommand list = new ListCommand();
                list.list(p);
            }
        }

        if (subchannel.equals("ListHomeOtherCommand")) {
            ProxiedPlayer p = getProxy().getPlayer(in.readUTF());
            if (utils.isOnline(p)) {
                ListCommand list = new ListCommand();
                list.list(p, in.readUTF());
            }
        }

        if (subchannel.equals("ListHomes")) {
            listHomes(getProxy().getPlayer(in.readUTF()));
        }

        if (subchannel.equals("ReadyListHomes")) {
            readyTolist(getProxy().getPlayer(in.readUTF()));
        }
    }

    public void sendCooldown(ProxiedPlayer p, Home home) {
        BungeePluginChannel bpc = new BungeePluginChannel(this, p.getServer().getInfo(),
                new Object[]{"HomeCooldown", p.getUniqueId().toString(), System.currentTimeMillis()});
        if (!getProxy().getServerInfo(home.getServer()).getName().equals(p.getServer().getInfo().getName())) {
            getProxy().getScheduler().schedule(this, bpc::send, 500, TimeUnit.MILLISECONDS);
        } else {
            bpc.send();
        }

    }

    public void listHomes(ProxiedPlayer p) {
        if (utils.isOnline(p)) {
            new BungeePluginChannel(this, p.getServer().getInfo(), new Object[]{
                    "ResetHomes",
                    p.getUniqueId().toString()
            }).send();
        }
    }

    public void readyTolist(ProxiedPlayer p) {
        for (Home home : new Home().getHomes(p.getUniqueId())) {
            StringJoiner info = new StringJoiner(":");
            Location loc = home.getLocation();
            info.add(home.getName())
                    .add(home.getServer())
                    .add(loc.getWorld())
                    .add(loc.getX().toString())
                    .add(loc.getY().toString())
                    .add(loc.getZ().toString());

            new BungeePluginChannel(this, p.getServer().getInfo(), new Object[]{
                    "AddHome",
                    p.getUniqueId().toString(),
                    info.toString()
            }).send();
        }
    }
}
