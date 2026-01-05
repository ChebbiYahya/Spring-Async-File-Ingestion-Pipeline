package com.bank.uploadfileanddatapersistdb_v3.application.interfaces;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

public interface FolderService {
    void ensureFoldersExist();

    List<String> listIn();

    List<String> listTreatment();

    List<String> listBackup();

    List<String> listFailed();

    Path saveToInFolder(MultipartFile file);

    boolean deleteFromIn(String fileName);

    List<String> deleteAllFromIn();

    Path moveOneFromInToTreatmentWithTimestamp();

    Path moveTreatmentToBackup(Path treatmentFile);

    Path moveTreatmentToFailed(Path treatmentFile);
}
