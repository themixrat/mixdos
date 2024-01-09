package themixray;

import themixray.minecraft.MinecraftServer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;

public class Main {
    public static InetSocketAddress parseAddress(String text, int port_default) {
        String[] ss = text.split(":");
        return new InetSocketAddress(ss[0],
            ss.length == 2 ? Integer.parseInt(ss[1]) : port_default);
    }

    public static Set<Proxy> parseProxies(File file) {
        Set<Proxy> proxies = new HashSet<>();
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                InetSocketAddress address = parseAddress(line,8080);
                proxies.add(new Proxy(Proxy.Type.SOCKS, address));
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return proxies;
    }

    public static Map<String,List<String>> parseParams(String[] args) {
        Map<String, List<String>> params = new HashMap<>();

        List<String> options = null;
        for (int i = 0; i < args.length; i++) {
            String a = args[i];

            if (a.startsWith("--")) {
                options = new ArrayList<>();
                params.put(a.substring(2), options);
            } else if (options != null) {
                options.add(a);
            } else {
                List<String> o = params.getOrDefault(
                        null, new ArrayList<>());
                o.add(a);
                params.put(null, o);
            }
        }

        return params;
    }

    public static boolean checkParams(
            Map<String, List<String>> params,
            List<String> keys_must_be) {
        for (String k:keys_must_be)
            if (!params.containsKey(k))
                return false;
        return true;
    }

    public static MinecraftServer server;

    public static InetSocketAddress host;
    public static int protocol_version;

    public static int bots_count;
    public static long bots_delay;
    public static Set<Proxy> proxies;
    public static String prefix;

    public static File exec_file;

    public static void sendHelpMessage() {
        System.out.println(
            "Example: "+exec_file.getName()+" --ip localhost --protocol 763\n\n" +

            "Args: \n" +
            "  --protocol <protocol version>\n" +
            "  --ip <server ip>\n\n" +

            "Optional args:\n" +
            "  --count <count of players>\n" +
            "  --delay <players connect delay in ms>\n" +
            "  --proxy <proxies.txt file>\n" +
            "  --prefix <player name prefix>\n");
    }

    public static void main(String[] args) {
        exec_file = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath());

        Map<String,List<String>> params = parseParams(args);

        if (args.length == 1) {
            switch (args[0]) {
                case ("help"): {
                    sendHelpMessage();
                    return;
                } case ("test"): {
                    new MinecraftServer(new InetSocketAddress(25565),
                        763).connectPlayer("proverka",null);
                    return;
                }
            }
        }

        if (args.length == 0 || !checkParams(params, List.of("protocol","ip"))) {
            sendHelpMessage();

            if (exec_file.getName().endsWith(".exe")) {
                try {
                    Runtime.getRuntime().exec("cmd.exe /c start \"MixDos\" cmd.exe /k \""+exec_file.getAbsolutePath()+"\" help");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return;
        }

        host = parseAddress(params.get("ip").get(0),25565);
        protocol_version = Integer.parseInt(params.get("protocol").get(0));

        bots_count = params.containsKey("count") ? Integer.parseInt(params.get("count").get(0)) : -1;
        bots_delay = params.containsKey("delay") ? Long.parseLong(params.get("delay").get(0)) : (bots_count == -1 ? 500 : 50);
        proxies = params.containsKey("proxy") ? parseProxies(new File(params.get("proxy").get(0))) : null;
        prefix = params.containsKey("prefix") ? params.get("prefix").get(0) : null;

        System.out.println(
                "Host: "+host+"\n"+
                "Protocol version: "+protocol_version+"\n"+
                "Bots count: "+bots_count+"\n"+
                "Bots delay: "+bots_delay+"\n"+
                "Proxy: "+(proxies != null ? params.get("proxy").get(0) : "-")+"\n"+
                "Prefix: "+prefix+"\n\n");

        server = new MinecraftServer(host,protocol_version);

        for (int i = 0; i < bots_count; i++) connectPlayer(i);
        while (true) sleep(10000);
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static final char[] nameChars = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
    public static final Random rand = new Random();

    public static String randomString(int size) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < size; i++)
            str.append(nameChars[rand.nextInt(nameChars.length)]);
        return str.toString();
    }

    public static String intToString(int n) {
        StringBuilder str = new StringBuilder();
        int x = n / nameChars.length;
        for (int i = 0; i < x; i++)
            str.append(nameChars[nameChars.length-1]);
        n = n - x * nameChars.length;
        str.append(nameChars[n]);
        return str.toString();
    }

    public static String getName(int n) {
        return prefix == null ? randomString(16) : prefix+intToString(n);
    }

    public static void connectPlayer(int n) {
        server.connectPlayer(getName(n), proxies != null ? getRandomElement(proxies) : null);
        sleep(bots_delay);
    }

    public static <T> T getRandomElement(Set<T> set) {
        if (set == null || set.isEmpty()) {
            throw new IllegalArgumentException("The Set cannot be empty.");
        }
        int randomIndex = rand.nextInt(set.size());
        int i = 0;
        for (T element : set) {
            if (i == randomIndex) {
                return element;
            }
            i++;
        }
        throw new IllegalStateException("Something went wrong while picking a random element.");
    }

    public static String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return "0x"+new String(hexDigits).toUpperCase();
    }

    public static void logOutputPacket(int length, byte packetId, byte[] message) {
        System.out.println("output "+byteToHex(packetId)+" ["+length+"] : "+new String(message)+" ["+message.length+"]");
    }

    public static void logInputPacket(int packetSize, byte packetId, byte[] data) {
        System.out.println("input "+byteToHex(packetId)+" ["+packetSize+"] : "+new String(data)+" ["+data.length+"]");
    }
}