# Stage Academy — Mon premier quiz

Un quiz en français pour découvrir la programmation pendant un stage de
troisième. Android et le bureau utilisent les mêmes écrans, animations et règles
grâce à Kotlin Multiplatform et Compose Multiplatform.

## Ouvrir le projet

1. Ouvrir ce dossier dans Android Studio (version compatible avec AGP 8.10).
2. Utiliser le JDK intégré à Android Studio (JDK 17 minimum ; JDK 21 convient).
3. Installer Android SDK Platform 36 et accepter les licences via le SDK Manager.
4. Laisser Android Studio synchroniser Gradle, puis lancer `app` sur un émulateur
   ou un téléphone Android 8.0 (API 26) ou supérieur.

Android Studio configure le chemin du SDK dans `local.properties`, qui reste local
à chaque ordinateur. En ligne de commande, on peut aussi définir `ANDROID_HOME`.
Le premier lancement nécessite Internet pour télécharger les dépendances.

## Compiler et vérifier

À la racine du projet, avec `JAVA_HOME` pointant vers le JDK :

```sh
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
./gradlew :shared:jvmTest
./gradlew :desktop:installDist
```

Sous Windows, utiliser `gradlew.bat` à la place de `./gradlew`.
Pour jouer sur ordinateur avec un JDK 17 ou supérieur, lancer
`gradlew.bat :desktop:run` (Windows) ou `./gradlew :desktop:run` (macOS/Linux).
Le bouton galerie ouvre le sélecteur de fichiers sur ordinateur. Le bouton de
prise de vue ouvre la webcam si elle est disponible. Chaque appareil conserve
son propre profil, ses points, ses réglages et ses secrets.
L’APK de développement se trouve dans `app/build/outputs/apk/debug/app-debug.apk`.
Sous Windows, si le projet est dans OneDrive, Gradle écrit les fichiers générés
dans `%LOCALAPPDATA%\StageAcademy\build\<identifiant-du-projet>\<module>`.
Dans le dossier du module `app`, l’APK se trouve sous `outputs\apk\debug`.

## Repérer les fichiers

- `shared/src/commonMain` contient la navigation, le thème, les écrans et leurs
  presenters dans les packages `*_view`. Les textes partagés sont dans
  `shared/src/commonMain/composeResources/values/strings.xml`.
- `QuizApp.kt` assemble les dépendances et le thème. Dans `navigation_view`,
  `QuizNavigation` affiche les écrans et transmet les actions au
  `QuizNavigationPresenter`, qui coordonne les presenters du quiz, des défis,
  du profil et du classement. Compose observe leur état partagé sur les deux
  plateformes.
- `shared/src/androidMain` relie l’application à CameraX, au sélecteur Android
  et aux données locales Android existantes.
- `shared/src/jvmMain` crée la fenêtre bureau et relie l’application à la
  webcam, au sélecteur de fichiers et aux données locales du bureau.
- `shared/src/jvmShared` contient le stockage du profil commun aux deux cibles JVM.
- `app` lance le quiz sur Android et `desktop` le lance sur ordinateur.
- `settings.gradle.kts` déclare ces trois modules.
- `build.gradle.kts` : fixe les versions des outils Android et Kotlin.
- `app/build.gradle.kts` : configure l’application et ses bibliothèques Compose.
- `app/src/main/AndroidManifest.xml` : déclare l’activité de lancement.
- `.gitignore` : écarte de Git les builds, les fichiers IDE locaux et les clés.
- `gradlew`, `gradlew.bat`, `gradle/wrapper/` : lancent la même version de Gradle
  pour tout le monde ; ces fichiers doivent être conservés dans Git.

## Versions retenues

Gradle 8.11.1, plugin Android 8.10.1, Kotlin et compilateur Compose 2.3.21,
Compose Multiplatform 1.9.3, Activity Compose 1.10.1, SDK de compilation/cible 36.
Les bibliothèques sont fixées à des versions stables ; il ne s’agit pas d’une
sélection automatique des dernières versions.

Références de compatibilité : [plugin Android et Gradle](https://developer.android.com/build/releases/agp-8-10-0-release-notes),
[Kotlin et Gradle](https://kotlinlang.org/docs/gradle-configure-project.html),
[compilateur Compose](https://developer.android.com/develop/ui/compose/compiler),
[Compose BOM de mai 2025](https://android-developers.googleblog.com/2025/05/whats-new-in-jetpack-compose.html).
