package themixray.proxy;

import themixray.JSON;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class ProxyParser {
    private static List<SoupParser> parsers = new ArrayList<>();

    private static void addParser(SoupParser parser) {
        parsers.add(parser);
    }

    private static void addLinesParser(String url, Proxy.Type type) {
        addParser(
            new SoupParser() {
                @Override
                public String getUrl() {
                    return url;
                }

                @Override
                public Set<Proxy> parseProxies(String text) {
                    Set<Proxy> proxies = new HashSet<>();
                    for (String s:text.split("\n")) {
                        try {
                            proxies.add(hostToProxy(s, type));
                        } catch (Exception e) {}
                    }
                    return proxies;
                }
            }
        );
    }

    private static Proxy hostToProxy(String host, Proxy.Type type) {
        String[] ss = host.split(":");
        return new Proxy(type, new InetSocketAddress(ss[0], Integer.parseInt(ss[1])));
    }

    static {
        addLinesParser("https://raw.githubusercontent.com/hookzof/socks5_list/master/proxy.txt", Proxy.Type.SOCKS);

        addLinesParser("https://raw.githubusercontent.com/TheSpeedX/SOCKS-List/master/http.txt", Proxy.Type.HTTP);
        addLinesParser("https://raw.githubusercontent.com/TheSpeedX/SOCKS-List/master/socks5.txt", Proxy.Type.SOCKS);
        addLinesParser("https://raw.githubusercontent.com/TheSpeedX/SOCKS-List/master/socks4.txt", Proxy.Type.SOCKS);

        addLinesParser("https://raw.githubusercontent.com/prxchk/proxy-list/master/http.txt", Proxy.Type.HTTP);
        addLinesParser("https://raw.githubusercontent.com/prxchk/proxy-list/master/socks5.txt", Proxy.Type.SOCKS);
        addLinesParser("https://raw.githubusercontent.com/prxchk/proxy-list/master/socks4.txt", Proxy.Type.SOCKS);

        addLinesParser("https://raw.githubusercontent.com/officialputuid/KangProxy/KangProxy/http/http.txt", Proxy.Type.HTTP);
        addLinesParser("https://raw.githubusercontent.com/officialputuid/KangProxy/KangProxy/https/https.txt", Proxy.Type.HTTP);
        addLinesParser("https://raw.githubusercontent.com/officialputuid/KangProxy/KangProxy/socks5/socks5.txt", Proxy.Type.SOCKS);
        addLinesParser("https://raw.githubusercontent.com/officialputuid/KangProxy/KangProxy/socks4/socks4.txt", Proxy.Type.SOCKS);

        addLinesParser("https://raw.githubusercontent.com/Anonym0usWork1221/Free-Proxies/main/proxy_files/http_proxies.txt", Proxy.Type.HTTP);
        addLinesParser("https://raw.githubusercontent.com/Anonym0usWork1221/Free-Proxies/main/proxy_files/https_proxies.txt", Proxy.Type.HTTP);
        addLinesParser("https://raw.githubusercontent.com/Anonym0usWork1221/Free-Proxies/main/proxy_files/socks5_proxies.txt", Proxy.Type.SOCKS);
        addLinesParser("https://raw.githubusercontent.com/Anonym0usWork1221/Free-Proxies/main/proxy_files/socks4_proxies.txt", Proxy.Type.SOCKS);

        addParser(
            new SoupParser() {
                @Override
                public String getUrl() {
                    return "https://raw.githubusercontent.com/proxifly/free-proxy-list/main/proxies/all/data.json";
                }

                @Override
                public Set<Proxy> parseProxies(String text) {
                    Set<Proxy> proxies = new HashSet<>();
                    for (Map<String,Object> d:(List<Map<String,Object>>) JSON.load(text)) {
                        String proxy_type = (String) d.get("protocol");
                        InetSocketAddress proxy_host = new InetSocketAddress(
                                (String) d.get("ip"), ((Long) d.get("port")).intValue());

                        if (proxy_type.startsWith("http"))
                            proxies.add(new Proxy(Proxy.Type.HTTP, proxy_host));
                        else if (proxy_type.startsWith("socks"))
                            proxies.add(new Proxy(Proxy.Type.SOCKS, proxy_host));
                    }
                    return proxies;
                }
            }
        );

        addParser(
            new SoupParser() {
                @Override
                public String getUrl() {
                    return "https://raw.githubusercontent.com/vakhov/fresh-proxy-list/master/proxylist.json";
                }

                @Override
                public Set<Proxy> parseProxies(String text) {
                    Set<Proxy> proxies = new HashSet<>();
                    for (Map<String,Object> d:(List<Map<String,Object>>) JSON.load(text)) {
                        Proxy.Type proxy_type = (d.get("socks5").equals("1") || d.get("socks4").equals("1")) ? Proxy.Type.SOCKS :
                                                (d.get("http").equals("1") || d.get("ssl").equals("1")) ? Proxy.Type.HTTP : null;
                        InetSocketAddress proxy_host = new InetSocketAddress((String) d.get("ip"), Integer.parseInt((String) d.get("port")));

                        proxies.add(new Proxy(proxy_type, proxy_host));
                    }
                    return proxies;
                }
            }
        );
    }

    public boolean checkProxy(Proxy proxy) {
        if (!proxy.type().equals(proxy_type)) return false;

        try {
            return ((InetSocketAddress)proxy.address()).getAddress().isReachable(1000);
        } catch (Exception e) {}
        return false;
    }

    public HttpClient httpClient = HttpClient.newHttpClient();

    public Set<Proxy> parseProxies(SoupParser s) {
        try {
            URI url = new URI(s.getUrl());
            return s.parseProxies(httpClient.send(
                HttpRequest.newBuilder().GET().uri(url).build(),
                HttpResponse.BodyHandlers.ofString()).body());
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<Proxy> parsed;
    public Proxy.Type proxy_type;
    public int seconds;

    public ProxyParser(Proxy.Type proxy_type) {
        this.parsed = new HashSet<>();
        this.proxy_type = proxy_type;
        this.seconds = 20;
    }

    public Set<Proxy> startParsing() {
        System.out.println("Parsing proxies... ("+seconds+" sec)\n");
        long start = System.currentTimeMillis();

        Collections.shuffle(parsers);

        for (SoupParser s:parsers) {
            List<Proxy> proxies = new ArrayList<>(parseProxies(s));
            Collections.shuffle(proxies);

            for (Proxy p : proxies) {
                if (checkProxy(p)) {
                    parsed.add(p);
                    if (System.currentTimeMillis() - start >= 1000 * seconds)
                        return parsed;
                }
            }
        }
        return parsed;
    }
}
