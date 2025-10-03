package com.github.moravcik.configtracker.lib.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ObjectUtils {

    private static final Pattern PATH_PART_ARRAY_PATTERN = Pattern.compile("^(.+)\\[(\\d+)\\]$");

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public enum DifferenceType {
        ADD, UPDATE, REMOVE, EQUAL
    }

    public static class Difference {
        public DifferenceType type;
        public String path;
        public Object oldValue;
        public Object newValue;

        public Difference(DifferenceType type, String path, Object oldValue, Object newValue) {
            this.type = type;
            this.path = path;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }

    public static List<Difference> calculateDifferences(Object obj1, Object obj2, boolean flat) {
        if (obj1 == null && obj2 == null) return new ArrayList<>();
        if (!flat && obj1 == null) return List.of(new Difference(DifferenceType.ADD, "", null, obj2));
        if (!flat && obj2 == null) return List.of(new Difference(DifferenceType.REMOVE, "", obj1, null));

        Map<String, Object> obj1Dot = flattenObject(obj1);
        Map<String, Object> obj2Dot = flattenObject(obj2);

        Set<String> allPaths = new HashSet<>();
        allPaths.addAll(obj1Dot.keySet());
        allPaths.addAll(obj2Dot.keySet());

        List<Difference> flatDiffs = allPaths.stream()
                .map(path -> {
                    DifferenceType type = getDifferenceType(obj1Dot, obj2Dot, path);
                    Object oldValue = obj1Dot.get(path);  // Will be null for ADD operations
                    Object newValue = obj2Dot.get(path);  // Will be null for REMOVE operations
                    return new Difference(type, path, oldValue, newValue);
                })
                .collect(Collectors.toList());


        if (flat) return flatDiffs.stream().filter(ObjectUtils::isDifference).collect(Collectors.toList());

        List<Difference> diffs = flatDiffs;
        List<Difference> reducedDiffs;
        do {
            reducedDiffs = mergeParentDifferences(obj1, obj2, diffs);
        } while (reducedDiffs.size() < diffs.size() && (diffs = reducedDiffs) != null);

        return diffs.stream().filter(ObjectUtils::isDifference).collect(Collectors.toList());
    }

    private static boolean isDifference(Difference diff) {
        return diff.type != DifferenceType.EQUAL;
    }

    private static DifferenceType getDifferenceType(Map<String, Object> obj1Dot, Map<String, Object> obj2Dot, String path) {
        boolean hasVal1 = obj1Dot.containsKey(path);
        boolean hasVal2 = obj2Dot.containsKey(path);

        if (hasVal1 && hasVal2) {
            Object val1 = obj1Dot.get(path);
            Object val2 = obj2Dot.get(path);
            
            // If new value is null but old value exists, it's a REMOVE
            if (val1 != null && val2 == null) {
                return DifferenceType.REMOVE;
            }
            // If old value is null but new value exists, it's an ADD
            if (val1 == null && val2 != null) {
                return DifferenceType.ADD;
            }
            
            return Objects.equals(val1, val2) ? DifferenceType.EQUAL : DifferenceType.UPDATE;
        } else if (hasVal2) {
            return DifferenceType.ADD;  // This should have newValue from obj2Dot
        } else {
            return DifferenceType.REMOVE;  // This should have oldValue from obj1Dot
        }
    }

    private static Map<String, Object> flattenObject(Object obj) {
        Map<String, Object> flattened = new HashMap<>();
        if (obj == null) return flattened;
        JsonNode node = objectMapper.valueToTree(obj);
        flattenNode("", node, flattened);
        return flattened;
    }

    private static void flattenNode(String prefix, JsonNode node, Map<String, Object> result) {
        if (node.isObject()) {
            // Handle nested objects
            node.fields().forEachRemaining(entry -> {
                String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                flattenNode(key, entry.getValue(), result);
            });
        } else if (node.isArray()) {
            // Handle arrays with index notation
            for (int i = 0; i < node.size(); i++) {
                String key = prefix + "[" + i + "]";
                flattenNode(key, node.get(i), result);
            }
        } else {
            // Handle primitive values - fix the value extraction
            Object value = extractNodeValue(node);
            result.put(prefix, value);
        }
    }

    private static Object extractNodeValue(JsonNode node) {
        if (node.isNull()) {
            return null;
        } else if (node.isTextual()) {
            return node.textValue();
        } else if (node.isBoolean()) {
            return node.booleanValue();
        } else if (node.isInt()) {
            return node.intValue();
        } else if (node.isLong()) {
            return node.longValue();
        } else if (node.isDouble()) {
            return node.doubleValue();
        } else if (node.isFloat()) {
            return node.floatValue();
        } else if (node.isBigDecimal()) {
            return node.decimalValue();
        } else if (node.isBigInteger()) {
            return node.bigIntegerValue();
        } else if (node.isObject() || node.isArray()) {
            // Convert ObjectNode/ArrayNode back to Java objects
            try {
                return objectMapper.treeToValue(node, Object.class);
            } catch (Exception e) {
                return node.toString();
            }
        } else {
            return node.asText(); // fallback for other types
        }
    }

    private static List<Difference> mergeParentDifferences(Object obj1, Object obj2, List<Difference> diffs) {
        List<Difference> reducedDiffs = new ArrayList<>();
        Set<String> reducedPaths = new HashSet<>();

        for (Difference diff : diffs) {
            if (reducedPaths.contains(diff.path)) continue;

            String parentPath = getParentPath(diff.path);
            List<Difference> siblings = diffs.stream()
                    .filter(d -> d.path.startsWith(parentPath + "."))
                    .collect(Collectors.toList());

            boolean allSameType = siblings.stream().allMatch(d -> d.type.equals(diff.type));

            if (!parentPath.isEmpty() && allSameType) {
                siblings.forEach(s -> reducedPaths.add(s.path));
                reducedDiffs.add(new Difference(
                        diff.type,
                        parentPath,
                        getValueAtPath(obj1, parentPath),
                        getValueAtPath(obj2, parentPath)
                ));
            } else {
                reducedDiffs.add(diff);
            }
        }
        return reducedDiffs;
    }

    private static String getParentPath(String path) {
        int lastDot = path.lastIndexOf('.');
        return lastDot > 0 ? path.substring(0, lastDot) : "";
    }

    private static Object getValueAtPath(Object obj, String path) {
        try {
            JsonNode node = objectMapper.valueToTree(obj);
            String[] parts = path.split("\\.");

            for (String part : parts) {
                Matcher matcher = PATH_PART_ARRAY_PATTERN.matcher(part);

                if (matcher.matches()) {
                    // Handle array access like "exceptions[1]"
                    String fieldName = matcher.group(1);
                    int index = Integer.parseInt(matcher.group(2));

                    node = node.get(fieldName);
                    if (node == null || !node.isArray() || index >= node.size()) {
                        return null;
                    }
                    node = node.get(index);
                } else {
                    // Handle regular field access
                    node = node.get(part);
                    if (node == null) return null;
                }
            }

            return extractNodeValue(node);
        } catch (Exception e) {
            return null;
        }
    }

}
