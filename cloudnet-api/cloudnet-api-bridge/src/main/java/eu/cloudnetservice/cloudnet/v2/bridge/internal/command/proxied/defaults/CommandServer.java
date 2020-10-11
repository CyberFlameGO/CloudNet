/*
 * Copyright 2017 Tarek Hosni El Alaoui
 * Copyright 2020 CloudNetService
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.cloudnet.v2.bridge.internal.command.proxied.defaults;

import eu.cloudnetservice.cloudnet.v2.api.CloudAPI;
import eu.cloudnetservice.cloudnet.v2.bridge.CloudProxy;
import eu.cloudnetservice.cloudnet.v2.lib.server.info.ServerInfo;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Map;

public class CommandServer extends Command {
    public CommandServer() {
        super("server", "bungeecord.command.server");
    }

    @Override
    public void execute(final CommandSender sender, final String[] args) {
        final Map<String, ServerInfo> servers = CloudProxy.getInstance().getServers();
        if ( args.length == 0 ) {
            if ( sender instanceof ProxiedPlayer ) {
                ComponentBuilder builder = new ComponentBuilder()
                    .appendLegacy(CloudAPI.getInstance().getPrefix())
                    .appendLegacy("&eCurrent Server: &r " + ( (ProxiedPlayer) sender ).getServer().getInfo().getName());
                sender.sendMessage(ProxyServer.getInstance().getTranslation("current_server", ( (ProxiedPlayer) sender ).getServer().getInfo().getName() ) );
            }
        }
    }
}
