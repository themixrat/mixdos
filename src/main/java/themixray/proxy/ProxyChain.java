package themixray.proxy;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

public class ProxyChain {
    private final List<Proxy> proxies;

    public ProxyChain() {
        this.proxies = new ArrayList<>();
    }

    public void addProxy(String host, int port) {
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port));
        proxies.add(proxy);
    }

    public void addProxy(Proxy proxy) {
        proxies.add(proxy);
    }

    public List<Proxy> getProxies() {
        return proxies;
    }
}
