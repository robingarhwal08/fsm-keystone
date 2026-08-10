package com.fsm.keystone.schema;

import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards that {@code docs/baseline/entity-column-inventory.md} covers every
 * {@code @Entity} class and every persistent field discovered by classpath scanning.
 *
 * <p>Persistent fields are defined as: non-static, non-{@code @Transient} fields that
 * are NOT the inverse side of a collection relationship ({@code @OneToMany} /
 * {@code @ManyToMany} without {@code @JoinColumn}).
 *
 * <p>If an entity class or field is added to the model and the inventory document is not
 * updated, this test fails and aggregates all missing items in a single message.
 */
class EntityInventoryCoverageTest {

    private static final String ENTITY_PACKAGE = "com.fsm.keystone.entity";
    private static final Path INVENTORY_PATH = Path.of("../docs/baseline/entity-column-inventory.md");

    @Test
    void inventoryCoversAllEntityClassesAndPersistentFields() throws IOException, ClassNotFoundException {
        assertTrue(Files.exists(INVENTORY_PATH),
                "Entity inventory document not found at " + INVENTORY_PATH.toAbsolutePath());

        String inventory = Files.readString(INVENTORY_PATH);

        Map<String, List<String>> entityToFields = discoverEntityFields();
        assertTrue(!entityToFields.isEmpty(),
                "No @Entity classes found in " + ENTITY_PACKAGE + " — check package name.");

        List<String> missing = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : new TreeMap<>(entityToFields).entrySet()) {
            String entityName = entry.getKey();
            if (!inventory.contains(entityName)) {
                missing.add("ENTITY: " + entityName + " (not found in inventory)");
            }
            for (String field : entry.getValue()) {
                if (!inventory.contains("`" + field + "`")) {
                    missing.add("FIELD:  " + entityName + "." + field + " (not found as `" + field + "` in inventory)");
                }
            }
        }

        assertTrue(missing.isEmpty(),
                "entity-column-inventory.md is missing the following @Entity classes or persistent fields.\n"
                + "Add sections/rows for each and re-run this test:\n\n"
                + missing.stream().map(n -> "  " + n).collect(Collectors.joining("\n"))
                + "\n\nInventory path: " + INVENTORY_PATH.toAbsolutePath());
    }

    /**
     * Discovers all @Entity classes in the entity package and, for each, returns a sorted list
     * of persistent field names: non-static, non-@Transient, not the ownerless inverse of a
     * collection (@OneToMany / @ManyToMany without @JoinTable or @JoinColumn on this side).
     */
    private Map<String, List<String>> discoverEntityFields() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        Map<String, List<String>> result = new LinkedHashMap<>();

        for (var bd : scanner.findCandidateComponents(ENTITY_PACKAGE)) {
            String className = bd.getBeanClassName();
            if (className == null) continue;
            Class<?> cls = Class.forName(className);
            String simpleName = cls.getSimpleName();

            List<String> fields = new ArrayList<>();
            for (Field f : getAllFields(cls)) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                if (f.isAnnotationPresent(Transient.class)) continue;
                // Skip collection inverse side (no owning column on this table)
                if (isInverseCollectionSide(f)) continue;
                fields.add(f.getName());
            }
            Collections.sort(fields);
            result.put(simpleName, fields);
        }
        return result;
    }

    private List<Field> getAllFields(Class<?> cls) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        return fields;
    }

    private boolean isInverseCollectionSide(Field f) {
        // @OneToMany with mappedBy (no FK column on this table)
        OneToMany otm = f.getAnnotation(OneToMany.class);
        if (otm != null && !otm.mappedBy().isEmpty()) return true;
        // @ManyToMany with mappedBy (inverse side)
        ManyToMany mtm = f.getAnnotation(ManyToMany.class);
        if (mtm != null && !mtm.mappedBy().isEmpty()) return true;
        return false;
    }
}
