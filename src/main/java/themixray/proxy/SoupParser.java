package themixray.proxy;

import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.Set;

public abstract class SoupParser {
    public abstract String getUrl();
    public abstract Set<Proxy> parseProxies(String text);
}