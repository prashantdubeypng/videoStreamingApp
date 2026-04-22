package com.animestream.upload.repository;

import com.animestream.upload.entity.VideoUpload;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VideoUploadRepository extends JpaRepository<VideoUpload, UUID> {

    Optional<VideoUpload> findByUploadId(String uploadId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VideoUpload v where v.id = :id and v.uploadId = :uploadId")
    Optional<VideoUpload> findForUpdate(@Param("id") UUID id, @Param("uploadId") String uploadId);
}
