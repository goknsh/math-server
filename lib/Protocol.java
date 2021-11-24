package lib;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to marshal and unmarshal requests.
 */
public class Protocol {
    /**
     * Converts a <code>Map</code> of <code>String</code>s to a URL encoded string ready to be sent to the client/server.
     *
     * @param map <code>Map</code> of <code>String</code>s to convert.
     * @return URL encoded string.
     */
    public static String marshal(Map<String, String> map) {
        StringBuilder string = new StringBuilder();
        for (String key : map.keySet()) {
            try {
                string.append(URLEncoder.encode(key, "UTF-8")).append("=").append(URLEncoder.encode(map.get(key), "UTF-8")).append("&");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("This program requires UTF-8 encoding support.");
            }
        }
        return string.append("\n").toString();
    }

    /**
     * Converts a URL encoded string to a <code>Map</code> of <code>String</code>s ready to be used by the client/server.
     *
     * @param string URL encoded string to convert.
     * @return <code>Map</code> of <code>String</code>s.
     */
    public static Map<String, String> unmarshal(String string) {
        while (string.endsWith("\n")) string = string.substring(0, string.length() - 1);
        Map<String, String> map = new HashMap<>();

        for (String keyValuePair : string.split("&")) {
            try {
                String[] keyValue = keyValuePair.split("=");
                map.put(URLDecoder.decode(keyValue[0], "UTF-8"), URLDecoder.decode(keyValue[1], "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("This program requires UTF-8 encoding support.");
            }
        }
        return map;
    }
}
