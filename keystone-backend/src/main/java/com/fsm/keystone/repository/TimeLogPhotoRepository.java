package com.fsm.keystone.repository;

import com.fsm.keystone.entity.TimeLogPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TimeLogPhotoRepository extends JpaRepository<TimeLogPhoto, Long> {

    @Query("""
            select p from TimeLogPhoto p
            join fetch p.timeLog t
            join fetch t.technician
            where p.id = :id
            """)
    Optional<TimeLogPhoto> findByIdWithDetails(@Param("id") Long id);
}
