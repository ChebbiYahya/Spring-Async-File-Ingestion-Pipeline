package com.bank.uploadfileanddatapersistdb_v3.application.service;

import com.bank.uploadfileanddatapersistdb_v3.api.dto.FileReaderConfigDto;
import com.bank.uploadfileanddatapersistdb_v3.api.mapper.FileReaderConfigMapper;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.FileReaderConfigService;
import com.bank.uploadfileanddatapersistdb_v3.domain.exception.ConfigNotFoundException;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.FileReaderConfig;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.persistence.repository.FileReaderConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service métier responsable de la gestion des configurations de lecture (FileReaderConfig).
 *
 * Responsabilités principales :
 * - Charger une configuration (Entity) avec son "graphe" (mappings CSV/XML + collections associées)
 * - Exposer une vue DTO pour l'API (sans exposer directement l'entité JPA)
 * - Créer / Mettre à jour (UPSERT) une configuration à partir d'un DTO
 * - Supprimer une configuration
 *
 * Remarque d'architecture :
 * - Le mapping DTO <-> Entity est externalisé dans FileReaderConfigMapper.
 *   Le service reste concentré sur : transactions, orchestration, persistance.
 */
@Service
@RequiredArgsConstructor
public class FileReaderConfigServiceImpl implements FileReaderConfigService {

    // Repository JPA pour accéder à FileReaderConfig (DB).
    private final FileReaderConfigRepository repo;

    // Mapper dédié pour convertir Entity <-> DTO (sorti du service pour SRP/testabilité).
    private final FileReaderConfigMapper mapper;

    /**
     * Récupère l'entité FileReaderConfig en garantissant que les sous-graphes nécessaires
     * (CSV/XML + collections) sont initialisés.
     *
     * Pourquoi :
     * - Les relations JPA sont souvent LAZY.
     * - Si on accède à des collections après la fin de la transaction, on risque une LazyInitializationException.
     *
     * Stratégie :
     * - Utilise une requête "JOIN FETCH" (repo.findWithMappingsByIdConfigFichier)
     * - Puis "touche" certaines collections avec size() pour forcer leur initialisation dans la transaction.
     *
     * @param id identifiant fonctionnel de config (ex: "EMPLOYEES")
     * @return entité JPA totalement exploitable dans le reste du pipeline
     */

    /**
     @Override
     Indique que cette méthode implémente/redéfinit une méthode
     définie dans une interface ou une classe parente.
     Permet au compilateur de détecter les erreurs de signature
     et sécurise le refactoring.

     @Transactional
     Exécute la méthode dans une transaction Spring.
     Garantit la cohérence des données et le rollback automatique
     en cas d’exception.
     @Transactional(readOnly = true)
     Démarre une transaction en lecture seule.
     Optimise les performances et permet le chargement des relations LAZY
     sans autoriser de modification en base.
     **/

    @Override
    @Transactional(readOnly = true) // Transaction de lecture (optimisée) + safe pour le LAZY loading
    public FileReaderConfig getEntity(String id) {

        // Requête custom conçue pour charger la config + mapping CSV/XML (et certaines collections)
        FileReaderConfig cfg = repo.findWithMappingsByIdConfigFichier(id)
                .orElseThrow(() -> new ConfigNotFoundException("Config not found: " + id));

        // "Touch" défensif : force l'initialisation de collections LAZY avant la fin de la transaction.
        // Utile si certains éléments restent LAZY selon les mappings / provider / query.
        if (cfg.getFileMappingCSV() != null) {
            cfg.getFileMappingCSV().getDuplicateCheck().size();
            cfg.getFileMappingCSV().getColumns().size();
        }
        if (cfg.getFileMappingXML() != null) {
            cfg.getFileMappingXML().getDuplicateCheck().size();
            cfg.getFileMappingXML().getFields().size();
        }

        return cfg;
    }

    /**
     * Retourne la configuration sous forme de DTO (utilisé par l'API).
     * - On charge l'entité via getEntity() (graph complet),
     * - puis on convertit en DTO via FileReaderConfigMapper.
     */
    @Override
    @Transactional(readOnly = true) // Lecture seule
    public FileReaderConfigDto get(String id) {
        return mapper.toDto(getEntity(id));
    }

    /**
     * UPSERT (Update or Insert) d'une configuration.
     *
     * Règles :
     * - dto.idConfigFichier est obligatoire (clé fonctionnelle).
     * - Si l'entité existe => mise à jour.
     * - Sinon => création.
     *
     * Le mapping DTO -> Entity est délégué au mapper (updateEntityFromDto),
     * ce qui permet de garder ce service focalisé sur l'orchestration + transactions.
     *
     * @param dto configuration à créer/mettre à jour
     * @return DTO complet relu en base (sans problèmes LAZY)
     */
    @Override
    @Transactional // Transaction d'écriture : commit/rollback atomique
    public FileReaderConfigDto upsert(FileReaderConfigDto dto) {

        // Validation minimale
        if (dto == null || dto.getIdConfigFichier() == null || dto.getIdConfigFichier().isBlank()) {
            throw new IllegalArgumentException("idConfigFichier is required");
        }

        // Charger l'existant ou créer une nouvelle entité
        FileReaderConfig cfg = repo.findById(dto.getIdConfigFichier())
                .orElseGet(() -> FileReaderConfig.builder()
                        .idConfigFichier(dto.getIdConfigFichier())
                        .build());

        // Appliquer dto -> entity (paths + mappings CSV/XML + colonnes/champs + relations bidirectionnelles)
        mapper.updateEntityFromDto(dto, cfg);

        // Persister l'aggregate. Cascade/OrphanRemoval gèrent les sous-objets.
        FileReaderConfig saved = repo.save(cfg);

        // Très important :
        // On relit l'entité via getEntity() (JOIN FETCH + init collections)
        // pour retourner un DTO "complet" et éviter des Lazy issues (open-in-view souvent désactivé).
        return mapper.toDto(getEntity(saved.getIdConfigFichier()));
    }

    /**
     * Supprime une configuration par ID.
     * En fonction des mappings JPA (cascade/orphanRemoval), les sous-entités associées
     * (mappings CSV/XML, colonnes, champs) peuvent être supprimées également.
     */
    @Override
    @Transactional // Écriture
    public void delete(String id) {
        repo.deleteById(id);
    }
}
