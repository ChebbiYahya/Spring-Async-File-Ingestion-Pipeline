package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.parser;

import com.bank.uploadfileanddatapersistdb_v3.domain.exception.SchemaValidationException;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation.ErrorCode;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation.RecordValidationException;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.XmlSchema;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.rules.XmlFieldRule;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.stream.*;
import java.io.InputStream;
import java.util.*;

/**
 * XmlRecordReader
 *
 * Lecteur XML en streaming (StAX) qui transforme un XML en records logiques (Map<String,String>).
 *
 * - rootElement : ex <employees>
 * - recordElement : ex <employee> (chaque <employee> = 1 record)
 *
 * Chaque record est lu sans charger tout le fichier en mémoire.
 */
public class XmlRecordReader implements RecordReader {

    /** Reader StAX qui émet des événements (START_ELEMENT, CHARACTERS, END_ELEMENT, ...) */
    private final XMLStreamReader reader;

    /** Schéma XML (rootElement, recordElement, fields) issu de la config DB */
    private final XmlSchema schema;

    /**
     * Constructeur :
     * - ouvre le stream du fichier
     * - crée un XMLStreamReader (StAX)
     * - avance jusqu'au premier START_ELEMENT (le root) et le valide
     */
    public XmlRecordReader(MultipartFile file, XmlSchema schema) throws Exception {
        this.schema = schema;

        // InputStream du fichier uploadé / PathMultipartFile
        InputStream is = file.getInputStream();

        // XMLInputFactory crée le parser StAX
        XMLInputFactory f = XMLInputFactory.newFactory();

        // XMLStreamReader lit l'XML comme un flux d'événements
        this.reader = f.createXMLStreamReader(is);

        // 1) Se positionner sur le 1er élément root (START_ELEMENT)
        // 2) Valider qu'il correspond au rootElement attendu par le schema
        String root = moveToFirstStartElement();
        if (!schema.getRootElement().equals(root)) {
            throw new SchemaValidationException(
                    "Root element <" + root + "> does not match expected <" + schema.getRootElement() + ">"
            );
        }
    }

    /**
     * Avance le reader StAX jusqu'au premier START_ELEMENT.
     * Cela devrait être le root du document XML.
     *
     * @return nom du root element (localName)
     */
    private String moveToFirstStartElement() throws XMLStreamException {
        while (reader.hasNext()) {
            int ev = reader.next();

            // On cherche le premier tag ouvrant
            if (ev == XMLStreamConstants.START_ELEMENT) {
                return reader.getLocalName(); // ex "employees"
            }
        }

        // Aucun élément trouvé => XML invalide
        throw new SchemaValidationException("Invalid XML: no root element found");
    }

    /**
     * Retourne un Iterator<Map<String,String>>.
     * Chaque Map représente un record <recordElement> (ex: <employee>).
     */
    @Override
    public Iterator<Map<String, String>> iterator() {
        return new Iterator<>() {

            /** Index logique du record (sert surtout pour erreurs/logs) */
            int recordIndex = 0;

            /** Buffer du prochain record déjà lu (look-ahead) */
            Map<String, String> next;

            /** Indique si "next" est déjà préparé */
            boolean prepared = false;

            /**
             * hasNext() est "lazy":
             * - si on n'a pas encore préparé next, on lit le prochain record depuis le flux XML
             * - s'il n'y a plus de record, next = null
             */
            @Override
            public boolean hasNext() {
                if (!prepared) {
                    next = readNextRecord();
                    prepared = true;
                }
                return next != null;
            }

            /**
             * next() retourne le record préparé.
             * Si aucun record disponible, NoSuchElementException.
             */
            @Override
            public Map<String, String> next() {
                if (!hasNext()) throw new NoSuchElementException();
                prepared = false; // on consomme next => on devra relire au prochain hasNext()
                return next;
            }

            /**
             * Lit le prochain <recordElement> dans le flux XML et construit la Map.
             *
             * Retourne :
             * - Map si un record est trouvé
             * - null si fin du flux
             */
            private Map<String, String> readNextRecord() {
                try {
                    // On parcourt le flux d'événements XML jusqu'à trouver <recordElement>
                    while (reader.hasNext()) {
                        int ev = reader.next();

                        // Détection du début d'un record (ex: <employee>)
                        if (ev == XMLStreamConstants.START_ELEMENT
                                && schema.getRecordElement().equals(reader.getLocalName())) {

                            recordIndex++;

                            // Map résultat pour ce record
                            Map<String, String> out = new HashMap<>();

                            // Liste des tags attendus selon le mapping (ex: id, firstName, lastName)
                            Set<String> expectedTags = new HashSet<>();
                            for (XmlFieldRule r : schema.getFields()) {
                                expectedTags.add(r.getTag());
                            }

                            /**
                             * depth sert à sortir proprement du recordElement.
                             * On est entré dans <recordElement> => depth=1
                             *
                             * Chaque START_ELEMENT => depth++
                             * Chaque END_ELEMENT   => depth--
                             * Quand depth redevient 0 => on a fermé </recordElement>
                             */
                            int depth = 1;

                            /**
                             * currentTag mémorise le "tag courant" qu'on veut capturer.
                             * Exemple :
                             * - on lit <firstName> => currentTag="firstName"
                             * - on lit CHARACTERS => on stocke le texte dans out
                             * - on lit </firstName> => currentTag=null
                             */
                            String currentTag = null;

                            // Boucle interne : lire jusqu'à la fin du recordElement
                            while (reader.hasNext() && depth > 0) {
                                int e = reader.next();

                                if (e == XMLStreamConstants.START_ELEMENT) {
                                    depth++;

                                    // Nom du tag entrant
                                    String tag = reader.getLocalName();

                                    // On capture seulement les tags qui sont dans le mapping
                                    if (expectedTags.contains(tag)) {
                                        currentTag = tag;
                                    }

                                } else if (e == XMLStreamConstants.CHARACTERS) {

                                    // On ne stocke du texte que si on est dans un tag attendu
                                    if (currentTag != null) {
                                        String text = reader.getText();
                                        if (text != null) {
                                            text = text.trim();

                                            // Ignore texte vide (espaces/retours)
                                            if (!text.isEmpty()) {
                                                // tagToFieldName convertit "firstName" -> "firstName" (ou autre mapping)
                                                // putIfAbsent : garde la 1ère valeur si jamais plusieurs segments CHARACTERS
                                                out.putIfAbsent(tagToFieldName(currentTag), text);
                                            }
                                        }
                                    }

                                } else if (e == XMLStreamConstants.END_ELEMENT) {

                                    // On sort d'un tag
                                    String tag = reader.getLocalName();

                                    // Si on ferme le tag qu'on lisait, on reset currentTag
                                    if (tag.equals(currentTag)) {
                                        currentTag = null;
                                    }

                                    depth--;
                                }
                            }

                            // Assure que tous les champs existent dans la Map (valeur null si non trouvé)
                            for (XmlFieldRule r : schema.getFields()) {
                                out.putIfAbsent(r.getName(), null);
                            }

                            return out; // record complet
                        }
                    }

                    // Fin du fichier XML => plus de record
                    return null;

                } catch (Exception ex) {
                    // On remonte une exception "fonctionnelle" avec contexte recordIndex
                    // (ici le code TYPE_MISMATCH est un peu générique, mais sert à signaler un souci XML)
                    throw new RecordValidationException(
                            ErrorCode.TYPE_MISMATCH,
                            "XML",
                            recordIndex,
                            "XML stream error: " + ex.getMessage()
                    );
                }
            }

            /**
             * Convertit un tag XML en "nom logique" de champ (fieldName).
             *
             * Exemple : si mapping dit tag="firstName" => name="firstName"
             * Mais on pourrait imaginer un mapping tag="first_name" => name="firstName".
             */
            private String tagToFieldName(String tag) {
                for (XmlFieldRule r : schema.getFields()) {
                    if (tag.equals(r.getTag())) return r.getName();
                }
                return tag; // fallback
            }
        };
    }

    /**
     * Fermeture des ressources.
     * Note: XMLStreamReader.close() ferme le reader, mais selon impl, pas toujours le InputStream.
     * Ici on se contente de fermer le reader (classique dans ce type de code).
     */
    @Override
    public void close() throws Exception {
        reader.close();
    }
}
