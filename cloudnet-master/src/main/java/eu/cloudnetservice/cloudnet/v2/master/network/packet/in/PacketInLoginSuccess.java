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

package eu.cloudnetservice.cloudnet.v2.master.network.packet.in;

import eu.cloudnetservice.cloudnet.v2.lib.network.protocol.packet.Packet;
import eu.cloudnetservice.cloudnet.v2.lib.network.protocol.packet.PacketInHandler;
import eu.cloudnetservice.cloudnet.v2.lib.network.protocol.packet.PacketSender;
import eu.cloudnetservice.cloudnet.v2.lib.player.CloudPlayer;
import eu.cloudnetservice.cloudnet.v2.master.CloudNet;

import java.util.UUID;

public class PacketInLoginSuccess implements PacketInHandler {

    public void handleInput(Packet packet, PacketSender packetSender) {
        UUID unique = packet.getData().getObject("uniqueId", UUID.class);
        CloudPlayer cloudPlayer = CloudNet.getInstance().getNetworkManager().getWaitingPlayers().get(unique);
        if (cloudPlayer != null) {
            CloudNet.getInstance().getNetworkManager().getWaitingPlayers().remove(unique);
            CloudNet.getInstance().getNetworkManager().getOnlinePlayers().put(cloudPlayer.getUniqueId(), cloudPlayer);
        }
    }
}
