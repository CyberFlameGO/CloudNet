package eu.cloudnetservice.cloudnet.v2.master.network.components;

import eu.cloudnetservice.cloudnet.v2.lib.DefaultType;
import eu.cloudnetservice.cloudnet.v2.lib.network.WrapperExternal;
import eu.cloudnetservice.cloudnet.v2.lib.network.WrapperInfo;
import eu.cloudnetservice.cloudnet.v2.lib.server.ProxyGroup;
import eu.cloudnetservice.cloudnet.v2.lib.server.ProxyProcessMeta;
import eu.cloudnetservice.cloudnet.v2.lib.server.ServerGroup;
import eu.cloudnetservice.cloudnet.v2.lib.server.ServerProcessMeta;
import eu.cloudnetservice.cloudnet.v2.lib.server.info.ProxyInfo;
import eu.cloudnetservice.cloudnet.v2.lib.server.info.ServerInfo;
import eu.cloudnetservice.cloudnet.v2.lib.server.template.Template;
import eu.cloudnetservice.cloudnet.v2.lib.user.SimpledUser;
import eu.cloudnetservice.cloudnet.v2.lib.user.User;
import eu.cloudnetservice.cloudnet.v2.master.CloudNet;
import eu.cloudnetservice.cloudnet.v2.master.network.packet.out.PacketOutCopyServer;
import eu.cloudnetservice.cloudnet.v2.master.network.packet.out.PacketOutCreateTemplate;
import eu.cloudnetservice.cloudnet.v2.master.network.packet.out.PacketOutExecuteCommand;
import eu.cloudnetservice.cloudnet.v2.master.network.packet.out.PacketOutExecuteServerCommand;
import eu.cloudnetservice.cloudnet.v2.master.network.packet.out.PacketOutScreen;
import eu.cloudnetservice.cloudnet.v2.master.network.packet.out.PacketOutStartProxy;
import eu.cloudnetservice.cloudnet.v2.master.network.packet.out.PacketOutStartServer;
import eu.cloudnetservice.cloudnet.v2.master.network.packet.out.PacketOutStopProxy;
import eu.cloudnetservice.cloudnet.v2.master.network.packet.out.PacketOutStopServer;
import eu.cloudnetservice.cloudnet.v2.master.network.packet.out.PacketOutUpdateWrapperProperties;
import eu.cloudnetservice.cloudnet.v2.master.network.packet.out.PacketOutWrapperInfo;
import io.netty.channel.Channel;
import net.md_5.bungee.config.Configuration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Tareko on 26.05.2017.
 */
public final class Wrapper implements INetworkComponent {

    private final Map<String, ProxyServer> proxies = new ConcurrentHashMap<>();
    private final Map<String, MinecraftServer> servers = new ConcurrentHashMap<>();
    // Group, ServiceId
    private final Map<String, WaitingService> waitingServices = new ConcurrentHashMap<>();
    private Channel channel;
    private WrapperInfo wrapperInfo;
    private final WrapperMeta networkInfo;
    private boolean ready;
    private double cpuUsage = -1;
    private int maxMemory = 0;

    private final String serverId;

    public Wrapper(WrapperMeta networkInfo) {
        this.serverId = networkInfo.getId();
        this.networkInfo = networkInfo;
    }

    public int getMaxMemory() {
        return maxMemory;
    }

    public void setMaxMemory(int maxMemory) {
        this.maxMemory = maxMemory;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public Map<String, MinecraftServer> getServers() {
        return servers;
    }

    public Map<String, ProxyServer> getProxies() {
        return proxies;
    }

    public Map<String, WaitingService> getWaitingServices() {
        return waitingServices;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    @Override
    public Wrapper getWrapper() {
        return this;
    }

    @Override
    public String getServerId() {
        return serverId;
    }

    public WrapperInfo getWrapperInfo() {
        return wrapperInfo;
    }

    public void setWrapperInfo(WrapperInfo wrapperInfo) {
        this.wrapperInfo = wrapperInfo;
    }

    public WrapperMeta getNetworkInfo() {
        return networkInfo;
    }

    @Override
    public String getName() {
        return serverId;
    }

    public int getUsedMemoryAndWaitings() {
        return waitingServices.values().stream()
                              .map(WaitingService::getUsedMemory)
                              .mapToInt(i -> i)
                              .sum() + getUsedMemory();
    }

    public int getUsedMemory() {
        int mem = 0;

        for (ProxyServer proxyServer : proxies.values()) {
            mem += proxyServer.getProxyInfo().getMemory();
        }

        for (MinecraftServer proxyServer : servers.values()) {
            mem += proxyServer.getProcessMeta().getMemory();
        }

        return mem;
    }

    public void sendCommand(String commandLine) {
        sendPacket(new PacketOutExecuteCommand(commandLine));
    }

    public void disconnect() {
        this.wrapperInfo = null;
        this.maxMemory = 0;
        for (MinecraftServer minecraftServer : servers.values()) {
            try {
                minecraftServer.disconnect();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        for (ProxyServer minecraftServer : proxies.values()) {
            try {
                minecraftServer.disconnect();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        waitingServices.clear();
        servers.clear();
        proxies.clear();
    }

    public Wrapper updateWrapper() {

        if (channel == null) {
            return this;
        }

        Map<String, ServerGroup> groups = new ConcurrentHashMap<>();
        for (ServerGroup serverGroup : CloudNet.getInstance().getServerGroups().values()) {
            if (serverGroup.getWrapper().contains(networkInfo.getId())) {
                groups.put(serverGroup.getName(), serverGroup);
                sendPacket(new PacketOutCreateTemplate(serverGroup));
            }
        }

        Map<String, ProxyGroup> proxyGroups = new ConcurrentHashMap<>();
        for (ProxyGroup serverGroup : CloudNet.getInstance().getProxyGroups().values()) {
            if (serverGroup.getWrapper().contains(networkInfo.getId())) {
                proxyGroups.put(serverGroup.getName(), serverGroup);
                sendPacket(new PacketOutCreateTemplate(serverGroup));
            }
        }

        User user = CloudNet.getInstance().getUser(networkInfo.getUser());
        SimpledUser simpledUser = null;
        if (user != null) {
            simpledUser = user.toSimple();
        }

        WrapperExternal wrapperExternal = new WrapperExternal(CloudNet.getInstance().getNetworkManager().newCloudNetwork(),
                                                              simpledUser,
                                                              groups,
                                                              proxyGroups);
        sendPacket(new PacketOutWrapperInfo(wrapperExternal));
        return this;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void writeCommand(String commandLine) {
        sendPacket(new PacketOutExecuteCommand(commandLine));
    }

    public void writeServerCommand(String commandLine, ServerInfo serverInfo) {
        sendPacket(new PacketOutExecuteServerCommand(serverInfo, commandLine));
    }

    public void writeProxyCommand(String commandLine, ProxyInfo proxyInfo) {
        sendPacket(new PacketOutExecuteServerCommand(proxyInfo, commandLine));
    }

    public List<Integer> getBoundPorts() {
        return Stream.concat(
            Stream.concat(waitingServices.values().stream()
                                         .map(WaitingService::getPort),
                          servers.values().stream()
                                 .map(MinecraftServer::getProcessMeta)
                                 .map(ServerProcessMeta::getPort)),
            proxies.values().stream()
                   .map(ProxyServer::getProcessMeta)
                   .map(ProxyProcessMeta::getPort))
                     .collect(Collectors.toList());
    }

    public void startProxy(ProxyProcessMeta proxyProcessMeta) {
        sendPacket(new PacketOutStartProxy(proxyProcessMeta));
        System.out.println("Proxy [" + proxyProcessMeta.getServiceId() + "] is now in " + serverId + " queue.");

        this.waitingServices.put(proxyProcessMeta.getServiceId().getServerId(),
                                 new WaitingService(proxyProcessMeta.getPort(),
                                                    proxyProcessMeta.getMemory(),
                                                    proxyProcessMeta.getServiceId(),
                                                    null));
    }

    public void startGameServer(ServerProcessMeta serverProcessMeta) {
        sendPacket(new PacketOutStartServer(serverProcessMeta));
        System.out.println("Server [" + serverProcessMeta.getServiceId() + "] is now in " + serverId + " queue.");

        this.waitingServices.put(serverProcessMeta.getServiceId().getServerId(),
                                 new WaitingService(serverProcessMeta.getPort(),
                                                    serverProcessMeta.getMemory(),
                                                    serverProcessMeta.getServiceId(),
                                                    serverProcessMeta.getTemplate()));
    }

    public Wrapper stopServer(MinecraftServer minecraftServer) {
        if (this.servers.containsKey(minecraftServer.getServerId())) {
            sendPacket(new PacketOutStopServer(minecraftServer.getServerInfo()));
        }

        this.waitingServices.remove(minecraftServer.getServerId());
        return this;
    }

    public Wrapper stopProxy(ProxyServer proxyServer) {
        if (this.proxies.containsKey(proxyServer.getServerId())) {
            sendPacket(new PacketOutStopProxy(proxyServer.getProxyInfo()));
        }

        this.waitingServices.remove(proxyServer.getServerId());
        return this;
    }

    public Wrapper enableScreen(ServerInfo serverInfo) {
        sendPacket(new PacketOutScreen(serverInfo, DefaultType.BUKKIT, true));
        return this;
    }

    public Wrapper enableScreen(ProxyInfo serverInfo) {
        sendPacket(new PacketOutScreen(serverInfo, DefaultType.BUNGEE_CORD, true));
        return this;
    }

    public Wrapper disableScreen(ProxyInfo serverInfo) {
        sendPacket(new PacketOutScreen(serverInfo, DefaultType.BUNGEE_CORD, false));
        return this;
    }

    public Wrapper disableScreen(ServerInfo serverInfo) {
        sendPacket(new PacketOutScreen(serverInfo, DefaultType.BUKKIT, false));
        return this;
    }

    public Wrapper copyServer(ServerInfo serverInfo) {
        sendPacket(new PacketOutCopyServer(serverInfo));
        return this;
    }

    public Wrapper copyServer(ServerInfo serverInfo, Template template) {
        sendPacket(new PacketOutCopyServer(serverInfo, template));
        return this;
    }

    public void setConfigProperties(Configuration properties) {
        sendPacket(new PacketOutUpdateWrapperProperties(properties));
    }
}
