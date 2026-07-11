#pragma once
#include <string>
#include "deck.h"
#include "hand.h"

using namespace std;

enum class GameState {
    BETTING,       // waiting on placeBet()
    PLAYER_TURN,   // player can hit or stand
    ROUND_OVER     // settled, call newRound() to keep going
};

enum class RoundResult {
    NONE,
    PLAYER_BLACKJACK,
    PLAYER_WIN,
    PLAYER_BUST,
    DEALER_BUST,
    DEALER_WIN,
    PUSH
};

struct RoundOutcome {
    RoundResult result = RoundResult::NONE;
    int walletDelta = 0;   // minutes gained (+) or lost (-) this round
    int walletAfter = 0;   // balance once everything settles
    string description;
};

class BlackjackGame {
public:
    // startingMinutes is your base allowance, e.g. 10 for Instagram
    explicit BlackjackGame(int startingMinutes)
        : wallet(startingMinutes), currentBet(0), state(GameState::BETTING) {}

    int getWallet() const { return wallet; }
    GameState getState() const { return state; }
    const Hand& getPlayerHand() const { return playerHand; }
    const Hand& getDealerHand() const { return dealerHand; }
    int getCurrentBet() const { return currentBet; }

    // bet is in minutes, anything from 1 up to whatever's left in the
    // wallet -- gamble as much of it as you want
    bool placeBet(int minutes) {
        if (state != GameState::BETTING) return false;
        if (minutes <= 0 || minutes > wallet) return false;
        currentBet = minutes;
        return true;
    }

    // deals the opening two cards each and checks for naturals right
    // away. if nobody has blackjack, state moves to PLAYER_TURN and
    // it's on you to hit or stand
    RoundOutcome dealInitial() {
        if (state != GameState::BETTING || currentBet <= 0) {
            return RoundOutcome{ RoundResult::NONE, 0, wallet, "Place a bet first." };
        }

        playerHand.clear();
        dealerHand.clear();

        playerHand.addCard(deck.dealCard());
        dealerHand.addCard(deck.dealCard());
        playerHand.addCard(deck.dealCard());
        dealerHand.addCard(deck.dealCard());

        state = GameState::PLAYER_TURN;

        if (playerHand.isBlackjack() || dealerHand.isBlackjack()) {
            return settle();
        }
        return RoundOutcome{ RoundResult::NONE, 0, wallet, "Cards dealt." };
    }

    RoundOutcome hit() {
        if (state != GameState::PLAYER_TURN) {
            return RoundOutcome{ RoundResult::NONE, 0, wallet, "Not your turn." };
        }
        playerHand.addCard(deck.dealCard());
        if (playerHand.isBust() || playerHand.value() == 21) {
            return settle();
        }
        return RoundOutcome{ RoundResult::NONE, 0, wallet, "Hit: " + playerHand.toString() };
    }

    RoundOutcome stand() {
        if (state != GameState::PLAYER_TURN) {
            return RoundOutcome{ RoundResult::NONE, 0, wallet, "Not your turn." };
        }
        return settle();
    }

    // wallet carries over, just resets the bet/hands for another round
    void newRound() {
        currentBet = 0;
        state = GameState::BETTING;
    }

    bool isBrokeOut() const { return wallet <= 0; }

    // testing helper only -- forces the wallet to a specific value
    // regardless of round state. Not reachable through normal play.
    void debugSetWallet(int minutes) {
        wallet = minutes;
    }

private:
    int wallet;
    Deck deck;
    Hand playerHand;
    Hand dealerHand;
    int currentBet;
    GameState state;

    // dealer stands on all 17s (soft included), then we compare hands
    // and actually move minutes in or out of the wallet
    RoundOutcome settle() {
        RoundOutcome outcome;
        outcome.result = RoundResult::NONE;

        bool playerBJ = playerHand.isBlackjack();
        bool dealerBJ = dealerHand.isBlackjack();

        if (playerHand.isBust()) {
            outcome.result = RoundResult::PLAYER_BUST;
            wallet -= currentBet;
            outcome.walletDelta = -currentBet;
            outcome.description = "Bust with " + to_string(playerHand.value()) +
                                   ". Lost " + to_string(currentBet) + " min.";
        } else if (playerBJ || dealerBJ) {
            if (playerBJ && dealerBJ) {
                outcome.result = RoundResult::PUSH;
                outcome.walletDelta = 0;
                outcome.description = "Both blackjack. Push.";
            } else if (playerBJ) {
                // blackjack pays 3:2
                int winnings = currentBet + currentBet / 2;
                wallet += winnings;
                outcome.result = RoundResult::PLAYER_BLACKJACK;
                outcome.walletDelta = winnings;
                outcome.description = "Blackjack! Won " + to_string(winnings) + " min.";
            } else {
                wallet -= currentBet;
                outcome.result = RoundResult::DEALER_WIN;
                outcome.walletDelta = -currentBet;
                outcome.description = "Dealer blackjack. Lost " + to_string(currentBet) + " min.";
            }
        } else {
            while (dealerHand.value() < 17) {
                dealerHand.addCard(deck.dealCard());
            }

            int p = playerHand.value();
            int d = dealerHand.value();

            if (dealerHand.isBust()) {
                wallet += currentBet;
                outcome.result = RoundResult::DEALER_BUST;
                outcome.walletDelta = currentBet;
                outcome.description = "Dealer busts with " + to_string(d) +
                                       ". Won " + to_string(currentBet) + " min.";
            } else if (p > d) {
                wallet += currentBet;
                outcome.result = RoundResult::PLAYER_WIN;
                outcome.walletDelta = currentBet;
                outcome.description = to_string(p) + " beats " + to_string(d) +
                                       ". Won " + to_string(currentBet) + " min.";
            } else if (p < d) {
                wallet -= currentBet;
                outcome.result = RoundResult::DEALER_WIN;
                outcome.walletDelta = -currentBet;
                outcome.description = to_string(d) + " beats " + to_string(p) +
                                       ". Lost " + to_string(currentBet) + " min.";
            } else {
                outcome.result = RoundResult::PUSH;
                outcome.walletDelta = 0;
                outcome.description = "Push at " + to_string(p) + ".";
            }
        }

        outcome.walletAfter = wallet;
        state = GameState::ROUND_OVER;
        return outcome;
    }
};
