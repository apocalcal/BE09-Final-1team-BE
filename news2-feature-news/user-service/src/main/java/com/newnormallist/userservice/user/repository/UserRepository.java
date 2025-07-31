package com.newnormallist.userservice.user.repository;

import com.newnormallist.userservice.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
