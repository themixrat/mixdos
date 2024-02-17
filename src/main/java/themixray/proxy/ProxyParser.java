package themixray.proxy;

import com.diogonunes.jcolor.Attribute;
import themixray.JSON;
import themixray.Main;
import themixray.minecraft.MinecraftServer;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.diogonunes.jcolor.Ansi.colorize;

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

        AtomicLong ping = new AtomicLong(-1);

        new Thread(() -> {
            try {
                ProxyChain chain = new ProxyChain();
                chain.addProxy(proxy);
                if (!parsed.isEmpty()) chain.addProxy(parsed.iterator().next());
                ping.set(MinecraftServer.fetchPing(server, chain));
            } catch (Exception e) {}
        }).start();

        long start = System.currentTimeMillis();

        while (true) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (System.currentTimeMillis() - start > 5000 ||
                    ping.get() != -1) {
                break;
            }
        }

        return ping.get() != -1 && ping.get() < 3000;
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
    public InetSocketAddress server;
    public boolean do_check;

    public ProxyParser(Proxy.Type proxy_type,
                       int parse_time,
                       InetSocketAddress server,
                       boolean do_check) {
        this.parsed = new HashSet<>();
        this.proxy_type = proxy_type;
        this.seconds = parse_time;
        this.server = server;
        this.do_check = do_check;
    }

    public void checkProxies(List<Proxy> proxies) {
        if (do_check) {
            Collections.shuffle(proxies);
            for (Proxy p : proxies) {
                new Thread(() -> {
                    if (checkProxy(p)) {
                        parsed.add(p);
                        if (Main.debug_mode)
                            System.out.println(colorize(p.toString(), Attribute.GREEN_TEXT()));
                    } else {
                        if (Main.debug_mode)
                            System.out.println(colorize(p.toString(), Attribute.RED_TEXT()));
                    }
                }).start();
            }
        } else {
            parsed.addAll(proxies);
        }
    }

    public Set<Proxy> startParsing() {
        System.out.println("Parsing proxies... ("+seconds+" sec)\n");

        Collections.shuffle(parsers);

        new Thread(() -> {
            for (SoupParser s : parsers)
                checkProxies(new ArrayList<>(parseProxies(s)));
        }).start();

        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return parsed;
    }
}
