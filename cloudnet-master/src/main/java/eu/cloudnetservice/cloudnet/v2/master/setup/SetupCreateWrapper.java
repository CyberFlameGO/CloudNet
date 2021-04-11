package eu.cloudnetservice.cloudnet.v2.master.setup;

import eu.cloudnetservice.cloudnet.v2.command.CommandManager;
import eu.cloudnetservice.cloudnet.v2.command.CommandSender;
import eu.cloudnetservice.cloudnet.v2.lib.ConnectableAddress;
import eu.cloudnetservice.cloudnet.v2.master.CloudNet;
import eu.cloudnetservice.cloudnet.v2.master.network.components.WrapperMeta;
import eu.cloudnetservice.cloudnet.v2.setup.Setup;
import eu.cloudnetservice.cloudnet.v2.setup.SetupRequest;
import eu.cloudnetservice.cloudnet.v2.setup.responsetype.IntegerResponseType;
import eu.cloudnetservice.cloudnet.v2.setup.responsetype.StringResponseType;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.apache.commons.validator.routines.InetAddressValidator;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class SetupCreateWrapper extends Setup {

    private static final Pattern IPV4_PATTERN = Pattern.compile("(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])");

    private static final Pattern IPV6_STD_PATTERN = Pattern.compile("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");

    private static final Pattern IPV6_HEX_COMPRESSED_PATTERN = Pattern.compile("^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)$");


    public SetupCreateWrapper(CommandSender sender) {
        super(CloudNet.getInstance().getConsoleManager());
        this.setupCancel(() -> {
            System.out.println("Setup was cancelled");
            CloudNet.getInstance().getConsoleManager().changeConsoleInput(CommandManager.class);
            CloudNet.getInstance().getConsoleRegistry().unregisterInput(SetupWrapper.class);
        })
            .setupComplete(data -> {
                String host = data.getString("listingIp");
                Integer memory = data.getInt("memory");
                Integer queue = data.getInt("queue");
                String wrapperId = data.getString("wrapperId");
                String user = data.getString("user");
                try {
                    InetAddressValidator validator = new InetAddressValidator();
                    if (!validator.isValid(host)) {
                        throw new UnknownHostException("No valid InetAddress found!");
                    }
                    InetAddress hostInet = InetAddress.getByName(host);
                    WrapperMeta wrapperMeta = new WrapperMeta(wrapperId, hostInet, user);
                    CloudNet.getInstance().getConfig().createWrapper(wrapperMeta);
                    sender.sendMessage(String.format("Wrapper [%s] was registered on CloudNet",
                                                     wrapperMeta.getId()));
                    Configuration configuration = new Configuration();
                    configuration.set("connection.cloudnet-host",
                                      CloudNet.getInstance()
                                              .getConfig()
                                              .getAddresses()
                                              .toArray(new ConnectableAddress[0])[0].getHostName());
                    configuration.set("connection.cloudnet-port", 1410);
                    configuration.set("connection.cloudnet-web", 1420);
                    configuration.set("general.wrapperId", wrapperId);
                    configuration.set("general.internalIp", host);
                    configuration.set("general.proxy-config-host", host);
                    configuration.set("general.max-memory", memory);
                    configuration.set("general.startPort", 41570);
                    configuration.set("general.auto-update", false);
                    configuration.set("general.saving-records", false);
                    configuration.set("general.maintenance-copyFileToDirectory", false);
                    configuration.set("general.processQueueSize", queue);
                    configuration.set("general.percentOfCPUForANewServer", 100D);
                    configuration.set("general.percentOfCPUForANewProxy", 100D);
                    try {
                        Files.createDirectories(Paths.get("local", "wrapper", wrapperId));
                        Files.copy(Paths.get("WRAPPER_KEY.cnd"), Paths.get("local", "wrapper", wrapperId, "WRAPPER_KEY.cnd"));
                    } catch (IOException e) {
                        throw new RuntimeException("Wrapper Key path " + Paths.get("local", "wrapper", wrapperId, "WRAPPER_KEY.cnd")
                                                                              .toAbsolutePath()
                                                                              .toString() + " could not be created", e);
                    }
                    try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(Paths.get("local",
                                                                                                                        "wrapper",
                                                                                                                        wrapperId,
                                                                                                                        "config.yml")),
                                                                                        StandardCharsets.UTF_8)) {

                        ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, outputStreamWriter);
                    } catch (IOException e) {
                        throw new RuntimeException("Wrapper config path " + Paths.get("local", "wrapper", wrapperId, "config.yml")
                                                                                 .toAbsolutePath()
                                                                                 .toString() + " could not be saved", e);
                    }
                    sender.sendMessage(String.format("Wrapper [%s] config is generated under -> %s",
                                                     wrapperMeta.getId(), "local/wrapper/" + wrapperId));

                    sender.sendMessage("WRAPPER_KEY.cnd is also copied to local/wrapper/" + wrapperId);
                    CloudNet.getInstance().getConsoleManager().changeConsoleInput(CommandManager.class);
                    CloudNet.getInstance().getConsoleRegistry().unregisterInput(SetupWrapper.class);
                } catch (UnknownHostException e) {
                    CloudNet.getLogger().log(Level.SEVERE, "Error create a new wrapper", e);
                }

            })

            .request(new SetupRequest("wrapperId",
                                      "What is the ID of this wrapper?",
                                      "",
                                      StringResponseType.getInstance(),
                                      key -> true))
            .request(new SetupRequest("listingIp",
                                      "What is the listing address of the wrapper for proxies and servers?",
                                      "The specified IP address is invalid!",
                                      StringResponseType.getInstance(),
                                      key -> IPV4_PATTERN.matcher(key).matches()
                                          || IPV6_STD_PATTERN.matcher(key).matches()
                                          || IPV6_HEX_COMPRESSED_PATTERN.matcher(key).matches()))
            .request(new SetupRequest("user",
                                      "What is the user of the wrapper?",
                                      "The specified user does not exist!",
                                      StringResponseType.getInstance(),
                                      key -> CloudNet.getInstance().getUser(key) != null))
            .request(new SetupRequest("memory",
                                      "How many memory is allowed to use for the wrapper services?",
                                      "No negative numbers or zero allowed",
                                      IntegerResponseType.getInstance(),
                                      key -> Integer.parseInt(key) <= 0))
            .request(new SetupRequest("queue",
                                      "How large should the server queue be?",
                                      "No negative numbers or zero allowed",
                                      IntegerResponseType.getInstance(),
                                      key -> Integer.parseInt(key) <= 0));
    }
}
