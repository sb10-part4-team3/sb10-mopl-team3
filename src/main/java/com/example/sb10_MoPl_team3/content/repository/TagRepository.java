package com.example.sb10_MoPl_team3.content.repository;

import com.example.sb10_MoPl_team3.content.entity.Tag;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, UUID> {

  Optional<Tag> findByName(String name);

}
