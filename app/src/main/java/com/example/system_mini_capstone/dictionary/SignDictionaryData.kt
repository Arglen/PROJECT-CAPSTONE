package com.example.system_mini_capstone.dictionary

enum class SignAccent { PURPLE, BLUE, GREEN, AMBER }

data class SignStep(
    val number: String,
    val title: String,
    val description: String
)

data class SignItem(
    val id: String,
    val indexNumber: String,
    val category: String,
    val name: String,
    val description: String,
    val accent: SignAccent,
    val steps: List<SignStep>
)

object SignRepository {
    val signs = listOf(
        SignItem(
            id = "hello",
            indexNumber = "01",
            category = "Greetings",
            name = "Hello",
            description = "Start a friendly conversation.",
            accent = SignAccent.PURPLE,
            steps = listOf(
                SignStep("1", "Raise your hand", "Bring an open hand near your forehead, palm facing outward."),
                SignStep("2", "Move outward", "Move your hand gently away from your forehead, like a small salute."),
                SignStep("3", "Keep it natural", "Use a relaxed motion and a friendly facial expression.")
            )
        ),
        SignItem(
            id = "thank_you",
            indexNumber = "02",
            category = "Everyday kindness",
            name = "Thank you",
            description = "Show a little appreciation.",
            accent = SignAccent.BLUE,
            steps = listOf(
                SignStep("1", "Start at your chin", "Hold your fingers together and touch your fingertips near your chin."),
                SignStep("2", "Move toward them", "Move your flat hand forward and slightly downward, away from your face."),
                SignStep("3", "Show appreciation", "Keep your palm facing upward as your hand moves outward.")
            )
        ),
        SignItem(
            id = "please",
            indexNumber = "03",
            category = "Everyday kindness",
            name = "Please",
            description = "Ask with kindness and respect.",
            accent = SignAccent.GREEN,
            steps = listOf(
                SignStep("1", "Open your hand", "Keep your fingers together with your palm flat."),
                SignStep("2", "Place it on your chest", "Rest your palm gently against the center of your chest."),
                SignStep("3", "Make a small circle", "Move your hand in a small circular motion against your chest.")
            )
        ),
        SignItem(
            id = "sorry",
            indexNumber = "04",
            category = "Everyday kindness",
            name = "Sorry",
            description = "A thoughtful way to apologize.",
            accent = SignAccent.AMBER,
            steps = listOf(
                SignStep("1", "Make a fist", "Close your hand into a fist with your thumb resting outside."),
                SignStep("2", "Bring it to your chest", "Place your fist gently against the center of your chest."),
                SignStep("3", "Move in a circle", "Rub your fist in a small circle, with an apologetic expression.")
            )
        )
    )

    fun getSignById(id: String): SignItem? = signs.firstOrNull { it.id == id }
}
