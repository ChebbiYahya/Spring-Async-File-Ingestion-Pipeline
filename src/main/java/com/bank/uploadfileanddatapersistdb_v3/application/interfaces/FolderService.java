package com.bank.uploadfileanddatapersistdb_v3.application.interfaces;
// Interface pour gerer les fichiers dans DATA_*

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

public interface FolderService {
    void ensureFoldersExist(String configId);

    List<String> listIn(String configId);

    List<String> listTreatment(String configId);

    List<String> listBackup(String configId);

    List<String> listFailed(String configId);

    Path saveToInFolder(MultipartFile file, String configId);

    boolean deleteFromIn(String configId, String fileName);

    List<String> deleteAllFromIn(String configId);

    Path moveOneFromInToTreatmentWithTimestamp(String configId);

    Path moveTreatmentToBackup(String configId, Path treatmentFile);

    Path moveTreatmentToFailed(String configId, Path treatmentFile);
}
