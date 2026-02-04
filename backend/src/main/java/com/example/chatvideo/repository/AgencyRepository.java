package com.example.chatvideo.repository;

import com.example.chatvideo.model.AgencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgencyRepository extends JpaRepository<AgencyEntity, Integer> {

    @Override
    Optional<AgencyEntity> findById(Integer id);
}
