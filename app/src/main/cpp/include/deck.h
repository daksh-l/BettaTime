#pragma once
#include <vector>
#include <random>
#include <algorithm>
#include "card.h"

using namespace std;

class Deck {
public:
    Deck() : rng(randomSeed()) {
        reset();
    }

    void reset() {
        cards.clear();
        cards.reserve(52);
        for (int s = 0; s < 4; ++s) {
            for (int r = 2; r <= 14; ++r) {
                cards.push_back(Card{ static_cast<Rank>(r), static_cast<Suit>(s) });
            }
        }
        shuffle();
    }

    void shuffle() {
        std::shuffle(cards.begin(), cards.end(), rng);
    }

    Card dealCard() {
        if (cards.empty()) {
            // ran out mid-session, just start a fresh shuffled shoe
            reset();
        }
        Card c = cards.back();
        cards.pop_back();
        return c;
    }

    size_t cardsRemaining() const { return cards.size(); }

private:
    vector<Card> cards;
    mt19937 rng;

    static unsigned int randomSeed() {
        random_device rd;
        return rd();
    }
};
