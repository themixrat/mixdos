package themixray;

import com.diogonunes.jcolor.AnsiFormat;
import com.diogonunes.jcolor.Attribute;
import com.sun.jna.Function;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import themixray.minecraft.MinecraftServer;
import themixray.proxy.ProxyParser;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import static com.diogonunes.jcolor.Ansi.colorize;
import static com.diogonunes.jcolor.Attribute.*;

public class Main {
    public static InetSocketAddress parseAddress(String text, int port_default) {
        String[] ss = text.split(":");
        return new InetSocketAddress(ss[0],
            ss.length == 2 ? Integer.parseInt(ss[1]) : port_default);
    }

    public static Set<Proxy> parseProxies(File file) {
        Set<Proxy> proxies = new HashSet<>();
        if (!file.exists()) return proxies;
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                InetSocketAddress address = parseAddress(line,8080);
                proxies.add(new Proxy(Proxy.Type.SOCKS, address));
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        ProxyParser p = new ProxyParser(Proxy.Type.SOCKS, 0, host, true);
        p.checkProxies(new ArrayList<>(proxies));
        return p.parsed;
    }

    public static Set<Proxy> parseProxies(InetSocketAddress parse_server) {
        return new ProxyParser(Proxy.Type.SOCKS, parse_time, parse_server, true).startParsing();
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

    public static void enableWindows10AnsiSupport() {
        Function GetStdHandleFunc = Function.getFunction("kernel32", "GetStdHandle");
        WinDef.DWORD STD_OUTPUT_HANDLE = new WinDef.DWORD(-11);
        WinNT.HANDLE hOut = (WinNT.HANDLE) GetStdHandleFunc.invoke(WinNT.HANDLE.class, new Object[]{STD_OUTPUT_HANDLE});

        WinDef.DWORDByReference p_dwMode = new WinDef.DWORDByReference(new WinDef.DWORD(0));
        Function GetConsoleModeFunc = Function.getFunction("kernel32", "GetConsoleMode");
        GetConsoleModeFunc.invoke(WinDef.BOOL.class, new Object[]{hOut, p_dwMode});

        int ENABLE_VIRTUAL_TERMINAL_PROCESSING = 4;
        WinDef.DWORD dwMode = p_dwMode.getValue();
        dwMode.setValue(dwMode.intValue() | ENABLE_VIRTUAL_TERMINAL_PROCESSING);
        Function SetConsoleModeFunc = Function.getFunction("kernel32", "SetConsoleMode");
        SetConsoleModeFunc.invoke(WinDef.BOOL.class, new Object[]{hOut, dwMode});
    }

    public static MinecraftServer server;

    public static InetSocketAddress host;
    public static int protocol_version;

    public static int bots_count;
    public static long bots_delay;
    public static String prefix;

    public static Set<Proxy> proxies;
    public static InetSocketAddress parse_server;
    public static int parse_time;

    public static List<String> chat_on_join;
    public static List<Long> chat_delays;

    public static boolean debug_mode;

    public static File exec_file;

    public static String getGradientColored(List<String> lines, Color from, Color to) {
        StringBuilder text = new StringBuilder();
        Gradient g = new Gradient(from, to);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Color color = g.getColor(i, lines.size());
            text.append(colorize(line, Attribute.TEXT_COLOR(color.getRed(), color.getGreen(), color.getBlue())));
            if (i != lines.size() - 1) text.append("\n");
        }
        return text.toString();
    }

    public static void sendLogoMessage() {
        System.out.println(getGradientColored(List.of((
                "• ▌ ▄ ·. ▪  ▐▄• ▄ ·▄▄▄▄        .▄▄ · \n" +
                "·██ ▐███▪██  █▌█▌▪██▪ ██ ▪     ▐█ ▀. \n" +
                "▐█ ▌▐▌▐█·▐█· ·██· ▐█· ▐█▌ ▄█▀▄ ▄▀▀▀█▄\n" +
                "██ ██▌▐█▌▐█▌▪▐█·█▌██. ██ ▐█▌.▐▌▐█▄▪▐█\n" +
                "▀▀  █▪▀▀▀▀▀▀•▀▀ ▀▀▀▀▀▀▀•  ▀█▄▀▪ ▀▀▀▀ "
                ).split("\n")),
                new Color(155, 0, 255),
                new Color(80, 0, 160))+"\n");
    }

    public static void sendHelpMessage() {
        System.out.println(
            "Example: "+exec_file.getName()+" --ip localhost\n\n" +

            "Required args: \n" +
            "  --ip <server ip>                // Server IP[:PORT] (default port 25565)\n\n" +

            "Optional args:\n" +
            "  --protocol <protocol version>   // Server protocol version (auto-detect by default)\n" +
            "  --count <count of players>      // Number of bots to connect (infinite by default)\n" +
            "  --delay <players connect delay> // Milliseconds between bot connections (default 500 if bots is infinite, else 50)\n" +
            "  --proxy <proxies.txt file>      // File with SOCKS5 proxy, on each line IP:PORT (parsing by default)\n" +
            "  --parse-time <proxy parse time> // Seconds to parse proxies (default 20)\n" +
            "  --parse-server <check server>   // Server to check parsed proxies (default server ip)\n" +
            "  --prefix <player name prefix>   // Bot nickname prefix (random characters by default)\n" +
            "  --cmds \"</cmd1>\" \"</cmd2>\" ...  // Entering commands after logging into the server\n" +
            "  --cmds-delay <value>            // Specific milliseconds before each command is entered\n" +
            "  --cmds-delay <min> <max>        // Random milliseconds before each command is entered\n" +
            "  --debug                         // See information about sent packets and errors (disabled by default)\n");
    }

    public static void main(String[] args) {
        exec_file = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath());

        Map<String,List<String>> params = parseParams(args);

        if (System.getProperty("os.name").toLowerCase().contains("windows"))
            enableWindows10AnsiSupport();

        sendLogoMessage();

        if ((args.length == 1 && args[0].equals("help")) ||
                (args.length == 0) ||
                (!checkParams(params,List.of("ip")))) {
            sendHelpMessage();
            return;
        }

//        System.out.println(MinecraftServer.fetchStatus(new InetSocketAddress("7.tcp.eu.ngrok.io",10963)));

        host = parseAddress(params.get("ip").get(0),25565);
        protocol_version = params.containsKey("protocol") ? Integer.parseInt(params.get("protocol").get(0)) : -1;
        bots_count = params.containsKey("count") ? Integer.parseInt(params.get("count").get(0)) : -1;
        parse_time = params.containsKey("parse-time") ? Integer.parseInt(params.get("parse-time").get(0)) : 40;
        parse_server = params.containsKey("parse-server") ? parseAddress(params.get("parse-server").get(0),25565) : host;
        bots_delay = params.containsKey("delay") ? Long.parseLong(params.get("delay").get(0)) : (bots_count == -1 ? 500 : 50);
        proxies = params.containsKey("proxy") ? parseProxies(new File(params.get("proxy").get(0))) : parseProxies(parse_server);
        prefix = params.containsKey("prefix") ? params.get("prefix").get(0) : null;
        debug_mode = params.containsKey("debug");
        chat_on_join = params.get("cmds");
        chat_delays = new ArrayList<>();

        if (params.containsKey("cmds-delay")) {
            List<String> chat_d = params.get("cmds-delay");
            if (chat_d.size() == 2) {
                long min = Long.parseLong(chat_d.get(0));
                long max = Long.parseLong(chat_d.get(1));
                for (int i = 0; i < chat_on_join.size(); i++)
                    chat_delays.add(rand.nextLong(max + 1 - min) + min);
            } else if (chat_d.size() == 1) {
                long value = Long.parseLong(chat_d.get(0));
                for (int i = 0; i < chat_on_join.size(); i++)
                    chat_delays.add(value);
            }
        } else if (chat_on_join != null) {
            long value = 1000;
            for (int i = 0; i < chat_on_join.size(); i++)
                chat_delays.add(value);
        }

        server = protocol_version == -1 ?
                new MinecraftServer(host) :
                new MinecraftServer(host,protocol_version);

        AnsiFormat param_name = new AnsiFormat(YELLOW_TEXT());
        AnsiFormat param_value = new AnsiFormat(BRIGHT_YELLOW_TEXT());

        System.out.print(
                param_name.format("            Host: ")+param_value.format(host.toString())+"\n"+
                param_name.format("Protocol version: ")+param_value.format(String.valueOf(server.protocol_version))+"\n"+
                param_name.format("      Bots count: ")+param_value.format(String.valueOf(bots_count))+"\n"+
                param_name.format("      Bots delay: ")+param_value.format(String.valueOf(bots_delay))+"\n"+
                param_name.format("         Proxies: ")+param_value.format(String.valueOf(proxies.size()))+"\n"+
                param_name.format("      Parse time: ")+param_value.format(String.valueOf(parse_time))+"\n"+
                param_name.format("          Prefix: ")+param_value.format(String.valueOf(prefix))+"\n"+
                param_name.format("      Debug mode: ")+param_value.format(String.valueOf(debug_mode))+"\n" +
                param_name.format("Commands on join: "));

        if (chat_on_join != null) {
            for (int i = 0; i < chat_on_join.size(); i++) {
                String v = chat_on_join.get(i);
                long d = chat_delays.get(i);

                boolean last = chat_on_join.size() == i + 1;

                System.out.print(param_value.format(v) +
                        param_name.format( " : ") + param_value.format(d + "ms")
                        + (!last ? param_name.format("\n                * ") : ""));
            }
            System.out.println("\n");
        } else {
            System.out.println(param_value.format("null\n"));
        }

        if (bots_count != -1) {
            for (int i = 0; i < bots_count; i++) connectPlayer(i);
            while (true) sleep(100000);
        } else {
            for (int i = 0; i > -1; i++) connectPlayer(i);
        }
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
        Proxy proxy = getRandomElement(proxies);
        server.connectPlayer(getName(n), proxy);
        sleep(bots_delay);
    }

    public static <T> T getRandomElement(Set<T> set) {
        if (set == null || set.isEmpty()) {
            return null;
        }
        int randomIndex = rand.nextInt(set.size());
        int i = 0;
        for (T element : set) {
            if (i == randomIndex) {
                return element;
            }
            i++;
        }
        return null;
    }

    public static String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return "0x"+new String(hexDigits).toUpperCase();
    }

    public static AnsiFormat output_log_first = new AnsiFormat(Attribute.BRIGHT_CYAN_TEXT());
    public static AnsiFormat output_log_second = new AnsiFormat(Attribute.CYAN_TEXT());
    public static AnsiFormat input_log_first = new AnsiFormat(Attribute.BRIGHT_MAGENTA_TEXT());
    public static AnsiFormat input_log_second = new AnsiFormat(Attribute.MAGENTA_TEXT());

    public static void logOutputPacket(int length, byte packetId, byte[] message) {
        if (debug_mode)
            System.out.println(output_log_second.format("-> ")+
                    output_log_first.format(byteToHex(packetId))+
                    output_log_second.format(" ["+length+"] : ")+
                    output_log_first.format(new String(message))+
                    output_log_second.format(" ["+message.length+"]"));
    }

    public static void logInputPacket(int length, byte packetId, byte[] message) {
        if (debug_mode)
            System.out.println(input_log_second.format("<- ")+
                    input_log_first.format(byteToHex(packetId))+
                    input_log_second.format(" ["+length+"] : ")+
                    input_log_first.format(new String(message))+
                    input_log_second.format(" ["+message.length+"]"));
    }

    public static byte[] zlibCompress(byte[] data) {
        Deflater compressor = new Deflater();
        compressor.setInput(data);
        compressor.finish();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        byte[] buf = new byte[1024];
        while (!compressor.finished()) {
            int count = compressor.deflate(buf);
            bos.write(buf, 0, count);
        }
        try {
            bos.close();
        } catch (IOException e) {}
        return bos.toByteArray();
    }

    public static byte[] zlipDecompress(byte[] data) throws DataFormatException {
        Inflater inflater = new Inflater();
        int to_decompress = data.length;
        inflater.setInput(data,0,to_decompress);
        List<Byte> result = new ArrayList<>();

        while (!inflater.needsInput()) {
            byte[] bytes = new byte[to_decompress];
            int bytes_decompressed = inflater.inflate(bytes);
            for (int b = 0; b < bytes_decompressed; b++)
                result.add(bytes[b]);
        }
        inflater.end();

        byte[] res_bytes = new byte[result.size()];
        for (int i = 0; i < res_bytes.length; i++)
            res_bytes[i] = result.get(i);
        return res_bytes;
    }
}