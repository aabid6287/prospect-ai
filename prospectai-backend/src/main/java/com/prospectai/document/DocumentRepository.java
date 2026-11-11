package com.prospectai.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<UploadedDocument, String> {

    List<UploadedDocument> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

    Optional<UploadedDocument> findByIdAndOwnerId(String id, String ownerId);

    long countByOwnerId(String ownerId);

    long countByOwnerIdAndStatus(String ownerId, DocumentStatus status);
}
