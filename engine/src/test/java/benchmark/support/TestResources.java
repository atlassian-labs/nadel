package benchmark.support;

import graphql.nadel.ServiceExecutionResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings({"rawtypes", "unchecked"})
public class TestResources {


    public static File getResourceFile(String resource) {
        URL resourceUri = TestResources.class.getClassLoader().getResource(resource);
        String path = Objects.requireNonNull(resourceUri, "Resource does not exist")
                .toString()
                .replace(resourceUri.getProtocol() + ":", "");

        return new File(path);
    }

    public static String readFromClasspath(String resource) throws IOException {
        File file = getResourceFile(resource);
        BufferedReader reader = new BufferedReader(new FileReader(file));

        StringBuilder overall = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            overall.append(line).append('\n');
        }

        return overall.toString();
    }

    @SuppressWarnings("unchecked")
    public static ServiceExecutionResult toServiceResult(Map<?, ?> responseMap) {
        return new ServiceExecutionResult(
                (Map<String, Object>) deepClone(responseMap.get("data")),
                (List<Map<String, Object>>) deepClone(responseMap.get("errors"))
        );
    }

    public static Object deepClone(Object input) {
        if (input instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) input;
            Map cloned = new HashMap(map.size());
            for (Map.Entry o : map.entrySet()) {
                cloned.put(
                        o.getKey(),
                        deepClone(o.getValue())
                );
            }
            return cloned;
        } else if (input instanceof List) {
            List list = (List) input;
            List cloned = new ArrayList(list.size());
            for (Object o : list) {
                cloned.add(deepClone(o));
            }
            return cloned;
        } else {
            return input;
        }
    }
}
