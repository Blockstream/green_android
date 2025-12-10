package com.blockstream.domain.boltz

/**
 * Aggregates Boltz-related domain use cases behind a single injectable entry point.
 *
 * Wired via Koin, this holder is injected into view models and other domain services so they can
 * access swap functionality without injecting each use case separately.
 *
 * Provided operations:
 * - [createReverseSubmarineSwapUseCase]: Create a Lightning invoice paid by an on‑chain deposit
 *   (Reverse Submarine Swap).
 * - [createNormalSubmarineSwapUseCase]: Prepare a payment for a BOLT11 invoice using an
 *   on‑chain transaction (Normal Submarine Swap).
 * - [handleSwapEventsUseCase]: Connect to LWK and let it process swap events for a period of
 *   time (used by background workers).
 * - [isSwapsEnabledUseCase]: Check if swaps are enabled for a given wallet.
 * - [getWalletFromSwapUseCase]: Resolve a stored swap to its associated wallet, if available.
 */
class BoltzUseCase constructor(
    val createReverseSubmarineSwapUseCase: CreateReverseSubmarineSwapUseCase,
    val createNormalSubmarineSwapUseCase: CreateNormalSubmarineSwapUseCase,
    val handleSwapEventsUseCase: HandleSwapEventsUseCase,
    val isSwapsEnabledUseCase: IsSwapsEnabledUseCase,
    val getWalletFromSwapUseCase: GetWalletFromSwapUseCase,
    val isAddressSwappableUseCase: IsAddressSwappableUseCase
)
