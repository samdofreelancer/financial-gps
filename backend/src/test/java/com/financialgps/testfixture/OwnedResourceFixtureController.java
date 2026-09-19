package com.financialgps.testfixture;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.platform.security.CurrentOwnerProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Test-fixture owned-resource controller (plan Deviation #2): proves ownership enforcement now,
 * WITHOUT implementing any production financial controller (forbidden for 007). Mirrors the
 * lifecycle every financial resource will follow: Create → Update → Archive → Delete.
 */
@RestController
@RequestMapping("/api/test/owned")
public class OwnedResourceFixtureController {

    public record CreateRequest(String label) {
    }

    public record UpdateRequest(String label) {
    }

    public record OwnedResourceView(UUID id, UUID ownerId, String label, boolean archived) {
    }

    private final TestOwnedResourceRepository repository;
    private final CurrentOwnerProvider currentOwnerProvider;

    public OwnedResourceFixtureController(TestOwnedResourceRepository repository,
                                          CurrentOwnerProvider currentOwnerProvider) {
        this.repository = repository;
        this.currentOwnerProvider = currentOwnerProvider;
    }

    @PostMapping
    public ResponseEntity<OwnedResourceView> create(@RequestBody CreateRequest request) {
        UUID ownerId = currentOwnerProvider.requireCurrentOwner().value();
        TestOwnedResourceEntity saved = repository.save(new TestOwnedResourceEntity(ownerId, request.label()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toView(saved));
    }

    @GetMapping
    public List<OwnedResourceView> list() {
        UUID ownerId = currentOwnerProvider.requireCurrentOwner().value();
        return repository.findByOwnerIdOrderByLabel(ownerId).stream().map(this::toView).toList();
    }

    @GetMapping("/{id}")
    public OwnedResourceView get(@PathVariable UUID id) {
        return repository.findByIdAndOwnerId(id, currentOwnerProvider.requireCurrentOwner().value())
                .map(this::toView)
                .orElseThrow(ResourceNotFoundException::new);
    }

    @PatchMapping("/{id}")
    public OwnedResourceView update(@PathVariable UUID id, @RequestBody UpdateRequest request) {
        TestOwnedResourceEntity entity = repository
                .findByIdAndOwnerId(id, currentOwnerProvider.requireCurrentOwner().value())
                .orElseThrow(ResourceNotFoundException::new);
        entity.setLabel(request.label());
        return toView(repository.save(entity));
    }

    @PostMapping("/{id}/archive")
    public OwnedResourceView archive(@PathVariable UUID id) {
        TestOwnedResourceEntity entity = repository
                .findByIdAndOwnerId(id, currentOwnerProvider.requireCurrentOwner().value())
                .orElseThrow(ResourceNotFoundException::new);
        entity.setArchived(true);
        return toView(repository.save(entity));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID ownerId = currentOwnerProvider.requireCurrentOwner().value();
        if (repository.deleteByIdAndOwnerId(id, ownerId) == 0) {
            throw new ResourceNotFoundException();
        }
        return ResponseEntity.noContent().build();
    }

    private OwnedResourceView toView(TestOwnedResourceEntity entity) {
        return new OwnedResourceView(entity.getId(), entity.getOwnerId(), entity.getLabel(), entity.isArchived());
    }
}
