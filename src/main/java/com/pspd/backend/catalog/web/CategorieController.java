package com.pspd.backend.catalog.web;

import com.pspd.backend.catalog.dto.CategorieResponse;
import com.pspd.backend.catalog.dto.CreateCategorieRequest;
import com.pspd.backend.catalog.dto.ServiceResponse;
import com.pspd.backend.catalog.service.AdminCatalogService;
import com.pspd.backend.catalog.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategorieController {

    private final CatalogService      catalogService;
    private final AdminCatalogService adminCatalogService;

    /** Arbre des catégories actives — public (B1). */
    @GetMapping
    public List<CategorieResponse> all() {
        return catalogService.categoriesTree();
    }

    /** Services actifs d'une catégorie — public (B1). */
    @GetMapping("/{id}/services")
    public List<ServiceResponse> services(@PathVariable String id) {
        return catalogService.servicesOfCategorie(id);
    }

    /** Création d'une catégorie — admin (B5). */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategorieResponse> create(@Valid @RequestBody CreateCategorieRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCatalogService.createCategorie(req));
    }
}
