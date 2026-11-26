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
    Question(
        text = "Dans quel domaine l’entreprise MWM est-elle spécialisée ?",
        choices = listOf("Le développement d’applications mobiles", "La fabrication d’ordinateurs", "La cybersécurité"),
        correctAnswer = "Le développement d’applications mobiles",
    ),
    Question(
        text = "Dans quelle ville le studio à l’origine de MWM a-t-il été fondé ?",
        choices = listOf("Lyon", "Paris", "Bordeaux"),
        correctAnswer = "Paris",
    ),
    Question(
        text = "En quelle année le studio à l’origine de MWM a-t-il été fondé ?",
        choices = listOf("2009", "2015", "2020"),
        correctAnswer = "2009",
    ),
    Question(
        text = "Qui sont les fondateurs du studio à l’origine de MWM ?",
        choices = listOf("Steve Jobs et Steve Wozniak", "Larry Page et Sergey Brin", "Jean-Baptiste Hironde et Nicolas Dupré"),
        correctAnswer = "Jean-Baptiste Hironde et Nicolas Dupré",
    ),
    Question(
        text = "Quelle est la première application lancée par MWM en 2012 ?",
        choices = listOf("Spotify", "edjing", "TikTok"),
        correctAnswer = "edjing",
    ),
    Question(
        text = "À quoi sert l’application edjing développée par MWM ?",
        choices = listOf("À mixer de la musique comme un DJ", "À retoucher des photos", "À apprendre la programmation"),
        correctAnswer = "À mixer de la musique comme un DJ",
    ),
    Question(
        text = "Quel était l’un des anciens noms de MWM ?",
        choices = listOf("Android World", "Mobile Tech France", "Music World Media"),
        correctAnswer = "Music World Media",
    ),
    Question(
        text = "Quels types d’applications MWM développe-t-elle notamment ?",
        choices = listOf("Uniquement des applications bancaires", "Des applications musicales et créatives", "Uniquement des applications de messagerie"),
        correctAnswer = "Des applications musicales et créatives",
    ),
    Question(
        text = "Combien de téléchargements les applications de MWM ont-elles dépassé au total ?",
        choices = listOf("1 milliard", "10 millions", "50 millions"),
        correctAnswer = "1 milliard",
    ),
    Question(
        text = "Quelle technologie MWM utilise-t-elle pour faciliter la création d’applications mobiles ?",
        choices = listOf("L’impression 3D", "La réalité virtuelle uniquement", "L’intelligence artificielle"),
        correctAnswer = "L’intelligence artificielle",
    ),
    Question(
        text = "Qui définit la stratégie technique d’une entreprise numérique ?",
        choices = listOf("Le CTO", "Le responsable de la publicité", "Le Product Designer"),
        correctAnswer = "Le CTO",
    ),
    Question(
        text = "L’application ralentit quand beaucoup de joueurs se connectent. Que peut décider le CTO ?",
        choices = listOf("Changer le prix de l’abonnement", "Faire tester la capacité du système", "Modifier le logo"),
        correctAnswer = "Faire tester la capacité du système",
    ),
    Question(
        text = "Quel choix peut générer des revenus dans une application gratuite ?",
        choices = listOf("Réduire le temps de chargement", "Changer la couleur du menu", "Vendre une option pour retirer les publicités"),
        correctAnswer = "Vendre une option pour retirer les publicités",
    ),
    Question(
        text = "Quel indicateur mesure la part des utilisateurs qui achètent une option payante ?",
        choices = listOf("Le taux de conversion vers l’achat", "Le nombre de téléchargements", "La durée moyenne des sessions"),
        correctAnswer = "Le taux de conversion vers l’achat",
    ),
    Question(
        text = "Quelle mission peut être confiée à un data scientist ?",
        choices = listOf("Dessiner le menu du quiz", "Construire un modèle qui prédit si les joueurs reviendront", "Configurer les mises à jour de l’application"),
        correctAnswer = "Construire un modèle qui prédit si les joueurs reviendront",
    ),
    Question(
        text = "Sur quelles données un data scientist teste-t-il la capacité d’un modèle à faire de nouvelles prédictions ?",
        choices = listOf("Seulement les exemples bien prédits", "Les mêmes données que pour l’entraînement", "Des données mises de côté pendant l’entraînement"),
        correctAnswer = "Des données mises de côté pendant l’entraînement",
    ),
    Question(
        text = "Quelle mission relève du data engineer ?",
        choices = listOf("Automatiser la collecte et la préparation des données", "Choisir le prix d’un abonnement", "Dessiner les écrans du quiz"),
        correctAnswer = "Automatiser la collecte et la préparation des données",
    ),
    Question(
        text = "Chaque partie doit apparaître une seule fois dans les données. Quel contrôle le data engineer peut-il ajouter ?",
        choices = listOf("Exiger un score différent à chaque partie", "Vérifier que chaque identifiant de partie est unique", "Garder uniquement la dernière partie de chaque joueur"),
        correctAnswer = "Vérifier que chaque identifiant de partie est unique",
    ),
    Question(
        text = "L’équipe VMA prépare souvent le même type de rapport. Quel outil lui ferait gagner du temps ?",
        choices = listOf("Un dossier supplémentaire pour les anciens rapports", "Un formulaire à recopier chaque semaine", "Un générateur qui remplit le rapport à partir des données disponibles"),
        correctAnswer = "Un générateur qui remplit le rapport à partir des données disponibles",
    ),
    Question(
        text = "Comment vérifier qu’un nouvel outil aide vraiment l’équipe VMA ?",
        choices = listOf("Observer l’équipe sur une tâche habituelle et comparer le temps et les erreurs", "Compter les boutons de l’outil", "Mesurer la taille du fichier d’installation"),
        correctAnswer = "Observer l’équipe sur une tâche habituelle et comparer le temps et les erreurs",
    ),
    Question(
        text = "L’équipe ne peut développer qu’une seule nouveauté. Comment le Product Manager (PM) peut-il décider ?",
        choices = listOf("Choisir la dernière idée proposée", "Comparer les besoins des joueurs, l’impact attendu et l’effort", "Choisir systématiquement la nouveauté la plus rapide à développer"),
        correctAnswer = "Comparer les besoins des joueurs, l’impact attendu et l’effort",
    ),
    Question(
        text = "Quel indicateur aide le PM à savoir si davantage de nouveaux joueurs finissent leur premier quiz ?",
        choices = listOf("Le nombre d’écrans de l’application", "Le nombre de questions disponibles", "La part des nouveaux joueurs qui terminent leur premier quiz"),
        correctAnswer = "La part des nouveaux joueurs qui terminent leur premier quiz",
    ),
    Question(
        text = "Que peut préparer un Product Designer pour tester un nouveau menu avant son développement ?",
        choices = listOf("Un prototype cliquable du menu", "Un tableau des ventes", "Un journal des erreurs du serveur"),
        correctAnswer = "Un prototype cliquable du menu",
    ),
    Question(
        text = "Comment le Product Designer peut-il vérifier si le bouton Recommencer est facile à trouver ?",
        choices = listOf("Compter les boutons sur l’écran", "Observer des joueurs qui essaient de relancer une partie sans leur montrer le bouton", "Demander aux joueurs leur couleur préférée"),
        correctAnswer = "Observer des joueurs qui essaient de relancer une partie sans leur montrer le bouton",
    ),
)
