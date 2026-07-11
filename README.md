# BettaTime 🃏⏳

**BettaTime** is an Android-native digital wellbeing application that introduces behavioral game theory to time management. Instead of rigid app blockers, users engage with a probability-based risk-reward engine (Blackjack) to dynamically win or restrict their allocated screentime quotas.

## 🛠️ Tech Stack & Architecture

*   **Frontend & UI:** Kotlin (Jetpack Compose) for a modern, fluid Android user interface and state observation.
*   **Core Logic Engine:** C++ (via JNI / Android NDK) to handle high-performance probability matrices, card-dealing algorithms, and game state management securely at the native level.
*   **Data Architecture:** Seamless JNI data bridging between the high-level Kotlin UI layer and the optimized low-level C++ backend.
