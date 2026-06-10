package com.pspd.backend.user.repository;

import com.pspd.backend.user.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, String> {
}
