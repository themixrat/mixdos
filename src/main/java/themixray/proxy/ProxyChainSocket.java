package themixray.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

public class ProxyChainSocket extends Socket {
    private final ProxyChain proxyChain;

    public ProxyChainSocket(ProxyChain proxyChain) {
        this.proxyChain = proxyChain;
    }

    public void connect(InetSocketAddress endpoint) throws IOException {
        Socket currentSocket = null;
        for (Proxy proxy : proxyChain.getProxies()) {
            Socket nextSocket = new Socket(proxy);
            if (currentSocket != null) {
                nextSocket.connect(currentSocket.getRemoteSocketAddress());
            } else {
                nextSocket.connect(endpoint);
            }
            currentSocket = nextSocket;
        }
    }
}