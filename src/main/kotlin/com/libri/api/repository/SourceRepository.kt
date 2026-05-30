package com.libri.api.repository

import com.libri.api.entity.Source
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SourceRepository : JpaRepository<Source, String> {
    fun findAllByEnabledTrue(): List<Source>
}
