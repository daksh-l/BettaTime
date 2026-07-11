#pragma once
#include <string>

using namespace std;

enum class Suit { CLUBS, DIAMONDS, HEARTS, SPADES };
enum class Rank {
    TWO = 2, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN,
    JACK, QUEEN, KING, ACE
};

struct Card {
    Rank rank;
    Suit suit;

    // face cards are worth 10, ace is 11 for now -- Hand deals with
    // knocking aces down to 1 if that would bust things
    int baseValue() const {
        switch (rank) {
            case Rank::JACK:
            case Rank::QUEEN:
            case Rank::KING:
                return 10;
            case Rank::ACE:
                return 11;
            default:
                return static_cast<int>(rank);
        }
    }

    bool isAce() const { return rank == Rank::ACE; }

    string toString() const {
        static const char* rankNames[] = {
            "", "", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"
        };
        static const char* suitNames[] = { "C", "D", "H", "S" };

        string r = (rank == Rank::ACE) ? "A" : rankNames[static_cast<int>(rank)];
        return r + suitNames[static_cast<int>(suit)];
    }
};
