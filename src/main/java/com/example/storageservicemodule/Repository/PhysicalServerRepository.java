package com.example.storageservicemodule.Repository;

import com.example.storageservicemodule.Bean.PhysicalServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhysicalServerRepository extends JpaRepository<PhysicalServer, Integer> {
}