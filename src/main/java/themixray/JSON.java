package themixray;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSON {
    public static String dump(Object o) {
        if (o instanceof Map<?,?> m)
            return JSONObject.toJSONString(m);
        if (o instanceof List<?> l)
            return JSONArray.toJSONString(l);
        return JSONValue.toJSONString(o);
    }

    private static Object load(Object o) {
        if (o instanceof JSONObject j) {
            Map<Object,Object> m = new HashMap<>();
            for (Object k:j.keySet())
                m.put(load(k),load(j.get(k)));
            return m;
        } else if (o instanceof JSONArray j) {
            List<Object> m = new ArrayList<>();
            for (Object v:j) m.add(load(v));
            return m;
        }
        return o;
    }

    public static Object load(String s) {
        return load(JSONValue.parse(s));
    }
}
