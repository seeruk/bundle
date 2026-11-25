package dev.seeruk.bundle.leaders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A place on a category's leaderboard that a player can hold, e.g. "#2 Miner", whoever's in it. A
 * leader mannequin shows one seat, and {@link Leaders#standing(Seat)} says who's sitting in it.
 *
 * @param category The category's ID.
 * @param rank     The rank, from 1 (the leader).
 */
public record Seat(String category, int rank) {
    public static final Codec<Seat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("category").forGetter(Seat::category),
        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("rank").forGetter(Seat::rank)
    ).apply(instance, Seat::new));
}
