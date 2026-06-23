package com.pspd.backend.user.dto;

import java.util.List;

/** Remplace la liste des services (approuvés) proposés par le prestataire. */
public record SetServicesRequest(List<String> serviceIds) {}
