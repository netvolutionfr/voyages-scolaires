package fr.siovision.voyages.domain.model;

/** Ordre pédagogique : 3e < 2nde < 1ere < Tle < BTS1 < BTS2 < B3 < B4 < B5 */
public enum YearTag {
    TROISIEME_PRO,
    LYCEE_PRO,
    SECONDE,
    SECONDE_PRO,
    PREMIERE,
    PREMIERE_PRO,
    TERMINALE,
    TERMINALE_PRO,
    BTS1,
    BTS2,
    FC,
    PREPA,
    SUPERIEUR,
}