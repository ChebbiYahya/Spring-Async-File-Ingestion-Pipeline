package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.persistence;
// Couche infrastructure: parsing, persistence, mapping, validation et filesystem.

import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation.TypeConverter;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.rules.FieldRule;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * RecordToEntityMapper
 *
 * Mapper générique :
 * - Map<String,String> (record) + List<FieldRule> (mapping) -> Entité JPA (T)
 * - Affecte dynamiquement les propriétés via BeanWrapper (pas de setters codés en dur)
 * - Convertit les types via TypeConverter en se basant sur rule.getType()
 */
@Component
public class RecordToEntityMapper {

    private final TypeConverter typeConverter = new TypeConverter();

    /**
     * Construit et remplit une entité de type T à partir d'un record et d'un mapping.
     *
     * Hypothèse :
     * - rule.name correspond au nom de propriété Java (ex: "firstName" -> setFirstName)
     */
    public <T> T toEntity(Map<String, String> record,
                          List<? extends FieldRule> rules,
                          Class<T> entityClass) {

        try {
            // 1) Instanciation de l'entité (nécessite un constructeur vide)
            T entity = entityClass.getDeclaredConstructor().newInstance();

            // 2) BeanWrapper permet d'écrire sur les propriétés par leur nom (reflection-safe)
            BeanWrapper bw = new BeanWrapperImpl(entity);

            // 3) Pour chaque champ défini dans le mapping, on copie record[fieldName] -> entity.fieldName
            for (FieldRule rule : rules) {
                String fieldName = rule.getName();   // ex: "hireDate"
                String raw = record.get(fieldName);  // valeur brute String

                // Si la propriété n'existe pas dans l'entité, on ignore (mapping peut être plus large)
                if (!bw.isWritableProperty(fieldName)) {
                    continue;
                }

                // Normalisation des valeurs vides -> null
                if (raw == null || raw.trim().isEmpty()) {
                    bw.setPropertyValue(fieldName, null);
                    continue;
                }

                // Conversion selon rule.type (LONG/DECIMAL/LOCAL_DATE/STRING)
                Object typed = typeConverter.convert(rule.getType(), raw.trim());

                // Affectation dynamique dans l'entité
                bw.setPropertyValue(fieldName, typed);
            }

            return entity;

        } catch (Exception e) {
            throw new IllegalStateException("Cannot map record to entity " + entityClass.getSimpleName()
                    + ": " + e.getMessage(), e);
        }
    }
}
