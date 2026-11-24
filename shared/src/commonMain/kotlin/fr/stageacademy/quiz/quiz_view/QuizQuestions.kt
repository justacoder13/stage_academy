package fr.stageacademy.quiz.quiz_view

val quizQuestions: List<Question> = listOf(
    Question(
        text = "Quel outil garde l’historique du code ?",
        choices = listOf("Git", "Android", "Compose"),
        correctAnswer = "Git",
    ),
    Question(
        text = "Quel composant Compose affiche du texte ?",
        choices = listOf("Button", "Text", "Column"),
        correctAnswer = "Text",
    ),
    Question(
        text = "Quel mot-clé déclare une fonction en Kotlin ?",
        choices = listOf("fun", "val", "if"),
        correctAnswer = "fun",
    ),
    Question(
        text = "Quel type représente vrai ou faux ?",
        choices = listOf("String", "Boolean", "Int"),
        correctAnswer = "Boolean",
    ),
    Question(
        text = "Quelle commande Git montre les fichiers modifiés ?",
        choices = listOf("git log", "git status", "git init"),
        correctAnswer = "git status",
    ),
    Question(
        text = "Quelle commande Git enregistre les modifications préparées dans l’historique ?",
        choices = listOf("git commit", "git status", "git branch"),
        correctAnswer = "git commit",
    ),
    Question(
        text = "Quel composant Compose place les éléments côte à côte ?",
        choices = listOf("Box", "Column", "Row"),
        correctAnswer = "Row",
    ),
    Question(
        text = "Quel composant Compose sert à créer un bouton sur lequel appuyer ?",
        choices = listOf("Text", "Button", "Column"),
        correctAnswer = "Button",
    ),
    Question(
        text = "En Kotlin, quel mot-clé déclare une variable dont on peut changer la valeur ?",
        choices = listOf("val", "fun", "var"),
        correctAnswer = "var",
    ),
    Question(
        text = "En Kotlin, quel symbole vérifie si deux valeurs sont égales ?",
        choices = listOf("==", "=", "!="),
        correctAnswer = "==",
    ),
    Question(
        text = "Quelle commande Git crée un dépôt dans le dossier actuel ?",
        choices = listOf("git init", "git log", "git diff"),
        correctAnswer = "git init",
    ),
    Question(
        text = "Quelle commande Git permet de consulter l’historique des commits ?",
        choices = listOf("git add", "git log", "git status"),
        correctAnswer = "git log",
    ),
    Question(
        text = "Quel composant Compose empile les éléments verticalement ?",
        choices = listOf("Row", "Text", "Column"),
        correctAnswer = "Column",
    ),
    Question(
        text = "En Kotlin, quel mot-clé permet de vérifier une condition ?",
        choices = listOf("if", "var", "fun"),
        correctAnswer = "if",
    ),
    Question(
        text = "En Kotlin, quel type sert à représenter du texte ?",
        choices = listOf("Int", "String", "Boolean"),
        correctAnswer = "String",
    ),
)
