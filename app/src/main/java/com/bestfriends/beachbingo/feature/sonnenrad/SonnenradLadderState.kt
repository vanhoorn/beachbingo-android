package com.bestfriends.beachbingo.feature.sonnenrad

sealed interface SonnenradLadderState {

    /** Vor dem ersten Enthüllungsergebnis – kein aktiver Zustand */
    data object Idle : SonnenradLadderState

    /**
     * Runde läuft.
     * [securedStep] ist die zuletzt GESICHERTE Stufe (1–6).
     * Invariante: securedStep kann in keinem Codepfad sinken.
     * [isTargetZone] gilt nur während der Timing-Herausforderung.
     */
    data class Active(
        val securedStep: Int,
        val isTargetZone: Boolean = false,
    ) : SonnenradLadderState

    /**
     * Runde abgeschlossen.
     * [finalStep] ist immer ≥ dem ursprünglichen Einstieg – nie 0, außer bei keinem Treffer.
     */
    data class Finished(
        val finalStep: Int,
        val pointsAwarded: Int,
    ) : SonnenradLadderState
}
