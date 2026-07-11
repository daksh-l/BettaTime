#pragma once
#include <vector>
#include <string>
#include "card.h"

using namespace std;

class Hand {
public:
    void addCard(const Card& c) { cards.push_back(c); }

    void clear() { cards.clear(); }

    const vector<Card>& getCards() const { return cards; }

    // best value <= 21 if there is one, otherwise whatever it busts at.
    // aces start as 11 and get knocked down to 1 one at a time until
    // we're under 21 or out of aces
    int value() const {
        int total = 0;
        int aces = 0;
        for (const auto& c : cards) {
            total += c.baseValue();
            if (c.isAce()) aces++;
        }
        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }
        return total;
    }

    bool isBust() const { return value() > 21; }

    // only really means anything on the first two cards
    bool isBlackjack() const {
        return cards.size() == 2 && value() == 21;
    }

    bool isSoft() const {
        int total = 0;
        int aces = 0;
        for (const auto& c : cards) {
            total += c.baseValue();
            if (c.isAce()) aces++;
        }
        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }
        return aces > 0;
    }

    string toString() const {
        string s;
        for (size_t i = 0; i < cards.size(); ++i) {
            if (i) s += " ";
            s += cards[i].toString();
        }
        return s;
    }

private:
    vector<Card> cards;
};
