package com.example.data

object LocalExercises {
    // Represents a beautifully preloaded Eye Exercise matching the expected Gemini output format
    data class FallbackExercise(
        val title: String,
        val steps: String,
        val durationSeconds: Int,
        val benefit: String
    )

    // A map of lists of scientifically-backed eye exercises customized per language
    private val exercisesByLanguage = mapOf(
        "en" to listOf(
            FallbackExercise(
                title = "20-20-20 Rule",
                steps = "Look away from your screen. Focus on an object at least 20 feet (6 meters) away for a full 20 seconds.",
                durationSeconds = 20,
                benefit = "Relaxes the ciliary muscle."
            ),
            FallbackExercise(
                title = "Intentional Blinking",
                steps = "Squeeze eyes gently and open to restore tear film. Complete several gentle cycles.",
                durationSeconds = 20,
                benefit = "Restores tear film. People blink 66% less at screens."
            ),
            FallbackExercise(
                title = "Palming",
                steps = "Rub hands to warm them, place gently over closed eyes for 30-60 seconds.",
                durationSeconds = 30,
                benefit = "Darkness and warmth relax the optic nerve."
            ),
            FallbackExercise(
                title = "Screen Ergonomics",
                steps = "Hold the phone 30-40 cm away and keep the screen slightly below eye level (10-15 cm).",
                durationSeconds = 20,
                benefit = "Reduces tear evaporation."
            )
        ),
        "ar" to listOf(
            FallbackExercise(
                title = "قاعدة 20-20-20",
                steps = "انظر بعيدًا عن شاشتك. ركز على شيء يبعد عنك 20 قدمًا (6 أمتار) على الأقل لمدة 20 ثانية كاملة.",
                durationSeconds = 20,
                benefit = "ترخي العضلة الهدبية للعين."
            ),
            FallbackExercise(
                title = "الرمش المتعمد",
                steps = "أغمض عينيك بلطف ثم افتحهما لتستعيد الغشاء الدمعي. كرر العملية لعدة مرات متتالية.",
                durationSeconds = 20,
                benefit = "يعيد بناء الغشاء الدمعي للقرنية. يقل معدل رمش العين بنسبة 66% عند الشاشات."
            ),
            FallbackExercise(
                title = "تدليك راحة اليد (Palming)",
                steps = "افرك يديك معًا لتوليد الدفء، ثم ضع كفيك بلطف فوق عينيك المغمضتين لمدة تتراوح بين 30 إلى 60 ثانية.",
                durationSeconds = 30,
                benefit = "تساعد الظلمة والحرارة اللطيفة على إرخاء العصب البصري تمامًا."
            ),
            FallbackExercise(
                title = "هندسة موضع الشاشة",
                steps = "حافظ على مسافة 30-40 سم للهاتف من عينك، واجعل مستوى الشاشة أقل قليلاً من مستوى العين بمقدار 10-15 سم.",
                durationSeconds = 20,
                benefit = "يقلل التبخر السريع لدموع العين المرطبة."
            )
        ),
        "fr" to listOf(
            FallbackExercise(
                title = "Règle 20-20-20",
                steps = "Détournez le regard de l'écran. Concentrez-vous sur un objet situé à au moins 6 mètres pendant 20 secondes.",
                durationSeconds = 20,
                benefit = "Relâche le muscle ciliaire."
            ),
            FallbackExercise(
                title = "Clignement intentionnel",
                steps = "Fermez doucement les yeux, puis ouvrez-les pour restaurer le film lacrymal.",
                durationSeconds = 20,
                benefit = "Restaure le film lacrymal. Les gens clignent 66% moins des yeux devant un écran."
            ),
            FallbackExercise(
                title = "Palming oculaire",
                steps = "Frottez vos mains pour les réchauffer, puis placez-les doucement sur vos yeux fermés pendant 30 à 60 secondes.",
                durationSeconds = 30,
                benefit = "L'obscurité et la chaleur détendent le nerf optique."
            ),
            FallbackExercise(
                title = "Ergonomie de l'écran",
                steps = "Tenez votre téléphone à une distance de 30-40 cm et gardez l'écran légèrement plus bas que le niveau des yeux (10-15 cm).",
                durationSeconds = 20,
                benefit = "Réduit l'évaporation des larmes."
            )
        ),
        "de" to listOf(
            FallbackExercise(
                title = "20-20-20-Regel",
                steps = "Schauen Sie vom Bildschirm weg. Fokussieren Sie ein Objekt in mindestens 6 Metern Entfernung für ganze 20 Sekunden.",
                durationSeconds = 20,
                benefit = "Entspannt den Ziliarmuskel."
            ),
            FallbackExercise(
                title = "Bewusstes Blinzeln",
                steps = "Kneifen Sie die Augen sanft zusammen und öffnen Sie sie wieder, um den Tränenfilm zu regenerieren.",
                durationSeconds = 20,
                benefit = "Stellt den Tränenfilm wieder her. Menschen blinzeln vor Bildschirmen um 66 % weniger."
            ),
            FallbackExercise(
                title = "Palming (Handauflegung)",
                steps = "Reiben Sie Ihre Hände warm, legen Sie sie dann für 30 bis 60 Sekunden sanft auf die geschlossenen Augen.",
                durationSeconds = 30,
                benefit = "Dunkelheit und Wärme entspannen den Sehnerv."
            ),
            FallbackExercise(
                title = "Bildschirm-Ergonomie",
                steps = "Halten Sie das Telefon 30-40 cm weit entfernt und positionieren Sie den Bildschirm leicht unter Augenhöhe (10-15 cm).",
                durationSeconds = 20,
                benefit = "Reduziert das Verdunsten der Tränenflüssigkeit."
            )
        ),
        "es" to listOf(
            FallbackExercise(
                title = "Regla 20-20-20",
                steps = "Deje de mirar la pantalla. Enfoque la vista en un objeto a una distancia de al menos 6 metros durante 20 segundos.",
                durationSeconds = 20,
                benefit = "Relaja el músculo ciliar."
            ),
            FallbackExercise(
                title = "Parpadeo intencional",
                steps = "Cierre los ojos suavemente y luego ábralos para restaurar la película lagrimal corneal.",
                durationSeconds = 20,
                benefit = "Restaura la película lagrimal. Las personas parpadean un 66% menos al mirar pantallas."
            ),
            FallbackExercise(
                title = "Palmeo (Palming)",
                steps = "Frote sus manos para calentarlas, luego colóquelas suavemente sobre los ojos cerrados durante 30 a 60 segundos.",
                durationSeconds = 30,
                benefit = "La oscuridad y el calor relajan el nervio óptico."
            ),
            FallbackExercise(
                title = "Ergonomía de la pantalla",
                steps = "Sostenga el teléfono a 30-40 cm de distancia y mantenga la pantalla ligeramente por debajo del nivel de los ojos (10-15 cm).",
                durationSeconds = 20,
                benefit = "Reduce la evaporación de las lágrimas."
            )
        )
    )

    // Safely retrieves a random exercise representing the scientifically backed backup catalog
    fun getRandomFallback(langCode: String): FallbackExercise {
        val normalizedCode = langCode.trim().lowercase()
        val list = exercisesByLanguage[normalizedCode] ?: exercisesByLanguage["en"]!!
        return list.random()
    }
}
