package com.github.moravcik.configtracker.lib.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;

public class ObjectUtilsTest {

    @Test
    public void testCalculateDifferences_SimpleValues() {
        Map<String, Object> oldObj = Map.of("name", "John", "age", 30);
        Map<String, Object> newObj = Map.of("name", "Jane", "age", 30);

        List<ObjectUtils.Difference> differences = ObjectUtils.calculateDifferences(oldObj, newObj, false);

        assertEquals(1, differences.size());
        ObjectUtils.Difference diff = differences.get(0);
        assertEquals(ObjectUtils.DifferenceType.UPDATE, diff.type);
        assertEquals("name", diff.path);
        assertEquals("John", diff.oldValue);
        assertEquals("Jane", diff.newValue);
    }

    @Test
    public void testCalculateDifferences_AddedField() {
        Map<String, Object> oldObj = Map.of("name", "John");
        Map<String, Object> newObj = Map.of("name", "John", "age", 30);

        List<ObjectUtils.Difference> differences = ObjectUtils.calculateDifferences(oldObj, newObj, false);

        assertEquals(1, differences.size());
        ObjectUtils.Difference diff = differences.get(0);
        assertEquals(ObjectUtils.DifferenceType.ADD, diff.type);
        assertEquals("age", diff.path);
        assertNull(diff.oldValue);
        assertEquals(30, diff.newValue);
    }

    @Test
    public void testCalculateDifferences_RemovedField() {
        Map<String, Object> oldObj = Map.of("name", "John", "age", 30);
        Map<String, Object> newObj = Map.of("name", "John");

        List<ObjectUtils.Difference> differences = ObjectUtils.calculateDifferences(oldObj, newObj, false);

        assertEquals(1, differences.size());
        ObjectUtils.Difference diff = differences.get(0);
        assertEquals(ObjectUtils.DifferenceType.REMOVE, diff.type);
        assertEquals("age", diff.path);
        assertEquals(30, diff.oldValue);
        assertNull(diff.newValue);
    }

    @Test
    public void testCalculateDifferences_NestedObjects() {
        Map<String, Object> oldObj = Map.of("user", Map.of("name", "John", "age", 30));
        Map<String, Object> newObj = Map.of("user", Map.of("name", "Jane", "age", 30));

        List<ObjectUtils.Difference> differences = ObjectUtils.calculateDifferences(oldObj, newObj, false);

        assertEquals(1, differences.size());
        ObjectUtils.Difference diff = differences.get(0);
        assertEquals(ObjectUtils.DifferenceType.UPDATE, diff.type);
        assertEquals("user.name", diff.path);
        assertEquals("John", diff.oldValue);
        assertEquals("Jane", diff.newValue);
    }

    @Test
    public void testCalculateDifferences_EqualObjects() {
        Map<String, Object> oldObj = Map.of("name", "John", "age", 30);
        Map<String, Object> newObj = Map.of("name", "John", "age", 30);

        List<ObjectUtils.Difference> differences = ObjectUtils.calculateDifferences(oldObj, newObj, false);

        assertTrue(differences.isEmpty());
    }

    @Test
    public void testCalculateDifferences_ArrayAddPrimitive() {
        Map<String, Object> oldObj = Map.of("tags", List.of("java", "spring"));
        Map<String, Object> newObj = Map.of("tags", List.of("java", "spring", "aws"));

        List<ObjectUtils.Difference> differences = ObjectUtils.calculateDifferences(oldObj, newObj, false);

        assertEquals(1, differences.size());
        ObjectUtils.Difference diff = differences.get(0);
        assertEquals(ObjectUtils.DifferenceType.ADD, diff.type);
        assertEquals("tags[2]", diff.path);
        assertNull(diff.oldValue);
        assertEquals("aws", diff.newValue);
    }

    @Test
    public void testCalculateDifferences_ArrayRemovePrimitive() {
        Map<String, Object> oldObj = Map.of("tags", List.of("java", "spring", "aws"));
        Map<String, Object> newObj = Map.of("tags", List.of("java", "spring"));

        List<ObjectUtils.Difference> differences = ObjectUtils.calculateDifferences(oldObj, newObj, false);

        assertEquals(1, differences.size());
        ObjectUtils.Difference diff = differences.get(0);
        assertEquals(ObjectUtils.DifferenceType.REMOVE, diff.type);
        assertEquals("tags[2]", diff.path);
        assertEquals("aws", diff.oldValue);
        assertNull(diff.newValue);
    }

    @Test
    public void testCalculateDifferences_ArrayAddObjectFlat() {
        Map<String, Object> oldObj = Map.of("users", List.of(
            Map.of("name", "John", "age", 30)
        ));
        Map<String, Object> newObj = Map.of("users", List.of(
            Map.of("name", "John", "age", 30),
            Map.of("name", "Jane", "age", 25)
        ));

        List<ObjectUtils.Difference> differences = ObjectUtils.calculateDifferences(oldObj, newObj, true);

        assertEquals(2, differences.size());
        
        ObjectUtils.Difference nameDiff = differences.stream()
            .filter(d -> d.path.equals("users[1].name"))
            .findFirst().orElse(null);
        assertNotNull(nameDiff);
        assertEquals(ObjectUtils.DifferenceType.ADD, nameDiff.type);
        assertEquals("Jane", nameDiff.newValue);

        ObjectUtils.Difference ageDiff = differences.stream()
            .filter(d -> d.path.equals("users[1].age"))
            .findFirst().orElse(null);
        assertNotNull(ageDiff);
        assertEquals(ObjectUtils.DifferenceType.ADD, ageDiff.type);
        assertEquals(25, ageDiff.newValue);
    }

    @Test
    public void testCalculateDifferences_ArrayRemoveObject() {
        Map<String, Object> oldObj = Map.of("users", List.of(
            Map.of("name", "John", "age", 30),
            Map.of("name", "Jane", "age", 25)
        ));
        Map<String, Object> newObj = Map.of("users", List.of(
            Map.of("name", "John", "age", 30)
        ));

        List<ObjectUtils.Difference> differences = ObjectUtils.calculateDifferences(oldObj, newObj, false);

        assertEquals(1, differences.size());
        
        ObjectUtils.Difference userDiff = differences.stream()
            .filter(d -> d.path.equals("users[1]"))
            .findFirst().orElse(null);
        assertNotNull(userDiff);
        assertEquals(ObjectUtils.DifferenceType.REMOVE, userDiff.type);
        assertEquals(Map.of("name", "Jane", "age", 25), userDiff.oldValue);
    }
}