package com.financialgps.testfixture;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Test-only owned resource (plan Deviation #2) — models a generic owned row with lifecycle. */
@Entity
@Table(name = "test_owned_resource")
public class TestOwnedResourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected TestOwnedResourceEntity() {
    }

    public TestOwnedResourceEntity(UUID ownerId, String label) {
        this.ownerId = ownerId;
        this.label = label;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }
}
