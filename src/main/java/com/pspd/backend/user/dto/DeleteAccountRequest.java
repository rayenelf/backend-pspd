package com.pspd.backend.user.dto;

/** Confirmation de suppression de compte. password requis pour les comptes locaux. */
public record DeleteAccountRequest(String password) {}
