package eu.cloudnetservice.cloudnet.v2.master.util.defaults;

import eu.cloudnetservice.cloudnet.v2.lib.server.ServerGroup;
import eu.cloudnetservice.cloudnet.v2.lib.server.ServerGroupMode;
import eu.cloudnetservice.cloudnet.v2.lib.server.ServerGroupType;
import eu.cloudnetservice.cloudnet.v2.lib.server.advanced.AdvancedServerConfig;
import eu.cloudnetservice.cloudnet.v2.lib.server.template.Template;
import eu.cloudnetservice.cloudnet.v2.lib.server.template.TemplateResource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public final class LobbyGroup extends ServerGroup {

    public static final String[] EMPTY_STRING_ARRAY = {};

    public LobbyGroup() {
        super("Lobby",
              Arrays.asList("Wrapper-1"),
              true,
              356,
              512,
              false,
              0,
              1,
              0,
              300,
              100,
              100,
              50,
              ServerGroupType.BUKKIT,
              ServerGroupMode.LOBBY,
              new Template("globaltemplate", TemplateResource.LOCAL, null, EMPTY_STRING_ARRAY, Collections.emptyList()),
              Collections.singletonList(new Template("default", TemplateResource.LOCAL, null, EMPTY_STRING_ARRAY, new ArrayList<>())),
              new AdvancedServerConfig(true, true, true, true));
    }
}