package com.backend.backend_pfe.enums;

/**
 * Catégories de notifications, communes aux trois profils.
 * Le frontend mappe ces types vers des libellés/couleurs par rôle.
 */
public enum TypeNotification {
    ANOMALIE,      // anomalie / conflit détecté
    AFFECTATION,   // nouvelle affectation à un projet
    PROJET,        // évènement lié à un projet (création, import prévision)
    SYSTEME        // message système générique
}
