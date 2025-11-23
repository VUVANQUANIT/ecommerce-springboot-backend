package com.example.demo.user.repository;

import com.example.demo.user.entity.Address;
import com.example.demo.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUser(User user);
    Optional<Address> findByUserAndIsDefaultTrue(User user);
}

